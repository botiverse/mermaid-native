# Original Usecase source/publication assertions

Runs the complete unchanged, hash-pinned `usecase.parser.spec.ts`: 77 reference assertions pass; the Native document yields 71 passing and six failing, with zero skipped. Eighteen passes were already covered by upstream-usecase-model; the source-AST baseline added 22 passing and 37 failing assertions. Declaration validation converted 11 failures to passes, then unquoted-label grammar validation converted another eight. Statement grammar diagnostics then converted nine failures. Metadata token validation then converted three failures. This leaves 53 passes and six failures beyond the original 18. See failures.md for the remaining contract gaps.

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

Seven negative controls change the header range, the `son` JSON identifier range, the publication snapshot following an invalid parse, the previous-origin text in a collision diagnostic, the actual unquoted-token diagnostic, or the unexpected JSON declaration location inside a boundary, or the actor metadata property column. Each causes exactly its targeted previously passing original assertion to fail; restoring transport returns 71/6. Existing 43 original model assertions and 309 core/277 layout/five sample tests pass. These are independent checks and are not all additional original coverage.

```sh
python3 compatibility/upstream-usecase-ast/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar --verify-mutations
```

Unquoted labels now reject nested delimiters, reserved relationship/class/metadata tokens and embedded quoted strings at their source location. Ordinary punctuation, quoted delimiters and valid single-quoted labels remain accepted; this does not claim complete single-quoted name or metadata support.

Build `:mermaid-core:bundleLibRuntimeToJarDebug` first. The script intentionally exits nonzero while six failures remain. No SDK publication is included.

Actor, usecase and boundary metadata are validated from every actual token occurrence, preserving quoted strings versus boolean literals and raw key spans. Invalid earlier values cannot be hidden by later overrides. Business geometry errors point to the first declaration. Edge/JSON metadata and the remaining six semantic/AST failures are outside this change.
