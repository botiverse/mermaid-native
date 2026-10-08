package build.raft.mermaid.layout

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Stroke-only path. A Move starts a new subpath, preserving intentional gaps. */
public sealed interface ScenePathSegment
public data class PathMove(val to: ScenePoint) : ScenePathSegment
public data class PathLine(val to: ScenePoint) : ScenePathSegment
public data class PathQuadratic(val control: ScenePoint, val to: ScenePoint) : ScenePathSegment

/**
 * Circular arc in the scene's y-down coordinates. Positive sweep is clockwise.
 * Sweeps are bounded to a half circle; larger arcs can be represented by successive arcs.
 * Like Canvas.arc, a line joins the current point to [start] when they differ.
 */
public data class PathArc(
    val center: ScenePoint,
    val radius: Double,
    val startAngle: Double,
    val sweepAngle: Double,
) : ScenePathSegment {
    init {
        require(center.x.isFinite() && center.y.isFinite())
        require(radius.isFinite() && radius > 0.0)
        require(startAngle.isFinite() && sweepAngle.isFinite() && abs(sweepAngle) <= PI)
    }
    public val start: ScenePoint get() = pointAt(startAngle)
    public val end: ScenePoint get() = pointAt(startAngle + sweepAngle)
    private fun pointAt(angle: Double): ScenePoint = ScenePoint(center.x + radius * cos(angle), center.y + radius * sin(angle))
}

public data class DrawPath(
    val segments: List<ScenePathSegment>,
    val stroke: SceneColor = SceneColor(DiagramPalette.SECONDARY),
    val strokeWidth: Double = 1.5,
    val pattern: StrokePattern = StrokePattern.SOLID,
) : DrawCommand {
    init {
        require(segments.isEmpty() || segments.first() is PathMove) { "A path must start with Move" }
        require(strokeWidth.isFinite() && strokeWidth >= 0.0)
        segments.forEach { segment ->
            val points = when (segment) {
                is PathMove -> listOf(segment.to)
                is PathLine -> listOf(segment.to)
                is PathQuadratic -> listOf(segment.control, segment.to)
                is PathArc -> listOf(segment.start, segment.end)
            }
            require(points.all { it.x.isFinite() && it.y.isFinite() }) { "Path coordinates must be finite" }
        }
    }

    public fun translated(dx: Double, dy: Double): DrawPath {
        require(dx.isFinite() && dy.isFinite())
        fun ScenePoint.move(): ScenePoint = ScenePoint(x + dx, y + dy)
        return copy(segments = segments.map { when (it) {
            is PathMove -> it.copy(to = it.to.move())
            is PathLine -> it.copy(to = it.to.move())
            is PathQuadratic -> it.copy(control = it.control.move(), to = it.to.move())
            is PathArc -> it.copy(center = it.center.move())
        } })
    }

    /** Conservative ink bounds: quadratic control hulls and full arc circles, plus half the stroke. */
    public fun conservativeBounds(): SceneRect? {
        if (segments.isEmpty()) return null
        val points = segments.flatMap { when (it) {
            is PathMove -> listOf(it.to)
            is PathLine -> listOf(it.to)
            is PathQuadratic -> listOf(it.control, it.to)
            is PathArc -> listOf(ScenePoint(it.center.x - it.radius, it.center.y - it.radius), ScenePoint(it.center.x + it.radius, it.center.y + it.radius))
        } }
        val half = strokeWidth / 2
        val x = points.minOf { it.x } - half
        val y = points.minOf { it.y } - half
        return SceneRect(x, y, points.maxOf { it.x } + half - x, points.maxOf { it.y } + half - y)
    }
}
