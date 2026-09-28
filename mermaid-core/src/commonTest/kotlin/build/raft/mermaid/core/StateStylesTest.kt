package build.raft.mermaid.core
import kotlin.test.*
class StateStylesTest {
    private fun parse(s:String)=assertIs<StateDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("stateDiagram-v2\n$s")).diagram)
    @Test fun classesAttachToTerminalAndGroupMembersWithoutLosingKind() {
        val d=parse("classDef hot fill:#ffcccc,stroke:#cc0000\nstate group {\n[*]:::hot --> A:::hot\n}\nclass group hot")
        assertEquals(listOf("fill:#ffcccc","stroke:#cc0000"),d.classDefinitions["hot"])
        assertEquals(listOf("hot"),d.states.first { it.kind==StateNodeKind.START }.classes)
        assertEquals(listOf("hot"),d.states.first { it.id=="A" }.classes)
        assertEquals(listOf("hot"),d.states.first { it.id=="group" }.classes)
    }
    @Test fun inlineClassStatementAndStyleCanCreateTargets() {
        val d=parse("A --> B class A hot\nclass A, B, C hot\nstyle B,C fill:#ffcccc,stroke-width:3px")
        assertEquals(setOf("A","B","C"),d.states.map { it.id }.toSet())
        assertEquals(listOf("fill:#ffcccc","stroke-width:3px"),d.states.first { it.id=="B" }.styles)
    }
    @Test fun whitespaceSeparatedIdentifiersKeepSinglePercentLiteral() {
        val d=parse("% not a comment\nMoving --> Still %inline")
        assertEquals(7,d.states.size)
        assertTrue(d.states.any { it.id=="%inline" })
    }
    @Test fun literalBracesSurviveDescriptionsTransitionLabelsAndInlineNotes() {
        val result=assertIs<MermaidParseResult.Success>(MermaidParser.parse("stateDiagram-v2\nA: map {x}\nA --> B: {ok}\nnote right of A: use {x}"))
        val d=assertIs<StateDiagram>(result.diagram)
        assertEquals("map {x}",d.states.single { it.id=="A" }.description)
        assertEquals("{ok}",d.transitions.single().label)
        assertEquals("use {x}",d.notes.single().text)
    }
    @Test fun indirectCompositeCyclesFailExplicitly() {
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("stateDiagram-v2\nstate A { B }\nstate B { A }"))
    }
}
