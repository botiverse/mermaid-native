# Native Journey document operations

This harness runs the entire unchanged upstream `journeyDb.spec.js`: nine test occurrences, including duplicate names in two clear/reset groups. It verifies source hashes in both the pinned checkout and the imported original copies.

The reference execution records direct database operations and exact arguments. The JVM bridge replays them against `JourneyDocument`; the adapter returns those Native snapshots to the original assertions. No Mermaid source is generated and JavaScript does not calculate tasks, actors, ordering or reset state. Traces are keyed by both test name and occurrence, and every operation must be consumed.

Five negative controls exercise stale tasks, stale accessibility title, task score, actor ordering and section ordering. Reset controls target both duplicate original occurrences, rather than silently merging them into one test.

Build `:mermaid-core:bundleLibRuntimeToJarDebug`, then run:

```sh
python3 compatibility/upstream-journey-document/run.py \
  --upstream /path/to/pinned/mermaid \
  --stdlib /path/to/kotlin-stdlib.jar \
  --verify-mutations
```

The runner uses the checkout's existing pnpm/Vitest dependencies. Results, traces, JAR hash and mutation reports are written under `.native-journey-document-audit`. `validation.json` records the tested source and artifact.

## Native scope

The document owns section insertion order, task data, distinct sorted actors, title/accessibility metadata and reset. It projects directly into the actual Native `UserJourneyDiagram` renderer, including repeated section names and tasks before the first named section. Returned snapshots remain detached after edits and clear.

This is the nine-assertion DB file, separate from the existing Journey parser harness. Native reads are intentionally idempotent and actors are available immediately: this does not reproduce upstream's untested repeated-`getTasks()` duplication or deferred actor-population side effects. Scores use the existing Native integer model; arbitrary JavaScript numeric values and browser-global configuration remain outside this batch. No SDK release.
