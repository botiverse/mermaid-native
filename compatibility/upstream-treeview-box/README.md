# Original TreeView preprocessing and consumer tests

Pinned upstream revision: `04ee3364045d6573f84034d3c9368cc50233a92f`.
The source hashes in `sources.json` must match before the unchanged original suites run.

`run.py --upstream PATH --stdlib PATH` uses the already-built Native Android JVM runtime JAR, captures original calls, then reruns the same assertions through Kotlin. It does not build Gradle itself.

The 42 preprocessing assertions run against `TreeViewBoxDrawing` (format detection, normalization and diagnostic line mapping). The 33 parser consumer assertions run against the real `MermaidParser` and project its tree nodes, annotations and hierarchy into the original database shape. No original database hierarchy implementation is reused. These are separate evidence categories: preprocessing passes are not renderer coverage. Original grammar coverage remains in `../upstream-treeview`.

The visual renderer retains Native tree colors, synthetic root and connector design; icon references are text labels, not downloaded icon packs. No arbitrary CSS execution is introduced.
