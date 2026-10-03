# Original registry example parsing

Runs the 79 unchanged `should have valid examples` assertions from pinned `packages/examples/src/example.spec.ts` against production Kotlin `MermaidParser.parse`. The one registry completeness assertion and three parse-and-render assertions remain unselected. All example registry sources are hash-pinned in sources.json.

The reference run captures every actual `mermaid.parse` input. A Java bridge invokes the Native production parser once per captured call and transports success or diagnostics. The Native pass replaces `mermaid.parse` with those results and uses per-source queues; missing results fail closed. Neither source text nor expected assertions are rewritten. The original registration and DOM setup still run but are not Native coverage; none of the selected assertions inspect them. No original parser fallback runs in the Native pass.

Baseline: upstream79 passed; Native66 passed/13 failed/4 unselected. Nine failures involve YAML frontmatter, two Block circle nodes, one expanded Flowchart shape and one Wardley component name. See failures.md for exact sources and diagnostics. The script exits nonzero while production gaps remain; do not treat known failures as passes or skips.

One negative-control run changes a passing Native result in each of32 diagram families into a failure. Exactly those32 assertions additionally fail; restoring results recovers the original66/13 outcome. All79 inputs are unique and all79 invoke Kotlin. This validates successful parsing only, not model contents, configuration application, layout quality or renderer parity.

```sh
python3 compatibility/upstream-example-parse/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar --verify-mutations
```

Build `:mermaid-core:bundleLibRuntimeToJarDebug` first. validation.json records the measured runtime hash, source commit, unchanged assertion counts and corruption-control failures. The known failing assertions are retained as failures for subsequent implementation batches.
