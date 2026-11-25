package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

/** Expected labels and ticks were printed by d3-time-format 4.1.0 and d3-time 3.1.0 with TZ=UTC. */
class GanttAxisTest {
    private fun scene(source: String) = SimpleMermaidLayout.layout(
        assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, FixedWidthTextMeasurer, LayoutConfig())
    private fun axis(scene: LayoutScene) = scene.commands.filterIsInstance<DrawText>().filter { it.anchor == TextAnchor.MIDDLE }
    private fun labels(source: String) = axis(scene(source)).map { it.text }
    private fun day(iso: String): Long = (parseDay(iso) - parseDay("1970-01-01")) * 86_400_000L
    private fun parseDay(iso: String): Long = assertIs<GanttDiagram>(assertIs<MermaidParseResult.Success>(
        MermaidParser.parse("gantt\nA:a,$iso,1d")).diagram).sections.single().tasks.single().startDay.toLong()
    private fun iso(ticks: List<Long>?) = ticks?.joinToString(" ") { GanttAxis.format("%Y-%m-%d", it) }

    @Test fun everyDirectiveFormatsAsD3() {
        val specifier = "%a|%A|%b|%B|%c|%d|%e|%f|%g|%G|%H|%I|%j|%L|%m|%M|%p|%q|%Q|%s|%S|%u|%U|%V|%w|%W|%x|%X|%y|%Y|%Z|%%|%-d|%_d|%0e|%-H|%_m|%-j|%k|%-"
        listOf(
            // 2026-11-23T14:05:09.007
            1795442709007L to "Mon|Monday|Nov|November|11/23/2026, 2:05:09 PM|23|23|007000|26|2026|14|02|327|007|11|05|PM|4|1795442709007|1795442709|09|1|47|48|1|47|11/23/2026|2:05:09 PM|26|2026|+0000|%|23|23|23|14|11|327|k|",
            // 2024-12-30
            1735516800000L to "Mon|Monday|Dec|December|12/30/2024, 12:00:00 AM|30|30|000000|25|2025|00|12|365|000|12|00|AM|4|1735516800000|1735516800|00|1|52|01|1|53|12/30/2024|12:00:00 AM|24|2024|+0000|%|30|30|30|0|12|365|k|",
            // 2023-01-01
            1672531200000L to "Sun|Sunday|Jan|January|1/1/2023, 12:00:00 AM|01| 1|000000|22|2022|00|12|001|000|01|00|AM|1|1672531200000|1672531200|00|7|01|52|0|00|1/1/2023|12:00:00 AM|23|2023|+0000|%|1| 1|01|0| 1|1|k|",
            // 2020-12-31T12:00
            1609416000000L to "Thu|Thursday|Dec|December|12/31/2020, 12:00:00 PM|31|31|000000|20|2020|12|12|366|000|12|00|PM|4|1609416000000|1609416000|00|4|52|53|4|52|12/31/2020|12:00:00 PM|20|2020|+0000|%|31|31|31|12|12|366|k|",
            // 1969-12-31T23:59:59.980
            -20L to "Wed|Wednesday|Dec|December|12/31/1969, 11:59:59 PM|31|31|980000|70|1970|23|11|365|980|12|59|PM|4|-20|-1|59|3|52|01|3|52|12/31/1969|11:59:59 PM|69|1969|+0000|%|31|31|31|23|12|365|k|",
            // 2021-01-03T00:30
            1609633800000L to "Sun|Sunday|Jan|January|1/3/2021, 12:30:00 AM|03| 3|000000|20|2020|00|12|003|000|01|30|AM|1|1609633800000|1609633800|00|7|01|53|0|00|1/3/2021|12:30:00 AM|21|2021|+0000|%|3| 3|03|0| 1|3|k|",
            // 0202-12-01
            -55763856000000L to "Wed|Wednesday|Dec|December|12/1/0202, 12:00:00 AM|01| 1|000000|02|0202|00|12|335|000|12|00|AM|4|-55763856000000|-55763856000|00|3|48|48|3|48|12/1/0202|12:00:00 AM|02|0202|+0000|%|1| 1|01|0|12|335|k|",
        ).forEach { (time, expected) -> assertEquals(expected, GanttAxis.format(specifier, time)) }
        assertEquals("Week 48 ", GanttAxis.format("Week %V %", day("2026-11-23")))
    }

    @Test fun automaticTicksMatchD3() {
        fun auto(start: String, end: String) = iso(GanttAxis.autoTicks(day(start), day(end)))
        assertEquals((0..8).map { day("2026-11-23") + it * 43_200_000L }, GanttAxis.autoTicks(day("2026-11-23"), day("2026-11-27")))
        assertEquals("2026-01-01 2026-01-03 2026-01-05 2026-01-07 2026-01-09 2026-01-11 2026-01-13 2026-01-15 2026-01-17 " +
            "2026-01-19 2026-01-21 2026-01-23 2026-01-25 2026-01-27 2026-01-29 2026-01-31", auto("2026-01-01", "2026-01-31"))
        assertEquals("2024-01-01 2024-04-01 2024-07-01 2024-10-01 2025-01-01 2025-04-01 2025-07-01 2025-10-01 2026-01-01", auto("2024-01-01", "2026-01-01"))
        assertEquals("2020-01-01 2021-01-01 2022-01-01 2023-01-01 2024-01-01 2025-01-01", auto("2020-01-01", "2025-01-01"))
        assertEquals("1990-01-01 2000-01-01 2010-01-01 2020-01-01 2030-01-01 2040-01-01 2050-01-01 2060-01-01 2070-01-01 " +
            "2080-01-01 2090-01-01", auto("1990-01-01", "2090-01-01"))
        assertEquals((0L..35L step 5L).toList(), GanttAxis.autoTicks(0, 35))
        assertEquals((0L..8L).map { it * 900_000L }, GanttAxis.autoTicks(0, 7_200_000))
    }

    @Test fun tickIntervalsMatchD3() {
        fun every(interval: String, weekday: String, start: String, end: String) = iso(GanttAxis.intervalTicks(interval, weekday, day(start), day(end)))
        assertEquals("2026-11-02 2026-11-09 2026-11-16 2026-11-23 2026-11-30", every("1week", "monday", "2026-11-01", "2026-11-30"))
        assertEquals("2026-11-01 2026-11-15 2026-11-29 2026-12-13 2026-12-27", every("2week", "sunday", "2026-11-01", "2026-12-31"))
        assertEquals("2026-11-05 2026-11-19 2026-12-03 2026-12-17 2026-12-31", every("2week", "thursday", "2026-11-01", "2026-12-31"))
        assertEquals("2026-02-01 2026-03-01 2026-04-01", every("1month", "sunday", "2026-01-15", "2026-04-01"))
        assertEquals("2026-01-01 2026-06-01 2026-11-01 2027-01-01", every("5month", "sunday", "2026-01-01", "2027-02-01"))
        assertEquals("2026-01-31 2026-02-01 2026-02-11 2026-02-21 2026-03-01", every("10day", "sunday", "2026-01-25", "2026-03-05"))
        val midnight = day("2026-01-01")
        assertEquals(listOf(6, 12, 18, 24).map { midnight + it * 3_600_000L },
            GanttAxis.intervalTicks("6hour", "sunday", midnight + 3_600_000L, midnight + 86_400_000L))
        assertEquals(listOf(15, 30, 45, 60).map { midnight + it * 60_000L },
            GanttAxis.intervalTicks("15minute", "sunday", midnight + 300_000L, midnight + 3_600_000L))
    }

    @Test fun malformedAndExcessiveIntervalsAreIgnored() {
        listOf("0day", "day", "1 day", "1year", "-1day", "99999999999day").forEach {
            assertNull(GanttAxis.intervalTicks(it, "sunday", 0, 86_400_000L), it)
        }
        assertEquals(10_000, GanttAxis.intervalTicks("1millisecond", "sunday", 1, 10_000)?.size)
        assertNull(GanttAxis.intervalTicks("1millisecond", "sunday", 0, 10_001))
        // Upstream would estimate few ticks, then search every second of the range.
        assertNull(GanttAxis.intervalTicks("3600second", "sunday", 0, 36_000_000_000L))
    }

    @Test fun axisFormatLabelsTheAxis() {
        val source = "gantt\ndateFormat DD/MM/YYYY\naxisFormat %d %b\nA:a,23/11/2026,2d\nB:b,after a,2d"
        // d3 also ticks at noon; those repeat the day's label and are left out.
        assertEquals(listOf("23 Nov", "24 Nov", "25 Nov", "26 Nov", "27 Nov"), labels(source))
        assertEquals(listOf("00:00", "00:15", "00:30", "00:45", "01:00", "01:15", "01:30", "01:45", "02:00"),
            labels("gantt\naxisFormat %H:%M\nA:a,2026-11-23,2h"))
        assertEquals(listOf("2026-11-23", "2026-11-24"), labels("gantt\nA:a,2026-11-23,1d"))
        assertEquals(listOf("23", "24"), labels("gantt\naxisFormat %d; comment\nA:a,2026-11-23,1d"))
    }

    @Test fun tickIntervalPlacesTicksAndHonorsWeekday() {
        val result = scene("gantt\naxisFormat %m/%d\ntickInterval 1week\nweekday monday\nA:a,2026-11-01,29d")
        assertEquals(listOf("11/02", "11/09", "11/16", "11/23", "11/30"), axis(result).map { it.text })
        val bar = result.commands.filterIsInstance<DrawRect>().single { it.rect.height == 24.0 }.rect
        axis(result).zip(listOf(1, 8, 15, 22, 29)).forEach { (tick, offset) ->
            assertEquals(bar.x + bar.width * offset / 29, tick.origin.x, .000001)
        }
        assertTrue(axis(scene("gantt\ntickInterval 1month\nA:a,2026-11-02,3d")).isEmpty())
    }

    @Test fun denseIntervalLabelsDoNotOverlap() {
        val ticks = axis(scene("gantt\naxisFormat %Y-%m-%d\ntickInterval 1day\nA:a,2026-01-01,200d"))
        assertTrue(ticks.size in 5..14)
        ticks.zipWithNext().forEach { (a, b) ->
            assertTrue(b.origin.x - a.origin.x >= FixedWidthTextMeasurer.measure(a.text, a.style).width)
        }
    }

    @Test fun lastLabelStaysInsideTheSectionBand() {
        listOf("%Y-%m-%d", "%d %b", "%A %d %B %Y").forEach { format ->
            val result = scene("gantt\naxisFormat $format\ntickInterval 1day\nsection Work\nA:a,2026-11-23,1d")
            val band = result.commands.filterIsInstance<DrawRect>().first { it.rect.height == 32.0 }.rect
            val bar = result.commands.filterIsInstance<DrawRect>().first { it.rect.height == 24.0 }.rect
            assertTrue(band.x + band.width - (bar.x + bar.width) >= 37.5)
            axis(result).forEach {
                val half = FixedWidthTextMeasurer.measure(it.text, it.style).width / 2
                assertTrue(it.origin.x + half <= band.x + band.width, format)
                assertTrue(it.origin.x - half >= band.x, format)
            }
        }
    }
}
