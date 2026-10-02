# Original orthogonal geometry assertions

Runs ten unchanged tests from pinned geometry.spec.ts against production Kotlin OrthogonalGeometry. The predicates are consumed by flat flowchart obstacle detour selection. Crossings/shared endpoints, collinear overlap, rectangle exclusions/shrink and edge identity/layout-only filtering are evaluated by Kotlin. JS captures calls and substitutes boolean results; Python/Java only serialize and project fields. Missing inputs fail, with no JS fallback.

Two node-collection/route-classification tests are unselected because those APIs have no production consumer in this change. Source hashes and exact runtime JAR hashes are recorded. No claim of general routing or complete swimlane compatibility.

Strict interior crossings take priority over shared stubs when choosing between production detours, reducing avoidable crossings between forward skips and feedback routes. Original strict-crossing assertions include T-junction, shared-endpoint, collinear and near-endpoint exclusions.
