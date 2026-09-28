# State upstream boundary audit 

The runner verifies the pinned revision and SHA-256 hashes in sources.json, then executes the four original State parser/v1/v2/style test files without changing assertions. Reference execution captures source inputs; a snapshot of the compiled Native core JAR parses every unique input. The adapter projects only returned typed StateDiagram data into the actual upstream StateDB. It must not reparse the source or call the official parser for Native results.

The StateDB extraction, normalization, configuration and renderer-derived assertions remain upstream behavior, not Native rendering coverage. Direct StateDB tests and parser-negative cases must be classified separately in the assertion ledger. A successful boundary test alone does not prove Native styles, layout or interaction behavior. Actual Native consumer tests and browser evidence are required independently.

Baseline: original 101 passed / 1 skipped, Native 23 passed / 78 failed / 1 skipped over 97 unique inputs. The refined core candidate passes76/fails25/skips1; remaining cases cover classes/styles, concurrent regions, floating notes and multiple tokens on a line. This is not complete State parity.

Run `python3 compatibility/upstream-state/run.py --upstream upstream-mermaid --repo mermaid-state --skip-build --stdlib PATH_TO_KOTLIN_2_1_21_STDLIB` only after building the current core JAR. The script snapshots the JAR and records its SHA-256 in `.native-state-audit/summary.json`.
