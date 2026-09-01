package build.raft.mermaid.web

import build.raft.mermaid.testkit.MermaidExamples
import kotlin.test.Test
import kotlin.test.assertContains

class MermaidWebWasmExportsTest {
    @Test
    fun exportedJsonRendersEveryPositiveGalleryFixture() {
        MermaidExamples.all.forEach { example ->
            val result = renderMermaidResultJson(example.source)
            assertContains(result, "\"ok\":true", message = example.path)
            assertContains(result, "\"svg\":\"", message = example.path)
        }
    }

    @Test
    fun exportedJsonPreservesSuccessAndDiagnosticShape() {
        val success = renderMermaidResultJson("flowchart TD\nA[Start] --> B[End]")
        assertContains(success, "\"ok\":true")
        assertContains(success, "\"svg\":\"")

        val failure = renderMermaidResultJson("not-a-diagram")
        assertContains(failure, "\"ok\":false")
        assertContains(failure, "\"line\":1")
        assertContains(failure, "\"column\":1")
    }
}
