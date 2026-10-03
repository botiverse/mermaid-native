# Original Usecase production model assertions

Replays 37 unchanged original assertions from pinned Mermaid: 25 from `usecase.spec.ts` and 12 from `usecase.parser.spec.ts`. Six parser assertions check bracketed and parenthesized boundary titles as unquoted text, plain strings, and Markdown strings. Boundary label types are read directly from the production Native model. The parser assertions also compare declaration-first and relation-first actor/rectangular-usecase refinement. The additional parser assertions check numeric-leading actor/usecase/JSON IDs and relationships, preservation of decimal and multi-word style values, and later boundary metadata overriding inline metadata. The general suite also checks two existing actors/usecases by ID. Source SHA-256 is verified before running.

The original pass captures `Diagram.fromText` and the selected direct `parser.parse` inputs. The Java bridge calls production `MermaidParser.parse` and projects its actor, usecase, boundary, relationship, JSON-node ID, style, direction, and class fields. Native ordered node and class-definition style maps are serialized as `key:value` entries and parent links as member IDs; the bridge neither parses source nor computes expected values. The Native pass replaces both parsing entry points and exposes only the transported results to the original getters, with no upstream parser/model fallback.

The 79 other assertions across these two files remain unselected: 14 from the general suite and 65 from the parser publication/AST suite. Database lifetime, complete normalized AST, source spans, and HTML/icon-pack rendering are not covered. The JSON projection checks IDs only; JSON payload parsing has separate coverage.

Run after building the core Android runtime JAR:

```sh
python3 compatibility/upstream-usecase-model/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar --verify-model-mutations
```

The mutation option independently corrupts boundary type, membership, a numeric actor ID, decimal styles, an overridden boundary type, spaces in a class-definition value, the actor collection after a forward reference, the rectangular shape after a forward reference, a Markdown boundary title type, and a plain boundary title type. Each must fail only its targeted original assertion; restoration must return all 37 tests to passing. These negative controls are not additional coverage. `--verify-boundary-mutations` remains an alias for existing callers.

This is parser/model coverage. Native boundary models preserve the parsed text/Markdown label type; rendering is unchanged, and this does not imply Markdown rendering parity. Reflection for older actor metadata remains supported; absent actor metadata defaults to the old normal figure and absent relationship type remains null.

The ID getter assertions are model-content checks: JavaScript selects a transported Native record by ID. They do not count as verification of a Native database lookup API or database lifecycle. Empty-database and clear/reset assertions remain excluded.

Forward-reference comparisons use the serialized Native model collections and Native shape enum (`RECTANGLE` is transported as `rect`). Their note collections are empty: `getNotes` checks the production note count and fails for nonempty notes instead of inventing Web note IDs. This does not add note-identity, complete AST, or renderer coverage.
