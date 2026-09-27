package build.raft.mermaid.core

import kotlin.test.*

class SequenceLifecycleTest {
    private fun parse(text: String) = assertIs<SequenceDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("sequenceDiagram\n$text")).diagram)

    @Test fun boxesKeepTheirParticipantsAndColorsWithoutBecomingTimeEvents() {
        val d=parse("box aqua Group one\nparticipant A\nactor B\nend\nbox Group two\nparticipant C\nend\nA->>C: hello")
        assertEquals(listOf(SequenceBox("Group one","aqua",listOf("A","B")),SequenceBox("Group two","transparent",listOf("C"))),d.boxes)
        assertEquals(1,d.events.size)
        assertEquals("#ff0000",parse("box #ff0000\nparticipant A\nend").boxes.single().color)
    }
    @Test fun actorDataPreservesQuotedDelimitersAndMergesLinks() {
        val d=parse("""participant A
links A: {"Repo": "https://example.test/a;b#part"}
link A: Mail @ https://example.test/?a=user@example.test
links A: {"Dashboard": "https://example.test/"}
properties A: {"class": "internal-service", "icon": "@clock"}
""")
        assertEquals(3,d.actors.single().links.size)
        assertEquals("https://example.test/a;b#part",d.actors.single().links["Repo"])
        assertEquals("https://example.test/?a=user@example.test",d.actors.single().links["Mail"])
        assertEquals(mapOf("class" to "internal-service","icon" to "@clock"),d.actors.single().properties)
    }
    @Test fun creationAndDestructionRemainOrderedAroundTheirMessages() {
        val d=parse("A->>B: start\ncreate actor C as Worker\nB->>C: create\nC->>B: ready\ndestroy C\nB->>C: finish")
        assertEquals(listOf(SequenceLifecycle("C",true),SequenceLifecycle("C",false)),d.events.filterIsInstance<SequenceLifecycle>())
        assertEquals(SequenceActorKind.ACTOR,d.actors.last().kind)
        assertIs<SequenceLifecycle>(d.events[1]);assertIs<SequenceLifecycle>(d.events[4])
    }
    @Test fun accessibilityKeepsLiteralHashesAndSemicolonsAndMessagesKeepBraces() {
        val d=parse("accTitle: A#1; visible\naccDescr: description#2; stays\nA->>B: {text}\nB->>A: next")
        assertEquals("A#1; visible",d.accessibilityTitle)
        assertEquals("description#2; stays",d.accessibilityDescription)
        assertEquals(listOf("{text}","next"),d.messages.map { it.label })
    }
    @Test fun invalidGroupsAndLifetimesFailClosed() {
        listOf("box A\nparticipant X", "box A\nX->>Y: invalid\nend", "box A\nparticipant X\nend\nbox B\nparticipant X\nend",
            "participant A\ncreate participant A\nB->>A: again", "create participant A", "create participant A\nB->>C: wrong",
            "A->>B: start\ndestroy B\nA->>B: bye\nB->>A: dead").forEach { input ->
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("sequenceDiagram\n$input"),input)
        }
    }
}
