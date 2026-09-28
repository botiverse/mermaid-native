# Original upstream Flowchart suites

Runs 15 unchanged parser suites from Mermaid revision
`04ee3364045d6573f84034d3c9368cc50233a92f`. Suite, wrapper, Jison grammar and FlowDB
source hashes are checked before execution. Original assertions and snapshots
are never rewritten. The original reference passes 948 tests with 3 skips.

```sh
python3 compatibility/upstream-flow/run.py \
  --upstream /path/to/installed-and-built/upstream-mermaid \
  --stdlib /path/to/kotlin-stdlib-2.1.21.jar
```

Use Java, Python, pnpm and the Android Gradle environment. Install the upstream
lockfile dependencies and run `pnpm exec tsx .esbuild/build.ts` first.
`--skip-build` requires a fresh core runtime JAR. The runner copies that JAR into
its audit directory before generating typed models, so later builds cannot
silently change its provenance. Reports and exact SHA-256 are in
`.native-flow-audit/` in the upstream checkout.

The previous Native subset passed 234, failed 714 and skipped 3. The frozen first
batch passed 865, failed 83 and skipped 3 (core JAR SHA-256
`a300ac89f4b487d9f4128809d941eb0f92f0e7f057c8d4e4b7338a01c851355f`).
Later extensions need their own frozen results. Failures remain visible and cause a nonzero exit.

The adapter serializes Native nodes, edges, markers, IDs, lengths, labels,
subgraphs and accessibility fields into the actual FlowDB. It does not call the
original parser for Native results. FlowDB still provides sanitization,
configuration and database behavior, so these are parser-boundary checks, not
Native visual or interaction equivalence.

This batch draws actual endpoint kinds, open/invisible lines, additional node
shapes, partial rectangle borders, nested group bounds, local direction and
minimum edge spacing. Native layout tests and production-browser evidence are
separate gates. Dynamic node/edge metadata,
link interpolation, rich Markdown/HTML rendering and advanced graph routing
remain follow-up work; unsupported declarations fail explicitly. The separately
implemented swimlane family is not routed through this Flowchart adapter.

The style extension retains class definitions, ordered node styles, edge styles
and typed callback/link metadata. The renderer applies node fill, stroke, border
width and font size/weight/color with measured geometry and edge stroke/width.
Callback arguments remain data; Native hosts do not execute callbacks or bind
links yet. Italic font rendering and exact dash arrays (including ellipse dashes)
are not implemented. FlowDB warnings/URL policy are upstream behavior, not Native
coverage. Frozen original-suite and multiplatform results are pending.

The metadata extension decodes scalar YAML labels and a set of supported shape
aliases, retaining metadata on typed nodes. Edge interpolation/animation settings
are retained as data; animated playback and exact D3 curve interpolation are not
yet implemented by Native drawing. Collapsed groups do affect actual drawing:
internal members disappear, internal edges are removed and external edges route
to the group node. Repeated sibling groups combine their members for placement.
The original suite now passes 947 tests, with 1 failure (the separately parsed
swimlane alias) and 3 upstream skips. Core JAR SHA-256:
`0cd01328367fcd9d56477696e90c2052e982900103addc57155ea55ca01fbc57`.
The frozen first matrix passed 142 core tests on each platform,104 layout tests
and5 testkit tests; the final public-Web-consumer matrix is being rerun after
updating a stale click-metadata negative assertion.
