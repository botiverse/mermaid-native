package build.raft.mermaid.layout.simple
import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*
class GanttGrammarLayoutTest {
    @Test fun actualBarsMeetAtExclusiveDependencyBoundaryAndMilestoneIsVisible() {
        val d=assertIs<MermaidParseResult.Success>(MermaidParser.parse("gantt\nsection Work\nBuild:a,2024-01-01,2024-01-03\nShip:b,after a,1d\nRelease:milestone,release,after b,0d")).diagram
        val scene=SimpleMermaidLayout.layout(d,FixedWidthTextMeasurer,LayoutConfig())
        val bars=scene.commands.filterIsInstance<DrawRect>()
        assertEquals(bars[0].rect.x+bars[0].rect.width,bars[1].rect.x)
        assertEquals(1,scene.commands.filterIsInstance<DrawPolygon>().count { it.points.size==4 })
    }
}
