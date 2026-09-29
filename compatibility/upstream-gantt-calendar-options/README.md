# Original Gantt calendar option assertions

Replays the three unchanged original `ganttDb.spec.ts` assertions for repeated includes/excludes token merging and deduplication. Source hashes and the selected count are checked. The other46 assertions are unselected by this harness; existing duration and task-model coverage stays separate.

The original calls are serialized into repeated Mermaid directives. The Java bridge calls the actual Native MermaidParser and returns GanttDiagram.excludes/includes. The adapter performs no token splitting, case normalization, deduplication or calendar computation. The normalized options are consumed by the production date resolver; a parser regression also checks that uppercase WEEKENDS advances a Friday task through the weekend.

```sh
python3 compatibility/upstream-gantt-calendar-options/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar --repo /path/to/built/native/repo
```
