package build.raft.mermaid.layout.simple

import build.raft.mermaid.layout.*

/**
 * Mermaid flowchart edge interpolation for a route's control points.
 *
 * `linear` keeps the polyline, `step`, `stepBefore` and `stepAfter` turn it
 * into axis-aligned steps like d3's step curves, and every other value
 * (including Mermaid's default `basis`) draws d3's uniform cubic B-spline.
 * Each spline cubic is emitted as two quadratic segments so every scene
 * renderer can draw it with the existing [DrawPath] primitives.
 */
internal fun flowEdgeCurve(
    points: List<ScenePoint>,
    curve: String?,
    stroke: SceneColor,
    strokeWidth: Double,
    pattern: StrokePattern,
): DrawCommand {
    val polyline = flowEdgeCurvePolyline(points, curve)
    if (polyline != null) {
        return if (polyline.size == 2) DrawLine(polyline[0], polyline[1], stroke, strokeWidth, pattern)
        else DrawPolyline(polyline, stroke, strokeWidth, pattern)
    }
    return DrawPath(basisSegments(points), stroke, strokeWidth, pattern)
}

/** The polyline drawn for non-spline curves, or null when the edge is a basis spline. */
internal fun flowEdgeCurvePolyline(points: List<ScenePoint>, curve: String?): List<ScenePoint>? = when (curve) {
    "linear" -> points
    "step", "stepBefore", "stepAfter" -> stepPoints(points, curve)
    else -> if (points.size <= 2) points else null
}

private fun stepPoints(points: List<ScenePoint>, curve: String): List<ScenePoint> {
    val result = mutableListOf(points.first())
    points.zipWithNext().forEach { (a, b) ->
        when (curve) {
            "stepBefore" -> result += ScenePoint(a.x, b.y)
            "stepAfter" -> result += ScenePoint(b.x, a.y)
            else -> { val mx = (a.x + b.x) / 2; result += ScenePoint(mx, a.y); result += ScenePoint(mx, b.y) }
        }
        result += b
    }
    return result.zipWithNext().filter { (a, b) -> a != b }.map { it.first } + result.last()
}

/** d3.curveBasis, with each cubic split at t=0.5 into two quadratic approximations. */
internal fun basisSegments(points: List<ScenePoint>): List<ScenePathSegment> {
    val segments = mutableListOf<ScenePathSegment>(PathMove(points.first()))
    if (points.size < 3) { points.drop(1).forEach { segments += PathLine(it) }; return segments }
    fun mix(a: ScenePoint, wa: Double, b: ScenePoint, wb: Double, c: ScenePoint = a, wc: Double = 0.0, d: Double) =
        ScenePoint((a.x * wa + b.x * wb + c.x * wc) / d, (a.y * wa + b.y * wb + c.y * wc) / d)
    var current = mix(points[0], 5.0, points[1], 1.0, d = 6.0)
    segments += PathLine(current)
    fun cubic(c1: ScenePoint, c2: ScenePoint, end: ScenePoint) {
        val p0 = current
        fun mid(a: ScenePoint, b: ScenePoint) = ScenePoint((a.x + b.x) / 2, (a.y + b.y) / 2)
        val m01 = mid(p0, c1); val m12 = mid(c1, c2); val m23 = mid(c2, end)
        val a = mid(m01, m12); val b = mid(m12, m23); val split = mid(a, b)
        fun quadratic(s: ScenePoint, q1: ScenePoint, q2: ScenePoint, e: ScenePoint) {
            val control = ScenePoint((3 * (q1.x + q2.x) - s.x - e.x) / 4, (3 * (q1.y + q2.y) - s.y - e.y) / 4)
            segments += PathQuadratic(control, e)
        }
        quadratic(p0, m01, a, split)
        quadratic(split, b, m23, end)
        current = end
    }
    // Interior knots, as d3 emits them while points stream in.
    for (i in 2 until points.size) {
        val x0 = points[i - 2]; val x1 = points[i - 1]; val p = points[i]
        cubic(mix(x0, 2.0, x1, 1.0, d = 3.0), mix(x0, 1.0, x1, 2.0, d = 3.0), mix(x0, 1.0, x1, 4.0, p, 1.0, 6.0))
    }
    // lineEnd: repeat the last point once, then close with a line.
    val x0 = points[points.size - 2]; val x1 = points.last()
    cubic(mix(x0, 2.0, x1, 1.0, d = 3.0), mix(x0, 1.0, x1, 2.0, d = 3.0), mix(x0, 1.0, x1, 5.0, d = 6.0))
    segments += PathLine(x1)
    return segments
}

/**
 * Route points with the ends pulled back under their arrowheads, like
 * Mermaid's marker offsets: a thick stroke stops inside the arrowhead while
 * the arrow tip still touches the node outline.
 */
internal fun flowMarkerInset(
    points: List<ScenePoint>,
    fromMarker: build.raft.mermaid.core.FlowMarker,
    toMarker: build.raft.mermaid.core.FlowMarker,
    strokeWidth: Double,
): List<ScenePoint> {
    // A thin stroke ends unseen under its arrowhead; only thick strokes would show past the tip.
    if (points.size < 2 || strokeWidth <= 2.0) return points
    fun inset(marker: build.raft.mermaid.core.FlowMarker) = when (marker) {
        build.raft.mermaid.core.FlowMarker.POINT -> 3.0 + strokeWidth
        else -> 0.0
    }
    fun pull(end: ScenePoint, toward: ScenePoint, distance: Double): ScenePoint {
        val dx = toward.x - end.x; val dy = toward.y - end.y
        val length = kotlin.math.sqrt(dx * dx + dy * dy)
        if (distance <= 0.0 || length <= distance * 2) return end
        return ScenePoint(end.x + dx / length * distance, end.y + dy / length * distance)
    }
    val result = points.toMutableList()
    result[result.lastIndex] = pull(points.last(), points[points.lastIndex - 1], inset(toMarker))
    result[0] = pull(points.first(), points[1], inset(fromMarker))
    return result
}
