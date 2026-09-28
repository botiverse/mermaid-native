package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ClosingBreakLayoutTest {
    @Test
    fun sequenceMessagesAndClassNotesMeasureClosingBreaksAsSeparateLines() {
        for (source in listOf(
            "sequenceDiagram\nA->>B: first</br>second</BR >third",
            "classDiagram\nclass A\nnote for A \"first</br>second</BR >third\"",
        )) {
            val d = assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram
            val scene = SimpleMermaidLayout.layout(d, TextMeasurer { text, style -> SceneSize(text.length * style.fontSize * .55, style.fontSize) }, LayoutConfig())
            val lines = scene.commands.filterIsInstance<DrawText>().filter { it.text in listOf("first", "second", "third") }
            assertEquals(listOf("first", "second", "third"), lines.map { it.text })
            assertTrue(lines.zipWithNext().all { (a,b) -> b.origin.y > a.origin.y })
            assertTrue(scene.commands.filterIsInstance<DrawText>().none { it.text.contains("</br", ignoreCase = true) })
        }
    }
}
