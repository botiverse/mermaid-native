# Original Usecase production model assertions

Replays 43 unchanged original assertions from pinned Mermaid: 25 from `usecase.spec.ts` and 18 from `usecase.parser.spec.ts`. Six parser assertions check bracketed and parenthesized boundary titles as unquoted text, plain strings, and Markdown strings. Boundary label types are read directly from the production Native model. The parser assertions also compare declaration-first and relation-first actor/rectangular-usecase refinement. The additional parser assertions check numeric-leading actor/usecase/JSON IDs and relationships, preservation of decimal and multi-word style values, and later boundary metadata overriding inline metadata. The general suite also checks two existing actors/usecases by ID. Source SHA-256 is verified before running.

The original pass captures `Diagram.fromText` and the selected direct `parser.parse` inputs. The Java bridge calls production `MermaidParser.parse` and projects its actor, usecase, boundary, relationship, JSON-node ID, style, direction, and class fields. Native ordered node and class-definition style maps are serialized as `key:value` entries and parent links as member IDs; the bridge neither parses source nor computes expected values. The Native pass replaces both parsing entry points and exposes only the transported results to the original getters, with no upstream parser/model fallback.

The 73 other assertions across these two files remain unselected: 14 from the general suite and 59 from the parser publication/AST suite. Database lifetime, complete normalized AST, complete source spans, and HTML/icon-pack rendering are not covered. The JSON projection checks IDs only; JSON payload parsing has separate coverage.

Run after building the core Android runtime JAR:

```sh
python3 compatibility/upstream-usecase-model/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar --verify-model-mutations
```

The mutation option independently corrupts boundary type, membership, a numeric actor ID, decimal styles, an overridden boundary type, spaces in a class-definition value, the actor collection after a forward reference, the rectangular shape after a forward reference, a Markdown boundary title type, and a plain boundary title type, boundary classes, the membership of a separate boundary, unresolved endpoint shape, repeated-declaration parent ownership, anonymous edge IDs, and anonymous note IDs. Each must fail only its targeted original assertion; restoration must return all 43 tests to passing. These negative controls are not additional coverage. `--verify-boundary-mutations` remains an alias for existing callers.

Two further assertions check complete explicit-boundary fields and separate derived/plain boundaries. Classes and style lists are projected from production attributes; the parser records the default rectangular boundary type while retaining any explicit metadata override.

This is parser/model coverage. Native boundary models preserve the parsed text/Markdown label type; rendering is unchanged, and this does not imply Markdown rendering parity. Reflection for older actor metadata remains supported; absent actor metadata defaults to the old normal figure and absent relationship type remains null.

The ID getter assertions are model-content checks: JavaScript selects a transported Native record by ID. They do not count as verification of a Native database lookup API or database lifecycle. Empty-database and clear/reset assertions remain excluded.

Forward-reference comparisons use the serialized Native model collections and Native shape enum (`RECTANGLE` is transported as `rect`). Their note collections are empty. The adapter now transports Native note IDs, labels and targets directly; it never generates IDs. This does not add complete AST or renderer coverage.

Two complete node-model assertions additionally check unresolved relation endpoints and equivalent repeated declarations within a boundary. Node classes, styles, stereotypes, parents and business flags come from production fields/attributes. Actor and usecase label types are read directly from the production model. Absent optional icon/stereotype/parent fields are omitted rather than serialized as null, matching their optional contract. This does not project a normalized AST or implement Web database lifetime behavior.

One additional unchanged original assertion checks deterministic anonymous edge and note IDs across repeated parsing. Production Native parsing assigns independent, source-ordered `edge-N` and `note-N` sequences; explicit edge IDs do not consume the anonymous sequence. Three corruption controls alter a transported edge ID, note ID, or only the second parse result and must fail only this assertion. Native regressions additionally cover explicit IDs interleaved with anonymous edges, numbering across fresh successful/failed parses, and unchanged legacy/extended drawing commands. This does not claim JavaScript singleton database lifecycle coverage.

Every captured original parse invocation is executed against the Native parser, including repeated identical input. The adapter consumes a queue of actual Native results for each source instead of reusing a de-duplicated model. Missing/exhausted invocations fail closed. A control corrupting only the second anonymous-ID result proves that the unchanged original repeat-parse assertion observes that invocation.

One unchanged original lexer-failure assertion checks rejection and the location-bearing diagnostic for an unterminated quoted actor label. The bridge transports the production diagnostic message without synthesizing any error text or offsets. Two negative controls shift its column or suppress the failure. Native regressions additionally cover CR/LF/CRLF, an earlier Unicode comment, multiline Markdown labels, plain physical-newline rejection and restored valid input. Native reports a half-open UTF-16 range from the opening quote to the consumed failure position; the original assertion only constrains the start line/column and the numeric range format, so this does not establish exact upstream lexer recovery/end-span or complete AST-span parity.
