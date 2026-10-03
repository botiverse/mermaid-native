package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.pow
import kotlin.test.*

class UsecaseMarkdownNodeTest {
    private val measurer = TextMeasurer { text, style ->
        SceneSize(UnicodeGraphemes.split(text).size * if (style.fontWeight >= 700) 12.0 else 8.0, 14.0)
    }
    private fun scene(source: String) = SimpleMermaidLayout.layout(
        assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, measurer, LayoutConfig()
    )

    @Test fun styledWrappedTextCornersStayInsideEllipse() {
        val output = scene("""usecase-beta
actor User
User --> Login("`**Review international customer authorization carefully** and *confirm all payment information*`")
""")
        val ellipse = output.commands.filterIsInstance<DrawEllipse>().maxBy { it.radiusX }
        val texts = output.commands.filterIsInstance<DrawText>().filter { it.text != "User" }
        assertTrue(texts.any { it.style.fontWeight == 700 })
        assertTrue(texts.any { it.style.italic })
        assertTrue(texts.all { '*' !in it.text })
        assertTrue(texts.map { it.origin.y }.distinct().size > 1)
        assertTrue(ellipse.radiusX < 250.0)
        for (text in texts) {
            val size = measurer.measure(text.text, text.style)
            val left = text.origin.x - if (text.anchor == TextAnchor.MIDDLE) size.width / 2 else 0.0
            for (x in listOf(left, left + size.width)) for (y in listOf(text.origin.y - size.height, text.origin.y + text.style.fontSize * .25)) {
                assertTrue(((x - ellipse.center.x) / ellipse.radiusX).pow(2) + ((y - ellipse.center.y) / ellipse.radiusY).pow(2) <= 1.0, text.text)
            }
        }
    }

    @Test fun actorAndRectangleKeepMetadataPrefixesAndTextColor() {
        val output = scene("""usecase-beta
actor User("`**Customer**
*reviewer*`") <<Human>>
User --> Login["`**Sign** *in*`"]
style Login color:#7a143b
""")
        val texts = output.commands.filterIsInstance<DrawText>()
        val prefix = texts.single { it.text == "«Human»" }
        val customer = texts.single { it.text == "Customer" }
        val reviewer = texts.single { it.text == "reviewer" }
        assertEquals(prefix.origin.y + 20, customer.origin.y)
        assertEquals(customer.origin.y + 20, reviewer.origin.y)
        val head = output.commands.filterIsInstance<DrawEllipse>().minBy { it.radiusX }
        assertTrue(prefix.origin.y - 14 > head.center.y + head.radiusY)
        for (text in texts.filter { it.text in listOf("Sign", "in") }) assertEquals(SceneColor("#7a143b"), text.style.color)
        assertTrue(texts.single { it.text == "in" }.style.italic)
    }

    @Test fun wrappingPreservesJoinedEmojiAndPlainLiteralLabels() {
        val family = "👨‍👩‍👧‍👦"
        val output = scene("usecase-beta\nactor User\nUser --> Login(\"`**${family.repeat(30)}**`\")")
        val texts = output.commands.filterIsInstance<DrawText>().filter { it.text != "User" }
        assertTrue(texts.size > 1)
        assertEquals(family.repeat(30), texts.joinToString("") { it.text })
        assertTrue(texts.all { UnicodeGraphemes.split(it.text).all { cluster -> cluster == family } })
        val plain = scene("usecase-beta\nactor User\nUser --> Login(\"**Sign in**\")\nstyle Login fill:#abc")
        assertTrue(plain.commands.filterIsInstance<DrawText>().any { it.text == "**Sign in**" && it.style.fontWeight == 600 })
    }
}
