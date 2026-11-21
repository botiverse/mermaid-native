#!/usr/bin/env node
// Headless Chromium step:
//  (a) render each case with mermaid.js (npm version pinned in package.json,
//      matching the pinned upstream revision) -> ref.svg + ref.png
//  (b) load the native SVG produced by native-render.mjs -> native.png
//  and reduce both SVGs to geometry records (see metrics.mjs) in geometry.json.
//
// Usage: node render-browser.mjs [--corpus dir] [--out dir] [--only substr] [--timeout ms]
import { createRequire } from 'node:module';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, extname, join } from 'node:path';
import { chromium } from 'playwright';
import { loadCorpus, parseArgs, repoRoot } from './common.mjs';

const require = createRequire(import.meta.url);
const opt = parseArgs(process.argv, {
  corpus: join(repoRoot, 'compatibility/render-parity'),
  out: join(repoRoot, 'build/render-parity'),
  only: '',
  timeout: '30000',
});
const timeoutMs = Number(opt.timeout);
const mermaidPkg = require('mermaid/package.json');
const roots = {
  '/mermaid/': join(dirname(require.resolve('mermaid/package.json')), 'dist/'),
  '/elk/': join(dirname(require.resolve('@mermaid-js/layout-elk/package.json')), 'dist/'),
};
const ORIGIN = 'http://render-parity.invalid';
const INDEX = `<!doctype html><html><head><meta charset="utf-8">
<style>body{margin:0;background:#fff;font-family:sans-serif} .stage{display:inline-block;padding:8px;background:#fff}</style>
</head><body><div id="stage" class="stage"></div>
<script type="module">
import mermaid from '/mermaid/mermaid.esm.min.mjs';
import elk from '/elk/mermaid-layout-elk.esm.min.mjs';
mermaid.registerLayoutLoaders(elk);
window.mermaid = mermaid;
window.parityReady = true;
</script></body></html>`;

/** Upstream Cypress helper defaults (cypress/helpers/util.ts at the pinned revision). */
function upstreamConfig(helper, options) {
  const o = { ...options };
  const base = { ...o, handDrawnSeed: 1, architecture: { seed: 1, ...(o.architecture ?? {}) } };
  if (helper !== 'imgSnapshotTest') return base;
  return {
    ...base,
    fontFamily: o.fontFamily ?? 'courier',
    fontSize: o.fontSize ?? '16px',
    sequence: {
      ...(o.sequence ?? {}),
      actorFontFamily: 'courier',
      noteFontFamily: o.sequence?.noteFontFamily ?? 'courier',
      messageFontFamily: 'courier',
    },
  };
}

// Runs inside the page. Reduces the <svg> inside `host` to a geometry record.
function pageExtract([hostId, flavor]) {
  const host = document.getElementById(hostId);
  const svg = host.querySelector('svg');
  if (!svg) return null;
  const rootInv = svg.getScreenCTM().inverse();
  const toUser = (x, y) => new DOMPoint(x, y).matrixTransform(rootInv);
  const rectOf = (el) => {
    const r = el.getBoundingClientRect();
    const a = toUser(r.left, r.top), b = toUser(r.right, r.bottom);
    return { x: Math.min(a.x, b.x), y: Math.min(a.y, b.y), w: Math.abs(b.x - a.x), h: Math.abs(b.y - a.y) };
  };
  const hidden = (el) => !!el.closest('defs, marker, symbol, clipPath, mask, style, title, desc, .actorPopupMenu')
    || getComputedStyle(el).display === 'none' || getComputedStyle(el).visibility === 'hidden';
  const clean = (s) => (s ?? '').replace(/\s+/g, ' ').trim();
  const has = (el, sel) => !!el.closest(sel);

  const textRole = (el) => {
    if (flavor === 'native') return null;
    if (has(el, '.sequenceNumber')) return 'seqnum';
    if (has(el, '.messageText')) return 'message';
    if (has(el, '.noteText')) return 'note';
    if (has(el, '[data-et="participant"]') || (el.closest('text')?.getAttribute('class') ?? '').split(/\s+/).includes('actor') || has(el, '.actor-man, .actor-top, .actor-bottom')) return 'actor';
    if (has(el, 'g.edgeLabel')) return 'edgeLabel';
    if (has(el, 'g.cluster, g.cluster-label')) return 'cluster';
    if (has(el, 'g.node')) return 'node';
    return null;
  };

  const texts = [];
  // SVG <text> elements (outermost only) and HTML labels inside <foreignObject>.
  for (const el of svg.querySelectorAll('text')) {
    if (hidden(el) || el.parentElement.closest('text')) continue;
    const text = clean(el.textContent);
    if (!text) continue;
    texts.push({ text, ...rectOf(el), role: textRole(el) });
  }
  for (const fo of svg.querySelectorAll('foreignObject')) {
    if (hidden(fo)) continue;
    const inner = fo.querySelector('span, p, div') ?? fo;
    const text = clean(fo.textContent);
    if (!text) continue;
    texts.push({ text, ...rectOf(inner), role: textRole(fo) });
  }

  const nodes = [];
  if (flavor === 'mermaid') {
    for (const g of svg.querySelectorAll('g.node')) {
      if (hidden(g)) continue;
      nodes.push({ label: clean(g.textContent), id: g.id, ...rectOf(g) });
    }
  }

  const sample = (el) => {
    let len = 0;
    try { len = el.getTotalLength(); } catch { return []; }
    const m = rootInv.multiply(el.getScreenCTM());
    const n = Math.max(2, Math.min(200, Math.ceil(len / 4)));
    const pts = [];
    for (let k = 0; k <= n; k++) {
      const p = el.getPointAtLength((len * k) / n).matrixTransform(m);
      pts.push([Math.round(p.x * 100) / 100, Math.round(p.y * 100) / 100]);
    }
    return pts;
  };

  const edges = [];
  const circles = [];
  if (flavor === 'mermaid') {
    for (const el of svg.querySelectorAll('path.flowchart-link, .edgePaths path, .edgePaths line')) {
      if (!hidden(el)) edges.push({ role: 'edge', points: sample(el) });
    }
    for (const el of svg.querySelectorAll('.messageLine0, .messageLine1')) {
      if (!hidden(el)) edges.push({ role: 'message', points: sample(el) });
    }
    for (const el of svg.querySelectorAll('[data-et="life-line"], line.actor-line')) {
      if (!hidden(el)) edges.push({ role: 'lifeline', points: sample(el) });
    }
  } else {
    const bbox = svg.getBBox();
    for (const el of svg.querySelectorAll('path, line, polyline')) {
      if (hidden(el)) continue;
      const cs = getComputedStyle(el);
      if (cs.stroke === 'none' || (el.tagName !== 'line' && cs.fill !== 'none')) continue;
      const points = sample(el);
      if (points.length < 2) continue;
      const vertical = el.tagName === 'line' && Math.abs(points[0][0] - points[points.length - 1][0]) < 0.5;
      const long = Math.abs(points[0][1] - points[points.length - 1][1]) > 0.4 * bbox.height;
      edges.push({ role: vertical && long ? 'lifeline' : 'edge', points });
    }
    for (const el of svg.querySelectorAll('circle, ellipse')) {
      if (hidden(el)) continue;
      const r = rectOf(el);
      if (Math.max(r.w, r.h) / 2 <= 14) circles.push({ cx: r.x + r.w / 2, cy: r.y + r.h / 2, r: Math.max(r.w, r.h) / 2 });
    }
  }
  const b = svg.getBBox();
  return { width: b.width, height: b.height, texts, nodes, edges, circles };
}

const SERVED_TYPES = { '.mjs': 'text/javascript', '.js': 'text/javascript', '.map': 'application/json', '.css': 'text/css' };

async function newPage(browser) {
  const page = await browser.newPage({ viewport: { width: 1600, height: 1000 }, deviceScaleFactor: 1 });
  await page.route(`${ORIGIN}/**`, (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === '/') return route.fulfill({ contentType: 'text/html', body: INDEX });
    for (const [prefix, dir] of Object.entries(roots)) {
      if (path.startsWith(prefix)) {
        const file = join(dir, path.slice(prefix.length));
        if (!file.startsWith(dir) || !existsSync(file)) return route.fulfill({ status: 404, body: 'not found' });
        return route.fulfill({ contentType: SERVED_TYPES[extname(file)] ?? 'application/octet-stream', body: readFileSync(file) });
      }
    }
    return route.fulfill({ status: 404, body: 'not found' });
  });
  page.setDefaultTimeout(timeoutMs);
  await page.goto(`${ORIGIN}/`);
  await page.waitForFunction(() => window.parityReady === true);
  return page;
}

const withTimeout = (p, ms, what) => Promise.race([p, new Promise((_, rej) => setTimeout(() => rej(new Error(`${what} timed out after ${ms} ms`)), ms))]);

async function renderRef(page, c) {
  const config = upstreamConfig(c.helper, c.options);
  return page.evaluate(async ([code, cfg]) => {
    const stage = document.getElementById('stage');
    stage.innerHTML = '';
    document.querySelectorAll('body > [id^="d"]').forEach((n) => n.remove());
    try {
      window.mermaid.initialize({ startOnLoad: false, ...cfg });
      const { svg } = await window.mermaid.render(`parity${Date.now()}`, code);
      stage.innerHTML = `<div id="ref">${svg}</div>`;
      const el = stage.querySelector('svg');
      const vb = el.viewBox.baseVal;
      if (vb && vb.width) { el.setAttribute('width', vb.width); el.setAttribute('height', vb.height); el.style.maxWidth = 'none'; }
      return { ok: true, svg };
    } catch (e) {
      return { ok: false, error: String(e?.message ?? e).slice(0, 500) };
    }
  }, [c.source, config]);
}

async function showNative(page, svgText) {
  await page.evaluate((s) => {
    const stage = document.getElementById('stage');
    stage.innerHTML = `<div id="native">${s}</div>`;
  }, svgText);
}

async function main() {
  const cases = loadCorpus(opt.corpus, opt.only);
  const browser = await chromium.launch();
  let page = await newPage(browser);
  const results = {};
  for (let i = 0; i < cases.length; i++) {
    const c = cases[i];
    const id = `${c.family}/${c.case}`;
    const dir = join(opt.out, 'cases', c.family, c.case);
    mkdirSync(dir, { recursive: true });
    const res = { ref: 'error', native: 'missing' };
    const geometry = { ref: null, native: null };
    try {
      const r = await withTimeout(renderRef(page, c), timeoutMs, 'mermaid.js render');
      if (r.ok) {
        writeFileSync(join(dir, 'ref.svg'), r.svg);
        geometry.ref = await page.evaluate(pageExtract, ['ref', 'mermaid']);
        await page.locator('#ref svg').screenshot({ path: join(dir, 'ref.png') });
        res.ref = 'ok';
      } else res.refError = r.error;
    } catch (e) {
      res.refError = String(e?.message ?? e).slice(0, 300);
      await page.close().catch(() => {});
      page = await newPage(browser);
    }
    const nativeSvg = join(dir, 'native.svg');
    if (existsSync(nativeSvg)) {
      try {
        await showNative(page, readFileSync(nativeSvg, 'utf8'));
        geometry.native = await page.evaluate(pageExtract, ['native', 'native']);
        await page.locator('#native svg').screenshot({ path: join(dir, 'native.png') });
        res.native = 'ok';
      } catch (e) {
        res.native = 'error';
        res.nativeBrowserError = String(e?.message ?? e).slice(0, 300);
        await page.close().catch(() => {});
        page = await newPage(browser);
      }
    }
    writeFileSync(join(dir, 'geometry.json'), JSON.stringify(geometry));
    results[id] = res;
    if ((i + 1) % 25 === 0) console.error(`browser: ${i + 1}/${cases.length}`);
  }
  const chromiumVersion = browser.version();
  await browser.close();
  writeFileSync(join(opt.out, 'browser.json'), JSON.stringify({ mermaid_version: mermaidPkg.version, chromium: chromiumVersion, cases: results }, null, 1));
  const tally = {};
  for (const r of Object.values(results)) tally[`ref-${r.ref}`] = (tally[`ref-${r.ref}`] ?? 0) + 1;
  console.log(JSON.stringify({ browser: tally, mermaid: mermaidPkg.version }));
}

await main();
