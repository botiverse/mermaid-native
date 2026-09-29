package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class UsecaseActorLayoutTest {
    private val measurer = TextMeasurer { text, style -> SceneSize(text.length * style.fontSize * .55, style.fontSize) }
    private fun scene(metadata: String, extra: String = "") = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse("usecase-beta\nactor User@{ $metadata }\n$extra")).diagram, measurer, LayoutConfig())

    @Test fun productionTypesDrawDistinctGeometryAndPreserveDefaultStickFigure() {
        val normal = scene("type: normal")
        assertEquals(1, normal.commands.filterIsInstance<DrawEllipse>().size)
        assertEquals(4, normal.commands.filterIsInstance<DrawLine>().size)
        assertEquals(10.0, normal.commands.filterIsInstance<DrawEllipse>().single().radiusX)
        val hollow = scene("type: hollow")
        assertEquals(0.0, hollow.commands.filterIsInstance<DrawEllipse>().single().fillOpacity)
        assertEquals(12, hollow.commands.filterIsInstance<DrawPolyline>().single().points.size)
        val awesome = scene("type: awesome")
        assertEquals(SceneColor(DiagramPalette.BLUE), awesome.commands.filterIsInstance<DrawEllipse>().single().fill)
        assertEquals(48.0, awesome.commands.filterIsInstance<DrawRect>().single().rect.width)
        val icon = scene("icon: \"missing:user\"")
        assertEquals(52.0, icon.commands.filterIsInstance<DrawRect>().single().rect.width)
        assertTrue(icon.commands.filterIsInstance<DrawText>().any { it.text == "?" })
        assertFalse(icon.commands.filterIsInstance<DrawText>().any { it.text.contains("missing:") })
    }

    @Test fun compactLabelIsDrawnOnItsActualConnection() {
        val result = scene("type: hollow", "User --important--> Login")
        assertTrue(result.commands.filterIsInstance<DrawText>().any { it.text == "important" })
        assertTrue(result.commands.filterIsInstance<DrawText>().any { it.text == "Login" })
    }

    @Test fun businessMarkerSharesStyleAndVariantEdgesStayOutsideBody() {
        val result = scene("type: hollow, business: true", "User --> Login\nstyle User stroke:#12506b")
        val head = result.commands.filterIsInstance<DrawEllipse>().first { it.radiusX == 9.0 }
        val lines = result.commands.filterIsInstance<DrawLine>()
        val marker = lines.single { it.stroke == SceneColor("#12506b") }
        assertEquals(head.stroke, marker.stroke)
        assertEquals(head.center.y - head.radiusY, marker.from.y)
        assertTrue(marker.to.x > head.center.x)
        val edge = lines.first { it.stroke != marker.stroke }
        assertEquals(head.center.x + 28.0, edge.from.x)
        assertTrue(result.commands.filterIsInstance<DrawText>().any { it.text == "«business»" })
    }
}
