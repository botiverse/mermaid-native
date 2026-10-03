# Native tree document operations

Runs both complete, unchanged upstream test files: Mindmap `mindmapDb.getData.test.ts` (11 assertions) and TreeView `db.spec.ts` (18). The runner verifies the pinned upstream files and the copies imported in `upstream-batch/originals` before execution. It does not select only passing tests.

The reference run records calls, arguments and direct Mindmap node edits. A JVM bridge replays those operations into production `MindmapDocument` / `TreeViewDocument` instances. Kotlin owns state, IDs, parent relationships, metadata, attributes, section assignment and layout-data export. The Native test adapter reads only these Kotlin results; it does not generate Mermaid source or synthesize hierarchy/configuration in JavaScript. Every call must match its recorded operation and arguments; unused operations fail the test. Five negative controls corrupt Native results for counters, IDs, width, inherited sections and accessibility metadata and require exactly the corresponding original assertion to fail.

```sh
python3 compatibility/upstream-tree-documents/run.py \
  --upstream /path/to/pinned/mermaid \
  --stdlib /path/to/kotlin-stdlib.jar \
  --verify-mutations
```

Build `:mermaid-core:bundleLibRuntimeToJarDebug` first. The runner uses the upstream checkout's installed pnpm/Vitest dependencies and writes raw reference/Native results, operation traces, mutation results and a summary under `.native-tree-documents-audit` there. `validation.json` records the tested production commit and JAR hash. No Gradle process is started by the runner.

## Production scope

`TreeViewDocument` provides direct node construction, a virtual root, IDs/counters, clear/reset, titles/accessibility metadata, default configuration and immutable diagram/root snapshots. `MindmapDocument` provides stable numeric IDs, direct edits through `node(id)` or the returned node, hierarchy, clear/reset, and detached layout-data snapshots with section inheritance/wrapping and edge metadata. `diagram()` projects both documents to the existing Native renderer. Layout integration tests compare actual drawing commands against equivalent parsed inputs, including nesting, shapes, icons and descriptions.

This is the tested document API contract, not complete browser DB or rendering parity. TreeView configuration is the default spacing, with no browser global overrides. Mindmap does not reproduce browser sanitization, global theme/config state or random diagram IDs. Edited Mindmap dimensions and coordinates are exported by `layoutData()`; `diagram()` continues to use Native text measurement and layout. External icon fetching and CSS painting remain outside this batch. Existing text parsers and renderer behavior are unchanged.

The previous TreeView model adapter already covered 12 of these 18 assertions; do not count those twice. The candidate coverage delta is 17 newly integrated assertions (11 Mindmap + 6 TreeView), contingent on validation and merge. The batch import inventory remains a snapshot of what was unintegrated when PR246 landed.
