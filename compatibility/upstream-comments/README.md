# Original Mermaid comment cleanup assertions through Native

Runs all five unchanged `comments.spec.ts` tests from pinned upstream revision
04ee3364045d6573f84034d3c9368cc50233a92f. Inputs are captured from the original
module, passed to the production Kotlin `MermaidComments.cleanup`, and replayed
through the original assertions and inline snapshots. Java/Python/JavaScript
only transport strings. File hashes and the runtime core JAR hash are recorded.

The same Kotlin cleanup implementation is consumed by MermaidParser's statement
scanner before semicolon splitting, fixing whole-line comments whose suffix was
previously interpreted as a header or statement. A retained-span map preserves
original line/column diagnostics after comment removal and leading whitespace
trimming. Family-specific parsers still receive their original source.

The five original assertions cover LF input. Native also accepts CRLF and bare
CR line endings without consuming subsequent source lines; this is verified by
additional production tests, not counted as original upstream coverage.
Directives are retained by cleanup; this does not claim execution of their
configuration or a YAML/frontmatter implementation.

After building the production Android core runtime JAR:

```sh
python3 compatibility/upstream-comments/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar
```

The runner never starts Gradle. Outputs are in upstream `.native-comments-audit`.
