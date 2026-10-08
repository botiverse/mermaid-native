package build.raft.mermaid.layout.simple

import build.raft.mermaid.layout.ScenePoint
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Ordered, immutable boundary for post-layout edge and group geometry. */
public object MaterializedGeometry {
    public data class Bounds(val left: Double, val right: Double, val top: Double, val bottom: Double)
    public data class Node(
        val id: String? = null, val x: Double? = null, val y: Double? = null,
        val width: Double? = null, val height: Double? = null,
        val isGroup: Boolean = false, val isEdgeLabel: Boolean = false,
        val parentId: String? = null, val direction: String? = null, val groupTitleRect: Bounds? = null,
    )
    public data class Edge(
        val id: String? = null, val start: String? = null, val end: String? = null,
        val points: List<ScenePoint>? = null, val isLayoutOnly: Boolean = false,
    )
    public data class Result(val edges: List<Edge>, val nodes: Map<String, Node>)
    public enum class Operation {
        SEPARATE_TERMINAL_LANES, COLLAPSE_DOGLEGS, LIFT_OBSTACLE_RAILS,
        LIFT_TOP_TITLES, SHIFT_LEFT_TITLES, SWAP_DESTINATION_TAILS,
        REASSIGN_EXTERNAL_CHANNELS, SHORTCUT_JOGS, RESOLVE_CROSSINGS,
    }

    /** Each call isolates mutations, preserving edge identity by its ordered occurrence, not its ID. */
    public fun apply(operation: Operation, edges: List<Edge>, nodes: Map<String, Node>): Result {
        val state = MaterializedState(edges, nodes)
        when (operation) {
            Operation.SEPARATE_TERMINAL_LANES -> state.separateTerminalLanes()
            Operation.COLLAPSE_DOGLEGS -> state.collapseDoglegs()
            Operation.LIFT_OBSTACLE_RAILS -> state.liftObstacleRails()
            Operation.LIFT_TOP_TITLES -> state.shiftTitles(left = false)
            Operation.SHIFT_LEFT_TITLES -> state.shiftTitles(left = true)
            Operation.SWAP_DESTINATION_TAILS -> state.swapDestinationTails()
            Operation.REASSIGN_EXTERNAL_CHANNELS -> state.reassignExternalChannels()
            Operation.SHORTCUT_JOGS -> state.shortcutJogs()
            Operation.RESOLVE_CROSSINGS -> state.resolveCrossings()
        }
        return state.result()
    }
}

internal typealias MPoint = ScenePoint
internal typealias MBounds = MaterializedGeometry.Bounds
internal const val M_EPS = 1e-3
internal const val M_SHARED = 8.0
internal fun mx(a: MPoint, b: MPoint, epsilon: Double = M_EPS) = abs(a.x - b.x) < epsilon
internal fun my(a: MPoint, b: MPoint, epsilon: Double = M_EPS) = abs(a.y - b.y) < epsilon
internal fun same(a: MPoint, b: MPoint, epsilon: Double = M_EPS) = mx(a, b, epsilon) && my(a, b, epsilon)
internal fun horizontal(a: MPoint, b: MPoint) = my(a, b) && abs(a.x - b.x) > M_EPS
internal fun vertical(a: MPoint, b: MPoint) = mx(a, b) && abs(a.y - b.y) > M_EPS
internal fun aligned(a: MPoint, b: MPoint) = mx(a, b) || my(a, b)
internal fun materializedOverlap(a: Double, b: Double, c: Double, d: Double) = max(0.0, min(max(a,b), max(c,d)) - max(min(a,b), min(c,d)))
internal data class MSegment(val index: Int, val a: MPoint, val b: MPoint) {
    val horizontal = horizontal(a,b)
    val vertical = vertical(a,b)
}
internal fun segments(points: List<MPoint>): List<MSegment> = points.zipWithNext().mapIndexedNotNull { i, (a,b) ->
    MSegment(i,a,b).takeIf { it.horizontal || it.vertical }
}
internal fun dedupe(points: List<MPoint>): List<MPoint> {
    val out = mutableListOf<MPoint>()
    for (p in points) if (out.lastOrNull()?.let { same(it,p) } != true) out += p.copy()
    return out
}
internal fun simplify(points: List<MPoint>): List<MPoint> {
    if (points.size < 3) return points
    var work = points
    repeat(32) {
        var changed = false
        val out = mutableListOf<MPoint>()
        var i = 0
        while (i < work.size) {
            val p = out.lastOrNull(); val c = work[i]; val n = work.getOrNull(i+1)
            if (p != null && n != null) {
                if (same(p,n)) { i += 2; changed = true; continue }
                fun between(v: Double,a: Double,b: Double) = v > min(a,b)+M_EPS && v < max(a,b)-M_EPS
                if ((mx(p,c) && mx(c,n) && between(c.y,p.y,n.y)) ||
                    (my(p,c) && my(c,n) && between(c.x,p.x,n.x))) { i++; changed = true; continue }
            }
            out += c; i++
        }
        work = out
        if (!changed) return work
    }
    return work
}
internal fun shared(a: MSegment, b: MSegment, epsilon: Double = 0.5): Double = when {
    a.horizontal && b.horizontal && my(a.a,b.a,epsilon) -> materializedOverlap(a.a.x,a.b.x,b.a.x,b.b.x)
    a.vertical && b.vertical && mx(a.a,b.a,epsilon) -> materializedOverlap(a.a.y,a.b.y,b.a.y,b.b.y)
    else -> 0.0
}
internal fun crosses(a: MSegment,b: MSegment) = OrthogonalGeometry.segmentsStrictlyCross(a.a,a.b,b.a,b.b)
internal fun bends(points: List<MPoint>): Int = segments(points).zipWithNext().count { (a,b) -> a.horizontal != b.horizontal }
internal fun MBounds.valid() = listOf(left,right,top,bottom).all { it.isFinite() } && right > left && bottom > top
internal fun MaterializedGeometry.Node.bounds(): MBounds? {
    val w = width ?: 0.0; val h = height ?: 0.0; val cx=x?:0.0; val cy=y?:0.0
    return if (w > 0 && h > 0) MBounds(cx-w/2,cx+w/2,cy-h/2,cy+h/2) else null
}
internal data class MRectEntry(val id: String, val rect: MBounds)
internal class MEdge(val input: MaterializedGeometry.Edge) {
    var points: List<MPoint>? = input.points?.map { it.copy() }
    val start get() = input.start
    val end get() = input.end
    val visible get() = !input.isLayoutOnly
    val endpointIds get() = listOfNotNull(start,end).filter { it.isNotEmpty() }
}
internal typealias MReplacements = Map<MEdge,List<MPoint>>
internal class MaterializedState(inputEdges: List<MaterializedGeometry.Edge>, inputNodes: Map<String,MaterializedGeometry.Node>) {
    val edges = inputEdges.map { MEdge(it) }
    val nodes = inputNodes.toMutableMap()
    val visible get() = edges.filter { it.visible }
    fun points(edge: MEdge, replacements: MReplacements = emptyMap()) = dedupe(replacements[edge] ?: edge.points.orEmpty())
    private val leafRects = collectRects(false)
    private val labelRects = collectRects(true)
    private fun collectRects(labels: Boolean): List<MRectEntry> = nodes.values.filter { !it.isGroup && it.isEdgeLabel == labels }
        .mapNotNull { node -> node.bounds()?.let { MRectEntry(node.id ?: "",it) } }
    fun rects(labels: Boolean = false): List<MRectEntry> = if (labels) labelRects else leafRects
    fun crossings(replacements: MReplacements = emptyMap()): Int {
        var count=0; val visible=visible
        for (i in visible.indices) for (j in i+1 until visible.size)
            for (a in segments(points(visible[i],replacements))) for (b in segments(points(visible[j],replacements)))
                if (crosses(a,b)) count++
        return count
    }
    fun totalBends(replacements: MReplacements = emptyMap()) = visible.sumOf { bends(points(it,replacements)) }
    fun nodeHit(edge: MEdge, path: List<MPoint>, labels: Boolean = true): Boolean = segments(path).any { s ->
        hits(s,rects(),edge.endpointIds,-2.0) || (labels && hits(s,rects(true),emptyList(),-2.0))
    }
    fun sharedTrack(edge: MEdge, path: List<MPoint>, replacements: MReplacements = emptyMap()): Boolean = visible.any { other ->
        other !== edge && segments(path).any { a -> segments(points(other,replacements)).any { b -> shared(a,b) >= M_SHARED } }
    }
    fun commit(replacements: MReplacements) { for ((edge,path) in replacements) edge.points=path }
    fun result() = MaterializedGeometry.Result(edges.map { it.input.copy(points=it.points?.toList()) },nodes.toMap())
}
internal fun hits(s: MSegment, entries: List<MRectEntry>, excluded: List<String> = emptyList(), shrink: Double = 0.0) = entries.any {
    val r=it.rect
    it.id !in excluded && max(s.a.x,s.b.x)>r.left+shrink && min(s.a.x,s.b.x)<r.right-shrink &&
        max(s.a.y,s.b.y)>r.top+shrink && min(s.a.y,s.b.y)<r.bottom-shrink
}
