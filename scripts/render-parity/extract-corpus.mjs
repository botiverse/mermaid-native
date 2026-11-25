#!/usr/bin/env node
// Statically extract diagram sources from upstream Mermaid Cypress rendering
// specs into compatibility/render-parity/<family>/<NNN-slug>.mmd + manifest.tsv.
//
// Usage:
//   node extract-corpus.mjs --mermaid <checkout> [--out <dir>] [--check] [--diff <file>]
//
// --check  extract into memory, compare with the committed corpus and exit 1
//          on any difference (without writing).
// --diff   write a human-readable added/removed/changed/unchanged report.
//
// The specs are parsed with @babel/parser; nothing from the upstream harness is
// executed. Template literals with ${} interpolation, non-literal sources and
// non-literal option objects are skipped and recorded in skipped.tsv.
import { parse } from '@babel/parser';
import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(here, '../..');

export const FAMILIES = {
  flowchart: 'cypress/integration/rendering/flowchart',
  sequence: 'cypress/integration/rendering/sequence',
};
const SNAPSHOT_CALLS = new Set(['imgSnapshotTest', 'renderGraph']);
const MANIFEST_HEADER = [
  'family', 'case', 'fixture', 'upstream_commit', 'upstream_path', 'upstream_index',
  'upstream_case', 'helper', 'sha256', 'options_json',
];
const SKIPPED_HEADER = ['family', 'upstream_commit', 'upstream_path', 'upstream_index', 'upstream_case', 'reason'];

function args(argv) {
  const out = { out: join(repoRoot, 'compatibility/render-parity') };
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--mermaid') out.mermaid = argv[++i];
    else if (a === '--out') out.out = argv[++i];
    else if (a === '--check') out.check = true;
    else if (a === '--diff') out.diff = argv[++i];
    else throw new Error(`unknown argument ${a}`);
  }
  if (!out.mermaid) throw new Error('--mermaid <checkout> is required');
  return out;
}

class NonLiteral extends Error {}

/**
 * Evaluate a literal-only expression (object/array/string/number/bool/null).
 * `consts` resolves identifiers to the initializer of the innermost visible
 * `const` binding (static only; nothing is executed).
 */
export function literalValue(node, consts = new Scope(null)) {
  const lit = (n) => literalValue(n, consts);
  switch (node.type) {
    case 'Identifier':
      if (consts.has(node.name)) return lit(consts.get(node.name));
      throw new NonLiteral('Identifier');
    case 'StringLiteral': case 'NumericLiteral': case 'BooleanLiteral': return node.value;
    case 'NullLiteral': return null;
    case 'TemplateLiteral':
      if (node.expressions.length) throw new NonLiteral('template interpolation');
      return node.quasis[0].value.cooked;
    case 'UnaryExpression':
      if (node.operator === '-' && node.argument.type === 'NumericLiteral') return -node.argument.value;
      throw new NonLiteral(`unary ${node.operator}`);
    case 'ArrayExpression': return node.elements.map(lit);
    case 'ObjectExpression': {
      const o = {};
      for (const p of node.properties) {
        if (p.type !== 'ObjectProperty' || p.computed) throw new NonLiteral(p.type === 'SpreadElement' ? 'spread' : 'computed key');
        const key = p.key.type === 'Identifier' ? p.key.name : literalValue(p.key);
        o[key] = lit(p.value);
      }
      return o;
    }
    case 'TSAsExpression': case 'TSSatisfiesExpression': case 'TSNonNullExpression':
      return lit(node.expression);
    default: throw new NonLiteral(node.type);
  }
}

function titleOf(node) {
  if (!node) return '';
  if (node.type === 'StringLiteral') return node.value;
  if (node.type === 'TemplateLiteral') {
    return node.quasis.map((q, i) => q.value.cooked + (i < node.expressions.length ? '${…}' : '')).join('');
  }
  return `<${node.type}>`;
}

/**
 * Remove common indentation and surrounding blank lines (the e2e page dedents
 * too). The first line is trimmed on its own because sources often start right
 * after the opening backtick while the body is indented with the test code.
 */
export function dedent(text) {
  const lines = text.replace(/\r\n?/g, '\n').split('\n');
  while (lines.length && !lines[0].trim()) lines.shift();
  while (lines.length && !lines[lines.length - 1].trim()) lines.pop();
  if (!lines.length) return '\n';
  const [first, ...rest] = lines;
  const indents = rest.filter((l) => l.trim()).map((l) => l.match(/^[ \t]*/)[0].length);
  const firstIndent = first.match(/^[ \t]*/)[0].length;
  const cut = Math.min(firstIndent || Infinity, ...indents);
  const body = rest.map((l) => l.slice(Number.isFinite(cut) ? Math.min(cut, l.match(/^[ \t]*/)[0].length) : 0));
  return [first.trimStart(), ...body].map((l) => l.replace(/[ \t]+$/, '')).join('\n') + '\n';
}

export function slugify(title) {
  const s = title.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 60).replace(/-+$/, '');
  return s || 'case';
}

/** Scope chain lookup: innermost `const X = ...` binding wins; `let`/`var` shadow as non-literal. */
class Scope {
  constructor(parent) { this.parent = parent; this.vars = new Map(); }
  has(name) { return this.lookup(name) !== undefined; }
  get(name) { return this.lookup(name); }
  lookup(name) {
    for (let s = this; s; s = s.parent) if (s.vars.has(name)) return s.vars.get(name) ?? undefined;
    return undefined;
  }
}

const SCOPE_NODES = new Set(['Program', 'BlockStatement', 'ArrowFunctionExpression', 'FunctionExpression', 'FunctionDeclaration']);

/** Walk an AST in source order, tracking it()/describe() titles and const bindings. */
function walk(node, visit, stack = [], scope = new Scope(null)) {
  if (!node || typeof node.type !== 'string') return;
  if (SCOPE_NODES.has(node.type)) scope = new Scope(scope);
  if (node.type === 'VariableDeclaration') {
    for (const d of node.declarations) {
      if (d.id.type === 'Identifier') scope.vars.set(d.id.name, node.kind === 'const' && d.init ? d.init : null);
    }
  }
  for (const p of node.params ?? []) if (p.type === 'Identifier') scope.vars.set(p.name, null);
  let pushed = false;
  if (node.type === 'CallExpression') {
    const c = node.callee;
    const name = c.type === 'Identifier' ? c.name
      : c.type === 'MemberExpression' && c.object.type === 'Identifier' ? c.object.name : null;
    if ((name === 'it' || name === 'describe') && node.arguments.length) {
      stack.push({ kind: name, title: titleOf(node.arguments[0]) });
      pushed = true;
    }
    visit(node, stack, scope);
  }
  for (const key of Object.keys(node)) {
    if (key === 'loc' || key === 'start' || key === 'end' || key === 'leadingComments' || key === 'trailingComments' || key === 'params') continue;
    const v = node[key];
    if (Array.isArray(v)) v.forEach((x) => walk(x, visit, stack, scope));
    else if (v && typeof v === 'object' && typeof v.type === 'string') walk(v, visit, stack, scope);
  }
  if (pushed) stack.pop();
}

/**
 * Extract snapshot calls from one spec file's text. Returns {cases, skipped}.
 * With `lenientOptions`, calls whose options are not literal keep their source and get
 * `options = { $unresolved: reason }` instead of being skipped.
 */
export function extractFromSpec(code, upstreamPath, { lenientOptions = false } = {}) {
  const ast = parse(code, {
    sourceType: 'module',
    plugins: upstreamPath.endsWith('.ts') ? ['typescript'] : ['jsx'],
    errorRecovery: false,
  });
  const cases = [];
  const skipped = [];
  let index = 0;
  walk(ast.program, (call, stack, consts) => {
    if (call.callee.type !== 'Identifier' || !SNAPSHOT_CALLS.has(call.callee.name)) return;
    const helper = call.callee.name;
    const callIndex = index++;
    const title = stack.map((s) => s.title).join(' > ');
    const inLoop = stack.length === 0;
    const record = (reason, sub) => skipped.push({ upstreamPath, index: sub ?? String(callIndex), title, reason });
    const [src, opts] = call.arguments;
    let options = {};
    if (opts) {
      try { options = literalValue(opts, consts); } catch (e) {
        // lenientOptions keeps the (literal) source and marks the options as unresolved.
        if (!lenientOptions) return record(`non-literal options (${e.message})`);
        options = { $unresolved: e.message };
      }
      if (options === null || typeof options !== 'object' || Array.isArray(options)) {
        if (!lenientOptions) return record('options is not an object');
        options = { $unresolved: 'not an object' };
      }
    }
    const sources = [];
    if (!src) return record('missing source');
    if (src.type === 'ArrayExpression') src.elements.forEach((e, k) => sources.push([e, `${callIndex}.${k}`]));
    else sources.push([src, String(callIndex)]);
    for (const [node, sub] of sources) {
      let text;
      try { text = literalValue(node, consts); } catch (e) {
        record(e.message === 'template interpolation' ? 'template interpolation' : `non-literal source (${e.message})`, sub);
        continue;
      }
      if (typeof text !== 'string') { record('source is not a string', sub); continue; }
      if (inLoop) { record('outside it() block', sub); continue; }
      cases.push({ upstreamPath, index: sub, title, helper, source: dedent(text), options });
    }
  });
  return { cases, skipped };
}

const sha256 = (s) => createHash('sha256').update(s).digest('hex');

/** Stable JSON (sorted keys) so option hashes do not depend on key order. */
export function stableJson(v) {
  if (Array.isArray(v)) return `[${v.map(stableJson).join(',')}]`;
  if (v && typeof v === 'object') return `{${Object.keys(v).sort().map((k) => `${JSON.stringify(k)}:${stableJson(v[k])}`).join(',')}}`;
  return JSON.stringify(v);
}

const tsvCell = (s) => String(s).replace(/[\t\n\r]/g, ' ');

function indexKey(i) { return i.split('.').map(Number); }
function cmpIndex(a, b) {
  const x = indexKey(a), y = indexKey(b);
  for (let k = 0; k < Math.max(x.length, y.length); k++) {
    const d = (x[k] ?? -1) - (y[k] ?? -1);
    if (d) return d;
  }
  return 0;
}

export function buildCorpus(mermaidDir, commit) {
  const rows = [];
  const skippedRows = [];
  const files = new Map();
  for (const [family, dir] of Object.entries(FAMILIES)) {
    const abs = join(mermaidDir, dir);
    const specs = readdirSync(abs).filter((f) => /\.spec\.[jt]s$/.test(f)).sort();
    const familyCases = [];
    for (const f of specs) {
      const upstreamPath = `${dir}/${f}`;
      const { cases, skipped } = extractFromSpec(readFileSync(join(abs, f), 'utf8'), upstreamPath);
      familyCases.push(...cases);
      skipped.forEach((s) => skippedRows.push({ family, ...s }));
    }
    familyCases.sort((a, b) => a.upstreamPath.localeCompare(b.upstreamPath) || cmpIndex(a.index, b.index));
    familyCases.forEach((c, i) => {
      const caseId = `${String(i + 1).padStart(3, '0')}-${slugify(c.title.split(' > ').pop())}`;
      const fixture = `compatibility/render-parity/${family}/${caseId}.mmd`;
      files.set(`${family}/${caseId}.mmd`, c.source);
      rows.push({
        family, case: caseId, fixture, upstream_commit: commit, upstream_path: c.upstreamPath,
        upstream_index: c.index, upstream_case: c.title, helper: c.helper,
        sha256: sha256(c.source), options_json: stableJson(c.options),
      });
    });
  }
  skippedRows.sort((a, b) => a.family.localeCompare(b.family) || a.upstreamPath.localeCompare(b.upstreamPath) || cmpIndex(a.index, b.index));
  const manifest = [MANIFEST_HEADER.join('\t'), ...rows.map((r) => MANIFEST_HEADER.map((h) => tsvCell(r[h])).join('\t'))].join('\n') + '\n';
  const skippedTsv = [SKIPPED_HEADER.join('\t'), ...skippedRows.map((r) => [r.family, commit, r.upstreamPath, r.index, r.title, r.reason].map(tsvCell).join('\t'))].join('\n') + '\n';
  return { rows, skippedRows, files, manifest, skippedTsv };
}

export function parseManifest(text) {
  const [head, ...lines] = text.trimEnd().split('\n');
  const cols = head.split('\t');
  return lines.filter(Boolean).map((l) => Object.fromEntries(l.split('\t').map((v, i) => [cols[i], v])));
}

/** Compare manifests by upstream identity (path#index), reporting content changes by hash. */
export function diffManifests(oldRows, newRows) {
  const key = (r) => `${r.family}|${r.upstream_path}#${r.upstream_index}`;
  const sig = (r) => `${r.sha256}|${r.options_json}`;
  const oldMap = new Map(oldRows.map((r) => [key(r), r]));
  const newMap = new Map(newRows.map((r) => [key(r), r]));
  const out = { added: [], removed: [], changed: [], unchanged: [], renamed: [] };
  for (const [k, r] of newMap) {
    const o = oldMap.get(k);
    if (!o) out.added.push(r);
    else if (sig(o) !== sig(r)) out.changed.push({ old: o, new: r });
    else if (o.case !== r.case) out.renamed.push({ old: o, new: r });
    else out.unchanged.push(r);
  }
  for (const [k, o] of oldMap) if (!newMap.has(k)) out.removed.push(o);
  return out;
}

export function formatDiff(d, oldCommit, newCommit) {
  const lines = [`# Render-parity corpus diff`, '', `- old upstream: ${oldCommit}`, `- new upstream: ${newCommit}`,
    `- added: ${d.added.length}, removed: ${d.removed.length}, changed: ${d.changed.length}, renamed only: ${d.renamed.length}, unchanged: ${d.unchanged.length}`, ''];
  const sect = (name, items, fmt) => { if (items.length) lines.push(`## ${name}`, '', ...items.map(fmt), ''); };
  sect('Added', d.added, (r) => `- ${r.family}/${r.case} — ${r.upstream_path}#${r.upstream_index} “${r.upstream_case}”`);
  sect('Removed', d.removed, (r) => `- ${r.family}/${r.case} — ${r.upstream_path}#${r.upstream_index} “${r.upstream_case}”`);
  sect('Changed', d.changed, (c) => `- ${c.new.family}/${c.new.case} (was ${c.old.case}) — ${c.new.upstream_path}#${c.new.upstream_index}: sha ${c.old.sha256.slice(0, 12)}→${c.new.sha256.slice(0, 12)}${c.old.options_json !== c.new.options_json ? `, options ${c.old.options_json} → ${c.new.options_json}` : ''}`);
  sect('Renamed only', d.renamed, (c) => `- ${c.old.family}/${c.old.case} → ${c.new.case}`);
  return lines.join('\n') + '\n';
}

function readExisting(outDir) {
  const p = join(outDir, 'manifest.tsv');
  return existsSync(p) ? readFileSync(p, 'utf8') : null;
}

function main() {
  const opt = args(process.argv);
  const mermaidDir = resolve(opt.mermaid);
  let commit = '';
  try { commit = readFileSync(join(repoRoot, 'compatibility/upstreams.lock'), 'utf8').match(/^mermaid=(\w+)/m)[1]; } catch {}
  if (process.env.MERMAID_COMMIT) commit = process.env.MERMAID_COMMIT;
  const corpus = buildCorpus(mermaidDir, commit);
  const outDir = resolve(opt.out);
  const existing = readExisting(outDir);
  const oldRows = existing ? parseManifest(existing) : [];
  const diff = diffManifests(oldRows, corpus.rows);
  const oldCommit = oldRows[0]?.upstream_commit ?? '(none)';
  if (opt.diff) writeFileSync(opt.diff, formatDiff(diff, oldCommit, commit));

  const counts = {};
  for (const r of corpus.rows) (counts[r.family] ??= { extracted: 0, skipped: 0 }).extracted++;
  for (const r of corpus.skippedRows) (counts[r.family] ??= { extracted: 0, skipped: 0 }).skipped++;

  if (opt.check) {
    const fileDrift = [];
    for (const [rel, text] of corpus.files) {
      const p = join(outDir, rel);
      if (!existsSync(p) || readFileSync(p, 'utf8') !== text) fileDrift.push(rel);
    }
    for (const family of Object.keys(FAMILIES)) {
      const dir = join(outDir, family);
      if (!existsSync(dir)) continue;
      for (const f of readdirSync(dir)) if (!corpus.files.has(`${family}/${f}`)) fileDrift.push(`${family}/${f} (orphan)`);
    }
    const skippedPath = join(outDir, 'skipped.tsv');
    const skippedSame = existsSync(skippedPath) && readFileSync(skippedPath, 'utf8') === corpus.skippedTsv;
    const ok = existing === corpus.manifest && !fileDrift.length && skippedSame;
    console.log(JSON.stringify({ check: ok ? 'clean' : 'drift', counts, fileDrift: fileDrift.slice(0, 20), skippedSame }, null, 1));
    process.exit(ok ? 0 : 1);
  }

  for (const family of Object.keys(FAMILIES)) {
    rmSync(join(outDir, family), { recursive: true, force: true });
    mkdirSync(join(outDir, family), { recursive: true });
  }
  for (const [rel, text] of corpus.files) writeFileSync(join(outDir, rel), text);
  writeFileSync(join(outDir, 'manifest.tsv'), corpus.manifest);
  writeFileSync(join(outDir, 'skipped.tsv'), corpus.skippedTsv);
  console.log(JSON.stringify({ commit, out: relative(process.cwd(), outDir), counts,
    diff: { added: diff.added.length, removed: diff.removed.length, changed: diff.changed.length, renamed: diff.renamed.length, unchanged: diff.unchanged.length } }, null, 1));
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) main();
