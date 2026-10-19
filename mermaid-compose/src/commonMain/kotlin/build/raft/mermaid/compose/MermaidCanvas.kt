package build.raft.mermaid.compose

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import build.raft.mermaid.layout.DrawCommand
import build.raft.mermaid.layout.DrawEllipse
import build.raft.mermaid.layout.DrawLine
import build.raft.mermaid.layout.DrawPath
import build.raft.mermaid.layout.DrawPolygon
import build.raft.mermaid.layout.DrawPolyline
import build.raft.mermaid.layout.DrawRect
import build.raft.mermaid.layout.DrawText
import build.raft.mermaid.layout.LayoutScene
import build.raft.mermaid.layout.PathArc
import build.raft.mermaid.layout.PathLine
import build.raft.mermaid.layout.PathMove
import build.raft.mermaid.layout.PathQuadratic
import build.raft.mermaid.layout.SceneColor
import build.raft.mermaid.layout.StrokePattern
import build.raft.mermaid.layout.TextAnchor
import kotlin.math.PI

/** Compose Multiplatform Canvas entry point for a platform-neutral Mermaid scene. */
@Composable
public fun MermaidCanvas(
    scene: LayoutScene,
    modifier: Modifier = Modifier,
    scale: Float = 1.0f,
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier) {
        drawMermaidScene(scene, textMeasurer, scale)
    }
}

/** Draws [scene] into the receiver's Compose [DrawScope], measuring text with [textMeasurer]. */
public fun DrawScope.drawMermaidScene(scene: LayoutScene, textMeasurer: TextMeasurer, scale: Float = 1.0f) {
    if (scale <= 0f) return
    withTransform(
        transformBlock = { scale(scale, scale, pivot = Offset.Zero) },
        drawBlock = { scene.commands.forEach { drawCommand(it, textMeasurer) } },
    )
}

private fun DrawScope.drawCommand(command: DrawCommand, textMeasurer: TextMeasurer) {
    when (command) {
        is DrawRect -> drawRect(command)
        is DrawEllipse -> drawEllipse(command)
        is DrawLine -> drawLine(command)
        is DrawPolyline -> drawPolyline(command)
        is DrawPath -> drawPath(command)
        is DrawPolygon -> drawPolygon(command)
        is DrawText -> drawText(command, textMeasurer)
    }
}

private fun DrawScope.drawRect(command: DrawRect) {
    val fill = command.fill.composeColor()
    val stroke = command.stroke.composeColor()
    if (fill == null && (stroke == null || command.strokeWidth <= 0)) return
    val topLeft = Offset(command.rect.x.toFloat(), command.rect.y.toFloat())
    val size = Size(command.rect.width.toFloat(), command.rect.height.toFloat())
    val radius = command.cornerRadius.toFloat().coerceAtLeast(0f)
    // Match the core Canvas renderer: an oversized radius falls back to a
    // regular rectangle instead of allowing the platform canvas to choose a
    // different corner-clamping policy.
    val rounded = radius > 0f && radius <= size.width / 2f && radius <= size.height / 2f
    fun drawShape(color: Color, style: DrawStyle) {
        if (rounded) drawRoundRect(color, topLeft, size, CornerRadius(radius, radius), style = style)
        else drawRect(color, topLeft, size, style = style)
    }
    fill?.let { drawShape(it, Fill) }
    if (stroke != null && command.strokeWidth > 0) drawShape(stroke, Stroke(command.strokeWidth.toFloat()))
}

private fun DrawScope.drawEllipse(command: DrawEllipse) {
    if (command.radiusX <= 0 || command.radiusY <= 0) return
    val fill = command.fill.composeColor(command.fillOpacity.toFloat())
    val stroke = command.stroke.composeColor()
    val topLeft = Offset((command.center.x - command.radiusX).toFloat(), (command.center.y - command.radiusY).toFloat())
    val size = Size((command.radiusX * 2).toFloat(), (command.radiusY * 2).toFloat())
    if (fill != null) drawOval(fill, topLeft, size)
    if (stroke != null && command.strokeWidth > 0) drawOval(stroke, topLeft, size, style = Stroke(command.strokeWidth.toFloat()))
}

private fun DrawScope.drawLine(command: DrawLine) {
    val color = command.stroke.composeColor() ?: return
    if (command.strokeWidth <= 0) return
    drawLine(color, Offset(command.from.x.toFloat(), command.from.y.toFloat()), Offset(command.to.x.toFloat(), command.to.y.toFloat()), command.strokeWidth.toFloat(), pathEffect = command.pattern.pathEffect())
}

private fun DrawScope.drawPolyline(command: DrawPolyline) {
    val color = command.stroke.composeColor() ?: return
    if (command.points.size < 2 || command.strokeWidth <= 0) return
    val path = Path().apply {
        moveTo(command.points.first().x.toFloat(), command.points.first().y.toFloat())
        command.points.drop(1).forEach { lineTo(it.x.toFloat(), it.y.toFloat()) }
    }
    drawPath(path, color, style = Stroke(command.strokeWidth.toFloat(), pathEffect = command.pattern.pathEffect()))
}

private fun DrawScope.drawPath(command: DrawPath) {
    val color = command.stroke.composeColor() ?: return
    if (command.segments.isEmpty() || command.strokeWidth <= 0) return
    val path = Path().apply {
        command.segments.forEach { segment ->
            when (segment) {
                is PathMove -> moveTo(segment.to.x.toFloat(), segment.to.y.toFloat())
                is PathLine -> lineTo(segment.to.x.toFloat(), segment.to.y.toFloat())
                is PathQuadratic -> quadraticBezierTo(segment.control.x.toFloat(), segment.control.y.toFloat(), segment.to.x.toFloat(), segment.to.y.toFloat())
                is PathArc -> {
                    lineTo(segment.start.x.toFloat(), segment.start.y.toFloat())
                    arcTo(
                        Rect((segment.center.x - segment.radius).toFloat(), (segment.center.y - segment.radius).toFloat(), (segment.center.x + segment.radius).toFloat(), (segment.center.y + segment.radius).toFloat()),
                        segment.startAngle.toFloat() * 180f / PI.toFloat(),
                        segment.sweepAngle.toFloat() * 180f / PI.toFloat(),
                        forceMoveTo = false,
                    )
                }
            }
        }
    }
    drawPath(path, color, style = Stroke(command.strokeWidth.toFloat(), pathEffect = command.pattern.pathEffect()))
}

private fun DrawScope.drawPolygon(command: DrawPolygon) {
    val fill = command.fill.composeColor() ?: return
    if (command.points.size < 3) return
    val path = Path().apply {
        moveTo(command.points.first().x.toFloat(), command.points.first().y.toFloat())
        command.points.drop(1).forEach { lineTo(it.x.toFloat(), it.y.toFloat()) }
        close()
    }
    val gradient = command.gradient
    if (gradient == null) {
        drawPath(path, fill)
    } else {
        drawPath(path, Brush.linearGradient(listOf(gradient.startColor.composeColor(gradient.opacity.toFloat()) ?: fill, gradient.endColor.composeColor(gradient.opacity.toFloat()) ?: fill), Offset(gradient.from.x.toFloat(), gradient.from.y.toFloat()), Offset(gradient.to.x.toFloat(), gradient.to.y.toFloat())))
    }
}

private fun DrawScope.drawText(command: DrawText, textMeasurer: TextMeasurer) {
    if (command.text.isEmpty()) return
    val family = when (command.style.fontFamily.lowercase()) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }
    val layout = textMeasurer.measure(command.text, TextStyle(
        color = command.style.color.composeColor() ?: return,
        fontSize = command.style.fontSize.toFloat().sp,
        fontWeight = FontWeight(command.style.fontWeight.coerceIn(1, 1000)),
        fontStyle = if (command.style.italic) FontStyle.Italic else FontStyle.Normal,
        fontFamily = family,
    ))
    // The scene places text like SVG: the origin is on the first baseline and
    // the anchor picks which horizontal edge (or the center) sits on it.
    val width = layout.size.width.toFloat()
    val x = command.origin.x.toFloat() - when (command.anchor) {
        TextAnchor.START -> 0f
        TextAnchor.MIDDLE -> width / 2f
        TextAnchor.END -> width
    }
    drawText(layout, topLeft = Offset(x, command.origin.y.toFloat() - layout.firstBaseline))
}

private fun StrokePattern.pathEffect(): PathEffect? = when (this) {
    StrokePattern.SOLID -> null
    StrokePattern.DASHED -> PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
}

private fun SceneColor.composeColor(alphaMultiplier: Float = 1f): Color? {
    val value = value.trim()
    if (value.equals("none", ignoreCase = true)) return null
    if (!value.startsWith("#")) return Color.Black
    val hex = value.drop(1)
    val expanded = when (hex.length) {
        3 -> hex.map { "$it$it" }.joinToString("") + "ff"
        4 -> hex.map { "$it$it" }.joinToString("")
        6 -> hex + "ff"
        8 -> hex
        else -> return Color.Black
    }
    val r = expanded.substring(0, 2).toInt(16)
    val g = expanded.substring(2, 4).toInt(16)
    val b = expanded.substring(4, 6).toInt(16)
    val a = (expanded.substring(6, 8).toInt(16) / 255f * alphaMultiplier.coerceIn(0f, 1f)).coerceIn(0f, 1f)
    return Color(r, g, b, (a * 255f).toInt())
}
