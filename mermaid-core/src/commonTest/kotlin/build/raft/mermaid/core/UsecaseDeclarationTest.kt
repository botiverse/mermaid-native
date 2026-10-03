package build.raft.mermaid.core

import kotlin.test.*

class UsecaseDeclarationTest {
    @Test fun repeatedPlainGeneratedLabelsAreDeclarationsEvenWhenIdIsUnchanged() {
        val doc = UsecaseDocument()
        assertIs<MermaidParseResult.Failure>(doc.parse("usecase-beta\n\"Same\"\n\"Same\""))
        assertNull(doc.ast); assertNull(doc.diagram)
        assertIs<MermaidParseResult.Success>(doc.parse("usecase-beta\nSame\nSame"))
        assertEquals(1, doc.diagram!!.useCases.size)
    }

    @Test fun conflictsUseUtf16OffsetsAndCrLfLineNumbers() {
        val source = "usecase-beta\r\n%% 😀\r\n\"A-B\"\r\n\"A B\""
        val failure = assertIs<MermaidParseResult.Failure>(UsecaseDocument().parse(source))
        val first = source.indexOf("A-B"); val second = source.indexOf("A B")
        assertEquals("Generated ID 'A_B' collides with another declaration at line 4, column 2 [$second,${second + 3}) (label \"A B\"); previous declaration at line 3, column 2 [$first,${first + 3}) (label \"A-B\")", failure.diagnostics.single().message)
        assertEquals(SourceLocation(4, 2), failure.diagnostics.single().location)
    }

    @Test fun globalSymbolsAreCheckedInEitherDeclarationOrder() {
        for (body in listOf("actor Shared\njson Shared@{}", "json Shared@{}\nactor Shared", "actor link\nA link@--> B", "A link@--> B\nactor link", "systemBoundary Shared\nend\nactor Shared", "actor Shared\nsystemBoundary Shared\nend")) {
            val doc = UsecaseDocument()
            assertIs<MermaidParseResult.Failure>(doc.parse("usecase-beta\n$body"), body)
            assertNull(doc.ast); assertNull(doc.diagram)
        }
    }
}
