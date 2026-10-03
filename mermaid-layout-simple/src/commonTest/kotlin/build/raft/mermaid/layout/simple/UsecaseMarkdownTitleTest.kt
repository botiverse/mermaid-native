package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class UsecaseMarkdownTitleTest {
    private val measurer = TextMeasurer { text, style ->
        SceneSize(UnicodeGraphemes.split(text).size * if (style.fontWeight >= 700) 12.0 else 8.0, 14.0)
    }
    private fun scene(label: String, markdown: Boolean = true): LayoutScene {
        val quoted = if (markdown) "\"`$label`\"" else "\"$label\""
        val source = "usecase-beta\nsystemBoundary B[$quoted]\nactor User\nLogin(Sign in)\nend\nUser --> Login"
        return SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, measurer, LayoutConfig())
    }
    private fun headers(scene: LayoutScene) = scene.commands.filterIsInstance<DrawText>().filter { it.text !in listOf("User", "Sign in") }

    @Test fun drawsEmphasisWithoutDelimitersAndLeavesPlainTitlesLiteral() {
        val rich = headers(scene("**Payment** and *service*"))
        assertEquals(listOf("Payment", "and", "service"), rich.map { it.text })
        assertEquals(700, rich.first().style.fontWeight)
        assertTrue(rich.last().style.italic)
        assertTrue(rich.all { it.anchor == TextAnchor.START })
        val plain = headers(scene("**Payment** and *service*", false))
        assertEquals("**Payment** and *service*", plain.joinToString("") { it.text })
        assertTrue(plain.all { it.style.fontWeight == 600 && !it.style.italic })
    }

    @Test fun wrapsStyledWordsInsideTheBoundaryAndReservesHeaderHeight() {
        val output = scene("**Payment processing and authorization service for international customers** and *careful review*")
        val box = output.commands.filterIsInstance<DrawRect>().first().rect
        val titles = headers(output)
        assertTrue(titles.map { it.origin.y }.distinct().size > 1)
        for (title in titles) {
            assertTrue(title.origin.x >= box.x + 16.0)
            assertTrue(title.origin.x + measurer.measure(title.text, title.style).width <= box.x + 256.0)
            assertTrue(title.origin.y - 14.0 > box.y)
        }
        val actorTop = output.commands.filterIsInstance<DrawEllipse>().minOf { it.center.y - it.radiusY }
        assertTrue(titles.maxOf { it.origin.y } + 14.0 < actorTop)
        assertEquals("Paymentprocessingandauthorizationserviceforinternationalcustomersandcarefulreview", titles.joinToString("") { it.text }.replace(" ", ""))
    }

    @Test fun preservesGraphemesPunctuationAndExplicitLinesWhenWrapping() {
        val family = "👨‍👩‍👧‍👦"
        val titles = headers(scene("**${family.repeat(30)}**!\n*Review* \\*literal\\*"))
        val bold = titles.filter { it.style.fontWeight == 700 }
        assertTrue(bold.size > 1)
        assertEquals(family.repeat(30), bold.joinToString("") { it.text })
        assertTrue(bold.all { UnicodeGraphemes.split(it.text).all { cluster -> cluster == family } })
        assertEquals(listOf("Review", "*literal*"), titles.takeLast(2).map { it.text })
        assertTrue(titles[titles.lastIndex - 1].style.italic)
    }
}
