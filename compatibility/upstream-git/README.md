# Original GitGraph consumer assertions

Runs the pinned `gitGraph.spec.ts` without edits, first against the official parser and then against Kotlin's resolved GitGraph model. Source hashes are checked before either run. The official pass records every parser input; a fresh runtime JAR resolves those inputs in a batch. The adapter only projects typed Native values into the original consumer getter shape and surfaces Native diagnostics/warnings. It does not calculate branch or commit history in JavaScript.

The suite has 74 cases (four upstream skips). Five configuration getter assertions do not invoke the parser and remain JavaScript-only coverage; report them separately from Native parser assertions. The lower-level Langium recovery-AST tests and renderer/theme configuration suites are not part of this boundary. Deterministic Native automatic IDs replace upstream random IDs; no original assertion is changed.

Run from the repository root with Node/pnpm, JDK and Kotlin 2.1.21 stdlib available:

```sh
python3 compatibility/upstream-git/run.py --upstream /path/to/pinned/mermaid --stdlib /path/to/kotlin-stdlib-2.1.21.jar
```

`--skip-build` consumes the already-built debug runtime JAR. Results and its SHA-256 are written to the upstream checkout's `.native-git-audit/` directory. A parser-boundary pass does not claim Native screenshot equivalence.
