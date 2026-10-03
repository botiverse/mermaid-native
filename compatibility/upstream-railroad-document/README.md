# Direct Native Railroad database operations

Runs all 12 unchanged assertions in pinned `railroadDb.spec.ts` against production `RailroadDocument`. The reference run records operations and raw inputs; a JVM bridge executes them directly in Kotlin. Native results are replayed through getters, with exact operation/argument matching and an unused-operation check. There is no generated Mermaid source, JavaScript rule storage, title cleanup or reference implementation fallback.

The complete file only constructs terminal expressions. The bridge accepts precisely that input shape and rejects unsupported expressions or fields. The production API accepts existing typed `RailroadNode` expressions; recursive copying/text handling has separate Kotlin tests. An absent Kotlin rule maps to JavaScript `undefined` at the transport boundary.

```sh
python3 compatibility/upstream-railroad-document/run.py \
  --upstream /path/to/pinned/mermaid \
  --stdlib /path/to/kotlin-stdlib.jar \
  --verify-mutations
```

Build `:mermaid-core:bundleLibRuntimeToJarDebug` first. The runner verifies both original source files and their imported copies, and uses upstream pnpm/Vitest dependencies. It writes raw results, call traces, JVM snapshots, six mutation reports and a summary under `.native-railroad-document-audit` in that checkout. No Gradle process is started. Mutation controls cover duplicate retention, last-definition lookup, metadata clear, title/rule cleanup and absent lookup; each must fail exactly its original assertion.

## Production scope

`RailroadDocument` owns ordered rule storage, last-definition lookup, all metadata and clear/reset. Duplicate names retain both rules for drawing while lookup returns the newest. Input trees and output snapshots are copied so caller-owned mutable lists cannot modify stored rules. `diagram()` exports the existing model to actual Native layout, with an integration test comparing complete draw commands and checking duplicate rendering.

Native labels are plain text. The document removes script/style text blocks, including their contents; it is **not an HTML sanitizer** or a DOMPurify implementation. Browser global configuration, arbitrary HTML cleanup, comment fields and repeat separators are outside this API. Other markup stays literal. No existing text parser, renderer or SDK version changes in this batch.

Five title/accessibility assertions were already covered by the older `upstream-railroad-model` adapter. The coverage candidate is seven newly integrated occurrences; those five must not be counted twice.
