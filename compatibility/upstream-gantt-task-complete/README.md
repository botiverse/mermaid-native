# Original Gantt task behavior assertions

Replays all 27 task-behavior assertions enabled in UTC in the pinned original ganttDb.spec.ts: dates, durations, IDs, ordering, dependencies, weekend calendars, inclusive ends, diagnostics, and format fallbacks. The daylight-saving assertion remains skipped under UTC, and 21 duration/database-configuration assertions are outside this harness. Existing 13 task assertions overlap earlier harnesses and must be deduplicated by original test occurrence in the coverage ledger.

The unmodified original test file is SHA-256 verified. Original DB mutations serialize to Mermaid source; the production MermaidParser resolves it. Java projects actual task fields, list order and diagnostics; JavaScript only wraps epoch timestamps as Date objects. No dates, dependencies, excluded calendars or durations are computed by the adapter. Two newly exposed model fields retain nullable render end and explicit end-date provenance. A legacy reflection fallback supports baseline measurement only.

Native date-only values and the current-day fallback use UTC; this does not claim local timezone/DST compatibility. Both replay processes run with TZ=UTC. The original seconds-only case asserts task count/ID/name, not timestamps: Native interprets 0..59 as seconds on the current UTC day; the upstream JavaScript fallback for a one-digit `0` instead starts in year2000. Passing this case does not establish equality of that fallback timestamp.

```sh
python3 compatibility/upstream-gantt-task-complete/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar --repo /path/to/built/native/repo
```
