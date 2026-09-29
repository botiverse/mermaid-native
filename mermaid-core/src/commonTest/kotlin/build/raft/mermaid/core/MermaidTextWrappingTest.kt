package build.raft.mermaid.core

import kotlin.test.*

class MermaidTextWrappingTest {
    @Test fun measuredWrappingPreservesWordStyleAndInput() {
        val input = listOf(MermaidWord("hello", "strong"), MermaidWord(" "), MermaidWord("world", "em"))
        val original = input.toList()
        val output = MermaidTextWrapping.wrapLine(input, { it.sumOf { w -> w.content.length } <= 3 })
        assertEquals(listOf(listOf(MermaidWord("hel", "strong")), listOf(MermaidWord("lo", "strong")),
            listOf(MermaidWord("wor", "em")), listOf(MermaidWord("ld", "em"))), output)
        assertEquals(original, input)
    }

    @Test fun impossibleWidthStillAdvancesByWholeGraphemes() {
        assertEquals(listOf("🏳️‍🌈", "é", "😀"), MermaidTextWrapping.wrap("🏳️‍🌈é😀") { false })
    }

    @Test fun multilineAndEmptyCardsKeepTheirRows() {
        assertEquals(listOf("", "a", ""), MermaidTextWrapping.wrap("\na\n") { true })
        assertEquals(listOf(""), MermaidTextWrapping.wrap("") { true })
    }

    @Test fun lineApiRejectsEmbeddedNewlines() {
        assertEquals("splitLineToFitWidth does not support newlines in the line",
            assertFailsWith<IllegalArgumentException> { MermaidTextWrapping.wrapLine(listOf(MermaidWord("a\nb")), { true }) }.message)
    }
}
