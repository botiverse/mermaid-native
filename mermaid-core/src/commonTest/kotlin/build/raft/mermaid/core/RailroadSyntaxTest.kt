package build.raft.mermaid.core

import kotlin.test.*

class RailroadSyntaxTest {
    @Test fun selectionDoesNotParseBodyAndRequiresWholeHeader() {
        for (syntax in RailroadSyntax.entries) {
            assertEquals(syntax, RailroadSyntax.detect(" \n\t${syntax.keyword.uppercase()}\ninvalid body"))
            assertNull(RailroadSyntax.detect("${syntax.keyword}extra"))
            assertNull(RailroadSyntax.detect("${syntax.keyword} extra"))
            assertNull(RailroadSyntax.detect("prefix ${syntax.keyword}"))
            assertNull(RailroadSyntax.detect("%% comment\n${syntax.keyword}"))
        }
        assertNull(RailroadSyntax.detect(" \n\t"))
        assertNull(RailroadSyntax.detect("flowchart TD\nA-->B"))
    }

    @Test fun parserDispatchesEveryGrammarAndPreservesLeadingComments() {
        val cases = mapOf(
            RailroadSyntax.NATIVE to "rule = terminal(\"test\");",
            RailroadSyntax.ABNF to "rule = \"test\";",
            RailroadSyntax.EBNF to "rule = \"test\";",
            RailroadSyntax.PEG to "rule <- \"test\";",
        )
        for ((syntax, body) in cases) {
            val input = "%% leading comment\n\n${syntax.keyword}\n$body"
            val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse(input), syntax.name)
            assertIs<RailroadDiagram>(result.diagram)
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("${syntax.keyword}extra\n$body"))
        }
    }
}
