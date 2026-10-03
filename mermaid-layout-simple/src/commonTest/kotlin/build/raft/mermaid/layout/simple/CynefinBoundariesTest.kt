package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.abs
import kotlin.test.*

class CynefinBoundariesTest {
    private fun scene(seed: Double? = null, amplitude: Double = 8.0): LayoutScene = SimpleMermaidLayout.layout(
        CynefinDiagram(domains = emptyList(), transitions = emptyList()), FixedWidthTextMeasurer,
        LayoutConfig(cynefin = CynefinBoundaryConfig(seed, "stable-diagram", amplitude)),
    )

    @Test fun actualRendererUsesSeededCubicBoundariesAndTheSameEllipseGeometry() {
        val scene = scene(42.0)
        val rectangles = scene.commands.filterIsInstance<DrawRect>()
        assertEquals(4, rectangles.size); assertTrue(rectangles.all { it.strokeWidth == 0.0 })
        val left = rectangles.minOf { it.rect.x }; val top = rectangles.minOf { it.rect.y }
        val width = rectangles.maxOf { it.rect.x + it.rect.width } - left
        val height = rectangles.maxOf { it.rect.y + it.rect.height } - top
        val expected = listOf(CynefinBoundaries.fold(width,height,42.0,8.0),CynefinBoundaries.horizontal(width,height,142.0,8.0),CynefinBoundaries.cliff(width,height))
        val paths = scene.commands.filterIsInstance<DrawPolyline>()
        assertEquals(3, paths.size)
        expected.zip(paths).forEach { (geometry, drawing) ->
            assertEquals(geometry.sampledPoints().map { ScenePoint(it.x+left,it.y+top) }, drawing.points)
            assertTrue(drawing.points.all { it.x in 0.0..scene.width && it.y in 0.0..scene.height })
        }
        assertEquals(3.0, paths.last().strokeWidth)
        assertEquals(StrokePattern.DASHED, paths.first().pattern)
        assertEquals(SceneColor(DiagramPalette.RED), paths.last().stroke)
        val ellipse=scene.commands.filterIsInstance<DrawEllipse>().single()
        val geometry=CynefinBoundaries.confusion(left+width/2,top+height/2,width*0.15,height*0.15)
        assertEquals(geometry.center,ellipse.center);assertEquals(geometry.radiusX,ellipse.radiusX);assertEquals(geometry.radiusY,ellipse.radiusY)
        assertEquals(scene,scene(42.0));assertNotEquals(paths,scene(43.0).commands.filterIsInstance<DrawPolyline>())
    }

    @Test fun zeroAmplitudeIsStraightAndNonFiniteSeedsUseTheStableIdentity() {
        val paths=scene(42.0,0.0).commands.filterIsInstance<DrawPolyline>()
        val x=paths[0].points.first().x; val y=paths[1].points.first().y
        assertTrue(paths[0].points.all { abs(it.x-x)<1e-9 });assertTrue(paths[1].points.all { abs(it.y-y)<1e-9 })
        assertEquals(scene(null),scene(0.0));assertEquals(scene(null),scene(Double.NaN));assertEquals(scene(null),scene(Double.POSITIVE_INFINITY))
        assertEquals(7, CynefinBoundaries.fold(800.0,600.0,42.0).curves.size)
        assertEquals(2, CynefinBoundaries.cliff(800.0,600.0).curves.size)
    }

    @Test fun documentModelFeedsExistingLabelsAndAccessibility() {
        val doc=CynefinDocument();doc.title="Decisions";doc.accessibilityTitle="Accessible decisions"
        doc.setDomains(listOf(CynefinDomainBlock(CynefinDomain.COMPLEX,listOf("Explore"))))
        val scene=SimpleMermaidLayout.layout(doc.diagram(),FixedWidthTextMeasurer,LayoutConfig())
        assertEquals("Accessible decisions",scene.accessibilityTitle)
        assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text=="Explore" })
        assertEquals(3,scene.commands.filterIsInstance<DrawPolyline>().size)
    }
    @Test fun contiguousDomainsStillHaveNonzeroCurvedTransitionsAndSelfLoopsAreIgnored() {
        val diagram=CynefinDiagram(domains=emptyList(),transitions=listOf(
            CynefinTransition(CynefinDomain.COMPLEX,CynefinDomain.COMPLICATED,"Pattern"),
            CynefinTransition(CynefinDomain.CONFUSION,CynefinDomain.CHAOTIC),
            CynefinTransition(CynefinDomain.CLEAR,CynefinDomain.CLEAR),
        ))
        val scene=SimpleMermaidLayout.layout(diagram,FixedWidthTextMeasurer,LayoutConfig())
        val arrows=scene.commands.filterIsInstance<DrawPolyline>().drop(3)
        assertEquals(2,arrows.size)
        arrows.forEach { assertNotEquals(it.points.first(),it.points.last());assertTrue(it.points.all { p->p.x.isFinite()&&p.y.isFinite() }) }
        val first=arrows.first().points
        assertEquals(first.first().y,first.last().y)
        assertTrue(first[12].y>first.first().y)
        assertEquals(2,scene.commands.filterIsInstance<DrawPolygon>().size)
    }

}
