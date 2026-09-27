package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class SequenceParticipantLayoutTest {
    @Test fun stereotypesReachTheRealLayoutAndProduceDistinctOutlines() {
        val drawings=SequenceActorKind.entries.associateWith { kind ->
            val d=assertIs<MermaidParseResult.Success>(MermaidParser.parse("sequenceDiagram\nparticipant A@{type: '${kind.name.lowercase()}', alias: 'Public interface'}")).diagram
            val s=SimpleMermaidLayout.layout(d,FixedWidthTextMeasurer,LayoutConfig())
            assertEquals(2,s.commands.filterIsInstance<DrawText>().count { it.text=="Public interface" })
            s.commands.filter { it !is DrawText }
        }
        assertEquals(SequenceActorKind.entries.size,drawings.values.distinct().size)
        assertEquals(4,drawings.getValue(SequenceActorKind.DATABASE).filterIsInstance<DrawEllipse>().size)
        assertEquals(6,drawings.getValue(SequenceActorKind.COLLECTIONS).filterIsInstance<DrawRect>().size)
        assertEquals(4,drawings.getValue(SequenceActorKind.QUEUE).filterIsInstance<DrawEllipse>().size)
    }
    @Test fun reverseHalfArrowAppearsAtSourceOnTheUpperSide() {
        val d=assertIs<MermaidParseResult.Success>(MermaidParser.parse("sequenceDiagram\nA /|- B: reverse")).diagram
        val s=SimpleMermaidLayout.layout(d,FixedWidthTextMeasurer,LayoutConfig())
        val line=s.commands.filterIsInstance<DrawLine>().single { it.from.y==it.to.y }
        val head=s.commands.filterIsInstance<DrawPolygon>().single()
        assertEquals(line.from,head.points.first())
        assertTrue(head.points.any { it.y<line.from.y })
        assertTrue(head.points.all { it.y<=line.from.y })
    }

}
