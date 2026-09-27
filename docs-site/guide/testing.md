# Testing contract

Run the current compatibility gates locally:

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

## Original Mermaid test suites

The upstream conformance campaign runs original tests from Mermaid revision
`04ee3364045d6573f84034d3c9368cc50233a92f`. Assertions, snapshots and test inputs
are kept unchanged. Each runner first verifies the pinned source hashes and runs
the original parser, then substitutes the Kotlin parser's typed model at the
parser boundary. It never calls the original parser to supply Native results.

| Original suite | Passing | Failing | Skipped | Boundary |
| --- | ---: | ---: | ---: | --- |
| ER grammar and subgraphs | 651 | 0 | 1 | Native parser model |
| Pie grammar | 45 | 0 | 0 | Native parser model; 3 config cases separate |
| Sequence | 146 | 0 | 0 | 79 parser cases; 67 upstream DB/renderer cases |
| Class | 383 | 0 | 1 | 382 parser-calling cases; 1 direct upstream DB case |

A passing parser-boundary assertion proves the modeled information used by that
assertion. It does **not** prove Native pixel equivalence, text formatting,
interactive host bindings or every syntax form in that family. The upstream
DB still performs some formatting/configuration in these adapters. Native
layout regressions and production Wasm browser captures are checked separately.
Existing Native colors and visual design are retained while fixing behavior.

Reproducible runners and precise limitations are in
[`compatibility/upstream-er`](https://github.com/botiverse/mermaid-native/tree/main/compatibility/upstream-er),
[`upstream-pie`](https://github.com/botiverse/mermaid-native/tree/main/compatibility/upstream-pie),
[`upstream-sequence`](https://github.com/botiverse/mermaid-native/tree/main/compatibility/upstream-sequence)
and [`upstream-class`](https://github.com/botiverse/mermaid-native/tree/main/compatibility/upstream-class).
Use each README's environment setup; the upstream checkout must have its pinned
lockfile dependencies installed and built.

The full original unit-test reference baseline contains **5,371 passing, 16
skipped and 2 todo run occurrences** (5,389 total, including 51 repeated
documentation assertions from the upstream project configuration). Most of
those assertions have not yet been integrated with Native. Original Cypress
runs are also being audited; newly generated screenshots are first captures,
not independently reviewed visual baselines. Full upstream parity is still
in progress.
