package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class GanttVisualLayoutTest {
    private fun scene(source: String) = SimpleMermaidLayout.layout(
        assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, FixedWidthTextMeasurer, LayoutConfig())

    @Test fun sectionsAreIndependentHeadersAndLabelsNeverRepeatSectionPrefixes() {
        val result = scene("""gantt
 title Product launch
 section Discovery
 Research :done, a, 2024-01-01, 2d
 Scope :active, b, after a, 1d
 section Delivery
 Build :crit, c, after b, 3d
 Launch :milestone, m, after c, 0d
""")
        val text = result.commands.filterIsInstance<DrawText>()
        assertEquals(1, text.count { it.text == "Discovery" })
        assertEquals(1, text.count { it.text == "Delivery" })
        assertTrue(text.none { it.text.contains("Discovery:") || it.text.contains("Delivery:") })
        val bars = result.commands.filterIsInstance<DrawRect>().filter { it.rect.height == 24.0 }
        assertEquals(3, bars.size)
        assertTrue(bars.zipWithNext().all { (a, b) -> b.rect.y >= a.rect.y + a.rect.height + 18 })
        assertEquals(bars[0].rect.x + bars[0].rect.width, bars[1].rect.x, .00001)
        val milestone = result.commands.filterIsInstance<DrawPolygon>().single()
        assertTrue(milestone.points.all { it.x >= 0 && it.x <= result.width && it.y >= 0 && it.y <= result.height })
        assertEquals(text.first { it.text == "Research" }.origin.x, text.first { it.text == "Build" }.origin.x)
    }

    @Test fun longLabelsTitlesAndSectionsStayInsideTheirMeasuredRowsAndScene() {
        val title = "A very long project title ".repeat(16)
        val label = "Deliver a detailed specification with multilingual text 测试😀 ".repeat(4)
        val section = "A group with a long name ".repeat(5)
        val result = scene("gantt\ntitle $title\nsection $section\n$label: a, 2024-01-01, 1d\nNext:b,after a,1d")
        assertTrue(result.width < 1100)
        val texts = result.commands.filterIsInstance<DrawText>()
        texts.forEach { text ->
            val size = FixedWidthTextMeasurer.measure(text.text, text.style)
            val x = text.origin.x - if (text.anchor == TextAnchor.MIDDLE) size.width / 2 else 0.0
            assertTrue(x >= 0 && x + size.width <= result.width + .001, text.text)
            assertTrue(text.origin.y - size.height >= 0 && text.origin.y <= result.height, text.text)
            assertFalse(text.text.firstOrNull()?.isLowSurrogate() == true)
            assertFalse(text.text.lastOrNull()?.isHighSurrogate() == true)
        }
        val bars = result.commands.filterIsInstance<DrawRect>().filter { it.rect.height == 24.0 }
        assertTrue(bars[1].rect.y - bars[0].rect.y > 42)
        val labelLines = texts.filter { it.style.fontSize == 13.0 }.dropLast(1)
        assertEquals(label.trim(), labelLines.joinToString("") { it.text })
        assertTrue(labelLines.last().origin.y < texts.single { it.text == "Next" }.origin.y - 18)
    }

    @Test fun negativeEpochCrossingMidnightAndTinyIntervalsKeepExactGeometry() {
        val result = scene("gantt\ndateFormat x\nBefore:a,-20,20ms\nAfter:b,after a,5ms")
        val bars = result.commands.filterIsInstance<DrawRect>().filter { it.rect.height == 24.0 }
        assertEquals(4.0, bars[0].rect.width / bars[1].rect.width, .000001)
        assertEquals(bars[0].rect.x + bars[0].rect.width, bars[1].rect.x, .000001)
        val labels = result.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue(labels.containsAll(listOf("1969-12-31", "1970-01-01", "23:59:59.980", "00:00:00.005")))
    }
}
