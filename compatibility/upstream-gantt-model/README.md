# Original Gantt task model assertions

Runs 12 unchanged pinned `ganttDb.spec.ts` assertions for explicit dates, durations, generated IDs and relative starts/ends across sections or multiple dependencies. The other37 assertions are unselected and are not counted by this harness. The seven duration/millisecond assertions covered by the separate duration harness retain their own coverage category.

Both original and Native replay use `TZ=UTC`, matching Native's deterministic date-only interpretation. This does not establish timezone or DST compatibility. Source SHA-256 is checked before execution.

The original run captures the test's clear/setDateFormat/addSection/addTask call sequence as Mermaid source. The Java bridge calls the actual production MermaidParser and serializes its task IDs, names and resolved timestamps. The adapter only projects these timestamps into Date objects. It neither parses the task data nor resolves dependencies or durations in JS. Reflection falls back to the prior runtime's whole-day model only for measuring the historical baseline.

This is additional original coverage of the existing production resolver. No production implementation or layout changes are made by this harness.

```sh
python3 compatibility/upstream-gantt-model/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar --repo /path/to/built/native/repo
```
