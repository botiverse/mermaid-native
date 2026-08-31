package build.raft.mermaid.web

import build.raft.mermaid.core.MermaidDiagnosticCode
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

    @Test
    fun radarGallerySourceRendersThroughPublicConsumer() {
        val source = "radar-beta\n  title Team skills\n  axis Docs,Code,UX\n  curve Team{8,7,6}"
        val result = assertIs<MermaidWebResult.Success>(MermaidWebAdapter.render(MermaidWebRequest(source)))

        assertContains(result.svg, "Team skills")
        assertContains(result.svg, "Docs")
        assertContains(result.svg, "Team")
    }

    @Test
    fun unsupportedRadarGallerySeriesFailsClosedWithTypedDiagnostic() {
        val source = "radar-beta\n  title Team skills\n  axis Docs,Code,UX\n  \"Team\": [8,7,6]"
        val result = assertIs<MermaidWebResult.Failure>(MermaidWebAdapter.render(MermaidWebRequest(source)))

        assertEquals(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, result.diagnostics.single().code)
        assertEquals(4, result.diagnostics.single().location.line)
    }

    @Test
    fun swimlaneGallerySourceRendersThroughPublicConsumer() {
        val source = "swimlane-beta LR\n  subgraph Support\n    A[Ticket]\n    B[Resolve]\n  end\n  A --> B"
        val result = assertIs<MermaidWebResult.Success>(MermaidWebAdapter.render(MermaidWebRequest(source)))

        assertContains(result.svg, "Support")
        assertContains(result.svg, "Ticket")
        assertContains(result.svg, "Resolve")
    }

    @Test
    fun unsupportedSwimlaneFlowchartWrapperFailsClosedWithTypedDiagnostic() {
        val source = "flowchart LR\n  subgraph Support\n    A[Ticket] --> B[Resolve]\n  end"
        val result = assertIs<MermaidWebResult.Failure>(MermaidWebAdapter.render(MermaidWebRequest(source)))

        assertEquals(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, result.diagnostics.single().code)
    }

    @Test
    fun paddingAffectsRenderedScene() {
        val source = "flowchart TD\nA[Start] --> B[End]"
        val compact = assertIs<MermaidWebResult.Success>(MermaidWebAdapter.render(MermaidWebRequest(source, MermaidWebLayoutOptions(8.0))))
        val padded = assertIs<MermaidWebResult.Success>(MermaidWebAdapter.render(MermaidWebRequest(source, MermaidWebLayoutOptions(40.0))))
        assertEquals(true, compact.svg != padded.svg)
    }
}
