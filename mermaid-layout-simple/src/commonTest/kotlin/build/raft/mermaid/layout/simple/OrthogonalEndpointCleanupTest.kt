package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class OrthogonalEndpointCleanupTest {
    @Test fun degenerateAndLayoutOnlyPathsPreserveInputs() {
        val point=ScenePoint(1.0,2.0)
        assertEquals(emptyList(),OrthogonalPolylineCleanup.orthogonalize(emptyList()))
        assertEquals(listOf(point),OrthogonalPolylineCleanup.orthogonalize(listOf(point)))
        val input=listOf(
            OrthogonalEndpointRoute("empty",emptyList()),
            OrthogonalEndpointRoute("singleton",listOf(point)),
            OrthogonalEndpointRoute("hidden",listOf(ScenePoint(0.0,0.0),ScenePoint(20.0,0.0)),"A","B",true),
        )
        val bounds=mapOf("A" to SceneRect(-5.0,-5.0,10.0,10.0),"B" to SceneRect(15.0,-5.0,10.0,10.0))
        assertEquals(input,OrthogonalEndpointCleanup.clipToBoundaries(input,bounds))
        assertEquals(input,OrthogonalEndpointCleanup.prepareForRenderer(input,bounds))
    }
    @Test fun nativeResultsDoNotMutateCallerRoutes() {
        val points=mutableListOf(ScenePoint(0.0,0.0),ScenePoint(20.0,0.0))
        val original=points.toList()
        val route=OrthogonalEndpointRoute("ab",points,"A","B")
        val result=OrthogonalEndpointCleanup.clipToBoundaries(listOf(route),mapOf(
            "A" to SceneRect(-5.0,-5.0,10.0,10.0),"B" to SceneRect(15.0,-5.0,10.0,10.0),
        )).single()
        assertEquals(original,points)
        assertEquals(listOf(ScenePoint(5.0,0.0),ScenePoint(15.0,0.0)),result.points)
        assertEquals("ab",result.id)
    }
    @Test fun productionGateCleansRectanglesButPreservesDiagonalAndDiamondRoutes() {
        val rectangles=FlowchartDiagram(FlowDirection.LR,listOf(FlowNode("A","A"),FlowNode("B","B")),listOf(FlowEdge("A","B")))
        val bounds=mapOf("A" to SceneRect(-10.0,-10.0,20.0,20.0),"B" to SceneRect(30.0,-20.0,20.0,40.0))
        val corner=listOf(ScenePoint(10.0,8.0),ScenePoint(30.0,8.0))
        assertEquals(listOf(ScenePoint(10.0,6.0),ScenePoint(30.0,6.0)),cleanFlowEndpointPaths(rectangles,bounds,mapOf(0 to corner)).getValue(0))
        val diagonal=listOf(ScenePoint(10.0,8.0),ScenePoint(30.0,12.0))
        assertEquals(diagonal,cleanFlowEndpointPaths(rectangles,bounds,mapOf(0 to diagonal)).getValue(0))
        val diamond=rectangles.copy(nodes=listOf(FlowNode("A","A",FlowNodeShape.DIAMOND),FlowNode("B","B")))
        assertEquals(corner,cleanFlowEndpointPaths(diamond,bounds,mapOf(0 to corner)).getValue(0))
    }
    @Test fun actualDrawingUsesCleanedRoutesWithoutRendererDuplicateTerminals() {
        val parsed=assertIs<MermaidParseResult.Success>(MermaidParser.parse("flowchart LR\nA[Input] --> B[Review]\nB -->|retry| A\nB --> C[Done]"))
        val scene=SimpleMermaidLayout.layout(parsed.diagram,TextMeasurer { text,style->SceneSize(text.length*7.0,style.fontSize) },LayoutConfig(validateOrthogonalLayout=true))
        val report=assertNotNull(scene.layoutValidation)
        val painted=scene.commands.mapNotNull { when(it){is DrawLine->listOf(it.from,it.to);is DrawPolyline->it.points;else->null} }
        assertEquals(3,report.geometry.edges.size)
        for(edge in report.geometry.edges) {
            assertTrue(edge.points in painted,"Diagnostic route must equal actual painted route")
            assertNotEquals(edge.points[0],edge.points[1])
            assertNotEquals(edge.points[edge.points.lastIndex-1],edge.points.last())
        }
    }
}
