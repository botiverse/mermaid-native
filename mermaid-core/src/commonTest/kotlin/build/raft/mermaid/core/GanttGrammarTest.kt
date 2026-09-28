package build.raft.mermaid.core
import kotlin.test.*
class GanttGrammarTest {
    private fun parse(s:String)=assertIs<GanttDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("gantt\n$s")).diagram)
    @Test fun fixedDatesHaveExclusiveEndsAndDependenciesMeetAtBoundaries() {
        val d=parse("dateFormat YYYY-MM-DD\nsection Work\nA:a,2024-01-27,2024-01-28\nB:b,after a,2024-01-30\nC:c,2024-01-20,until a\nD:d,after c,until b")
        val t=d.sections.single().tasks
        assertEquals(listOf(1,2,7,1),t.map { it.durationDays })
        assertEquals(t[0].startDay+t[0].durationDays,t[1].startDay)
        assertEquals(t[2].startDay+t[2].durationDays,t[3].startDay)
    }
    @Test fun directivesAndLiteralLabelsSurviveWithoutTasks() {
        val d=parse("title ;Plan #Q4\nexcludes weekends\nexcludes 10-02-2025,11-02-2025\nweekday monday\nsection ;Work #1")
        assertEquals(";Plan #Q4",d.title)
        assertEquals(listOf("weekends","10-02-2025","11-02-2025"),d.excludes)
        assertEquals("monday",d.weekday)
        assertEquals(";Work #1",d.sections.single().name)
    }
    @Test fun combinedTagsAndMilestoneAreIndependent() {
        val t=parse("section Work\nRelease:crit,milestone,done,2024-01-01,0d").sections.single().tasks.single()
        assertTrue(t.milestone)
        assertEquals(setOf(GanttTaskStatus.CRITICAL,GanttTaskStatus.DONE),t.statuses)
        assertEquals(GanttTaskStatus.CRITICAL,t.status)
    }
    @Test fun excludedWeekendsExtendDurationAndIncludesOverrideThem() {
        val a=parse("excludes weekends\nTask:a,2024-01-05,2d").sections.single().tasks.single()
        val b=parse("excludes weekends\nincludes 2024-01-06\nTask:a,2024-01-05,2d").sections.single().tasks.single()
        assertEquals(4,a.durationDays)
        assertEquals(3,b.durationDays)
    }
    @Test fun excludedEndBoundariesAndUntilUseTheOriginalCalendarRule() {
        val tasks=parse("excludes weekends\nDesign:design,2026-09-24,2d\nBuild:build,after design,2d\nLaunch:milestone,launch,after build,0d\nReview:review,2026-09-25,until launch").sections.single().tasks
        assertEquals(listOf(4,2,0,7),tasks.map { it.durationDays })
        assertEquals(tasks[0].startDay+4,tasks[1].startDay)
        val fixed=parse("excludes weekends\nFixed:a,2026-09-24,2026-09-26").sections.single().tasks.single()
        assertEquals(2,fixed.durationDays)
    }
    @Test fun dependencyCyclesAndUnknownReferencesFail() {
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("gantt\nA:a,after b,1d\nB:b,after a,1d"))
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("gantt\nA:a,after unknown,1d"))
    }
}
