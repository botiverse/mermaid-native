# Upstream Sequence integration runner

Executes the unchanged `sequenceDiagram.spec.js` from Mermaid revision
`04ee3364045d6573f84034d3c9368cc50233a92f` twice: once with its original Jison
parser, then with the Kotlin parser substituted at that boundary. Source hashes
are verified. Native parsing errors remain errors, and a failed assertion makes
the runner exit nonzero.

```sh
python3 compatibility/upstream-sequence/run.py \
  --upstream /path/to/installed-and-built/upstream-mermaid \
  --stdlib /path/to/kotlin-stdlib-2.1.21.jar
```

Requires Java, Python 3, pnpm and the Android Gradle environment. Build upstream
with its locked dependencies and `pnpm exec tsx .esbuild/build.ts` first. Use
`--skip-build` only with a freshly built core runtime JAR. Reports and the exact
JAR SHA-256 are saved under the upstream `.native-sequence-audit/` directory.
The bridge serializes typed actors and ordered events; the adapter copies them
into the real upstream SequenceDB. It does not parse input or call the upstream
parser to create Native results.

Initial result: reference **146 passed**; Native boundary **57 passed, 89 failed**.
After the ordered-event batch: Native boundary **115 passed, 31 failed**.
Remaining failures are retained, covering participant metadata/shapes, boxes,
creation/destruction, actor links/properties and half-arrows.

These 146 cases are a **mixed integration suite**, not 146 Native rendering tests:

- The central-connection and parser groups contain 79 original cases (current
  result 71 passed, 8 failed).
- The 30 database-group cases include participant parsing and direct upstream
  database behavior; passing direct database checks does not prove a Native API.
- The other 37 cases exercise upstream rendering/bounds or cross-diagram state.
  They still use the upstream renderer. Their pass status must not be presented
  as Native layout, SVG, Canvas, or screenshot parity.

Separate Native common tests verify source order, invalid nesting, decimal
numbering, metadata propagation, measured note/self-loop bounds, and activation
end positions through the actual layout consumer. Real Native browser evidence
is required in addition to this adapter run. The broader upstream campaign and
Native rendering equivalence remain outstanding.
