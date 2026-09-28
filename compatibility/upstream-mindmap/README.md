# Original Mindmap assertions

Runs all 26 unchanged original mindmap.spec.ts assertions at the pinned revision through the Native Kotlin parser. The bridge projects Native node identity, parent links, shape enum and decorations; the adapter only arranges that flat tree for the original getMindmap() accessor. Original hierarchy logic is not used.

Build core, then run `python3 run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib-2.1.21.jar --skip-build`. Source hashes, both reports, captured inputs, exact Native JAR and projected output are retained under `.native-mindmap-audit`.

The 26 assertions cover the parser boundary, not original layout or visual equivalence. Native keeps unique rendering IDs separately from source IDs and retains its existing circle/rectangle styles. Cloud, burst and hexagon get Native outlines; icon names are rendered as measured text. Arbitrary icon packs, arbitrary CSS execution, full Markdown rendering, sanitization parity and recovery AST are not claimed. The existing reserved internal ID prefix remains rejected.
