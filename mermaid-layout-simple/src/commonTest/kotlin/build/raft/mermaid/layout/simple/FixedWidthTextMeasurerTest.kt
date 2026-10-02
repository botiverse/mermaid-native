package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.MermaidParseResult
import build.raft.mermaid.core.MermaidParser
import build.raft.mermaid.layout.*
import kotlin.test.*

class FixedWidthTextMeasurerTest {
    @Test fun wideBmpScriptsAndFullwidthFormsReserveOneEm() {
        for (text in listOf("中文", "かな", "カナ", "한글", "Ａ１", "，。")) {
            for (size in listOf(12.0, 14.0, 21.0)) {
                assertEquals(text.length * size, FixedWidthTextMeasurer.measure(text, TextStyle(fontSize = size)).width, text)
            }
        }
        assertEquals(32.0, FixedWidthTextMeasurer.measure("A中B文", TextStyle(fontSize = 10.0)).width)
    }

    @Test fun narrowAmbiguousSupplementaryAndMalformedTextKeepHistoricalMetrics() {
        for (text in listOf("", "abc XYZ 123", "éΩ·", "ｶﾅ", "e\u0301", "😀", "𠀀", "\uD800", "\uDC00")) {
            val size = 14.25
            assertEquals(SceneSize(text.length * size * 0.6, size * 1.2), FixedWidthTextMeasurer.measure(text, TextStyle(fontSize = size)), text)
        }
    }

    @Test fun recognizedEmojiSequencesReserveOneSupplementaryGlyph() {
        val sequences = listOf("👨‍👩‍👧‍👦", "👩‍💻", "👍🏽", "🇨🇳", "1️⃣", "1⃣", "🏳️‍🌈", "❤️‍🔥")
        for (size in listOf(10.0, 14.25, 24.0)) {
            val style = TextStyle(fontSize = size)
            for (text in sequences) {
                assertEquals(size * 1.2, FixedWidthTextMeasurer.measure(text, style).width, 0.00001, text)
                assertEquals(size * 1.2, FixedWidthTextMeasurer.measure(text, style).height)
            }
            assertEquals(size * (1.2 + 0.6 + 1.2), FixedWidthTextMeasurer.measure("👨‍👩‍👧‍👦 👍🏽", style).width, 0.00001)
            assertEquals(size * (1.0 + 1.2 + 0.6), FixedWidthTextMeasurer.measure("中👨‍👩‍👧‍👦A", style).width, 0.00001)
        }
    }

    @Test fun arbitraryJoinedPictographsAndTextPresentationStayConservative() {
        for (text in listOf("😀‍😀", "A‍B", "❤️‍A", "☀︎", "🇨", "\u200D", "\uFE0F")) {
            val size = 14.25
            val wide = text.count { UnicodeWideBmpData.contains(it.code) }
            val oldWidth = (text.length - wide) * size * 0.6 + wide * size
            assertEquals(oldWidth, FixedWidthTextMeasurer.measure(text, TextStyle(fontSize=size)).width, text)
        }
    }

    @Test fun familyEmojiNodeAndMarkdownRunsUseCorrectedWidths() {
        val emoji = "👨‍👩‍👧‍👦"
        for (direction in listOf("LR", "RL", "TD", "BT")) {
            val result = scene("graph $direction\nA[\"${emoji.repeat(4)}\"]")
            val rect = result.commands.filterIsInstance<DrawRect>().single().rect
            val text = result.commands.filterIsInstance<DrawText>().single()
            assertEquals(4 * 1.2 * text.style.fontSize + 32.0, rect.width, 0.00001)
            assertEquals(emoji.repeat(4),text.text)
        }
        val result = scene("graph TD\nA[\"`**Family** $emoji *ready*`\"]")
        val words = result.commands.filterIsInstance<DrawText>()
        assertEquals(listOf("Family",emoji,"ready"), words.map { it.text })
        val space = FixedWidthTextMeasurer.measure(" ",words[1].style).width
        assertEquals(words[1].origin.x + words[1].style.fontSize * 1.2 + space, words[2].origin.x,0.00001)
    }

    private fun scene(source: String) = SimpleMermaidLayout.layout(
        assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram,
        FixedWidthTextMeasurer, LayoutConfig(),
    )

    @Test fun flowNodeReservesFullEmForLongCjkLabel() {
        val value = "中文".repeat(16)
        for (direction in listOf("LR", "RL", "TD", "BT")) {
            val result = scene("flowchart $direction\nA[$value]")
            val rect = result.commands.filterIsInstance<DrawRect>().single().rect
            val text = result.commands.filterIsInstance<DrawText>().single()
            val expectedWidth = value.length * text.style.fontSize
            assertTrue(rect.width >= expectedWidth + 24.0)
            assertTrue(text.origin.x - expectedWidth / 2 >= rect.x)
            assertTrue(text.origin.x + expectedWidth / 2 <= rect.x + rect.width)
        }
    }

    @Test fun jsonValuesWrapInsideTableUsingFullEmCjkWidth() {
        val value = "中文".repeat(32)
        for (direction in listOf("LR", "RL", "TB", "BT")) {
            val result = scene("usecase-beta\ndirection $direction\njson Data@{\"message\":\"$value\"}")
            val rect = result.commands.filterIsInstance<DrawRect>().single().rect
            val lines = result.commands.filterIsInstance<DrawText>().filter { it.text.startsWith("中") || it.text.startsWith("文") }
            assertEquals(value, lines.joinToString("") { it.text })
            assertTrue(lines.size > 2)
            for (text in lines) {
                assertEquals(TextAnchor.START, text.anchor)
                assertTrue(text.origin.x >= rect.x)
                assertTrue(text.origin.x + text.text.length * text.style.fontSize <= rect.x + rect.width - 10.0)
                assertTrue(text.origin.y > rect.y && text.origin.y < rect.y + rect.height)
            }
        }
    }
}
