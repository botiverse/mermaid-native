# Original XY parser assertions

Runs all 58 unchanged parser cases (62 inputs) pinned to04ee3364045d6573f84034d3c9368cc50233a92f against the original parser, then against the Kotlin parser. Source hashes are checked before execution. The Java bridge serializes Native typed axes, orientation, point labels, series names, text types and accessibility metadata. A thin adapter makes the original mock-DB setter calls; assertions are not rewritten.

This covers the parser boundary, not original XYChartDB/legend/builder internals or full visual parity. Actual horizontal/numeric/band-truncation drawing is covered by separate Native layout regressions and browser validation.

Run `python3 compatibility/upstream-xy/run.py --upstream /path/to/pinned/mermaid --stdlib /path/to/kotlin-stdlib.jar`. `--skip-build` snapshots the already-built Native JAR and records its hash. Reports are written to `.native-xy-audit` in the upstream checkout.
