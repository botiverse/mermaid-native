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
    public fun render(scene: LayoutScene, config: SvgRenderConfig = SvgRenderConfig()): String = buildString {
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
            append(command.toSvg(config))
            append('\n')
        }
        append("</svg>\n")
    }
}

/** Host-selected font fallback for text whose glyphs are not in the scene's default family. */
public data class SvgRenderConfig(
    public val cjkFontFamily: String = "Noto Sans CJK SC, PingFang SC, HarmonyOS Sans SC, Microsoft YaHei, sans-serif",
)

private fun DrawCommand.toSvg(config: SvgRenderConfig): String = when (this) {
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
        val family = if (style.fontFamily == "sans-serif" && text.any(Char::isCjk)) {
            config.cjkFontFamily
        } else {
            style.fontFamily
        }
        append(" text-anchor=\"${anchor.svgName()}\" font-family=\"${family.escapeXml()}\"")
        append(" font-size=\"${style.fontSize.svgNumber()}\" font-weight=\"${style.fontWeight}\"")
        append(" fill=\"${style.color.value.escapeXml()}\">${text.escapeXml()}</text>")
    }
}

private fun Char.isCjk(): Boolean = code in 0x3400..0x4DBF || code in 0x4E00..0x9FFF || code in 0xF900..0xFAFF

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
