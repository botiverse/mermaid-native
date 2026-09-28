package build.raft.mermaid.layout.simple
import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*
class SwimlaneGrammarLayoutTest {
    @Test fun enrichedSwimlaneKeepsActualShapeAndExplicitStyle() {
        val d=assertIs<MermaidParseResult.Success>(MermaidParser.parse("swimlane-beta LR\nsubgraph Lane\nA@{shape: hexagon,label: Work}-->B\nend\nstyle A fill:#abcdef")).diagram
        val scene=SimpleMermaidLayout.layout(d,FixedWidthTextMeasurer,LayoutConfig())
        assertTrue(scene.commands.filterIsInstance<DrawPolygon>().any { it.points.size==6 && it.fill.value=="#abcdef" })
        assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text=="Work" })
    }
    @Test fun edgesCanActuallyConnectToLaneGroups() {
        val d=assertIs<MermaidParseResult.Success>(MermaidParser.parse("swimlane-beta\nsubgraph L\nA\nend\nA-->L")).diagram
        val scene=SimpleMermaidLayout.layout(d,FixedWidthTextMeasurer,LayoutConfig())
        assertEquals(1, scene.commands.filterIsInstance<DrawLine>().size)
        assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text=="L" })
    }
    @Test fun plainLaneSelfLoopHasVisiblePathAndKeepsNodeColors() {
        val d=assertIs<MermaidParseResult.Success>(MermaidParser.parse("swimlane-beta\nsubgraph L\nA-->A\nend")).diagram
        val scene=SimpleMermaidLayout.layout(d,FixedWidthTextMeasurer,LayoutConfig())
        val path=scene.commands.filterIsInstance<DrawPolyline>().single()
        assertEquals(4,path.points.distinct().size)
        assertTrue(path.points.all { it.x in 0.0..scene.width && it.y in 0.0..scene.height })
        assertTrue(scene.commands.filterIsInstance<DrawRect>().any { it.stroke.value=="#2563eb" })
    }
}
