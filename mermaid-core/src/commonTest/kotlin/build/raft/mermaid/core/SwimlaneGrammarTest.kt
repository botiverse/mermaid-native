package build.raft.mermaid.core
import kotlin.test.*
class SwimlaneGrammarTest {
    @Test fun flowGrammarSupportsSemicolonHeaderAndImplicitDefaultLane() {
        val d=assertIs<SwimlaneDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("swimlane-beta LR;A-->B;")).diagram)
        assertEquals(FlowDirection.LR,d.direction)
        assertEquals(listOf("A","B"),d.lanes.single().nodes.map { it.id })
        assertEquals("A",d.flowchart!!.edges.single().sourceId)
    }
    @Test fun nestedGroupsStylesAndMetadataAreRetained() {
        val d=assertIs<SwimlaneDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("swimlane-beta LR\nsubgraph outer\nsubgraph inner\nA@{shape: hexagon}-->B\nend\nend\nclassDef accent fill:#abcdef\nclass A accent")).diagram)
        assertEquals(FlowNodeShape.HEXAGON,d.flowchart!!.nodes.first().shape)
        assertEquals("outer",d.flowchart!!.subgraphs.first().parentId)
        assertEquals(listOf("accent"),d.flowchart!!.nodes.first().classes)
    }
}
