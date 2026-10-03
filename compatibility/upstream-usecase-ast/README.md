# Original Usecase source/publication assertions

Runs the complete unchanged, hash-pinned `usecase.parser.spec.ts`: 77 reference assertions pass; the Native document yields 51 passing and 26 failing, with zero skipped. Eighteen passes were already covered by upstream-usecase-model; the source-AST baseline added 22 passing and 37 failing assertions. Declaration validation subsequently converted 11 failures to passes, leaving 33 passes and 26 failures beyond the original 18. See failures.md for the remaining contract gaps.

The production `UsecaseParser` records half-open UTF-16 ranges while consuming source. Its `sourceAst` is published only after successful `parseValidated()`. `UsecaseDocument` pairs that AST with the existing renderer model, clears both before parsing and on failure, and supports explicit clear. `UsecaseSourceAst.asMap()` returns a detached JSON-compatible snapshot. Renderer model equality and fields remain unchanged.

```kotlin
val document = UsecaseDocument()
val result = document.parse(source)
val syntax = document.ast?.asMap()
document.clear()
```

Supported snapshot data includes source/header, ordered statements, nested boundary children, node/JSON-ID occurrences, label/metadata/stereotype spans, and a basic diagram projection. This is not the complete upstream AST or lexer contract. Other exact semantic diagnostics, some grammar rejection, animation metadata, complete JSON/edge AST fields and the independent lexer suite remain incomplete. A version1-shaped snapshot does not imply full upstream version1 compatibility.

The reference pass records all external `clear` and `parse` calls, including repeated sources. A Java bridge executes all 180 operations (102 parses,78 clears) on the actual production Native document and serializes snapshots. It does not parse source or synthesize AST ranges. The Native replay replaces parser/DB/common getters with these snapshots; per-test operation queues fail closed on source/op mismatch. Failure messages come directly from Kotlin. JavaScript getters only project transported records; there is no upstream parser, AST, accessibility or publication-state fallback.

Some failing assertions stop before consuming all captured operations for their test. All reference operations are still executed in Native; their results are not counted as passing unless the unchanged assertion passes. Per-test queues prevent this early stop from shifting subsequent tests. Tests remain failures, never skips.

Four negative controls change the header range, the `son` JSON identifier range, the publication snapshot following an invalid parse, or the previous-origin text in a collision diagnostic. Each causes exactly its targeted previously passing original assertion to fail; restoring transport returns 51/26. Existing 43 original model assertions and 299 core/277 layout/five sample tests pass. These are independent checks and are not all additional original coverage.

```sh
python3 compatibility/upstream-usecase-ast/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar --verify-mutations
```

Build `:mermaid-core:bundleLibRuntimeToJarDebug` first. The script intentionally exits nonzero while 26 failures remain. No SDK publication is included.
