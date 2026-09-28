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
}
