package build.raft.mermaid.web

import kotlin.test.Test
import kotlin.test.assertContains

class MermaidWebWasmExportsTest {
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
