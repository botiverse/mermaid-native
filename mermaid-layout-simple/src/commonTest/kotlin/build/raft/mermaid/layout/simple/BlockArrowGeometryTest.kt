package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.abs
import kotlin.test.*

class BlockArrowGeometryTest {
    private val label = SceneSize(100.0, 20.0)
    private val measure = TextMeasurer { _, _ -> label }
    private fun scene(nodes: List<BlockNode>, columns: Int = 2, edges: List<BlockEdge> = emptyList()) =
        SimpleMermaidLayout.layout(BlockDiagram(columns, nodes, edges), measure, LayoutConfig())
    private fun node(id: String, directions: List<String>, span: Int = 1) = BlockNode(id, id, span, "block_arrow", directions = directions)
    private fun onBoundary(p: ScenePoint, points: List<ScenePoint>): Boolean = points.indices.any { i ->
        val a = points[i]; val b = points[(i + 1) % points.size]
        abs((b.x-a.x)*(p.y-a.y)-(b.y-a.y)*(p.x-a.x)) < 1e-7 &&
            p.x >= minOf(a.x,b.x)-1e-7 && p.x <= maxOf(a.x,b.x)+1e-7 &&
            p.y >= minOf(a.y,b.y)-1e-7 && p.y <= maxOf(a.y,b.y)+1e-7
    }

    @Test fun spanWidthOnlyAppliesWhenPositionedAndWiderThanNatural() {
        val natural = BlockArrowGeometry.shape(listOf("x"), label, 10.0)
        assertEquals(150.0, natural.width)
        assertEquals(40.0, natural.height)
        assertEquals(natural, BlockArrowGeometry.shape(listOf("x"), label, 10.0, false, 2, 500.0))
        assertEquals(natural, BlockArrowGeometry.shape(listOf("x"), label, 10.0, true, 1, 500.0))
        assertEquals(natural, BlockArrowGeometry.shape(listOf("x"), label, 10.0, true, 2, 100.0))
        assertEquals(500.0, BlockArrowGeometry.shape(listOf("x"), label, 10.0, true, 2, 500.0).width)
    }

    @Test fun allDirectionSubsetsReachProductionWithTheirExactTranslatedPolygon() {
        val directions = listOf("right", "left", "up", "down")
        for (mask in 1..15) {
            val subset = directions.filterIndexed { index, _ -> mask and (1 shl index) != 0 }
            val s = scene(listOf(node("A", subset)), 1)
            val poly = s.commands.filterIsInstance<DrawPolygon>().single()
            val text = s.commands.filterIsInstance<DrawText>().single()
            val center = ScenePoint(text.origin.x, text.origin.y - 6.0)
            val raw = BlockArrowGeometry.shape(subset, label)
            assertEquals(raw.points.map { ScenePoint(it.x + center.x, it.y + center.y) }, poly.points, "$subset")
            assertTrue(poly.points.all { it.x >= 0.0 && it.y >= 0.0 && it.x <= s.width && it.y <= s.height })
        }
    }

    @Test fun spanningFourWayArrowReservesTipsAndKeepsItsLabelCentered() {
        val s = scene(listOf(node("A", listOf("x", "y"), 2), BlockNode("B", "B")), 2)
        val points = s.commands.filterIsInstance<DrawPolygon>().single().points
        val text = s.commands.filterIsInstance<DrawText>().first()
        val rect = s.commands.filterIsInstance<DrawRect>().single().rect
        assertEquals((points.minOf { it.x } + points.maxOf { it.x }) / 2.0, text.origin.x)
        assertTrue(points.maxOf { it.x } - points.minOf { it.x } > 2.0 * rect.width)
        assertTrue(points.maxOf { it.y } < rect.y)
        assertTrue(points.all { it.x >= 24.0 && it.x <= s.width - 24.0 && it.y >= 24.0 && it.y <= s.height - 24.0 })
    }

    @Test fun nestedArrowsStayInsideTheirContainerAndPreserveStyle() {
        val child = node("A", listOf("x", "y")).copy(styles = listOf("fill:#ff0000", "stroke:#00ff00", "stroke-width:3"))
        val s = scene(listOf(BlockNode("G", "Group", type = "composite", children = listOf(child))), 1)
        val box = s.commands.filterIsInstance<DrawRect>().single().rect
        val polygon = s.commands.filterIsInstance<DrawPolygon>().single()
        assertEquals(SceneColor("#ff0000"), polygon.fill)
        val outline = s.commands.filterIsInstance<DrawPolyline>().single()
        assertEquals(SceneColor("#00ff00"), outline.stroke)
        assertEquals(3.0, outline.strokeWidth)
        assertEquals(polygon.points + polygon.points.first(), outline.points)
        assertTrue(polygon.points.all { it.x > box.x && it.x < box.x + box.width && it.y > box.y && it.y < box.y + box.height })
    }

    @Test fun diagonalEdgeTerminatesOnPaintedArrowBoundaryRatherThanBoundingRectangle() {
        val s = scene(listOf(node("A", listOf("right")), BlockNode("Space", "", type = "space"),
            BlockNode("Space2", "", type = "space"), node("B", listOf("left"))), 2, listOf(BlockEdge("A", "B")))
        val polygons = s.commands.filterIsInstance<DrawPolygon>().filter { it.points.size > 3 }
        val line = s.commands.filterIsInstance<DrawLine>().single()
        assertTrue(onBoundary(line.from, polygons[0].points))
        assertTrue(onBoundary(line.to, polygons[1].points))
        val texts = s.commands.filterIsInstance<DrawText>()
        val ca = ScenePoint(texts[0].origin.x, texts[0].origin.y - 6.0)
        val cb = ScenePoint(texts[1].origin.x, texts[1].origin.y - 6.0)
        assertTrue(abs((line.to.x-line.from.x)*(cb.y-ca.y)-(line.to.y-line.from.y)*(cb.x-ca.x)) < 1e-7)
    }

    @Test fun diagonalArrowToCircleConnectionStillEndsOnTheCircle() {
        val s = scene(listOf(node("A", listOf("right")), BlockNode("S", "", type = "space"),
            BlockNode("T", "", type = "space"), BlockNode("B", "Circle", type = "circle")), 2, listOf(BlockEdge("A", "B")))
        val circle = s.commands.filterIsInstance<DrawEllipse>().single()
        val line = s.commands.filterIsInstance<DrawLine>().single()
        val dx = line.to.x - circle.center.x; val dy = line.to.y - circle.center.y
        assertEquals(circle.radiusX * circle.radiusX, dx * dx + dy * dy, 1e-7)
    }

    @Test fun parserAxesAndDuplicateDirectionsReachTheSameScene() {
        fun parsed(directions: String): LayoutScene {
            val source = "block-beta\ncolumns 1\nA<[\"Arrow\"]>($directions)"
            val d = assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram
            return SimpleMermaidLayout.layout(d, measure, LayoutConfig())
        }
        assertEquals(parsed("right,left,up,down"), parsed("x,right,y,up"))
    }
}
