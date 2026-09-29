package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class GanttPrecisionLayoutTest {
    private val measurer = TextMeasurer { text, style -> SceneSize(text.length * style.fontSize * .55, style.fontSize) }
    private fun scene(source: String) = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, measurer, LayoutConfig())

    @Test fun millisecondTasksHaveProportionalVisibleBarsAndAccurateAxisLabels() {
        val result = scene("""gantt
dateFormat x
section Network
Request : a, 0, 20ms
Response : b, after a, 5ms
Other : c, 20, 10ms
Decimal : d, after c, 0.005s
""")
        val bars = result.commands.filterIsInstance<DrawRect>()
        assertEquals(4, bars.size)
        assertEquals(bars[0].rect.width / 4, bars[1].rect.width, 0.000001)
        assertEquals(bars[0].rect.x + bars[0].rect.width, bars[1].rect.x, 0.000001)
        assertEquals(bars[1].rect.x, bars[2].rect.x, 0.000001)
        assertTrue(bars.all { it.rect.width > 0 && it.rect.x >= 0 && it.rect.x + it.rect.width <= result.width })
        val ticks = result.commands.filterIsInstance<DrawText>().filter { it.anchor == TextAnchor.MIDDLE }
        assertEquals("00:00:00.000", ticks.first().text)
        assertEquals("00:00:00.035", ticks.last().text)
        assertTrue(ticks.zipWithNext().all { (a, b) -> b.origin.x - a.origin.x >= measurer.measure(a.text, a.style).width })
    }

    @Test fun dayOnlyDiagramsRetainCalendarAxisAndPalette() {
        val result = scene("gantt\nsection Work\nA : done, a, 2024-01-01, 2d\nB : b, after a, 1d")
        val bars = result.commands.filterIsInstance<DrawRect>()
        assertEquals(SceneColor("#16a34a"), bars[0].fill)
        assertEquals(SceneColor("#94a3b8"), bars[1].fill)
        assertEquals(bars[0].rect.width / 2, bars[1].rect.width, .000001)
        assertTrue(result.commands.filterIsInstance<DrawText>().any { it.text == "2024-01-01" })
        assertFalse(result.commands.filterIsInstance<DrawText>().any { it.text.contains("00:00:") })
        val subday = scene("gantt\nTask : a, 2024-01-01, 2h")
        assertTrue(subday.commands.filterIsInstance<DrawText>().any { it.text == "2024-01-01 00:00:00.000" })
    }
}
