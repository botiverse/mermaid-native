package build.raft.mermaid.core

import kotlin.test.*

class MermaidTypeDetectionTest {
    @Test fun classificationDoesNotValidateTheBody() {
        val source = "  \n  graph TB\nA[unclosed"
        assertEquals(MermaidDiagramType.FLOWCHART, MermaidParser.detectType(source))
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source))
        assertEquals(MermaidDiagramType.GITGRAPH, MermaidParser.detectType("gitGraph TB:\nbfs1:queue"))
    }

    @Test fun commentsAndSeparatorsUseTheParserHeaderBoundary() {
        for (newline in listOf("\n", "\r\n", "\r")) {
            val source = listOf("", "%% misleading; pie", "  graph TD; A-->B").joinToString(newline)
            assertEquals(MermaidDiagramType.FLOWCHART, MermaidParser.detectType(source))
            assertIs<MermaidParseResult.Success>(MermaidParser.parse(source))
        }
        assertEquals(MermaidDiagramType.SEQUENCE, MermaidParser.detectType("sequenceDiagram\nA->>B: pie"))
        assertIs<MermaidParseResult.Success>(MermaidParser.parse("sequenceDiagram\nA->>B: pie"))
    }

    @Test fun unsupportedHeadersRemainDistinctFromMalformedKnownHeaders() {
        assertNull(MermaidParser.detectType("%% only a comment"))
        for (header in listOf("unknown", "---", "graphical", "swimlane-beta ZZ")) {
            assertNull(MermaidParser.detectType("%% first\n  $header"))
            val error = assertIs<MermaidParseResult.Failure>(MermaidParser.parse("%% first\n  $header")).diagnostics.single()
            val expected = if (header in listOf("graphical", "swimlane-beta ZZ")) MermaidDiagnosticCode.INVALID_HEADER else MermaidDiagnosticCode.UNSUPPORTED_DIAGRAM
            assertEquals(expected, error.code)
            assertEquals(SourceLocation(2, 3), error.location)
        }
        assertNull(MermaidParser.detectType("---\ntitle: foo\n---\ngraph TD\nA-->B"))
    }

    @Test fun familyDetectionDoesNotRelaxCaseOrDirectionValidation() {
        assertNull(MermaidParser.detectType("TREEVIEW-BETA\nroot"))
        assertNull(MermaidParser.detectType("GITGRAPH\ncommit"))
        assertEquals(MermaidDiagramType.FLOWCHART, MermaidParser.detectType("graph SIDEWAYS\nA-->B"))
        val error = assertIs<MermaidParseResult.Failure>(MermaidParser.parse("graph SIDEWAYS\nA-->B"))
        assertEquals(MermaidDiagnosticCode.INVALID_HEADER, error.diagnostics.first().code)
        for (header in listOf("railroad-beta", "railroad-ebnf-beta", "railroad-abnf-beta", "railroad-peg-beta")) {
            assertEquals(MermaidDiagramType.RAILROAD, MermaidParser.detectType(header))
        }
    }
}
