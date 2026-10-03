# Native Usecase draft publication

Runs the complete, unchanged `usecaseDb.spec.ts` (four tests) from pinned upstream revision `04ee3364045d6573f84034d3c9368cc50233a92f`. Source hashes are checked against both the upstream checkout and the imported original file before execution.

The reference run records draft creation, each map/array/property mutation, commit, clear, title/accessibility operations and reads. The JVM bridge replays those operations on actual Kotlin `UsecaseDraft` and `UsecaseDocument` instances. In particular, edits **after** commit reach the original Native draft, so serialization alone cannot make the isolation assertion pass. The adapter returns Native snapshots to the original assertions; it neither generates Mermaid text nor calculates commit/reset results in JavaScript. Calls and arguments must match in order; unconsumed calls fail. Per-test draft identities isolate negative controls from later tests.

Five controls independently corrupt actor detachment, complete replacement, incomplete-model rejection, cleared direction and shape padding. Each must fail exactly its original test, followed by restoration of the passing baseline.

```sh
python3 compatibility/upstream-usecase-document/run.py \
  --upstream /path/to/pinned/mermaid \
  --stdlib /path/to/kotlin-stdlib.jar \
  --verify-mutations
```

First build `:mermaid-core:bundleLibRuntimeToJarDebug`. The runner uses existing pnpm/Vitest dependencies in the upstream checkout and writes its raw traces, Native JAR, reports and mutation results under `.native-usecase-document-audit`. `validation.json` records the tested source and artifact hash.

## Production behavior and limits

`UsecaseDocument.commit` validates an editable draft and copies all collections, nested styles, JSON values/property order and AST before publishing the new diagram and AST. An invalid draft leaves the prior commit intact; a complete replacement removes old collections. Clear and failed parse remove published state and metadata. Direct draft editing is a Native editor API; text parsing still uses the existing parser.

Both plain and extended layouts consume the same label inset contract as document clients: 20 units per side for ellipses, 10 for rectangles. Long rectangular labels can therefore use a narrower box; existing minimum dimensions still apply. Ellipses retain the separate geometric containment calculation for wrapped labels. A document's custom insets also reach actual drawing commands.

This batch covers these four original tests, not the separate `usecase.spec.ts` rendering/configuration suite. Native configuration here contains label insets; other layout settings remain in `LayoutConfig`. It does not reproduce browser-global configuration, DOM sanitization, CSS selectors, animated edge painting, or all browser `getData()` properties. The test bridge transports the tested subset of mutable operations and fails on unsupported operations. No SDK is released by this batch.
