# Original Gantt duration assertions

Replays the six unchanged `when using duration` assertions and the original `should handle milliseconds` production-model assertion from pinned `ganttDb.spec.ts`. Source hashes are checked. The other42 original assertions (including mutable database lifetime and broader date/timezone behavior) remain unselected and are not counted as Native passes or skips.

The bridge calls `GanttDuration.parse`, the same Native duration parser used by the production Gantt resolver. The adapter only converts the transported NaN sentinel back to JavaScript NaN. The task adapter serializes original setDateFormat/addSection/addTask calls into source and projects the actual MermaidParser model into Date objects. There is no JS parsing, dependency resolution or unit calculation in the Native path.

Before this change the Native duration boundary was absent, so the six helper assertions were unavailable rather than six distinct product bugs. The original millisecond diagram also fails on the old production parser. Separate calls to that parser confirm that `1d`/`2w` work while `2h`/`30m`/`0.1s`/`1ms` are rejected. Production parser-to-scene tests cover millisecond dependencies, proportional bars, time ticks and retention of the existing day-only geometry and colors.

The resolver supports d/w/h/m/s/ms durations and x/X epoch date formats. Duration token parsing preserves M/y tokens, but calendar-month/year arithmetic, arbitrary date formats and timezone/DST compatibility remain pending. Date-only Native inputs use UTC midnight for deterministic calendar geometry.

```sh
python3 compatibility/upstream-gantt-duration/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar
```
