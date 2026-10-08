@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.UsecaseJsonParser
import build.raft.mermaid.core.UsecaseJsonValue
import build.raft.mermaid.layout.*
import kotlin.io.encoding.Base64
import kotlin.math.*

/** Line hops over true segment crossings, ported from Mermaid's lineJump.ts. */
public object LineJumps {
    public data class Edge(
        val id: String,
        val points: List<ScenePoint>,
        val curve: String? = null,
        val arrowTypeStart: String? = null,
        val arrowTypeEnd: String? = null,
    )
    public enum class Style { ARC, GAP }
    public data class Config(val enabled: Boolean = true, val radius: Double = 6.0, val style: Style = Style.ARC) {
        init { require(radius.isFinite() && radius >= 0) }
    }
    public data class Crossing(val jumpEdgeId: String, val otherEdgeId: String, val segIndex: Int, val t: Double, val point: ScenePoint)
    public data class Geometry(val d: String, val segments: List<ScenePathSegment>)
    /** Host-rendered attributes. dataPoints is the original base64 JSON attribute, before marker offsets. */
    public data class RenderedPath(val id: String, val d: String, val dataPoints: String? = null, val style: String = "")
    public data class Patch(val id: String, val geometry: Geometry, val style: String)

    public fun findEdgeIntersections(edges: List<Edge>): List<Crossing> = buildList {
        for (i in edges.indices) for (j in i + 1 until edges.size) {
            val a = edges[i]; val b = edges[j]
            for (si in 0 until a.points.lastIndex) for (sj in 0 until b.points.lastIndex) {
                val a1 = a.points[si]; val a2 = a.points[si + 1]
                val b1 = b.points[sj]; val b2 = b.points[sj + 1]
                val ax = a2.x - a1.x; val ay = a2.y - a1.y
                val bx = b2.x - b1.x; val by = b2.y - b1.y
                val denominator = ax * by - ay * bx
                if (denominator == 0.0) continue
                val dx = b1.x - a1.x; val dy = b1.y - a1.y
                val ta = (dx * by - dy * bx) / denominator
                val tb = (dx * ay - dy * ax) / denominator
                if (ta <= 1e-6 || ta >= 1 - 1e-6 || tb <= 1e-6 || tb >= 1 - 1e-6) continue
                val point = ScenePoint(a1.x + ta * ax, a1.y + ta * ay)
                val ah = abs(ax) >= abs(ay); val bh = abs(bx) >= abs(by)
                add(if (ah != bh && ah) Crossing(a.id, b.id, si, ta, point) else Crossing(b.id, a.id, sj, tb, point))
            }
        }
    }

    // JS Math.round, including negative halfway values, rather than Kotlin's ties-to-even round.
    private fun fmt(value: Double): String {
        val rounded = floor(value * 1000.0 + 0.5) / 1000.0
        if (rounded == 0.0) return "0"
        return if (abs(rounded) < 1e18 && rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
    }
    private fun point(p: ScenePoint): String = "${fmt(p.x)},${fmt(p.y)}"
    private fun plain(points: List<ScenePoint>): Geometry = Geometry(
        points.mapIndexed { index, p -> (if (index == 0) "M" else "L") + point(p) }.joinToString(" "),
        points.mapIndexed { index, p -> if (index == 0) PathMove(p) else PathLine(p) },
    )
    private fun offsetPoints(edge: Edge): List<ScenePoint> {
        if (edge.points.size < 2) return edge.points.toList()
        fun offset(type: String?): Double = when (type) {
            "aggregation", "extension", "composition" -> 17.25
            "dependency" -> 6.0
            "lollipop" -> 13.5
            "arrow_point" -> 4.0
            "arrow_barb_neo" -> 5.5
            else -> 0.0
        }
        val out = edge.points.toMutableList()
        val first = out.first(); val next = out[1]
        val last = out.last(); val previous = out[out.lastIndex - 1]
        val startAngle = atan2(next.y - first.y, next.x - first.x)
        val endAngle = atan2(last.y - previous.y, last.x - previous.x)
        val start = offset(edge.arrowTypeStart); val end = offset(edge.arrowTypeEnd)
        out[0] = ScenePoint(first.x + start * cos(startAngle), first.y + start * sin(startAngle))
        out[out.lastIndex] = ScenePoint(last.x - end * cos(endAngle), last.y - end * sin(endAngle))
        return out
    }
    private data class Corner(val start: ScenePoint, val end: ScenePoint, val control: ScenePoint, val cut: Double)
    private fun corner(previous: ScenePoint, current: ScenePoint, next: ScenePoint): Corner? {
        val ax = current.x - previous.x; val ay = current.y - previous.y
        val bx = next.x - current.x; val by = next.y - current.y
        val al = hypot(ax, ay); val bl = hypot(bx, by)
        if (al < 1e-5 || bl < 1e-5) return null
        val angle = acos((ax / al * bx / bl + ay / al * by / bl).coerceIn(-1.0, 1.0))
        if (angle < 1e-5 || abs(PI - angle) < 1e-5) return null
        val cut = minOf(5.0 / sin(angle / 2), al / 2, bl / 2)
        return Corner(ScenePoint(current.x - ax / al * cut, current.y - ay / al * cut),
            ScenePoint(current.x + bx / bl * cut, current.y + by / bl * cut), current, cut)
    }
    private data class Jump(val crossing: Crossing, val distance: Double, var radius: Double)
    private fun rewrite(edge: Edge, crossings: List<Crossing>, config: Config): Geometry {
        if (edge.points.size < 2) return Geometry("", emptyList())
        val points = offsetPoints(edge)
        val parts = mutableListOf("M" + point(points.first()))
        val path = mutableListOf<ScenePathSegment>(PathMove(points.first()))
        fun line(p: ScenePoint) { parts += "L" + point(p); path += PathLine(p) }
        for (i in 0 until points.lastIndex) {
            val a = points[i]; val b = points[i + 1]
            val dx = b.x - a.x; val dy = b.y - a.y; val length = hypot(dx, dy)
            val ux = if (length == 0.0) 0.0 else dx / length
            val uy = if (length == 0.0) 0.0 else dy / length
            val clockwise = if (abs(dx) >= abs(dy)) dx >= 0 else dy >= 0
            val before = if (edge.curve == "rounded" && i > 0) corner(points[i - 1], a, b) else null
            val after = if (edge.curve == "rounded" && i + 2 < points.size) corner(a, b, points[i + 2]) else null
            val start = before?.cut ?: 0.0; val stop = length - (after?.cut ?: 0.0)
            val jumps = crossings.filter { it.segIndex == i }.sortedBy { it.t }.map {
                val d = it.t * length
                Jump(it, d, minOf(config.radius, d - start, stop - d))
            }
            for (k in 0 until jumps.lastIndex) {
                val gap = jumps[k + 1].distance - jumps[k].distance
                if (jumps[k].radius + jumps[k + 1].radius > gap) {
                    jumps[k].radius = min(jumps[k].radius, gap / 2)
                    jumps[k + 1].radius = min(jumps[k + 1].radius, gap / 2)
                }
            }
            for (jump in jumps) {
                val r = jump.radius
                if (r < 1e-3) continue
                val c = jump.crossing.point
                val pre = ScenePoint(c.x - ux * r, c.y - uy * r)
                val post = ScenePoint(c.x + ux * r, c.y + uy * r)
                line(pre)
                if (config.style == Style.ARC) {
                    parts += "A${fmt(r)},${fmt(r)} 0 0 ${if (clockwise) 1 else 0} ${point(post)}"
                    path += PathArc(c, r, atan2(-uy, -ux), if (clockwise) PI else -PI)
                } else { parts += "M" + point(post); path += PathMove(post) }
            }
            if (after != null) {
                line(after.start)
                parts += "Q${point(after.control)} ${point(after.end)}"
                path += PathQuadratic(after.control, after.end)
            } else line(b)
        }
        return Geometry(parts.joinToString(" "), path)
    }

    public fun processGeometry(edges: List<Edge>, config: Config): Map<String, Geometry> {
        val crossings = if (config.enabled) findEdgeIntersections(edges).groupBy { it.jumpEdgeId } else emptyMap()
        return edges.associate { it.id to (crossings[it.id]?.let { hits -> rewrite(it, hits, config) } ?: plain(it.points)) }
    }
    public fun processEdgesWithJumps(edges: List<Edge>, config: Config): Map<String, String> =
        processGeometry(edges, config).mapValues { it.value.d }

    public fun isStraightPath(d: String): Boolean = Regex("^[\\d\\s+,.LMelm-]*$").matches(d)
    public fun curveSupportsLineHops(curve: String?): Boolean = curve.isNullOrEmpty() ||
        curve in listOf("linear", "rounded", "step", "stepBefore", "stepAfter")

    private fun decodePoints(raw: String?): List<ScenePoint>? {
        if (raw.isNullOrEmpty()) return null
        return try {
            val json = Base64.Default.decode(raw).decodeToString()
            val values = UsecaseJsonParser.parseOrderedJsonObject("{\"points\":$json}", 1, 1).value["points"] as? UsecaseJsonValue.ArrayValue ?: return null
            values.value.mapNotNull { value ->
                val fields = (value as? UsecaseJsonValue.ObjectValue)?.value ?: return@mapNotNull null
                val x = (fields["x"] as? UsecaseJsonValue.NumberValue)?.value ?: return@mapNotNull null
                val y = (fields["y"] as? UsecaseJsonValue.NumberValue)?.value ?: return@mapNotNull null
                if (!x.isFinite() || !y.isFinite()) null else ScenePoint(x, y)
            }.takeIf { it.size >= 2 }
        } catch (_: IllegalArgumentException) { null }
    }

    /**
     * DOM-independent equivalent of applyLineJumpsToSvg. Only changed paths are returned.
     * Host supplies post-rewrite length when it has getTotalLength; absent length leaves neo style intact.
     * CSS lookup and attribute writes belong to the host, not the layout algorithm.
     */
    public fun patchRenderedPaths(
        edges: List<Edge>, paths: List<RenderedPath>, config: Config,
        totalLength: ((id: String, d: String) -> Double?)? = null,
    ): List<Patch> {
        if (!config.enabled) return emptyList()
        val byId = paths.associateBy { it.id }
        val rendered = edges.mapNotNull { edge -> byId[edge.id]?.let { edge.copy(points = decodePoints(it.dataPoints) ?: edge.points) } }
        val crossings = findEdgeIntersections(rendered).groupBy { it.jumpEdgeId }
        val dash = Regex("stroke-dasharray\\s*:\\s*0\\s+([\\d.]+)\\s+[\\d.]+\\s+([\\d.]+)")
        return rendered.mapNotNull { edge ->
            val hits = crossings[edge.id] ?: return@mapNotNull null
            val original = byId.getValue(edge.id)
            if (edge.curve != null && !curveSupportsLineHops(edge.curve)) return@mapNotNull null
            if (edge.curve == null && !isStraightPath(original.d)) return@mapNotNull null
            val geometry = rewrite(edge, hits, config)
            val match = dash.find(original.style)
            var style = original.style
            if (match != null) {
                val start = match.groupValues[1].toDouble(); val end = match.groupValues[2].toDouble()
                val length = totalLength?.invoke(edge.id, geometry.d)
                if (length != null) {
                    require(length.isFinite() && length >= 0)
                    fun number(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
                    val value = "0 ${number(start)} ${number(max(0.0, length - start - end))} ${number(end)}"
                    style = Regex("stroke-dasharray\\s*:[^;]*;?").replace(style, "stroke-dasharray: $value;")
                    style = Regex(";\\s*;+").replace(style, ";")
                }
            }
            Patch(edge.id, geometry, style)
        }
    }
}
