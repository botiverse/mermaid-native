# Swimlane direction geometry

The pinned Mermaid LR/RL/BT direction operations are implemented in
`SwimlaneDirectionGeometry`; production `placeSwimlanes` consumes the same
transform. Native then reserves upright, measured title bands, separates
sibling bounds, and aligns top-level lane spans. Styles and richer grammar
share this producer instead of switching to ordinary Flowchart placement.

Run the unchanged seven original direction assertions and mutation controls:

```sh
./gradlew :mermaid-layout-simple:testDebugUnitTest
python3 compatibility/upstream-swimlane-direction/run.py \
  --upstream /path/to/pinned/mermaid \
  --stdlib /path/to/kotlin-stdlib-2.1.21.jar --verify-mutations
```

The bridge transports measured geometry into Kotlin and checks every returned
field against the original operation. Three tests still use upstream
`writeBackToLayoutData` to construct their initial geometry; this harness does
not claim a Native implementation of that helper. This change does not promote
any assertion-ledger entries. Native integration tests separately cover actual
parsed scenes, cosmetic style invariance, nesting, empty/collapsed lanes,
explicit local directions, long titles, unequal node sizes, markers and hops.

This is not DOM, pixel or complete routing parity. Native retains its own text
measurement, straight cross-lane connectors and simple feedback routing.
