package build.raft.mermaid.render.svg

import build.raft.mermaid.layout.DrawText
import build.raft.mermaid.layout.LayoutScene
import build.raft.mermaid.layout.ScenePoint
import build.raft.mermaid.layout.TextAnchor
import build.raft.mermaid.layout.TextStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SvgRendererTest {
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
}
