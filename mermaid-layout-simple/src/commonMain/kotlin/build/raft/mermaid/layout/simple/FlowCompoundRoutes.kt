package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.FlowDirection
import build.raft.mermaid.core.FlowEdgeStyle
import build.raft.mermaid.core.FlowchartDiagram
import build.raft.mermaid.layout.*
import kotlin.math.abs

/** Exterior lanes for edges between separate top-level containers. */
internal fun flowCompoundRoutes(
    diagram: FlowchartDiagram,
    nodes: Map<String, SceneRect>,
    groups: Map<String, SceneRect>,
    existing: Map<Int, FlowReturnRoute>,
    measurer: TextMeasurer,
    routeRootEdges: Boolean = false,
): Map<Int, FlowReturnRoute> {
    if (groups.isEmpty() || nodes.isEmpty()) return emptyMap()
    val definitions = diagram.subgraphs.associateBy { it.id }
    fun ancestors(node: String): Set<String> {
        val result = linkedSetOf<String>()
        var parent = diagram.subgraphs.firstOrNull { node in it.nodeIds }?.id
        while (parent != null && result.add(parent)) parent = definitions[parent]?.parentId
        return result
    }
    val owners = nodes.keys.associateWith(::ancestors)
    val all = nodes.values + groups.values
    val left = all.minOf { it.x }
    val right = all.maxOf { it.x + it.width }
    val top = all.minOf { it.y }
    val bottom = all.maxOf { it.y + it.height }
    val headers = groups.map { (id, rect) ->
        val font = flowGroupStyle(definitions.getValue(id), diagram).text
        OrthogonalGeometry.RectEntry("header:$id", SceneRect(rect.x, rect.y, rect.width, font.fontSize + 18.0))
    }
    val result = linkedMapOf<Int, FlowReturnRoute>()
    var laneInset = 24.0
    diagram.edges.forEachIndexed { index, edge ->
        if (index in existing || edge.style == FlowEdgeStyle.INVISIBLE || edge.sourceId == edge.targetId) return@forEachIndexed
        val source = nodes[edge.sourceId] ?: return@forEachIndexed
        val target = nodes[edge.targetId] ?: return@forEachIndexed
        val sourceOwners = owners.getValue(edge.sourceId)
        val targetOwners = owners.getValue(edge.targetId)
        // A common enclosing frame needs its own reserved interior lanes. Do not
        // escape that frame or enlarge it over adjacent sibling containers here.
        if (sourceOwners.intersect(targetOwners).isNotEmpty() ||
            !routeRootEdges && sourceOwners.isEmpty() && targetOwners.isEmpty()) return@forEachIndexed
        val horizontal = diagram.direction in listOf(FlowDirection.LR, FlowDirection.RL)
        val reversed = diagram.direction in listOf(FlowDirection.RL, FlowDirection.BT)
        fun center(r: SceneRect) = if (horizontal) r.x + r.width / 2 else r.y + r.height / 2
        val backward = if (reversed) center(target) >= center(source) else center(target) <= center(source)
        val unrelatedNodes = nodes.filterKeys { it != edge.sourceId && it != edge.targetId }
            .map { OrthogonalGeometry.RectEntry(it.key, it.value) }
        val direct = if (horizontal) {
            val after = center(target) >= center(source)
            ScenePoint(if (after) source.x + source.width else source.x, source.y + source.height / 2) to
                ScenePoint(if (after) target.x else target.x + target.width, target.y + target.height / 2)
        } else {
            val after = center(target) >= center(source)
            ScenePoint(source.x + source.width / 2, if (after) source.y + source.height else source.y) to
                ScenePoint(target.x + target.width / 2, if (after) target.y else target.y + target.height)
        }
        if (!backward && !OrthogonalGeometry.segmentHitsAnyRect(direct.first, direct.second, unrelatedNodes)) return@forEachIndexed
        val obstacles = nodes.map { OrthogonalGeometry.RectEntry(it.key, it.value) } + headers +
            groups.filterKeys { it !in sourceOwners && it !in targetOwners }
                .map { OrthogonalGeometry.RectEntry("group:${it.key}", it.value) }
        val labelSize = edge.label?.takeIf { it.isNotEmpty() }?.let { measurer.measure(it, TextStyle()) }
        val candidates = (0..3).mapNotNull { side ->
            val acrossX = side < 2
            val low = side % 2 == 0
            val lane = when (side) { 0 -> top - laneInset; 1 -> bottom + laneInset; 2 -> left - laneInset; else -> right + laneInset }
            fun port(r: SceneRect) = if (acrossX) ScenePoint(r.x + r.width / 2, if (low) r.y else r.y + r.height)
                else ScenePoint(if (low) r.x else r.x + r.width, r.y + r.height / 2)
            val start = port(source); val end = port(target)
            val points = listOf(start, if (acrossX) ScenePoint(start.x, lane) else ScenePoint(lane, start.y),
                if (acrossX) ScenePoint(end.x, lane) else ScenePoint(lane, end.y), end).fold(emptyList<ScenePoint>()) { acc, p ->
                if (acc.lastOrNull() == p) acc else acc + p
            }
            if (points.size < 3 || points.zipWithNext().any { (a, b) -> OrthogonalGeometry.segmentHitsAnyRect(a, b, obstacles) }) return@mapNotNull null
            val label = labelSize?.let { size ->
                if (acrossX) ScenePoint((start.x + end.x) / 2, lane + if (low) -8.0 else size.height + 8.0)
                else ScenePoint(lane + (if (low) -1 else 1) * (size.width / 2 + 8.0), (start.y + end.y) / 2 + size.height * .35)
            }
            val minX = minOf(points.minOf { it.x }, label?.let { it.x - labelSize!!.width / 2 } ?: Double.POSITIVE_INFINITY)
            val maxX = maxOf(points.maxOf { it.x }, label?.let { it.x + labelSize!!.width / 2 } ?: Double.NEGATIVE_INFINITY)
            val minY = minOf(points.minOf { it.y }, label?.let { it.y - labelSize!!.height } ?: Double.POSITIVE_INFINITY)
            val maxY = maxOf(points.maxOf { it.y }, label?.y ?: Double.NEGATIVE_INFINITY)
            FlowReturnRoute(points, label, SceneRect(minX, minY, maxX - minX, maxY - minY))
        }
        val otherRoutes = existing.values + result.values
        val chosen = candidates.minWithOrNull(compareBy<FlowReturnRoute> { route ->
            route.points.zipWithNext().sumOf { (a, b) -> otherRoutes.sumOf { other ->
                other.points.zipWithNext().count { (c, d) -> OrthogonalGeometry.segmentsStrictlyCross(a, b, c, d) }
            } }
        }.thenBy { route -> route.points.zipWithNext().sumOf { (a, b) -> abs(a.x - b.x) + abs(a.y - b.y) } }) ?: return@forEachIndexed
        result[index] = chosen
        // A later lane cannot put its label into the preceding exterior label.
        laneInset += maxOf(labelSize?.width ?: 0.0, labelSize?.height ?: 0.0) + 32.0
    }
    return result
}
