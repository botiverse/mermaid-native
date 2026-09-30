package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class UnicodeWrappingLayoutTest {
    private fun texts(source: String): List<String> = SimpleMermaidLayout.layout(
        assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram,
        FixedWidthTextMeasurer, LayoutConfig(),
    ).commands.filterIsInstance<DrawText>().map { it.text }

    @Test fun usecaseLabelDoesNotSplitSurrogatePair() {
        val prefix = "a".repeat(29)
        val output = texts("usecase-beta\nU(\"${prefix}😀end\")\njson Data@{\"key\":\"value\"}\nU --> Data")
        assertTrue("😀end" in output)
        assertTrue(prefix in output)
        assertTrue(output.none { it.lastOrNull()?.isHighSurrogate() == true || it.firstOrNull()?.isLowSurrogate() == true })
    }

    @Test fun jsonTableDoesNotSplitEmojiOrCombiningClusters() {
        for (cluster in listOf("🏳️‍🌈", "é", "👩🏾‍❤️‍👨🏻")) {
            val prefix = "a".repeat(33)
            val output = texts("usecase-beta\njson Data@{\"key\":\"$prefix${cluster}end\"}")
            assertTrue(output.any { cluster in it }, cluster)
            assertTrue(prefix in output)
        }
    }
}
