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
    @Test fun floatingNoteActuallyRendersMultilineYellowBox() {
        val scene=layout("note \"First<br/>Second\" as N1")
        assertTrue(scene.commands.filterIsInstance<DrawRect>().any { it.fill.value=="#fff5ad" })
        assertEquals(listOf("First","Second"),scene.commands.filterIsInstance<DrawText>().map { it.text })
    }
    @Test fun compositeAndLeafSelfLoopsHaveVisiblePathsWithinScene() {
        val scene=layout("state Active { Idle }\nActive --> Active\nIdle --> Idle")
        val loops=scene.commands.filterIsInstance<DrawPolyline>()
        assertEquals(2,loops.size)
        loops.forEach { path ->
            assertEquals(4,path.points.distinct().size)
            assertTrue(path.points.all { it.x in 0.0..scene.width && it.y in 0.0..scene.height })
        }
    }
}
