# Original TreeView model assertions through Native

Replays twelve unchanged pinned upstream DB assertions: nine hierarchy/annotation cases and three metadata cases. Setter inputs become TreeView source; production Kotlin parses indentation and creates `parentIndex`. The Java bridge serializes those parent indices as nested JSON. It does not compute a parent stack or derive hierarchy from whitespace. Actual Native drawing uses the same model parent indices for connectors.

Six cases remain unselected: clear/reset, empty-root initialization, two count cases, generated IDs and configuration. The adapter does not claim DB lifecycle/ID parity, CSS painting or external icon resolution. Annotation preservation is tested as model data.

Run `python3 compatibility/upstream-treeview-model/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar`. Optional `--runtime-jar` uses an already validated production JAR with identical core sources; its hash is recorded. No Gradle process is started.
