package build.raft.mermaid.layout.simple

import build.raft.mermaid.layout.*

internal fun isBuiltinTreeViewIcon(icon: String?): Boolean =
    icon == "mermaid-treeview:file" || icon == "mermaid-treeview:folder"

/** Native vector glyphs; custom pack references remain readable text until a pack renderer exists. */
internal fun treeViewIconCommands(icon: String, origin: ScenePoint): List<DrawCommand> {
    fun p(x: Double, y: Double): ScenePoint = ScenePoint(origin.x + x, origin.y + y)
    val outline = SceneColor(DiagramPalette.SECONDARY)
    return when (icon) {
        "mermaid-treeview:folder" -> {
            val points = listOf(p(0.0, 2.0), p(7.0, 2.0), p(10.0, 5.0), p(18.0, 5.0), p(18.0, 16.0), p(0.0, 16.0))
            listOf(DrawPolygon(points, SceneColor(DiagramPalette.AMBER)), DrawPolyline(points + points.first(), outline, 1.0))
        }
        "mermaid-treeview:file" -> {
            val points = listOf(p(2.0, 0.0), p(11.0, 0.0), p(17.0, 6.0), p(17.0, 18.0), p(2.0, 18.0))
            listOf(
                DrawPolygon(points, SceneColor(DiagramPalette.BLUE_SURFACE)),
                DrawPolyline(points + points.first(), outline, 1.0),
                DrawPolyline(listOf(p(11.0, 0.0), p(11.0, 6.0), p(17.0, 6.0)), outline, 1.0),
                DrawLine(p(6.0, 10.0), p(13.0, 10.0), outline, 1.0),
                DrawLine(p(6.0, 13.0), p(13.0, 13.0), outline, 1.0),
            )
        }
        else -> emptyList()
    }
}
