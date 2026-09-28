package build.raft.mermaid.core
import kotlin.test.*
class StateGrammarTest {
    private fun parse(s:String)=assertIs<StateDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("stateDiagram-v2\n$s")).diagram)
    @Test fun bareNamesAndRepeatedDescriptionsRemainOrdered() {
        val d=parse("assemble\nstate assemblies\nassemble: first\nassemble : second: active")
        assertEquals(listOf("assemble","assemblies"),d.states.map { it.id })
        assertEquals("first\nsecond: active",d.states.first().description)
    }
    @Test fun quotedCompositesAndInlineChildrenRemainNested() {
        val d=parse("state \"Named group\" as outer { state inner { X } }\nouter-->Y")
        assertEquals(listOf("inner"),d.states.first { it.id=="outer" }.childIds)
        assertEquals(listOf("X"),d.states.first { it.id=="inner" }.childIds)
    }
    @Test fun multilineNotesEndOnlyAtStandaloneEndNote() {
        val d=parse("A\nnote right of A\nthis contains end note inside text\nthis contains send note too\nend note")
        assertEquals("this contains end note inside text\nthis contains send note too",d.notes.single().text)
    }
    @Test fun accessibilityAndLocalDirectionsAreTyped() {
        val d=parse("accTitle: State machine\naccDescr {\nfirst\n second\n}\nstate outer {\ndirection LR\nA-->B\n}")
        assertEquals("State machine",d.accessibilityTitle)
        assertEquals("first\nsecond",d.accessibilityDescription)
        assertEquals(FlowDirection.LR,d.states.first().direction)
    }
    @Test fun unclosedAndCyclicCompositesFailExplicitly() {
        listOf("state A { B","state A { state A { B } }","state invalid name { X }").forEach { assertIs<MermaidParseResult.Failure>(MermaidParser.parse("stateDiagram-v2\n$it")) }
    }
    @Test fun identicalAliasAndImplicitReferencesRetainDeclarationIdentity() {
        val d=parse("state \"as\" as as\nas --> Other")
        assertTrue(d.states.first { it.id=="as" }.explicitLabel)
        assertTrue(d.states.first { it.id=="as" }.declared)
        assertFalse(d.states.first { it.id=="Other" }.declared)
    }
    @Test fun leadingCommentsPreserveHeaderAndInvalidScaleFails() {
        assertIs<MermaidParseResult.Success>(MermaidParser.parse("%% comment\nstateDiagram-v2\nA"))
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("stateDiagram-v2\nscale nonsense\nA"))
    }
}
