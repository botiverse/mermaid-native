package build.raft.mermaid.core
import kotlin.test.*
class GanttCalendarTest {
    private fun parse(s:String)=assertIs<GanttDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("gantt\n$s")).diagram)
    @Test fun excludedTailHasSeparatePaintAndDependencyEnds(){
        val t=parse("excludes weekends\nDesign:d,2026-09-24,2d\nBuild:b,after d,2d").sections.single().tasks
        assertEquals(4,t[0].durationDays);assertEquals(2,t[0].renderDurationDays);assertEquals(t[0].startDay+4,t[1].startDay)
    }
    @Test fun calendarTokensAlsoAcceptIsoDatesWithAlternateInputFormat(){
        val a=parse("dateFormat DD-MM-YYYY\nexcludes 2025-02-10\nA:a,09-02-2025,1d").sections.single().tasks.single()
        val b=parse("dateFormat DD-MM-YYYY\nexcludes 2025-02-10\nincludes 2025-02-10\nA:a,09-02-2025,1d").sections.single().tasks.single()
        assertEquals(2,a.durationDays);assertEquals(1,b.durationDays)
    }
    @Test fun milestoneRequiresAnActualStartDate(){assertIs<MermaidParseResult.Failure>(MermaidParser.parse("gantt\nM:milestone,m1,2024-01-01"));assertTrue(parse("M:milestone,m1,2024-01-01,0d").sections.single().tasks.single().milestone)}
}
