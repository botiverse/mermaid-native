#!/usr/bin/env node
// Render every corpus case with mermaid-native's production Kotlin/Wasm module
// (the artifact built by `./gradlew :mermaid-web:wasmJsBrowserProductionWebpack`,
// same as scripts/verify-usecase-production-wasm.mjs) and record per-case status:
//   ok | parse-error (typed diagnostics) | render-error (layout/SVG threw) | timeout
//
// Usage: node native-render.mjs --module <optimized .mjs> [--corpus dir] [--out dir] [--timeout ms]
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
import { Worker, isMainThread, parentPort, workerData } from 'node:worker_threads';
import { loadCorpus, parseArgs, repoRoot } from './common.mjs';

if (!isMainThread) {
  const engine = await import(workerData.moduleUrl);
  parentPort.on('message', ({ i, source }) => {
    const t0 = performance.now();
    let result;
    try {
      const r = JSON.parse(engine.renderMermaidResultJson(source));
      result = r.ok
        ? { status: 'ok', svg: r.svg }
        : { status: 'parse-error', diagnostics: r.diagnostics };
    } catch (e) {
      result = { status: 'render-error', error: String(e?.message ?? e).slice(0, 500) };
    }
    result.ms = Math.round(performance.now() - t0);
    parentPort.postMessage({ i, result });
  });
  parentPort.postMessage({ ready: true });
} else {
  const opt = parseArgs(process.argv, {
    module: join(repoRoot, 'mermaid-web/build/compileSync/wasmJs/main/productionExecutable/optimized/mermaid-native-mermaid-web-wasm-js.mjs'),
    corpus: join(repoRoot, 'compatibility/render-parity'),
    out: join(repoRoot, 'build/render-parity'),
    timeout: '20000',
    only: '',
  });
  const cases = loadCorpus(opt.corpus, opt.only);
  const moduleUrl = pathToFileURL(resolve(opt.module)).href;
  const timeoutMs = Number(opt.timeout);
  const results = {};

  const startWorker = () => new Promise((res, rej) => {
    const w = new Worker(new URL(import.meta.url), { workerData: { moduleUrl } });
    w.once('message', (m) => (m.ready ? res(w) : rej(new Error('unexpected worker message'))));
    w.once('error', rej);
  });

  let worker = await startWorker();
  for (let i = 0; i < cases.length; i++) {
    const c = cases[i];
    const result = await new Promise((res) => {
      const timer = setTimeout(() => res({ status: 'timeout', error: `no result within ${timeoutMs} ms` }), timeoutMs);
      const onMsg = (m) => { if (m.i === i) { clearTimeout(timer); worker.off('error', onErr); res(m.result); } };
      const onErr = (e) => { clearTimeout(timer); worker.off('message', onMsg); res({ status: 'render-error', error: `worker crashed: ${e?.message ?? e}` }); };
      worker.once('message', onMsg);
      worker.once('error', onErr);
      worker.postMessage({ i, source: c.source });
    });
    if (result.status === 'timeout' || /worker crashed/.test(result.error ?? '')) {
      await worker.terminate().catch(() => {});
      worker = await startWorker();
    }
    const dir = join(opt.out, 'cases', c.family, c.case);
    mkdirSync(dir, { recursive: true });
    if (result.svg) writeFileSync(join(dir, 'native.svg'), result.svg);
    const { svg, ...meta } = result;
    results[`${c.family}/${c.case}`] = meta;
    if ((i + 1) % 50 === 0) console.error(`native: ${i + 1}/${cases.length}`);
  }
  await worker.terminate();
  mkdirSync(opt.out, { recursive: true });
  writeFileSync(join(opt.out, 'native.json'), JSON.stringify(results, null, 1));
  const tally = {};
  for (const r of Object.values(results)) tally[r.status] = (tally[r.status] ?? 0) + 1;
  console.log(JSON.stringify({ native: tally, module: opt.module }));
}
