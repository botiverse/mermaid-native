# Original Info grammar assertions

Runs all 12 unchanged original `packages/parser/tests/info.test.ts` assertions at the pinned revision. The Java bridge calls the real `MermaidParser` and verifies that the resulting product model is `InfoDiagram`; the adapter replaces the original Langium parse result. It does not run the original parser for Native results. Source hashes, input capture, runtime JAR, and reports remain in `.native-info-audit`.

Build core, then run `python3 compatibility/upstream-info/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib-2.1.21.jar`.

Native displays its own Gradle build version, never the upstream JavaScript package version. Both `info` and `info showInfo` render the version, matching the original renderer's behavior. The 12 assertions check grammar/model admission only; original consumer tests requiring exact Langium error text and original renderer geometry remain separate coverage. The supported syntax is `info` with optional `showInfo`, whitespace and comment lines; metadata beyond that slice is not claimed.
