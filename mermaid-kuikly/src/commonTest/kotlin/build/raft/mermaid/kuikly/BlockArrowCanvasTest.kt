package build.raft.mermaid.kuikly

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import build.raft.mermaid.layout.simple.*
import kotlin.test.*

class BlockArrowCanvasTest {
    @Test fun configuredGridRectanglesAndNestedLabelsReachCanvas() {
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse("---\nconfig:\n  block:\n    padding: 20\n---\nblock-beta\ncolumns 2\nblock:Group\nA B\nend\nC"))
        val scene = SimpleMermaidLayout.layout(parsed.diagram, FixedWidthTextMeasurer, LayoutConfig())
        val context = MockCanvasContext()
        MermaidKuiklyRenderer.render(scene, context)
        for (label in listOf("Group", "A", "B", "C")) assertTrue(context.log.any { it.startsWith("fillText($label,") })
        assertTrue(scene.commands.filterIsInstance<DrawRect>().size == 4)
        assertTrue(context.log.count { it == "fill" } >= 4)
    }
    @Test fun actualBlockArrowPolygonAndLabelReachNativeCanvas() {
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse("block-beta\ncolumns 1\nA<[\"Arrow\"]>(x,y):2"))
        val scene = SimpleMermaidLayout.layout(parsed.diagram, FixedWidthTextMeasurer, LayoutConfig())
        val polygon = scene.commands.filterIsInstance<DrawPolygon>().single()
        assertEquals(16, polygon.points.size)
        val context = MockCanvasContext()
        MermaidKuiklyRenderer.render(scene, context)
        val first = polygon.points.first()
        assertTrue(context.log.contains("moveTo(${first.x.toFloat()}, ${first.y.toFloat()})"))
        for (point in polygon.points.drop(1)) assertTrue(context.log.contains("lineTo(${point.x.toFloat()}, ${point.y.toFloat()})"))
        assertTrue(context.log.contains("closePath"))
        assertTrue(context.log.contains("fill"))
        assertTrue(context.log.contains("stroke"))
        assertTrue(context.log.any { it.startsWith("fillText(Arrow,") })
    }
}
