package build.raft.mermaid.layout.simple
import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*
class StateStylesLayoutTest {
    @Test fun actualStateClassFillStrokeAndMeasuredFontReachScene() {
        val d=assertIs<MermaidParseResult.Success>(MermaidParser.parse("stateDiagram-v2\nA:::hot --> B\nclassDef hot fill:#ffcccc,stroke:#cc0000,stroke-width:3px,font-size:24px\nA: Long description")).diagram
        val scene=SimpleMermaidLayout.layout(d,FixedWidthTextMeasurer,LayoutConfig())
        assertTrue(scene.commands.filterIsInstance<DrawRect>().any { it.fill.value=="#ffcccc" && it.stroke.value=="#cc0000" && it.strokeWidth==3.0 })
        assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text=="Long description" && it.style.fontSize==24.0 })
        assertTrue(scene.commands.filterIsInstance<DrawRect>().any { it.fill.value=="#eeeeee" })
    }
}
