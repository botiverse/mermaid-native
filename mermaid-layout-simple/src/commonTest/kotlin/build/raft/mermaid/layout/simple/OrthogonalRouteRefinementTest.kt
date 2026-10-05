package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class OrthogonalRouteRefinementTest {
    private fun p(x:Int,y:Int)=ScenePoint(x.toDouble(),y.toDouble())
    private fun node(id:String,x:Double,y:Double,w:Double=40.0,h:Double=40.0,label:Boolean=false)=
        OrthogonalGeometry.nodeBoundsFromCenter(id,x,y,w,h,false,label)
    @Test fun obstaclesAndHiddenRoutesPreventUnsafePortSwaps() {
        val points=listOf(p(20,0),p(60,0),p(60,100),p(80,100))
        val route=OrthogonalRefinementRoute("ab",points,"A","B")
        val nodes=listOf(node("A",0.0,0.0),node("B",100.0,100.0))
        assertEquals(listOf(p(0,20),p(0,100),p(80,100)),OrthogonalRouteRefinement.swapPorts(listOf(route),nodes).single().points)
        assertEquals(listOf(route),OrthogonalRouteRefinement.swapPorts(listOf(route),nodes+node("obstacle",0.0,55.0,60.0,30.0)))
        val hidden=route.copy(isLayoutOnly=true)
        assertEquals(listOf(hidden),OrthogonalRouteRefinement.swapPorts(listOf(hidden),nodes))
        assertEquals(points,route.points)
    }
    @Test fun terminalLabelReanchorsWithoutMutatingInputsAndObstacleBlocksTheRewrite() {
        val route=OrthogonalRefinementRoute("ab",listOf(p(0,120),p(70,120),p(70,100),p(78,100)),"A","B",false,"label")
        val nodes=listOf(node("B",100.0,110.0),node("label",10.0,150.0,10.0,5.0,true))
        val result=OrthogonalRouteRefinement.collapseTerminalStubs(listOf(route),nodes)
        assertEquals(listOf(p(0,120),p(100,120),p(100,130)),result.routes.single().points)
        val moved=result.nodes.last().rect
        assertEquals(SceneRect(45.0,117.5,10.0,5.0),moved)
        assertEquals(150.0,nodes.last().rect.y+2.5)
        val blocked=OrthogonalRouteRefinement.collapseTerminalStubs(listOf(route),nodes+node("block",100.0,125.0,4.0,4.0))
        assertEquals(listOf(route),blocked.routes)
    }
    @Test fun sharedRailSearchKeepsEndpointsAndStopsWhenAllCandidatesAreBlocked() {
        val first=OrthogonalRefinementRoute("a",listOf(p(0,0),p(50,0),p(50,100),p(80,100)))
        val second=OrthogonalRefinementRoute("b",listOf(p(52,0),p(52,100)))
        val input=listOf(first,second)
        val moved=OrthogonalRouteRefinement.nudgeSharedTracks(input,emptyList())
        assertEquals(p(43,0),moved[0].points[1])
        assertEquals(first.points.first(),moved[0].points.first())
        assertEquals(first.points.last(),moved[0].points.last())
        val blocked=OrthogonalRouteRefinement.nudgeSharedTracks(input,listOf(node("block",50.0,50.0,100.0,90.0)))
        assertEquals(input,blocked)
    }
    @Test fun productionPassShortensOnlySafeRectangleRoutes() {
        val diagram=FlowchartDiagram(FlowDirection.LR,listOf(FlowNode("A","A"),FlowNode("B","B")),listOf(FlowEdge("A","B")))
        val nodes=mapOf("A" to SceneRect(-20.0,-20.0,40.0,40.0),"B" to SceneRect(80.0,80.0,40.0,40.0))
        val input=mapOf(0 to listOf(p(20,0),p(60,0),p(60,100),p(80,100)))
        val refined=refineFlowOrthogonalPaths(diagram,nodes,input)
        assertEquals(listOf(p(0,20),p(0,100),p(80,100)),refined.getValue(0))
        val diamond=diagram.copy(nodes=listOf(FlowNode("A","A",FlowNodeShape.DIAMOND),FlowNode("B","B")))
        assertEquals(input,refineFlowOrthogonalPaths(diamond,nodes,input))
        val diagonal=mapOf(0 to listOf(p(20,0),p(80,100)))
        assertEquals(diagonal,refineFlowOrthogonalPaths(diagram,nodes,diagonal))
    }
    @Test fun actualRenderedLoopRetainsArrowClearanceAfterTrackSeparation() {
        val source="""flowchart LR
N0[Node 0]
N1[Node 1]
N2[Node 2]
N3[Node 3]
N1 --> N2
N3 --> N3
N1 --> N3
N2 --> N2
N0 --> N2
N2 --> N1
N2 --> N2
N0 --> N3"""
        val diagram=assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram
        val scene=SimpleMermaidLayout.layout(diagram,TextMeasurer { text,style->SceneSize(text.length*7.0,style.fontSize) },LayoutConfig(validateOrthogonalLayout=true))
        val painted=scene.commands.mapNotNull { when(it){is DrawLine->listOf(it.from,it.to);is DrawPolyline->it.points;else->null} }
        val routes=assertNotNull(scene.layoutValidation).geometry.edges
        assertEquals(8,routes.size)
        for(edge in routes) {
            assertTrue(edge.points in painted)
            for((a,b) in listOf(edge.points[0] to edge.points[1],edge.points[edge.points.lastIndex-1] to edge.points.last()))
                assertTrue(kotlin.math.hypot(b.x-a.x,b.y-a.y)>=8,"Track separation must retain arrow clearance")
        }
    }

}
