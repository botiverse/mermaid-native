package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.FlowDirection
import build.raft.mermaid.core.FlowNodeShape
import build.raft.mermaid.core.FlowchartDiagram
import build.raft.mermaid.layout.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Layered layout for flat flowcharts that contain loops, following the
 * Sugiyama pipeline used by Mermaid's default dagre renderer:
 *
 * - feedback arcs found by depth-first search are reversed for ranking;
 * - every edge spans two ranks (Mermaid doubles ranks to make room for edge
 *   labels), so each edge owns at least one dummy vertex and a labelled edge
 *   reserves its label box on the middle one;
 * - dummies take part in ordering and x positioning, which gives a back edge
 *   its own lane beside the forward chain instead of a detour around it;
 * - routes are the dummy positions clipped to the node outlines, intended to
 *   be drawn as a basis spline.
 *
 * This is an independent implementation; ordering and positioning use
 * barycenter sweeps and a weighted least-squares placement instead of dagre's
 * exact heuristics, so coordinates are similar to Mermaid's but not identical.
 */
internal data class FlowLayeredLoopResult(
    val width: Double,
    val height: Double,
    val nodes: Map<String, SceneRect>,
    /** Route control points per edge index, from source to target. */
    val paths: Map<Int, List<ScenePoint>>,
    /** Edge label centers per edge index. */
    val labels: Map<Int, ScenePoint>,
)

/** Flat (subgraph-free) flowcharts with a cycle or self-loop use the layered loop layout. */
internal fun flowHasLoops(diagram: FlowchartDiagram): Boolean {
    if (diagram.subgraphs.isNotEmpty() || diagram.nodes.isEmpty()) return false
    val ids = diagram.nodes.map { it.id }.toSet()
    val edges = diagram.edges.filter { it.sourceId in ids && it.targetId in ids }
    if (edges.any { it.sourceId == it.targetId }) return true
    return feedbackEdges(diagram.nodes.map { it.id }, edges.map { it.sourceId to it.targetId }).isNotEmpty()
}

/** Edge indexes (into [edges]) closing a cycle during a declaration-order DFS. */
private fun feedbackEdges(nodes: List<String>, edges: List<Pair<String, String>>): Set<Int> {
    val outgoing = edges.withIndex().filter { it.value.first != it.value.second }.groupBy({ it.value.first }, { it.index })
    val visited = mutableSetOf<String>()
    val active = mutableSetOf<String>()
    val feedback = mutableSetOf<Int>()
    nodes.forEach { root ->
        if (!visited.add(root)) return@forEach
        active.add(root)
        val stack = mutableListOf(root to outgoing[root].orEmpty().iterator())
        while (stack.isNotEmpty()) {
            val (id, iterator) = stack.last()
            if (!iterator.hasNext()) { active.remove(id); stack.removeAt(stack.lastIndex); continue }
            val index = iterator.next()
            val target = edges[index].second
            if (target in active) feedback.add(index)
            else if (visited.add(target)) { active.add(target); stack.add(target to outgoing[target].orEmpty().iterator()) }
        }
    }
    return feedback
}

internal fun flowLayeredLoopLayout(
    diagram: FlowchartDiagram,
    nodeSizes: Map<String, SceneSize>,
    measurer: TextMeasurer,
    config: LayoutConfig,
): FlowLayeredLoopResult? {
    if (!flowHasLoops(diagram)) return null
    val horizontal = diagram.direction == FlowDirection.LR || diagram.direction == FlowDirection.RL
    val ids = diagram.nodes.map { it.id }
    val nodeIndex = ids.withIndex().associate { it.value to it.index }
    val shapes = diagram.nodes.associate { it.id to it.shape }

    // Vertex tables. Sizes are in rank space: width runs across ranks' order axis.
    val width = mutableListOf<Double>()
    val height = mutableListOf<Double>()
    val dummy = mutableListOf<Boolean>()
    val rank = mutableListOf<Int>()
    ids.forEach { id ->
        val size = nodeSizes.getValue(id)
        width += if (horizontal) size.height else size.width
        height += if (horizontal) size.width else size.height
        dummy += false
        rank += 0
    }
    fun labelSize(index: Int): SceneSize? = diagram.edges[index].label?.takeIf { it.isNotEmpty() }?.let {
        val text = measurer.measure(it, TextStyle())
        val box = SceneSize(text.width + LABEL_PAD_X * 2, text.height + LABEL_PAD_Y * 2)
        if (horizontal) SceneSize(box.height, box.width) else box
    }

    val valid = diagram.edges.withIndex().filter { it.value.sourceId in nodeIndex && it.value.targetId in nodeIndex }
    val selfLoops = valid.filter { it.value.sourceId == it.value.targetId }.map { it.index }
    val links = valid.filter { it.value.sourceId != it.value.targetId }
    val feedback = feedbackEdges(ids, links.map { it.value.sourceId to it.value.targetId }).map { links[it].index }.toSet()

    // Oriented edges: forward edges first, then reversed feedback arcs (dagre re-inserts them last).
    data class Oriented(val index: Int, val from: Int, val to: Int, val minLength: Int, val reversed: Boolean)
    val oriented = (links.filter { it.index !in feedback } + links.filter { it.index in feedback }).map { (index, edge) ->
        val reversed = index in feedback
        val from = nodeIndex.getValue(if (reversed) edge.targetId else edge.sourceId)
        val to = nodeIndex.getValue(if (reversed) edge.sourceId else edge.targetId)
        Oriented(index, from, to, 2 * edge.length.coerceIn(1, 10), reversed)
    }

    // Longest-path ranking, then pull each node to the median of its ideal ranks.
    run {
        val incoming = IntArray(ids.size)
        oriented.forEach { incoming[it.to]++ }
        val queue = ArrayDeque((ids.indices).filter { incoming[it] == 0 })
        val out = oriented.groupBy { it.from }
        val order = mutableListOf<Int>()
        while (queue.isNotEmpty()) {
            val v = queue.removeFirst(); order += v
            out[v].orEmpty().forEach { e ->
                rank[e.to] = max(rank[e.to], rank[v] + e.minLength)
                if (--incoming[e.to] == 0) queue.addLast(e.to)
            }
        }
        val into = oriented.groupBy { it.to }
        repeat(4) {
            for (v in order.asReversed() + order) {
                val preds = into[v].orEmpty(); val succs = out[v].orEmpty()
                if (preds.isEmpty() && succs.isEmpty()) continue
                val low = preds.maxOfOrNull { rank[it.from] + it.minLength } ?: Int.MIN_VALUE
                val high = succs.minOfOrNull { rank[it.to] - it.minLength } ?: Int.MAX_VALUE
                if (low > high) continue
                fun cost(r: Int) = preds.sumOf { r - rank[it.from] } + succs.sumOf { rank[it.to] - r }
                val candidates = (preds.map { rank[it.from] + it.minLength } + succs.map { rank[it.to] - it.minLength })
                    .filter { it in low..high }
                val best = candidates.minWithOrNull(compareBy<Int>({ cost(it) }, { abs(it - rank[v]) })) ?: continue
                if (cost(best) < cost(rank[v])) rank[v] = best
            }
        }
        val minimum = rank.minOrNull() ?: 0
        for (i in rank.indices) rank[i] -= minimum
    }

    // Split every edge into unit-span segments through dummy vertices.
    val chains = mutableMapOf<Int, List<Int>>()
    val labelVertex = mutableMapOf<Int, Int>()
    val segmentWeight = mutableMapOf<Pair<Int, Int>, Int>()
    val successors = mutableMapOf<Int, MutableList<Int>>()
    val predecessors = mutableMapOf<Int, MutableList<Int>>()
    oriented.forEach { e ->
        val chain = mutableListOf(e.from)
        val labelRank = (rank[e.from] + rank[e.to]) / 2
        val label = labelSize(e.index)
        for (r in rank[e.from] + 1 until rank[e.to]) {
            val v = width.size
            val box = if (r == labelRank) label else null
            width += box?.width ?: 0.0
            height += box?.height ?: 0.0
            dummy += true
            rank += r
            if (box != null) labelVertex[e.index] = v
            chain += v
        }
        chain += e.to
        chain.zipWithNext().forEach { (a, b) ->
            successors.getOrPut(a) { mutableListOf() } += b
            predecessors.getOrPut(b) { mutableListOf() } += a
            segmentWeight[a to b] = if (dummy[a] && dummy[b]) 8 else if (dummy[a] || dummy[b]) 2 else 1
        }
        if (label != null && e.index !in labelVertex) labelVertex[e.index] = chain[chain.size / 2]
        chains[e.index] = if (e.reversed) chain.reversed() else chain
    }
    // A short edge's only dummy lines up with the endpoint for which it is
    // the sole edge on that side, so the edge meets that node straight on
    // (dagre's vertical alignment gives the same look).
    chains.values.filter { it.size == 3 }.forEach { chain ->
        val (a, d, b) = if (rank[chain[0]] < rank[chain[2]]) chain else chain.reversed()
        val aSingle = successors[a].orEmpty().size == 1
        val bSingle = predecessors[b].orEmpty().size == 1
        if (aSingle && !bSingle) segmentWeight[a to d] = 8
        if (bSingle && !aSingle) segmentWeight[d to b] = 8
    }
    val maxRank = rank.maxOrNull() ?: 0

    // Initial order: depth-first from real nodes in rank order.
    val layers = List(maxRank + 1) { mutableListOf<Int>() }
    run {
        val seen = BooleanArray(width.size)
        ids.indices.sortedBy { rank[it] }.forEach { root ->
            val stack = mutableListOf(root)
            while (stack.isNotEmpty()) {
                val v = stack.removeAt(stack.lastIndex)
                if (seen[v]) continue
                seen[v] = true
                layers[rank[v]] += v
                successors[v].orEmpty().asReversed().forEach { if (!seen[it]) stack += it }
            }
        }
    }

    fun crossings(order: List<List<Int>>): Long {
        val position = IntArray(width.size)
        order.forEach { layer -> layer.forEachIndexed { i, v -> position[v] = i } }
        var total = 0L
        for (r in 0 until maxRank) {
            val segments = order[r].flatMap { u -> successors[u].orEmpty().map { v -> Triple(position[u], position[v], segmentWeight.getValue(u to v)) } }
            for (i in segments.indices) for (j in i + 1 until segments.size) {
                val a = segments[i]; val b = segments[j]
                if ((a.first - b.first).toLong() * (a.second - b.second) < 0) total += a.third.toLong() * b.third
            }
        }
        return total
    }
    var best = layers.map { it.toList() }
    var bestCrossings = crossings(best)
    run {
        val current = layers.map { it.toMutableList() }
        var sinceBest = 0
        var iteration = 0
        while (sinceBest < 4 && iteration < 24) {
            val down = iteration % 2 == 1
            val biasRight = iteration % 4 >= 2
            val range = if (down) (1..maxRank) else (maxRank - 1 downTo 0)
            for (r in range) {
                val fixed = current[if (down) r - 1 else r + 1]
                val position = fixed.withIndex().associate { it.value to it.index }
                val layer = current[r]
                val centers = layer.map { v ->
                    val neighbors = (if (down) predecessors[v] else successors[v]).orEmpty()
                    if (neighbors.isEmpty()) null else {
                        val weights = neighbors.map { u -> if (down) segmentWeight.getValue(u to v) else segmentWeight.getValue(v to u) }
                        neighbors.indices.sumOf { position.getValue(neighbors[it]).toDouble() * weights[it] } / weights.sum()
                    }
                }
                val sortable = layer.indices.filter { centers[it] != null }.sortedWith { a, b ->
                    val c = centers[a]!!.compareTo(centers[b]!!)
                    if (c != 0) c else if (biasRight) b - a else a - b
                }
                val result = arrayOfNulls<Int>(layer.size)
                layer.indices.filter { centers[it] == null }.forEach { result[it] = layer[it] }
                val iterator = sortable.iterator()
                for (i in result.indices) if (result[i] == null) result[i] = layer[iterator.next()]
                layer.clear(); layer.addAll(result.map { it!! })
            }
            val count = crossings(current)
            if (count < bestCrossings) { bestCrossings = count; best = current.map { it.toList() }; sinceBest = 0 } else sinceBest++
            iteration++
        }
    }
    val order = best.map { it.toMutableList() }

    // Self loops: a dummy to the right of the node holds the loop and its label.
    val selfVertex = mutableMapOf<Int, Int>()
    selfLoops.forEach { index ->
        val owner = nodeIndex.getValue(diagram.edges[index].sourceId)
        val label = labelSize(index)
        val v = width.size
        width += label?.width ?: 0.0
        height += label?.height ?: 0.0
        dummy += true
        rank += rank[owner]
        val layer = order[rank[owner]]
        var at = layer.indexOf(owner) + 1
        while (at < layer.size && layer[at] in selfVertex.values) at++
        layer.add(at, v)
        selfVertex[index] = v
    }

    // X positioning: weighted least squares toward neighbors under separation constraints.
    val nodeHalfGap = config.nodeGap / 2
    fun half(v: Int) = if (dummy[v]) EDGE_GAP / 2 else nodeHalfGap
    fun separation(a: Int, b: Int) = (width[a] + width[b]) / 2 + half(a) + half(b)
    val x = DoubleArray(width.size)
    order.forEach { layer ->
        var cursor = 0.0
        layer.forEachIndexed { i, v -> if (i > 0) cursor += separation(layer[i - 1], v); x[v] = cursor }
        val shift = cursor / 2
        layer.forEach { x[it] -= shift }
    }
    val neighborWeights = mutableMapOf<Int, MutableList<Pair<Int, Int>>>()
    segmentWeight.forEach { (pair, w) ->
        neighborWeights.getOrPut(pair.first) { mutableListOf() } += pair.second to w
        neighborWeights.getOrPut(pair.second) { mutableListOf() } += pair.first to w
    }
    val selfOwner = selfVertex.entries.associate { (index, v) -> v to nodeIndex.getValue(diagram.edges[index].sourceId) }
    fun place(layer: List<Int>) {
        if (layer.isEmpty()) return
        val offsets = DoubleArray(layer.size)
        for (i in 1 until layer.size) offsets[i] = offsets[i - 1] + separation(layer[i - 1], layer[i])
        // Pool-adjacent-violators on x - offset keeps order and spacing.
        val blockWeight = mutableListOf<Double>(); val blockSum = mutableListOf<Double>(); val blockCount = mutableListOf<Int>()
        layer.forEachIndexed { i, v ->
            val neighbors = neighborWeights[v].orEmpty()
            val owner = selfOwner[v]
            val (w, target) = when {
                owner != null -> 1.0 to x[owner] + separation(owner, v)
                neighbors.isEmpty() -> 1e-3 to x[v]
                else -> neighbors.sumOf { it.second }.toDouble() to neighbors.sumOf { x[it.first] * it.second } / neighbors.sumOf { it.second }
            }
            blockWeight += w; blockSum += w * (target - offsets[i]); blockCount += 1
            while (blockWeight.size > 1 && blockSum[blockSum.size - 2] / blockWeight[blockWeight.size - 2] > blockSum.last() / blockWeight.last()) {
                val last = blockSum.size - 1
                blockWeight[last - 1] += blockWeight[last]; blockSum[last - 1] += blockSum[last]; blockCount[last - 1] += blockCount[last]
                blockWeight.removeAt(last); blockSum.removeAt(last); blockCount.removeAt(last)
            }
        }
        var i = 0
        blockCount.indices.forEach { b ->
            val value = blockSum[b] / blockWeight[b]
            repeat(blockCount[b]) { x[layer[i]] = value + offsets[i]; i++ }
        }
    }
    repeat(POSITION_SWEEPS) { sweep ->
        val sequence = if (sweep % 2 == 0) order else order.asReversed()
        sequence.forEach(::place)
    }

    // Y positioning: ranks with no members collapse.
    val rankHeight = DoubleArray(maxRank + 1)
    order.forEachIndexed { r, layer -> rankHeight[r] = layer.maxOfOrNull { height[it] } ?: 0.0 }
    val rankCenter = DoubleArray(maxRank + 1)
    run {
        var cursor = 0.0
        var previous = -1
        for (r in 0..maxRank) {
            if (order[r].isEmpty()) continue
            if (previous >= 0) cursor += rankHeight[previous] / 2 + config.nodeGap / 2 + rankHeight[r] / 2
            else cursor = rankHeight[r] / 2
            rankCenter[r] = cursor
            previous = r
        }
    }

    // Rank space to scene space.
    fun scene(px: Double, py: Double): ScenePoint = when (diagram.direction) {
        FlowDirection.LR -> ScenePoint(py, px)
        FlowDirection.RL -> ScenePoint(-py, px)
        FlowDirection.BT -> ScenePoint(px, -py)
        else -> ScenePoint(px, py)
    }
    fun center(v: Int) = scene(x[v], rankCenter[rank[v]])
    val rawNodes = ids.indices.associate { v ->
        val c = center(v); val size = nodeSizes.getValue(ids[v])
        ids[v] to SceneRect(c.x - size.width / 2, c.y - size.height / 2, size.width, size.height)
    }
    val rawPaths = linkedMapOf<Int, List<ScenePoint>>()
    val rawLabels = linkedMapOf<Int, ScenePoint>()
    chains.forEach { (index, chain) ->
        val edge = diagram.edges[index]
        val inner = chain.subList(1, chain.size - 1).map(::center)
        val source = rawNodes.getValue(edge.sourceId); val target = rawNodes.getValue(edge.targetId)
        val start = boundaryPoint(source, shapes.getValue(edge.sourceId), inner.firstOrNull() ?: rectCenter(target))
        val end = boundaryPoint(target, shapes.getValue(edge.targetId), inner.lastOrNull() ?: rectCenter(source))
        rawPaths[index] = listOf(start) + inner + end
        labelVertex[index]?.let { rawLabels[index] = center(it) }
    }
    selfVertex.forEach { (index, v) ->
        val owner = nodeIndex.getValue(diagram.edges[index].sourceId)
        // Rank space: the loop leaves the node's far side and turns back just before its label.
        val nx = x[owner] + width[owner] / 2; val ny = rankCenter[rank[owner]]
        val dx = x[v] - width[v] / 2 - nx; val dy = height[owner] / 2
        rawPaths[index] = listOf(
            scene(nx, ny - dy / 2), scene(nx + dx * 2 / 3, ny - dy), scene(nx + dx, ny),
            scene(nx + dx * 2 / 3, ny + dy), scene(nx, ny + dy / 2),
        )
        if (labelSize(index) != null) rawLabels[index] = scene(x[v], ny)
    }

    // Translate everything into the padded canvas.
    val labelBoxes = rawLabels.map { (index, c) ->
        val text = measurer.measure(diagram.edges[index].label!!, TextStyle())
        SceneRect(c.x - text.width / 2 - LABEL_PAD_X, c.y - text.height / 2 - LABEL_PAD_Y, text.width + 2 * LABEL_PAD_X, text.height + 2 * LABEL_PAD_Y)
    }
    val xs = rawNodes.values.flatMap { listOf(it.x, it.x + it.width) } + rawPaths.values.flatten().map { it.x } + labelBoxes.flatMap { listOf(it.x, it.x + it.width) }
    val ys = rawNodes.values.flatMap { listOf(it.y, it.y + it.height) } + rawPaths.values.flatten().map { it.y } + labelBoxes.flatMap { listOf(it.y, it.y + it.height) }
    val dx = config.padding - xs.min(); val dy = config.padding - ys.min()
    fun move(p: ScenePoint) = ScenePoint(snap(p.x + dx), snap(p.y + dy))
    return FlowLayeredLoopResult(
        width = snap(xs.max() - xs.min() + 2 * config.padding),
        height = snap(ys.max() - ys.min() + 2 * config.padding),
        nodes = rawNodes.mapValues { (_, r) -> SceneRect(snap(r.x + dx), snap(r.y + dy), r.width, r.height) },
        paths = rawPaths.entries.sortedBy { it.key }.associate { (index, points) -> index to points.map(::move) },
        labels = rawLabels.mapValues { (_, p) -> move(p) },
    )
}

private const val EDGE_GAP = 20.0
private const val LABEL_PAD_X = 4.0
private const val LABEL_PAD_Y = 2.0
private const val POSITION_SWEEPS = 60

private fun snap(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0

private fun rectCenter(r: SceneRect) = ScenePoint(r.x + r.width / 2, r.y + r.height / 2)

/** Where the segment from the node center toward [toward] leaves the node outline. */
internal fun boundaryPoint(r: SceneRect, shape: FlowNodeShape, toward: ScenePoint): ScenePoint {
    val c = rectCenter(r)
    val dx = toward.x - c.x; val dy = toward.y - c.y
    if (abs(dx) < 1e-9 && abs(dy) < 1e-9) return c
    val hw = r.width / 2; val hh = r.height / 2
    val t = when (shape) {
        FlowNodeShape.DIAMOND -> 1.0 / (abs(dx) / hw + abs(dy) / hh)
        FlowNodeShape.CIRCLE, FlowNodeShape.DOUBLE_CIRCLE, FlowNodeShape.ELLIPSE ->
            1.0 / sqrt((dx * dx) / (hw * hw) + (dy * dy) / (hh * hh))
        else -> min(if (abs(dx) < 1e-9) Double.MAX_VALUE else hw / abs(dx), if (abs(dy) < 1e-9) Double.MAX_VALUE else hh / abs(dy))
    }
    return ScenePoint(c.x + dx * min(t, 1.0), c.y + dy * min(t, 1.0))
}
