# Original C4 parser assertions

Runs all 127 unchanged original C4 Jison spec assertions at the pinned revision. Native Kotlin parses macros, variants, named arguments and boundaries. The bridge projects Native elements and boundaries into the exact getters tested by upstream, preserving object identity for boundary relation targets. No upstream parser or database builder supplies product state.

Build core, then run `python3 run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib-2.1.21.jar --skip-build`. Source hashes, both reports, captured inputs, projected models and exact JAR are retained in `.native-c4-audit`.

These are parser/model boundary assertions, not layout or screenshot equivalence. Actual Native tests cover nested containment, technology labels and boundary edge endpoints. Native keeps its green/blue/amber card styling. Generic C4 headers are accepted, but Deployment_Node, layout/style commands, clickable links, sprite rendering and complete C4 deployment/dynamic semantics are not claimed. Named label attributes remain typed metadata and their values render as text; tags/links/sprite names are retained. The original C4 getter tests do not establish full original sanitization or configuration parity.
