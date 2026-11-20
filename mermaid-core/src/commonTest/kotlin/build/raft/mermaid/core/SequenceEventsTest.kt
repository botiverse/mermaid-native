package build.raft.mermaid.core

import kotlin.test.*

class SequenceEventsTest {
    private fun parse(source: String) = assertIs<SequenceDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)

    @Test fun preservesNotesActivationsAndNestedFragmentsInSourceOrder() {
        val d = parse("""sequenceDiagram
            A->>+B: first
            Note right of B: reminder
            alt success
            B-->>-A: done
            else retry
            loop twice
            A->>B: retry
            end
            end
        """.trimIndent())
        assertEquals(listOf("message", "activation", "note", "ALT:START", "message", "activation", "ALT:BRANCH", "LOOP:START", "message", "LOOP:END", "ALT:END"), d.events.map {
            when (it) { is SequenceMessage -> "message"; is SequenceActivation -> "activation"; is SequenceNote -> "note"; is SequenceFragment -> "${it.kind}:${it.boundary}"; else -> "numbering" }
        })
        assertTrue(d.messages.first().activate)
    }

    @Test fun handlesDecimalNumberingAndMetadata() {
        val d = parse("""sequenceDiagram
            title: Request flow
            accTitle:Accessible flow
            accDescr {
            A request and response
            }
            autonumber 10.01 .01
            1->>2:wrap: one two
            autonumber off
            2-->>1:nowrap: done
        """.trimIndent())
        assertEquals("Request flow",d.title)
        assertEquals("Accessible flow",d.accessibilityTitle)
        assertEquals("A request and response",d.accessibilityDescription)
        assertEquals(SequenceNumbering(true,10.01,.01),d.events[0])
        assertEquals(true,d.messages[0].wrap)
        assertEquals("one two",d.messages[0].label)
        assertEquals(false,d.messages[1].wrap)
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("sequenceDiagram\nautonumber 1.001"))
    }

    @Test fun parsesAutonumberStartStepAndOffInSourceOrder() {
        val d = parse("""sequenceDiagram
            autonumber
            A->>B: one
            autonumber 10 5
            B->>B: self
            Note right of B: not numbered
            autonumber off
            B-->>A: hidden
            autonumber 3
            A->>B: restarted
        """.trimIndent())
        assertEquals(listOf(SequenceNumbering(true), SequenceNumbering(true, 10.0, 5.0), SequenceNumbering(false), SequenceNumbering(true, 3.0, 1.0)),
            d.events.filterIsInstance<SequenceNumbering>())
        assertEquals(listOf("numbering", "message", "numbering", "message", "note", "numbering", "message", "numbering", "message"), d.events.map {
            when (it) { is SequenceMessage -> "message"; is SequenceNote -> "note"; is SequenceNumbering -> "numbering"; else -> "other" }
        })
        listOf("autonumber 1 2 3", "autonumber ten", "autonumber -1").forEach {
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("sequenceDiagram\n$it\nA->>B: x"), it)
        }
    }

    @Test fun selfMessagesKeepOneParticipantAndTheirArrow() {
        val d = parse("sequenceDiagram\nparticipant Bob\nBob->>Bob: think\nBob-->>Bob: reply to self\nBob-)Bob: async")
        assertEquals(listOf("Bob"), d.actors.map { it.id })
        assertTrue(d.messages.all { it.from == "Bob" && it.to == "Bob" })
        assertEquals(listOf("think", "reply to self", "async"), d.messages.map { it.label })
        assertEquals(listOf(SequenceArrowHead.FILLED, SequenceArrowHead.FILLED, SequenceArrowHead.OPEN), d.messages.map { it.arrowHead })
        assertEquals(SequenceLineStyle.DASHED, d.messages[1].lineStyle)
    }

    @Test fun keepsCentralAndBidirectionalSemantics() {
        val d=parse("sequenceDiagram; Alice ()<<-->>() Bob: hello")
        assertEquals(SequenceCentralConnection.BOTH,d.messages.single().centralConnection)
        assertTrue(d.messages.single().bidirectional)
        assertEquals(SequenceLineStyle.DASHED,d.messages.single().lineStyle)
    }

    @Test fun rejectsInvalidEventNestingAndInactiveEnds() {
        listOf("else no", "end", "loop unfinished", "opt maybe\nelse invalid\nend", "deactivate A", "A->>B no colon").forEach {
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("sequenceDiagram\n$it"),it)
        }
    }
}
