package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class BlockShapeLayoutTest {
    private fun scene(source: String) = SimpleMermaidLayout.layout(
        assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram,
        FixedWidthTextMeasurer, LayoutConfig(),
    )

    @Test fun circleUsesRealBoundsForTheVerticalEdgeAndHonorsStyles() {
        val s = scene("""block-beta
columns 1
A(("A long circular label"))
B["Rectangle"]
A --> B
style A fill:#ffe0b2,stroke:#fb8c00,stroke-width:3px
""")
        val circle = s.commands.filterIsInstance<DrawEllipse>().single()
        assertEquals(circle.radiusX, circle.radiusY)
        assertEquals(SceneColor("#ffe0b2"), circle.fill)
        assertEquals(3.0, circle.strokeWidth)
        val edge = s.commands.filterIsInstance<DrawLine>().single()
        assertEquals(circle.center.x, edge.from.x, 1e-9)
        assertEquals(circle.center.y + circle.radiusY, edge.from.y, 1e-9)
        val rectangle = s.commands.filterIsInstance<DrawRect>().single()
        assertTrue(rectangle.rect.y > circle.center.y + circle.radiusY)
        assertEquals(rectangle.rect.y, edge.to.y)
    }

    @Test fun nestedCylinderHasTwoCapsAndStylesWithoutReplacingTheCircle() {
        val s = scene("""block-beta
columns 1
block:Group
columns 2
A(("Circle")) DB[("Database")]
end
style DB fill:#bbdefb,stroke:#1e88e5,stroke-width:4px
""")
        val ellipses = s.commands.filterIsInstance<DrawEllipse>()
        assertEquals(3, ellipses.size)
        val caps = ellipses.filter { it.radiusX != it.radiusY }
        assertEquals(2, caps.size)
        assertTrue(caps.all { it.fill == SceneColor("#bbdefb") && it.strokeWidth == 4.0 })
        assertEquals(caps.first().center.x, caps.last().center.x)
        assertTrue(caps.first().center.y > caps.last().center.y)
        assertTrue(s.width.isFinite() && s.height.isFinite())
    }
}
