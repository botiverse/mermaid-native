package build.raft.mermaid.core

import kotlin.test.*

class GanttTaskModelTest {
    private fun tasks(source: String): List<GanttTask> = assertIs<GanttDiagram>(
        assertIs<MermaidParseResult.Success>(MermaidParser.parse("gantt\n$source")).diagram
    ).sections.flatMap { it.tasks }

    @Test fun missingStartDependenciesUseTodayAndKnownDependenciesTakePrecedence() {
        val before = ganttFloorDay(ganttCurrentEpochMillis())
        val result = tasks("A:a,after unknown,1d\nB:b,2024-01-01,2d\nC:c,after absent b,1d")
        val after = ganttFloorDay(ganttCurrentEpochMillis())
        assertTrue(result[0].startDay in before..after)
        assertEquals(result[1].startEpochMillis + result[1].durationMillis, result[2].startEpochMillis)
    }

    @Test fun secondsFormatPreservesActualSubdayDuration() {
        val task = tasks("dateFormat ss\nRTT:rtt,00,20").single()
        assertEquals(20_000L, task.durationMillis)
        assertEquals(0L, task.startEpochMillis % GANTT_DAY_MILLIS)
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("gantt\ndateFormat ss\nBad:a,70,80"))
    }

    @Test fun boundedYearFallbackAndCompactDateFormatResolveRealDates() {
        val short = tasks("A:a,202-12-01,7d").single()
        assertEquals(parseIsoDay("0202-12-01"), short.startDay)
        val compact = tasks("dateFormat YYYYMMDD\nA:a,20241201,1d").single()
        assertEquals(parseIsoDay("2024-12-01"), compact.startDay)
        val invalid = assertIs<MermaidParseResult.Failure>(MermaidParser.parse("gantt\ndateFormat YYYYMMDD\nA:a,202304,1d"))
        assertEquals("Invalid date:202304", invalid.diagnostics.single().message)
    }

    @Test fun calendarModelRetainsRenderEndAndExplicitEndProvenance() {
        val result = tasks("excludes weekends\nA:a,2019-02-01,1d\nB:b,2019-02-01,2019-02-02")
        assertEquals(result[0].startEpochMillis + GANTT_DAY_MILLIS, result[0].renderEndEpochMillis)
        assertEquals(3 * GANTT_DAY_MILLIS, result[0].durationMillis)
        assertFalse(result[0].manualEndTime)
        assertNull(result[1].renderEndEpochMillis)
        assertTrue(result[1].manualEndTime)
        assertEquals(GANTT_DAY_MILLIS, result[1].durationMillis)
    }
}
