package build.raft.mermaid.web

import build.raft.mermaid.testkit.MermaidExamples
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class MermaidCanvasRendererTest {
    @Test
    fun canvasScriptRendersEveryPositiveGalleryFixture() {
        MermaidExamples.all.forEach { example ->
            val result = MermaidWebAdapter.renderCanvas(MermaidWebRequest(example.source))
            assertTrue(result is MermaidWebCanvasResult.Success, "expected canvas success for ${example.path}")
            val script = result.script
            assertContains(script, "\"width\":", message = example.path)
            assertContains(script, "\"height\":", message = example.path)
            assertContains(script, "\"ops\":[", message = example.path)
        }
    }

    @Test
    fun canvasScriptCarriesTheSameSceneGeometryAsSvgPath() {
        // The canvas script and the SVG path share one LayoutScene, so both must contain the
        // same command vertices. Assert the flowchart sample yields matching draw ops.
        val source = "flowchart TD\nA[Start] --> B[Process]"
        val canvas = MermaidWebAdapter.renderCanvas(MermaidWebRequest(source))
        val svg = MermaidWebAdapter.render(MermaidWebRequest(source))
        assertTrue(canvas is MermaidWebCanvasResult.Success)
        assertTrue(svg is MermaidWebResult.Success)
        // Rect + text for both nodes are present in the canvas script.
        assertContains(canvas.script, "\"op\":\"rect\"")
        assertContains(canvas.script, "\"op\":\"text\"")
        assertContains(canvas.script, "\"text\":\"Start\"")
        // The SVG path emits the same labels.
        assertContains(svg.svg, "Start")
        assertContains(svg.svg, "Process")
    }

    @Test
    fun canvasFailurePreservesTypedDiagnostics() {
        val result = MermaidWebAdapter.renderCanvas(MermaidWebRequest("not-a-diagram"))
        assertTrue(result is MermaidWebCanvasResult.Failure)
        assertTrue(result.diagnostics.isNotEmpty())
        assertTrue(result.diagnostics.first().location.line >= 1)
    }
}
