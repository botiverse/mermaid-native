package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.FlowDirection
import build.raft.mermaid.core.FlowEdgeStyle
import build.raft.mermaid.core.FlowchartDiagram
import build.raft.mermaid.layout.*
import kotlin.math.max

/**
 * Global layering for compound flowcharts whose containers are not rank-convex.
 *
 * The recursive [FlowPlacement] ranks the children of each container as units.
 * When edges leave a container and come back (`Core → App → Core`), the
 * container-level graph is cyclic: one direction is treated as feedback and
 * every node of a container collapses into its local ranks. This placement
 * instead ranks all nodes by their edges, as Mermaid's dagre layout does, and
 * lets container membership constrain only the cross-axis grouping and the
 * extent of the frames:
 *
 * 1. Feedback arcs (depth-first, declaration order) are reversed and nodes are
 *    ranked by longest path; sources are pulled next to their successors.
 * 2. Ranks are doubled. Every edge becomes a chain of virtual units, one per
 *    intermediate rank; the middle odd rank of a labeled edge holds its label.
 *    Virtual units join the deepest container of the nearer endpoint that
 *    already spans their rank, so edges enter frames where their nodes are.
 * 3. Barycentric sweeps order each rank. Members of a container stay
 *    contiguous and sibling containers keep one order across all ranks.
 * 4. Cross positions solve the separation and containment constraints as
 *    longest paths; the mean of the left- and right-compacted solutions
 *    centers every rank while keeping all constraints.
 * 5. Frames extend from their first to their last rank. Rank gaps reserve the
 *    frame headers and padding that start or end there.
 *
 * Edges are returned as complete routes through their chains, so labels sit in
 * reserved rows and never overlap. Back edges attach to side ports. Returns
 * null when the recursive placement applies or the input is outside this
 * placement's scope (self-loops, edges to containers, mixed axes, collapsed
 * frames).
 */
internal fun flowCompoundLayering(
    diagram: FlowchartDiagram,
    nodeSizes: Map<String, SceneSize>,
    config: LayoutConfig,
    measurer: TextMeasurer,
): FlowPlacement.Result? {
    if (diagram.subgraphs.isEmpty() || diagram.nodes.isEmpty()) return null
    val horizontal = diagram.direction == FlowDirection.LR || diagram.direction == FlowDirection.RL
    val reversed = diagram.direction == FlowDirection.BT || diagram.direction == FlowDirection.RL
    val groups = diagram.subgraphs.associateBy { it.id }
    val nodeIds = diagram.nodes.map { it.id }
    val nodeSet = nodeIds.toSet()
    if (groups.size != diagram.subgraphs.size || nodeSet.size != nodeIds.size) return null
    if (nodeIds.any { it in groups || it !in nodeSizes }) return null
    if (diagram.subgraphs.any { g ->
            g.collapsed || g.direction?.let { (it == FlowDirection.LR || it == FlowDirection.RL) != horizontal } == true
        }) return null
    for (g in diagram.subgraphs) {
        val seen = mutableSetOf(g.id)
        var p = g.parentId
        while (p != null) {
            if (p !in groups || !seen.add(p)) return null
            p = groups.getValue(p).parentId
        }
    }
    val edges = diagram.edges
    if (edges.any { it.sourceId !in nodeSet || it.targetId !in nodeSet || it.sourceId == it.targetId }) return null

    fun depth(group: String): Int {
        var d = 0
        var p = groups.getValue(group).parentId
        while (p != null) { d++; p = groups.getValue(p).parentId }
        return d
    }
    // The innermost declaring container owns a node.
    val owner: Map<String, String?> = nodeIds.associateWith { id ->
        diagram.subgraphs.filter { id in it.nodeIds }.maxByOrNull { depth(it.id) }?.id
    }
    fun clusterChain(start: String?): List<String> {
        val result = mutableListOf<String>()
        var c = start
        while (c != null) { result.add(c); c = groups.getValue(c).parentId }
        return result
    }
    val nodeClusters = nodeIds.associateWith { clusterChain(owner[it]) }
    if (groups.keys.any { g -> nodeClusters.values.none { g in it } }) return null

    // 1. Feedback arcs and ranks.
    val outgoing = edges.indices.groupBy { edges[it].sourceId }
    val feedback = mutableSetOf<Int>()
    val visited = mutableSetOf<String>()
    val active = mutableSetOf<String>()
    for (root in nodeIds) {
        if (!visited.add(root)) continue
        active.add(root)
        val stack = mutableListOf(root to outgoing[root].orEmpty().iterator())
        while (stack.isNotEmpty()) {
            val (id, iterator) = stack.last()
            if (!iterator.hasNext()) { active.remove(id); stack.removeAt(stack.lastIndex); continue }
            val e = iterator.next()
            val target = edges[e].targetId
            if (target in active) feedback.add(e)
            else if (visited.add(target)) { active.add(target); stack.add(target to outgoing[target].orEmpty().iterator()) }
        }
    }
    fun upper(e: Int) = if (e in feedback) edges[e].targetId else edges[e].sourceId
    fun lower(e: Int) = if (e in feedback) edges[e].sourceId else edges[e].targetId
    fun minLength(e: Int) = edges[e].length.coerceIn(1, 10)
    val down = edges.indices.groupBy(::upper)
    val indegree = nodeIds.associateWith { 0 }.toMutableMap()
    edges.indices.forEach { indegree[lower(it)] = indegree.getValue(lower(it)) + 1 }
    val rank = nodeIds.associateWith { 0 }.toMutableMap()
    val queue = nodeIds.filter { indegree.getValue(it) == 0 }.toMutableList()
    var next = 0
    while (next < queue.size) {
        val id = queue[next++]
        for (e in down[id].orEmpty()) {
            val t = lower(e)
            rank[t] = max(rank.getValue(t), rank.getValue(id) + minLength(e))
            indegree[t] = indegree.getValue(t) - 1
            if (indegree.getValue(t) == 0) queue.add(t)
        }
    }
    if (queue.size != nodeIds.size) return null
    val hasIncoming = edges.indices.map(::lower).toSet()
    for (id in queue.asReversed()) {
        if (id in hasIncoming) continue
        val outs = down[id] ?: continue
        rank[id] = outs.minOf { rank.getValue(lower(it)) - minLength(it) }
    }
    val lowest = rank.values.minOrNull() ?: 0
    nodeIds.forEach { rank[it] = rank.getValue(it) - lowest }

    // Only take over where the recursive placement must invert edges: some
    // container's children are connected in both directions.
    fun under(container: String?, node: String): String? {
        if (owner[node] == container) return node
        return nodeClusters.getValue(node).firstOrNull { groups.getValue(it).parentId == container }
    }
    fun cyclic(arcs: Set<Pair<String, String>>): Boolean {
        val vertices = arcs.flatMap { listOf(it.first, it.second) }.toSet()
        val degree = vertices.associateWith { v -> arcs.count { it.second == v } }.toMutableMap()
        val ready = vertices.filter { degree.getValue(it) == 0 }.toMutableList()
        var seen = 0
        while (ready.isNotEmpty()) {
            val v = ready.removeAt(ready.lastIndex)
            seen++
            arcs.filter { it.first == v }.forEach { (_, t) ->
                degree[t] = degree.getValue(t) - 1
                if (degree.getValue(t) == 0) ready.add(t)
            }
        }
        return seen != vertices.size
    }
    val containers = listOf<String?>(null) + diagram.subgraphs.map { it.id }
    if (containers.none { p ->
            cyclic(edges.indices.mapNotNull { e ->
                val a = under(p, upper(e))
                val b = under(p, lower(e))
                if (a == null || b == null || a == b) null else a to b
            }.toSet())
        }) return null

    // 2. Units: real nodes on even ranks, virtual chain units in between.
    class LayerUnit(val id: String, val rank: Int, val cluster: String?, val cross: Double, val main: Double,
        val real: Boolean, val label: SceneSize? = null)
    val units = linkedMapOf<String, LayerUnit>()
    nodeIds.forEach { id ->
        val size = nodeSizes.getValue(id)
        units[id] = LayerUnit(id, 2 * rank.getValue(id), owner[id], if (horizontal) size.height else size.width,
            if (horizontal) size.width else size.height, true)
    }
    val realSpan = groups.keys.associateWith { g ->
        val ranks = nodeIds.filter { g in nodeClusters.getValue(it) }.map { units.getValue(it).rank }
        ranks.min() to ranks.max()
    }
    val labelSizes = edges.map { e ->
        e.label?.takeIf { it.isNotEmpty() && e.style != FlowEdgeStyle.INVISIBLE }?.let { measurer.measure(it, TextStyle()) }
    }
    val chains = edges.indices.map { e ->
        val a = upper(e)
        val b = lower(e)
        val ra = units.getValue(a).rank
        val rb = units.getValue(b).rank
        val span = (rb - ra) / 2
        val labelRank = ra + 2 * ((span - 1) / 2) + 1
        fun pick(node: String, k: Int) = nodeClusters.getValue(node).firstOrNull { g ->
            realSpan.getValue(g).let { k >= it.first && k <= it.second }
        }
        val common = nodeClusters.getValue(a).firstOrNull { it in nodeClusters.getValue(b) }
        val chain = mutableListOf(a)
        for (k in ra + 1 until rb) {
            val nearUpper = 2 * k <= ra + rb
            val cluster = pick(if (nearUpper) a else b, k) ?: pick(if (nearUpper) b else a, k) ?: common
            val size = labelSizes[e]?.takeIf { k == labelRank }
            val id = "\u0000$e:$k"
            units[id] = if (size == null) LayerUnit(id, k, cluster, 0.0, 0.0, false)
            else LayerUnit(id, k, cluster, if (horizontal) size.height + 12.0 else size.width + 14.0,
                if (horizontal) size.width else size.height, false, size)
            chain.add(id)
        }
        chain.add(b)
        chain.toList()
    }
    val unitClusters = units.mapValues { (_, u) -> clusterChain(u.cluster) }
    val span = groups.keys.associateWith { g ->
        val ranks = units.values.filter { g in unitClusters.getValue(it.id) }.map { it.rank }
        ranks.min() to ranks.max()
    }
    val maxRank = units.values.maxOf { it.rank }
    val children = containers.associateWith { p -> diagram.subgraphs.filter { it.parentId == p }.map { it.id } }
    val upNeighbors = mutableMapOf<String, MutableList<String>>()
    val downNeighbors = mutableMapOf<String, MutableList<String>>()
    chains.forEach { chain ->
        chain.zipWithNext().forEach { (a, b) ->
            downNeighbors.getOrPut(a) { mutableListOf() }.add(b)
            upNeighbors.getOrPut(b) { mutableListOf() }.add(a)
        }
    }

    // 3. Ordering with contiguous containers.
    val rankUnits = (0..maxRank).map { k -> units.values.filter { it.rank == k }.map { it.id } }
    val order = rankUnits.map { it.toMutableList() }.toMutableList()
    val pos = mutableMapOf<String, Double>()
    fun index() = order.forEach { list -> list.forEachIndexed { i, id -> pos[id] = i.toDouble() } }
    index()
    val clusterKey = mutableMapOf<String, Double>()
    val clusterOrder = mutableMapOf<String?, List<String>>()
    fun orderClusters() {
        groups.keys.forEach { g ->
            clusterKey[g] = units.keys.filter { g in unitClusters.getValue(it) }.map { pos.getValue(it) }.average()
        }
        containers.forEach { p -> clusterOrder[p] = children.getValue(p).sortedBy { clusterKey.getValue(it) } }
    }
    orderClusters()
    var structure = (0..maxRank).map { mutableMapOf<String?, List<String>>() }
    fun arrange(k: Int, key: Map<String, Double>): List<String> {
        fun build(p: String?): List<String> {
            val direct = rankUnits[k].filter { units.getValue(it).cluster == p }
            val subs = children.getValue(p).filter { span.getValue(it).let { (lo, hi) -> k in lo..hi } }
            fun keyOf(item: String): Double {
                if (item !in groups) return key.getValue(item)
                val inside = rankUnits[k].filter { item in unitClusters.getValue(it) }
                return if (inside.isEmpty()) clusterKey.getValue(item) else inside.map { key.getValue(it) }.average()
            }
            val sorted = (direct + subs).sortedBy(::keyOf)
            val fixed = clusterOrder.getValue(p).filter { it in subs }.iterator()
            val items = sorted.map { if (it in groups) fixed.next() else it }
            structure[k][p] = items
            return items.flatMap { if (it in groups) build(it) else listOf(it) }
        }
        return build(null)
    }
    fun crossings(): Int {
        var total = 0
        for (k in 0 until maxRank) {
            val segments = rankUnits[k].flatMap { a -> downNeighbors[a].orEmpty().map { pos.getValue(a) to pos.getValue(it) } }
            for (i in segments.indices) for (j in i + 1 until segments.size) {
                if ((segments[i].first - segments[j].first) * (segments[i].second - segments[j].second) < 0) total++
            }
        }
        return total
    }
    for (k in 0..maxRank) order[k] = arrange(k, rankUnits[k].associateWith { pos.getValue(it) }).toMutableList()
    index()
    var bestCrossings = crossings()
    var bestOrder = order.map { it.toList() }
    var bestStructure = structure.map { it.toMap() }
    repeat(12) { iteration ->
        orderClusters()
        structure = (0..maxRank).map { mutableMapOf() }
        val downward = iteration % 2 == 0
        val sequence = if (downward) (0..maxRank).toList() else (maxRank downTo 0).toList()
        for (k in sequence) {
            val key = rankUnits[k].associateWith { u ->
                val neighbors = if (downward) upNeighbors[u] else downNeighbors[u]
                if (neighbors.isNullOrEmpty()) pos.getValue(u) else neighbors.map { pos.getValue(it) }.average()
            }
            order[k] = arrange(k, key).toMutableList()
            order[k].forEachIndexed { i, id -> pos[id] = i.toDouble() }
        }
        val count = crossings()
        if (count < bestCrossings) {
            bestCrossings = count
            bestOrder = order.map { it.toList() }
            bestStructure = structure.map { it.toMap() }
        }
    }

    // 4. Cross-axis positions.
    fun header(g: String) = max(32.0, flowGroupStyle(groups.getValue(g), diagram).text.fontSize + 18)
    fun crossStart(g: String) = if (horizontal) header(g) else 16.0
    val crossEnd = 16.0
    val variables = linkedMapOf<String, Int>()
    units.keys.forEach { variables[it] = variables.size }
    groups.keys.forEach { g -> variables["L\u0000$g"] = variables.size; variables["R\u0000$g"] = variables.size }
    fun left(g: String) = variables.getValue("L\u0000$g")
    fun right(g: String) = variables.getValue("R\u0000$g")
    data class Constraint(val from: Int, val to: Int, val distance: Double)
    val constraints = mutableListOf<Constraint>()
    val nodeGap = config.nodeGap
    bestStructure.forEach { levels ->
        levels.values.forEach { items ->
            items.zipWithNext().forEach { (a, b) ->
                val solid = (a in groups || units.getValue(a).real) && (b in groups || units.getValue(b).real)
                val gap = if (solid) nodeGap else nodeGap / 4
                val from = if (a in groups) right(a) else variables.getValue(a)
                val width = if (a in groups) 0.0 else units.getValue(a).cross
                constraints += Constraint(from, if (b in groups) left(b) else variables.getValue(b), width + gap)
            }
        }
    }
    units.values.forEach { u ->
        val g = u.cluster ?: return@forEach
        constraints += Constraint(left(g), variables.getValue(u.id), crossStart(g))
        constraints += Constraint(variables.getValue(u.id), right(g), u.cross + crossEnd)
    }
    diagram.subgraphs.forEach { g ->
        val title = if (horizontal) 0.0 else measurer.measure(g.label, flowGroupStyle(g, diagram).text).width + 24
        constraints += Constraint(left(g.id), right(g.id), max(crossStart(g.id) + crossEnd, title))
        val p = g.parentId ?: return@forEach
        constraints += Constraint(left(p), left(g.id), crossStart(p))
        constraints += Constraint(right(g.id), right(p), crossEnd)
    }
    val count = variables.size
    val low = DoubleArray(count)
    var stable = false
    for (iteration in 0..count + 1) {
        var changed = false
        constraints.forEach { c ->
            if (low[c.to] < low[c.from] + c.distance - 1e-9) { low[c.to] = low[c.from] + c.distance; changed = true }
        }
        if (!changed) { stable = true; break }
    }
    if (!stable) return null
    val extentOf = DoubleArray(count)
    units.values.forEach { extentOf[variables.getValue(it.id)] = it.cross }
    val total = (0 until count).maxOf { low[it] + extentOf[it] }
    val high = DoubleArray(count) { total - extentOf[it] }
    stable = false
    for (iteration in 0..count + 1) {
        var changed = false
        constraints.forEach { c ->
            if (high[c.from] > high[c.to] - c.distance + 1e-9) { high[c.from] = high[c.to] - c.distance; changed = true }
        }
        if (!changed) { stable = true; break }
    }
    if (!stable) return null
    val cross = DoubleArray(count) { (low[it] + high[it]) / 2 }
    fun crossOf(id: String) = cross[variables.getValue(id)]

    // 5. Main-axis positions: bands per rank, frame paddings in the gaps.
    val band = (0..maxRank).map { k -> rankUnits[k].maxOfOrNull { units.getValue(it).main } ?: 0.0 }
    val extra = groups.keys.associateWith { 0.0 }.toMutableMap()
    fun mainStart(g: String) = (if (!horizontal && !reversed) header(g) else 16.0) + extra.getValue(g)
    fun mainEnd(g: String) = (if (!horizontal && reversed) header(g) else 16.0) + extra.getValue(g)
    lateinit var bandStart: DoubleArray
    val topStack = mutableMapOf<String, Double>()
    val bottomStack = mutableMapOf<String, Double>()
    fun computeMain() {
        topStack.clear(); bottomStack.clear()
        fun top(g: String): Double = topStack.getOrPut(g) {
            mainStart(g) + (children.getValue(g).filter { span.getValue(it).first == span.getValue(g).first }
                .maxOfOrNull(::top) ?: 0.0)
        }
        fun bottom(g: String): Double = bottomStack.getOrPut(g) {
            mainEnd(g) + (children.getValue(g).filter { span.getValue(it).second == span.getValue(g).second }
                .maxOfOrNull(::bottom) ?: 0.0)
        }
        groups.keys.forEach { top(it); bottom(it) }
        val starts = DoubleArray(maxRank + 1)
        var cursor = 0.0
        for (k in 0..maxRank) {
            if (k > 0) {
                cursor += groups.keys.filter { span.getValue(it).second == k - 1 }.maxOfOrNull { bottomStack.getValue(it) } ?: 0.0
                cursor += nodeGap / 2
                cursor += groups.keys.filter { span.getValue(it).first == k }.maxOfOrNull { topStack.getValue(it) } ?: 0.0
            }
            starts[k] = cursor
            cursor += band[k]
        }
        bandStart = starts
    }
    fun frameMain(g: String): Pair<Double, Double> {
        val (lo, hi) = span.getValue(g)
        return bandStart[lo] - topStack.getValue(g) to bandStart[hi] + band[hi] + bottomStack.getValue(g)
    }
    computeMain()
    if (horizontal) {
        // Frame titles run along the main axis here; widen short frames.
        repeat(3) {
            var grown = false
            diagram.subgraphs.forEach { g ->
                val (a, b) = frameMain(g.id)
                val missing = measurer.measure(g.label, flowGroupStyle(g, diagram).text).width + 24 - (b - a)
                if (missing > 1e-6) { extra[g.id] = extra.getValue(g.id) + missing / 2; grown = true }
            }
            if (!grown) return@repeat
            computeMain()
        }
    }

    // Rank space: (cross, main). Normalize, then map to the screen direction.
    fun nodeMain(id: String): Double {
        val u = units.getValue(id)
        return bandStart[u.rank] + (band[u.rank] - u.main) / 2
    }
    val frames = groups.keys.associateWith { g ->
        val (a, b) = frameMain(g)
        SceneRect(crossOf("L\u0000$g"), a, crossOf("R\u0000$g") - crossOf("L\u0000$g"), b - a)
    }
    data class RankPoint(val cross: Double, val main: Double)
    data class Label(val center: RankPoint, val size: SceneSize)
    val paths = mutableMapOf<Int, List<RankPoint>>()
    val labels = mutableMapOf<Int, Label>()
    chains.forEachIndexed { e, chain ->
        if (edges[e].style == FlowEdgeStyle.INVISIBLE) return@forEachIndexed
        val back = e in feedback
        val points = mutableListOf<RankPoint>()
        fun lineOf(id: String): Double {
            val u = units.getValue(id)
            return if (u.real) crossOf(id) + u.cross / 2
            else if (u.label != null) crossOf(id) + if (horizontal) u.label.height + 8.0 else 4.0
            else crossOf(id)
        }
        chain.forEachIndexed { i, id ->
            val u = units.getValue(id)
            val start = bandStart[u.rank]
            val end = start + band[u.rank]
            if (!u.real) {
                val line = lineOf(id)
                points += RankPoint(line, start)
                points += RankPoint(line, end)
                u.label?.let { size ->
                    val center = (start + end) / 2
                    labels[e] = Label(if (horizontal) RankPoint(line - 4.0 - size.height / 2, center)
                        else RankPoint(line + 6.0 + size.width / 2, center), size)
                }
                return@forEachIndexed
            }
            val first = i == 0
            val neighbor = lineOf(chain[if (first) 1 else i - 1])
            val center = crossOf(id) + u.cross / 2
            val nodeStart = nodeMain(id)
            val nodeEnd = nodeStart + u.main
            if (back) {
                // Side ports keep back edges apart from forward edges.
                val outward = if (neighbor >= center) 1.0 else -1.0
                val side = if (outward > 0) crossOf(id) + u.cross else crossOf(id)
                val stub = side + outward * 12.0
                val middle = nodeStart + u.main / 2
                val sequence = listOf(RankPoint(side, middle), RankPoint(stub, middle), RankPoint(stub, if (first) end else start))
                points += if (first) sequence else sequence.reversed()
            } else if (first) {
                points += RankPoint(center, nodeEnd)
                points += RankPoint(center, end)
            } else {
                points += RankPoint(center, start)
                points += RankPoint(center, nodeStart)
            }
        }
        val distinct = points.fold(emptyList<RankPoint>()) { acc, p ->
            val last = acc.lastOrNull()
            if (last != null && kotlin.math.abs(last.cross - p.cross) < 1e-6 && kotlin.math.abs(last.main - p.main) < 1e-6) acc else acc + p
        }
        paths[e] = if (back) distinct.reversed() else distinct
    }

    val crossMin = minOf(units.keys.minOf { crossOf(it) }, frames.values.minOfOrNull { it.x } ?: 0.0,
        labels.values.minOfOrNull { it.center.cross - (if (horizontal) it.size.height else it.size.width) / 2 } ?: 0.0)
    val crossMax = maxOf(units.values.maxOf { crossOf(it.id) + it.cross }, frames.values.maxOfOrNull { it.x + it.width } ?: 0.0,
        labels.values.maxOfOrNull { it.center.cross + (if (horizontal) it.size.height else it.size.width) / 2 } ?: 0.0,
        paths.values.flatten().maxOfOrNull { it.cross } ?: 0.0)
    val mainMin = minOf(0.0, frames.values.minOfOrNull { it.y } ?: 0.0)
    val mainMax = maxOf(bandStart[maxRank] + band[maxRank], frames.values.maxOfOrNull { it.y + it.height } ?: 0.0)
    val crossLow = minOf(crossMin, paths.values.flatten().minOfOrNull { it.cross } ?: crossMin)
    val mainExtent = mainMax - mainMin
    val padding = config.padding
    fun mainScreen(m: Double) = padding + if (reversed) mainExtent - (m - mainMin) else m - mainMin
    fun crossScreen(c: Double) = padding + c - crossLow
    fun screenPoint(p: RankPoint) = if (horizontal) ScenePoint(mainScreen(p.main), crossScreen(p.cross))
        else ScenePoint(crossScreen(p.cross), mainScreen(p.main))
    fun screenRect(r: SceneRect): SceneRect {
        // r is (cross, main, crossSize, mainSize) in rank space.
        val mainLow = if (reversed) mainScreen(r.y + r.height) else mainScreen(r.y)
        return if (horizontal) SceneRect(mainLow, crossScreen(r.x), r.height, r.width)
            else SceneRect(crossScreen(r.x), mainLow, r.width, r.height)
    }
    val rects = linkedMapOf<String, SceneRect>()
    nodeIds.forEach { id ->
        val u = units.getValue(id)
        rects[id] = screenRect(SceneRect(crossOf(id), nodeMain(id), u.cross, u.main))
    }
    val groupRects = linkedMapOf<String, SceneRect>()
    diagram.subgraphs.forEach { g -> groupRects[g.id] = screenRect(frames.getValue(g.id)) }
    val routes = linkedMapOf<Int, FlowReturnRoute>()
    paths.keys.sorted().forEach { e ->
        val points = paths.getValue(e).map(::screenPoint)
        val label = labels[e]
        val origin = label?.let { screenPoint(it.center).let { c -> ScenePoint(c.x, c.y + it.size.height * 0.35) } }
        val minX = minOf(points.minOf { it.x }, if (label != null) origin!!.x - label.size.width / 2 else Double.POSITIVE_INFINITY)
        val maxX = maxOf(points.maxOf { it.x }, if (label != null) origin!!.x + label.size.width / 2 else Double.NEGATIVE_INFINITY)
        val minY = minOf(points.minOf { it.y }, if (label != null) origin!!.y - label.size.height else Double.POSITIVE_INFINITY)
        val maxY = maxOf(points.maxOf { it.y }, origin?.y ?: Double.NEGATIVE_INFINITY)
        routes[e] = FlowReturnRoute(points, origin, SceneRect(minX, minY, maxX - minX, maxY - minY))
    }
    val crossExtent = crossMax - crossLow
    return if (horizontal) FlowPlacement.Result(mainExtent + 2 * padding, crossExtent + 2 * padding, rects, groupRects, routes)
        else FlowPlacement.Result(crossExtent + 2 * padding, mainExtent + 2 * padding, rects, groupRects, routes)
}
