package build.raft.mermaid.layout.simple

import build.raft.mermaid.layout.*
import kotlin.math.abs

/** Mermaid blockArrow.ts geometry (MIT); coordinates preserve the upstream polygon contract.
 * Layout and measurement stay in the host-independent Native scene pipeline.
 */
public object BlockArrowGeometry {
    /** Raw upstream coordinates: x grows right; negative y values extend above the bottom edge. */
    public fun points(directions: List<String>, label: SceneSize, nodePadding: Double = 0.0, totalWidth: Double? = null): List<ScenePoint> {
        val expanded = directions.flatMap { when (it) { "x" -> listOf("right", "left"); "y" -> listOf("up", "down"); else -> listOf(it) } }.toSet()
        val key = listOf("right", "left", "up", "down").filter { it in expanded }.joinToString("|")
        val padding = nodePadding / 2.0
        val height = label.height + 4.0 * padding
        val midpoint = height / 2.0
        val width = totalWidth ?: (label.width + 2.0 * midpoint + 2.0 * padding)
        return when (key) {
            "right|left|up|down" -> listOf(
                ScenePoint(0.0, 0.0),
                ScenePoint(midpoint, 0.0),
                ScenePoint(width / 2.0, 2.0 * padding),
                ScenePoint(width - midpoint, 0.0),
                ScenePoint(width, 0.0),
                ScenePoint(width, -height / 3.0),
                ScenePoint(width + 2.0 * padding, -height / 2.0),
                ScenePoint(width, (-2.0 * height) / 3.0),
                ScenePoint(width, -height),
                ScenePoint(width - midpoint, -height),
                ScenePoint(width / 2.0, -height - 2.0 * padding),
                ScenePoint(midpoint, -height),
                ScenePoint(0.0, -height),
                ScenePoint(0.0, (-2.0 * height) / 3.0),
                ScenePoint(-2.0 * padding, -height / 2.0),
                ScenePoint(0.0, -height / 3.0),
            )
            "right|left|up" -> listOf(
                ScenePoint(midpoint, 0.0),
                ScenePoint(width - midpoint, 0.0),
                ScenePoint(width, -height / 2.0),
                ScenePoint(width - midpoint, -height),
                ScenePoint(midpoint, -height),
                ScenePoint(0.0, -height / 2.0),
            )
            "right|left|down" -> listOf(
                ScenePoint(0.0, 0.0),
                ScenePoint(midpoint, -height),
                ScenePoint(width - midpoint, -height),
                ScenePoint(width, 0.0),
            )
            "right|up|down" -> listOf(
                ScenePoint(0.0, 0.0),
                ScenePoint(width, -midpoint),
                ScenePoint(width, -height + midpoint),
                ScenePoint(0.0, -height),
            )
            "left|up|down" -> listOf(
                ScenePoint(width, 0.0),
                ScenePoint(0.0, -midpoint),
                ScenePoint(0.0, -height + midpoint),
                ScenePoint(width, -height),
            )
            "right|left" -> listOf(
                ScenePoint(midpoint, 0.0),
                ScenePoint(midpoint, -padding),
                ScenePoint(width - midpoint, -padding),
                ScenePoint(width - midpoint, 0.0),
                ScenePoint(width, -height / 2.0),
                ScenePoint(width - midpoint, -height),
                ScenePoint(width - midpoint, -height + padding),
                ScenePoint(midpoint, -height + padding),
                ScenePoint(midpoint, -height),
                ScenePoint(0.0, -height / 2.0),
            )
            "up|down" -> listOf(
                ScenePoint(width / 2.0, 0.0),
                ScenePoint(0.0, -padding),
                ScenePoint(midpoint, -padding),
                ScenePoint(midpoint, -height + padding),
                ScenePoint(0.0, -height + padding),
                ScenePoint(width / 2.0, -height),
                ScenePoint(width, -height + padding),
                ScenePoint(width - midpoint, -height + padding),
                ScenePoint(width - midpoint, -padding),
                ScenePoint(width, -padding),
            )
            "right|up" -> listOf(
                ScenePoint(0.0, 0.0),
                ScenePoint(width, -midpoint),
                ScenePoint(0.0, -height),
            )
            "right|down" -> listOf(
                ScenePoint(0.0, 0.0),
                ScenePoint(width, 0.0),
                ScenePoint(0.0, -height),
            )
            "left|up" -> listOf(
                ScenePoint(width, 0.0),
                ScenePoint(0.0, -midpoint),
                ScenePoint(width, -height),
            )
            "left|down" -> listOf(
                ScenePoint(width, 0.0),
                ScenePoint(0.0, 0.0),
                ScenePoint(width, -height),
            )
            "right" -> listOf(
                ScenePoint(midpoint, -padding),
                ScenePoint(midpoint, -padding),
                ScenePoint(width - midpoint, -padding),
                ScenePoint(width - midpoint, 0.0),
                ScenePoint(width, -height / 2.0),
                ScenePoint(width - midpoint, -height),
                ScenePoint(width - midpoint, -height + padding),
                ScenePoint(midpoint, -height + padding),
                ScenePoint(midpoint, -height + padding),
            )
            "left" -> listOf(
                ScenePoint(midpoint, 0.0),
                ScenePoint(midpoint, -padding),
                ScenePoint(width - midpoint, -padding),
                ScenePoint(width - midpoint, -height + padding),
                ScenePoint(midpoint, -height + padding),
                ScenePoint(midpoint, -height),
                ScenePoint(0.0, -height / 2.0),
            )
            "up" -> listOf(
                ScenePoint(midpoint, -padding),
                ScenePoint(midpoint, -height + padding),
                ScenePoint(0.0, -height + padding),
                ScenePoint(width / 2.0, -height),
                ScenePoint(width, -height + padding),
                ScenePoint(width - midpoint, -height + padding),
                ScenePoint(width - midpoint, -padding),
            )
            "down" -> listOf(
                ScenePoint(width / 2.0, 0.0),
                ScenePoint(0.0, -padding),
                ScenePoint(midpoint, -padding),
                ScenePoint(midpoint, -height + padding),
                ScenePoint(width - midpoint, -height + padding),
                ScenePoint(width - midpoint, -padding),
                ScenePoint(width, -padding),
            )
            else -> listOf(ScenePoint(0.0, 0.0))
        }
    }

    public data class Shape(val width: Double, val height: Double, val points: List<ScenePoint>) {
        /** Ink bounds relative to the label's center, including four-way arrow tips. */
        public val bounds: SceneRect get() {
            val left = points.minOf { it.x }; val top = points.minOf { it.y }
            return SceneRect(left, top, points.maxOf { it.x } - left, points.maxOf { it.y } - top)
        }
    }

    /** Mirrors block_arrow width selection and insertPolygonShape's translation. */
    public fun shape(directions: List<String>, label: SceneSize, nodePadding: Double = 8.0,
                     positioned: Boolean = false, widthInColumns: Int = 1, nodeWidth: Double = 0.0): Shape {
        val height = label.height + 2.0 * nodePadding
        val naturalWidth = label.width + height + nodePadding
        val width = if (positioned && widthInColumns > 1 && nodeWidth > naturalWidth) nodeWidth else naturalWidth
        return Shape(width, height, points(directions, label, nodePadding, width).map { ScenePoint(it.x - width / 2.0, it.y + height / 2.0) })
    }

    /** Ray exit on the painted polygon; avoids treating concave arrow shoulders as a rectangle. */
    internal fun intersect(points: List<ScenePoint>, center: ScenePoint, target: ScenePoint): ScenePoint {
        val dx = target.x - center.x; val dy = target.y - center.y
        var exit = -1.0
        points.indices.forEach { i ->
            val a = points[i]; val b = points[(i + 1) % points.size]
            val ex = b.x - a.x; val ey = b.y - a.y
            val denominator = dx * ey - dy * ex
            if (abs(denominator) > 1e-10) {
                val ax = a.x - center.x; val ay = a.y - center.y
                val t = (ax * ey - ay * ex) / denominator
                val u = (ax * dy - ay * dx) / denominator
                if (t >= -1e-10 && u >= -1e-10 && u <= 1.0 + 1e-10 && t > exit) exit = t
            }
        }
        return if (exit < 0.0) center else ScenePoint(center.x + exit * dx, center.y + exit * dy)
    }
}
