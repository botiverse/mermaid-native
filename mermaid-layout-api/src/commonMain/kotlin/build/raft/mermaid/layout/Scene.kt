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
    val color: SceneColor = SceneColor("#111827"),
)

public enum class TextAnchor { START, MIDDLE, END }

public enum class StrokePattern { SOLID, DASHED }

public sealed interface DrawCommand

public data class DrawRect(
    val rect: SceneRect,
    val cornerRadius: Double = 0.0,
    val fill: SceneColor = SceneColor("#ffffff"),
    val stroke: SceneColor = SceneColor("#334155"),
    val strokeWidth: Double = 1.5,
) : DrawCommand

public data class DrawLine(
    val from: ScenePoint,
    val to: ScenePoint,
    val stroke: SceneColor = SceneColor("#475569"),
    val strokeWidth: Double = 1.5,
    val pattern: StrokePattern = StrokePattern.SOLID,
) : DrawCommand

public data class DrawPolyline(
    val points: List<ScenePoint>,
    val stroke: SceneColor = SceneColor("#475569"),
    val strokeWidth: Double = 1.5,
    val pattern: StrokePattern = StrokePattern.SOLID,
) : DrawCommand

public data class DrawPolygon(
    val points: List<ScenePoint>,
    val fill: SceneColor = SceneColor("#475569"),
) : DrawCommand

public data class DrawText(
    val text: String,
    val origin: ScenePoint,
    val anchor: TextAnchor = TextAnchor.START,
    val style: TextStyle = TextStyle(),
) : DrawCommand

public data class LayoutScene(
    val width: Double,
    val height: Double,
    val commands: List<DrawCommand>,
)

public fun interface TextMeasurer {
    public fun measure(text: String, style: TextStyle): SceneSize
}

public data class LayoutConfig(
    val padding: Double = 24.0,
    val nodeGap: Double = 56.0,
    val messageGap: Double = 56.0,
)

public fun interface DiagramLayout {
    public fun layout(
        diagram: MermaidDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene
}
