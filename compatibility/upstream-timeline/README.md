# Original Timeline parser assertions

Runs all 11 unchanged original Timeline parser cases (10 unique sources), pinned to04ee3364045d6573f84034d3c9368cc50233a92f. Checks source hashes, runs the original reference, snapshots the Kotlin JAR, then projects Native typed periods, event text, ordered sections and directions into the original TimelineDB. The original assertions are unchanged; this checks parser semantics, not Native rendering parity or independent DB internals.

Run `python3 compatibility/upstream-timeline/run.py --upstream /path/to/pinned/mermaid --stdlib /path/to/kotlin-stdlib.jar`. `--skip-build` uses a preceding build but still snapshots and hashes the JAR. Reports live in `.native-timeline-audit` under the upstream checkout.
