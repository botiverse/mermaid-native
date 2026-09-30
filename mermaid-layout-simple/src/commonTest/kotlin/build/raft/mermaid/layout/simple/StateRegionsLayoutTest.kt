package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class StateRegionsLayoutTest {
    private fun layout(s:String)=SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse("stateDiagram-v2\n$s")).diagram,FixedWidthTextMeasurer,LayoutConfig())
    @Test fun concurrentRegionsRenderSeparateColumnsWithDividerInsideGroup() {
        val scene=layout("state Active {\nA --> B\n--\nC --> D\n}")
        val separator=scene.commands.filterIsInstance<DrawLine>().single { it.pattern==StrokePattern.DASHED }
        val text=scene.commands.filterIsInstance<DrawText>().associateBy { it.text }
        assertTrue(text.getValue("A").origin.x < separator.from.x)
        assertTrue(text.getValue("C").origin.x > separator.from.x)
        assertEquals(text.getValue("A").origin.x,text.getValue("B").origin.x)
        assertEquals(text.getValue("C").origin.x,text.getValue("D").origin.x)
        assertEquals(3,scene.commands.filterIsInstance<DrawLine>().size)
    }
    @Test fun floatingNoteActuallyRendersMultilineNoteBox() {
        val scene=layout("note \"First<br/>Second\" as N1")
        val first=scene.commands.filterIsInstance<DrawText>().single { it.text=="First" }.origin
        // Notes use the web note colors; identify the box by the note text it contains.
        val note=scene.commands.filterIsInstance<DrawRect>().single { it.rect.contains(first) }
        assertEquals(DiagramPalette.NOTE_SURFACE,note.fill.value)
        assertEquals(DiagramPalette.NOTE_BORDER,note.stroke.value)
        assertEquals(listOf("First","Second"),scene.commands.filterIsInstance<DrawText>().map { it.text })
    }
    private fun SceneRect.contains(p:ScenePoint)=p.x>=x && p.x<=x+width && p.y>=y && p.y<=y+height
    @Test fun compositeAndLeafSelfLoopsHaveVisiblePathsWithinScene() {
        val scene=layout("state Active { Idle }\nActive --> Active\nIdle --> Idle")
        val loops=scene.commands.filterIsInstance<DrawPolyline>()
        assertEquals(2,loops.size)
        loops.forEach { path ->
            assertEquals(4,path.points.distinct().size)
            assertTrue(path.points.all { it.x in 0.0..scene.width && it.y in 0.0..scene.height })
        }
    }
    @Test fun defaultClassFontMeasuresCompositeTitleAsDrawn() {
        val scene=layout("classDef default font-size:40px\nstate LongCompositeTitle { A }")
        val title=scene.commands.filterIsInstance<DrawText>().single { it.text=="LongCompositeTitle" }
        val group=scene.commands.filterIsInstance<DrawRect>().first().rect
        assertEquals(40.0,title.style.fontSize)
        assertTrue(group.width>=FixedWidthTextMeasurer.measure(title.text,title.style).width+24)
    }
}
