package build.raft.mermaid.core

import kotlin.test.*

class GanttDurationTest {
    private fun tasks(source: String) = assertIs<GanttDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram).sections.flatMap { it.tasks }

    @Test fun subdayDurationAndDependenciesRetainMillisecondPrecision() {
        val result = tasks("""gantt
dateFormat x
section Network
Request : a, 0, 20ms
Response : b, after a, 5ms
Other : c, 20, 10ms
Decimal : d, after c, 0.005s
""")
        assertEquals(listOf(0L, 20L, 20L, 30L), result.map { it.startEpochMillis })
        assertEquals(listOf(20L, 5L, 10L, 5L), result.map { it.durationMillis })
        assertTrue(result.none { it.milestone })
        assertEquals(100L, tasks("gantt\ndateFormat X\nTask : t, 0.5, 0.1s").single().durationMillis)
        assertEquals(500L, tasks("gantt\ndateFormat X\nTask : t, 0.5, 0.1s").single().startEpochMillis)
    }

    @Test fun hoursMinutesAndCalendarDaysShareTheProductionResolver() {
        val result = tasks("""gantt
section Work
Start : a, 2024-01-01, 2h
Then : b, after a, 30m
Finish : c, after b, 0.5d
""")
        assertEquals(1704067200000L, result[0].startEpochMillis)
        assertEquals(7_200_000L, result[0].durationMillis)
        assertEquals(1_800_000L, result[1].durationMillis)
        assertEquals(86_400_000L, result[2].durationMillis) // dayjs calendar-day rounding
        assertEquals(result[0].startEpochMillis + 7_200_000L, result[1].startEpochMillis)
        assertEquals(1, result[2].durationDays)
    }

    @Test fun invalidAndExcessiveDurationsFailWithoutOverflow() {
        assertTrue(GanttDuration.parse("1f").amount.isNaN())
        assertEquals("ms", GanttDuration.parse("1f").unit)
        for (duration in listOf("1f", "-1s", "999999999999999999999999999999w")) {
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("gantt\nTask : a, 2024-01-01, $duration"))
        }
        val result = tasks("gantt\ndateFormat x\nTask : a, -1, 1ms").single()
        assertEquals(-1L, result.startEpochMillis)
        assertEquals(719527, result.startDay)
    }
}
