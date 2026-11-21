#!/usr/bin/env node
// Combine native + browser results into metrics, classes, summary.json,
// summary.md and an HTML report; compare with the committed baseline.
//
// Usage: node report.mjs [--out dir] [--baseline file] [--fail-on-regression]
//                        [--write-baseline file] [--worst N]
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { loadCorpus, parseArgs, readJson, repoRoot } from './common.mjs';
import { parseManifest } from './extract-corpus.mjs';
import { THRESHOLDS, baselineFrom, classify, compareWithBaseline, flowchartMetrics, sequenceMetrics } from './metrics.mjs';

const opt = parseArgs(process.argv, {
  corpus: join(repoRoot, 'compatibility/render-parity'),
  out: join(repoRoot, 'build/render-parity'),
  baseline: join(repoRoot, 'compatibility/render-parity/baseline.json'),
  'fail-on-regression': false,
  'write-baseline': '',
  worst: '10',
});

const CLASSES = ['match', 'differs', 'native-fails', 'ref-fails'];

export function buildSummary({ cases, nativeResults, browser, geometryOf, skipped }) {
  const rows = [];
  for (const c of cases) {
    const id = `${c.family}/${c.case}`;
    const n = nativeResults[id] ?? { status: 'missing' };
    const b = browser.cases?.[id] ?? { ref: 'error', refError: 'not rendered' };
    const status = {
      ref: b.ref === 'ok' ? 'ok' : 'error',
      native: n.status === 'ok' ? (b.native === 'ok' ? 'ok' : 'render-error') : n.status,
    };
    if (b.refError) status.refError = b.refError;
    if (n.status === 'parse-error') status.nativeError = (n.diagnostics ?? []).map((d) => `${d.code}: ${d.message}`).join('; ').slice(0, 300);
    else if (n.error) status.nativeError = n.error;
    else if (b.nativeBrowserError) status.nativeError = `browser: ${b.nativeBrowserError}`;
    let metrics = null;
    let metricsError = null;
    if (status.ref === 'ok' && status.native === 'ok') {
      const g = geometryOf(c);
      try {
        metrics = c.family === 'flowchart' ? flowchartMetrics(g.ref, g.native, c.source) : sequenceMetrics(g.ref, g.native);
      } catch (e) { metricsError = String(e?.message ?? e); }
    }
    const cls = metricsError
      ? { class: 'differs', reasons: [`metrics error: ${metricsError}`], score: 50 }
      : classify(c.family, status, metrics ?? {});
    rows.push({
      family: c.family, case: c.case, upstream_path: c.upstream_path, upstream_index: c.upstream_index,
      upstream_case: c.upstream_case, sha256: c.sha256, options_json: c.options_json,
      nativeMs: n.ms ?? null, status, ...cls, metrics,
    });
  }
  const counts = {};
  for (const r of rows) {
    const f = (counts[r.family] ??= { extracted: 0, skipped: 0, ...Object.fromEntries(CLASSES.map((k) => [k, 0])), native: {} });
    f.extracted++;
    f[r.class]++;
    f.native[r.status.native] = (f.native[r.status.native] ?? 0) + 1;
  }
  for (const s of skipped) (counts[s.family] ??= { extracted: 0, skipped: 0, ...Object.fromEntries(CLASSES.map((k) => [k, 0])), native: {} }).skipped++;
  return { rows, counts };
}

const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (ch) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch]);

function metricCells(r) {
  const m = r.metrics;
  if (!m) return '';
  const pick = r.family === 'flowchart'
    ? ['direction', 'matchedNodes', 'refNodes', 'labelCoverage', 'rankTau', 'crossAxisTau', 'refCrossings', 'nativeCrossings', 'refOverlaps', 'nativeOverlaps', 'aspectRatioRatio', 'areaRatio']
    : ['refParticipants', 'participantCoverage', 'participantOrderEqual', 'matchedMessages', 'refMessages', 'messageOrderEqual', 'messageTau', 'refSelfMessages', 'nativeSelfMessages', 'refAutonumber', 'nativeAutonumber', 'refOverlaps', 'nativeOverlaps', 'aspectRatioRatio', 'areaRatio'];
  return pick.map((k) => `<span class="m"><b>${k}</b> ${esc(m[k])}</span>`).join(' ');
}

export function renderHtml(summary, sources) {
  const rows = summary.cases.map((r) => {
    const base = `cases/${r.family}/${r.case}`;
    return `<tr data-class="${r.class}" data-family="${r.family}" data-regression="${r.regression ? 1 : 0}">
<td><div class="id">${esc(r.family)}/${esc(r.case)}</div><div class="t">${esc(r.upstream_case)}</div>
<div class="up">${esc(r.upstream_path)}#${esc(r.upstream_index)} · options <code>${esc(r.options_json)}</code></div>
<div><span class="cls ${r.class}">${r.class}</span>${r.regression ? ` <span class="cls reg">regression (was ${esc(r.regression.from)})</span>` : ''} score ${r.score}</div>
<ul>${r.reasons.map((x) => `<li>${esc(x)}</li>`).join('')}</ul><div class="metrics">${metricCells(r)}</div>
<details><summary>source</summary><pre>${esc(sources.get(`${r.family}/${r.case}`))}</pre></details></td>
<td>${r.status.ref === 'ok' ? `<a href="${base}/ref.svg"><img loading="lazy" src="${base}/ref.png" alt="mermaid.js"></a>` : `<pre class="err">${esc(r.status.refError)}</pre>`}</td>
<td>${r.status.native === 'ok' ? `<a href="${base}/native.svg"><img loading="lazy" src="${base}/native.png" alt="native"></a>` : `<pre class="err">${esc(r.status.native)}: ${esc(r.status.nativeError)}</pre>`}</td></tr>`;
  }).join('\n');
  const countRows = Object.entries(summary.counts).map(([f, c]) => `<tr><td>${f}</td><td>${c.extracted}</td><td>${c.skipped}</td>${CLASSES.map((k) => `<td>${c[k]}</td>`).join('')}</tr>`).join('');
  return `<!doctype html><html><head><meta charset="utf-8"><title>mermaid-native render parity</title>
<style>
body{font:13px/1.4 system-ui,sans-serif;margin:16px;color:#1a1a1a}
table{border-collapse:collapse;width:100%}td,th{border:1px solid #ddd;padding:6px;vertical-align:top;text-align:left}
#cases td:nth-child(1){width:28%}#cases td img{max-width:100%;max-height:520px;background:#fff}
.id{font-weight:600}.t{color:#444}.up{color:#777;font-size:11px;word-break:break-all}
.cls{display:inline-block;padding:1px 6px;border-radius:3px;font-weight:600}
.match{background:#d9f2d9}.differs{background:#fde9c8}.native-fails{background:#f8d0d0}.ref-fails{background:#e2e2e2}.reg{background:#c00;color:#fff}
.m{display:inline-block;margin-right:6px;font-size:11px;color:#333}.err{white-space:pre-wrap;color:#a00;max-width:600px}
.filters button{margin-right:6px}.filters button.on{background:#333;color:#fff}pre{white-space:pre-wrap}
</style></head><body>
<h1>mermaid-native vs mermaid.js render parity</h1>
<p>Upstream <code>${esc(summary.upstream_commit)}</code> · mermaid ${esc(summary.mermaid_version)} · Chromium ${esc(summary.chromium)} · native ${esc(summary.native_revision)}</p>
<table><tr><th>family</th><th>extracted</th><th>skipped</th>${CLASSES.map((k) => `<th>${k}</th>`).join('')}</tr>${countRows}</table>
<p>Baseline: ${summary.baseline ? `${summary.baseline.regressions.length} regressions, ${summary.baseline.improvements.length} improvements, ${summary.baseline.added.length} new` : 'none'}</p>
<div class="filters">Class: ${['all', ...CLASSES, 'regression'].map((k) => `<button data-f="${k}">${k}</button>`).join('')}
Family: ${['all', ...Object.keys(summary.counts)].map((k) => `<button data-fam="${k}">${k}</button>`).join('')}
<label><input type="checkbox" id="worst"> sort by score</label></div>
<table id="cases"><thead><tr><th>case</th><th>mermaid.js</th><th>mermaid-native</th></tr></thead><tbody>
${rows}
</tbody></table>
<script>
let cls='all', fam='all';
const tbody=document.querySelector('#cases tbody'); const original=[...tbody.rows];
function apply(){for(const r of original){const okC=cls==='all'||(cls==='regression'?r.dataset.regression==='1':r.dataset.class===cls);const okF=fam==='all'||r.dataset.family===fam;r.style.display=okC&&okF?'':'none';}
document.querySelectorAll('[data-f]').forEach(b=>b.classList.toggle('on',b.dataset.f===cls));document.querySelectorAll('[data-fam]').forEach(b=>b.classList.toggle('on',b.dataset.fam===fam));}
document.querySelectorAll('[data-f]').forEach(b=>b.onclick=()=>{cls=b.dataset.f;apply();});
document.querySelectorAll('[data-fam]').forEach(b=>b.onclick=()=>{fam=b.dataset.fam;apply();});
document.getElementById('worst').onchange=(e)=>{const rows=[...original];if(e.target.checked)rows.sort((a,b)=>parseFloat(b.innerText.match(/score ([\\d.]+)/)?.[1]??0)-parseFloat(a.innerText.match(/score ([\\d.]+)/)?.[1]??0));rows.forEach(r=>tbody.appendChild(r));};
apply();
</script></body></html>`;
}

export function renderMarkdown(summary, worstN) {
  const L = ['# Render parity: mermaid-native vs mermaid.js', '',
    `Upstream \`${summary.upstream_commit}\`, mermaid ${summary.mermaid_version}, Chromium ${summary.chromium}, native ${summary.native_revision}.`, '',
    `| family | extracted | skipped | ${CLASSES.join(' | ')} |`, `| --- | --- | --- | ${CLASSES.map(() => '---').join(' | ')} |`];
  for (const [f, c] of Object.entries(summary.counts)) L.push(`| ${f} | ${c.extracted} | ${c.skipped} | ${CLASSES.map((k) => c[k]).join(' | ')} |`);
  L.push('');
  const nativeStatus = Object.entries(summary.counts).map(([f, c]) => `${f}: ${Object.entries(c.native).map(([k, v]) => `${k} ${v}`).join(', ')}`).join('; ');
  L.push(`Native status — ${nativeStatus}.`, '');
  const b = summary.baseline;
  if (b) {
    L.push(`## Baseline comparison`, '', `${b.regressions.length} regression(s), ${b.improvements.length} improvement(s), ${b.added.length} new case(s), ${b.removed.length} removed.`, '');
    for (const r of b.regressions) L.push(`- **${r.case}**: ${r.from} → ${r.to} — ${r.reasons.join('; ')}`);
    if (b.improvements.length) L.push('', '<details><summary>Improvements</summary>', '', ...b.improvements.map((r) => `- ${r.case}: ${r.from} → ${r.to}`), '', '</details>');
    L.push('');
  } else L.push('No baseline found; nothing compared.', '');
  const table = (title, items) => {
    L.push(`## ${title}`, '', '| case | class | score | reasons |', '| --- | --- | --- | --- |');
    for (const r of items) L.push(`| ${r.family}/${r.case} | ${r.class} | ${r.score} | ${r.reasons.join('; ').replace(/\|/g, '\\|').replace(/\s+/g, ' ').slice(0, 300)} |`);
    L.push('');
  };
  const byScore = (a, b2) => b2.score - a.score || `${a.family}/${a.case}`.localeCompare(`${b2.family}/${b2.case}`);
  table(`Worst ${worstN} differences`, summary.cases.filter((r) => r.class === 'differs').sort(byScore).slice(0, worstN));
  const fails = summary.cases.filter((r) => r.class === 'native-fails').sort(byScore);
  table(`Native failures (${fails.length}; first ${Math.min(fails.length, worstN * 3)})`, fails.slice(0, worstN * 3));
  L.push('');
  return L.join('\n');
}

function main() {
  const cases = loadCorpus(opt.corpus);
  const nativeResults = readJson(join(opt.out, 'native.json'), {});
  const browser = readJson(join(opt.out, 'browser.json'), { cases: {} });
  const skipped = existsSync(join(opt.corpus, 'skipped.tsv')) ? parseManifest(readFileSync(join(opt.corpus, 'skipped.tsv'), 'utf8')) : [];
  const geometryOf = (c) => readJson(join(opt.out, 'cases', c.family, c.case, 'geometry.json'));
  const { rows, counts } = buildSummary({ cases, nativeResults, browser, geometryOf, skipped });
  const summary = {
    upstream_commit: cases[0]?.upstream_commit ?? '',
    mermaid_version: browser.mermaid_version ?? '',
    chromium: browser.chromium ?? '',
    native_revision: process.env.GITHUB_SHA ?? 'local',
    thresholds: THRESHOLDS,
    counts,
    cases: rows,
  };
  const baseline = existsSync(opt.baseline) ? readJson(opt.baseline) : null;
  if (baseline) {
    summary.baseline = compareWithBaseline(baseline, rows);
    const reg = new Map(summary.baseline.regressions.map((r) => [r.case, r]));
    for (const r of rows) if (reg.has(`${r.family}/${r.case}`)) r.regression = { from: reg.get(`${r.family}/${r.case}`).from };
  }
  writeFileSync(join(opt.out, 'summary.json'), JSON.stringify(summary, null, 1));
  writeFileSync(join(opt.out, 'summary.md'), renderMarkdown(summary, Number(opt.worst)));
  writeFileSync(join(opt.out, 'index.html'), renderHtml(summary, new Map(cases.map((c) => [`${c.family}/${c.case}`, c.source]))));
  if (opt['write-baseline']) writeFileSync(opt['write-baseline'], JSON.stringify(baselineFrom(summary), null, 1) + '\n');
  const regressions = summary.baseline?.regressions.length ?? 0;
  console.log(JSON.stringify({ counts, regressions, improvements: summary.baseline?.improvements.length ?? 0 }));
  if (opt['fail-on-regression'] && regressions > 0) {
    console.error(`::error::${regressions} render-parity regression(s) against baseline; see summary.md`);
    process.exit(1);
  }
}

if (process.argv[1] && process.argv[1].endsWith('report.mjs')) main();
