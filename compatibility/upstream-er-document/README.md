# Native ER document cluster contract

The complete unchanged `erDb.spec.js` contains eight assertions covering cluster identity, first-owner membership, nested parents, cluster relationship endpoints and styles/classes. The recorder captures direct calls, suppressing nested calls within the reference DB so each API operation is replayed once on the real Native `EntityRelationshipDocument`. Arguments and consumption order are checked. No Mermaid source is manufactured and no business state is computed by JavaScript.

Native document data owns the generated entity identities and graph parent/edge projection. Its actual `EntityRelationshipDiagram` uses stable input names, with explicit conversion at the Native data boundary. This avoids changing the existing text parser's ID convention. Both projections use the same stored members, styles and relationships.

Five corruption controls independently verify identity, duplicate ownership, nested parents, cluster edge endpoints and styles. Build `:mermaid-core:bundleLibRuntimeToJarDebug`, then run `run.py --upstream /path/to/upstream --stdlib /path/to/kotlin-stdlib.jar --verify-mutations`.

Scope is these eight original assertions and the tested Native renderer projection. This does not claim full browser `getData` fields, global configuration, DOM sanitization or CSS rendering parity. Duplicate subgraph IDs and cycles are rejected by the Native editor API; all original duplicate-ID behavior is outside the eight-assertion file. No SDK release.
