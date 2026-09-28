package build.raft.mermaid.layout.simple
import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*
class FlowGrammarLayoutTest {
    private fun layout(s:String)=SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse("graph LR\n$s")).diagram,FixedWidthTextMeasurer,LayoutConfig())
    @Test fun openAndInvisibleEdgesDoNotDrawArrowheads() {
        val open=layout("A --- B")
        assertTrue(open.commands.none { it is DrawPolygon })
        assertEquals(1,open.commands.filterIsInstance<DrawLine>().size)
        assertEquals(0,layout("A ~~~ B").commands.filterIsInstance<DrawLine>().size)
    }
    @Test fun circleAndCrossMarkersReachTheActualDrawing() {
        val circle=layout("A o--o B")
        assertEquals(2,circle.commands.filterIsInstance<DrawEllipse>().size)
        val cross=layout("A x--x B")
        assertEquals(5,cross.commands.filterIsInstance<DrawLine>().size)
    }
    @Test fun localDirectionNestedBoundsAndMinimumEdgeLengthAffectPlacement() {
        val scene=layout("subgraph Outer[Outer group]\nsubgraph Inner[Inner group]\ndirection TB\nA --> B\nend\nend")
        val labels=scene.commands.filterIsInstance<DrawText>().associateBy { it.text }
        assertTrue(labels.getValue("B").origin.y>labels.getValue("A").origin.y)
        val groups=scene.commands.filterIsInstance<DrawRect>().filter { it.fill.value=="#f7f7f7" }
        assertEquals(2,groups.size)
        val outer=groups[0].rect;val inner=groups[1].rect
        assertTrue(inner.x>=outer.x && inner.y>=outer.y && inner.x+inner.width<=outer.x+outer.width && inner.y+inner.height<=outer.y+outer.height)
        val short=layout("A --> B");val long=layout("A ----> B")
        assertTrue(long.width>short.width)
    }
    @Test fun circlesHaveEqualRadiiEvenWithWideLabels() {
        val scene=layout("A((A wide circle label))")
        val circle=scene.commands.filterIsInstance<DrawEllipse>().single()
        assertEquals(circle.radiusX,circle.radiusY)
    }
    @Test fun ellipseAndPartialRectangleBordersAreActuallyDrawn() {
        val ellipse=layout("A(-Wide elliptical node-)").commands.filterIsInstance<DrawEllipse>().single()
        assertTrue(ellipse.radiusX>ellipse.radiusY)
        val partial=layout("B[|borders:lt|Partial border]")
        assertEquals("none",partial.commands.filterIsInstance<DrawRect>().single().stroke.value)
        assertEquals(2,partial.commands.filterIsInstance<DrawLine>().size)
    }
    @Test fun addedNodeShapesAreDistinctGlyphs() {
        val scene=layout("A{{Hexagon}}\nB[[Subroutine]]\nC[(Database)]\nD[/Parallel/]\nE>Asymmetric]")
        assertEquals(3,scene.commands.filterIsInstance<DrawPolygon>().size)
        assertEquals(2,scene.commands.filterIsInstance<DrawEllipse>().size)
        assertEquals(5,scene.commands.filterIsInstance<DrawText>().size)
    }
}
