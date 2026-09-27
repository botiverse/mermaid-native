package build.raft.mermaid.web

import build.raft.mermaid.core.MermaidDiagnostic
import build.raft.mermaid.core.MermaidDiagram
import build.raft.mermaid.core.MermaidParser
import build.raft.mermaid.core.MermaidParseResult
import build.raft.mermaid.layout.LayoutConfig
import build.raft.mermaid.layout.simple.FixedWidthTextMeasurer
import build.raft.mermaid.layout.simple.SimpleMermaidLayout
import build.raft.mermaid.render.svg.SvgRenderer

/** Stable, platform-neutral request accepted by the Web/Wasm consumer. */
public data class MermaidWebRequest(
    val source: String,
    val layout: MermaidWebLayoutOptions = MermaidWebLayoutOptions(),
)

/** Deterministic layout options exposed by the Web adapter. */
public data class MermaidWebLayoutOptions(
    val padding: Double = 24.0,
)

public sealed interface MermaidWebResult {
    public data class Success(
        val svg: String,
        val diagram: MermaidDiagram,
    ) : MermaidWebResult

    public data class Failure(
        val diagnostics: List<MermaidDiagnostic>,
    ) : MermaidWebResult
}

/** Result of the Canvas2D export path; carries a JSON draw-script instead of SVG markup. */
public sealed interface MermaidWebCanvasResult {
    public data class Success(
        val script: String,
        val diagram: MermaidDiagram,
    ) : MermaidWebCanvasResult

    public data class Failure(
        val diagnostics: List<MermaidDiagnostic>,
    ) : MermaidWebCanvasResult
}

/**
 * Browser-safe adapter: parsing, layout and SVG serialization all remain in
 * commonMain. Host DOM, sizing, lifecycle and accessibility are deliberately
 * outside this API.
 */
public object MermaidWebAdapter {
    public fun render(request: MermaidWebRequest): MermaidWebResult = when (
        val parsed = MermaidParser.parse(request.source)
    ) {
        is MermaidParseResult.Failure -> MermaidWebResult.Failure(parsed.diagnostics)
        is MermaidParseResult.Success -> {
            val scene = SimpleMermaidLayout.layout(
                parsed.diagram,
                FixedWidthTextMeasurer,
                LayoutConfig(padding = request.layout.padding),
            )
            MermaidWebResult.Success(
                svg = SvgRenderer.render(scene),
                diagram = parsed.diagram,
            )
        }
    }

    /**
     * Canvas2D companion to [render]: identical parse + layout, but serializes the scene to a
     * JSON draw-script ([MermaidCanvasRenderer]) instead of SVG markup. Same determinism, same
     * failure taxonomy.
     */
    public fun renderCanvas(request: MermaidWebRequest): MermaidWebCanvasResult = when (
        val parsed = MermaidParser.parse(request.source)
    ) {
        is MermaidParseResult.Failure -> MermaidWebCanvasResult.Failure(parsed.diagnostics)
        is MermaidParseResult.Success -> {
            val scene = SimpleMermaidLayout.layout(
                parsed.diagram,
                FixedWidthTextMeasurer,
                LayoutConfig(padding = request.layout.padding),
            )
            MermaidWebCanvasResult.Success(
                script = MermaidCanvasRenderer.render(scene),
                diagram = parsed.diagram,
            )
        }
    }
}
