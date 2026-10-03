# Original orthogonal geometry assertions

Runs all twelve unchanged tests from pinned geometry.spec.ts against production Kotlin OrthogonalGeometry. The predicates are consumed by flat flowchart obstacle detour selection. Crossings/shared endpoints, collinear overlap, rectangle exclusions/shrink and edge identity/layout-only filtering are evaluated by Kotlin. JS captures calls and substitutes Native results; Python/Java only serialize and project fields. Missing inputs fail, with no JS fallback.

The two node-collection/route-classification assertions now use production helpers. Flow obstacle routing consumes the measured-leaf bounds filter without changing existing rectangles. Mixed forward/return track ordering checks that the middle four points have the required H-V-H or V-H-V orientation before moving them. Source hashes and exact runtime JAR hashes are recorded. No claim of general routing or complete swimlane compatibility.

Strict interior crossings take priority over shared stubs when choosing between production detours, reducing avoidable crossings between forward skips and feedback routes. Original strict-crossing assertions include T-junction, shared-endpoint, collinear and near-endpoint exclusions.

The bridge transports rectangles and route kinds from Kotlin. The adapter reconstructs the original Map interface from returned node IDs/rectangles and maps a null classification to JavaScript undefined; it performs no geometric decisions. The original bounds assertion checks Map keys and rectangle coordinates, not full node-info metadata. The classifier assertion checks kinds and rejection, not point-object identity.

Use `--verify-result-mutations` to corrupt route kind, diagonal-route rejection, node identity and rectangle bounds independently. Each corruption must fail only its targeted original assertion; restoration must pass all twelve. No complete validateLayout/scoreLayout, general diagram validation, or full swimlane rendering compatibility is claimed.
