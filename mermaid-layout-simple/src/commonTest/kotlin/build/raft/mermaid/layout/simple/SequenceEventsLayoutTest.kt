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
    }
    @Test fun rendersNumberingFragmentsAndMetadata() {
        val s=scene("sequenceDiagram\ntitle Request\naccTitle: Flow\nautonumber 10.01 .01\nloop retry\nA->>B: first\nB-->>A: second\nend\nautonumber off\nA->>B: third")
        val text=s.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue("10.01. first" in text)
        assertTrue("10.02. second" in text)
        assertTrue("third" in text)
        assertTrue("loop" in text && "retry" in text && "Request" in text)
        assertEquals("Flow",s.accessibilityTitle)
        val frame=s.commands.filterIsInstance<DrawRect>().first()
        assertEquals("none",frame.fill.value)
        assertTrue(frame.rect.height>100)
    }
    @Test fun hiddenMessagesStillAdvanceTheSequenceCounter() {
        val s=scene("sequenceDiagram\nA->>B: hidden\nautonumber\nA->>B: shown\nautonumber off\nA->>B: hidden again\nautonumber\nA->>B: shown again")
        val text=s.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue("2. shown" in text)
        assertTrue("4. shown again" in text)
    }
    @Test fun measuredNotesStayInsideSceneAndSelfLoopsHaveWidth() {
        val s=scene("sequenceDiagram\nNote left of A: a wide note beside the first participant\nA->>A: a long self call label\nNote over A: final")
        s.commands.filterIsInstance<DrawRect>().forEach { assertTrue(it.rect.x>=0 && it.rect.x+it.rect.width<=s.width) }
        s.commands.filterIsInstance<DrawPolyline>().forEach { line -> line.points.forEach { assertTrue(it.x<=s.width) } }
    }
}
