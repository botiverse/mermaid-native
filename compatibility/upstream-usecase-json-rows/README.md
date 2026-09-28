# Original Usecase JSON row assertion

Runs the single unchanged upstream test `flattens JSON rows depth-first using source property order` from pinned `usecase.spec.ts`. Source hashes are checked; the other38 assertions in that file are unintegrated and not counted by this harness.

The official run captures the committed JSON value and pointer order. Java decodes that transport into Native typed values and calls `UsecaseJsonTable.rows`, the same projector consumed by the actual Native table layout. The adapter replaces only `jsonRows`; the selected original assertion observes those Native rows. JS database construction and unrelated LayoutData fields remain upstream and are not claimed as Native coverage.

Run `python3 compatibility/upstream-usecase-json-rows/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar` after building the core runtime JAR. This is row projection coverage, not full Usecase shape/AST/security/HTML parity. Native cells are plain DrawText and never interpreted as HTML.
