# Upstream Pie assertion runner

Runs unchanged parser assertions from Mermaid revision
`04ee3364045d6573f84034d3c9368cc50233a92f` in both the original implementation
and Kotlin `MermaidParser`. Source hashes are verified before execution.

```sh
python3 compatibility/upstream-pie/run.py \
  --upstream /path/to/installed-and-built/upstream-mermaid \
  --stdlib /path/to/kotlin-stdlib-2.1.21.jar
```

Java, Python 3, pnpm and the regular Android Gradle environment are required.
Install the upstream locked dependencies and run `pnpm exec tsx .esbuild/build.ts`
first. The runner builds the Native core JAR and records its SHA-256. A fresh
JAR from a separate validation build can be reused with `--skip-build`.
Reports, logs, captured inputs and the Native model cache are written to the
upstream checkout's `.native-pie-audit/` directory. Failed Native assertions
produce a nonzero exit; source tests and assertions are never rewritten.

The parser helper is replaced only at the implementation boundary. Native AST
values and API database values come from the Kotlin model; the official parser
is never called to produce Native results. A Kotlin failure is mapped to parser
errors or an API exception as appropriate. These checks prove accepted input,
metadata, section values and rejection behavior; empty Langium error arrays do
not imply Native uses or tests the Langium lexer itself.

Current result: **45 passed** in each implementation (41 distinct runtime inputs).
Three configuration cases are explicitly excluded because they do not exercise
Native parsing; two were already upstream todo cases. They are not counted as
Native successes or upstream skips. The original Pie renderer suites, Cypress
visual comparison and other diagram families remain separate outstanding work.
Native common tests additionally check malformed inputs, quoted delimiters,
diagnostic source locations, the actual layout metadata path and text bounds.
