package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

/** Mermaid wraps flowchart labels at 200px, breaking where a browser breaks. */
class FlowLabelWrapTest {
    private val measure = TextMeasurer { text, style -> SceneSize(UnicodeGraphemes.split(text).size * (if (style.fontWeight >= 700) 12.0 else 10.0), 14.0) }
    private fun scene(source: String): LayoutScene =
        SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, measure, LayoutConfig())
    private fun texts(scene: LayoutScene) = scene.commands.filterIsInstance<DrawText>()

    @Test fun longNodeLabelsWrapAtSpacesInsideABoxOfTheLimit() {
        val scene = scene("flowchart TD\nA[Collect the responses from every page]")
        assertEquals(listOf("Collect the", "responses from every", "page"), texts(scene).map { it.text })
        val box = scene.commands.filterIsInstance<DrawRect>().single { it.cornerRadius == 5.0 }.rect
        assertEquals(232.0, box.width, 1e-6)
        assertTrue(texts(scene).all { it.origin.y > box.y && it.origin.y < box.y + box.height })
    }

    @Test fun aLabelThatFitsAndALongWordKeepTheirText() {
        assertEquals(listOf("Twenty letters  here"), texts(scene("flowchart TD\nA[Twenty letters  here]")).map { it.text })
        val word = "a".repeat(32)
        assertEquals(listOf("see", word), texts(scene("flowchart TD\nA[see $word]")).map { it.text })
    }

    @Test fun cjkBreaksBetweenCharactersAndKeepsPunctuationOffTheLineStart() {
        val label = "一二三四五六七八九十一二三四五六七八九十，后面"
        val lines = texts(scene("flowchart TD\nA[$label]")).map { it.text }
        assertEquals(label, lines.joinToString(""))
        assertEquals(listOf("一二三四五六七八九十一二三四五六七八九", "十，后面"), lines)
    }

    @Test fun explicitBreaksStay() {
        assertEquals(listOf("one", "two"), texts(scene("flowchart TD\nA[one<br/>two]")).map { it.text })
    }

    @Test fun markdownNodeAndEdgeLabelsWrapWithTheirEmphasis() {
        val scene = scene("flowchart LR\nA[\"`Collect **every** response from the pages`\"] -->|\"`1o **bold**`\"| B")
        val bold = texts(scene).filter { it.style.fontWeight == 700 }.map { it.text }
        assertEquals(setOf("every", "bold"), bold.toSet())
        assertTrue(texts(scene).none { '*' in it.text })
        val node = texts(scene).filter { it.text !in setOf("1o", "bold", "B") }
        assertEquals(3, node.map { it.origin.y }.distinct().size)
    }

    @Test fun manyLinesStayInsideRoundShapes() {
        val label = "Another line with many words ".repeat(8).trim()
        fun box(shape: String) = scene("flowchart TD\n$shape").commands.filterIsInstance<DrawRect>().single { it.rect.width >= 232.0 }
        assertEquals(20.0, box("A($label)").cornerRadius, 1e-6)
        val stadium = box("A([$label])")
        assertEquals(stadium.rect.height / 2, stadium.cornerRadius, 1e-6)
        val cx = stadium.rect.width / 2 - stadium.cornerRadius
        for (text in texts(scene("flowchart TD\nA([$label])"))) {
            val x = measure.measure(text.text, text.style).width / 2 - cx
            val y = text.origin.y - stadium.rect.y - stadium.cornerRadius
            assertTrue(x <= 0 || kotlin.math.hypot(x, y) <= stadium.cornerRadius, text.text)
        }
        val circle = scene("flowchart TD\nA(($label))")
        val ellipse = circle.commands.filterIsInstance<DrawEllipse>().first()
        for (text in texts(circle)) {
            val half = measure.measure(text.text, text.style).width / 2
            assertTrue(kotlin.math.hypot(half, text.origin.y - ellipse.center.y) <= ellipse.radiusX, text.text)
        }
    }
}
