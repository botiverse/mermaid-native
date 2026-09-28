# Original Gantt parser assertions

Runs the unchanged 28 original Gantt parser cases at upstream revision04ee3364045d6573f84034d3c9368cc50233a92f (34 unique inputs), first against the original parser and then against the actual Kotlin parser. Source hashes are checked before execution. The Java bridge serializes typed Native fields; the adapter projects Native-resolved dates, tags and task labels directly into getTasks, with append/clear behavior retained between parse calls. It does not ask the original DB to calculate dates, so the DB cannot conceal Native date/dependency defects.

Metadata and interaction statements call the original GanttDB setters for unchanged spy assertions. These are parser-boundary checks, not evidence of Native interaction binding, calendar-axis rendering, or full visual parity. The 49 direct GanttDB tests remain outside this adapter.

Run `python3 compatibility/upstream-gantt/run.py --upstream /path/to/pinned/mermaid --stdlib /path/to/kotlin-stdlib-2.1.21.jar`. A preceding build can be reused with `--skip-build`; every run snapshots and hashes the actual JAR. Reports live in the upstream checkout's `.native-gantt-audit` directory.
