package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.FlowDirection
import build.raft.mermaid.core.FlowEdgeStyle
import build.raft.mermaid.core.FlowchartDiagram
import build.raft.mermaid.layout.*
import kotlin.math.abs

internal data class FlowReturnRoute(
    val points: List<ScenePoint>,
    val label: ScenePoint?,
    val bounds: SceneRect,
)

/** Feedback tracks around a flat set of nodes, also used inside leaf containers. */
internal fun flowReturnRoutes(
    diagram: FlowchartDiagram,
    rects: Map<String, SceneRect>,
    measurer: TextMeasurer,
    config: LayoutConfig,
): Map<Int, FlowReturnRoute> {
    if (diagram.subgraphs.isNotEmpty() || rects.isEmpty()) return emptyMap()
    val horizontal = diagram.direction == FlowDirection.LR || diagram.direction == FlowDirection.RL
    val reversed = diagram.direction == FlowDirection.BT || diagram.direction == FlowDirection.RL
    val sign = if (reversed) -1.0 else 1.0
    data class Box(val start: Double, val end: Double, val cross: Double)
    fun box(r: SceneRect): Box {
        val lo = if (horizontal) r.x else r.y
        val hi = lo + if (horizontal) r.width else r.height
        return Box(if (reversed) -hi else lo, if (reversed) -lo else hi,
            if (horizontal) r.y + r.height / 2 else r.x + r.width / 2)
    }
    fun point(main: Double, cross: Double) =
        if (horizontal) ScenePoint(main * sign, cross) else ScenePoint(cross, main * sign)
    val boxes = rects.mapValues { box(it.value) }
    val gap = (config.nodeGap / 3).coerceIn(4.0, 16.0)
    var track = rects.values.minOf { if (horizontal) it.y else it.x } - 24.0
    val result = linkedMapOf<Int, FlowReturnRoute>()
    diagram.edges.forEachIndexed { index, edge ->
        if (edge.style == FlowEdgeStyle.INVISIBLE) return@forEachIndexed
        val source = boxes[edge.sourceId] ?: return@forEachIndexed
        val target = boxes[edge.targetId] ?: return@forEachIndexed
        val sourceCenter = (source.start + source.end) / 2
        val targetCenter = (target.start + target.end) / 2
        if (targetCenter > sourceCenter) return@forEachIndexed
        // Cross the clear gap outside the whole rank, including taller/wider peers.
        val exit = boxes.values.filter { abs((it.start + it.end) / 2 - sourceCenter) < 0.001 }.maxOf { it.end } + gap
        val entry = boxes.values.filter { abs((it.start + it.end) / 2 - targetCenter) < 0.001 }.minOf { it.start } - gap
        val points = listOf(point(source.end, source.cross), point(exit, source.cross),
            point(exit, track), point(entry, track), point(entry, target.cross), point(target.start, target.cross))
        val text = edge.label?.takeIf { it.isNotEmpty() }
        val size = text?.let { measurer.measure(it, TextStyle()) }
        val label = size?.let {
            if (horizontal) ScenePoint((entry + exit) / 2 * sign, track - 8.0)
            else ScenePoint(track - 8.0 - it.width / 2, (entry + exit) / 2 * sign + it.height * 0.35)
        }
        val minX = minOf(points.minOf { it.x }, if (label != null) label.x - size!!.width / 2 else Double.POSITIVE_INFINITY)
        val maxX = maxOf(points.maxOf { it.x }, if (label != null) label.x + size!!.width / 2 else Double.NEGATIVE_INFINITY)
        val minY = minOf(points.minOf { it.y }, if (label != null) label.y - size!!.height else Double.POSITIVE_INFINITY)
        val maxY = maxOf(points.maxOf { it.y }, label?.y ?: Double.NEGATIVE_INFINITY)
        result[index] = FlowReturnRoute(points, label, SceneRect(minX, minY, maxX - minX, maxY - minY))
        track -= (if (horizontal) size?.height ?: 0.0 else size?.width ?: 0.0) + 32.0
    }
    // Keep the established tracks unless moving one to the opposite side removes
    // a strict crossing. Consider all return routes together, including self-loops.
    val obstacles = rects.map { OrthogonalGeometry.RectEntry(it.key, it.value) }
    val edges = diagram.edges.mapIndexed { index, edge ->
        val a = boxes[edge.sourceId]; val b = boxes[edge.targetId]
        OrthogonalGeometry.Edge(result[index]?.points ?: if (a != null && b != null)
            listOf(point(a.end, a.cross), point(b.start, b.cross)) else emptyList(),
            edge.style == FlowEdgeStyle.INVISIBLE)
    }.toMutableList()
    // Forward obstacle detours choose their side in the next routing pass. Keep
    // the established feedback tracks when that pass is needed: independently
    // moving both families can merely relocate crossings onto return labels.
    if (diagram.edges.withIndex().any { (index, edge) ->
            val a = boxes[edge.sourceId]; val b = boxes[edge.targetId]
            index !in result && edge.style != FlowEdgeStyle.INVISIBLE && a != null && b != null &&
                b.start > a.end && abs(a.cross - b.cross) < 1e-6 &&
                OrthogonalGeometry.segmentHitsAnyRect(point(a.end, a.cross), point(b.start, b.cross),
                    obstacles, listOf(edge.sourceId, edge.targetId))
        }) return result
    var high = rects.values.maxOf { if (horizontal) it.y + it.height else it.x + it.width }
    for ((index, original) in result.toMap()) {
        val otherSegments = edges.filterIndexed { i, edge -> i != index && !edge.isLayoutOnly }
            .flatMap { it.points.zipWithNext() }
        fun crossings(points: List<ScenePoint>) = points.zipWithNext().sumOf { (a, b) ->
            otherSegments.count { (c, d) -> OrthogonalGeometry.segmentsStrictlyCross(a, b, c, d) }
        }
        val oldCrossings = crossings(original.points)
        if (oldCrossings == 0) continue
        val oppositeTrack = high + 24.0
        val points = original.points.mapIndexed { i, p ->
            if (i == 2 || i == 3) {
                if (horizontal) ScenePoint(p.x, oppositeTrack) else ScenePoint(oppositeTrack, p.y)
            } else p
        }
        if (crossings(points) >= oldCrossings || points.zipWithNext().any { (a, b) ->
                OrthogonalGeometry.segmentHitsAnyRect(a, b, obstacles)
            }) continue
        val size = diagram.edges[index].label?.takeIf { it.isNotEmpty() }?.let { measurer.measure(it, TextStyle()) }
        val label = original.label?.let {
            if (horizontal) ScenePoint(it.x, oppositeTrack + size!!.height + 8.0)
            else ScenePoint(oppositeTrack + 8.0 + size!!.width / 2, it.y)
        }
        val minX = minOf(points.minOf { it.x }, label?.let { it.x - size!!.width / 2 } ?: Double.POSITIVE_INFINITY)
        val maxX = maxOf(points.maxOf { it.x }, label?.let { it.x + size!!.width / 2 } ?: Double.NEGATIVE_INFINITY)
        val minY = minOf(points.minOf { it.y }, label?.let { it.y - size!!.height } ?: Double.POSITIVE_INFINITY)
        val maxY = maxOf(points.maxOf { it.y }, label?.y ?: Double.NEGATIVE_INFINITY)
        val route = FlowReturnRoute(points, label, SceneRect(minX, minY, maxX - minX, maxY - minY))
        result[index] = route
        edges[index] = OrthogonalGeometry.Edge(points)
        high = if (horizontal) maxY else maxX
    }
    return result
}
