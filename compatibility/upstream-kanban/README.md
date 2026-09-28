# Original Kanban parser assertions

Runs all 30 unchanged assertions from pinned upstream kanban.spec.ts through the Native Kotlin parser. The bridge projects typed Native columns, flattened cards, metadata and diagnostics; it replaces the database getters used by these tests, so no upstream parser or hierarchy builder supplies expected product data. Original configuration and rendering are not counted as Native coverage.

Run `python3 run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib-2.1.21.jar --skip-build` after building mermaid-core. Source hashes are checked before either run. Outputs under `.native-kanban-audit` include both reports, captured inputs, models, and the exact JAR.

Native retains class metadata but does not interpret arbitrary CSS classes. Metadata values are plain strings rather than a complete YAML implementation; full YAML schema, sanitization parity, recovery AST, arbitrary icon-pack rendering and screenshot equivalence are not claimed. The original 30 tests cover the scoped grammar and metadata assertions; direct Native layout tests cover visible metadata and empty columns.
