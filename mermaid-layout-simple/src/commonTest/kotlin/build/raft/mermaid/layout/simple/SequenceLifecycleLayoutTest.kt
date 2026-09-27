package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class SequenceLifecycleLayoutTest {
    private fun layout(text: String): LayoutScene {
        val d=assertIs<MermaidParseResult.Success>(MermaidParser.parse("sequenceDiagram\n$text")).diagram
        return SimpleMermaidLayout.layout(d,FixedWidthTextMeasurer,LayoutConfig())
    }
    @Test fun createdParticipantStartsAtItsIncomingMessageAndDestroyedParticipantHasNoFooter() {
        val s=layout("A->>B: start\ncreate participant C\nautonumber\nB->>C: create\ndestroy C\nB->>C: finish")
        val cText=s.commands.filterIsInstance<DrawText>().single { it.text=="C" }
        val aText=s.commands.filterIsInstance<DrawText>().first { it.text=="A" }
        assertTrue(cText.origin.y>aText.origin.y+50)
        val cBox=s.commands.filterIsInstance<DrawRect>().single { cText.origin.x>it.rect.x && cText.origin.x<it.rect.x+it.rect.width && cText.origin.y>it.rect.y && cText.origin.y<it.rect.y+it.rect.height }
        val incoming=s.commands.filterIsInstance<DrawLine>().single { it.from.y==it.to.y && it.to.x==cBox.rect.x }
        assertEquals(cBox.rect.y+cBox.rect.height/2,incoming.to.y)
        val life=s.commands.filterIsInstance<DrawLine>().single { it.from.x==cText.origin.x && it.to.x==cText.origin.x }
        assertEquals(cBox.rect.y+cBox.rect.height,life.from.y)
        assertTrue(life.to.y<s.height-50)
        assertEquals(2,s.commands.filterIsInstance<DrawLine>().count { it.strokeWidth==2.0 && it.from.x!=it.to.x && it.from.y!=it.to.y })
    }
    @Test fun boxBackgroundContainsItsParticipantsAndStaysBehindMessages() {
        val s=layout("box aqua Workers\nparticipant A\nparticipant B\nend\nparticipant C\nA->>C: start")
        val box=s.commands.filterIsInstance<DrawRect>().single { it.fill.value=="#00ffff" }
        for(label in listOf("A","B")) s.commands.filterIsInstance<DrawText>().filter { it.text==label }.forEach {
            assertTrue(it.origin.x>box.rect.x && it.origin.x<box.rect.x+box.rect.width)
            assertTrue(it.origin.y>box.rect.y && it.origin.y<box.rect.y+box.rect.height)
        }
        assertEquals(box,s.commands.first())
        assertTrue(s.commands.filterIsInstance<DrawText>().first { it.text=="C" }.origin.x>box.rect.x+box.rect.width)
    }
}
