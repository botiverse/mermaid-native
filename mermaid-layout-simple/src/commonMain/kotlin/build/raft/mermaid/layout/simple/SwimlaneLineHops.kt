package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.SwimlaneLineHops
import build.raft.mermaid.layout.*

/** Only registered edge strokes participate; node outlines and marker polygons never do. */
internal fun applySwimlaneHops(commands: MutableList<DrawCommand>, edgeIndices: List<Int>, setting: SwimlaneLineHops): List<SceneRect> {
    if (setting == SwimlaneLineHops.DISABLED) return emptyList()
    val edges = edgeIndices.map { index ->
        val points = when (val command = commands[index]) {
            is DrawLine -> listOf(command.from, command.to)
            is DrawPolyline -> command.points
            else -> error("Registered edge is not a line")
        }
        // Native markers are separate polygons. These points are already clipped; no SVG marker offset.
        LineJumps.Edge(index.toString(), points)
    }
    val jumping = LineJumps.findEdgeIntersections(edges).map { it.jumpEdgeId }.toSet()
    if (jumping.isEmpty()) return emptyList()
    val geometry = LineJumps.processGeometry(edges, LineJumps.Config(style = if (setting == SwimlaneLineHops.GAP) LineJumps.Style.GAP else LineJumps.Style.ARC))
    return edgeIndices.filter { it.toString() in jumping }.mapNotNull { index ->
        val path = when (val command = commands[index]) {
            is DrawLine -> DrawPath(geometry.getValue(index.toString()).segments, command.stroke, command.strokeWidth, command.pattern)
            is DrawPolyline -> DrawPath(geometry.getValue(index.toString()).segments, command.stroke, command.strokeWidth, command.pattern)
            else -> error("Registered edge is not a line")
        }
        commands[index] = path
        path.conservativeBounds()
    }
}
