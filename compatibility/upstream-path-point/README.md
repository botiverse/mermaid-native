# Upstream path-point assertions

Runs the five unchanged `calculatePoint` tests in pinned `utils.spec.ts`, including
inline snapshots and the out-of-range error. Other utils tests are unselected.

Build `:mermaid-layout-simple:testDebugUnitTest` and run:

```sh
python3 compatibility/upstream-path-point/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar
```

The bridge passes numbers to production `MermaidPathGeometry.pointAt`; no Java,
Python or JavaScript geometry implementation is used. The flowchart renderer
consumes the same function via `midpoint` to position edge labels. Original
source hashes are checked before Vitest runs. Native's explicit handling of
repeated points, singleton paths and invalid distances has separate unit tests;
those are additional behavior, not part of the five original assertions.
