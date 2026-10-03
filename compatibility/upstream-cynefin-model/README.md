# Original Cynefin model assertions through Native

Runs seven unchanged `Cynefin Database` tests from the pinned upstream source: domain blocks/items, transitions, empty labels and self-loop filtering. The adapter translates setter inputs to Cynefin source, then reads production `MermaidParser` results through the existing `CynefinNativeBridge`. Filtering, label normalization and domain construction happen in Kotlin, not in JavaScript.

The other 20 tests remain unselected: four database lifecycle/configuration cases and sixteen boundary/seed cases. Native currently draws its own domain cards; this runner does not claim upstream boundary geometry coverage or DB setter timing parity.

Use `python3 compatibility/upstream-cynefin-model/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar`. A previously validated, source-matching production core JAR may be passed with `--runtime-jar`; the runner never starts Gradle. Its SHA-256 is recorded with both original and Native results. Production code is unchanged by this addition.

The complete 27-test DB/boundary file now has a direct production-state and geometry runner in [`upstream-cynefin-boundaries`](../upstream-cynefin-boundaries/README.md). Its seven overlapping DB assertions must not be counted again.
