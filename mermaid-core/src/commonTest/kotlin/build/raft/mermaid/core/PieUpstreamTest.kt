package build.raft.mermaid.core

import kotlin.test.*

class PieUpstreamTest {
    private fun parse(source: String) = assertIs<PieDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)

    @Test fun multilineMetadataAndCommentsArePreserved() {
        val diagram = parse("""
            pie showData title A  neat chart %% comment
            accTitle: Accessible  title
            accDescr {
              A neat description

              on multiple lines
            }
            "dogs": 60
            "rats": 40
        """.trimIndent())
        assertEquals("A neat chart", diagram.title)
        assertEquals("Accessible title", diagram.accessibilityTitle)
        assertEquals("A neat description\non multiple lines", diagram.accessibilityDescription)
        assertTrue(diagram.showData)
        assertEquals(listOf(PieSection("dogs", 60.0), PieSection("rats", 40.0)), diagram.sections)
    }

    @Test fun delimitersInsideLabelsAndEscapesDoNotSplitStatements() {
        val diagram = parse("pie\n\"Semi;colon %% literal\": 10\n'He said \\\"yes\\\"':20\n\"line\\nfeed\":30")
        assertEquals(listOf("Semi;colon %% literal", "He said \"yes\"", "line\nfeed"), diagram.sections.map { it.label })
    }

    @Test fun zeroAndDuplicateLabelsRetainFirstValue() {
        val diagram = parse("pie\n\"__proto__\":0\n\"constructor\":25\n\"constructor\":75")
        assertEquals(listOf(PieSection("__proto__", 0.0), PieSection("constructor", 25.0)), diagram.sections)
    }

    @Test fun malformedValuesAndStatementsFail() {
        for (source in listOf("pie title-bad\n\"a\":1", "pie\n\"a\":01", "pie\n\"a\":1;\"b\":2", "pie\n\"a\":1e5", "pie\n\"a\":NaN", "pie accDescr {missing end", "pie\n\"mismatched':1", "pie\n\"a\":${"9".repeat(400)}")) {
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source)
        }
        val failure = assertIs<MermaidParseResult.Failure>(MermaidParser.parse("pie\n\"dogs\":-60.67"))
        assertEquals(MermaidDiagnosticCode.INVALID_VALUE, failure.diagnostics.single().code)
        assertEquals("\"dogs\" has invalid value: -60.67. Negative values are not allowed in pie charts. All slice values must be >= 0.", failure.diagnostics.single().message)
    }
}
