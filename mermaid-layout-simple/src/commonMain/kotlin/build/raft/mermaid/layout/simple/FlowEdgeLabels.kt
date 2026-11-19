package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.FlowEdgeStyle
import build.raft.mermaid.core.FlowchartDiagram
import build.raft.mermaid.layout.*
import kotlin.math.abs
import kotlin.math.sqrt

internal data class FlowEdgeLabelPlacement(val origin: ScenePoint, val bounds: SceneRect)

/** Move a colliding flat straight-edge label beside its line when that space is clear. */
internal fun flowStraightEdgeLabels(
    diagram: FlowchartDiagram,
    nodes: Map<String, SceneRect>,
    paths: Map<Int, List<ScenePoint>>,
    routes: Map<Int, FlowReturnRoute>,
    measurer: TextMeasurer,
): Map<Int, FlowEdgeLabelPlacement> {
    if (diagram.subgraphs.isNotEmpty()) return emptyMap()
    val sizes = diagram.edges.mapIndexedNotNull { index, edge ->
        edge.label?.takeIf { it.isNotEmpty() && edge.style != FlowEdgeStyle.INVISIBLE && index in paths }
            ?.let { index to measurer.measure(it, TextStyle()) }
    }.toMap()
    fun placement(index: Int, origin: ScenePoint): FlowEdgeLabelPlacement {
        val size = sizes.getValue(index)
        return FlowEdgeLabelPlacement(origin, SceneRect(origin.x - size.width / 2, origin.y - size.height, size.width, size.height))
    }
    val occupied = sizes.keys.associateWith { index ->
        placement(index, routes[index]?.label ?: MermaidPathGeometry.midpoint(paths.getValue(index)).let { it.copy(y = it.y - 6.0) })
    }.toMutableMap()
    val segments = paths.filterKeys { diagram.edges[it].style != FlowEdgeStyle.INVISIBLE }.values.flatMap { it.zipWithNext() }
    val result = linkedMapOf<Int, FlowEdgeLabelPlacement>()
    fun overlaps(a: SceneRect, b: SceneRect) = a.x < b.x + b.width && b.x < a.x + a.width &&
        a.y < b.y + b.height && b.y < a.y + a.height
    for (index in sizes.keys) {
        val points = paths.getValue(index)
        if (index in routes || points.size != 2) continue
        val dx = points[1].x - points[0].x
        val dy = points[1].y - points[0].y
        val vertical = abs(dx) < 1e-6
        if (abs(dy) < 1e-6) continue
        val original = occupied.getValue(index)
        val size = sizes.getValue(index)
        val candidates = if (vertical) {
            listOf(-1.0, 1.0).map { side ->
                placement(index, original.origin.copy(x = original.origin.x + side * (size.width / 2 + 8.0)))
            }
        } else {
            if (!labelSegmentHitsRect(points[0], points[1], original.bounds)) continue
            val length = sqrt(dx * dx + dy * dy)
            // Prefer the upper side, independent of source/target direction.
            val side = if (dx > 0) -1.0 else 1.0
            val nx = -dy / length * side
            val ny = dx / length * side
            val distance = (abs(nx) * size.width + abs(ny) * size.height) / 2 + 8.0
            // A midpoint candidate may be squeezed between the source and target
            // boxes. Small along-edge shifts can provide clearance without rerouting.
            listOf(0.5, 0.3, 0.7).flatMap { fraction ->
                val mid = ScenePoint(points[0].x + dx * fraction, points[0].y + dy * fraction)
                if (size.width > size.height * 4) {
                    // Keep wide text at the edge's height instead of displacing it
                    // far above/below its nodes along the diagonal's normal.
                    val offset = size.width / 2 + abs(dx / dy) * size.height / 2 + 8.0 * length / abs(dy)
                    listOf(-1.0, 1.0).map { sign ->
                        placement(index, ScenePoint(mid.x + sign * offset, mid.y + size.height / 2))
                    }
                } else {
                    listOf(1.0, -1.0).map { sign ->
                        placement(index, ScenePoint(mid.x + sign * nx * distance, mid.y + sign * ny * distance + size.height / 2))
                    }
                }
            }
        }
        val chosen = candidates.firstOrNull { candidate ->
            val r = candidate.bounds
            val padded = SceneRect(r.x - 3.0, r.y - 3.0, r.width + 6.0, r.height + 6.0)
            val obstacle = listOf(OrthogonalGeometry.RectEntry("label", padded))
            nodes.values.none { overlaps(padded, it) } &&
                occupied.none { (other, label) -> other != index && overlaps(padded, label.bounds) } &&
                segments.none { (a, b) ->
                    // Preserve the established conservative vertical placement. Diagonal
                    // candidates need exact clipping to use the empty space beside a line.
                    if (vertical) OrthogonalGeometry.segmentHitsAnyRect(a, b, obstacle)
                    else labelSegmentHitsRect(a, b, padded)
                }
        } ?: continue
        occupied[index] = chosen
        result[index] = chosen
    }
    return result
}

/** Clip a finite segment to a rectangle; a diagonal bounding box alone is insufficient. */
private fun labelSegmentHitsRect(a: ScenePoint, b: ScenePoint, r: SceneRect): Boolean {
    var start = 0.0
    var end = 1.0
    fun clip(origin: Double, delta: Double, min: Double, max: Double): Boolean {
        if (abs(delta) < 1e-9) return origin >= min && origin <= max
        val first = (min - origin) / delta
        val last = (max - origin) / delta
        start = maxOf(start, minOf(first, last))
        end = minOf(end, maxOf(first, last))
        return start <= end
    }
    return clip(a.x, b.x - a.x, r.x, r.x + r.width) &&
        clip(a.y, b.y - a.y, r.y, r.y + r.height)
}

internal data class FlowTitlePlacement(val center: Double, val frame: SceneRect)

/**
 * Horizontal title position that keeps a frame title clear of edges crossing
 * its title band; Mermaid never draws an edge through a cluster label. The
 * title stays centered when it is clear, otherwise it slides to the nearest
 * clear position. When the frame has no clear span wide enough, the frame
 * grows sideways into free space to make one.
 */
internal fun flowTitlePlacement(
    id: String,
    diagram: FlowchartDiagram,
    frames: Map<String, SceneRect>,
    nodes: Map<String, SceneRect>,
    baseline: Double,
    textWidth: Double,
    fontSize: Double,
    paths: List<List<ScenePoint>>,
): FlowTitlePlacement {
    val frame = frames.getValue(id)
    val center = frame.x + frame.width / 2
    val top = baseline - fontSize - 2.0
    val bottom = baseline + 4.0
    val half = textWidth / 2 + 4.0
    val blocked = paths.flatMap { it.zipWithNext() }.mapNotNull { (a, b) ->
        val low = minOf(a.y, b.y); val high = maxOf(a.y, b.y)
        if (high < top || low > bottom) return@mapNotNull null
        fun xAt(y: Double) = a.x + (b.x - a.x) * (y - a.y) / (b.y - a.y)
        val xs = if (abs(b.y - a.y) < 1e-9) listOf(a.x, b.x) else listOf(maxOf(low, top), minOf(high, bottom)).map(::xAt)
        xs.min() to xs.max()
    }
    fun clear(c: Double) = blocked.none { (l, r) -> l < c + half && r > c - half }
    if (clear(center)) return FlowTitlePlacement(center, frame)
    val candidates = (blocked.flatMap { (l, r) -> listOf(l - half - 0.01, r + half + 0.01) }).filter(::clear)
    val inside = candidates.map { it.coerceIn(frame.x + half, maxOf(frame.x + half, frame.x + frame.width - half)) }
        .filter { clear(it) && it - half >= frame.x - 1e-6 && it + half <= frame.x + frame.width + 1e-6 }
    inside.minByOrNull { abs(it - center) }?.let { return FlowTitlePlacement(it, frame) }
    // Grow the frame only into space no other node or unrelated frame uses.
    val ancestors = generateSequence(diagram.subgraphs.firstOrNull { it.id == id }?.parentId) { parent ->
        diagram.subgraphs.firstOrNull { it.id == parent }?.parentId
    }.toSet()
    val members = mutableSetOf<String>()
    fun collect(group: String) {
        diagram.subgraphs.filter { it.id == group }.forEach { members += it.nodeIds }
        diagram.subgraphs.filter { it.parentId == group }.forEach { members += it.id; collect(it.id) }
    }
    collect(id)
    val obstacles = nodes.filterKeys { it !in members }.values + frames.filterKeys { it != id && it !in members && it !in ancestors }.values
    val parent = diagram.subgraphs.firstOrNull { it.id == id }?.parentId?.let { frames[it] }
    fun overlaps(a: SceneRect, b: SceneRect) = a.x < b.x + b.width && b.x < a.x + a.width && a.y < b.y + b.height && b.y < a.y + a.height
    return candidates.sortedBy { abs(it - center) }.firstNotNullOfOrNull { c ->
        val left = minOf(frame.x, c - half - 4.0); val right = maxOf(frame.x + frame.width, c + half + 4.0)
        val grown = SceneRect(left, frame.y, right - left, frame.height)
        val fits = (parent == null || (grown.x >= parent.x && grown.x + grown.width <= parent.x + parent.width)) &&
            obstacles.none { overlaps(it, grown) }
        if (fits) FlowTitlePlacement(c, grown) else null
    } ?: FlowTitlePlacement(center, frame)
}
