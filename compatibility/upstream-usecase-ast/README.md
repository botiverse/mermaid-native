# Direct Native Usecase parser and source AST

The complete unchanged `usecase.parser.spec.ts` executes against the production Kotlin `UsecaseDocument` and parser. The current batch addresses the six remaining semantic/AST assertions: relationship endpoint rules, class/style targets, invalid note targets with both source locations, typed edge animation metadata, and serializable JSON/relationship/internal-note AST fields. Earlier batches provided the other 71 passes. Eighteen of the 77 original assertions were already covered by the model harness; do not count them again.

The document pairs the actual renderer model with a detached source AST and clears both before parsing and after failure. Relationship fields retain arrow direction, minimum length, explicit IDs, label type and animation state. Animation state is published in the model and AST; this does not add animated rendering to Native backends.

```kotlin
val document = UsecaseDocument()
val result = document.parse(source)
val syntax = document.ast?.asMap()
document.clear()
```

The reference run records all 180 external operations (102 parses and 78 clears), including repeated inputs. A Java bridge executes them directly on the Native document. It transports production data and errors without parsing source, computing spans, validating semantics or substituting reference results. Replay queues fail closed on input/order mismatch. Fourteen mutation controls cover publication, token/origin locations, relationship/target diagnostics, animation in both consumers, and JSON AST values.

```sh
python3 compatibility/upstream-usecase-ast/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar --verify-mutations
```

Build `:mermaid-core:bundleLibRuntimeToJarDebug` first. The full independent lexer suite is in `compatibility/upstream-usecase-tokenizer`. Passing this parser file does not imply complete grammar, DB, renderer, layout, browser configuration or animation parity. The separate model harness still selects 43 of 116 assertions. No SDK is published by this batch. See `validation.json` for archived results and source pins.
