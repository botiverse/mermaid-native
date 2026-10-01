package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class FlowMarkdownLabelTest {
    private fun diagram(label: String, markdown: Boolean = true, shape: FlowNodeShape = FlowNodeShape.RECTANGLE) =
        FlowchartDiagram(FlowDirection.TD, listOf(FlowNode("A", label, shape, if(markdown) "markdown" else "text")), emptyList())
    @Test fun styledMeasurementAndDrawingUseTheSameWordWidths() {
        val measure = TextMeasurer { text, style -> SceneSize(text.length * (if(style.fontWeight==700)20.0 else if(style.italic)15.0 else 10.0),14.0) }
        val scene=SimpleMermaidLayout.layout(diagram("**Bold** and *italic*"),measure,LayoutConfig())
        val node=scene.commands.filterIsInstance<DrawRect>().single().rect
        val words=scene.commands.filterIsInstance<DrawText>()
        assertEquals(listOf("Bold","and","italic"),words.map { it.text })
        assertEquals(700,words[0].style.fontWeight);assertTrue(words[2].style.italic)
        val heavy=diagram("**Bold**").let { it.copy(nodes=it.nodes.map { n->n.copy(styles=listOf("font-weight:900")) }) }
        assertEquals(900,SimpleMermaidLayout.layout(heavy,measure,LayoutConfig()).commands.filterIsInstance<DrawText>().single().style.fontWeight)
        assertEquals(252.0,node.width,0.0001)
        assertEquals(node.x+16,words.first().origin.x,0.0001)
        assertEquals(node.x+node.width-16,words.last().let { it.origin.x+measure.measure(it.text,it.style).width },0.0001)
    }
    @Test fun punctuationAdjacencyEscapesAndOrdinaryLabelsArePreserved() {
        val scene=SimpleMermaidLayout.layout(diagram("Hello **bold**! \\*literal\\*"),FixedWidthTextMeasurer,LayoutConfig())
        val words=scene.commands.filterIsInstance<DrawText>()
        assertEquals(listOf("Hello","bold","! *literal*"),words.map { it.text })
        assertEquals(words[1].origin.x+FixedWidthTextMeasurer.measure("bold",words[1].style).width,words[2].origin.x,0.0001)
        val plain=SimpleMermaidLayout.layout(diagram("**bold** and *italic*",false),FixedWidthTextMeasurer,LayoutConfig())
        assertEquals("**bold** and *italic*",plain.commands.filterIsInstance<DrawText>().single().text)
    }
    @Test fun explicitLinesFitDiamondAndUnicodeRemainsIntact() {
        val scene=SimpleMermaidLayout.layout(diagram("**中文**\n*你好* 👨‍👩‍👧‍👦",shape=FlowNodeShape.DIAMOND),FixedWidthTextMeasurer,LayoutConfig())
        val words=scene.commands.filterIsInstance<DrawText>()
        assertEquals(listOf("中文","你好","👨‍👩‍👧‍👦"),words.map { it.text })
        assertEquals(2,words.map { it.origin.y }.distinct().size)
        assertEquals(700,words.first().style.fontWeight);assertTrue(words[1].style.italic)
        val polygon=scene.commands.filterIsInstance<DrawPolygon>().single().points
        val cx=polygon.map { it.x }.average();val cy=polygon.map { it.y }.average()
        val rx=(polygon.maxOf { it.x }-polygon.minOf { it.x })/2
        val ry=(polygon.maxOf { it.y }-polygon.minOf { it.y })/2
        for(word in words) {
            val size=FixedWidthTextMeasurer.measure(word.text,word.style)
            val left=word.origin.x-if(word.anchor==TextAnchor.MIDDLE)size.width/2 else 0.0
            for(x in listOf(left,left+size.width))for(y in listOf(word.origin.y-size.height,word.origin.y))
                assertTrue(kotlin.math.abs(x-cx)/rx+kotlin.math.abs(y-cy)/ry<=1.001)
        }
    }
    @Test fun emptyUnmatchedAndUnderscoreLabelsRemainRenderable() {
        assertEquals(listOf(emptyList()),MermaidMarkdown.lines(""))
        assertEquals(listOf("an_unmatched","*literal"),MermaidMarkdown.lines("an_unmatched *literal").single().map { it.content })
        assertEquals(MermaidMarkdown.WordType.STRONG,MermaidMarkdown.lines("__strong__").single().single().type)
        val scene=SimpleMermaidLayout.layout(diagram(""),FixedWidthTextMeasurer,LayoutConfig())
        assertTrue(scene.width>0);assertTrue(scene.height>0)
    }
}
