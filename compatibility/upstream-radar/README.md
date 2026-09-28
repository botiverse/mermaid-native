# Original Radar grammar assertions

Executes the pinned, unchanged `packages/parser/tests/radar.test.ts` (85 assertions). The reference run records calls through the exported synchronous parser and asynchronous API. The Native run replaces those entry points with values and diagnostics produced by the shared Kotlin parser; it does not parse the input again in JavaScript. The original `MermaidParseError` formatter wraps Native diagnostic locations for the asynchronous consumer assertions.

The runtime JAR hash, source hashes, all captured inputs and both reports are retained in `.native-radar-audit/`. Parser-boundary assertions do not claim Native screenshot equivalence or the original Langium recovery tree implementation.

The runner requires `--upstream` and `--stdlib`, with an optional `--repo` override, and consumes an already-built debug runtime JAR. It never starts Gradle itself.
