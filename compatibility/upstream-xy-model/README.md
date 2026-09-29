# Original XY band-data model assertions through Native

Runs all six unchanged pinned `xychartDb.spec.ts` assertions. The adapter converts setter inputs into chart source; production Kotlin `XySeries.valuesFor` determines category slots, missing values and truncation, and `displayTitle` trims legend titles. Both axis inference and actual vertical/horizontal drawing consume these model operations. The Java bridge projects these results; no JavaScript truncation, title trimming or range calculation is used.

The full original grammar suite remains a separate regression. Raw parser title text is preserved; this suite checks the processed title used in drawing. Missing category values are encoded as JSON null (the selected assertion checks slot count, not undefined identity). No extra HTML sanitizer or broader configuration parity is claimed.

Run `python3 compatibility/upstream-xy-model/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar` after building the production core JAR. Optional `--runtime-jar` identifies another source-matching JAR; its hash is recorded. The runner never launches Gradle.
