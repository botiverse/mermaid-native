# Testing contract

Run the Phase 0 gates locally:

```bash
./gradlew :mermaid-core:allTests :mermaid-testkit:allTests --no-daemon --offline
```

The tests cover Android debug/release unit targets and iOS Simulator Arm64.
The testkit exposes normalized vectors for the declared
`beautiful-mermaid`-derived subset. Core tests additionally require:

- every failure has a non-empty typed diagnostic with source location;
- unsupported headers never fall back to another family;
- unsupported body syntax never produces partial success;
- actor/node order is stable;
- arrow/operator boundaries do not consume hyphenated IDs;
- semicolon-separated statements retain physical line/column locations.

Changes to a syntax family must add an accepted fixture, a rejected fixture, and
at least one right-cause negative test. Platform pixel comparisons belong in
the renderer/sample gates; parser tests must not depend on font rasterization.
