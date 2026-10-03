package build.raft.mermaid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UsecaseQuoteDiagnosticTest {
    @Test fun unterminatedLabelsPointAtTheirOpeningQuoteAcrossLineEndings() {
        for (newline in listOf("\n", "\r\n", "\r")) {
            val source = "usecase-beta${newline}%% 😀${newline}  actor \"Reader"
            val diagnostic = assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source)).diagnostics.single()
            val start = source.indexOf('"')
            assertEquals(SourceLocation(3, 9), diagnostic.location)
            assertEquals("Error lexing usecase diagram: Unclosed usecase label at line 3, column 9 [$start,${source.length})", diagnostic.message)
        }
    }

    @Test fun multilineMarkdownFailureRetainsTheOpeningLocation() {
        val source = "usecase-beta\nLogin(\"`First\nsecond"
        val diagnostic = assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source)).diagnostics.single()
        assertEquals(SourceLocation(2, 7), diagnostic.location)
        assertTrue(diagnostic.message.endsWith("[${source.indexOf('"')},${source.length})"))
        val restored = assertIs<MermaidParseResult.Success>(MermaidParser.parse(source + "`\")"))
        assertEquals("First\nsecond", assertIs<UsecaseDiagram>(restored.diagram).useCases.single().label)
    }

    @Test fun plainPhysicalNewlineReportsQuoteAndKeepsValidQuotedLabels() {
        val source = "usecase-beta\nactor \"First\nsecond\""
        val diagnostic = assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source)).diagnostics.single()
        assertEquals(SourceLocation(2, 7), diagnostic.location)
        assertTrue(diagnostic.message.contains("Physical newlines require a Markdown label"))
        assertTrue(diagnostic.message.endsWith("[${source.indexOf('"')},${source.indexOf('\n', source.indexOf('"'))})"))
        val restored = assertIs<MermaidParseResult.Success>(MermaidParser.parse("usecase-beta\nactor \"Reader\""))
        assertEquals("Reader", assertIs<UsecaseDiagram>(restored.diagram).actors.single().label)
    }
}
