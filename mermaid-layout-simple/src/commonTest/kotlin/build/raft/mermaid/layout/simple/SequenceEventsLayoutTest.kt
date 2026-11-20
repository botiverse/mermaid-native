package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class SequenceEventsLayoutTest {
    private fun scene(source: String): LayoutScene {
        val d=assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram
        return SimpleMermaidLayout.layout(d,TextMeasurer { text,style -> SceneSize(text.length*style.fontSize*.55,style.fontSize) },LayoutConfig())
    }
    @Test fun notesKeepTimeAndActivationEndsAtReply() {
        val s=scene("sequenceDiagram\nA->>+B: first\nNote right of B: between\nB-->>-A: reply\nA->>B: after")
        val texts=s.commands.filterIsInstance<DrawText>().associate { it.text to it.origin.y }
        assertTrue(texts.getValue("first")<texts.getValue("between"))
        assertTrue(texts.getValue("between")<texts.getValue("reply"))
        assertTrue(texts.getValue("reply")<texts.getValue("after"))
        val bar=s.commands.filterIsInstance<DrawRect>().single { it.rect.width==8.0 }
        assertEquals(texts.getValue("first")+8,bar.rect.y)
        assertEquals(texts.getValue("reply")+8,bar.rect.y+bar.rect.height)
        val request=s.commands.filterIsInstance<DrawLine>().single { it.from.y==texts.getValue("first")+8 && it.from.y==it.to.y }
        assertEquals(bar.rect.x,request.to.x)
        val response=s.commands.filterIsInstance<DrawLine>().single { it.from.y==texts.getValue("reply")+8 && it.from.y==it.to.y }
        assertEquals(bar.rect.x,response.from.x)
    }
    @Test fun rendersNumberingFragmentsAndMetadata() {
        val s=scene("sequenceDiagram\ntitle Request\naccTitle: Flow\nautonumber 10.01 .01\nloop retry\nA->>B: first\nB-->>A: second\nend\nautonumber off\nA->>B: third")
        val text=s.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue("first" in text && "second" in text && "third" in text)
        assertEquals(listOf("10.01","10.02"),numbers(s))
        assertTrue("loop" in text && "retry" in text && "Request" in text)
        assertEquals("Flow",s.accessibilityTitle)
        val frame=s.commands.filterIsInstance<DrawRect>().first()
        assertEquals("none",frame.fill.value)
        assertTrue(frame.rect.height>100)
    }
    @Test fun hiddenMessagesStillAdvanceTheSequenceCounter() {
        val s=scene("sequenceDiagram\nA->>B: hidden\nautonumber\nA->>B: shown\nautonumber off\nA->>B: hidden again\nautonumber\nA->>B: shown again")
        assertEquals(listOf("2","4"),numbers(s))
    }
    private fun numberDiscs(s: LayoutScene)=s.commands.filterIsInstance<DrawEllipse>().filter { it.fill.value==DiagramPalette.INK }
    private fun numbers(s: LayoutScene)=s.commands.filterIsInstance<DrawText>().filter { it.style.color.value==DiagramPalette.CANVAS }.map { it.text }
    @Test fun autonumberDrawsFilledDiscsAtEachMessageStart() {
        val s=scene("sequenceDiagram\nautonumber\nAlice->>John: hello\nNote right of John: thinking\nJohn->>John: self\nJohn-->>Alice: great")
        assertEquals(listOf("1","2","3"),numbers(s))
        val discs=numberDiscs(s)
        assertEquals(3,discs.size)
        val texts=s.commands.filterIsInstance<DrawText>()
        val alice=texts.first { it.text=="Alice" }.origin.x; val john=texts.first { it.text=="John" }.origin.x
        // Each disc sits on its message line at the sending end, with its number centred inside.
        assertEquals(listOf(alice,john,john),discs.map { it.center.x })
        val lines=s.commands.filterIsInstance<DrawLine>().filter { it.from.y==it.to.y && it.from.x!=it.to.x }
        assertEquals(lines.map { it.from }.toSet(),setOf(discs[0].center,discs[2].center))
        assertEquals(discs[1].center,s.commands.filterIsInstance<DrawPath>().single().segments.first().let { (it as PathMove).to })
        texts.filter { it.style.color.value==DiagramPalette.CANVAS }.zip(discs).forEach { (number,disc) ->
            assertEquals(TextAnchor.MIDDLE,number.anchor)
            assertEquals(disc.center.x,number.origin.x)
            assertTrue(number.origin.y>disc.center.y && number.origin.y<disc.center.y+disc.radiusY)
        }
        assertTrue(texts.none { it.text.startsWith("1.") || it.text.startsWith("2.") })
    }
    @Test fun autonumberHonoursStartStepAndOff() {
        val s=scene("sequenceDiagram\nautonumber 10 5\nA->>B: a\nB->>C: b\nC-->>B: c\nautonumber off\nB-->>A: d")
        assertEquals(listOf("10","15","20"),numbers(s))
        assertEquals(3,numberDiscs(s).size)
    }
    @Test fun selfMessageLoopsRightAndReturnsBelowWithCentredLabel() {
        val s=scene("sequenceDiagram\nparticipant A\nparticipant B\nA->>B: go\nB->>B: think\nB-->>A: done")
        val texts=s.commands.filterIsInstance<DrawText>()
        val b=texts.first { it.text=="B" }.origin.x
        val label=texts.single { it.text=="think" }
        assertEquals(TextAnchor.MIDDLE,label.anchor)
        assertEquals(b,label.origin.x)
        val loop=s.commands.filterIsInstance<DrawPath>().single()
        val start=(loop.segments.first() as PathMove).to
        val curves=loop.segments.drop(1).map { it as PathQuadratic }
        assertEquals(2,curves.size)
        val end=curves.last().to
        assertEquals(b,start.x); assertEquals(b,end.x)
        assertEquals(20.0,end.y-start.y)
        assertTrue(start.y>label.origin.y+8)
        assertTrue(curves.all { it.control.x>b+30 && it.control.x<b+60 })
        // The filled head points back into the lifeline at the bottom of the loop.
        val head=s.commands.filterIsInstance<DrawPolygon>().single { end in it.points }
        assertTrue(head.points.all { it.x>=b })
        // The reply starts below the loop instead of overlapping it.
        assertTrue(texts.single { it.text=="done" }.origin.y>end.y)
        assertTrue(s.commands.filterIsInstance<DrawPolyline>().none())
    }
    @Test fun longSelfMessageLabelPushesNeighboursApartAndStaysInScene() {
        val label="Recompute the cached totals for every open order"
        val s=scene("sequenceDiagram\nparticipant A\nparticipant B\nparticipant C\nA->>B: go\nB->>B: $label\nB->>C: next")
        val texts=s.commands.filterIsInstance<DrawText>()
        val half=label.length*14*.55/2
        val a=texts.first { it.text=="A" }.origin.x; val b=texts.first { it.text=="B" }.origin.x; val c=texts.first { it.text=="C" }.origin.x
        assertTrue(b-a>=half && c-b>=half)
        assertEquals(b,texts.single { it.text==label }.origin.x)
        assertTrue(b+half<=s.width)
        val first=scene("sequenceDiagram\nA->>A: $label\nA->>B: next")
        val x=first.commands.filterIsInstance<DrawText>().single { it.text==label }.origin.x
        assertTrue(x-half>=0.0 && x+half<=first.width)
    }
    @Test fun measuredNotesStayInsideSceneAndSelfLoopsHaveWidth() {
        val s=scene("sequenceDiagram\nNote left of A: a wide note beside the first participant\nA->>A: a long self call label\nNote over A: final")
        s.commands.filterIsInstance<DrawRect>().forEach { assertTrue(it.rect.x>=0 && it.rect.x+it.rect.width<=s.width) }
        s.commands.filterIsInstance<DrawPath>().forEach { path -> path.conservativeBounds()!!.let { assertTrue(it.x>=0 && it.x+it.width<=s.width) } }
        s.commands.filterIsInstance<DrawText>().filter { it.text=="a long self call label" }.forEach { assertTrue(it.origin.x>0) }
    }
}
