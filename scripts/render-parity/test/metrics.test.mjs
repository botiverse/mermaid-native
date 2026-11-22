import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  canvasOverflow, caseKey, classify, compareWithBaseline, countCrossings, countSelfLoops, flowDirection, flowchartMetrics,
  hasAutonumber, kendallTauB, labelOverlaps, matchLabels, mergeStackedTexts, segmentHitsBox, sequenceMetrics,
} from '../metrics.mjs';
import { dedent, diffManifests, extractFromSpec, slugify, stableJson } from '../extract-corpus.mjs';

const box = (text, x, y, w = 40, h = 16, role = null) => ({ text, x, y, w, h, role });

test('kendall tau-b: identical, reversed, ties', () => {
  assert.equal(kendallTauB([1, 2, 3, 4], [10, 20, 30, 40]), 1);
  assert.equal(kendallTauB([1, 2, 3, 4], [4, 3, 2, 1]), -1);
  assert.equal(kendallTauB([1], [1]), null);
  assert.equal(kendallTauB([1, 1, 1], [1, 2, 3]), null);
  const t = kendallTauB([0, 0, 100, 200], [0, 50, 100, 200], 3);
  assert.ok(t > 0.8 && t < 1, `tau ${t}`);
  // Tolerance turns near-equal coordinates into ties.
  assert.equal(kendallTauB([0, 1, 100], [0, 50, 100], 3), kendallTauB([0, 0, 100], [0, 50, 100], 0));
});

test('crossings ignore shared endpoints and count each X once', () => {
  const x1 = [[0, 0], [100, 100]];
  const x2 = [[0, 100], [100, 0]];
  assert.equal(countCrossings([x1, x2]), 1);
  // Two edges leaving the same node.
  assert.equal(countCrossings([[[0, 0], [100, 0]], [[0, 0], [0, 100]]]), 0);
  // Densely sampled curves crossing once.
  const sampled = (f) => Array.from({ length: 51 }, (_, i) => [i * 2, f(i * 2)]);
  assert.equal(countCrossings([{ points: sampled((x) => x) }, { points: sampled((x) => 100 - x) }]), 1);
  assert.equal(countCrossings([x1]), 0);
});

test('label overlaps: text/text and text/edge', () => {
  const a = box('A', 0, 0, 50, 20);
  const b = box('B', 10, 5, 50, 20);
  const c = box('C', 200, 0, 50, 20);
  assert.equal(labelOverlaps([a, b, c], []).textText, 1);
  const edge = { points: [[225, -10], [225, 40]] };
  assert.equal(labelOverlaps([c], [edge], [c]).textEdge, 1);
  assert.equal(labelOverlaps([a], [edge], [a]).textEdge, 0);
  assert.ok(segmentHitsBox([0, 5], [100, 5], { x: 10, y: 0, w: 10, h: 10 }));
  assert.ok(!segmentHitsBox([0, 50], [100, 50], { x: 10, y: 0, w: 10, h: 10 }));
});

test('stacked lines merge into one label block', () => {
  const merged = mergeStackedTexts([box('John', 100, 10, 40, 16), box('Second Line', 80, 28, 80, 16), box('Other', 400, 10)]);
  assert.equal(merged.length, 2);
  assert.equal(merged[0].text, 'John Second Line');
});

test('label matching normalises text and drops ambiguous keys when unique', () => {
  const ref = [box('Go shopping', 0, 0), box('X', 0, 50), box('X', 0, 90)];
  const nat = [box('go  shopping', 5, 5), box('X', 0, 40)];
  const m = matchLabels(ref, nat, { unique: true });
  assert.equal(m.pairs.length, 1);
  assert.equal(m.considered, 1);
  const all = matchLabels(ref, nat);
  assert.equal(all.pairs.length, 2);
  assert.equal(all.coverage, 2 / 3);
});

test('flow direction from header', () => {
  assert.equal(flowDirection('flowchart LR\nA-->B'), 'LR');
  assert.equal(flowDirection('---\ntitle: x\n---\ngraph TD\nA'), 'TB');
  assert.equal(flowDirection('%% c\nflowchart-elk RL\n'), 'RL');
  assert.equal(flowDirection('flowchart\nA-->B'), 'TB');
});

function chain(labels, step, vertical = true) {
  const nodes = labels.map((l, i) => ({ label: l, x: vertical ? 0 : i * step, y: vertical ? i * step : 0, w: 40, h: 20 }));
  const texts = nodes.map((n) => ({ text: n.label, x: n.x + 5, y: n.y + 2, w: 30, h: 16, role: 'node' }));
  const edges = nodes.slice(1).map((n, i) => ({ role: 'edge', points: [[nodes[i].x + 20, nodes[i].y + 20], [n.x + 20, n.y]] }));
  const width = vertical ? 40 : labels.length * step;
  const height = vertical ? labels.length * step : 20;
  return { width, height, texts, nodes, edges, circles: [] };
}

test('flowchart metrics and classification: same layout matches, reversed differs', () => {
  const ref = chain(['A', 'B', 'C', 'D'], 60);
  const nat = { ...chain(['A', 'B', 'C', 'D'], 70), nodes: [] };
  const m = flowchartMetrics(ref, nat, 'flowchart TD\nA-->B-->C-->D');
  assert.equal(m.rankTau, 1);
  assert.equal(m.labelCoverage, 1);
  assert.equal(m.nativeCrossings, 0);
  assert.equal(classify('flowchart', { ref: 'ok', native: 'ok' }, m).class, 'match');

  const rev = { ...chain(['D', 'C', 'B', 'A'], 60), nodes: [] };
  const m2 = flowchartMetrics(ref, rev, 'flowchart TD\nA-->B-->C-->D');
  assert.equal(m2.rankTau, -1);
  const c2 = classify('flowchart', { ref: 'ok', native: 'ok' }, m2);
  assert.equal(c2.class, 'differs');
  assert.match(c2.reasons.join(), /rank order/);

  const wide = { ...chain(['A', 'B', 'C', 'D'], 60, false), nodes: [] };
  const m3 = flowchartMetrics(ref, wide, 'flowchart TD\nA-->B-->C-->D');
  assert.match(classify('flowchart', { ref: 'ok', native: 'ok' }, m3).reasons.join(), /aspect ratio/);
});

test('classification of failures', () => {
  assert.equal(classify('flowchart', { ref: 'error', native: 'ok' }, {}).class, 'ref-fails');
  const f = classify('sequence', { ref: 'ok', native: 'parse-error', nativeError: 'UNSUPPORTED' }, {});
  assert.equal(f.class, 'native-fails');
  assert.equal(f.score, 100);
});

function seq({ actors, messages, selfLoop = false, autonumber = false, native = false }) {
  const texts = [];
  actors.forEach((a, i) => {
    texts.push(box(a, 100 * i, 0, 40, 16, native ? null : 'actor'));
    texts.push(box(a, 100 * i, 400, 40, 16, native ? null : 'actor'));
  });
  messages.forEach((m, i) => texts.push(box(m, 30, 50 + 40 * i, 60, 14, native ? null : 'message')));
  const edges = messages.map((_, i) => ({ role: native ? 'edge' : 'message', points: [[20, 70 + 40 * i], [120, 70 + 40 * i]] }));
  if (selfLoop) edges.push({ role: native ? 'edge' : 'message', points: [[120, 300], [160, 305], [160, 315], [120, 320]] });
  const circles = [];
  if (autonumber) {
    if (native) { texts.push(box('1', 15, 64, 10, 12)); circles.push({ cx: 20, cy: 70, r: 9 }); }
    else texts.push(box('1', 15, 64, 10, 12, 'seqnum'));
  }
  return { width: 100 * actors.length, height: 420, texts, nodes: [], edges, circles };
}

test('sequence metrics: order, self messages, autonumber', () => {
  const ref = seq({ actors: ['Alice', 'Bob'], messages: ['Hello', 'Hi back'], selfLoop: true, autonumber: true });
  const same = seq({ actors: ['Alice', 'Bob'], messages: ['Hello', 'Hi back'], selfLoop: true, autonumber: true, native: true });
  const m = sequenceMetrics(ref, same);
  assert.equal(m.participantOrderEqual, true);
  assert.equal(m.messageOrderEqual, true);
  assert.equal(m.refSelfMessages, 1);
  assert.equal(m.nativeSelfMessages, 1);
  assert.equal(m.nativeAutonumber, true);
  assert.equal(classify('sequence', { ref: 'ok', native: 'ok' }, m).class, 'match');

  const swapped = seq({ actors: ['Bob', 'Alice'], messages: ['Hi back', 'Hello'], native: true });
  const m2 = sequenceMetrics(ref, swapped);
  assert.equal(m2.participantOrderEqual, false);
  assert.equal(m2.messageOrderEqual, false);
  assert.equal(m2.nativeSelfMessages, 0);
  assert.equal(m2.nativeAutonumber, false);
  const c2 = classify('sequence', { ref: 'ok', native: 'ok' }, m2);
  assert.equal(c2.class, 'differs');
  assert.ok(c2.reasons.length >= 4, c2.reasons.join());
  assert.equal(countSelfLoops([{ points: [[0, 0], [0, 100]] }]), 0);
  assert.equal(hasAutonumber({ texts: [], circles: [] }), false);
});

test('baseline comparison flags only worsening transitions', () => {
  const row = (c, cls, sha) => ({ family: 'flowchart', case: c, sha256: sha, options_json: '{}', class: cls, reasons: [], status: { native: 'ok' } });
  const baseline = { cases: {
    [caseKey(row('a', 'match', '1'))]: { class: 'match' },
    [caseKey(row('b', 'differs', '2'))]: { class: 'differs' },
    [caseKey(row('c', 'differs', '3'))]: { class: 'differs' },
    [caseKey(row('d', 'ref-fails', '4'))]: { class: 'ref-fails' },
  } };
  const now = [row('a2', 'differs', '1'), row('b', 'native-fails', '2'), row('c', 'match', '3'), row('d', 'native-fails', '4'), row('e', 'differs', '5')];
  const r = compareWithBaseline(baseline, now);
  assert.deepEqual(r.regressions.map((x) => x.case), ['flowchart/a2', 'flowchart/b']);
  assert.deepEqual(r.improvements.map((x) => x.case), ['flowchart/c']);
  assert.deepEqual(r.added.map((x) => x.case), ['flowchart/e']);
});

test('corpus extraction: literals, options, interpolation and arrays', () => {
  const spec = `
import { imgSnapshotTest, renderGraph } from '../../helpers/util.ts';
const shared = { flowchart: { htmlLabels: false } };
describe('Suite', () => {
  it('1: simple', () => {
    imgSnapshotTest(\`flowchart TD
      A --> B
    \`, { theme: 'forest', n: -1 });
  });
  it('2: interpolated', () => {
    const x = 'B';
    imgSnapshotTest(\`flowchart TD
      A --> \${x}\`);
  });
  it('3: const and shared options', () => {
    const code = 'graph LR\\nA-->B';
    renderGraph(code, shared);
  });
  ['a', 'b'].forEach((look) => {
    it('4: loop ' + look, () => { imgSnapshotTest('graph TD\\nA', { look }); });
  });
  it('5: array', () => { renderGraph(['graph TD\\nA', \`graph TD\\nB\${1}\`]); });
});`;
  const { cases, skipped } = extractFromSpec(spec, 'x/flowchart.spec.js');
  assert.deepEqual(cases.map((c) => c.index), ['0', '2', '4.0']);
  assert.equal(cases[0].source, 'flowchart TD\nA --> B\n');
  assert.deepEqual(cases[0].options, { theme: 'forest', n: -1 });
  assert.equal(cases[0].title, 'Suite > 1: simple');
  assert.deepEqual(cases[1].options, { flowchart: { htmlLabels: false } });
  assert.equal(cases[1].helper, 'renderGraph');
  assert.deepEqual(skipped.map((s) => [s.index, s.reason]), [
    ['1', 'template interpolation'], ['3', 'non-literal options (Identifier)'], ['4.1', 'template interpolation'],
  ]);
});

test('dedent, slugify, stableJson, manifest diff', () => {
  assert.equal(dedent('\n    sequenceDiagram\n      A->>B: hi\n  '), 'sequenceDiagram\n  A->>B: hi\n');
  assert.equal(dedent('graph TD\n      A --> B\n      B --> C'), 'graph TD\nA --> B\nB --> C\n');
  assert.equal(slugify('1: Should render <b>HTML</b>!'), '1-should-render-b-html-b');
  assert.equal(stableJson({ b: 1, a: { d: 2, c: [1, { f: 1, e: 2 }] } }), '{"a":{"c":[1,{"e":2,"f":1}],"d":2},"b":1}');
  const row = (i, sha, c = `00${i}`) => ({ family: 'f', case: c, upstream_path: 'p', upstream_index: String(i), sha256: sha, options_json: '{}' });
  const d = diffManifests([row(0, 'a'), row(1, 'b'), row(2, 'c')], [row(0, 'a'), row(1, 'B'), row(3, 'd'), row(2, 'c', 'x')]);
  assert.equal(d.unchanged.length, 1);
  assert.equal(d.changed.length, 1);
  assert.equal(d.added.length, 1);
  assert.equal(d.renamed.length, 1);
  assert.equal(d.removed.length, 0);
});

test('styled runs on one baseline merge before stacked lines', () => {
  const merged = mergeStackedTexts([box('The', 100, 10, 30, 16), box('cat', 132, 10, 26, 16), box('in the hat', 160, 10, 70, 16)]);
  assert.equal(merged.length, 1);
  assert.equal(merged[0].text, 'The cat in the hat');
  const lines = mergeStackedTexts([box('The dog in', 100, 10, 80, 16), box('the', 110, 28, 30, 16), box('hog', 142, 28, 28, 16), box('far', 500, 300)]);
  assert.deepEqual(lines.map((t) => t.text), ['The dog in the hog', 'far']);
});

test('self-message flag and dotted autonumber badges', () => {
  assert.equal(countSelfLoops([{ role: 'message', self: true, points: [[86, 111], [76, 131]] }, { role: 'message', self: false, points: [[0, 0], [0, 10], [20, 5]] }]), 1);
  assert.equal(hasAutonumber({ texts: [box('10.1', 60, 98, 20, 12)], circles: [{ cx: 68, cy: 104, r: 9, rx: 16.2, ry: 9 }] }), true);
});

test('content past the viewBox counts as clipped', () => {
  const canvas = { x: 0, y: 0, w: 200, h: 100 };
  assert.equal(canvasOverflow({ x: 24, y: 24, width: 152, height: 52, canvas }), 0);
  assert.equal(canvasOverflow({ x: 24, y: 24, width: 226, height: 52, canvas }), 50);
  assert.equal(canvasOverflow({ x: -10, y: 24, width: 52, height: 52, canvas }), 10);
  assert.equal(canvasOverflow({ x: 24, y: 24, width: 52, height: 90, canvas }), 14);
  assert.equal(canvasOverflow({ x: 24, y: -7, width: 52, height: 52, canvas }), 7);
  assert.equal(canvasOverflow({ x: 5, y: 5, width: 10, height: 10, canvas: { x: 10, y: 5, w: 10, h: 10 } }), 5);
  assert.equal(canvasOverflow({ width: 999, height: 999, canvas: null }), 0);

  for (const [family, metrics] of [['flowchart', flowchartMetrics], ['sequence', sequenceMetrics]]) {
    const geom = (w, native) => ({ x: 0, y: 0, width: w, height: 100, canvas: native ? canvas : null, texts: [], nodes: [], edges: [], circles: [] });
    const run = (w) => classify(family, { ref: 'ok', native: 'ok' }, metrics(geom(w), geom(w, true), 'flowchart TB'));
    assert.deepEqual(run(201).reasons, []);
    assert.equal(run(250).class, 'differs');
    assert.deepEqual(run(250).reasons, ['clipped by 50px']);
  }
});
