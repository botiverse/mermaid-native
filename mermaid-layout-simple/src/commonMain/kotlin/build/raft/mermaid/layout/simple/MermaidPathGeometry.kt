package build.raft.mermaid.layout.simple

import build.raft.mermaid.layout.ScenePoint
import kotlin.math.floor
import kotlin.math.sqrt

/** Distances are measured along straight segments, in scene coordinates. */
public object MermaidPathGeometry {
    private fun distance(a: ScenePoint, b: ScenePoint): Double {
        val dx = b.x - a.x
        val dy = b.y - a.y
        return sqrt(dx * dx + dy * dy)
    }

    public fun length(points: List<ScenePoint>): Double =
        points.zipWithNext().sumOf { (a, b) -> distance(a, b) }

    /**
     * Returns the point at [distanceToTraverse], rounding interpolated coordinates
     * to five decimal places to match Mermaid. Repeated vertices consume no distance.
     * Throws when the path is empty or the finite, nonnegative distance exceeds it.
     */
    public fun pointAt(points: List<ScenePoint>, distanceToTraverse: Double): ScenePoint {
        require(distanceToTraverse.isFinite() && distanceToTraverse >= 0.0) {
            "Distance must be finite and nonnegative"
        }
        if (points.isEmpty()) error("Could not find a suitable point for the given distance")
        if (distanceToTraverse == 0.0) return points.first()
        var remaining = distanceToTraverse
        for (index in 1 until points.size) {
            val previous = points[index - 1]
            val point = points[index]
            val segment = distance(previous, point)
            if (segment == 0.0) continue
            if (segment < remaining) {
                remaining -= segment
            } else {
                val ratio = remaining / segment
                if (ratio >= 1.0) return point
                fun interpolate(a: Double, b: Double): Double =
                    floor(((1.0 - ratio) * a + ratio * b) * 100_000.0 + 0.5) / 100_000.0
                return ScenePoint(interpolate(previous.x, point.x), interpolate(previous.y, point.y))
            }
        }
        error("Could not find a suitable point for the given distance")
    }

    /** Shared label anchor for line and polyline consumers. */
    public fun midpoint(points: List<ScenePoint>): ScenePoint = pointAt(points, length(points) / 2.0)
}
