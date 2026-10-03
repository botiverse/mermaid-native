// Exercise the optimized production artifact: development Wasm tests do not
// cover production lowering. Run after :mermaid-web:wasmJsBrowserDistribution.
import assert from 'node:assert/strict';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
const modulePath = process.argv[2] ?? 'mermaid-web/build/compileSync/wasmJs/main/productionExecutable/optimized/mermaid-native-mermaid-web-wasm-js.mjs';
const engine = await import(pathToFileURL(resolve(modulePath)).href);
let rejected = 0;
for (const operator of ['--o', '--x', 'o--', 'x--']) {
  const source = `usecase-beta\njson Payload@{}\nInspect ${operator} Payload`;
  const expected = /JSON relationship 'Inspect' to 'Payload' permits only point, reversed-point, or markerless solid association at line 3, column 9 \[37,48\)/;
  assert.throws(() => engine.renderMermaidSvg(source), expected, operator);
  for (const call of [engine.renderMermaidResultJson, engine.renderMermaidCanvasJson]) {
    const result = JSON.parse(call(source));
    assert.equal(result.ok, false, operator);
    assert.match(result.diagnostics[0].message, expected);
  }
  rejected++;
}
let accepted = 0;
for (const operator of ['-->', '<--', '--']) {
  const source = `usecase-beta\njson Payload@{}\nInspect ${operator} Payload`;
  assert.match(engine.renderMermaidSvg(source), /^<svg\b/, operator);
  accepted++;
}
assert.throws(() => engine.renderMermaidSvg('usecase-beta\nactor User\nLogin\nUser --|> Login'), /Generalization requires actor-to-actor/);
assert.throws(() => engine.renderMermaidSvg('usecase-beta\nA link@--> B\nlink@{ animate: fast }'), /Metadata property 'animate' is invalid for edge 'link'/);
assert.match(engine.renderMermaidSvg('usecase-beta\nA link@--> B\nlink@{ animation: slow }'), /^<svg\b/);
console.log(JSON.stringify({productionModule: modulePath, rejectedJsonMarkers: rejected, acceptedJsonMarkers: accepted, mixedKindRejected: true, typedAnimationRejected: true, validRecovery: true}));
