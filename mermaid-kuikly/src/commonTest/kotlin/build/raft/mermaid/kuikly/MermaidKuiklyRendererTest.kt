package build.raft.mermaid.kuikly

import build.raft.mermaid.layout.DrawEllipse
import build.raft.mermaid.layout.DrawLine
import build.raft.mermaid.layout.DrawPolygon
import build.raft.mermaid.layout.DrawPolyline
import build.raft.mermaid.layout.DrawRect
import build.raft.mermaid.layout.DrawText
import build.raft.mermaid.layout.LayoutScene
import build.raft.mermaid.layout.SceneColor
import build.raft.mermaid.layout.ScenePoint
import build.raft.mermaid.layout.SceneRect
import build.raft.mermaid.layout.StrokePattern
import build.raft.mermaid.layout.TextAnchor
import build.raft.mermaid.layout.TextStyle
import build.raft.mermaid.layout.LayoutConfig
import build.raft.mermaid.layout.simple.FixedWidthTextMeasurer
import build.raft.mermaid.layout.simple.SimpleMermaidLayout
import build.raft.mermaid.testkit.MermaidExamples
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MermaidKuiklyRendererTest {

    @Test
    fun testParseSceneColorHexFormats() {
        val hex6 = MermaidKuiklyRenderer.parseSceneColor(SceneColor("#ff0000"))
        assertNotNull(hex6)
        assertEquals(0xffff0000L, hex6.hexColor)

        val hex3 = MermaidKuiklyRenderer.parseSceneColor(SceneColor("#0f0"))
        assertNotNull(hex3)
        assertEquals(0xff00ff00L, hex3.hexColor)

        val hex8 = MermaidKuiklyRenderer.parseSceneColor(SceneColor("#0000ff80"))
        assertNotNull(hex8)

        val noneColor = MermaidKuiklyRenderer.parseSceneColor(SceneColor("none"))
        assertNull(noneColor)
    }

    @Test
    fun testRendererDrawsPrimitivesOntoMockCanvas() {
        val scene = LayoutScene(
            width = 400.0,
            height = 300.0,
            commands = listOf(
                DrawRect(
                    rect = SceneRect(10.0, 10.0, 100.0, 50.0),
                    cornerRadius = 4.0,
                    fill = SceneColor("#ffffff"),
                    stroke = SceneColor("#334155"),
                    strokeWidth = 1.5,
                ),
                DrawEllipse(
                    center = ScenePoint(200.0, 150.0),
                    radiusX = 40.0,
                    radiusY = 20.0,
                    fill = SceneColor("#e2e8f0"),
                    stroke = SceneColor("#1e293b"),
                ),
                DrawLine(
                    from = ScenePoint(0.0, 0.0),
                    to = ScenePoint(100.0, 100.0),
                    stroke = SceneColor("#64748b"),
                    pattern = StrokePattern.DASHED,
                ),
                DrawPolyline(
                    points = listOf(ScenePoint(10.0, 10.0), ScenePoint(20.0, 30.0), ScenePoint(40.0, 50.0)),
                    stroke = SceneColor("#000000"),
                ),
                DrawPolygon(
                    points = listOf(ScenePoint(0.0, 0.0), ScenePoint(10.0, 0.0), ScenePoint(5.0, 10.0)),
                    fill = SceneColor("#ff0000"),
                ),
                DrawText(
                    text = "Hello Kuikly",
                    origin = ScenePoint(50.0, 50.0),
                    anchor = TextAnchor.MIDDLE,
                    style = TextStyle(fontSize = 16.0, fontWeight = 700),
                )
            )
        )

        val mockContext = MockCanvasContext()
        MermaidKuiklyRenderer.render(
            scene = scene,
            context = mockContext,
            scale = 2.0f,
        )

        assertTrue(mockContext.log.contains("save"))
        assertTrue(mockContext.log.contains("scale(2.0, 2.0)"))
        assertTrue(mockContext.log.contains("restore"))
        assertTrue(mockContext.log.any { it.startsWith("fillText(Hello Kuikly") })
        assertTrue(mockContext.log.contains("fill"))
        assertTrue(mockContext.log.contains("stroke"))
    }

    @Test
    fun testAllMermaidExamplesRenderOntoKuiklyCanvas() {
        val examples = MermaidExamples.all

        assertTrue(examples.size >= 32, "Expected at least 32 canonical diagram examples, got ${examples.size}")

        for (example in examples) {
            val scene = SimpleMermaidLayout.layout(example.expected, FixedWidthTextMeasurer, LayoutConfig())
            assertTrue(scene.width > 0, "Scene width should be positive for ${example.path}")
            assertTrue(scene.height > 0, "Scene height should be positive for ${example.path}")
            assertTrue(scene.commands.isNotEmpty(), "Scene commands should not be empty for ${example.path}")

            val mockContext = MockCanvasContext()
            MermaidKuiklyRenderer.render(
                scene = scene,
                context = mockContext,
                scale = 1.0f,
            )

            assertTrue(mockContext.log.contains("save"))
            assertTrue(mockContext.log.contains("restore"))
            assertTrue(
                mockContext.log.any { it == "fill" || it == "stroke" || it.startsWith("fillText") },
                "Expected drawing operations for ${example.path}"
            )
        }
    }
}
