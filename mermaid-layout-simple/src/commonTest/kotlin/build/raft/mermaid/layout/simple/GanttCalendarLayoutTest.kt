package build.raft.mermaid.layout.simple
import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*
class GanttCalendarLayoutTest {
    private fun scene(s:String)=SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(s)).diagram,FixedWidthTextMeasurer,LayoutConfig())
    @Test fun paintStopsBeforeExcludedTailWhileDependencyStartsAfterIt(){
        val s=scene("gantt\nexcludes weekends\nDesign:d,2026-09-24,2d\nBuild:b,after d,2d")
        val r=s.commands.filterIsInstance<DrawRect>();assertEquals(2,r.size);assertTrue(r[0].rect.x+r[0].rect.width<r[1].rect.x);assertEquals(r[0].rect.width,r[1].rect.width)
    }
    @Test fun longSpansUseBoundedTicksAndKeepFinalDateLabelInsideScene(){
        val s=scene("gantt\nLong:a,2024-01-01,100000d")
        val ticks=s.commands.filterIsInstance<DrawText>().filter { Regex("\\d{4}-\\d{2}-\\d{2}").matches(it.text) }
        assertTrue(ticks.size<=14);assertTrue(s.width<1200);assertTrue(s.commands.size<60)
        for(t in ticks){val half=FixedWidthTextMeasurer.measure(t.text,t.style).width/2;assertTrue(t.origin.x-half>=0);assertTrue(t.origin.x+half<=s.width)}
    }
}
