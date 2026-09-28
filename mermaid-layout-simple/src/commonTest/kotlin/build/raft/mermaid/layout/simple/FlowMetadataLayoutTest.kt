package build.raft.mermaid.layout.simple
import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*
class FlowMetadataLayoutTest {
    @Test fun manualCyclicAndMissingGroupParentsRemainRenderable() {
        val d=FlowchartDiagram(FlowDirection.LR,listOf(FlowNode("node","Visible")),emptyList(),listOf(
            FlowSubgraph("first","First",listOf("node"),parentId="second"),
            FlowSubgraph("second","Second",emptyList(),parentId="first"),
            FlowSubgraph("third","Third",emptyList(),parentId="missing")))
        val scene=SimpleMermaidLayout.layout(d,FixedWidthTextMeasurer,LayoutConfig())
        assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text=="Visible" })
        assertTrue(scene.width.isFinite() && scene.height.isFinite())
    }
    @Test fun collapsedGroupsHideMembersAndRedirectExternalEdgesInActualLayout() {
        val d=assertIs<MermaidParseResult.Success>(MermaidParser.parse("graph LR\nsubgraph one[Group]\nA[HiddenAlpha]-->B[HiddenBeta]\nend\nC[Visible]-->A\none@{view: collapsed}")).diagram
        val scene=SimpleMermaidLayout.layout(d,FixedWidthTextMeasurer,LayoutConfig())
        assertEquals(setOf("Visible","Group"),scene.commands.filterIsInstance<DrawText>().map { it.text }.toSet())
        assertEquals(2,scene.commands.filterIsInstance<DrawRect>().size)
        assertEquals(1,scene.commands.filterIsInstance<DrawLine>().size)
    }
    @Test fun collapsedPipelineFollowsEdgesInEveryDirection() {
        for (direction in listOf("LR", "RL", "TB", "BT")) {
            val d = assertIs<MermaidParseResult.Success>(MermaidParser.parse("""
                flowchart $direction
                subgraph order[Order service]
                A[Validate] --> B[Save]
                end
                C[Customer] --> A
                B --> D[Receipt]
                order@{view: collapsed}
            """.trimIndent())).diagram
            val scene = SimpleMermaidLayout.layout(d, FixedWidthTextMeasurer, LayoutConfig())
            val labels = scene.commands.filterIsInstance<DrawText>().associateBy { it.text }
            assertEquals(setOf("Customer", "Order service", "Receipt"), labels.keys)
            val positions = listOf("Customer", "Order service", "Receipt").map {
                val label = labels.getValue(it)
                if (direction == "LR" || direction == "RL") label.origin.x else label.origin.y
            }
            val forward = direction == "LR" || direction == "TB"
            assertTrue(positions.zipWithNext().all { (a, b) -> if (forward) a < b else a > b }, "$direction: $positions")
        }
    }
    @Test fun collapsedCycleKeepsAllVisibleNodesAndEdges() {
        val d = assertIs<MermaidParseResult.Success>(MermaidParser.parse("graph LR\nsubgraph group[Group]\nA-->B\nend\nC-->A\nB-->C\ngroup@{view: collapsed}")).diagram
        val scene = SimpleMermaidLayout.layout(d, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(setOf("C", "Group"), scene.commands.filterIsInstance<DrawText>().map { it.text }.toSet())
        assertEquals(2, scene.commands.filterIsInstance<DrawLine>().size)
        assertTrue(scene.width.isFinite() && scene.height.isFinite())
    }
}
