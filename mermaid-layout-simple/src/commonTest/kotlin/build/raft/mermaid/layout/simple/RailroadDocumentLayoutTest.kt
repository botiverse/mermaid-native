package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class RailroadDocumentLayoutTest {
    @Test fun documentStateFeedsActualRailroadDrawingWithoutParserInput() {
        val doc = RailroadDocument()
        doc.title = "Grammar<script>discarded</script>"
        doc.addRule(RailroadRule("entry", RailroadTerminal("token<style>discarded</style>")))
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse("railroad-beta\ntitle Grammar\nentry = terminal('token');")).diagram
        val expected = SimpleMermaidLayout.layout(parsed, FixedWidthTextMeasurer, LayoutConfig())
        val actual = SimpleMermaidLayout.layout(doc.diagram(), FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(expected, actual)
        assertFalse(actual.commands.filterIsInstance<DrawText>().any { "discarded" in it.text })
        doc.addRule(RailroadRule("entry", RailroadTerminal("second")))
        val next = SimpleMermaidLayout.layout(doc.diagram(), FixedWidthTextMeasurer, LayoutConfig())
        val labels = next.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue("token" in labels && "second" in labels)
        assertTrue(next.height > actual.height)
    }
}
