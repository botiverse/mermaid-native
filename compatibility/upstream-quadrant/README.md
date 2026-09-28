# Original Quadrant parser assertions

Runs the pinned, unchanged `quadrant.jison.spec.ts`: 24 original tests, 46 distinct inputs. The reference parser captures inputs; the Native adapter projects shared Kotlin labels, axis presence, coordinate lexemes, classes and styles into the original mock database calls. It never parses the source again in JavaScript.

Run `python3 compatibility/upstream-quadrant/run.py --upstream /path/to/upstream --stdlib /path/to/kotlin-stdlib.jar --skip-build` after building `:mermaid-core:bundleLibRuntimeToJarDebug`. Omitting `--skip-build` performs that build. Original source hashes, runtime JAR SHA256, inputs and reports are retained in the upstream `.native-quadrant-audit` directory.

These are parser-boundary assertions. The original Quadrant database/layout unit tests and screenshot equivalence are separate coverage; no claim is made that this harness executes them.

Native reports typed diagnostics at the first invalid statement; it does not reproduce Jison's complete parse-error wording. Styles follow the original database's numeric, hex-color and pixel-width validation. Native keeps diamond markers and side-anchored point labels rather than the upstream circular markers.
