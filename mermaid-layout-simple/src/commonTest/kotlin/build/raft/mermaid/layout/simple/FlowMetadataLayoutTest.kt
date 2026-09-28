package build.raft.mermaid.layout.simple
import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*
class FlowMetadataLayoutTest {
    @Test fun collapsedGroupsHideMembersAndRedirectExternalEdgesInActualLayout() {
        val d=assertIs<MermaidParseResult.Success>(MermaidParser.parse("graph LR\nsubgraph one[Group]\nA[HiddenAlpha]-->B[HiddenBeta]\nend\nC[Visible]-->A\none@{view: collapsed}")).diagram
        val scene=SimpleMermaidLayout.layout(d,FixedWidthTextMeasurer,LayoutConfig())
        assertEquals(setOf("Visible","Group"),scene.commands.filterIsInstance<DrawText>().map { it.text }.toSet())
        assertEquals(2,scene.commands.filterIsInstance<DrawRect>().size)
        assertEquals(1,scene.commands.filterIsInstance<DrawLine>().size)
    }
}
