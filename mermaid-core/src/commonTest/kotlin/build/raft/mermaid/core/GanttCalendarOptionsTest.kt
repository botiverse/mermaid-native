package build.raft.mermaid.core

import kotlin.test.*

class GanttCalendarOptionsTest {
    @Test fun calendarTokensNormalizeAndKeepFirstOccurrenceOrderInProductionResolver() {
        val source = """gantt
excludes WEEKENDS,2019-02-06
excludes WEEKENDS 2019-02-07
includes 2019-02-06
includes 2019-02-06,2019-02-08
Task : a, 2019-02-01, 1d
"""
        val diagram = assertIs<GanttDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
        assertEquals(listOf("weekends", "2019-02-06", "2019-02-07"), diagram.excludes)
        assertEquals(listOf("2019-02-06", "2019-02-08"), diagram.includes)
        val task = diagram.sections.single().tasks.single()
        assertEquals(3, task.durationDays) // Friday's computed end advances through the weekend to Monday.
        assertEquals(parseIsoDay("2019-02-04"), task.startDay + task.durationDays)
    }
}
