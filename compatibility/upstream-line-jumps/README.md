# Original line-jump assertions

Pinned upstream: `04ee3364045d6573f84034d3c9368cc50233a92f`.
Run `run.py --upstream <checkout> --stdlib <kotlin-stdlib.jar> --verify-mutations` after building the Native Android runtime jars for core/layout-api/layout-simple. Requires the upstream pnpm dependencies and Java.

Both unchanged files are hash-checked against the batch-import originals. The reference pass records complete calls. The JVM bridge invokes Native algorithms and rendered-attribute patching, compares all 26 results, and replays those Native results into the original tests: 22 unit assertions plus one crossing-detection integration assertion. The integration fixture is routed by upstream; only its crossing detection is replaced, so this is not evidence of complete layout parity. DOM lookup/writes are transport, not a copied implementation.

Four mutation groups corrupt crossing, path, predicate, and DOM results to demonstrate the original assertions consume Native output. Supplemental Kotlin tests target weak original branches: the original neo-dash test changes the vertical/non-jumping edge, and its data-points example also targets an unchanged edge. Neither alone proves recomputation or geometry precedence on the changed edge.

No original source files are modified, skipped, deleted, or credited solely by running the reference.
