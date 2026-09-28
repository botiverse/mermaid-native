# Original Architecture grammar assertions

Runs all 27 unchanged original `packages/parser/tests/architecture.test.ts` assertions at pinned revision `04ee3364045d6573f84034d3c9368cc50233a92f`. The Native Java bridge invokes shared Kotlin `ArchitectureParser.parse()` and projects its group/service labels and title/accessibility metadata. The adapter substitutes that result at the original Langium parser boundary, without reusing the original parser or hierarchy logic. Source hashes, input capture, exact runtime JAR, and both reports are retained in `.native-architecture-audit`.

Build core, then run `python3 compatibility/upstream-architecture/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib-2.1.21.jar`.

The original grammar tests allow a service referencing an undeclared group. This grammar boundary preserves that behavior; the real `MermaidParser` calls `parseValidated()` and rejects missing group/service references before layout. These are parser-boundary passes, not DB, Cytoscape, or visual-equivalence coverage.

Native keeps its measured blue service cards, group columns and port routing. It supports empty diagrams, metadata, quoted and unquoted labels, optional icons and labels, and forward references resolved at product admission. Junctions, alignment directives, nested groups, custom icon packs, edge titles/group ports, and left/bidirectional arrows remain unsupported. No consumer DB or original layout tests are counted here.
