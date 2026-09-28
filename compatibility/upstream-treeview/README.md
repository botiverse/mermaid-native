# Original TreeView grammar assertions

Runs all 28 unchanged original `packages/parser/tests/treeView.test.ts` assertions at the pinned revision through the real Native `MermaidParser`. The bridge projects raw indentation, names, annotations, and metadata from the Kotlin product model. The adapter replaces only Langium parse results; original hierarchy logic is not reused. Native parents are computed by nearest lower indentation and rendered using the existing synthetic-root/connector design.

Build core, then run `python3 compatibility/upstream-treeview/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib-2.1.21.jar`. Source hashes, inputs, runtime JAR, and both reports are retained under `.native-treeView-audit`.

Native supports empty trees, unindented roots, arbitrary space/tab indentation, quoted and bare Unicode labels, annotations and metadata. Icon references and descriptions are rendered as measured secondary text; class names are retained without executing arbitrary CSS. Box-drawing input preprocessing, icon packs, original TreeView DB tests and original layout/visual equivalence remain separate coverage.
