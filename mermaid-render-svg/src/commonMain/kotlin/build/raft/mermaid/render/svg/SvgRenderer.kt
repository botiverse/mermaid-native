package build.raft.mermaid.render.svg

import build.raft.mermaid.layout.DrawCommand
import build.raft.mermaid.layout.DrawLine
import build.raft.mermaid.layout.DrawPolygon
import build.raft.mermaid.layout.DrawPolyline
import build.raft.mermaid.layout.DrawRect
import build.raft.mermaid.layout.DrawText
import build.raft.mermaid.layout.LayoutScene
import build.raft.mermaid.layout.StrokePattern
import build.raft.mermaid.layout.TextAnchor

/** Deterministic, markup-safe serializer for a platform-neutral [LayoutScene]. */
public object SvgRenderer {
    public fun render(scene: LayoutScene): String = buildString {
        append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
        append(scene.width.svgNumber())
        append("\" height=\"")
        append(scene.height.svgNumber())
        append("\" viewBox=\"0 0 ")
        append(scene.width.svgNumber())
        append(' ')
        append(scene.height.svgNumber())
        append("\" role=\"img\">\n")
        scene.commands.forEach { command ->
            append("  ")
            append(command.toSvg())
            append('\n')
        }
        append("</svg>\n")
    }
}

private fun DrawCommand.toSvg(): String = when (this) {
    is DrawRect -> buildString {
        append("<rect x=\"${rect.x.svgNumber()}\" y=\"${rect.y.svgNumber()}\"")
        append(" width=\"${rect.width.svgNumber()}\" height=\"${rect.height.svgNumber()}\"")
        append(" rx=\"${cornerRadius.svgNumber()}\" fill=\"${fill.value.escapeXml()}\"")
        append(" stroke=\"${stroke.value.escapeXml()}\" stroke-width=\"${strokeWidth.svgNumber()}\"/>")
    }
    is DrawLine -> buildString {
        append("<line x1=\"${from.x.svgNumber()}\" y1=\"${from.y.svgNumber()}\"")
        append(" x2=\"${to.x.svgNumber()}\" y2=\"${to.y.svgNumber()}\"")
        append(" stroke=\"${stroke.value.escapeXml()}\" stroke-width=\"${strokeWidth.svgNumber()}\"")
        appendPattern(pattern)
        append(" fill=\"none\"/>")
    }
    is DrawPolyline -> buildString {
        val serializedPoints = points.joinToString(" ") { "${it.x.svgNumber()},${it.y.svgNumber()}" }
        append("<polyline points=\"$serializedPoints\" stroke=\"${stroke.value.escapeXml()}\"")
        append(" stroke-width=\"${strokeWidth.svgNumber()}\"")
        appendPattern(pattern)
        append(" fill=\"none\"/>")
    }
    is DrawPolygon -> buildString {
        val serializedPoints = points.joinToString(" ") { "${it.x.svgNumber()},${it.y.svgNumber()}" }
        append("<polygon points=\"$serializedPoints\" fill=\"${fill.value.escapeXml()}\"/>")
    }
    is DrawText -> buildString {
        append("<text x=\"${origin.x.svgNumber()}\" y=\"${origin.y.svgNumber()}\"")
        append(" text-anchor=\"${anchor.svgName()}\" font-family=\"${style.fontFamily.escapeXml()}\"")
        append(" font-size=\"${style.fontSize.svgNumber()}\" font-weight=\"${style.fontWeight}\"")
        append(" fill=\"${style.color.value.escapeXml()}\">${text.escapeXml()}</text>")
    }
}

private fun StringBuilder.appendPattern(pattern: StrokePattern) {
    if (pattern == StrokePattern.DASHED) append(" stroke-dasharray=\"6 4\"")
}

private fun TextAnchor.svgName(): String = when (this) {
    TextAnchor.START -> "start"
    TextAnchor.MIDDLE -> "middle"
    TextAnchor.END -> "end"
}

private fun Double.svgNumber(): String {
    if (this == 0.0) return "0"
    val integral = toLong()
    if (this == integral.toDouble()) return integral.toString()
    return toString().trimEnd('0').trimEnd('.')
}

private fun String.escapeXml(): String = buildString(length) {
    this@escapeXml.forEach { character ->
        when (character) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            else -> append(character)
        }
    }
}
