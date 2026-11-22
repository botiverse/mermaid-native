package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class FlowGroupNodesTest {
    private fun parse(source: String) = assertIs<FlowchartDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
    private fun merge(source: String) = mergeFlowGroupNodes(parse(source))
    private fun layout(source: String) = SimpleMermaidLayout.layout(parse(source), FixedWidthTextMeasurer, LayoutConfig())

    @Test fun styleStatementsReachTheSubgraph() {
        val source = "graph TB\nsubgraph bar[Bar]\nF\nend\nstyle bar fill:#999,stroke-width:10px,stroke:#0F0,color:blue"
        val merged = merge(source)
        assertEquals(listOf("F"), merged.nodes.map { it.id })
        assertTrue("fill:#999" in merged.subgraphs.single().styles)
        val scene = layout(source)
        val frame = scene.commands.filterIsInstance<DrawRect>().first()
        assertEquals("#999", frame.fill.value)
        assertEquals("#0f0", frame.stroke.value)
        assertEquals(10.0, frame.strokeWidth)
        assertEquals(1, scene.commands.filterIsInstance<DrawText>().count { it.text == "Bar" })
        assertEquals(0, scene.commands.filterIsInstance<DrawText>().count { it.text == "bar" })
    }

    @Test fun edgesToASubgraphDeclaredLaterUseItsFrame() {
        val source = "flowchart TD\nP1\nP1 -->P1.5\nsubgraph P1.5\nP2\nP2.5(( A ))\nP3\nend\nP2 --> P4\nP3 --> P6\nP1.5 --> P5"
        assertFalse(merge(source).nodes.any { it.id == "P1.5" })
        assertEquals(1, layout(source).commands.filterIsInstance<DrawText>().count { it.text == "P1.5" })
    }

    @Test fun subgraphsListedAsMembersAreNested() {
        val merged = merge("flowchart TB\nb-->B\na-->c\nsubgraph O\nA\nend\nsubgraph B\nc\nend\nsubgraph A\na\nb\nB\nend")
        assertEquals(mapOf("O" to null, "B" to "A", "A" to "O"), merged.subgraphs.associate { it.id to it.parentId })
        assertEquals(setOf("a", "b", "c"), merged.nodes.map { it.id }.toSet())
        assertTrue(layout("flowchart LR\na -->b\nsubgraph A\nB\nend\nsubgraph B\nb\nend").width > 0)
    }

    @Test fun mutualMembershipStaysAcyclic() {
        val merged = merge("flowchart LR\nsubgraph A\nB\nend\nsubgraph B\nA\nx\nend")
        assertTrue(merged.subgraphs.any { it.parentId == null })
        val scene = layout("flowchart LR\nsubgraph A\nB\nend\nsubgraph B\nA\nx\nend")
        assertTrue(scene.width > 0)
    }

    @Test fun edgesBetweenSubgraphsDeclaredLater() {
        val scene = layout("flowchart LR\nX --> Y\nsubgraph X\ndirection LR\nA\nC\nend\nsubgraph Y\nB\nD\nend")
        assertEquals(listOf("A", "B", "C", "D", "X", "Y"), scene.commands.filterIsInstance<DrawText>().map { it.text }.sorted())
    }
}
