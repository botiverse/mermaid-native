# Original Usecase production model assertions

Replays 25 unchanged original assertions from pinned Mermaid: 23 from `usecase.spec.ts` and two from `usecase.parser.spec.ts`. The additional parser assertions check numeric-leading actor/usecase/JSON IDs and relationships, and preservation of decimal style values. Source SHA-256 is verified before running.

The original pass captures `Diagram.fromText` and the two direct `parser.parse` inputs. The Java bridge calls production `MermaidParser.parse` and projects its actor, usecase, boundary, relationship, JSON-node ID, style, direction, and class fields. Native ordered style maps are serialized as `key:value` entries and parent links as member IDs; the bridge neither parses source nor computes expected values. The Native pass replaces both parsing entry points and exposes only the transported results to the original getters, with no upstream parser/model fallback.

The 91 other assertions across these two files remain unselected: 16 from the general suite and 75 from the parser publication/AST suite. Database lifetime, complete normalized AST, source spans, and HTML/icon-pack rendering are not covered. The JSON projection checks IDs only; JSON payload parsing has separate coverage.

Run after building the core Android runtime JAR:

```sh
python3 compatibility/upstream-usecase-model/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar --verify-model-mutations
```

The mutation option independently corrupts boundary type, membership, a numeric actor ID, and decimal styles. Each must fail only its targeted original assertion; restoration must return all 25 tests to passing. These negative controls are not additional coverage. `--verify-boundary-mutations` remains an alias for existing callers.

This is parser/model coverage; no production rendering code changes are required, and it does not imply renderer parity. Reflection for older actor metadata remains supported; absent actor metadata defaults to the old normal figure and absent relationship type remains null.
