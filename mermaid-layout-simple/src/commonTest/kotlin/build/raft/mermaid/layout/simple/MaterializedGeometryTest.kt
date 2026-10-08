package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class MaterializedGeometryTest {
    private fun p(x:Int,y:Int)=ScenePoint(x.toDouble(),y.toDouble())
    private val measurer=TextMeasurer { text,style -> SceneSize(text.length*7.0,style.fontSize) }
    @Test fun identicalIdsRetainOccurrenceIdentityAndInputsStayImmutable() {
        val points=listOf(p(-30,0),p(-5,0))
        val edges=listOf(MaterializedGeometry.Edge("same","A","B",points),MaterializedGeometry.Edge("same","A","B",points))
        val nodes=mapOf("A" to MaterializedGeometry.Node("A",-40.0,-30.0,10.0,10.0),"B" to MaterializedGeometry.Node("B",0.0,0.0,10.0,80.0))
        val result=MaterializedGeometry.apply(MaterializedGeometry.Operation.SEPARATE_TERMINAL_LANES,edges,nodes)
        assertNotEquals(result.edges[0].points,result.edges[1].points)
        assertEquals(listOf(points,points),edges.map { it.points })
        assertEquals(nodes,result.nodes)
        val hidden=edges.map { it.copy(isLayoutOnly=true) }
        assertEquals(hidden,MaterializedGeometry.apply(MaterializedGeometry.Operation.SEPARATE_TERMINAL_LANES,hidden,nodes).edges)
    }
    @Test fun doglegWithoutEndpointsCollapsesButLabelsProtectTheirSpace() {
        val e=MaterializedGeometry.Edge(points=listOf(p(0,0),p(10,0),p(10,10),p(0,10),p(0,20)))
        val operation=MaterializedGeometry.Operation.COLLAPSE_DOGLEGS
        assertEquals(listOf(p(0,0),p(0,20)),MaterializedGeometry.apply(operation,listOf(e),emptyMap()).edges.single().points)
        val label=MaterializedGeometry.Node("label",0.0,10.0,2.0,2.0,isEdgeLabel=true)
        assertEquals(listOf(e),MaterializedGeometry.apply(operation,listOf(e),mapOf("label" to label)).edges)
    }
    @Test fun titleMovesAndExpandsGroupWithoutChangingInputOrNestedTitle() {
        val title=MaterializedGeometry.Bounds(-332.0,664.0,-36.0,-15.0)
        val group=MaterializedGeometry.Node("group",166.0,54.0,996.0,180.0,isGroup=true,direction="TD",groupTitleRect=title)
        val nested=group.copy(id="nested",parentId="group")
        val e=MaterializedGeometry.Edge(points=listOf(p(-166,21),p(-166,-20),p(830,-20),p(830,21)))
        val input=mapOf("different-map-key" to group,"nested" to nested)
        val result=MaterializedGeometry.apply(MaterializedGeometry.Operation.LIFT_TOP_TITLES,listOf(e),input)
        assertEquals(49.5,result.nodes.getValue("different-map-key").y)
        assertEquals(189.0,result.nodes.getValue("different-map-key").height)
        assertEquals(title.copy(top=-45.0,bottom=-24.0),result.nodes.getValue("different-map-key").groupTitleRect)
        assertEquals(nested,result.nodes.getValue("nested"));assertEquals(54.0,group.y)
    }
    @Test fun fixedDecimalKeysMatchBinaryRoundingAndNegativeRoundedZero() {
        assertEquals(materializedFixed3Key(1.0),materializedFixed3Key(1.0005))
        assertEquals(materializedFixed3Key(1.001),materializedFixed3Key(1.0005000000000002))
        assertEquals(materializedFixed3Key(-1.0),materializedFixed3Key(-1.0005))
        assertNotEquals(materializedFixed3Key(-0.0001),materializedFixed3Key(0.0))
        assertEquals(materializedFixed3Key(-0.0),materializedFixed3Key(0.0))
        assertEquals(materializedFixed3Key(Double.MIN_VALUE),materializedFixed3Key(0.0))
        assertNotEquals(materializedFixed3Key(1e20),materializedFixed3Key(1e20+16384))
    }
    @Test fun resolverRemovesActualCrossingWithDuplicateIds() {
        val nodes=listOf(MaterializedGeometry.Node("a",0.0,50.0,20.0,20.0),MaterializedGeometry.Node("b",200.0,50.0,20.0,20.0),MaterializedGeometry.Node("c",100.0,-50.0,20.0,20.0),MaterializedGeometry.Node("d",100.0,150.0,20.0,20.0)).associateBy { it.id!! }
        val edges=listOf(MaterializedGeometry.Edge("same","a","b",listOf(p(10,50),p(190,50))),MaterializedGeometry.Edge("same","c","d",listOf(p(100,-40),p(100,140))))
        val result=MaterializedGeometry.apply(MaterializedGeometry.Operation.RESOLVE_CROSSINGS,edges,nodes)
        assertEquals(1,MaterializedState(edges,nodes).crossings())
        assertEquals(0,MaterializedState(result.edges,result.nodes).crossings())
        assertEquals(nodes,result.nodes);assertEquals(2,edges[0].points!!.size)
    }
    @Test fun productionRailUsesRealObstaclesAndKeepsConsumerUnsupportedCases() {
        val diagram=FlowchartDiagram(FlowDirection.TD,listOf(FlowNode("a","a"),FlowNode("b","b"),FlowNode("obstacle","o")),listOf(FlowEdge("a","b")))
        val rects=mapOf("a" to SceneRect(-282.0,21.0,232.0,66.0),"b" to SceneRect(714.0,21.0,232.0,66.0),"obstacle" to SceneRect(50.0,0.0,232.0,108.0))
        val paths=mapOf(0 to listOf(p(-166,21),p(-166,1),p(830,1),p(830,21)))
        val result=materializeFlowGeometry(diagram,rects,emptyMap(),paths,measurer)
        assertEquals(listOf(p(-166,21),p(-166,-20),p(830,-20),p(830,21)),result.paths.getValue(0))
        assertEquals(paths,materializeFlowGeometry(diagram.copy(edges=listOf(FlowEdge("a","b",label="keep"))),rects,emptyMap(),paths,measurer).paths)
        assertEquals(paths,materializeFlowGeometry(diagram.copy(nodes=diagram.nodes.map { it.copy(shape=FlowNodeShape.DIAMOND) }),rects,emptyMap(),paths,measurer).paths)
    }
    @Test fun productionTitleExpansionKeepsBottomAndRejectsNewSiblingOverlap() {
        val group=FlowSubgraph("g","Manager",listOf("a","obstacle"))
        val diagram=FlowchartDiagram(FlowDirection.LR,listOf(FlowNode("a","a"),FlowNode("b","b"),FlowNode("obstacle","o")),listOf(FlowEdge("a","b")),listOf(group))
        val rects=mapOf("a" to SceneRect(-282.0,21.0,232.0,66.0),"b" to SceneRect(714.0,21.0,232.0,66.0),"obstacle" to SceneRect(50.0,0.0,232.0,108.0))
        val groups=mapOf("g" to SceneRect(-332.0,-36.0,996.0,180.0))
        val paths=mapOf(0 to listOf(p(-166,21),p(-166,1),p(830,1),p(830,21)))
        val result=materializeFlowGeometry(diagram,rects,groups,paths,measurer)
        val before=groups.getValue("g");val after=result.groups.getValue("g")
        assertTrue(after.y<before.y)
        assertEquals(before.y+before.height,after.y+after.height)
        assertEquals(before.x,after.x);assertEquals(before.width,after.width)
        val style=flowGroupStyle(group,diagram).text
        assertEquals(-24.0,after.y+6+style.fontSize)
        val sibling=FlowSubgraph("s","Other",emptyList())
        val withSibling=groups+("s" to SceneRect(-200.0,-60.0,300.0,22.0))
        val blocked=materializeFlowGeometry(diagram.copy(subgraphs=listOf(group,sibling)),rects,withSibling,paths,measurer)
        assertEquals(withSibling,blocked.groups)
        // A blocked title move must also roll back the coupled rail lift.
        assertEquals(paths,blocked.paths)
    }
    @Test fun paintedPathsGroupBoundsAndDiagnosticsAgreeInActualConsumer() {
        val source="""flowchart TD
subgraph g[Group]
A[A] --> B[B]
B --> C[C]
C --> A
A --> C
end
C --> D[D]
D --> D"""
        val parsed=assertIs<MermaidParseResult.Success>(MermaidParser.parse(source))
        val scene=SimpleMermaidLayout.layout(parsed.diagram,measurer,LayoutConfig(validateOrthogonalLayout=true))
        val geometry=assertNotNull(scene.layoutValidation).geometry
        val painted=scene.commands.mapNotNull { when(it) { is DrawLine -> listOf(it.from,it.to);is DrawPolyline -> it.points;else -> null } }
        for(edge in geometry.edges) {
            assertTrue(edge.points in painted)
            assertTrue(edge.points.all { it.x>=0 && it.y>=0 && it.x<=scene.width && it.y<=scene.height })
        }
        for(group in geometry.nodes.filter { it.isGroup }) {
            assertTrue(scene.commands.filterIsInstance<DrawRect>().any { it.rect==group.bounds })
            val title=assertNotNull(group.groupTitleBounds)
            assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text=="Group" && it.origin.y==title.y+title.height })
        }
    }

    @Test fun renderedTJunctionsDoNotIncreaseWhenSeparatingTerminals() {
        val cases=listOf(
            "flowchart LR\nN0[Node 0]\nN1[Node 1]\nN2[Node 2]\nN3[Node 3]\nN3 --> N1\nN0 --> N0\nN1 --> N3\nN3 --> N1\nN0 --> N2" to 3,
            "flowchart LR\nN0[Node 0]\nN1[Node 1]\nN2[Node 2]\nN2 --> N1\nN2 --> N1\nN0 --> N2\nN1 --> N0\nN1 --> N2" to 1,
        )
        for((source,maximum) in cases) {
            val parsed=assertIs<MermaidParseResult.Success>(MermaidParser.parse(source))
            val scene=SimpleMermaidLayout.layout(parsed.diagram,measurer,LayoutConfig(validateOrthogonalLayout=true))
            assertTrue(assertNotNull(scene.layoutValidation).result.breakdown.crossings<=maximum)
        }
    }

}
