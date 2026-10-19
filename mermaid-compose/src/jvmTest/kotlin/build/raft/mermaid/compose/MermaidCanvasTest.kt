package build.raft.mermaid.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.use
import build.raft.mermaid.core.MermaidParseResult
import build.raft.mermaid.core.MermaidParser
import build.raft.mermaid.layout.LayoutConfig
import build.raft.mermaid.layout.LayoutScene
import build.raft.mermaid.layout.simple.FixedWidthTextMeasurer
import build.raft.mermaid.layout.simple.SimpleMermaidLayout
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

@OptIn(ExperimentalComposeUiApi::class)
class MermaidCanvasTest {
    private fun scene(source: String): LayoutScene {
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse(source))
        return SimpleMermaidLayout.layout(parsed.diagram, FixedWidthTextMeasurer, LayoutConfig())
    }

    private fun render(scene: LayoutScene, scale: Float = 1f): Image =
        ImageComposeScene(ceil(scene.width * scale).toInt(), ceil(scene.height * scale).toInt()) {
            MermaidCanvas(scene, Modifier.fillMaxSize().background(Color.White), scale)
        }.use { it.render() }

    private fun Image.inkPixels(): Int {
        val bitmap = org.jetbrains.skia.Bitmap.makeFromImage(this)
        var count = 0
        for (y in 0 until height) for (x in 0 until width) {
            val pixel = bitmap.getColor(x, y)
            if ((pixel and 0xFFFFFF) != 0xFFFFFF) count++
        }
        return count
    }

    @Test fun drawsAFlowchart() {
        val image = render(scene("flowchart LR\nA[Start] --> B{Decide}\nB -->|yes| C[Done]\nB -->|no| A"))
        System.getProperty("mermaid.compose.snapshotDir")?.let { dir ->
            java.io.File(dir).apply { mkdirs() }.resolve("flowchart.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        assertTrue(image.inkPixels() > image.width * image.height / 50, "expected the diagram to cover part of the canvas")
    }

    @Test fun scalesFromTheTopLeftCorner() {
        val scene = scene("flowchart TD\nA[Start] --> B[Finish]")
        val small = render(scene)
        val large = render(scene, scale = 2f)
        val ratio = large.inkPixels().toDouble() / small.inkPixels()
        assertTrue(ratio in 3.0..5.0, "doubling the scale should roughly quadruple the drawn area, was $ratio")
    }
}
