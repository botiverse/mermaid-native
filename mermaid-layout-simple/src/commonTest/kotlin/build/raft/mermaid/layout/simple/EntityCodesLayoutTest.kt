package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class EntityCodesLayoutTest {
    private val measured = mutableListOf<String>()
    private fun texts(source: String): List<String> {
        val diagram = assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram
        val measurer = TextMeasurer { text, style -> measured += text; SceneSize(text.length * style.fontSize * .55, style.fontSize) }
        return SimpleMermaidLayout.layout(diagram, measurer, LayoutConfig()).commands.filterIsInstance<DrawText>().map { it.text }
    }
    @Test fun drawsAndMeasuresTheDecodedCharacters() {
        val drawn = texts("flowchart TB\na[\"say #quot;hi#quot;\"] -->|#lt;br#gt;| b[\"`**x** #42;y#42;`\"]")
        assertTrue("say \"hi\"" in drawn, drawn.toString())
        assertTrue("<br>" in drawn, drawn.toString())
        assertTrue("x" in drawn && "*y*" in drawn, drawn.toString())
        assertTrue(measured.none { text -> text.any { it.isSurrogate() } })
    }
    @Test fun literalBreakTagsInSequenceTextAreNotLineBreaks() {
        val drawn = texts("sequenceDiagram\nparticipant A as multi<br>using #lt;br#gt;\nA->>B: hi<br/>there #lt;br/#gt;")
        assertTrue(listOf("multi", "using <br>", "hi", "there <br/>").all { it in drawn }, drawn.toString())
    }
    @Test fun accessibilityTextIsDecoded() {
        val diagram = assertIs<MermaidParseResult.Success>(MermaidParser.parse("flowchart TB\naccTitle: A #amp; B\na-->b")).diagram
        val scene = SimpleMermaidLayout.layout(diagram, { text, style -> SceneSize(text.length * style.fontSize, style.fontSize) }, LayoutConfig())
        assertEquals("A & B", scene.accessibilityTitle)
    }
}
