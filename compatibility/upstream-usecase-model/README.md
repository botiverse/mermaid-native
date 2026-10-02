# Original Usecase production model assertions

Replays 23 unchanged original assertions from pinned `usecase.spec.ts`: basic actors, use cases, relationships, simple boundaries, direction, actor metadata, complex diagrams and class definitions. The explicit boundary assertion also checks the Native boundary type and ordered parent links. The database lifetime/LayoutData/AST/style assertions remain excluded (16 unselected tests, not passes or skips for Native coverage). Source SHA-256 is verified before running.

The official run captures `Diagram.fromText` inputs. The Java bridge calls the real `MermaidParser.parse`, serializes its typed actor/relationship fields, node collections, boundary type and parent links, direction and class definitions, and renames sourceId/targetId for transport. It does not parse syntax, resolve metadata, or construct layout. Reflection supports measuring the prior production runtime: absent actor metadata defaults to the old normal figure and absent relationship type remains null.

The adapter supplies these Native results to the original getter assertions; it does not run the JS parser/model builder in the Native pass. JS database lifecycle, complete normalized AST and HTML/icon-pack rendering are not covered. Actual Native variant rendering is separately checked through production parser-to-scene tests and the Wasm page.

Run after the core Android runtime JAR is built:

```sh
python3 compatibility/upstream-usecase-model/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar --verify-boundary-mutations
```

Boundary membership is projected from the ordered Native `attributes` map by `parentId`; the bridge never examines the source text or invokes the upstream model builder. This is model coverage, not new renderer or complete AST coverage.

With `--verify-boundary-mutations`, the harness separately corrupts the transported boundary type and membership. Each must fail only the explicit-boundary original assertion; it then restores the production results and requires all 23 assertions to pass again. The mutation reports are negative controls, never Native passing coverage.
