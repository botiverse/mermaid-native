package build.raft.mermaid.core

import kotlin.test.*

class UnicodeGraphemesTest {
    private fun point(code: Int): String = if (code <= 0xFFFF) code.toChar().toString() else {
        val value = code - 0x10000
        "${(0xD800 + (value shr 10)).toChar()}${(0xDC00 + (value and 1023)).toChar()}"
    }

    @Test fun officialUnicode17Conformance() {
        var count = 0
        for (chunk in unicodeGraphemeCases) for (line in chunk.lines()) {
            val expected = mutableListOf<String>(); var cluster = ""
            for (token in line.split(' ')) when (token) {
                "÷" -> if (cluster.isNotEmpty()) { expected += cluster; cluster = "" }
                "×", "" -> Unit
                else -> cluster += point(token.toInt(16))
            }
            assertEquals(expected, UnicodeGraphemes.split(expected.joinToString("")), line)
            count++
        }
        assertEquals(766, count)
    }

    @Test fun codePointFallbackPreservesPairsAndIsolatedSurrogates() {
        assertEquals(listOf("A", "😀", "\uD800", "Z", "\uDC00"), UnicodeGraphemes.codePoints("A😀\uD800Z\uDC00"))
        assertEquals(emptyList(), UnicodeGraphemes.split(""))
        assertEquals(listOf("\uD800", "Z", "\uDC00"), UnicodeGraphemes.split("\uD800Z\uDC00"))
    }
}
