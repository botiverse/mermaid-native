#!/usr/bin/env node
// Reference outcome of every upstream-corpus case with mermaid.js (npm version pinned in
// scripts/render-parity/package.json, matching the pinned upstream revision) in headless
// Chromium: `ok` (parse + render succeed), `parse-error` (mermaid.parse rejects),
// `render-error` (parse accepts, render throws), `timeout`, or `unsupported` (the source has an
// exact diagram header but the npm release has no such diagram registered, e.g. usecase-beta
// exists at the pinned commit but not in the release; zenuml is a separate package).
//
// Writes compatibility/upstream-corpus/reference.tsv, which is committed so the JVM
// conformance test (UpstreamCorpusTest) needs no browser.
//
// Usage: node scripts/upstream-corpus/reference.mjs [--only substr] [--timeout ms] [--chromium path]
import { createRequire } from 'node:module';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, extname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { hasExactHeader } from './extract.mjs';

const here = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(here, '../..');
const corpusDir = join(repoRoot, 'compatibility/upstream-corpus');
const require = createRequire(join(here, '../render-parity/package.json'));
const { chromium } = require('playwright');

const opt = { only: '', timeout: '20000', chromium: process.env.CHROMIUM_PATH ?? '' };
for (let i = 2; i < process.argv.length; i += 2) {
  const k = process.argv[i].replace(/^--/, '');
  if (!(k in opt)) throw new Error(`unknown argument ${process.argv[i]}`);
  opt[k] = process.argv[i + 1];
}
const timeoutMs = Number(opt.timeout);
const mermaidVersion = require('mermaid/package.json').version;
const roots = {
  '/mermaid/': join(dirname(require.resolve('mermaid/package.json')), 'dist/'),
  '/elk/': join(dirname(require.resolve('@mermaid-js/layout-elk/package.json')), 'dist/'),
};
const ORIGIN = 'http://upstream-corpus.invalid';
const INDEX = `<!doctype html><html><head><meta charset="utf-8"></head><body><div id="stage"></div>
<script type="module">
import mermaid from '/mermaid/mermaid.esm.min.mjs';
import elk from '/elk/mermaid-layout-elk.esm.min.mjs';
mermaid.registerLayoutLoaders(elk);
window.mermaid = mermaid;
window.ready = true;
</script></body></html>`;
const TYPES = { '.mjs': 'text/javascript', '.js': 'text/javascript', '.map': 'application/json', '.css': 'text/css' };

function readManifest() {
  const [head, ...lines] = readFileSync(join(corpusDir, 'manifest.tsv'), 'utf8').trimEnd().split('\n');
  const cols = head.split('\t');
  return lines.map((l) => Object.fromEntries(l.split('\t').map((v, i) => [cols[i], v])));
}

async function newPage(browser) {
  const page = await browser.newPage({ viewport: { width: 1600, height: 1000 } });
  await page.route(`${ORIGIN}/**`, (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === '/') return route.fulfill({ contentType: 'text/html', body: INDEX });
    for (const [prefix, dir] of Object.entries(roots)) {
      if (!path.startsWith(prefix)) continue;
      const file = join(dir, path.slice(prefix.length));
      if (!file.startsWith(dir) || !existsSync(file)) break;
      return route.fulfill({ contentType: TYPES[extname(file)] ?? 'application/octet-stream', body: readFileSync(file) });
    }
    return route.fulfill({ status: 404, body: 'not found' });
  });
  await page.goto(`${ORIGIN}/`);
  await page.waitForFunction(() => window.ready === true);
  return page;
}

/** Literal upstream options minus unresolved markers; same seeds as the Cypress helper. */
function configOf(row) {
  const o = JSON.parse(row.options_json);
  delete o.$unresolved;
  return { ...o, handDrawnSeed: 1, architecture: { seed: 1, ...(o.architecture ?? {}) } };
}

async function runCase(page, row) {
  const source = readFileSync(join(repoRoot, row.fixture), 'utf8');
  return page.evaluate(async ([code, cfg]) => {
    document.querySelectorAll('body > :not(#stage):not(script)').forEach((n) => n.remove());
    window.mermaid.initialize({ startOnLoad: false, ...cfg });
    try { await window.mermaid.parse(code); } catch (e) { return { status: 'parse-error', error: String(e?.message ?? e) }; }
    try { await window.mermaid.render(`ref${Date.now()}`, code); } catch (e) { return { status: 'render-error', error: String(e?.message ?? e) }; }
    return { status: 'ok', error: '' };
  }, [source, configOf(row)]);
}

const clean = (s) => String(s ?? '').replace(/\s+/g, ' ').trim().slice(0, 160);

async function main() {
  const rows = readManifest().filter((r) => !opt.only || r.case.includes(opt.only));
  const browser = await chromium.launch(opt.chromium ? { executablePath: opt.chromium } : {});
  let page = await newPage(browser);
  const out = [];
  const tally = {};
  for (let i = 0; i < rows.length; i++) {
    const row = rows[i];
    let r;
    try {
      r = await Promise.race([
        runCase(page, row),
        new Promise((res) => setTimeout(() => res({ status: 'timeout', error: `no result within ${timeoutMs} ms` }), timeoutMs)),
      ]);
    } catch (e) {
      r = { status: 'render-error', error: `page crashed: ${e?.message ?? e}` };
    }
    if (r.status === 'timeout' || /page crashed/.test(r.error)) {
      await page.close().catch(() => {});
      page = await newPage(browser);
    }
    if (r.status === 'parse-error' && /No diagram type detected/.test(r.error)
      && hasExactHeader(readFileSync(join(repoRoot, row.fixture), 'utf8'))) r.status = 'unsupported';
    tally[r.status] = (tally[r.status] ?? 0) + 1;
    out.push([row.case, row.sha256, r.status, clean(r.error)].join('\t'));
    if ((i + 1) % 200 === 0) console.error(`reference: ${i + 1}/${rows.length}`);
  }
  await browser.close();
  if (!opt.only) {
    writeFileSync(join(corpusDir, 'reference.tsv'),
      `# mermaid ${mermaidVersion} (npm) in headless Chromium; regenerate with scripts/upstream-corpus/reference.mjs\n` +
      ['case\tsha256\tref_status\tref_error', ...out].join('\n') + '\n');
  } else {
    console.log(out.join('\n'));
  }
  console.log(JSON.stringify({ mermaid: mermaidVersion, cases: rows.length, tally }));
}

await main();
