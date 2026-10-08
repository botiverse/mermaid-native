# Original registry example parsing

Runs the 79 unchanged `should have valid examples` assertions from pinned `packages/examples/src/example.spec.ts` against production Kotlin `MermaidParser.parse`. The one registry completeness assertion and three parse-and-render assertions remain unselected. All example registry sources are hash-pinned in sources.json.

The reference run captures every actual `mermaid.parse` input. A Java bridge invokes the Native production parser once per captured call and transports success or diagnostics. The Native pass replaces `mermaid.parse` with those results and uses per-source queues; missing results fail closed. Neither source text nor expected assertions are rewritten. The original registration and DOM setup still run but are not Native coverage; none of the selected assertions inspect them. No original parser fallback runs in the Native pass.

Current local result after document metadata and consumer-option integration: upstream79 passed; Native79 passed/0 failed/4 unselected. The unselected cases are one registry-completeness assertion and three parse-and-render assertions, not failed parsing assertions hidden by skips. All 79 inputs are unique and invoke production Kotlin.

One negative-control run changes a passing Native result in each of33 diagram families into a failure. Exactly those33 assertions additionally fail; restoring results recovers79/0. This validates successful parsing only, not model contents, configuration application, layout quality or renderer parity. Native model/layout tests and real consumer checks independently validate admitted titles, configuration and expanded shapes; unknown options still fail explicitly.

```sh
python3 compatibility/upstream-example-parse/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar --verify-mutations
```

Build `:mermaid-core:bundleLibRuntimeToJarDebug` first. validation.json records the measured runtime hash, source commit, unchanged assertion counts and corruption-control failures. No original parsing assertion is changed or removed to obtain the result.
