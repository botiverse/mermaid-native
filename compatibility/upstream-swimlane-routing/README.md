# Swimlane orthogonal-routing compatibility audit

This directory records the Native implementation and a replay of Mermaid's
orthogonal-router tests at revision `04ee3364045d6573f84034d3c9368cc50233a92f`.
`SwimlaneObstacleRoutes` is an independent bounded visibility-grid/Dijkstra
implementation used by the production swimlane layout. It treats nodes, titles, unrelated groups and previously placed labels as
obstacles, penalizes crossings/overlap with earlier routes, preserves existing
self-loops, and keeps same-lane feedback routes when their segments are clear.
Rectangular nodes offer three candidate ports per side; other shapes retain
center ports before shape clipping. Paths are computed in final upright
coordinates for TB, BT, LR and RL layouts.

Routing is bounded to 256 nodes/groups/edges and 16,384 grid points. If a route
cannot be found within the bounds, the prior route or straight-edge fallback
remains; obstacle freedom is not guaranteed for fallback routes or self-loops.
Edge order affects results, and crossing penalties do not guarantee a
crossing-free drawing. Generic endpoint cleanup is bypassed for swimlanes
because it does not know lane-title obstacles.

The unmodified upstream suite has 30 assertions: 28 pass and 2 are upstream
skips. The Native replay currently has 25 pass, 3 failures, and 2 skips. The
three differences are converging-port placement, anchor offsets, and one
crossing/order case. These results are retained as evidence only; they do not
promote upstream coverage or change the project coverage ledger. Mutation
controls in `run.py` verify that missing paths and off-boundary ports remain
observable.

The bridge transports measured node rectangles to the production Native
pathfinder; it does not validate the entire upstream layout pipeline. Test
sources are hash-checked against the unchanged batch-import originals. The
runner writes logs, snapshots and `summary.json` under
`<upstream>/.native-swimlane-routing-audit/` and exits nonzero while the three
original assertions still fail. SVG/Canvas and full pipeline tests are separate.

After building `:mermaid-layout-simple:bundleLibRuntimeToJarDebug`, run from the
repository root:

```sh
python3 compatibility/upstream-swimlane-routing/run.py \
  --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar \
  --verify-mutations
```
