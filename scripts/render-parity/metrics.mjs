// Pure layout-parity metrics. No DOM, no I/O: the browser step reduces each SVG
// to a geometry record and everything here works on those records, so the
// logic is unit-testable with plain `node --test`.
//
// Geometry record (both sides, user-space coordinates of the root <svg>):
// {
//   x, y, width, height,                 // content bounding box
//   canvas: { x, y, w, h } | null,       // root viewBox; content outside it is clipped
//   texts:  [{ text, x, y, w, h, role }], // role: node|edgeLabel|cluster|actor|message|note|seqnum|null
//   nodes:  [{ label, x, y, w, h }],      // semantic nodes (mermaid.js side only)
//   edges:  [{ points: [[x, y], ...], role }], // role: edge|message|lifeline|null
//   circles:[{ cx, cy, r }],              // small circles (autonumber badges)
// }

/** Documented classification thresholds (see compatibility/render-parity/README.md). */
export const THRESHOLDS = Object.freeze({
  rankTauMin: 0.7, // Kendall tau-b along the flow axis, flowchart
  rankMinNodes: 3, // fewer matched nodes: rank metric is not evaluated
  tieTolerance: 3, // px; coordinates closer than this are a tie
  labelCoverageMin: 0.8, // fraction of upstream node/message labels found natively
  crossingSlack: 1, // native may exceed upstream crossings by max(slack, 25%)
  crossingSlackRatio: 0.25,
  overlapSlack: 0, // native label overlaps may exceed upstream by this many
  aspectRatioRange: [0.5, 2], // (native w/h) / (upstream w/h)
  areaRatioRange: [0.25, 4], // (native w*h) / (upstream w*h)
  overflowMax: 1, // px the native content may extend past its viewBox
  edgeEndTrim: 6, // px around polyline endpoints ignored for crossings
});

export const normKey = (s) => String(s ?? '').normalize('NFKC').toLowerCase().replace(/[^\p{L}\p{N}]+/gu, '');

const cx = (b) => b.x + b.w / 2;
const cy = (b) => b.y + b.h / 2;

const union = (a, b, text) => {
  const x = Math.min(a.x, b.x);
  const y = Math.min(a.y, b.y);
  const r = Math.max(a.x + a.w, b.x + b.w);
  const bt = Math.max(a.y + a.h, b.y + b.h);
  return { ...a, text, x, y, w: r - x, h: bt - y };
};

/**
 * Merge label fragments into blocks: first runs on one baseline (styled
 * segments emitted as separate <text> elements), then consecutive stacked
 * lines (same role, same horizontal centre or left edge, small vertical gap).
 * Native SVG emits one <text> per line or run; mermaid.js usually one element
 * per label.
 */
export function mergeStackedTexts(texts) {
  const runs = [];
  for (const t of texts) {
    const prev = runs[runs.length - 1];
    if (prev && prev.role === t.role && Math.abs(cy(prev) - cy(t)) < 2 && Math.abs(prev.h - t.h) < 2) {
      const gap = t.x - (prev.x + prev.w);
      if (gap > -2 && gap < Math.max(4, t.h * 1.2)) { runs[runs.length - 1] = union(prev, t, `${prev.text} ${t.text}`); continue; }
    }
    runs.push({ ...t });
  }
  const out = [];
  for (const t of runs) {
    const prev = out[out.length - 1];
    if (prev && prev.role === t.role && t.text.trim()) {
      const sameColumn = Math.abs(cx(prev) - cx(t)) < Math.max(3, 0.2 * Math.max(prev.w, t.w)) || Math.abs(prev.x - t.x) < 2;
      const gap = t.y - (prev.y + prev.h);
      if (sameColumn && gap > -prev.h * 0.5 && gap < Math.max(4, t.h * 0.6)) {
        out[out.length - 1] = { ...union(prev, t, `${prev.text} ${t.text}`), lines: (prev.lines ?? 1) + 1 };
        continue;
      }
    }
    out.push({ ...t });
  }
  return out;
}

/**
 * Pair upstream labels with native text items by normalised text. The k-th
 * occurrence of a key on one side is paired with the k-th occurrence on the
 * other side (both in reading order). With `unique`, keys occurring more than
 * once on either side are dropped because their pairing would be arbitrary.
 */
export function matchLabels(refItems, nativeItems, { unique = false, order = readingOrder } = {}) {
  const group = (items) => {
    const m = new Map();
    for (const it of [...items].sort(order)) {
      const k = normKey(it.text ?? it.label);
      if (!k) continue;
      if (!m.has(k)) m.set(k, []);
      m.get(k).push(it);
    }
    return m;
  };
  const r = group(refItems);
  const n = group(nativeItems);
  const pairs = [];
  let considered = 0;
  for (const [k, refs] of r) {
    const nats = n.get(k) ?? [];
    if (unique && (refs.length > 1 || nats.length > 1)) continue;
    considered += refs.length;
    for (let i = 0; i < Math.min(refs.length, nats.length); i++) pairs.push({ key: k, ref: refs[i], native: nats[i] });
  }
  return { pairs, considered, coverage: considered ? pairs.length / considered : 1 };
}

export function readingOrder(a, b) {
  return (a.y - b.y) || (a.x - b.x);
}

/**
 * Kendall tau-b between two equally long numeric sequences. Differences within
 * `tol` count as ties. Returns null for fewer than two items or when either
 * side is entirely tied (undefined correlation).
 */
export function kendallTauB(a, b, tol = 0) {
  const n = a.length;
  if (n !== b.length) throw new Error('length mismatch');
  if (n < 2) return null;
  let conc = 0, disc = 0, tieA = 0, tieB = 0;
  for (let i = 0; i < n; i++) {
    for (let j = i + 1; j < n; j++) {
      const da = Math.abs(a[i] - a[j]) <= tol ? 0 : Math.sign(a[i] - a[j]);
      const db = Math.abs(b[i] - b[j]) <= tol ? 0 : Math.sign(b[i] - b[j]);
      if (da === 0 && db === 0) continue;
      if (da === 0) tieA++;
      else if (db === 0) tieB++;
      else if (da === db) conc++;
      else disc++;
    }
  }
  const denom = Math.sqrt((conc + disc + tieA) * (conc + disc + tieB));
  return denom === 0 ? null : (conc - disc) / denom;
}

/** Proper segment intersection point, or null (collinear overlaps are ignored). */
export function segmentIntersection(p1, p2, p3, p4) {
  const d = (p2[0] - p1[0]) * (p4[1] - p3[1]) - (p2[1] - p1[1]) * (p4[0] - p3[0]);
  if (Math.abs(d) < 1e-9) return null;
  const t = ((p3[0] - p1[0]) * (p4[1] - p3[1]) - (p3[1] - p1[1]) * (p4[0] - p3[0])) / d;
  const u = ((p3[0] - p1[0]) * (p2[1] - p1[1]) - (p3[1] - p1[1]) * (p2[0] - p1[0])) / d;
  if (t < 0 || t > 1 || u < 0 || u > 1) return null;
  return [p1[0] + t * (p2[0] - p1[0]), p1[1] + t * (p2[1] - p1[1])];
}

const dist = (a, b) => Math.hypot(a[0] - b[0], a[1] - b[1]);

/**
 * Count crossings between distinct edge polylines. Intersections within
 * `trim` px of either polyline's endpoints are ignored (edges legitimately
 * meet at shared nodes), and near-duplicate points are counted once.
 */
export function countCrossings(edges, trim = THRESHOLDS.edgeEndTrim) {
  const polys = edges.map((e) => e.points ?? e).filter((p) => p.length >= 2);
  let count = 0;
  for (let i = 0; i < polys.length; i++) {
    for (let j = i + 1; j < polys.length; j++) {
      const A = polys[i], B = polys[j];
      const ends = [A[0], A[A.length - 1], B[0], B[B.length - 1]];
      const hits = [];
      for (let a = 0; a < A.length - 1; a++) {
        for (let b = 0; b < B.length - 1; b++) {
          const p = segmentIntersection(A[a], A[a + 1], B[b], B[b + 1]);
          if (!p || ends.some((e) => dist(e, p) <= trim)) continue;
          if (hits.some((h) => dist(h, p) <= trim)) continue;
          hits.push(p);
        }
      }
      count += hits.length;
    }
  }
  return count;
}

const shrink = (b, m) => ({ x: b.x + m, y: b.y + m, w: Math.max(0, b.w - 2 * m), h: Math.max(0, b.h - 2 * m) });

export function intersectArea(a, b) {
  const w = Math.min(a.x + a.w, b.x + b.w) - Math.max(a.x, b.x);
  const h = Math.min(a.y + a.h, b.y + b.h) - Math.max(a.y, b.y);
  return w > 0 && h > 0 ? w * h : 0;
}

/** Liang-Barsky: does segment p-q pass through the box interior? */
export function segmentHitsBox(p, q, b) {
  let t0 = 0, t1 = 1;
  const dx = q[0] - p[0], dy = q[1] - p[1];
  const checks = [[-dx, p[0] - b.x], [dx, b.x + b.w - p[0]], [-dy, p[1] - b.y], [dy, b.y + b.h - p[1]]];
  for (const [pp, qq] of checks) {
    if (pp === 0) { if (qq < 0) return false; continue; }
    const r = qq / pp;
    if (pp < 0) { if (r > t1) return false; if (r > t0) t0 = r; }
    else { if (r < t0) return false; if (r < t1) t1 = r; }
  }
  return t0 <= t1 && b.w > 0 && b.h > 0;
}

/**
 * Label overlap count = text/text overlaps (intersection above 10% of the
 * smaller box after a 1px inset) + `edgeTexts` boxes crossed by an edge
 * polyline (2px inset).
 */
export function labelOverlaps(texts, edges, edgeTexts = []) {
  let textText = 0;
  const boxes = texts.filter((t) => t.w > 0 && t.h > 0).map((t) => shrink(t, 1));
  for (let i = 0; i < boxes.length; i++) {
    for (let j = i + 1; j < boxes.length; j++) {
      const a = boxes[i], b = boxes[j];
      const minArea = Math.min(a.w * a.h, b.w * b.h);
      if (minArea > 0 && intersectArea(a, b) > 0.1 * minArea) textText++;
    }
  }
  let textEdge = 0;
  for (const t of edgeTexts) {
    const b = shrink(t, 2);
    if (b.w <= 0 || b.h <= 0) continue;
    if (edges.some((e) => { const p = e.points ?? e; for (let k = 0; k < p.length - 1; k++) if (segmentHitsBox(p[k], p[k + 1], b)) return true; return false; })) textEdge++;
  }
  return { textText, textEdge, total: textText + textEdge };
}

/** Flow direction from the diagram header; subgraph-local directions are ignored. */
export function flowDirection(source) {
  for (const raw of String(source).split('\n')) {
    const line = raw.trim();
    if (!line || line.startsWith('%%') || line === '---') continue;
    const m = line.match(/^(?:flowchart|graph)(?:-elk)?\s+(TB|TD|BT|LR|RL)\b/i);
    if (m) return m[1].toUpperCase() === 'TD' ? 'TB' : m[1].toUpperCase();
    if (/^(?:flowchart|graph)/i.test(line)) return 'TB';
  }
  return 'TB';
}

const ratio = (a, b) => (b > 0 && a > 0 ? a / b : null);
const round = (v, d = 3) => (v == null || !Number.isFinite(v) ? v ?? null : Math.round(v * 10 ** d) / 10 ** d);

function sizeMetrics(ref, nat) {
  const aspect = ratio(nat.width / nat.height, ref.width / ref.height);
  const area = ratio(nat.width * nat.height, ref.width * ref.height);
  return { aspectRatioRatio: round(aspect), areaRatio: round(area) };
}

/** How far (px) the content extends past the viewBox on its worst side; 0 when nothing is clipped. */
export function canvasOverflow(geom) {
  const c = geom.canvas;
  if (!c || geom.x == null) return 0;
  return Math.max(0, c.x - geom.x, c.y - geom.y, geom.x + geom.width - (c.x + c.w), geom.y + geom.height - (c.y + c.h));
}

/** Flowchart metrics from two geometry records. */
export function flowchartMetrics(ref, nat, source) {
  const dir = flowDirection(source);
  const main = dir === 'LR' || dir === 'RL' ? 'x' : 'y';
  const cross = main === 'x' ? 'y' : 'x';
  const natTexts = mergeStackedTexts(nat.texts);
  const refNodes = ref.nodes.map((n) => ({ ...n, text: n.label }));
  const { pairs, coverage, considered } = matchLabels(refNodes, natTexts, { unique: true });
  const at = (b, axis) => (axis === 'x' ? cx(b) : cy(b));
  const tau = pairs.length >= THRESHOLDS.rankMinNodes
    ? kendallTauB(pairs.map((p) => at(p.ref, main)), pairs.map((p) => at(p.native, main)), THRESHOLDS.tieTolerance)
    : null;
  const tauCross = pairs.length >= THRESHOLDS.rankMinNodes
    ? kendallTauB(pairs.map((p) => at(p.ref, cross)), pairs.map((p) => at(p.native, cross)), THRESHOLDS.tieTolerance)
    : null;
  const refNodeTexts = ref.texts.filter((t) => t.role === 'node');
  const natNodeTexts = pairs.map((p) => p.native);
  const refEdges = ref.edges.filter((e) => e.role !== 'lifeline');
  const natEdges = nat.edges.filter((e) => e.role !== 'lifeline');
  return {
    direction: dir,
    refNodes: ref.nodes.length,
    matchedNodes: pairs.length,
    labelCoverage: round(coverage),
    labelsConsidered: considered,
    rankTau: round(tau),
    crossAxisTau: round(tauCross),
    refCrossings: countCrossings(refEdges),
    nativeCrossings: countCrossings(natEdges),
    refEdges: refEdges.length,
    nativeEdges: natEdges.length,
    refOverlaps: labelOverlaps(ref.texts, refEdges, refNodeTexts).total,
    nativeOverlaps: labelOverlaps(natTexts, natEdges, natNodeTexts).total,
    nativeOverflow: round(canvasOverflow(nat)),
    ...sizeMetrics(ref, nat),
  };
}

/** Count self-message loops: open paths whose ends share x and that bulge sideways. */
export function countSelfLoops(edges) {
  return edges.filter((e) => {
    const p = e.points ?? e;
    if (e.role === 'lifeline') return false;
    if (typeof e.self === 'boolean') return e.self; // mermaid.js data-from/data-to
    if (p.length < 3) return false;
    const a = p[0], b = p[p.length - 1];
    const xs = p.map((q) => q[0]);
    const bulge = Math.max(...xs.map((x) => Math.abs(x - a[0])));
    return Math.abs(a[0] - b[0]) <= 2 && Math.abs(a[1] - b[1]) > 2 && bulge > 10;
  }).length;
}

const SEQ_NUMBER = /^\d+(?:\.\d+)*$/;

/** Autonumber: a seqnum-role text, or a numeric text centred inside a small circle. */
export function hasAutonumber(geom) {
  if (geom.texts.some((t) => t.role === 'seqnum')) return true;
  return geom.texts.some((t) => SEQ_NUMBER.test(t.text.trim()) && (geom.circles ?? []).some((c) => c.r <= 14
    && Math.abs(cx(t) - c.cx) <= (c.rx ?? c.r) && Math.abs(cy(t) - c.cy) <= (c.ry ?? c.r)));
}

const sameOrder = (a, b) => a.length === b.length && a.every((v, i) => v === b[i]);

/** Sequence metrics from two geometry records. */
export function sequenceMetrics(ref, nat) {
  const natTexts = mergeStackedTexts(nat.texts);
  // Participants: upstream actor labels (top row only), matched natively by text.
  const refActorsAll = mergeStackedTexts(ref.texts.filter((t) => t.role === 'actor'));
  const topY = new Map();
  for (const t of refActorsAll) {
    const k = normKey(t.text);
    if (!topY.has(k) || cy(t) < cy(topY.get(k))) topY.set(k, t);
  }
  const refActors = [...topY.values()];
  const natTop = new Map();
  for (const t of natTexts) {
    const k = normKey(t.text);
    if (!topY.has(k)) continue;
    if (!natTop.has(k) || cy(t) < cy(natTop.get(k))) natTop.set(k, t);
  }
  const actorKeysRef = [...refActors].sort((a, b) => cx(a) - cx(b)).map((t) => normKey(t.text));
  const actorKeysNat = [...natTop.entries()].sort((a, b) => cx(a[1]) - cx(b[1])).map(([k]) => k);
  const actorCoverage = actorKeysRef.length ? actorKeysNat.length / actorKeysRef.length : 1;
  const participantOrderEqual = sameOrder(actorKeysRef.filter((k) => natTop.has(k)), actorKeysNat);

  // Messages: vertical order of message labels.
  const refMsgs = mergeStackedTexts(ref.texts.filter((t) => t.role === 'message'));
  const used = new Set(actorKeysNat);
  const natMsgCandidates = natTexts.filter((t) => !used.has(normKey(t.text)) || refMsgs.some((m) => normKey(m.text) === normKey(t.text)));
  const { pairs, coverage } = matchLabels(refMsgs, natMsgCandidates, { order: (a, b) => cy(a) - cy(b) || cx(a) - cx(b) });
  const refOrder = [...pairs].sort((a, b) => cy(a.ref) - cy(b.ref)).map((p) => p.key);
  const natOrder = [...pairs].sort((a, b) => cy(a.native) - cy(b.native)).map((p) => p.key);
  const messageTau = pairs.length >= 2
    ? kendallTauB(pairs.map((p) => cy(p.ref)), pairs.map((p) => cy(p.native)), THRESHOLDS.tieTolerance)
    : null;

  const refEdges = ref.edges.filter((e) => e.role !== 'lifeline');
  const natEdges = nat.edges.filter((e) => e.role !== 'lifeline');
  return {
    refParticipants: actorKeysRef.length,
    participantCoverage: round(actorCoverage),
    participantOrderEqual,
    refMessages: refMsgs.length,
    matchedMessages: pairs.length,
    messageCoverage: round(coverage),
    messageOrderEqual: sameOrder(refOrder, natOrder),
    messageTau: round(messageTau),
    refSelfMessages: countSelfLoops(ref.edges),
    nativeSelfMessages: countSelfLoops(nat.edges),
    refAutonumber: hasAutonumber(ref),
    nativeAutonumber: hasAutonumber(nat),
    refOverlaps: labelOverlaps(ref.texts.filter((t) => t.role !== 'seqnum'), refEdges, refMsgs).total,
    nativeOverlaps: labelOverlaps(natTexts.filter((t) => !SEQ_NUMBER.test(t.text.trim())), natEdges, pairs.map((p) => p.native)).total,
    nativeOverflow: round(canvasOverflow(nat)),
    ...sizeMetrics(ref, nat),
  };
}

const outside = (v, [lo, hi]) => v == null || v < lo || v > hi;

/**
 * Classify one case.
 * status: { ref: 'ok'|'error', native: 'ok'|'parse-error'|'render-error'|'timeout'|'skipped' }
 * Returns { class: match|differs|native-fails|ref-fails, reasons[], score }.
 * `score` orders cases by badness for the "worst N" list (higher is worse).
 */
export function classify(family, status, metrics) {
  if (status.ref !== 'ok') return { class: 'ref-fails', reasons: [`mermaid.js: ${status.refError ?? 'error'}`], score: 0 };
  if (status.native !== 'ok') return { class: 'native-fails', reasons: [`native ${status.native}${status.nativeError ? `: ${status.nativeError}` : ''}`], score: 100 };
  const T = THRESHOLDS;
  const reasons = [];
  let score = 0;
  const m = metrics;
  if (family === 'flowchart') {
    if (m.labelCoverage < T.labelCoverageMin) { reasons.push(`labels found ${m.matchedNodes}/${m.labelsConsidered}`); score += 20 * (1 - m.labelCoverage); }
    if (m.rankTau != null && m.rankTau < T.rankTauMin) { reasons.push(`rank order tau ${m.rankTau}`); score += 20 * (1 - m.rankTau); }
    const allowed = m.refCrossings + Math.max(T.crossingSlack, Math.ceil(T.crossingSlackRatio * m.refCrossings));
    if (m.nativeCrossings > allowed) { reasons.push(`crossings ${m.nativeCrossings} vs ${m.refCrossings}`); score += 3 * (m.nativeCrossings - m.refCrossings); }
  } else {
    if (m.participantCoverage < 1) { reasons.push(`participants found ${Math.round(m.participantCoverage * m.refParticipants)}/${m.refParticipants}`); score += 20 * (1 - m.participantCoverage); }
    if (!m.participantOrderEqual) { reasons.push('participant order'); score += 15; }
    if (m.messageCoverage < T.labelCoverageMin) { reasons.push(`messages found ${m.matchedMessages}/${m.refMessages}`); score += 20 * (1 - m.messageCoverage); }
    if (!m.messageOrderEqual) { reasons.push(`message order (tau ${m.messageTau})`); score += 15; }
    if (m.refSelfMessages !== m.nativeSelfMessages) { reasons.push(`self messages ${m.nativeSelfMessages} vs ${m.refSelfMessages}`); score += 5; }
    if (m.refAutonumber !== m.nativeAutonumber) { reasons.push(`autonumber ${m.nativeAutonumber} vs ${m.refAutonumber}`); score += 5; }
  }
  if (m.nativeOverlaps > m.refOverlaps + T.overlapSlack) { reasons.push(`label overlaps ${m.nativeOverlaps} vs ${m.refOverlaps}`); score += 3 * (m.nativeOverlaps - m.refOverlaps); }
  if (m.nativeOverflow > T.overflowMax) { reasons.push(`clipped by ${m.nativeOverflow}px`); score += 10; }
  if (outside(m.aspectRatioRatio, T.aspectRatioRange)) { reasons.push(`aspect ratio x${m.aspectRatioRatio}`); score += 5 * Math.abs(Math.log2(m.aspectRatioRatio || 1e-3)); }
  if (outside(m.areaRatio, T.areaRatioRange)) { reasons.push(`area x${m.areaRatio}`); score += 3 * Math.abs(Math.log2(m.areaRatio || 1e-3)); }
  return { class: reasons.length ? 'differs' : 'match', reasons, score: round(score, 2) };
}

/** Stable identity of a case across corpus renumbering: family + source hash + options. */
export function caseKey(row) {
  return `${row.family}|${row.sha256}|${row.options_json}`;
}

/**
 * Compare current results with a baseline. A regression is match→differs,
 * match→native-fails, or differs→native-fails (native ok→fails). Cases absent
 * from the baseline are reported as new, never as regressions.
 */
export function compareWithBaseline(baseline, results) {
  const base = baseline?.cases ?? {};
  const regressions = [], improvements = [], added = [];
  const seen = new Set();
  for (const r of results) {
    const k = caseKey(r);
    if (seen.has(k)) continue;
    seen.add(k);
    const b = base[k];
    if (!b) { added.push({ case: `${r.family}/${r.case}`, now: r.class }); continue; }
    const from = b.class, to = r.class;
    const rank = { match: 0, differs: 1, 'native-fails': 2 };
    if (!(from in rank) || !(to in rank)) continue;
    if (rank[to] > rank[from]) regressions.push({ case: `${r.family}/${r.case}`, from, to, reasons: r.reasons });
    else if (rank[to] < rank[from]) improvements.push({ case: `${r.family}/${r.case}`, from, to });
  }
  const removed = Object.keys(base).filter((k) => !seen.has(k)).map((k) => base[k].case);
  return { regressions, improvements, added, removed };
}

/** Baseline document derived from a summary's case list. */
export function baselineFrom(summary) {
  const cases = {};
  for (const r of summary.cases) cases[caseKey(r)] = { case: `${r.family}/${r.case}`, class: r.class, native: r.status.native };
  return { version: 1, upstream_commit: summary.upstream_commit, mermaid_version: summary.mermaid_version, counts: summary.counts, cases };
}
