package build.raft.mermaid.kuikly

import build.raft.mermaid.layout.DrawCommand
import build.raft.mermaid.layout.DrawEllipse
import build.raft.mermaid.layout.DrawLine
import build.raft.mermaid.layout.DrawPolygon
import build.raft.mermaid.layout.DrawPolyline
import build.raft.mermaid.layout.DrawRect
import build.raft.mermaid.layout.DrawText
import build.raft.mermaid.layout.LayoutScene
import build.raft.mermaid.layout.SceneColor
import build.raft.mermaid.layout.StrokePattern
import build.raft.mermaid.layout.TextAnchor
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.views.CanvasContext
import com.tencent.kuikly.core.views.ContextApi
import com.tencent.kuikly.core.views.FontStyle
import com.tencent.kuikly.core.views.FontWeight
import com.tencent.kuikly.core.views.TextAlign
import kotlin.math.PI

/**
 * High-performance Kuikly Canvas renderer for platform-neutral Mermaid [LayoutScene].
 *
 * Maps abstract [DrawCommand] layout primitives (rectangles, rounded corners, ellipses,
 * lines, polylines, polygons, and text) directly into Kuikly [ContextApi] drawing operations.
 */
public object MermaidKuiklyRenderer {

    /**
     * Renders a complete [LayoutScene] onto a Kuikly [CanvasContext].
     *
     * @param scene The layout scene containing dimensions and draw commands.
     * @param context The target Kuikly canvas context.
     * @param scale Scaling factor applied to the scene (default 1.0f).
     * @param batchDraw Whether to buffer commands into native batchDraw (default true).
     */
    public fun render(
        scene: LayoutScene,
        context: CanvasContext,
        scale: Float = 1.0f,
        batchDraw: Boolean = true,
    ) {
        context.batchDraw = batchDraw
        render(scene, context as ContextApi, scale)
    }

    /**
     * Renders a complete [LayoutScene] onto any Kuikly [ContextApi].
     *
     * @param scene The layout scene containing dimensions and draw commands.
     * @param context The target Kuikly canvas context API.
     * @param scale Scaling factor applied to the scene (default 1.0f).
     */
    public fun render(
        scene: LayoutScene,
        context: ContextApi,
        scale: Float = 1.0f,
    ) {
        context.save()

        if (scale != 1.0f && scale > 0f) {
            context.scale(scale, scale)
        }

        for (command in scene.commands) {
            renderCommand(command, context)
        }

        context.restore()
    }

    private fun renderCommand(command: DrawCommand, context: ContextApi) {
        when (command) {
            is DrawRect -> renderRect(command, context)
            is DrawEllipse -> renderEllipse(command, context)
            is DrawLine -> renderLine(command, context)
            is DrawPolyline -> renderPolyline(command, context)
            is DrawPolygon -> renderPolygon(command, context)
            is DrawText -> renderText(command, context)
        }
    }

    private fun renderRect(cmd: DrawRect, context: ContextApi) {
        val x = cmd.rect.x.toFloat()
        val y = cmd.rect.y.toFloat()
        val w = cmd.rect.width.toFloat()
        val h = cmd.rect.height.toFloat()
        val r = cmd.cornerRadius.toFloat()

        context.beginPath()
        if (r > 0f && r <= w / 2f && r <= h / 2f) {
            // Draw rounded rectangle with 4 corner arcs
            val pi = PI.toFloat()
            context.moveTo(x + r, y)
            context.lineTo(x + w - r, y)
            context.arc(x + w - r, y + r, r, -pi / 2f, 0f, false)
            context.lineTo(x + w, y + h - r)
            context.arc(x + w - r, y + h - r, r, 0f, pi / 2f, false)
            context.lineTo(x + r, y + h)
            context.arc(x + r, y + h - r, r, pi / 2f, pi, false)
            context.lineTo(x, y + r)
            context.arc(x + r, y + r, r, pi, 3f * pi / 2f, false)
            context.closePath()
        } else {
            // Standard rectangle path
            context.moveTo(x, y)
            context.lineTo(x + w, y)
            context.lineTo(x + w, y + h)
            context.lineTo(x, y + h)
            context.closePath()
        }

        // Fill
        val fillColor = parseSceneColor(cmd.fill)
        if (fillColor != null) {
            context.fillStyle(fillColor)
            context.fill()
        }

        // Stroke
        val strokeColor = parseSceneColor(cmd.stroke)
        if (strokeColor != null && cmd.strokeWidth > 0) {
            context.strokeStyle(strokeColor)
            context.lineWidth(cmd.strokeWidth.toFloat())
            context.setLineDash(emptyList())
            context.stroke()
        }
    }

    private fun renderEllipse(cmd: DrawEllipse, context: ContextApi) {
        val cx = cmd.center.x.toFloat()
        val cy = cmd.center.y.toFloat()
        val rx = cmd.radiusX.toFloat()
        val ry = cmd.radiusY.toFloat()

        if (rx <= 0f || ry <= 0f) return

        context.save()
        context.translate(cx, cy)
        context.scale(1f, ry / rx)

        context.beginPath()
        context.arc(0f, 0f, rx, 0f, (2 * PI).toFloat(), false)
        context.closePath()

        val fillColor = parseSceneColor(cmd.fill, alphaMultiplier = cmd.fillOpacity.toFloat())
        if (fillColor != null) {
            context.fillStyle(fillColor)
            context.fill()
        }

        val strokeColor = parseSceneColor(cmd.stroke)
        if (strokeColor != null && cmd.strokeWidth > 0) {
            context.strokeStyle(strokeColor)
            context.lineWidth(cmd.strokeWidth.toFloat())
            context.setLineDash(emptyList())
            context.stroke()
        }

        context.restore()
    }

    private fun renderLine(cmd: DrawLine, context: ContextApi) {
        val strokeColor = parseSceneColor(cmd.stroke) ?: return
        if (cmd.strokeWidth <= 0) return

        context.beginPath()
        context.moveTo(cmd.from.x.toFloat(), cmd.from.y.toFloat())
        context.lineTo(cmd.to.x.toFloat(), cmd.to.y.toFloat())

        context.strokeStyle(strokeColor)
        context.lineWidth(cmd.strokeWidth.toFloat())
        applyPattern(cmd.pattern, context)
        context.stroke()
    }

    private fun renderPolyline(cmd: DrawPolyline, context: ContextApi) {
        if (cmd.points.isEmpty()) return
        val strokeColor = parseSceneColor(cmd.stroke) ?: return
        if (cmd.strokeWidth <= 0) return

        context.beginPath()
        val first = cmd.points.first()
        context.moveTo(first.x.toFloat(), first.y.toFloat())
        for (i in 1 until cmd.points.size) {
            val pt = cmd.points[i]
            context.lineTo(pt.x.toFloat(), pt.y.toFloat())
        }

        context.strokeStyle(strokeColor)
        context.lineWidth(cmd.strokeWidth.toFloat())
        applyPattern(cmd.pattern, context)
        context.stroke()
    }

    private fun renderPolygon(cmd: DrawPolygon, context: ContextApi) {
        if (cmd.points.isEmpty()) return
        val fillColor = parseSceneColor(cmd.fill) ?: return

        context.beginPath()
        val first = cmd.points.first()
        context.moveTo(first.x.toFloat(), first.y.toFloat())
        for (i in 1 until cmd.points.size) {
            val pt = cmd.points[i]
            context.lineTo(pt.x.toFloat(), pt.y.toFloat())
        }
        context.closePath()

        context.fillStyle(fillColor)
        context.fill()
    }

    private fun renderText(cmd: DrawText, context: ContextApi) {
        val textColor = parseSceneColor(cmd.style.color) ?: return
        if (cmd.text.isEmpty()) return

        val textAlign = when (cmd.anchor) {
            TextAnchor.START -> TextAlign.LEFT
            TextAnchor.MIDDLE -> TextAlign.CENTER
            TextAnchor.END -> TextAlign.RIGHT
        }
        context.textAlign(textAlign)

        val fontWeight = if (cmd.style.fontWeight >= 700) {
            FontWeight.BOLD
        } else {
            FontWeight.NORMAL
        }

        context.font(
            style = FontStyle.NORMAL,
            weight = fontWeight,
            size = cmd.style.fontSize.toFloat(),
            family = cmd.style.fontFamily
        )

        context.fillStyle(textColor)
        context.fillText(cmd.text, cmd.origin.x.toFloat(), cmd.origin.y.toFloat())
    }

    private fun applyPattern(pattern: StrokePattern, context: ContextApi) {
        when (pattern) {
            StrokePattern.SOLID -> context.setLineDash(emptyList())
            StrokePattern.DASHED -> context.setLineDash(listOf(6f, 4f))
        }
    }

    /**
     * Parses CSS/Hex [SceneColor] to Kuikly [Color].
     * Supports #rgb, #rgba, #rrggbb, #rrggbbaa, and 'none'.
     */
    public fun parseSceneColor(sceneColor: SceneColor, alphaMultiplier: Float = 1.0f): Color? {
        val trimmed = sceneColor.value.trim()
        if (trimmed.equals("none", ignoreCase = true)) return null

        if (trimmed.startsWith("#")) {
            val hex = trimmed.substring(1)
            var r = 0
            var g = 0
            var b = 0
            var a = 255

            when (hex.length) {
                3 -> { // #RGB
                    r = hex.substring(0, 1).repeat(2).toInt(16)
                    g = hex.substring(1, 2).repeat(2).toInt(16)
                    b = hex.substring(2, 3).repeat(2).toInt(16)
                }
                4 -> { // #RGBA
                    r = hex.substring(0, 1).repeat(2).toInt(16)
                    g = hex.substring(1, 2).repeat(2).toInt(16)
                    b = hex.substring(2, 3).repeat(2).toInt(16)
                    a = hex.substring(3, 4).repeat(2).toInt(16)
                }
                6 -> { // #RRGGBB
                    r = hex.substring(0, 2).toInt(16)
                    g = hex.substring(2, 4).toInt(16)
                    b = hex.substring(4, 6).toInt(16)
                }
                8 -> { // #RRGGBBAA
                    r = hex.substring(0, 2).toInt(16)
                    g = hex.substring(2, 4).toInt(16)
                    b = hex.substring(4, 6).toInt(16)
                    a = hex.substring(6, 8).toInt(16)
                }
                else -> return Color.BLACK
            }

            val finalAlpha = ((a / 255f) * alphaMultiplier.coerceIn(0f, 1f)).coerceIn(0f, 1f)
            return Color(r, g, b, finalAlpha)
        }

        return Color.BLACK
    }
}
