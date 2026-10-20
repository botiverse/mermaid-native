package build.raft.mermaid.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
import build.raft.mermaid.layout.TextStyle as SceneTextStyle
import kotlin.math.PI

/**
 * Compose Multiplatform Canvas entry point for a platform-neutral Mermaid scene.
 *
 * One scene unit is drawn as one dp multiplied by [scale]. The canvas sizes itself to
 * `scene.width × scene.height × scale` dp (after [mermaidSafeScale] keeps the largest side within
 * [MermaidMaxSidePx]); size modifiers in [modifier] still take precedence. Scene text uses
 * [textStyle] as its base style (see [measureMermaidScene]). Text is measured once per
 * scene, text measurer and style, not on every frame. When the scene has an accessibility title
 * or description, it becomes the canvas content description.
 */
@Composable
public fun MermaidCanvas(
    scene: LayoutScene,
    modifier: Modifier = Modifier,
    scale: Float = 1.0f,
    textStyle: TextStyle = TextStyle.Default,
) {
    val textMeasurer = rememberTextMeasurer()
    val measured = remember(scene, textMeasurer, textStyle) { measureMermaidScene(scene, textMeasurer, textStyle) }
    val density = LocalDensity.current.density
    val drawScale = mermaidSafeScale(scene.width.toFloat(), scene.height.toFloat(), scale, density)
    val description = listOf(scene.accessibilityTitle, scene.accessibilityDescription).firstOrNull { !it.isNullOrBlank() }
    Canvas(
        modifier
            .width((scene.width.toFloat() * drawScale).dp)
            .height((scene.height.toFloat() * drawScale).dp)
            .then(if (description == null) Modifier else Modifier.semantics { contentDescription = description }),
    ) {
        drawMermaidScene(measured, this.density * drawScale)
    }
}

/**
 * Largest size in pixels that [MermaidCanvas] gives either side of a diagram. Compose packs a
 * node's constraints into a single Long. Sides up to 2^15 - 1 pixels are always representable;
 * larger sizes may not be, and an unrepresentable size throws during layout.
 */
public const val MermaidMaxSidePx: Float = 32_000f

/**
 * Returns [scale] reduced, if necessary, so that a `sceneWidth × sceneHeight` scene drawn at
 * `scale` dp per scene unit with the given pixel [density] stays within [MermaidMaxSidePx] on both
 * sides. Returns 0 when any input is not finite or not positive, or the scene is empty.
 */
public fun mermaidSafeScale(sceneWidth: Float, sceneHeight: Float, scale: Float, density: Float): Float {
    if (!scale.isFinite() || scale <= 0f || !density.isFinite() || density <= 0f) return 0f
    val longestSide = maxOf(sceneWidth, sceneHeight)
    if (!longestSide.isFinite() || longestSide <= 0f) return 0f
    return minOf(scale, MermaidMaxSidePx / (longestSide * density))
}

/**
 * A [LayoutScene] whose text has been measured, ready for [drawMermaidScene]. Create it with
 * [measureMermaidScene] and keep it (for example with `remember`) while the scene, text measurer
 * and base style are unchanged.
 */
public class MeasuredMermaidScene internal constructor(
    public val scene: LayoutScene,
    /** One layout per command, index-aligned; null for non-text commands and unpainted text. */
    internal val texts: List<TextLayoutResult?>,
)

/**
 * Measures the text of [scene] with [textMeasurer], in scene units.
 *
 * Each text command uses [textStyle] merged with the scene's font size, weight, italic and color.
 * The scene's font family applies only when [textStyle] has no font family, so a host can force
 * its own UI font. Line height is 1.2 × font size; text does not wrap and has no background.
 */
public fun measureMermaidScene(
    scene: LayoutScene,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle = TextStyle.Default,
): MeasuredMermaidScene = MeasuredMermaidScene(
    scene,
    scene.commands.map { command ->
        if (command !is DrawText || command.text.isEmpty()) return@map null
        val style = mermaidTextStyle(textStyle, command.style) ?: return@map null
        textMeasurer.measure(
            text = command.text,
            style = style,
            softWrap = false,
            constraints = Constraints(),
            // Scene units: the draw scope is already scaled to pixels.
            density = SceneDensity,
        )
    },
)

private val SceneDensity = Density(1f, 1f)

/**
 * Draws [scene] into the receiver's Compose [DrawScope], measuring text with [textMeasurer]. One
 * scene unit is drawn as [scale] pixels; pass `scale * density` to draw in dp. This measures text
 * on every call; inside a frequently redrawn scope, prefer the [MeasuredMermaidScene] overload.
 */
public fun DrawScope.drawMermaidScene(
    scene: LayoutScene,
    textMeasurer: TextMeasurer,
    scale: Float = 1.0f,
    textStyle: TextStyle = TextStyle.Default,
) {
    if (scale <= 0f || !scale.isFinite()) return
    drawMermaidScene(measureMermaidScene(scene, textMeasurer, textStyle), scale)
}

/** Draws a [measured] scene into the receiver's Compose [DrawScope], one scene unit per [scale] pixels. */
public fun DrawScope.drawMermaidScene(measured: MeasuredMermaidScene, scale: Float = 1.0f) {
    if (scale <= 0f || !scale.isFinite()) return
    withTransform(
        transformBlock = { scale(scale, scale, pivot = Offset.Zero) },
        drawBlock = {
            measured.scene.commands.forEachIndexed { index, command -> drawCommand(command, measured.texts.getOrNull(index)) }
        },
    )
}

/**
 * Host [base] style merged with a scene text [style]; null when the scene color does not paint.
 */
internal fun mermaidTextStyle(base: TextStyle, style: SceneTextStyle): TextStyle? {
    val color = style.color.composeColor() ?: return null
    val fontSize = style.fontSize.toFloat()
    return base.merge(
        TextStyle(
            color = color,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.2f).sp,
            fontWeight = FontWeight(style.fontWeight.coerceIn(1, 1000)),
            fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
            fontFamily = base.fontFamily ?: style.fontFamily.composeFontFamily(),
            textAlign = TextAlign.Start,
        ),
    ).copy(background = Color.Unspecified)
}

private fun String.composeFontFamily(): FontFamily = when (trim().lowercase()) {
    "serif" -> FontFamily.Serif
    "monospace" -> FontFamily.Monospace
    else -> FontFamily.SansSerif
}

private fun DrawScope.drawCommand(command: DrawCommand, text: TextLayoutResult?) {
    when (command) {
        is DrawRect -> drawRect(command)
        is DrawEllipse -> drawEllipse(command)
        is DrawLine -> drawLine(command)
        is DrawPolyline -> drawPolyline(command)
        is DrawPath -> drawPath(command)
        is DrawPolygon -> drawPolygon(command)
        is DrawText -> if (text != null) drawText(command, text)
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
    if (command.points.size < 3) return
    val fill = command.fill.composeColor()
    val gradient = command.gradient
    // A gradient paints even when the flat fill is `none`, like the Raft app renderer.
    if (fill == null && gradient == null) return
    val path = Path().apply {
        moveTo(command.points.first().x.toFloat(), command.points.first().y.toFloat())
        command.points.drop(1).forEach { lineTo(it.x.toFloat(), it.y.toFloat()) }
        close()
    }
    if (gradient == null) {
        drawPath(path, fill ?: return)
    } else {
        val opacity = gradient.opacity.toFloat()
        val fallback = fill ?: Color.Transparent
        drawPath(path, Brush.linearGradient(listOf(gradient.startColor.composeColor(opacity) ?: fallback, gradient.endColor.composeColor(opacity) ?: fallback), Offset(gradient.from.x.toFloat(), gradient.from.y.toFloat()), Offset(gradient.to.x.toFloat(), gradient.to.y.toFloat())))
    }
}

private fun DrawScope.drawText(command: DrawText, layout: TextLayoutResult) {
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

/**
 * Parses a scene color: `#rgb`, `#rgba`, `#rrggbb` or `#rrggbbaa`, with the alpha multiplied by
 * [alphaMultiplier]. `none` and anything unparseable mean "do not paint" and return null.
 */
internal fun SceneColor.composeColor(alphaMultiplier: Float = 1f): Color? {
    val value = value.trim()
    if (!value.startsWith("#")) return null
    val hex = value.substring(1)
    val expanded = when (hex.length) {
        3 -> hex.map { "$it$it" }.joinToString("") + "ff"
        4 -> hex.map { "$it$it" }.joinToString("")
        6 -> hex + "ff"
        8 -> hex
        else -> return null
    }
    if (!expanded.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
    val r = expanded.substring(0, 2).toInt(16)
    val g = expanded.substring(2, 4).toInt(16)
    val b = expanded.substring(4, 6).toInt(16)
    val multiplier = if (alphaMultiplier.isNaN()) 0f else alphaMultiplier.coerceIn(0f, 1f)
    val a = expanded.substring(6, 8).toInt(16) / 255f * multiplier
    return Color(r, g, b, (a * 255f).toInt().coerceIn(0, 255))
}
