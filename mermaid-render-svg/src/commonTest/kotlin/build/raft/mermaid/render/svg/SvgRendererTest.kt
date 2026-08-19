package build.raft.mermaid.render.svg

import build.raft.mermaid.core.FlowDirection
import build.raft.mermaid.core.StateDiagram
import build.raft.mermaid.core.StateNode
import build.raft.mermaid.core.StateNodeKind
import build.raft.mermaid.core.StateTransition
import build.raft.mermaid.layout.DrawText
import build.raft.mermaid.layout.LayoutConfig
import build.raft.mermaid.layout.LayoutScene
import build.raft.mermaid.layout.ScenePoint
import build.raft.mermaid.layout.TextAnchor
import build.raft.mermaid.layout.TextStyle
import build.raft.mermaid.layout.simple.FixedWidthTextMeasurer
import build.raft.mermaid.layout.simple.SimpleMermaidLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SvgRendererTest {
    @Test
    fun stateDiagramSvgIsDeterministicAndContainsVisibleTransition() {
        val diagram = StateDiagram(
            direction = FlowDirection.LR,
            states = listOf(StateNode("__start_0", "", StateNodeKind.START), StateNode("Ready", "Ready")),
            transitions = listOf(StateTransition("__start_0", "Ready", "begin & run")),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        val svg = SvgRenderer.render(scene)

        assertEquals(svg, SvgRenderer.render(scene))
        assertTrue(svg.contains("begin &amp; run"))
        assertTrue(svg.contains("<polygon"))
        assertTrue(svg.endsWith("</svg>\n"))
    }

    @Test
    fun serializerIsDeterministicAndEscapesTextAndAttributes() {
        val scene = LayoutScene(
            width = 100.0,
            height = 40.5,
            commands = listOf(
                DrawText(
                    text = "<unsafe & visible>",
                    origin = ScenePoint(50.0, 24.25),
                    anchor = TextAnchor.MIDDLE,
                    style = TextStyle(fontFamily = "A&B\""),
                ),
            ),
        )
        val svg = SvgRenderer.render(scene)
        assertEquals(svg, SvgRenderer.render(scene))
        assertTrue(svg.contains("&lt;unsafe &amp; visible&gt;"))
        assertTrue(svg.contains("font-family=\"A&amp;B&quot;\""))
        assertFalse(svg.contains("<unsafe"))
        assertTrue(svg.endsWith("</svg>\n"))
    }

    @Test
    fun hostCanInjectCjkFontFallbackWithoutChangingTextBytes() {
        val scene = LayoutScene(
            width = 100.0,
            height = 40.0,
            commands = listOf(DrawText("中文", ScenePoint(50.0, 24.0), TextAnchor.MIDDLE)),
        )

        val svg = SvgRenderer.render(scene, SvgRenderConfig("Noto Sans CJK SC, PingFang SC, sans-serif"))

        assertTrue(svg.contains("font-family=\"Noto Sans CJK SC, PingFang SC, sans-serif\""))
        assertTrue(svg.contains(">中文</text>"))
    }

    @Test
    fun defaultCjkStackDoesNotDriftLatinText() {
        val scene = LayoutScene(
            width = 100.0,
            height = 40.0,
            commands = listOf(DrawText("English", ScenePoint(50.0, 24.0), TextAnchor.MIDDLE)),
        )

        assertTrue(SvgRenderer.render(scene).contains("font-family=\"sans-serif\""))
    }
}
