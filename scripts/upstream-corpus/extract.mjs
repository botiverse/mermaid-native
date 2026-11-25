#!/usr/bin/env node
// Import every Mermaid source string from upstream mermaid-js tests into
// compatibility/upstream-corpus/ as fixture files with provenance.
//
// Two upstream test kinds are read, at the pinned revision in compatibility/upstreams.lock:
//  - cypress: every imgSnapshotTest()/renderGraph() call in cypress/integration/rendering/**
//    (one case per call / array element, options kept), same rules as scripts/render-parity.
//  - spec: every string literal in packages/**/*.{spec,test}.{js,ts} whose text starts with a
//    Mermaid diagram header (after optional front matter, %% directives and comments).
//    String concatenation and template literals whose ${} parts resolve to const string
//    bindings are evaluated statically. Identical sources within one diagram directory are
//    stored once; every occurrence is listed in provenance.tsv.
//
// Nothing from upstream is executed. Calls/strings that cannot be resolved statically are
// listed in skipped.tsv.
//
// Usage:
//   node scripts/upstream-corpus/extract.mjs --mermaid <checkout> [--out dir] [--check]
import { createHash } from 'node:crypto';
import { createRequire } from 'node:module';
import { existsSync, mkdirSync, readFileSync, readdirSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { dedent, extractFromSpec, slugify, stableJson } from '../render-parity/extract-corpus.mjs';

const here = dirname(fileURLToPath(import.meta.url));
// Dependencies are pinned once, in scripts/render-parity/package.json.
const { parse } = createRequire(join(here, '../render-parity/package.json'))('@babel/parser');
const repoRoot = resolve(here, '../..');
const CORPUS_REL = 'compatibility/upstream-corpus';

/** Diagram headers recognised by Mermaid's detectors at the pinned revision (plus graph/flowchart/zenuml). */
const HEADERS = [
  'flowchart-elk', 'flowchart-v2', 'flowchart', 'graph', 'sequenceDiagram', 'classDiagram-v2', 'classDiagram',
  'stateDiagram-v2', 'stateDiagram', 'erDiagram', 'gantt', 'pie', 'gitGraph', 'journey', 'mindmap', 'timeline',
  'C4Context', 'C4Container', 'C4Component', 'C4Dynamic', 'C4Deployment', 'xychart-beta', 'xychart',
  'quadrantChart', 'requirementDiagram', 'requirement', 'block-beta', 'block', 'sankey-beta', 'sankey',
  'packet-beta', 'packet', 'architecture-beta', 'architecture', 'kanban', 'radar-beta', 'treemap-beta', 'treemap',
  'venn-beta', 'treeView-beta', 'ishikawa-beta', 'ishikawa', 'cynefin-beta', 'wardley-beta', 'railroad-abnf-beta',
  'railroad-ebnf-beta', 'railroad-peg-beta', 'railroad-beta', 'usecase-beta', 'eventmodeling', 'swimlane-beta',
  'info', 'zenuml',
];
const PREAMBLE = /^(?:\s*---\n[\s\S]*?\n\s*---\s*\n)?(?:\s*%%\{[\s\S]*?\}%%[^\n]*(?:\n|$)|\s*%%[^\n]*\n|\s*\n)*/;
const HEADER_RE = new RegExp(`^(${HEADERS.map((h) => h.replace(/[-]/g, '\\-')).join('|')})(?=[\\s;:]|$)`, 'i');

/** Diagram type of a source (header keyword normalised), or null when it has none. */
export function diagramType(source) {
  const body = source.replace(/\r\n?/g, '\n').replace(PREAMBLE, '').trimStart();
  const m = body.match(HEADER_RE);
  if (!m) return null;
  const h = m[1].toLowerCase();
  if (h === 'graph' || h.startsWith('flowchart')) return 'flowchart';
  if (h.startsWith('c4')) return 'c4';
  if (h.startsWith('railroad')) return 'railroad';
  if (h.startsWith('requirement')) return 'requirement';
  if (h.startsWith('classdiagram')) return 'class';
  if (h.startsWith('statediagram')) return 'state';
  if (h === 'sequencediagram') return 'sequence';
  if (h === 'erdiagram') return 'er';
  if (h === 'gitgraph') return 'git';
  if (h === 'quadrantchart') return 'quadrant';
  if (h === 'treeview-beta') return 'treeview';
  return h.replace(/-beta$/, '');
}

const EXACT_HEADER_RE = new RegExp(HEADER_RE.source);

/** True when the source starts with a header spelled exactly as a Mermaid detector expects. */
export function hasExactHeader(source) {
  return EXACT_HEADER_RE.test(source.replace(/\r\n?/g, '\n').replace(PREAMBLE, '').trimStart());
}

/** A spec string is a diagram source when, after the preamble, a header is followed by content. */
function looksLikeDiagram(text) {
  const body = text.replace(/\r\n?/g, '\n').replace(PREAMBLE, '').trimStart();
  const m = body.match(HEADER_RE);
  if (!m) return false;
  const rest = body.slice(m[0].length);
  // A bare keyword ("pie", "graph") is a type name, not a diagram; require a body or a direction.
  if (!/\S/.test(rest) || !(/[\n;]/.test(rest) || /^\s+\S/.test(rest))) return false;
  // Single-line prose ("Block diagram", "Journey to the shops") only counts with an exact-case header.
  return /\n/.test(body.trim()) || EXACT_HEADER_RE.test(body);
}

class NonLiteral extends Error {}

class Scope {
  constructor(parent) { this.parent = parent; this.vars = new Map(); }
  lookup(name) {
    for (let s = this; s; s = s.parent) if (s.vars.has(name)) return s.vars.get(name);
    return undefined;
  }
}
const SCOPE_NODES = new Set(['Program', 'BlockStatement', 'ArrowFunctionExpression', 'FunctionExpression', 'FunctionDeclaration', 'ForStatement', 'ForOfStatement', 'ForInStatement']);

/** Evaluate a string-valued expression: literals, +, templates with const-resolvable parts. */
function stringValue(node, scope, depth = 0) {
  if (depth > 20) throw new NonLiteral('depth');
  const sv = (n) => stringValue(n, scope, depth + 1);
  switch (node.type) {
    case 'StringLiteral': return node.value;
    case 'NumericLiteral': return String(node.value);
    case 'TemplateLiteral':
      return node.quasis.map((q, i) => q.value.cooked + (i < node.expressions.length ? sv(node.expressions[i]) : '')).join('');
    case 'BinaryExpression':
      if (node.operator !== '+') throw new NonLiteral(`operator ${node.operator}`);
      return sv(node.left) + sv(node.right);
    case 'Identifier': {
      const init = scope.lookup(node.name);
      if (!init) throw new NonLiteral(`identifier ${node.name}`);
      return sv(init);
    }
    case 'TSAsExpression': case 'TSSatisfiesExpression': case 'TSNonNullExpression': return sv(node.expression);
    default: throw new NonLiteral(node.type);
  }
}

const STRINGISH = new Set(['StringLiteral', 'TemplateLiteral', 'BinaryExpression']);

/** Raw text of a string-ish node that may contain a header, ignoring unresolvable parts. */
function roughText(node) {
  if (node.type === 'StringLiteral') return node.value;
  if (node.type === 'TemplateLiteral') return node.quasis.map((q) => q.value.cooked ?? q.value.raw).join('${…}');
  if (node.type === 'BinaryExpression' && node.operator === '+') {
    const l = STRINGISH.has(node.left.type) ? roughText(node.left) : '${…}';
    const r = STRINGISH.has(node.right.type) ? roughText(node.right) : '${…}';
    return l + r;
  }
  return '';
}

function titleOf(node) {
  if (!node) return '';
  if (node.type === 'StringLiteral') return node.value;
  if (node.type === 'TemplateLiteral') return node.quasis.map((q, i) => q.value.cooked + (i < node.expressions.length ? '${…}' : '')).join('');
  return `<${node.type}>`;
}

/** Every diagram source string in one unit spec. Returns {cases, skipped}. */
export function extractStrings(code, upstreamPath) {
  const ast = parse(code, {
    sourceType: 'module',
    plugins: [...(/\.tsx?$/.test(upstreamPath) ? ['typescript'] : ['jsx']), ['optionalChainingAssign', { version: '2023-07' }]],
    errorRecovery: true,
  });
  const cases = [];
  const skipped = [];
  let index = 0;
  const visit = (node, stack, scope) => {
    if (!node || typeof node.type !== 'string') return;
    if (SCOPE_NODES.has(node.type)) scope = new Scope(scope);
    if (node.type === 'VariableDeclaration') {
      for (const d of node.declarations) {
        if (d.id.type === 'Identifier') scope.vars.set(d.id.name, node.kind === 'const' && d.init ? d.init : null);
      }
    }
    for (const p of node.params ?? []) if (p.type === 'Identifier') scope.vars.set(p.name, null);
    if (STRINGISH.has(node.type)) {
      const rough = roughText(node);
      if (looksLikeDiagram(rough.replace(/\$\{…\}/g, 'x'))) {
        const title = stack.map((s) => s.title).join(' > ');
        const i = index++;
        try {
          const text = stringValue(node, scope);
          if (looksLikeDiagram(text)) cases.push({ upstreamPath, index: String(i), title, source: dedent(text) });
        } catch (e) {
          skipped.push({ upstreamPath, index: String(i), title, reason: `unresolved (${e.message})` });
        }
        return; // nested parts of a matched string are not separate sources
      }
      if (node.type === 'StringLiteral') return;
    }
    let pushed = false;
    let titleNode = null;
    if (node.type === 'CallExpression') {
      // it(title), it.skip(title), and it.each(table)(title): the title is a string argument.
      const c = node.callee.type === 'CallExpression' ? node.callee.callee : node.callee;
      const name = c.type === 'Identifier' ? c.name
        : c.type === 'MemberExpression' && c.object.type === 'Identifier' ? c.object.name : null;
      const titled = node.arguments.length && ['StringLiteral', 'TemplateLiteral'].includes(node.arguments[0].type);
      if ((name === 'it' || name === 'describe' || name === 'test') && titled) {
        stack.push({ title: titleOf(node.arguments[0]) });
        pushed = true;
        titleNode = node.arguments[0]; // a test title is never a diagram source
      }
    }
    for (const key of Object.keys(node)) {
      if (key === 'loc' || key === 'start' || key === 'end' || key === 'leadingComments' || key === 'trailingComments' || key === 'innerComments' || key === 'extra') continue;
      const v = node[key];
      if (Array.isArray(v)) v.forEach((x) => { if (x !== titleNode) visit(x, stack, scope); });
      else if (v && typeof v === 'object' && typeof v.type === 'string') visit(v, stack, scope);
    }
    if (pushed) stack.pop();
  };
  visit(ast.program, [], new Scope(null));
  return { cases, skipped };
}

const sha256 = (s) => createHash('sha256').update(s).digest('hex');
const tsvCell = (s) => String(s ?? '').replace(/[\t\n\r]/g, ' ');

function listFiles(root, rel, pred, out = []) {
  const abs = join(root, rel);
  if (!existsSync(abs)) return out;
  for (const f of readdirSync(abs).sort()) {
    if (f === 'node_modules' || f.startsWith('.')) continue;
    const r = `${rel}/${f}`;
    if (statSync(join(root, r)).isDirectory()) listFiles(root, r, pred, out);
    else if (pred(r)) out.push(r);
  }
  return out;
}

/** Directory family of an upstream path, used for fixture folders. */
function familyOf(kind, path) {
  if (kind === 'cypress') {
    const parts = path.split('/');
    return parts.length > 4 ? parts[3] : 'misc';
  }
  let m = path.match(/packages\/mermaid\/src\/diagrams\/([^/]+)\//);
  if (m) return m[1];
  m = path.match(/packages\/parser\/tests\/([A-Za-z]+?)(?:ValueConverter)?\.test\./);
  if (m) return m[1];
  m = path.match(/^packages\/([^/]+)\//);
  return m && m[1] !== 'mermaid' ? m[1] : 'mermaid-core';
}

export const MANIFEST_HEADER = ['case', 'kind', 'family', 'diagram_type', 'fixture', 'upstream_commit', 'upstream_path', 'upstream_index', 'upstream_case', 'occurrences', 'sha256', 'options_json'];
const PROVENANCE_HEADER = ['case', 'upstream_path', 'upstream_index', 'upstream_case'];
const SKIPPED_HEADER = ['kind', 'upstream_path', 'upstream_index', 'upstream_case', 'reason'];

export function buildCorpus(mermaidDir, commit) {
  const groups = new Map(); // `${kind}/${family}` -> [{...}]
  const skipped = [];
  const add = (kind, c) => {
    const family = familyOf(kind, c.upstreamPath);
    const key = `${kind}/${family}`;
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push({ kind, family, ...c });
  };
  for (const p of listFiles(mermaidDir, 'cypress/integration/rendering', (r) => /\.spec\.[jt]s$/.test(r))) {
    const { cases, skipped: s } = extractFromSpec(readFileSync(join(mermaidDir, p), 'utf8'), p, { lenientOptions: true });
    cases.forEach((c) => add('cypress', c));
    s.forEach((x) => skipped.push({ kind: 'cypress', ...x }));
  }
  for (const p of listFiles(mermaidDir, 'packages', (r) => /\.(spec|test)\.[cm]?[jt]sx?$/.test(r))) {
    const { cases, skipped: s } = extractStrings(readFileSync(join(mermaidDir, p), 'utf8'), p);
    cases.forEach((c) => add('spec', { ...c, options: {} }));
    s.forEach((x) => skipped.push({ kind: 'spec', ...x }));
  }
  const rows = [];
  const provenance = [];
  const files = new Map();
  for (const key of [...groups.keys()].sort()) {
    const list = groups.get(key);
    // Spec sources are deduplicated per family; cypress calls are kept 1:1 (options may differ).
    const unique = [];
    const bySig = new Map();
    for (const c of list) {
      const sig = c.kind === 'spec' ? sha256(c.source) : null;
      if (sig && bySig.has(sig)) { bySig.get(sig).refs.push(c); continue; }
      const u = { ...c, refs: [c] };
      if (sig) bySig.set(sig, u);
      unique.push(u);
    }
    unique.forEach((u, i) => {
      const caseId = `${key}/${String(i + 1).padStart(3, '0')}-${slugify(u.title.split(' > ').pop())}`;
      const fixture = `${caseId}.mmd`;
      files.set(fixture, u.source);
      rows.push({
        case: caseId, kind: u.kind, family: u.family, diagram_type: diagramType(u.source) ?? 'unknown',
        fixture: `${CORPUS_REL}/${fixture}`, upstream_commit: commit, upstream_path: u.upstreamPath,
        upstream_index: u.index, upstream_case: u.title, occurrences: u.refs.length,
        sha256: sha256(u.source), options_json: stableJson(u.options ?? {}),
      });
      for (const r of u.refs) provenance.push({ case: caseId, upstream_path: r.upstreamPath, upstream_index: r.index, upstream_case: r.title });
    });
  }
  const tsv = (header, list) => [header.join('\t'), ...list.map((r) => header.map((h) => tsvCell(r[h])).join('\t'))].join('\n') + '\n';
  return {
    rows, files,
    manifest: tsv(MANIFEST_HEADER, rows),
    provenance: tsv(PROVENANCE_HEADER, provenance),
    skippedTsv: tsv(SKIPPED_HEADER, skipped.map((s) => ({ kind: s.kind, upstream_path: s.upstreamPath, upstream_index: s.index, upstream_case: s.title, reason: s.reason }))),
  };
}

function main() {
  const opt = { out: join(repoRoot, CORPUS_REL) };
  for (let i = 2; i < process.argv.length; i++) {
    const a = process.argv[i];
    if (a === '--mermaid') opt.mermaid = process.argv[++i];
    else if (a === '--out') opt.out = process.argv[++i];
    else if (a === '--check') opt.check = true;
    else throw new Error(`unknown argument ${a}`);
  }
  if (!opt.mermaid) throw new Error('--mermaid <checkout> is required');
  const commit = process.env.MERMAID_COMMIT
    ?? readFileSync(join(repoRoot, 'compatibility/upstreams.lock'), 'utf8').match(/^mermaid=(\w+)/m)[1];
  const corpus = buildCorpus(resolve(opt.mermaid), commit);
  const outDir = resolve(opt.out);
  const counts = {};
  for (const r of corpus.rows) {
    const c = (counts[r.kind] ??= { cases: 0, occurrences: 0 });
    c.cases++; c.occurrences += Number(r.occurrences);
  }
  const generated = new Map([...[...corpus.files].map(([k, v]) => [k, v]), ['manifest.tsv', corpus.manifest], ['provenance.tsv', corpus.provenance], ['skipped.tsv', corpus.skippedTsv]]);
  if (opt.check) {
    const drift = [];
    for (const [rel, text] of generated) {
      const p = join(outDir, rel);
      if (!existsSync(p) || readFileSync(p, 'utf8') !== text) drift.push(rel);
    }
    for (const kind of ['cypress', 'spec']) {
      for (const f of listFiles(outDir, kind, (r) => r.endsWith('.mmd'))) if (!generated.has(f)) drift.push(`${f} (orphan)`);
    }
    console.log(JSON.stringify({ check: drift.length ? 'drift' : 'clean', counts, drift: drift.slice(0, 20) }, null, 1));
    process.exit(drift.length ? 1 : 0);
  }
  for (const kind of ['cypress', 'spec']) rmSync(join(outDir, kind), { recursive: true, force: true });
  for (const [rel, text] of generated) {
    mkdirSync(dirname(join(outDir, rel)), { recursive: true });
    writeFileSync(join(outDir, rel), text);
  }
  console.log(JSON.stringify({ commit, out: relative(process.cwd(), outDir), counts }, null, 1));
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) main();
