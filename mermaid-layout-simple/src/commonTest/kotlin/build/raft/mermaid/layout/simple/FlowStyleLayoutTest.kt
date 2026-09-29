package build.raft.mermaid.layout.simple
import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*
class FlowStyleLayoutTest {
    private fun layout(s:String)=SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse("graph LR\n$s")).diagram,FixedWidthTextMeasurer,LayoutConfig())
    @Test fun classAndInlineStylesReachMeasuredActualNodes() {
        val scene=layout("A[Wide]:::accent --> B\nclassDef accent background:rgb(1,2,3),border:3px solid red,font-size:150%,color:blue\nstyle A fill:#ffeeaa")
        val boxes=scene.commands.filterIsInstance<DrawRect>()
        assertEquals("#ffeeaa",boxes[0].fill.value)
        assertEquals("#ff0000",boxes[0].stroke.value)
        assertEquals(3.0,boxes[0].strokeWidth)
        val label=scene.commands.filterIsInstance<DrawText>().first { it.text=="Wide" }
        assertEquals(21.0,label.style.fontSize)
        assertEquals("#0000ff",label.style.color.value)
        assertEquals(DiagramPalette.SURFACE,boxes[1].fill.value)
    }
    @Test fun edgeOverridesAndDashedNodeBordersReachDrawCommands() {
        val scene=layout("A:::dashed --> B\nclassDef dashed stroke-dasharray:4\nlinkStyle 0 stroke:#ff0000,stroke-width:4px")
        assertTrue(scene.commands.filterIsInstance<DrawPolygon>().any { it.fill.value=="#ff0000" })
        assertTrue(scene.commands.filterIsInstance<DrawPolyline>().any { it.pattern==StrokePattern.DASHED })
        assertTrue(scene.commands.filterIsInstance<DrawLine>().any { it.stroke.value=="#ff0000" && it.strokeWidth==4.0 })
    }
}
