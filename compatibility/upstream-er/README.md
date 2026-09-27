# ER upstream assertion runner

This runner executes the unchanged upstream `erDiagram.spec.js` twice: first
with its Jison parser, then with `MermaidParser` from the compiled Kotlin core.
The upstream ER database remains the assertion adapter. The Native adapter only
copies parsed entities, ordered attributes, aliases, labels, identification and
endpoint cardinalities into that database; it never invokes the upstream parser
to produce Native results.

Reference files are SHA-256 checked against `sources.json`, from Mermaid revision
`04ee3364045d6573f84034d3c9368cc50233a92f`. Install the checkout's locked dependencies
and build it before running:

```sh
cd /path/to/upstream-mermaid
pnpm install --frozen-lockfile --ignore-scripts
pnpm exec tsx .esbuild/build.ts
cd /path/to/mermaid-native
python3 compatibility/upstream-er/run.py \
  --upstream /path/to/upstream-mermaid \
  --stdlib /path/to/kotlin-stdlib-2.1.21.jar
```

Java, Python 3, pnpm, and the normal Android Gradle environment are required.
The command rebuilds the Native core JAR. It writes temporary configs, compiled
bridge, raw JSON reports, logs and `summary.json` under the upstream checkout's
`.native-er-audit/`; it does not edit the upstream test, parser or database.
Results are recalculated on every run. The command exits nonzero if any Native
assertion fails. Known gaps are not converted into expected successes or skips.

Current result: reference **633 passed, 1 skipped**; Native **633 passed,
0 failed, 1 skipped**. Styles/classes, accessibility metadata and the parent
marker now round-trip through the Native model. Rendering regression tests also
verify the emitted styles, title/description and endpoint markers. Supported
style properties are fill, stroke, color, stroke-width, font-size and font-weight;
font sizes and stroke widths support numeric/px values. Direction and subgraph
assertions live in other suites and are not included in these counts. The full
upstream campaign remains incomplete.

These are parser/database assertions, not visual parity or full upstream
coverage. Some upstream negative cases wrap both parsing and a failing `expect`
in `toThrow`, so passing those cases alone does not prove parser rejection.
Native common tests separately check failures, ordered model values and the
actual parser-to-layout path. Existing colors and typography are preserved;
new aliases, multiple keys and non-identifying dashed relationships are rendered.
