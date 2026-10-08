# Swimlane line hops

Swimlane diagrams now distinguish true edge crossings using circular hops. The default is `arc`, matching the upstream `swimlane.lineHops` setting. Use `gap` for a break in the crossing edge or `false` to retain the original straight strokes:

```mermaid
---
config:
  swimlane:
    lineHops: gap
---
swimlane-beta
subgraph L1
A
B
end
subgraph L2
C
D
end
A-->D
B-->C
```

`true` and `arc` select arcs. Other values and unknown swimlane configuration keys are rejected. Ordinary flowcharts retain their previous rendering.

Both the bounded lane renderer and the rich flowchart fallback apply the setting. Only registered edge strokes participate; node outlines and arrow polygons do not. Markers retain their existing clipped endpoints. Hops can expand the scene's ink bounds; any resulting translation is applied to the whole scene, including labels and markers. Layout diagnostics continue to describe centerline routing, not decorative hop geometry.

## Native API

`LineJumps.findEdgeIntersections` detects segment crossings, excludes shared endpoints and T-junctions, and prefers the horizontal-dominant segment. Same-orientation crossings go to the later edge. `processGeometry` returns SVG path text and typed scene segments; `processEdgesWithJumps` returns only path text. Arc/gap radii are clamped against adjacent jumps and, for rounded edges, rounded corners.

`patchRenderedPaths` is the DOM-independent equivalent of the upstream SVG adapter. A host provides rendered path attributes; base64 `data-points` takes precedence over pre-render layout points. The adapter respects curve hints, applies upstream SVG marker offsets when requested, and returns only changed paths. When supplied, the host's `totalLength(id, newD)` callback lets the adapter recompute neo-style dash clearance after rewriting. The callback must measure the new path, not the previous one. Without a measurement API, the old dash declaration is preserved, as upstream does. Attribute lookup and writes are host responsibilities.

Native scene producers already clip edge endpoints and paint independent arrow polygons, so they do not request additional SVG marker offsets. Their existing stroke pattern is preserved. Typed segments use full geometry precision; the compatibility path string follows upstream three-decimal formatting. SVG and Canvas can therefore differ by subpixel rounding on diagonal strokes, without changing the crossing direction or gap.

## Verification and limits

The unchanged upstream unit file contributes 22 assertions, including eight SVG attribute tests. Its integration file contributes one assertion: Native crossing detection runs on the upstream layout's real routed fixture. This does **not** imply parity with that entire upstream layout pipeline.

Additional Native tests exercise branches the original tests do not actually reach: a jumping smoothed edge is skipped, rendered coordinates of the jumping edge override layout coordinates, and the jumping edge's neo dash length changes. Production tests cover both lane renderers, disabled/arc/gap modes, endpoint/marker preservation, and Kuikly Canvas calls from an actual diagram.

The new paths render through the existing SVG, browser Canvas, and Kuikly renderers. Kuikly evidence uses the actual ContextApi recording test; physical-device rendering of these new line hops has not been tested. No SDK release is implied by this source change. This does not establish full complex-flowchart compatibility.
