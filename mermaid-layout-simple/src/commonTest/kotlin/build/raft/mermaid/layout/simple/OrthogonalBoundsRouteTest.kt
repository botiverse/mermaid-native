package build.raft.mermaid.layout.simple

import build.raft.mermaid.layout.*
import build.raft.mermaid.core.*
import kotlin.test.*

class OrthogonalBoundsRouteTest {
    @Test fun excludesGroupsLabelsAndNonpositiveBoundsWhilePreservingOrderAndRectangles() {
        val a = OrthogonalGeometry.nodeBoundsFromCenter("A", 10.0, 20.0, 40.0, 20.0)
        val b = OrthogonalGeometry.NodeBounds("B", SceneRect(101.125, 12.75, 9.5, 15.25))
        val result = OrthogonalGeometry.collectRealNodeBounds(listOf(a, a.copy(id="group", isGroup=true),
            a.copy(id="label", isEdgeLabel=true), a.copy(id="empty", rect=SceneRect(0.0, 0.0, 0.0, 2.0)),
            a.copy(id="negative", rect=SceneRect(0.0, 0.0, 2.0, -2.0)), b))
        assertEquals(listOf("A", "B"), result.map { it.id })
        assertEquals(SceneRect(-10.0, 10.0, 40.0, 20.0), result.first().rect)
        assertSame(b.rect, result.last().rect)
    }

    @Test fun mixedTrackOptimizerPreservesRoutesWithWrongMiddleOrientation() {
        val diagram = assertIs<FlowchartDiagram>(assertIs<MermaidParseResult.Success>(
            MermaidParser.parse("graph LR\nA-->B\nB-->A")
        ).diagram)
        val rects = mapOf("A" to SceneRect(-10.0, -10.0, 20.0, 20.0), "B" to SceneRect(90.0, -10.0, 20.0, 20.0))
        val wrongMiddle = FlowReturnRoute(listOf(ScenePoint(10.0, 0.0), ScenePoint(30.0, 0.0),
            ScenePoint(40.0, 0.0), ScenePoint(40.0, 20.0), ScenePoint(60.0, 20.0), ScenePoint(90.0, 20.0)),
            null, SceneRect(10.0, 0.0, 80.0, 20.0))
        val crossing = FlowReturnRoute(listOf(ScenePoint(35.0, -30.0), ScenePoint(35.0, 30.0)),
            null, SceneRect(35.0, -30.0, 0.0, 60.0))
        val routes = mapOf(0 to wrongMiddle, 1 to crossing)
        assertEquals(routes, flowMixedRoutes(diagram, rects, routes, FixedWidthTextMeasurer))
    }

    @Test fun classifiesBothAxesAndRejectsDegenerateDiagonalAndWrongLengthRoutes() {
        val hvh = listOf(ScenePoint(0.0, 0.0), ScenePoint(10.0, 0.0), ScenePoint(10.0, 20.0), ScenePoint(30.0, 20.0))
        assertEquals(OrthogonalGeometry.RouteKind.HVH, OrthogonalGeometry.classifyThreeSegmentRoute(hvh)?.kind)
        assertEquals(OrthogonalGeometry.RouteKind.VHV, OrthogonalGeometry.classifyThreeSegmentRoute(hvh.map { ScenePoint(it.y, it.x) })?.kind)
        assertNull(OrthogonalGeometry.classifyThreeSegmentRoute(hvh.take(3)))
        assertNull(OrthogonalGeometry.classifyThreeSegmentRoute(listOf(hvh[0], hvh[0], hvh[2], hvh[3])))
        assertNull(OrthogonalGeometry.classifyThreeSegmentRoute(listOf(hvh[0], ScenePoint(10.0, 10.0), hvh[2], hvh[3])))
        val near = listOf(hvh[0], ScenePoint(10.0, 0.00001), hvh[2], hvh[3])
        assertNotNull(OrthogonalGeometry.classifyThreeSegmentRoute(near))
        assertNull(OrthogonalGeometry.classifyThreeSegmentRoute(near, 0.000001))
    }
}
