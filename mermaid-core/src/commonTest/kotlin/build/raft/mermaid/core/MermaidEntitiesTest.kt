package build.raft.mermaid.core

import kotlin.test.*

class MermaidEntitiesTest {
    private fun roundTrip(text: String) = MermaidEntities.decode(MermaidEntities.encode(text))

    @Test fun namedAndNumericCodesBecomeTheirCharacter() {
        assertEquals("say \"hi\" <b> & # ; ~ A", roundTrip("say #quot;hi#quot; #lt;b#gt; #amp; #35; #59; #126; #65;"))
        assertEquals("♥ é € \uD83D\uDE00", roundTrip("#9829; #eacute; #euro; #128512;"))
    }
    @Test fun punctuationStaysInertUntilDecoded() {
        val encoded = MermaidEntities.encode("a #lt;br#gt; #quot;b#quot; #35;")
        assertTrue(encoded.none { it in "<>\"#;" })
        assertFalse(MermaidText.hasBreaks(encoded))
        assertEquals("a <br> \"b\" #", MermaidText.decodeEntities(encoded))
    }
    @Test fun otherHashesAreUntouched() {
        for (text in listOf("issue #1; next", "#notaname; x", "#0; #127; #55296; #9999999;", "# comment;", "plain"))
            assertEquals(text, MermaidEntities.encode(text))
    }
    @Test fun colorsEndingAStatementAreUntouched() {
        for (text in listOf("style A fill:#333;", "  classDef x stroke:#444, color:#555;", "linkStyle 0 stroke:#666;", "box #777;", "A-->B; style A fill:#333;"))
            assertEquals(text, MermaidEntities.encode(text))
        assertEquals("A[\"style guide\"]", roundTrip("A[#quot;style guide#quot;]"))
    }
    @Test fun flowLabelsKeepCodesThroughParsing() {
        val diagram = assertIs<FlowchartDiagram>(assertIs<MermaidParseResult.Success>(
            MermaidParser.parse("flowchart TB\na[\"x #quot;y#quot;\"] --> b{#123;z#125;}\nstyle a fill:#333;")).diagram)
        assertEquals(listOf("x \"y\"", "{z}"), diagram.nodes.map { MermaidText.decodeEntities(it.label) })
    }
    @Test fun sequenceTextAfterACodeIsNotAComment() {
        val diagram = assertIs<SequenceDiagram>(assertIs<MermaidParseResult.Success>(
            MermaidParser.parse("sequenceDiagram\nparticipant A as one #lt;br#gt;\nA->>B: fix #35;5#59; done # comment")).diagram)
        assertEquals("one <br>", MermaidText.decodeEntities(diagram.actors.first().label))
        assertEquals("fix #5; done", MermaidText.decodeEntities(diagram.messages.single().label))
    }
}
