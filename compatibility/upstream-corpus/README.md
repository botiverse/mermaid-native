# Upstream test corpus

Every Mermaid source found in upstream mermaid-js tests at the pinned revision
(`upstream_commit` in `manifest.tsv`, same as `compatibility/upstreams.lock`),
run through Native parse + layout + SVG render and compared with mermaid.js.
Mermaid is MIT licensed (see `LICENSE-mermaid.txt`); no upstream test code or
runtime is copied, only the diagram sources.

| kind | upstream files | extraction |
| --- | --- | --- |
| `cypress` | `cypress/integration/rendering/**/*.spec.{js,ts}` | one case per `imgSnapshotTest(…)` / `renderGraph(…)` call (array arguments: one per element); literal options kept, non-literal options recorded as `{"$unresolved": …}` |
| `spec` | `packages/**/*.{spec,test}.{js,ts}` | every string literal whose text starts with a Mermaid diagram header (after front matter, `%%` directives and comments); `+` concatenation and `${}` parts bound to `const` strings are evaluated; test titles are ignored; single-line strings need the exact-case header. Identical sources within one directory are stored once |

## Files

- `<kind>/<family>/<NNN-slug>.mmd`: fixtures. `family` is the upstream directory
  (diagram folder for unit specs, `misc`/`mermaid-core` for the rest).
- `manifest.tsv`: one row per fixture: kind, family, `diagram_type` (from the
  header), upstream path, call/string index, `describe > it` title, number of
  occurrences, SHA-256 of the fixture bytes, options.
- `provenance.tsv`: every upstream occurrence of every fixture.
- `skipped.tsv`: sources that cannot be resolved statically (loop variables,
  computed values) and are therefore not imported.
- `reference.tsv`: mermaid.js outcome per fixture (`ok`, `parse-error`,
  `render-error`, `timeout`, `unsupported`), from the npm release of the pinned
  revision (`scripts/render-parity/package.json`) in headless Chromium.
- `expected-failures.tsv`: cases that currently fail, with their outcome.

## Outcomes

`UpstreamCorpusTest` (`:mermaid-testkit:testDebugUnitTest`, part of `check`)
classifies each case:

- `pass`: both render it, or mermaid.js rejects it and Native reports a parse diagnostic.
- `rejects-valid`: mermaid.js renders it, Native reports a parse diagnostic.
- `accepts-invalid`: mermaid.js rejects it, Native renders it.
- `crash`: Native parse, layout or SVG rendering throws or takes over 20 s.
- unscored (`ref-render-error`, `ref-timeout`, `ref-unsupported`): mermaid.js
  parses it but cannot draw it, or its release has no such diagram (usecase-beta
  exists at the pinned commit but not in 11.16.1; zenuml is a separate package).
  A Native crash is still a failure.

The test fails on a failing case not listed in `expected-failures.tsv`
(regression), on a listed case that now passes or changed outcome, and on a
fixture whose hash differs from the manifest or reference. It writes
`mermaid-testkit/build/upstream-corpus/{summary.md,results.tsv,expected-failures.candidate.tsv}`;
after an intended change, copy the candidate over `expected-failures.tsv`.

This measures whether Native accepts and draws what mermaid.js accepts and draws.
Visual comparison of flowchart and sequence cases is in `compatibility/render-parity`.

## Regenerating

```sh
(cd scripts/render-parity && npm ci)
scripts/upstream-corpus/fetch-upstream.sh "" /tmp/mermaid      # or another sha
node scripts/upstream-corpus/extract.mjs --mermaid /tmp/mermaid  # --check: compare only
node scripts/upstream-corpus/reference.mjs                       # needs Playwright Chromium
./gradlew :mermaid-testkit:testDebugUnitTest --tests '*UpstreamCorpusTest*'
cp mermaid-testkit/build/upstream-corpus/expected-failures.candidate.tsv compatibility/upstream-corpus/expected-failures.tsv
```

The `upstream-corpus` job in `.github/workflows/render-parity.yml` checks that
the committed corpus equals a fresh extraction at the pinned revision.
