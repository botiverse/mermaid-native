package build.raft.mermaid.core

import kotlin.test.*

class MermaidCommentsTest {
    @Test fun cleanupPreservesDirectivesAndInteriorBlankLines() {
        assertEquals("%%{init: {}}%%\ngraph TD\n\n A-->B\n", MermaidComments.cleanup("\n%% first\n%%{init: {}}%%\ngraph TD\n\n A-->B\n %% last"))
        assertEquals("graph TD\n", MermaidComments.cleanup("\uFEFF\u00A0graph TD\n"))
    }

    @Test fun commentSemicolonsDoNotBecomeHeadersOrStatements() {
        val flow = MermaidParser.parse("%% ignore; bogusHeader\ngraph TD\nA-->B")
        assertIs<MermaidParseResult.Success>(flow)
        val plain = MermaidParser.parse("zenuml\nA\nA->B.foo()")
        val commented = MermaidParser.parse("%% ignore; bogusHeader\nzenuml\nA\n%% ignore; bogusStatement\nA->B.foo()")
        assertIs<MermaidParseResult.Success>(commented)
        assertEquals(plain, commented)
    }

    @Test fun removedCommentLinesAndTrimmedIndentKeepOriginalDiagnosticLocations() {
        for (newline in listOf("\n", "\r\n", "\r")) {
            val source = listOf("", "%% comment; not code", "", "  unsupportedHeader").joinToString(newline)
            val failure = assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source))
            assertEquals(SourceLocation(4, 3), failure.diagnostics.single().location)
        }
        val failure = assertIs<MermaidParseResult.Failure>(MermaidParser.parse("%% first\nzenuml\nA\n%% ignore; not code\n  A->B.foo();  unsupported syntax!"))
        assertEquals(SourceLocation(5, 16), failure.diagnostics.single().location)
    }

    @Test fun commentOnlySourceIsEmptyAndInlineTextIsPreserved() {
        assertEquals("", MermaidComments.cleanup("\n%% comment\n  %% another; tail"))
        assertEquals("graph TD; A[100%% complete]", MermaidComments.cleanup("graph TD; A[100%% complete]"))
        val failure = assertIs<MermaidParseResult.Failure>(MermaidParser.parse("%% comment; not a diagram"))
        assertEquals(MermaidDiagnosticCode.EMPTY_SOURCE, failure.diagnostics.single().code)
    }
}
