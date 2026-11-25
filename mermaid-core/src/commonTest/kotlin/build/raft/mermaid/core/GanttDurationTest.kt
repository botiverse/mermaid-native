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

    @Test fun dayjsDateFormatsParseStrictly() {
        val day = 86_400_000L
        val dated = tasks("gantt\ndateFormat YYYY-MM-DD HH:mm:ss\nA : a1, 2025-07-04 20:30:30, 2025-07-08 10:30:30").single()
        assertEquals(20273L * day + (20 * 3600 + 30 * 60 + 30) * 1000L, dated.startEpochMillis)
        assertEquals(((3 * 24 + 14) * 3600) * 1000L, dated.durationMillis)
        assertEquals(1000L * 3600 * 12, tasks("gantt\ndateFormat HH:mm:ss\nA : 12:00:00, 10m").single().startEpochMillis % day)
        assertEquals(listOf(0L, 6L), tasks("gantt\ndateFormat SSS\nA : a1, 000, 6ms\nB : after a1, 6ms").map { it.startEpochMillis % day })
        assertEquals(0L, tasks("gantt\ndateFormat D\nA : a, 1, 1d").single().startEpochMillis % day)
        assertEquals(20199L * day, tasks("gantt\ndateFormat DD/MM/YYYY\nA : a, 21/04/2025, 1d").single().startEpochMillis)
        assertEquals(20199L * day + 13 * 3600_000L, tasks("gantt\ndateFormat YYYY-MM-DD h A\nA : a, 2025-04-21 1 PM, 1d").single().startEpochMillis)
        listOf("2025-4-21", "2025-04-31", "21/04/2025").forEach {
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("gantt\ndateFormat YYYY-MM-DD\nA : a, $it, 1d"), it)
        }
    }

    @Test fun vertMarkersRepeatedIdsAndZeroEnds() {
        val result = tasks("gantt\ndateFormat HH:mm\nV : vert, v1, 17:30, 2m\nV : vert, v1, 17:30, 2m\nT : t, after v1, 3m\nM : milestone, m, 17:40, 0\nE : vert, 17:50,")
        assertEquals(listOf(true, true, false, false, true), result.map { it.vert })
        assertEquals(result[1].startEpochMillis + 120_000L, result[2].startEpochMillis)
        assertEquals(listOf(0L, 0L), listOf(result[3].durationMillis, result[4].durationMillis))
        assertEquals("YYYY-MM-DD", assertIs<GanttDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("gantt\ndateFormat   :YYYY-MM-DD\nA : a, 2014-01-06, 1d")).diagram).dateFormat)
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
