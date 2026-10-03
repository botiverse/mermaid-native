package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.FlowDirection
import build.raft.mermaid.core.FlowchartDiagram
import build.raft.mermaid.layout.*
import kotlin.math.abs

/** Order exterior tracks after both feedback and forward obstacle detours are known. */
internal fun flowMixedRoutes(
    diagram: FlowchartDiagram,
    rects: Map<String, SceneRect>,
    routes: Map<Int, FlowReturnRoute>,
    measurer: TextMeasurer,
): Map<Int, FlowReturnRoute> {
    if (diagram.subgraphs.isNotEmpty() || routes.size < 2) return routes
    val horizontal = diagram.direction in listOf(FlowDirection.LR, FlowDirection.RL)
    // Only optimize a single row/column. Unrouted diagonal edges in a branched
    // placement need a general intersection scorer before their tracks can move.
    val centers = rects.values.map { if (horizontal) it.y + it.height / 2 else it.x + it.width / 2 }
    if (centers.isEmpty() || centers.any { abs(it - centers.first()) > 1e-6 }) return routes
    val obstacles = rects.map { OrthogonalGeometry.RectEntry(it.key, it.value) }
    val sizes = routes.keys.associateWith { index ->
        diagram.edges[index].label?.takeIf { it.isNotEmpty() }?.let { measurer.measure(it, TextStyle()) }
    }
    fun labelBounds(index: Int, route: FlowReturnRoute): SceneRect? {
        val label = route.label ?: return null
        val size = sizes[index] ?: return null
        return SceneRect(label.x - size.width / 2, label.y - size.height, size.width, size.height)
    }
    val result = routes.toMutableMap()
    // A single deterministic pass: equal scores retain the established geometry.
    for (index in routes.keys) {
        val original = result.getValue(index)
        if (original.points.size != 6) continue
        val middle = OrthogonalGeometry.classifyThreeSegmentRoute(original.points.subList(1, 5)) ?: continue
        val expectedKind = if (horizontal) OrthogonalGeometry.RouteKind.VHV else OrthogonalGeometry.RouteKind.HVH
        if (middle.kind != expectedKind) continue
        val others = result.filterKeys { it != index }
        val segments = others.values.flatMap { it.points.zipWithNext() }
        fun crossings(route: FlowReturnRoute) = route.points.zipWithNext().sumOf { (a, b) ->
            segments.count { (c, d) -> OrthogonalGeometry.segmentsStrictlyCross(a, b, c, d) }
        }
        val oldCrossings = crossings(original)
        if (oldCrossings == 0) continue
        val bounds = rects.values + result.values.map { it.bounds }
        val low = bounds.minOf { if (horizontal) it.y else it.x } - 24.0
        val high = bounds.maxOf { if (horizontal) it.y + it.height else it.x + it.width } + 24.0
        val otherLabels = others.mapNotNull { (i, route) -> labelBounds(i, route)?.let {
            OrthogonalGeometry.RectEntry(i.toString(), it)
        } }
        fun candidate(track: Double, lower: Boolean): FlowReturnRoute? {
            val points = original.points.mapIndexed { i, p ->
                if (i == 2 || i == 3) {
                    if (horizontal) ScenePoint(p.x, track) else ScenePoint(track, p.y)
                } else p
            }
            if (points.zipWithNext().any { (a, b) ->
                    OrthogonalGeometry.segmentHitsAnyRect(a, b, obstacles + otherLabels)
                }) return null
            val size = sizes[index]
            val label = original.label?.let {
                if (horizontal) ScenePoint(it.x, track + if (lower) -8.0 else size!!.height + 8.0)
                else ScenePoint(track + (if (lower) -1 else 1) * (8.0 + size!!.width / 2), it.y)
            }
            val minX = minOf(points.minOf { it.x }, label?.let { it.x - size!!.width / 2 } ?: Double.POSITIVE_INFINITY)
            val maxX = maxOf(points.maxOf { it.x }, label?.let { it.x + size!!.width / 2 } ?: Double.NEGATIVE_INFINITY)
            val minY = minOf(points.minOf { it.y }, label?.let { it.y - size!!.height } ?: Double.POSITIVE_INFINITY)
            val maxY = maxOf(points.maxOf { it.y }, label?.y ?: Double.NEGATIVE_INFINITY)
            val route = FlowReturnRoute(points, label, SceneRect(minX, minY, maxX - minX, maxY - minY))
            // Moving a track also moves its label; neither may obscure another route.
            labelBounds(index, route)?.let { r ->
                val labelObstacle = listOf(OrthogonalGeometry.RectEntry("label", r))
                if (segments.any { (a, b) -> OrthogonalGeometry.segmentHitsAnyRect(a, b, labelObstacle) }) return null
                if ((obstacles + otherLabels).any { (_, b) ->
                        r.x < b.x + b.width && b.x < r.x + r.width &&
                            r.y < b.y + b.height && b.y < r.y + r.height
                    }) return null
            }
            return route
        }
        val best = listOfNotNull(candidate(low, true), candidate(high, false))
            .minByOrNull(::crossings) ?: continue
        if (crossings(best) < oldCrossings) result[index] = best
    }
    return result
}
