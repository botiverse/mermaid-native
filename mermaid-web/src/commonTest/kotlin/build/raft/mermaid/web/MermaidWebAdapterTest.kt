package build.raft.mermaid.web

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MermaidWebAdapterTest {
    @Test
    fun supportedSourceProducesDeterministicSvg() {
        val request = MermaidWebRequest("flowchart TD\nA[Start] --> B[End]")
        val first = assertIs<MermaidWebResult.Success>(MermaidWebAdapter.render(request))
        val second = assertIs<MermaidWebResult.Success>(MermaidWebAdapter.render(request))
        assertEquals(first.svg, second.svg)
        assertContains(first.svg, "<svg")
        assertContains(first.svg, "Start")
    }

    @Test
    fun unsupportedSourceReturnsTypedDiagnostics() {
        val result = assertIs<MermaidWebResult.Failure>(MermaidWebAdapter.render(MermaidWebRequest("not-a-diagram")))
        assertEquals(1, result.diagnostics.size)
    }
}
