package build.raft.mermaid.core
import kotlin.test.*
class FlowGrammarTest {
    private fun parse(source:String)=assertIs<FlowchartDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
    @Test fun endpointKindsLengthsIdsAndInvisibleEdgesAreTyped() {
        val d=parse("flowchart LR\nA id1@o-- Label --o B\nB x-.-x C\nC ~~~ D")
        assertEquals(FlowMarker.CIRCLE,d.edges[0].fromMarker)
        assertEquals(FlowMarker.CIRCLE,d.edges[0].toMarker)
        assertEquals("id1",d.edges[0].id)
        assertEquals("Label",d.edges[0].label)
        assertEquals(FlowEdgeStyle.INVISIBLE,d.edges[2].style)
    }
    @Test fun dottedLinksMayOmitTheLeadingDash() {
        val d=parse("flowchart TD\nR1 .-> R2\nR2 <.-> C\nC <-...-> E4\nP2.5 --> C")
        assertEquals(listOf("R1" to "R2","R2" to "C","C" to "E4","P2.5" to "C"),d.edges.map { it.sourceId to it.targetId })
        assertEquals(listOf(FlowEdgeStyle.DOTTED,FlowEdgeStyle.DOTTED,FlowEdgeStyle.DOTTED,FlowEdgeStyle.NORMAL),d.edges.map { it.style })
        assertEquals(listOf(1,1,3,1),d.edges.map { it.length })
        assertEquals(listOf(FlowMarker.NONE,FlowMarker.POINT,FlowMarker.POINT,FlowMarker.NONE),d.edges.map { it.fromMarker })
    }
    @Test fun linksContinueAcrossLineBreaks() {
        val d=parse("flowchart TB\na{{\"x\"}}\n--> b\nb -->\r\nc\nx-->y\no---z")
        assertEquals(listOf("a" to "b","b" to "c","x" to "y","o" to "z"),d.edges.map { it.sourceId to it.targetId })
        assertEquals(1,parse("flowchart TB\nsubgraph s\na\nend\nb").subgraphs.size)
    }
    @Test fun arrowsInsideQuotedLabelsAndSemicolonsRemainText() {
        val d=parse("graph TD;A[\"a-->b; c\"] -->|\"quoted; label\"| B[\"end\"]")
        assertEquals("a-->b; c",d.nodes[0].label)
        assertEquals("quoted; label",d.edges.single().label)
        assertEquals("end",d.nodes[1].label)
    }
    @Test fun chainedNodeListsProduceCartesianEdgesWithoutLosingLabels() {
        val d=parse("graph TD;A[Alpha] & B --> C & D;A --> C")
        assertEquals(5,d.edges.size)
        assertEquals(listOf("A" to "C","A" to "D","B" to "C","B" to "D"),d.edges.take(4).map { it.sourceId to it.targetId })
        assertEquals("Alpha",d.nodes.first().label)
    }
    @Test fun subgraphLabelsDirectionsAndParentsRemainTyped() {
        val d=parse("flowchart LR\nsubgraph outer[Outer group]\nsubgraph inner[Inner group]\ndirection BT\nA-->B\nend\nend")
        assertEquals("Inner group",d.subgraphs[0].label)
        assertEquals("outer",d.subgraphs[0].parentId)
        assertEquals(FlowDirection.BT,d.subgraphs[0].direction)
    }
    @Test fun originalEllipseBordersAndQuotedLabelKindsAreRetained() {
        val d=parse("graph TD;A(-Ellipse-) --> B[|borders:lt|Partial];C[\"Quoted\"]")
        assertEquals(FlowNodeShape.ELLIPSE,d.nodes[0].shape)
        assertEquals("Ellipse",d.nodes[0].label)
        assertEquals("Partial",d.nodes[1].label)
        assertEquals("lt",d.nodes[1].borders)
        assertEquals("string",d.nodes[2].labelType)
    }
    @Test fun dottedLabelLengthAndSymbolsAreNotConsumedAsLabelText() {
        val d=parse("graph LR;A -. contains == text ...-> B")
        assertEquals("contains == text",d.edges.single().label)
        assertEquals(3,d.edges.single().length)
        assertEquals(listOf("&node","node&","A"),parse("graph LR;&node --> node& --> A").nodes.map { it.id })
    }
    @Test fun trailingMarkerLettersRemainPartOfBareNodeIds() {
        val d=parse("graph LR;Todo-->Done;Inbox==>Y;Foo-.->Bar;Todo-->Inbox")
        assertEquals(listOf("Todo","Done","Inbox","Y","Foo","Bar"),d.nodes.map { it.id })
        assertEquals(listOf("Todo","Inbox","Foo","Todo"),d.edges.map { it.sourceId })
        assertTrue(d.edges.all { it.fromMarker==FlowMarker.NONE })
        assertEquals(FlowMarker.CIRCLE,parse("graph LR;A[Alpha]o--o B").edges.single().fromMarker)
    }
    @Test fun unclosedShapesGroupsAndLabelsAreRejected() {
        listOf("A[broken","subgraph Open\nA","A -- label B","A -->|oops B","graph.node","A[bad ( text]","A[bad \"text\" mix]").forEach { assertIs<MermaidParseResult.Failure>(MermaidParser.parse("graph TD\n$it")) }
    }
}
