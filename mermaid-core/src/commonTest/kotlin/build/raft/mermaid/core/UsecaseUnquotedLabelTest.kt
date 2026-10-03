package build.raft.mermaid.core

import kotlin.test.*

class UsecaseUnquotedLabelTest {
    @Test fun reportsTheActualOperatorTokenAfterACompleteIdentifier() {
        val source = "usecase-beta\r\n%% 😀\r\nLiteral(foo--bar)"
        val failure = assertIs<MermaidParseResult.Failure>(UsecaseDocument().parse(source))
        val offset = source.indexOf("--")
        assertTrue(failure.diagnostics.single().message.endsWith("at line 3, column 12 [$offset,${offset + 2})"))
        assertTrue(failure.diagnostics.single().message.contains("but found: '--'"))
        assertEquals(SourceLocation(3, 12), failure.diagnostics.single().location)
    }

    @Test fun punctuationRemainsLiteralAndQuotedDelimitersStayValid() {
        val doc = UsecaseDocument()
        assertIs<MermaidParseResult.Success>(doc.parse("usecase-beta\nLiteral(Review; 50% done — @user)\nQuoted(\"Literal [brackets] @{ ::: -- markers\")\nSingle('Quoted [literal]')"))
        assertEquals(listOf("Review; 50% done — @user", "Literal [brackets] @{ ::: -- markers", "Quoted [literal]"), doc.diagram!!.useCases.map { it.label })
    }

    @Test fun lexicalAndGrammarFailuresClearPriorPublishedState() {
        val doc = UsecaseDocument()
        for (text in listOf("Literal[wrong) delimiter]", "Literal(inner \"closed\" quote)", "Literal(Reviewer's note)", "Literal(nested {brace})", "Literal(nested o-- operator)")) {
            assertIs<MermaidParseResult.Success>(doc.parse("usecase-beta\nactor Previous"))
            assertIs<MermaidParseResult.Failure>(doc.parse("usecase-beta\n$text"), text)
            assertNull(doc.ast); assertNull(doc.diagram)
        }
    }
}
