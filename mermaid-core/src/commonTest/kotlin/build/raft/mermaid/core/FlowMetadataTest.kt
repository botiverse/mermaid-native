package build.raft.mermaid.core
import kotlin.test.*
class FlowMetadataTest {
    private fun parse(s:String)=assertIs<FlowchartDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("graph LR\n$s")).diagram)
    @Test fun shapeAndQuotedBraceMetadataReachNodesInChains() {
        val d=parse("A@{ shape: rounded, label: \"A } @ B\" } & B@{shape: circle} --> C")
        assertEquals("A } @ B",d.nodes[0].label)
        assertEquals(FlowNodeShape.ROUNDED,d.nodes[0].shape)
        assertEquals(FlowNodeShape.CIRCLE,d.nodes[1].shape)
        assertEquals(2,d.edges.size)
    }
    @Test fun scalarBlockAndQuotedMultilineValuesAreDecoded() {
        assertEquals("first\nsecond\n",flowMetadata("\n  label: |\n    first\n    second\n  other: value\n")["label"])
        assertEquals("first<br/>second",flowMetadata("label: \"first\n  second\"")["label"])
    }
    @Test fun edgeMetadataUpdatesDoNotCreateNodesAndFalseOverridesTrue() {
        val d=parse("A e1@--> B\ne1@{animate: true, curve: basis}\ne1@{animate: false}\nlinkStyle default interpolate linear")
        assertEquals(listOf("A","B"),d.nodes.map { it.id })
        assertEquals(false,d.edges.single().animate)
        assertEquals("basis",d.edges.single().interpolate)
        assertEquals("linear",d.defaultInterpolate)
    }
    @Test fun repeatedSiblingGroupsKeepAllMembersWithoutCreatingCycles() {
        val d=parse("subgraph outer\nsubgraph inner\nA-->B\nend\nsubgraph inner\nC-->D\nend\nend")
        assertEquals(setOf("A","B","C","D"),d.subgraphs.first { it.id=="inner" }.nodeIds.toSet())
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("graph LR\nsubgraph a\nsubgraph a\nend\nend"))
    }
    @Test fun groupMetadataDoesNotCreateNode() {
        val d=parse("subgraph one[Group]\nA-->B\nend\none@{view: collapsed}")
        assertTrue(d.subgraphs.single().collapsed)
        assertEquals(listOf("A","B"),d.nodes.map { it.id })
    }
}
