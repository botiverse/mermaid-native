package build.raft.mermaid.layout.simple

import build.raft.mermaid.layout.*

/** Spline edges flattened to dense polylines for geometric assertions. */
internal fun DrawPath.samples(steps: Int = 12): List<ScenePoint> {
    val points = mutableListOf<ScenePoint>()
    segments.forEach { segment ->
        when (segment) {
            is PathMove -> points += segment.to
            is PathLine -> points += segment.to
            is PathQuadratic -> {
                val start = points.last()
                for (i in 1..steps) {
                    val t = i.toDouble() / steps; val u = 1 - t
                    points += ScenePoint(u * u * start.x + 2 * u * t * segment.control.x + t * t * segment.to.x,
                        u * u * start.y + 2 * u * t * segment.control.y + t * t * segment.to.y)
                }
            }
            is PathArc -> { points += segment.start; points += segment.end }
        }
    }
    return points
}

internal val DrawPath.start: ScenePoint get() = (segments.first() as PathMove).to
internal val DrawPath.end: ScenePoint get() = when (val last = segments.last()) {
    is PathMove -> last.to; is PathLine -> last.to; is PathQuadratic -> last.to; is PathArc -> last.end
}

/** Edge curves in paint order. */
internal fun LayoutScene.edgeCurves(): List<DrawPath> = commands.filterIsInstance<DrawPath>()

/** Node and frame boxes; edge-label backgrounds have no stroke. */
internal fun LayoutScene.boxes(): List<SceneRect> = commands.filterIsInstance<DrawRect>().filter { it.stroke.value != "none" }.map { it.rect }

/** Edge-label backgrounds. */
internal fun LayoutScene.labelBackgrounds(): List<SceneRect> = commands.filterIsInstance<DrawRect>().filter { it.stroke.value == "none" }.map { it.rect }

internal fun List<ScenePoint>.entersBox(r: SceneRect, inset: Double = 1.0): Boolean = any {
    it.x > r.x + inset && it.x < r.x + r.width - inset && it.y > r.y + inset && it.y < r.y + r.height - inset
}

/** Proper crossings between two flattened curves. */
internal fun crossings(a: List<ScenePoint>, b: List<ScenePoint>): Int {
    fun orient(p: ScenePoint, q: ScenePoint, r: ScenePoint) = (q.x - p.x) * (r.y - p.y) - (q.y - p.y) * (r.x - p.x)
    var count = 0
    for ((p1, p2) in a.zipWithNext()) for ((q1, q2) in b.zipWithNext()) {
        val d1 = orient(q1, q2, p1); val d2 = orient(q1, q2, p2); val d3 = orient(p1, p2, q1); val d4 = orient(p1, p2, q2)
        if (d1 * d2 < -1e-9 && d3 * d4 < -1e-9) count++
    }
    return count
}

internal fun SceneRect.intersects(b: SceneRect) = x < b.x + b.width && b.x < x + width && y < b.y + b.height && b.y < y + height
