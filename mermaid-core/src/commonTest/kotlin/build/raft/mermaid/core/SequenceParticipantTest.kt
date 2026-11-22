package build.raft.mermaid.core
import kotlin.test.*

class SequenceParticipantTest {
    private fun parse(source: String)=assertIs<SequenceDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
    @Test fun metadataTypesAndAliasesSurviveParsing() {
        val d=parse("""sequenceDiagram
            participant API@{ type: 'boundary', alias: 'Internal' } as Public API
            actor DB@{ "type": "database", "alias": "Database" }
            participant Q@{type: queue}
            API->>DB: select
            DB->>Q: publish
        """.trimIndent())
        assertEquals(listOf(SequenceActorKind.BOUNDARY,SequenceActorKind.DATABASE,SequenceActorKind.QUEUE),d.actors.map { it.kind })
        assertEquals(listOf("Public API","Database","Q"),d.actors.map { it.label })
    }
    @Test fun quotedMetadataCanContainDelimiters() {
        val d=parse("sequenceDiagram\nparticipant A@{alias: \"One; #two: \\\"three\\\"\", type: 'entity'}\nA->>A: test")
        assertEquals("One; #two: \"three\"",d.actors.single().label)
    }
    @Test fun malformedMetadataDoesNotFallBackToOrdinaryParticipant() {
        listOf("type 'database'", "type: 'missing", "type: 'unsupported'", "type: database, type: actor", "alias: [not,a,string]").forEach {
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("sequenceDiagram\nparticipant A@{$it}"),it)
        }
    }
    @Test fun linksDeclareTheirParticipant() {
        val d=parse("sequenceDiagram\nlink Alice: Dashboard @ https://dashboard.contoso.com/alice\nlink John: Wiki @ https://wiki.contoso.com/john\nAlice->>John: Hello")
        assertEquals(listOf("Alice","John"),d.actors.map { it.id })
        assertEquals("https://wiki.contoso.com/john",d.actors.first { it.id=="John" }.links["Wiki"])
    }
    @Test fun halfArrowsRetainHeadSideDirectionAndLineStyle() {
        val d=parse("sequenceDiagram\nA -|\\ B: top\nA \\|-- B: lower reverse dotted\nA -// B: lower open")
        assertEquals(SequenceArrowHead.HALF_FILLED_TOP,d.messages[0].arrowHead)
        assertFalse(d.messages[0].headAtSource)
        assertEquals(SequenceArrowHead.HALF_FILLED_BOTTOM,d.messages[1].arrowHead)
        assertTrue(d.messages[1].headAtSource)
        assertEquals(SequenceLineStyle.DASHED,d.messages[1].lineStyle)
        assertEquals(SequenceArrowHead.HALF_OPEN_BOTTOM,d.messages[2].arrowHead)
    }

}
