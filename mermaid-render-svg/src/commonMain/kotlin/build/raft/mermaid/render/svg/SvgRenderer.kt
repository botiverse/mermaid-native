package build.raft.mermaid.render.svg

import build.raft.mermaid.layout.*
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

/** Deterministic, markup-safe serializer for a platform-neutral [LayoutScene]. */
public object SvgRenderer {
    public fun render(scene: LayoutScene): String = render(
        scene,
        baseId = "mermaid-" + (scene.accessibilityTitle.orEmpty() + "\u0000" + scene.accessibilityDescription.orEmpty()).hashCode().toUInt().toString(16) +
            if (scene.commands.any { it is DrawPolygon && it.gradient != null }) "-" + scene.commands.filterIsInstance<DrawPolygon>().map { it.gradient }.toString().hashCode().toUInt().toString(16) else "",
    )

    /**
     * [baseId] scopes title/description IDs. Hosts composing multiple inline SVGs
     * should provide a distinct ID per diagram instance. The one-argument overload
     * keeps deterministic IDs derived from the accessible text for standalone SVGs.
     * [diagramType] is an optional human-readable role description.
     */
    public fun render(scene: LayoutScene, baseId: String, diagramType: String = ""): String {
        require(baseId.isNotEmpty() && baseId.none { it.isWhitespace() }) { "SVG base ID must be nonempty and contain no whitespace" }
        val title = scene.accessibilityTitle?.takeIf { it.isNotEmpty() }
        val description = scene.accessibilityDescription?.takeIf { it.isNotEmpty() }
        return buildString {
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
            append(scene.width.svgNumber())
            append("\" height=\"")
            append(scene.height.svgNumber())
            append("\" viewBox=\"0 0 ")
            append(scene.width.svgNumber())
            append(' ')
            append(scene.height.svgNumber())
            append("\" role=\"graphics-document document\"")
            if (diagramType.isNotEmpty()) append(" aria-roledescription=\"").append(diagramType.escapeXml()).append('"')
            if (title != null) append(" aria-labelledby=\"chart-title-").append(baseId.escapeXml()).append('"')
            if (description != null) append(" aria-describedby=\"chart-desc-").append(baseId.escapeXml()).append('"')
            append(">\n")
            title?.let { append("  <title id=\"chart-title-").append(baseId.escapeXml()).append("\">").append(it.escapeXml()).append("</title>\n") }
            description?.let { append("  <desc id=\"chart-desc-").append(baseId.escapeXml()).append("\">").append(it.escapeXml()).append("</desc>\n") }
            scene.commands.forEachIndexed { index, command ->
                append("  ")
                append(command.toSvg("$baseId-gradient-$index"))
                append('\n')
            }
            scene.links.forEach { link ->
                append("  <a href=\"").append(link.url.escapeXml()).append("\" target=\"_blank\" rel=\"noopener noreferrer\" aria-label=\"").append(link.label.escapeXml()).append("\">")
                append("<rect x=\"").append(link.rect.x.svgNumber()).append("\" y=\"").append(link.rect.y.svgNumber())
                append("\" width=\"").append(link.rect.width.svgNumber()).append("\" height=\"").append(link.rect.height.svgNumber())
                append("\" fill=\"transparent\" pointer-events=\"all\"/></a>\n")
            }
            append("</svg>\n")
        }
    }
}

private fun DrawCommand.toSvg(gradientId: String): String = when (this) {
    is DrawRect -> buildString {
        append("<rect x=\"${rect.x.svgNumber()}\" y=\"${rect.y.svgNumber()}\"")
        append(" width=\"${rect.width.svgNumber()}\" height=\"${rect.height.svgNumber()}\"")
        append(" rx=\"${cornerRadius.svgNumber()}\" fill=\"${fill.value.escapeXml()}\"")
        append(" stroke=\"${stroke.value.escapeXml()}\" stroke-width=\"${strokeWidth.svgNumber()}\"/>")
    }
    is DrawEllipse -> buildString {
        append("<ellipse cx=\"").append(center.x.svgNumber()).append("\" cy=\"").append(center.y.svgNumber())
        append("\" rx=\"").append(radiusX.svgNumber()).append("\" ry=\"").append(radiusY.svgNumber())
        append("\" fill=\"").append(fill.value.escapeXml()).append("\" fill-opacity=\"").append(fillOpacity.svgNumber())
        append("\" stroke=\"").append(stroke.value.escapeXml()).append("\" stroke-width=\"").append(strokeWidth.svgNumber()).append("\"/>")
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
    is DrawPath -> buildString {
        val path = segments.joinToString(" ") { segment ->
            fun point(p: ScenePoint): String = "${p.x.svgNumber()},${p.y.svgNumber()}"
            when (segment) {
                is PathMove -> "M${point(segment.to)}"
                is PathLine -> "L${point(segment.to)}"
                is PathQuadratic -> "Q${point(segment.control)} ${point(segment.to)}"
                is PathArc -> "L${point(segment.start)}" + if (segment.sweepAngle == 0.0) "" else
                    " A${segment.radius.svgNumber()},${segment.radius.svgNumber()} 0 0 ${if (segment.sweepAngle > 0) 1 else 0} ${point(segment.end)}"
            }
        }
        append("<path d=\"${path.escapeXml()}\" stroke=\"${stroke.value.escapeXml()}\" stroke-width=\"${strokeWidth.svgNumber()}\"")
        appendPattern(pattern)
        append(" fill=\"none\"/>")
    }
    is DrawPolygon -> buildString {
        val serializedPoints = points.joinToString(" ") { "${it.x.svgNumber()},${it.y.svgNumber()}" }
        val g=gradient
        if(g!=null) {
            append("<defs><linearGradient id=\"${gradientId.escapeXml()}\" gradientUnits=\"userSpaceOnUse\" x1=\"${g.from.x.svgNumber()}\" y1=\"${g.from.y.svgNumber()}\" x2=\"${g.to.x.svgNumber()}\" y2=\"${g.to.y.svgNumber()}\">")
            append("<stop offset=\"0\" stop-color=\"${g.startColor.value.escapeXml()}\"/><stop offset=\"1\" stop-color=\"${g.endColor.value.escapeXml()}\"/></linearGradient></defs>")
            append("<polygon points=\"$serializedPoints\" fill=\"url(#${gradientId.escapeXml()})\" fill-opacity=\"${g.opacity.svgNumber()}\"/>")
        } else append("<polygon points=\"$serializedPoints\" fill=\"${fill.value.escapeXml()}\"/>")
    }
    is DrawText -> buildString {
        append("<text x=\"${origin.x.svgNumber()}\" y=\"${origin.y.svgNumber()}\"")
        append(" text-anchor=\"${anchor.svgName()}\" font-family=\"${style.fontFamily.escapeXml()}\"")
        append(" font-size=\"${style.fontSize.svgNumber()}\" font-weight=\"${style.fontWeight}\"")
        if (style.italic) append(" font-style=\"italic\"")
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
