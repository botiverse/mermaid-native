package build.raft.mermaid.core

import kotlin.test.*

class StateRegionsTest {
    private fun parse(source:String)=assertIs<StateDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("stateDiagram-v2\n$source")).diagram)
    @Test fun concurrentBoundariesPreserveDistinctTerminalsAndChildOrder() {
        val d=parse("state Active {\n[*] --> Off\n--\n[*] --> On\n}")
        val children=d.states.single { it.id=="Active" }.childIds.map { id -> d.states.single { it.id==id } }
        assertEquals(listOf(StateNodeKind.START,StateNodeKind.STATE,StateNodeKind.DIVIDER,StateNodeKind.START,StateNodeKind.STATE),children.map { it.kind })
        assertEquals(2,d.transitions.map { it.from }.distinct().size)
    }
    @Test fun floatingNoteKeepsAliasAndMultilineText() {
        val d=parse("state Box {\nnote \"First<br/>Second\" as N1\nN1 --> Done\n}")
        val n=d.states.single { it.id=="N1" }
        assertEquals(StateNodeKind.NOTE,n.kind)
        assertEquals("First<br/>Second",n.label)
        assertTrue("N1" in d.states.single { it.id=="Box" }.childIds)
    }
    @Test fun dividerOutsideCompositeIsRejected() {
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("stateDiagram-v2\n--"))
    }
}
