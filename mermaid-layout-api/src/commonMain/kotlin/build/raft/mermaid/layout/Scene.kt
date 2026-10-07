package build.raft.mermaid.layout

import build.raft.mermaid.core.MermaidDiagram

public data class ScenePoint(val x: Double, val y: Double)

public data class SceneSize(val width: Double, val height: Double)

public data class SceneRect(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)

public data class SceneColor(val value: String)

public data class TextStyle(
    val fontSize: Double = 14.0,
    val fontFamily: String = "sans-serif",
    val fontWeight: Int = 400,
    val color: SceneColor = SceneColor(DiagramPalette.INK),
    val italic: Boolean = false,
)

public enum class TextAnchor { START, MIDDLE, END }

public enum class StrokePattern { SOLID, DASHED }

public sealed interface DrawCommand

public data class DrawRect(
    val rect: SceneRect,
    val cornerRadius: Double = 0.0,
    val fill: SceneColor = SceneColor(DiagramPalette.SURFACE),
    val stroke: SceneColor = SceneColor(DiagramPalette.OUTLINE),
    val strokeWidth: Double = 1.5,
) : DrawCommand

public data class DrawEllipse(
    val center: ScenePoint,
    val radiusX: Double,
    val radiusY: Double,
    val fill: SceneColor = SceneColor(DiagramPalette.SURFACE),
    val fillOpacity: Double = 1.0,
    val stroke: SceneColor = SceneColor(DiagramPalette.OUTLINE),
    val strokeWidth: Double = 1.5,
) : DrawCommand

public data class DrawLine(
    val from: ScenePoint,
    val to: ScenePoint,
    val stroke: SceneColor = SceneColor(DiagramPalette.SECONDARY),
    val strokeWidth: Double = 1.5,
    val pattern: StrokePattern = StrokePattern.SOLID,
) : DrawCommand

public data class DrawPolyline(
    val points: List<ScenePoint>,
    val stroke: SceneColor = SceneColor(DiagramPalette.SECONDARY),
    val strokeWidth: Double = 1.5,
    val pattern: StrokePattern = StrokePattern.SOLID,
) : DrawCommand

/** Two-stop gradient in scene coordinates, shared by SVG and Canvas renderers. */
public data class SceneLinearGradient(
    val from: ScenePoint,
    val to: ScenePoint,
    val startColor: SceneColor,
    val endColor: SceneColor,
    val opacity: Double = 1.0,
)

public data class DrawPolygon(
    val points: List<ScenePoint>,
    val fill: SceneColor = SceneColor(DiagramPalette.SECONDARY),
    val gradient: SceneLinearGradient? = null,
) : DrawCommand

public data class DrawText(
    val text: String,
    val origin: ScenePoint,
    val anchor: TextAnchor = TextAnchor.START,
    val style: TextStyle = TextStyle(),
) : DrawCommand

/** Host-owned navigation target. Rect is in the same coordinates as drawing commands. */
public data class SceneLink(val rect: SceneRect, val url: String, val label: String) {
    init { require(build.raft.mermaid.core.isSafeMermaidLink(url)) { "Scene links require an absolute HTTP(S) URL" } }
}

public data class LayoutScene(
    val width: Double,
    val height: Double,
    val commands: List<DrawCommand>,
    val accessibilityTitle: String? = null,
    val accessibilityDescription: String? = null,
    val layoutValidation: LayoutValidationReport? = null,
    val links: List<SceneLink> = emptyList(),
)

public fun interface TextMeasurer {
    public fun measure(text: String, style: TextStyle): SceneSize
}

public data class LayoutConfig(
    val padding: Double = 24.0,
    val nodeGap: Double = 56.0,
    val messageGap: Double = 56.0,
    val cynefin: CynefinBoundaryConfig = CynefinBoundaryConfig(),
    /** Opt-in quadratic diagnostic for flowchart geometry; never changes rendering. */
    val validateOrthogonalLayout: Boolean = false,
)

public fun interface DiagramLayout {
    public fun layout(
        diagram: MermaidDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene
}
