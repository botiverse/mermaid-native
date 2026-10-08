# Typed stroke paths

`DrawPath` adds stroke-only paths to `LayoutScene`. `PathMove` starts a new subpath (an intentional gap), `PathLine` adds a straight segment, and `PathQuadratic` supplies a control point and endpoint. `PathArc` supplies a center, positive radius, start angle and signed sweep in radians, using the scene's y-down coordinates. Positive sweeps are clockwise. Each arc supports at most a half circle; combine successive arcs for larger sweeps.

An arc connects the current point to its start with a straight segment, matching Canvas arc semantics. A zero sweep adds only that connection. Paths must start with a move, and all coordinates, radii, angles and stroke widths must be finite. `translated(dx, dy)` moves endpoints, control points and arc centers together. `conservativeBounds()` includes quadratic control hulls, full circles for arcs, and half the stroke width; it is intentionally a conservative bound, not an exact curve bounding box.

SVG, the web Canvas script and the Kuikly renderer consume the same segments. Canvas scripts use a new `path` operation with `segments` arrays: `['M', x, y]`, `['L', x, y]`, `['Q', cx, cy, x, y]`, and `['A', cx, cy, radius, startAngle, sweepAngle]`. Path parameters retain full numeric precision. Hosts using the Canvas script must update their executor to support `path`; the reference executor is `acceptance/mermaid-canvas.js`.

This extends the sealed `DrawCommand` hierarchy. Custom consumers with exhaustive `when` statements must add a `DrawPath` case when upgrading. Existing diagram layout producers do not emit paths yet, so default diagram rendering is unchanged. This foundation does not enable swimlane line hops, implement their configuration, or establish any additional upstream assertion coverage.
