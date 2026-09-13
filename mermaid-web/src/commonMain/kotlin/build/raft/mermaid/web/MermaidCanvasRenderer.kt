package build.raft.mermaid.web

import build.raft.mermaid.layout.DrawCommand
import build.raft.mermaid.layout.DrawEllipse
import build.raft.mermaid.layout.DrawLine
import build.raft.mermaid.layout.DrawPolygon
import build.raft.mermaid.layout.DrawPolyline
import build.raft.mermaid.layout.DrawRect
import build.raft.mermaid.layout.DrawText
import build.raft.mermaid.layout.LayoutScene
import build.raft.mermaid.layout.StrokePattern
import build.raft.mermaid.layout.TextAnchor

/**
 * Browser-canvas export path (task #364 successor): maps the renderer-agnostic [LayoutScene]
 * to a JSON draw-script the host page applies to a `<canvas>` 2D context.
 *
 * This mirrors the native Kuikly Canvas renderer (`MermaidKuiklyRenderer`): the same
 * `DrawCommand` stream drives both, so the web canvas output is geometrically identical to the
 * SVG and native outputs. Only the emission format differs — a compact, ordered list of
 * `{op, ...}` ops that a ~40-line JS layer replays onto `CanvasRenderingContext2D`.
 *
 * Zero DOM ownership: the Wasm side never touches `document`/`window`; sizing, DPR scaling and
 * accessibility stay with the host app (matching [MermaidWebAdapter]'s boundary).
 */
public object MermaidCanvasRenderer {
    /**
     * Serializes [scene] to a JSON draw-script:
     * `{"width":W,"height":H,"ops":[{"op":"rect",...},...]}`.
     *
     * Op shapes (all coordinates are absolute scene units):
     * - `rect`    `{op,x,y,w,h,r,fill,stroke,sw}`
     * - `ellipse` `{op,cx,cy,rx,ry,fill,stroke,sw,fo}`
     * - `line`    `{op,x1,y1,x2,y2,stroke,sw,dash}`
     * - `polyline`/`polygon` `{op,pts:[x,y,...],stroke|fill,sw,dash}`
     * - `text`    `{op,text,x,y,anchor,size,family,weight,fill}`
     */
    public fun render(scene: LayoutScene): String = buildString {
        append("{\"width\":").append(number(scene.width))
        append(",\"height\":").append(number(scene.height))
        append(",\"ops\":[")
        scene.commands.forEachIndexed { index, command ->
            if (index > 0) append(',')
            append(commandJson(command))
        }
        append("]}")
    }

    private fun commandJson(command: DrawCommand): String = when (command) {
        is DrawRect -> buildString {
            append("{\"op\":\"rect\"")
            append(",\"x\":").append(number(command.rect.x))
            append(",\"y\":").append(number(command.rect.y))
            append(",\"w\":").append(number(command.rect.width))
            append(",\"h\":").append(number(command.rect.height))
            append(",\"r\":").append(number(command.cornerRadius))
            append(",\"fill\":").append(jsonString(command.fill.value))
            append(",\"stroke\":").append(jsonString(command.stroke.value))
            append(",\"sw\":").append(number(command.strokeWidth))
            append('}')
        }
        is DrawEllipse -> buildString {
            append("{\"op\":\"ellipse\"")
            append(",\"cx\":").append(number(command.center.x))
            append(",\"cy\":").append(number(command.center.y))
            append(",\"rx\":").append(number(command.radiusX))
            append(",\"ry\":").append(number(command.radiusY))
            append(",\"fill\":").append(jsonString(command.fill.value))
            append(",\"fo\":").append(number(command.fillOpacity))
            append(",\"stroke\":").append(jsonString(command.stroke.value))
            append(",\"sw\":").append(number(command.strokeWidth))
            append('}')
        }
        is DrawLine -> buildString {
            append("{\"op\":\"line\"")
            append(",\"x1\":").append(number(command.from.x))
            append(",\"y1\":").append(number(command.from.y))
            append(",\"x2\":").append(number(command.to.x))
            append(",\"y2\":").append(number(command.to.y))
            append(",\"stroke\":").append(jsonString(command.stroke.value))
            append(",\"sw\":").append(number(command.strokeWidth))
            append(",\"dash\":").append(command.pattern == StrokePattern.DASHED)
            append('}')
        }
        is DrawPolyline -> buildString {
            append("{\"op\":\"polyline\",\"pts\":").append(points(command.points))
            append(",\"stroke\":").append(jsonString(command.stroke.value))
            append(",\"sw\":").append(number(command.strokeWidth))
            append(",\"dash\":").append(command.pattern == StrokePattern.DASHED)
            append('}')
        }
        is DrawPolygon -> buildString {
            append("{\"op\":\"polygon\",\"pts\":").append(points(command.points))
            append(",\"fill\":").append(jsonString(command.fill.value))
            append('}')
        }
        is DrawText -> buildString {
            append("{\"op\":\"text\",\"text\":").append(jsonString(command.text))
            append(",\"x\":").append(number(command.origin.x))
            append(",\"y\":").append(number(command.origin.y))
            append(",\"anchor\":").append(jsonString(command.anchor.wire()))
            append(",\"size\":").append(number(command.style.fontSize))
            append(",\"family\":").append(jsonString(command.style.fontFamily))
            append(",\"weight\":").append(command.style.fontWeight)
            append(",\"fill\":").append(jsonString(command.style.color.value))
            append('}')
        }
    }

    private fun points(points: List<build.raft.mermaid.layout.ScenePoint>): String =
        points.joinToString(prefix = "[", postfix = "]") { "${number(it.x)},${number(it.y)}" }

    private fun TextAnchor.wire(): String = when (this) {
        TextAnchor.START -> "start"
        TextAnchor.MIDDLE -> "middle"
        TextAnchor.END -> "end"
    }

    private fun number(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        val rounded = kotlin.math.round(value * 1000.0) / 1000.0
        return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
    }

    private fun jsonString(value: String): String = buildString {
        append('"')
        value.forEach { c ->
            when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(c)
            }
        }
        append('"')
    }
}
