# Original Sankey parser assertions

Runs all six unchanged original Sankey assertions, with six captured inputs including the complete energy CSV, prototype-like node identifiers, cycles, parallel rows and punctuation. Shared Kotlin supplies the nodes/links; the bridge projects those getters without the original parser or database builder. Source hashes and exact JAR receipts are retained.

Build core, then run `python3 run.py --upstream /path/to/upstream --stdlib /path/to/kotlin-stdlib.jar --skip-build`. Native retains finite positive values and rejects empty/malformed CSV; these six assertions do not establish full CSV or sanitization equivalence. Parser/model assertions are separate from rendering. Native preserves the existing blue boxes and weight-based line widths, with dedicated return lanes for feedback and self links. It does not claim D3 Sankey geometry or screenshot parity.
