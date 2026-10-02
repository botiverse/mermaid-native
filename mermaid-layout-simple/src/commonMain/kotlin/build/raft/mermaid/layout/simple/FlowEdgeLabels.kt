package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.FlowEdgeStyle
import build.raft.mermaid.core.FlowchartDiagram
import build.raft.mermaid.layout.*
import kotlin.math.abs

internal data class FlowEdgeLabelPlacement(val origin: ScenePoint, val bounds: SceneRect)

/** Move a flat vertical straight-edge label beside its line when that space is clear. */
internal fun flowVerticalEdgeLabels(
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
        if (index in routes || points.size != 2 || abs(points[0].x - points[1].x) > 1e-6) continue
        val original = occupied.getValue(index)
        val size = sizes.getValue(index)
        val candidates = listOf(-1.0, 1.0).map { side ->
            placement(index, original.origin.copy(x = original.origin.x + side * (size.width / 2 + 8.0)))
        }
        val chosen = candidates.firstOrNull { candidate ->
            val r = candidate.bounds
            val padded = SceneRect(r.x - 3.0, r.y - 3.0, r.width + 6.0, r.height + 6.0)
            val obstacle = listOf(OrthogonalGeometry.RectEntry("label", padded))
            nodes.values.none { overlaps(padded, it) } &&
                occupied.none { (other, label) -> other != index && overlaps(padded, label.bounds) } &&
                // Bounding-box rejection is conservative for diagonal edges: it may keep
                // the original label, but never accepts a candidate through a diagonal.
                segments.none { (a, b) -> OrthogonalGeometry.segmentHitsAnyRect(a, b, obstacle) }
        } ?: continue
        occupied[index] = chosen
        result[index] = chosen
    }
    return result
}
