package build.raft.mermaid.core

import kotlin.test.*

class FlowCommentBoundaryTest {
    @Test fun trailingCommentsDoNotRunPastEndOfSource() {
        val plain = MermaidParser.parse("graph TD\nA-->B")
        assertIs<MermaidParseResult.Success>(plain)
        for (suffix in listOf(" %% trailing", " %%", "\n%% trailing", "\n%%", "\r%% trailing", "\r\n%% trailing")) {
            assertEquals(plain, MermaidParser.parse("graph TD\nA-->B$suffix"), suffix)
        }
    }

    @Test fun allNewlinesEndCommentsAndPreserveFollowingStatements() {
        val plain = MermaidParser.parse("graph TD\nA-->B\nB-->C")
        for (newline in listOf("\n", "\r\n", "\r")) {
            val source = listOf("graph TD", "A-->B %% inline; ignored", "%% line; ignored", "B-->C").joinToString(newline)
            assertEquals(plain, MermaidParser.parse(source), newline)
        }
    }

    @Test fun carriageReturnsPreserveOriginalDiagnosticLineAndColumn() {
        for (newline in listOf("\n", "\r\n", "\r")) {
            val source = listOf("graph TD", "%% ignored;", "  A[unclosed").joinToString(newline)
            val failure = assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source))
            assertEquals(SourceLocation(3, 3), failure.diagnostics.single().location)
        }
    }

    @Test fun percentSignsInsideLabelsAreNotComments() {
        val diagram = assertIs<MermaidParseResult.Success>(MermaidParser.parse("graph TD\nA[100%% complete]-->B[Done] %% trailing"))
        assertEquals("100%% complete", (diagram.diagram as FlowchartDiagram).nodes.first().label)
    }
}
