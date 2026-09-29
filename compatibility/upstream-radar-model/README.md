# Original Radar model and radius assertions

Pinned upstream revision: `04ee3364045d6573f84034d3c9368cc50233a92f`.

Run `python3 compatibility/upstream-radar-model/run.py --upstream /path/to/installed/upstream --stdlib /path/to/kotlin-stdlib.jar` after building debug runtime JARs for core and layout-simple.

The harness runs the unchanged `radar.spec.ts` assertions twice. It records source inputs and relative-radius arguments on the official run, then replays the same assertions over production `MermaidParser`, `RadarChartDiagram.resolvedOptions`/`axisValues`, and `RadarGeometry.relativeRadius`. These functions are also consumed by the actual layout. Java only projects Kotlin data to JSON; JavaScript only serves captured results and manages per-test state. No JavaScript geometry or option calculation substitutes for Native behavior.

Selected: 9 model/parser cases and 6 relative-radius cases. The 3 closed-round-curve path tests and 2 JS draw orchestration tests are unselected and do not count as Native coverage. Config-override cases assert input acceptance only; they do not establish full theme/config support. Empty/reversed scales stay finite in Native as a deliberate rendering guard beyond these assertions.

Auto scale now uses the maximum plotted value unless `max` is supplied, so values 1–3 fill their intended radius instead of being compressed onto a 0–100 scale. Existing explicit-max samples and the shared palette remain stable.
