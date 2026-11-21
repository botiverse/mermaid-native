# Upstream rendering-test parity corpus

Diagram sources extracted from Mermaid's Cypress rendering specs at the exact
revision in `manifest.tsv` (`upstream_commit`, matching `compatibility/upstreams.lock`).
Mermaid is MIT licensed (see `LICENSE-mermaid.txt`); every row records the
original spec path, the call index within that file and the enclosing
`describe > it` titles. The Cypress harness and Mermaid runtime are not copied.

Phase 1 covers two families:

| family | upstream specs |
| --- | --- |
| flowchart | `cypress/integration/rendering/flowchart/*.spec.{js,ts}` |
| sequence | `cypress/integration/rendering/sequence/*.spec.{js,ts}` |

## Files

- `<family>/<NNN-slug>.mmd`: one diagram per `imgSnapshotTest(…)` / `renderGraph(…)`
  call (array arguments yield one case per element). NNN is the position after
  sorting by upstream path and call index; the slug comes from the `it()` title.
  Sources are dedented and trimmed (the upstream e2e page dedents too).
- `manifest.tsv`: tab-separated, sorted; `sha256` is over the fixture bytes and
  `options_json` is the literal options object (sorted keys) passed to the helper.
- `skipped.tsv`: calls that cannot be extracted statically: template literals with
  `${}` interpolation, sources/options bound to loop variables or computed values.
- `baseline.json`: per-case class from a full CI run; PRs are compared against it.

Extraction parses the specs with `@babel/parser`; nothing from the spec is
executed. `const` bindings initialised with literals are resolved lexically.

## Running

Everything runs in `.github/workflows/render-parity.yml`:

1. `scripts/render-parity/fetch-upstream.sh` + `extract-corpus.mjs --check`: the
   committed corpus must equal a fresh extraction at the pinned revision.
2. `native-render.mjs`: mermaid-native's production Kotlin/Wasm module renders each
   case (status `ok`, `parse-error`, `render-error` or `timeout`).
3. `render-browser.mjs`: headless Chromium (Playwright, pinned) renders the
   mermaid.js reference with the npm release of the pinned revision
   (`packages/mermaid/package.json` version) using the upstream helper defaults
   (`courier` fonts, `fontSize: 16px`, `handDrawnSeed: 1`, plus the case's
   options; `@mermaid-js/layout-elk` is registered). Both SVGs are rasterized to
   PNG and reduced to geometry (text boxes, node boxes, sampled edge polylines).
4. `report.mjs`: metrics, classes, `index.html`, `summary.json`, `summary.md`, and
   the comparison with `baseline.json`.

Native rendering ignores the upstream options (theme, look, `htmlLabels`, ELK);
the reference honours them, so such cases measure native output against what
upstream users configured.

## Metrics

Labels are matched by normalised text (case-folded, punctuation and whitespace
removed); stacked native text lines are merged first.

Flowchart:
- **rankTau**: Kendall tau-b between upstream node centres and native label
  centres along the flow axis (`TB/BT` → y, `LR/RL` → x), ties within 3 px.
  Labels that occur more than once are excluded. Not evaluated below 3 nodes.
- **crossings**: proper intersections between distinct edge polylines, ignoring
  points within 6 px of an edge endpoint.
- **label overlaps**: text/text box overlaps above 10% of the smaller box, plus
  node labels crossed by an edge.
- **aspectRatioRatio** = native (w/h) ÷ upstream (w/h); **areaRatio** = native w·h ÷ upstream w·h.

Sequence: participant order (top-row labels by x), message label vertical order,
self-message loops, autonumber presence, label overlaps, aspect and area ratios.

## Classes and thresholds

- `ref-fails`: mermaid.js itself failed; excluded from regressions.
- `native-fails`: native parse error, render error or timeout.
- `match`: every check below passes.
- `differs`: at least one check fails; the reasons are listed per case.

| check | threshold |
| --- | --- |
| label coverage (flowchart nodes, sequence messages) | ≥ 0.8 (participants: all) |
| rankTau (flowchart) | ≥ 0.7 |
| crossings (flowchart) | native ≤ upstream + max(1, 25% of upstream) |
| label overlaps | native ≤ upstream |
| participant / message order (sequence) | equal |
| self messages, autonumber (sequence) | equal |
| aspectRatioRatio | 0.5–2 |
| areaRatio | 0.25–4 |

The thresholds live in `scripts/render-parity/metrics.mjs` (`THRESHOLDS`). The
"worst" ranking uses a per-case score that weights each failed check.

## Baseline and regressions

A case regresses when its class worsens relative to `baseline.json`
(`match` → `differs`/`native-fails`, `differs` → `native-fails`). Cases are
keyed by family, fixture hash and options, so renumbering does not matter. New
cases are reported but never fail a PR. Existing differences never fail a PR.

To accept improvements or intended changes, download the
`render-parity-report` artifact and copy `baseline.candidate.json` over
`baseline.json`.

## Updating the upstream revision

Run the workflow manually with `mermaid_ref` set. The `render-parity-corpus-diff`
artifact contains `corpus-diff.md` (added, removed, changed, renamed and
unchanged cases), a patch, and the upstream `package.json` version. Apply the
patch in a PR together with the matching `mermaid` version in
`scripts/render-parity/package.json`, then refresh the baseline. Locally:

```sh
scripts/render-parity/fetch-upstream.sh <sha> /tmp/mermaid
MERMAID_COMMIT=<sha> node scripts/render-parity/extract-corpus.mjs --mermaid /tmp/mermaid --diff /tmp/corpus-diff.md
```
