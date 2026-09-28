package build.raft.mermaid.layout.simple
import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*
class StateGrammarLayoutTest {
    private fun layout(s:String)=SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse("stateDiagram-v2\n$s")).diagram,FixedWidthTextMeasurer,LayoutConfig())
    @Test fun compositeContainsItsActualChildRectangles() {
        val scene=layout("state outer { A-->B }")
        val rects=scene.commands.filterIsInstance<DrawRect>().map { it.rect }
        assertEquals(3,rects.size)
        val outer=rects.first()
        rects.drop(1).forEach { assertTrue(it.x>=outer.x && it.y>=outer.y && it.x+it.width<=outer.x+outer.width && it.y+it.height<=outer.y+outer.height) }
    }
    @Test fun descriptionsAndMultilineNotesAreActuallyDrawnWithinScene() {
        val scene=layout("A: first\nA: second\nnote left of A\nLong note line\nAnother note line\nend note")
        val text=scene.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue(text.containsAll(listOf("first","second","Long note line","Another note line")))
        scene.commands.filterIsInstance<DrawRect>().forEach { assertTrue(it.rect.x>=0 && it.rect.x+it.rect.width<=scene.width && it.rect.y+it.rect.height<=scene.height) }
    }
    @Test fun transitionsUseTheDirectionOfTheirCommonComposite() {
        for (direction in listOf("LR", "RL", "TB", "BT")) {
            val global = if (direction in listOf("LR", "RL")) "TB" else "LR"
            val scene = layout("direction $global\nstate outer {\ndirection $direction\nA: first\nA: second\nA --> B\n}")
            val line = scene.commands.filterIsInstance<DrawLine>().single()
            val nodes = scene.commands.filterIsInstance<DrawRect>().drop(1).map { it.rect }
            val a = nodes[0]; val b = nodes[1]
            when (direction) {
                "LR" -> { assertEquals(a.x+a.width,line.from.x); assertEquals(b.x,line.to.x); assertEquals(line.from.y,line.to.y) }
                "RL" -> { assertEquals(a.x,line.from.x); assertEquals(b.x+b.width,line.to.x); assertEquals(line.from.y,line.to.y) }
                "TB" -> { assertEquals(a.y+a.height,line.from.y); assertEquals(b.y,line.to.y); assertEquals(line.from.x,line.to.x) }
                "BT" -> { assertEquals(a.y,line.from.y); assertEquals(b.y+b.height,line.to.y); assertEquals(line.from.x,line.to.x) }
            }
        }
    }
}
