# Original Railroad title/accessibility model assertions

Five unchanged assertions from pinned `railroadDb.spec.ts` run against production `MermaidParser` and `RailroadDiagram` fields. Setter calls serialize title and accessibility metadata into Mermaid source; the existing Railroad Java bridge projects the actual Native model. Getters return those fields (null optional text maps to the upstream empty string convention). No title sanitization or whitespace algorithm is implemented in the adapter.

The other seven assertions remain unselected: rule lookup/storage/duplicates, missing rule lookup, sanitization, and database clear lifecycle are not claimed. Setup resets the input builder only. No JS database lifecycle coverage is inferred. The returned title is drawn by `layoutRailroad`; accessibility fields flow to SVG metadata.

Use `python3 compatibility/upstream-railroad-model/run.py --upstream <pinned checkout with dependencies> --stdlib <kotlin-stdlib.jar> --runtime-jar <validated production core jar>`. The runner does not launch Gradle. Preserve the runtime hash and source revision with results.
