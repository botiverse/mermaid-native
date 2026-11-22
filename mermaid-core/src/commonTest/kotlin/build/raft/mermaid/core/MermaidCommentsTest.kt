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

    @Test fun directivesOfAnyLengthDoNotReachTheDiagram() {
        val plain = MermaidParser.parse("flowchart TD\nA[\"100%\"] --> B")
        for (init in listOf("%%{init: {'theme': 'dark'}}%%", "%%{\n  init: {\n    'theme': 'base',\n    'themeVariables': { 'primaryColor': '#ff0000' }\n  }\n}%%", "%%{wrap}%%")) {
            assertEquals(MermaidDiagramType.FLOWCHART, MermaidParser.detectType("$init\nflowchart TD\nA --> B"))
            val parsed = MermaidParser.parse("$init\nflowchart TD\nA[\"100%\"] --> B")
            assertEquals(assertIs<MermaidParseResult.Success>(plain).diagram, assertIs<MermaidParseResult.Success>(parsed).diagram)
        }
        for (family in listOf("sequenceDiagram\nA->>B: hi", "pie\n\"a\": 1", "classDiagram\nclass A", "erDiagram\nA ||--o{ B : has", "gantt\nsection S\nTask :a1, 2024-01-01, 3d"))
            assertIs<MermaidParseResult.Success>(MermaidParser.parse("%%{init: {\n  'theme': 'dark'\n}}%%\n$family"), family)
    }

    @Test fun errorsAfterADirectiveKeepTheirLine() {
        val error = assertIs<MermaidParseResult.Failure>(MermaidParser.parse("%%{init: {\n  'theme': 'dark'\n}}%%\ngraph SIDEWAYS\nA-->B"))
        assertEquals(SourceLocation(4, 1), error.diagnostics.first().location)
    }
}
