package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.pow
import kotlin.test.*

class UsecaseEllipseLayoutTest {
    private fun scene(source: String, measurer: TextMeasurer = FixedWidthTextMeasurer): LayoutScene =
        SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, measurer, LayoutConfig())

    private fun normalized(p: ScenePoint, e: DrawEllipse): Double =
        ((p.x - e.center.x) / e.radiusX).pow(2) + ((p.y - e.center.y) / e.radiusY).pow(2)

    @Test fun wrappedTextCornersStayInsideEllipseWithAsciiCjkAndHostMetrics() {
        val host = TextMeasurer { text, style -> SceneSize(text.length * style.fontSize * .7, style.fontSize * 1.4) }
        for (measurer in listOf(FixedWidthTextMeasurer, host)) for (label in listOf("abcd ".repeat(80), "中文说明".repeat(50))) {
            val s = scene("usecase-beta\nU(\"$label\")\nstyle U fill:#dbeafe", measurer)
            val e = s.commands.filterIsInstance<DrawEllipse>().single()
            val rows = s.commands.filterIsInstance<DrawText>()
            assertTrue(rows.size > 4)
            for (row in rows) {
                val size = measurer.measure(row.text, row.style)
                for (x in listOf(row.origin.x - size.width / 2, row.origin.x + size.width / 2))
                    for (y in listOf(row.origin.y - size.height, row.origin.y + row.style.fontSize * .25))
                        assertTrue(normalized(ScenePoint(x, y), e) < 1.0, "Text corner outside ellipse: $row")
            }
        }
    }

    @Test fun resizedEllipsesKeepConnectionsAndBoundaryContainmentInAllDirections() {
        for (direction in listOf("LR", "RL", "TB", "BT")) {
            val s = scene("usecase-beta\ndirection $direction\nsystemBoundary B\nU(\"${"abcd ".repeat(80)}\")\nV[Result]\nend\nU --> V")
            val e = s.commands.filterIsInstance<DrawEllipse>().single()
            val edge = s.commands.filterIsInstance<DrawLine>().single()
            assertEquals(1.0, normalized(edge.from, e), 1e-9, direction)
            val boundary = s.commands.filterIsInstance<DrawRect>().single { it.cornerRadius == 8.0 }.rect
            assertTrue(e.center.x - e.radiusX >= boundary.x, direction)
            assertTrue(e.center.x + e.radiusX <= boundary.x + boundary.width, direction)
            assertTrue(e.center.y - e.radiusY >= boundary.y, direction)
            assertTrue(e.center.y + e.radiusY <= boundary.y + boundary.height, direction)
            val rect = s.commands.filterIsInstance<DrawRect>().single { it.cornerRadius == 4.0 }.rect
            assertTrue(e.center.y + e.radiusY < rect.y || rect.y + rect.height < e.center.y - e.radiusY || e.center.x + e.radiusX < rect.x || rect.x + rect.width < e.center.x - e.radiusX, direction)
        }
    }

    @Test fun shortLabelsRetainExistingEllipseDimensions() {
        val extended = scene("usecase-beta\nU(Login)\nstyle U fill:#dbeafe").commands.filterIsInstance<DrawEllipse>().single()
        assertEquals(90.0, extended.radiusX)
        assertEquals(38.0, extended.radiusY)
        val plain = scene("usecase-beta\nU(Login)").commands.filterIsInstance<DrawEllipse>().single()
        assertEquals(75.0, plain.radiusX)
        assertEquals(38.0, plain.radiusY)
    }
}
