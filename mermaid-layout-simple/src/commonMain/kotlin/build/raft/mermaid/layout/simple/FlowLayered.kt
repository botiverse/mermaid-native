package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.FlowDirection
import build.raft.mermaid.core.FlowNodeShape
import build.raft.mermaid.core.FlowSubgraph
import build.raft.mermaid.core.FlowchartDiagram
import build.raft.mermaid.layout.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Layered flowchart layout following the Sugiyama pipeline of Mermaid's
 * default dagre renderer:
 *
 * - feedback arcs found by depth-first search are reversed for ranking;
 * - every edge spans two ranks (Mermaid doubles ranks to make room for edge
 *   labels), so each edge owns at least one dummy vertex and a labelled edge
 *   reserves its label box on the middle one;
 * - dummies take part in ordering and x positioning, which gives long and
 *   back edges their own lanes instead of detours around the nodes;
 * - routes are the dummy positions clipped to the node outlines, drawn as a
 *   basis spline (or the edge's `interpolate` curve).
 *
 * Subgraphs follow Mermaid's compound handling. A subgraph that no edge
 * crosses (edges to the subgraph itself do not count) is laid out on its own,
 * in its declared direction or else the direction Mermaid toggles to, and
 * placed as one box. Other subgraphs stay in the surrounding layout like
 * dagre's nesting graph: left and right border vertices on every rank they
 * span keep their members contiguous, sibling frames keep one left-to-right
 * order on every rank, and extra rank space holds the frame title.
 *
 * This is an independent implementation; ordering and positioning use
 * barycenter sweeps and a weighted least-squares placement instead of dagre's
 * exact heuristics, so coordinates are similar to Mermaid's but not identical.
 */
internal data class FlowLayeredResult(
    val width: Double,
    val height: Double,
    val nodes: Map<String, SceneRect>,
    /** Subgraph frames. */
    val groups: Map<String, SceneRect>,
    /** Route control points per edge index, from source to target. */
    val paths: Map<Int, List<ScenePoint>>,
    /** Edge label centers per edge index. */
    val labels: Map<Int, ScenePoint>,
)

/**
 * Layered layout for a flowchart, or null when its subgraph structure is
 * outside what the layered layout supports (a node and a subgraph sharing an
 * id, cyclic subgraph parents, or edges between a subgraph and its own
 * members).
 */
internal fun flowLayeredLayout(
    diagram: FlowchartDiagram,
    nodeSizes: Map<String, SceneSize>,
    measurer: TextMeasurer,
    config: LayoutConfig,
    titleSizes: Map<String, SceneSize> = emptyMap(),
): FlowLayeredResult? {
    if (diagram.nodes.isEmpty()) return null
    val nodeIds = diagram.nodes.map { it.id }.distinct()
    val nodeSet = nodeIds.toSet()
    val groups = LinkedHashMap<String, FlowSubgraph>()
    diagram.subgraphs.forEach { groups.getOrPut(it.id) { it } }
    if (groups.keys.any { it in nodeSet }) return null
    val parent = groups.values.associate { it.id to it.parentId?.takeIf { p -> p in groups && p != it.id } }
    for (id in groups.keys) {
        val seen = mutableSetOf(id)
        var p = parent[id]
        while (p != null) { if (!seen.add(p)) return null; p = parent[p] }
    }
    fun groupChain(g: String?): List<String> = generateSequence(g) { parent[it] }.toList()
    val depth = groups.keys.associateWith { groupChain(it).size }
    // The deepest subgraph that lists the node owns it.
    val owner = nodeIds.associateWith { id ->
        diagram.subgraphs.filter { id in it.nodeIds && it.id in groups }.maxByOrNull { depth.getValue(it.id) }?.id
    }
    fun inside(endpoint: String, g: String): Boolean =
        if (endpoint in groups) endpoint != g && g in groupChain(endpoint) else g in groupChain(owner[endpoint])
    val linked = diagram.edges.withIndex().filter { (_, e) ->
        (e.sourceId in nodeSet || e.sourceId in groups) && (e.targetId in nodeSet || e.targetId in groups)
    }
    // An edge between a subgraph and its own member (or the subgraph itself)
    // takes no part in the layout; it is drawn afterwards to the frame side.
    val (inward, edges) = linked.partition { (_, e) ->
        (e.sourceId in groups && (e.sourceId == e.targetId || inside(e.targetId, e.sourceId))) ||
            (e.targetId in groups && inside(e.sourceId, e.targetId))
    }
    // Subgraphs no edge crosses are laid out separately, like Mermaid's cluster extraction.
    val isolated = groups.keys.filter { g -> edges.none { (_, e) -> inside(e.sourceId, g) != inside(e.targetId, g) } }.toSet()
    fun isolatedAncestor(chain: List<String>): String? = chain.firstOrNull { it in isolated }
    val levelOfNode = nodeIds.associateWith { isolatedAncestor(groupChain(owner[it])) }
    val levelOfGroup = groups.keys.associateWith { isolatedAncestor(groupChain(parent[it])) }
    fun strictLevels(endpoint: String): List<String?> =
        (if (endpoint in groups) groupChain(parent[endpoint]) else groupChain(owner[endpoint])).filter { it in isolated } + listOf(null)
    val edgeLevel = edges.associate { (index, e) ->
        val targetLevels = strictLevels(e.targetId).toSet()
        index to strictLevels(e.sourceId).first { it in targetLevels }
    }
    val firstNode = nodeIds.withIndex().associate { it.value to it.index }
    fun orderKey(leaf: String): Int = if (leaf in groups) {
        nodeIds.filter { inside(it, leaf) }.minOfOrNull { firstNode.getValue(it) } ?: (nodeIds.size + groups.keys.indexOf(leaf))
    } else firstNode.getValue(leaf)
    val shapes = diagram.nodes.associate { it.id to it.shape }
    fun titleBand(g: String) = (titleSizes[g]?.height ?: 14.0) + TITLE_GAP
    fun titleWidth(g: String) = titleSizes[g]?.width ?: 0.0

    class Placed(val level: LevelResult, val boxes: Map<String, Placed>)

    fun solve(level: String?, direction: FlowDirection): Placed? {
        val leafNodes = nodeIds.filter { levelOfNode[it] == level }
        val leafBoxes = groups.keys.filter { it in isolated && it != level && levelOfGroup[it] == level }
        val boxes = linkedMapOf<String, Placed>()
        for (box in leafBoxes) {
            val own = groups.getValue(box).direction
            val inner = own ?: if (direction == FlowDirection.TB || direction == FlowDirection.TD) FlowDirection.LR else FlowDirection.TB
            boxes[box] = solve(box, inner) ?: return null
        }
        val leaves = (leafNodes + leafBoxes).sortedBy(::orderKey).map { id ->
            val box = boxes[id]
            if (box == null) LeafSpec(id, nodeSizes.getValue(id), shapes.getValue(id))
            else {
                val width = max(box.level.width + 2 * BOX_PAD, titleWidth(id) + 2 * TITLE_MARGIN)
                LeafSpec(id, SceneSize(width, box.level.height + titleBand(id) + BOX_PAD), FlowNodeShape.RECTANGLE)
            }
        }
        val clusters = groups.keys.filter { it !in isolated && levelOfGroup[it] == level }
        val clusterSet = clusters.toSet()
        val clusterSpecs = clusters.map { ClusterSpec(it, parent[it]?.takeIf { p -> p in clusterSet }, titleWidth(it), titleBand(it)) }
        val leafCluster = leaves.associate { leaf ->
            leaf.id to (if (leaf.id in groups) parent[leaf.id] else owner[leaf.id])?.takeIf { it in clusterSet }
        }
        val leafSet = leaves.map { it.id }.toSet()
        fun boxOf(chain: List<String>): String? = chain.firstOrNull { it in leafSet }
        fun representative(cluster: String): String? = leaves.firstOrNull { leaf ->
            cluster in groupChain(leafCluster[leaf.id])
        }?.id
        val specs = mutableListOf<EdgeSpec>()
        for ((index, e) in edges) {
            if (edgeLevel[index] != level) continue
            fun resolve(endpoint: String): Pair<String, String?>? = when {
                endpoint in leafSet -> endpoint to null
                endpoint in clusterSet -> representative(endpoint)?.let { it to endpoint }
                endpoint in groups -> boxOf(groupChain(endpoint))?.let { it to null }
                else -> boxOf(groupChain(owner[endpoint]))?.let { it to null }
            }
            val from = resolve(e.sourceId) ?: return null
            val to = resolve(e.targetId) ?: return null
            if (from.first == to.first && e.sourceId != e.targetId) return null
            val label = e.label?.takeIf { it.isNotEmpty() }?.let {
                val text = if (e.labelType == "markdown") flowMarkdownSize(flowEdgeMarkdown(it, measurer))
                    else flowEdgeLabelSize(flowEdgeLabelLines(it, measurer), measurer)
                SceneSize(text.width + LABEL_PAD_X * 2, text.height + LABEL_PAD_Y * 2)
            }
            specs += EdgeSpec(index, from.first, to.first, e.length.coerceIn(1, 10), label, from.second, to.second)
        }
        val result = layoutLevel(direction, leaves, leafCluster, clusterSpecs, specs, config.nodeGap)
        return Placed(result, boxes)
    }

    val root = solve(null, diagram.direction) ?: return null
    val nodes = linkedMapOf<String, SceneRect>()
    val frames = linkedMapOf<String, SceneRect>()
    val paths = linkedMapOf<Int, List<ScenePoint>>()
    val labels = linkedMapOf<Int, ScenePoint>()
    fun place(placed: Placed, dx: Double, dy: Double) {
        val level = placed.level
        level.leaves.forEach { (id, r) ->
            val moved = SceneRect(r.x + dx, r.y + dy, r.width, r.height)
            val box = placed.boxes[id]
            if (box == null) nodes[id] = moved
            else {
                frames[id] = moved
                place(box, moved.x + (moved.width - box.level.width) / 2, moved.y + titleBand(id))
            }
        }
        level.clusters.forEach { (id, r) -> frames[id] = SceneRect(r.x + dx, r.y + dy, r.width, r.height) }
        level.paths.forEach { (index, points) -> paths[index] = points.map { ScenePoint(it.x + dx, it.y + dy) } }
        level.labels.forEach { (index, p) -> labels[index] = ScenePoint(p.x + dx, p.y + dy) }
    }
    place(root, 0.0, 0.0)
    for ((index, e) in inward) {
        if (e.sourceId == e.targetId) continue
        val outward = e.targetId in groups && inside(e.sourceId, e.targetId)
        val memberId = if (outward) e.sourceId else e.targetId
        val member = nodes[memberId] ?: frames[memberId] ?: continue
        val frame = frames[if (outward) e.targetId else e.sourceId] ?: continue
        val center = rectCenter(member)
        val shape = if (memberId in nodes) shapes.getValue(memberId) else FlowNodeShape.RECTANGLE
        // Straight to the nearest frame side that no other node blocks.
        val sides = listOf(
            ScenePoint(center.x, frame.y), ScenePoint(center.x, frame.y + frame.height),
            ScenePoint(frame.x, center.y), ScenePoint(frame.x + frame.width, center.y),
        ).sortedBy { abs(it.x - center.x) + abs(it.y - center.y) }
        fun blocked(to: ScenePoint) = nodes.any { (id, r) ->
            id != memberId && (1..19).any { i ->
                val t = i / 20.0
                val p = ScenePoint(center.x + (to.x - center.x) * t, center.y + (to.y - center.y) * t)
                p.x > r.x && p.x < r.x + r.width && p.y > r.y && p.y < r.y + r.height
            }
        }
        val onFrame = sides.firstOrNull { !blocked(it) } ?: sides.first()
        val onMember = boundaryPoint(member, shape, onFrame)
        val (start, end) = if (outward) onMember to onFrame else onFrame to onMember
        if (abs(start.x - end.x) + abs(start.y - end.y) < 1e-6) continue
        val middle = ScenePoint((start.x + end.x) / 2, (start.y + end.y) / 2)
        paths[index] = listOf(start, middle, end)
        if (!e.label.isNullOrEmpty()) labels[index] = middle
    }
    // Every subgraph gets a frame, empty or not, in declaration order.
    val pad = config.padding
    fun move(p: ScenePoint) = ScenePoint(snap(p.x + pad), snap(p.y + pad))
    fun moveRect(r: SceneRect) = SceneRect(snap(r.x + pad), snap(r.y + pad), r.width, r.height)
    return FlowLayeredResult(
        width = snap(root.level.width + 2 * pad),
        height = snap(root.level.height + 2 * pad),
        nodes = nodeIds.filter { it in nodes }.associateWith { moveRect(nodes.getValue(it)) },
        groups = groups.keys.filter { it in frames }.associateWith { moveRect(frames.getValue(it)) },
        paths = paths.entries.sortedBy { it.key }.associate { (index, points) -> index to points.map(::move) },
        labels = labels.mapValues { (_, p) -> move(p) },
    )
}

private class LeafSpec(val id: String, val size: SceneSize, val shape: FlowNodeShape)
private class ClusterSpec(val id: String, val parent: String?, val titleWidth: Double, val titleBand: Double)
private class EdgeSpec(
    val index: Int, val from: String, val to: String, val length: Int, val label: SceneSize?,
    val clipFrom: String?, val clipTo: String?,
)
private class LevelResult(
    val width: Double, val height: Double,
    val leaves: Map<String, SceneRect>, val clusters: Map<String, SceneRect>,
    val paths: Map<Int, List<ScenePoint>>, val labels: Map<Int, ScenePoint>,
)

private const val NODE = 0
private const val DUMMY = 1
private const val LEFT = 2
private const val RIGHT = 3

/** One layered graph; coordinates start at the origin, without canvas padding. */
private fun layoutLevel(
    direction: FlowDirection,
    leaves: List<LeafSpec>,
    leafCluster: Map<String, String?>,
    clusterSpecs: List<ClusterSpec>,
    edgeSpecs: List<EdgeSpec>,
    nodeGap: Double,
): LevelResult {
    val horizontal = direction == FlowDirection.LR || direction == FlowDirection.RL
    val leafIndex = leaves.withIndex().associate { it.value.id to it.index }
    val clusterIndex = clusterSpecs.withIndex().associate { it.value.id to it.index }
    val clusterParent = IntArray(clusterSpecs.size) { c -> clusterSpecs[c].parent?.let { clusterIndex.getValue(it) } ?: -1 }
    fun clusterChain(c: Int): List<Int> = generateSequence(c.takeIf { it >= 0 }) { clusterParent[it].takeIf { p -> p >= 0 } }.toList()

    // Vertex tables. Sizes are in rank space: width runs along the order axis.
    val width = mutableListOf<Double>()
    val height = mutableListOf<Double>()
    val kind = mutableListOf<Int>()
    val owner = mutableListOf<Int>()
    val rank = mutableListOf<Int>()
    fun addVertex(w: Double, h: Double, k: Int, c: Int, r: Int): Int {
        width += w; height += h; kind += k; owner += c; rank += r
        return width.size - 1
    }
    leaves.forEach { leaf ->
        addVertex(if (horizontal) leaf.size.height else leaf.size.width, if (horizontal) leaf.size.width else leaf.size.height,
            NODE, leafCluster[leaf.id]?.let { clusterIndex.getValue(it) } ?: -1, 0)
    }
    fun rankSize(size: SceneSize?): SceneSize? = size?.let { if (horizontal) SceneSize(it.height, it.width) else it }

    val selfLoops = edgeSpecs.filter { it.from == it.to }
    val links = edgeSpecs.filter { it.from != it.to }
    val feedback = feedbackEdges(leaves.map { it.id }, links.map { it.from to it.to }).map { links[it].index }.toSet()

    // Oriented edges: forward edges first, then reversed feedback arcs (dagre re-inserts them last).
    class Oriented(val spec: EdgeSpec, val from: Int, val to: Int, val minLength: Int, val reversed: Boolean)
    val oriented = (links.filter { it.index !in feedback } + links.filter { it.index in feedback }).map { spec ->
        val reversed = spec.index in feedback
        val from = leafIndex.getValue(if (reversed) spec.to else spec.from)
        val to = leafIndex.getValue(if (reversed) spec.from else spec.to)
        Oriented(spec, from, to, 2 * spec.length, reversed)
    }

    // Longest-path ranking, then pull each node to the median of its ideal ranks.
    // An edge from (to) a subgraph also keeps the target below (source above)
    // every member, so it meets the frame instead of passing beside it; such
    // constraints are skipped where they would close a cycle.
    class RankEdge(val from: Int, val to: Int, val minLength: Int, val attract: Boolean)
    val rankEdges = oriented.map { RankEdge(it.from, it.to, it.minLength, true) }.toMutableList()
    run {
        val members = clusterSpecs.indices.associateWith { c -> leaves.indices.filter { c in clusterChain(owner[it]) } }
        fun reaches(from: Int, to: Int): Boolean {
            val out = rankEdges.groupBy({ it.from }, { it.to })
            val seen = mutableSetOf(from); val stack = mutableListOf(from)
            while (stack.isNotEmpty()) { val v = stack.removeAt(stack.lastIndex); if (v == to) return true; out[v].orEmpty().forEach { if (seen.add(it)) stack += it } }
            return false
        }
        oriented.filter { !it.reversed }.forEach { e ->
            val extra = (e.spec.clipFrom?.let { c -> members.getValue(clusterIndex.getValue(c)).map { it to e.to } }.orEmpty() +
                e.spec.clipTo?.let { c -> members.getValue(clusterIndex.getValue(c)).map { e.from to it } }.orEmpty())
            extra.filter { (a, b) -> a != b }.forEach { (a, b) -> if (!reaches(b, a)) rankEdges += RankEdge(a, b, e.minLength, false) }
        }
    }
    run {
        val incoming = IntArray(leaves.size)
        rankEdges.forEach { incoming[it.to]++ }
        val queue = ArrayDeque(leaves.indices.filter { incoming[it] == 0 })
        val out = rankEdges.groupBy { it.from }
        val order = mutableListOf<Int>()
        while (queue.isNotEmpty()) {
            val v = queue.removeFirst(); order += v
            out[v].orEmpty().forEach { e ->
                rank[e.to] = max(rank[e.to], rank[v] + e.minLength)
                if (--incoming[e.to] == 0) queue.addLast(e.to)
            }
        }
        val into = rankEdges.groupBy { it.to }
        repeat(4) {
            for (v in order.asReversed() + order) {
                val allPreds = into[v].orEmpty(); val allSuccs = out[v].orEmpty()
                val preds = allPreds.filter { it.attract }; val succs = allSuccs.filter { it.attract }
                if (preds.isEmpty() && succs.isEmpty()) continue
                val low = allPreds.maxOfOrNull { rank[it.from] + it.minLength } ?: Int.MIN_VALUE
                val high = allSuccs.minOfOrNull { rank[it.to] - it.minLength } ?: Int.MAX_VALUE
                if (low > high) continue
                fun cost(r: Int) = preds.sumOf { r - rank[it.from] } + succs.sumOf { rank[it.to] - r }
                val candidates = (preds.map { rank[it.from] + it.minLength } + succs.map { rank[it.to] - it.minLength } + listOf(low, high).filter { it != Int.MIN_VALUE && it != Int.MAX_VALUE })
                    .filter { it in low..high }
                val best = candidates.minWithOrNull(compareBy<Int>({ cost(it) }, { abs(it - rank[v]) })) ?: continue
                if (cost(best) < cost(rank[v])) rank[v] = best
            }
        }
        val minimum = rank.minOrNull() ?: 0
        for (i in rank.indices) rank[i] -= minimum
    }

    // Rank spans of the in-layout subgraphs; a self loop occupies the rank after its node.
    val loopRank = selfLoops.associate { it.index to rank[leafIndex.getValue(it.from)] + 1 }
    val spanLow = IntArray(clusterSpecs.size) { Int.MAX_VALUE }
    val spanHigh = IntArray(clusterSpecs.size) { Int.MIN_VALUE }
    fun cover(c: Int, low: Int, high: Int) = clusterChain(c).forEach { spanLow[it] = min(spanLow[it], low); spanHigh[it] = max(spanHigh[it], high) }
    leaves.indices.forEach { v -> cover(owner[v], rank[v], rank[v]) }
    selfLoops.forEach { cover(owner[leafIndex.getValue(it.from)], loopRank.getValue(it.index), loopRank.getValue(it.index)) }
    val liveCluster = BooleanArray(clusterSpecs.size) { spanLow[it] <= spanHigh[it] }
    fun spans(c: Int, r: Int) = liveCluster[c] && r >= spanLow[c] && r <= spanHigh[c]

    // Split every edge into unit-span segments through dummy vertices.
    val chains = mutableMapOf<Int, List<Int>>()
    val labelVertex = mutableMapOf<Int, Int>()
    val segmentWeight = mutableMapOf<Pair<Int, Int>, Int>()
    val successors = mutableMapOf<Int, MutableList<Int>>()
    val predecessors = mutableMapOf<Int, MutableList<Int>>()
    fun link(a: Int, b: Int, weight: Int) {
        successors.getOrPut(a) { mutableListOf() } += b
        predecessors.getOrPut(b) { mutableListOf() } += a
        segmentWeight[a to b] = weight
    }
    // An edge from a subgraph starts at its lowest member, an edge into one ends
    // at its highest member; the route is cut at the frame afterwards.
    fun extreme(cluster: String?, current: Int, lowest: Boolean, bound: (Int) -> Boolean): Int {
        if (cluster == null) return current
        val c = clusterIndex.getValue(cluster)
        val candidates = leaves.indices.filter { c in clusterChain(owner[it]) && bound(it) }
        return (if (lowest) candidates.maxByOrNull { rank[it] } else candidates.minByOrNull { rank[it] }) ?: current
    }
    oriented.forEach { e ->
        var from = e.from; var to = e.to
        if (!e.reversed) {
            from = extreme(e.spec.clipFrom, from, true) { rank[it] + 2 <= rank[to] }
            to = extreme(e.spec.clipTo, to, false) { rank[from] + 2 <= rank[it] }
        }
        val chain = mutableListOf(from)
        val labelRank = (rank[from] + rank[to]) / 2
        val label = rankSize(e.spec.label)
        val upper = clusterChain(owner[from]); val lower = clusterChain(owner[to])
        for (r in rank[from] + 1 until rank[to]) {
            // Like dagre's parent dummy chains: a dummy stays inside the subgraphs that span its rank.
            val a = upper.firstOrNull { spans(it, r) }; val b = lower.firstOrNull { spans(it, r) }
            val cluster = when {
                a == null -> b ?: -1
                b == null -> a
                a in clusterChain(b) -> b
                b in clusterChain(a) -> a
                r - rank[from] <= rank[to] - r -> a
                else -> b
            }
            val box = if (r == labelRank) label else null
            val v = addVertex(box?.width ?: 0.0, box?.height ?: 0.0, DUMMY, cluster, r)
            if (box != null) labelVertex[e.spec.index] = v
            chain += v
        }
        chain += to
        chain.zipWithNext().forEach { (a, b) ->
            val dummies = (if (kind[a] == DUMMY) 1 else 0) + (if (kind[b] == DUMMY) 1 else 0)
            link(a, b, if (dummies == 2) 8 else if (dummies == 1) 2 else 1)
        }
        if (label != null && e.spec.index !in labelVertex) labelVertex[e.spec.index] = chain[chain.size / 2]
        chains[e.spec.index] = if (e.reversed) chain.reversed() else chain
    }
    // A short edge's only dummy lines up with the endpoint for which it is
    // the sole edge on that side, so the edge meets that node straight on
    // (dagre's vertical alignment gives the same look).
    chains.values.filter { it.size == 3 }.forEach { chain ->
        val (a, d, b) = if (rank[chain[0]] < rank[chain[2]]) chain else chain.reversed()
        val aSingle = successors[a].orEmpty().size == 1
        val bSingle = predecessors[b].orEmpty().size == 1
        if (aSingle && !bSingle) segmentWeight[a to d] = ALIGN_WEIGHT
        if (bSingle && !aSingle) segmentWeight[d to b] = ALIGN_WEIGHT
        if (aSingle && bSingle) { segmentWeight[a to d] = 8; segmentWeight[d to b] = 8 }
    }
    // Self loops: a vertex on the next rank holds the loop and its label, tied to its node.
    val loopVertex = mutableMapOf<Int, Int>()
    selfLoops.forEach { spec ->
        val node = leafIndex.getValue(spec.from)
        val label = rankSize(spec.label)
        val v = addVertex(max(label?.width ?: 0.0, LOOP_WIDTH), (label?.height ?: 0.0) + LOOP_DEPTH, DUMMY, owner[node], loopRank.getValue(spec.index))
        link(node, v, ALIGN_WEIGHT)
        loopVertex[spec.index] = v
    }
    // Border vertices on every rank a subgraph spans keep its members contiguous.
    val borders = mutableListOf<Int>()
    clusterSpecs.indices.filter { liveCluster[it] }.forEach { c ->
        for (r in spanLow[c]..spanHigh[c]) { borders += addVertex(0.0, 0.0, LEFT, c, r); borders += addVertex(0.0, 0.0, RIGHT, c, r) }
    }
    val vertexCount = width.size
    val maxRank = rank.maxOrNull() ?: 0

    // Ordering. The cluster tree constrains every layer: a subgraph's
    // vertices stay between its borders, and sibling subgraphs keep the same
    // left-to-right order on every rank.
    val clusterKey = DoubleArray(clusterSpecs.size)
    fun childOf(c: Int, d: Int): Int {
        var x = d
        while (x >= 0 && clusterParent[x] != c) x = clusterParent[x]
        return if (d == c) -1 else x
    }
    fun regroup(layer: MutableList<Int>, key: (Int) -> Double) {
        fun arrange(c: Int, members: List<Int>): List<Int> {
            var left = -1; var right = -1
            val items = LinkedHashMap<Int, MutableList<Int>>()
            for (v in members) {
                if (kind[v] == LEFT && owner[v] == c) { left = v; continue }
                if (kind[v] == RIGHT && owner[v] == c) { right = v; continue }
                val child = if (owner[v] == c) -1 else childOf(c, owner[v])
                items.getOrPut(if (child < 0) v else -(child + 2)) { mutableListOf() } += v
            }
            fun itemKey(k: Int, vs: List<Int>): Double = if (k >= 0) key(k) else {
                val inner = vs.filter { kind[it] == NODE || kind[it] == DUMMY }.ifEmpty { vs }
                inner.sumOf(key) / inner.size
            }
            val sorted = items.entries.sortedBy { itemKey(it.key, it.value) }.toMutableList()
            val slots = sorted.indices.filter { sorted[it].key < 0 }
            val byGlobal = slots.map { sorted[it] }.sortedBy { clusterKey[-it.key - 2] }
            slots.forEachIndexed { i, slot -> sorted[slot] = byGlobal[i] }
            val result = mutableListOf<Int>()
            if (left >= 0) result += left
            sorted.forEach { (k, vs) -> if (k >= 0) result += k else result += arrange(-k - 2, vs) }
            if (right >= 0) result += right
            return result
        }
        val arranged = arrange(-1, layer.toList())
        layer.clear(); layer.addAll(arranged)
    }
    fun updateClusterKeys(order: List<List<Int>>) {
        val sum = DoubleArray(clusterSpecs.size); val count = IntArray(clusterSpecs.size)
        order.forEach { layer -> layer.forEachIndexed { i, v -> clusterChain(owner[v]).forEach { c -> sum[c] += i.toDouble(); count[c]++ } } }
        for (c in clusterSpecs.indices) clusterKey[c] = if (count[c] == 0) 0.0 else sum[c] / count[c]
    }

    // Initial order: depth-first from real nodes in rank order.
    val layers = List(maxRank + 1) { mutableListOf<Int>() }
    run {
        val seen = BooleanArray(vertexCount)
        leaves.indices.sortedBy { rank[it] }.forEach { root ->
            val stack = mutableListOf(root)
            while (stack.isNotEmpty()) {
                val v = stack.removeAt(stack.lastIndex)
                if (seen[v]) continue
                seen[v] = true
                layers[rank[v]] += v
                successors[v].orEmpty().asReversed().forEach { if (!seen[it]) stack += it }
            }
        }
        for (v in 0 until vertexCount) if (!seen[v]) layers[rank[v]] += v
    }
    fun regroupAll(order: List<MutableList<Int>>) {
        updateClusterKeys(order)
        order.forEach { layer -> val position = layer.withIndex().associate { it.value to it.index.toDouble() }; regroup(layer) { position.getValue(it) } }
    }
    if (clusterSpecs.isNotEmpty()) regroupAll(layers)

    fun crossings(order: List<List<Int>>): Long {
        val position = IntArray(vertexCount)
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
                if (clusterSpecs.isNotEmpty()) {
                    val slot = layer.withIndex().associate { it.value to it.index.toDouble() }
                    regroup(layer) { slot.getValue(it) }
                }
            }
            if (clusterSpecs.isNotEmpty()) regroupAll(current)
            val count = crossings(current)
            if (count < bestCrossings) { bestCrossings = count; best = current.map { it.toList() }; sinceBest = 0 } else sinceBest++
            iteration++
        }
    }
    val order = best.map { it.toMutableList() }

    // X positioning: weighted least squares toward neighbors under separation constraints.
    fun within(v: Int, c: Int) = c in clusterChain(owner[v])
    fun outerMargin(v: Int) = when (kind[v]) { NODE -> nodeGap / 2; DUMMY -> EDGE_GAP / 2; else -> BORDER_GAP }
    fun innerPad(border: Int, v: Int): Double {
        val c = owner[border]
        // Horizontal layouts put the frame title on the order axis, before the first member.
        if (horizontal && kind[border] == LEFT) return clusterSpecs[c].titleBand + (if (kind[v] == NODE) 0.0 else CLUSTER_PAD_INNER)
        return when (kind[v]) { NODE -> CLUSTER_PAD_NODE; DUMMY -> CLUSTER_PAD_INNER; else -> CLUSTER_PAD_INNER }
    }
    fun separation(a: Int, b: Int): Double {
        val base = (width[a] + width[b]) / 2
        if (kind[a] == LEFT && within(b, owner[a])) return base + innerPad(a, b)
        if (kind[b] == RIGHT && within(a, owner[b])) return base + innerPad(b, a)
        return base + outerMargin(a) + outerMargin(b)
    }
    val x = DoubleArray(vertexCount)
    fun initialX() = order.forEach { layer ->
        var cursor = 0.0
        layer.forEachIndexed { i, v -> if (i > 0) cursor += separation(layer[i - 1], v); x[v] = cursor }
        val shift = cursor / 2
        layer.forEach { x[it] -= shift }
    }
    val sharedLeft = DoubleArray(clusterSpecs.size) { Double.MAX_VALUE }
    val sharedRight = DoubleArray(clusterSpecs.size) { -Double.MAX_VALUE }
    fun updateShared() {
        sharedLeft.fill(Double.MAX_VALUE); sharedRight.fill(-Double.MAX_VALUE)
        // The tightest frame around the members; borders are pulled to it.
        order.forEach { layer -> layer.forEachIndexed { i, v ->
            val c = owner[v]
            if (kind[v] == LEFT) sharedLeft[c] = min(sharedLeft[c], x[layer[i + 1]] - separation(v, layer[i + 1]))
            if (kind[v] == RIGHT) sharedRight[c] = max(sharedRight[c], x[layer[i - 1]] + separation(layer[i - 1], v))
        } }
    }
    val neighborWeights = mutableMapOf<Int, MutableList<Pair<Int, Int>>>()
    // A vertex inside a frame does not follow a neighbor that must stay
    // outside that frame on its own rank; following it would drag the frame
    // after a neighbor the frame itself keeps pushing away.
    fun follows(v: Int, other: Int) = clusterChain(owner[v]).none { c -> !within(other, c) && spans(c, rank[other]) }
    segmentWeight.forEach { (pair, w) ->
        if (follows(pair.first, pair.second)) neighborWeights.getOrPut(pair.first) { mutableListOf() } += pair.second to w
        if (follows(pair.second, pair.first)) neighborWeights.getOrPut(pair.second) { mutableListOf() } += pair.first to w
    }
    fun place(layer: List<Int>) {
        if (layer.isEmpty()) return
        val offsets = DoubleArray(layer.size)
        for (i in 1 until layer.size) offsets[i] = offsets[i - 1] + separation(layer[i - 1], layer[i])
        // Pool-adjacent-violators on x - offset keeps order and spacing.
        val blockWeight = mutableListOf<Double>(); val blockSum = mutableListOf<Double>(); val blockCount = mutableListOf<Int>()
        layer.forEachIndexed { i, v ->
            val neighbors = neighborWeights[v].orEmpty()
            val (w, target) = when {
                kind[v] == LEFT -> BORDER_WEIGHT to sharedLeft[owner[v]]
                kind[v] == RIGHT -> BORDER_WEIGHT to sharedRight[owner[v]]
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
    fun positionX() {
        initialX()
        updateShared()
        repeat(POSITION_SWEEPS) { sweep ->
            val sequence = if (sweep % 2 == 0) order else order.asReversed()
            sequence.forEach(::place)
            if (borders.isNotEmpty()) updateShared()
        }
        // Frames are rectangles: every border of a subgraph takes the subgraph's
        // outermost position, then separation is restored left to right.
        if (borders.isNotEmpty()) {
            for (round in 0 until vertexCount + 4) {
                var changed = false
                borders.forEach { v -> x[v] = if (kind[v] == LEFT) sharedLeft[owner[v]] else sharedRight[owner[v]] }
                clusterSpecs.indices.filter { liveCluster[it] && !horizontal }.forEach { c ->
                    val minimum = clusterSpecs[c].titleWidth + 2 * TITLE_MARGIN
                    if (sharedRight[c] - sharedLeft[c] < minimum - 1e-6) { sharedRight[c] = sharedLeft[c] + minimum; changed = true }
                }
                borders.forEach { v -> x[v] = if (kind[v] == LEFT) sharedLeft[owner[v]] else sharedRight[owner[v]] }
                order.forEach { layer ->
                    for (i in 1 until layer.size) {
                        val v = layer[i]
                        val need = x[layer[i - 1]] + separation(layer[i - 1], v)
                        if (x[v] < need - 1e-6) {
                            x[v] = need; changed = true
                            if (kind[v] == LEFT) sharedLeft[owner[v]] = need
                            if (kind[v] == RIGHT) sharedRight[owner[v]] = need
                        }
                    }
                }
                if (!changed) break
            }
        }
    }
    // A short edge's dummy aligned with an endpoint sits exactly on that
    // endpoint's axis when its neighbors leave room, like dagre's alignment.
    fun snapAlignedDummies() = chains.values.filter { it.size == 3 }.forEach { chain ->
        val (a, d, b) = if (rank[chain[0]] < rank[chain[2]]) chain else chain.reversed()
        val target = when {
            segmentWeight[a to d] == ALIGN_WEIGHT -> x[a]
            segmentWeight[d to b] == ALIGN_WEIGHT -> x[b]
            else -> return@forEach
        }
        val layer = order[rank[d]]; val i = layer.indexOf(d)
        val leftOk = i == 0 || target - x[layer[i - 1]] >= separation(layer[i - 1], d) - 1e-6
        val rightOk = i == layer.lastIndex || x[layer[i + 1]] - target >= separation(d, layer[i + 1]) - 1e-6
        if (leftOk && rightOk) x[d] = target
    }
    // Like Brandes-Köpf blocks: a run of vertices joined one-to-one (each the
    // only successor of the one before and the only predecessor of the one
    // after) is drawn as one straight column when every layer leaves room.
    fun straightenBlocks() {
        val next = IntArray(vertexCount) { -1 }
        val hasPrevious = BooleanArray(vertexCount)
        segmentWeight.keys.forEach { (a, b) ->
            if (successors[a].orEmpty().size == 1 && predecessors[b].orEmpty().size == 1) { next[a] = b; hasPrevious[b] = true }
        }
        repeat(2) {
            for (start in 0 until vertexCount) {
                if (hasPrevious[start] || next[start] < 0) continue
                val block = generateSequence(start) { next[it].takeIf { n -> n >= 0 } }.toList()
                var low = -Double.MAX_VALUE; var high = Double.MAX_VALUE
                block.forEach { v ->
                    val layer = order[rank[v]]; val i = layer.indexOf(v)
                    if (i > 0) low = max(low, x[layer[i - 1]] + separation(layer[i - 1], v))
                    if (i < layer.lastIndex) high = min(high, x[layer[i + 1]] - separation(v, layer[i + 1]))
                }
                if (low > high + 1e-6) continue
                val sorted = block.map { x[it] }.sorted()
                val target = sorted[sorted.size / 2].coerceIn(low, max(low, high))
                block.forEach { x[it] = target }
            }
        }
    }
    positionX()
    straightenBlocks()
    snapAlignedDummies()
    // A self loop goes on the side of its node that its other edges leave
    // free, so neither crosses the other (Mermaid keeps loops clear as well).
    run {
        var moved = false
        loopVertex.values.forEach { v ->
            val node = predecessors.getValue(v).first()
            val layer = order[rank[v]]
            val others = layer.filter { d -> d != v && kind[d] == DUMMY && d !in loopVertex.values &&
                (predecessors[d].orEmpty().contains(node) || successors[d].orEmpty().contains(node)) }
            if (others.isEmpty()) return@forEach
            fun far(d: Int) = (predecessors[d].orEmpty() + successors[d].orEmpty()).first { it != node }
            val before = others.count { x[far(it)] < x[node] - 1e-6 }
            val after = others.count { x[far(it)] > x[node] + 1e-6 }
            if (before == after) return@forEach
            val positions = others.map { layer.indexOf(it) }
            val current = layer.indexOf(v)
            val wanted = if (before > after) positions.max() + 1 else positions.min()
            if ((before > after && current > positions.max()) || (after > before && current < positions.min())) return@forEach
            layer.removeAt(current)
            layer.add(if (wanted > current) wanted - 1 else wanted, v)
            if (clusterSpecs.isNotEmpty()) { val slot = layer.withIndex().associate { it.value to it.index.toDouble() }; regroup(layer) { slot.getValue(it) } }
            moved = true
        }
        if (moved) { positionX(); straightenBlocks(); snapAlignedDummies() }
    }

    // Y positioning. Ranks with no members collapse; frame paddings widen rank gaps.
    val titleAtStart = direction != FlowDirection.BT
    fun startPad(c: Int) = if (!horizontal && titleAtStart) clusterSpecs[c].titleBand else CLUSTER_PAD_RANK
    fun endPad(c: Int) = if (!horizontal && !titleAtStart) clusterSpecs[c].titleBand else CLUSTER_PAD_RANK
    val live = clusterSpecs.indices.filter { liveCluster[it] }
    val startOffset = DoubleArray(clusterSpecs.size)
    val endOffset = DoubleArray(clusterSpecs.size)
    // Deepest subgraphs first, so a parent adds its padding to its children's.
    live.sortedByDescending { clusterChain(it).size }.forEach { c ->
        val children = live.filter { clusterParent[it] == c }
        startOffset[c] = startPad(c) + (children.filter { spanLow[it] == spanLow[c] }.maxOfOrNull { startOffset[it] } ?: 0.0)
        endOffset[c] = endPad(c) + (children.filter { spanHigh[it] == spanHigh[c] }.maxOfOrNull { endOffset[it] } ?: 0.0)
    }
    val needStart = DoubleArray(maxRank + 1)
    val needEnd = DoubleArray(maxRank + 1)
    live.forEach { c ->
        needStart[spanLow[c]] = max(needStart[spanLow[c]], startOffset[c])
        needEnd[spanHigh[c]] = max(needEnd[spanHigh[c]], endOffset[c])
    }
    val rankHeight = DoubleArray(maxRank + 1)
    order.forEachIndexed { r, layer -> rankHeight[r] = layer.maxOfOrNull { height[it] } ?: 0.0 }
    val rankCenter = DoubleArray(maxRank + 1)
    run {
        var cursor = 0.0
        var previous = -1
        for (r in 0..maxRank) {
            if (order[r].isEmpty()) continue
            cursor = if (previous >= 0) cursor + rankHeight[previous] / 2 + needEnd[previous] + nodeGap / 2 + needStart[r] + rankHeight[r] / 2
            else rankHeight[r] / 2
            rankCenter[r] = cursor
            previous = r
        }
    }

    // Rank space to scene space.
    fun scene(px: Double, py: Double): ScenePoint = when (direction) {
        FlowDirection.LR -> ScenePoint(py, px)
        FlowDirection.RL -> ScenePoint(-py, px)
        FlowDirection.BT -> ScenePoint(px, -py)
        else -> ScenePoint(px, py)
    }
    fun sceneRect(x0: Double, y0: Double, x1: Double, y1: Double): SceneRect {
        val a = scene(x0, y0); val b = scene(x1, y1)
        return SceneRect(min(a.x, b.x), min(a.y, b.y), abs(a.x - b.x), abs(a.y - b.y))
    }
    fun center(v: Int) = scene(x[v], rankCenter[rank[v]])
    val rawLeaves = leaves.indices.associate { v ->
        val c = center(v); val size = leaves[v].size
        leaves[v].id to SceneRect(c.x - size.width / 2, c.y - size.height / 2, size.width, size.height)
    }
    val rawFrames = live.associate { c ->
        val top = rankCenter[spanLow[c]] - rankHeight[spanLow[c]] / 2 - startOffset[c]
        val bottom = rankCenter[spanHigh[c]] + rankHeight[spanHigh[c]] / 2 + endOffset[c]
        var frame = sceneRect(sharedLeft[c], top, sharedRight[c], bottom)
        val minimum = clusterSpecs[c].titleWidth + 2 * TITLE_MARGIN
        if (frame.width < minimum) frame = SceneRect(frame.x - (minimum - frame.width) / 2, frame.y, minimum, frame.height)
        clusterSpecs[c].id to frame
    }
    val shapes = leaves.associate { it.id to it.shape }
    val rawPaths = linkedMapOf<Int, List<ScenePoint>>()
    val rawLabels = linkedMapOf<Int, ScenePoint>()
    val specByIndex = edgeSpecs.associateBy { it.index }
    chains.forEach { (index, chain) ->
        val spec = specByIndex.getValue(index)
        val inner = chain.subList(1, chain.size - 1).map(::center)
        val sourceId = leaves[chain.first()].id; val targetId = leaves[chain.last()].id
        val source = rawLeaves.getValue(sourceId); val target = rawLeaves.getValue(targetId)
        val start = boundaryPoint(source, shapes.getValue(sourceId), inner.firstOrNull() ?: rectCenter(target))
        val end = boundaryPoint(target, shapes.getValue(targetId), inner.lastOrNull() ?: rectCenter(source))
        var points = listOf(start) + inner + end
        // Edges to a subgraph end on its frame, as in Mermaid.
        spec.clipTo?.let { id -> points = clipAtFrame(points, rawFrames.getValue(id)) }
        spec.clipFrom?.let { id -> points = clipAtFrame(points.reversed(), rawFrames.getValue(id)).reversed() }
        rawPaths[index] = points
        labelVertex[index]?.let { rawLabels[index] = center(it) }
    }
    loopVertex.forEach { (index, v) ->
        val spec = specByIndex.getValue(index)
        val node = leafIndex.getValue(spec.from)
        val rect = rawLeaves.getValue(spec.from)
        // Rank space: the loop hangs from the node's far side, its label below it.
        val half = width[node] / 2
        val cx = x[v].coerceIn(x[node] - half / 2, x[node] + half / 2)
        val near = rankCenter[rank[node]] + height[node] / 2
        val label = rankSize(spec.label)
        val labelCenter = rankCenter[rank[v]] + LOOP_DEPTH / 2
        val turn = if (label != null) labelCenter - label.height / 2 else rankCenter[rank[v]]
        val spread = min(LOOP_WIDTH / 2 - 2, half / 2)
        val a = scene(cx - spread - 10, turn - 1); val b = scene(cx + spread + 10, turn - 1)
        val start = boundaryPoint(rect, shapes.getValue(spec.from), scene(cx - spread, near))
        val end = boundaryPoint(rect, shapes.getValue(spec.from), scene(cx + spread, near))
        rawPaths[index] = listOf(start, a, b, end)
        if (label != null) rawLabels[index] = scene(x[v], labelCenter)
    }

    // Translate everything to the origin.
    val labelBoxes = rawLabels.map { (index, c) ->
        val size = specByIndex.getValue(index).label!!
        SceneRect(c.x - size.width / 2, c.y - size.height / 2, size.width, size.height)
    }
    val rects = rawLeaves.values + rawFrames.values + labelBoxes
    val xs = rects.flatMap { listOf(it.x, it.x + it.width) } + rawPaths.values.flatten().map { it.x }
    val ys = rects.flatMap { listOf(it.y, it.y + it.height) } + rawPaths.values.flatten().map { it.y }
    if (xs.isEmpty()) return LevelResult(0.0, 0.0, emptyMap(), emptyMap(), emptyMap(), emptyMap())
    val dx = -xs.min(); val dy = -ys.min()
    fun move(p: ScenePoint) = ScenePoint(p.x + dx, p.y + dy)
    fun moveRect(r: SceneRect) = SceneRect(r.x + dx, r.y + dy, r.width, r.height)
    return LevelResult(
        width = xs.max() - xs.min(),
        height = ys.max() - ys.min(),
        leaves = rawLeaves.mapValues { moveRect(it.value) },
        clusters = rawFrames.mapValues { moveRect(it.value) },
        paths = rawPaths.mapValues { (_, points) -> points.map(::move) },
        labels = rawLabels.mapValues { move(it.value) },
    )
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

/** Cut a route where it first enters [frame], walking from its start. */
private fun clipAtFrame(points: List<ScenePoint>, frame: SceneRect): List<ScenePoint> {
    fun inside(p: ScenePoint) = p.x > frame.x + 1e-6 && p.x < frame.x + frame.width - 1e-6 && p.y > frame.y + 1e-6 && p.y < frame.y + frame.height - 1e-6
    if (inside(points.first())) return points
    for (i in 1 until points.size) {
        if (!inside(points[i])) continue
        val a = points[i - 1]; val b = points[i]
        // Bisection is enough for one convex box.
        var low = 0.0; var high = 1.0
        repeat(40) { val mid = (low + high) / 2; if (inside(ScenePoint(a.x + (b.x - a.x) * mid, a.y + (b.y - a.y) * mid))) high = mid else low = mid }
        val hit = ScenePoint(a.x + (b.x - a.x) * high, a.y + (b.y - a.y) * high)
        val kept = points.subList(0, i) + hit
        return if (kept.size >= 2) kept else points
    }
    return points
}

private const val EDGE_GAP = 20.0
private const val BORDER_GAP = 12.0
private const val CLUSTER_PAD_NODE = 20.0
private const val CLUSTER_PAD_INNER = 12.0
private const val CLUSTER_PAD_RANK = 16.0
private const val BOX_PAD = 20.0
private const val TITLE_GAP = 16.0
private const val TITLE_MARGIN = 12.0
private const val BORDER_WEIGHT = 4.0
private const val LABEL_PAD_X = 4.0
private const val LABEL_PAD_Y = 2.0
private const val LOOP_WIDTH = 28.0
private const val LOOP_DEPTH = 8.0
private const val POSITION_SWEEPS = 60
private const val ALIGN_WEIGHT = 24

private fun snap(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0

private fun rectCenter(r: SceneRect) = ScenePoint(r.x + r.width / 2, r.y + r.height / 2)

/** Polygon outlines of the straight-sided node shapes, as drawn by [flowSpecialShape]. */
internal fun flowShapeOutline(shape: FlowNodeShape, r: SceneRect): List<List<ScenePoint>> {
    val expanded = expandedFlowPolygons(shape, r)
    if (expanded.isNotEmpty()) return expanded
    val x = r.x; val y = r.y; val w = r.width; val h = r.height; val inset = 12.0
    fun p(dx: Double, dy: Double) = ScenePoint(x + dx, y + dy)
    return when (shape) {
        FlowNodeShape.HEXAGON -> listOf(listOf(p(inset, 0.0), p(w - inset, 0.0), p(w, h / 2), p(w - inset, h), p(inset, h), p(0.0, h / 2)))
        FlowNodeShape.ASYMMETRIC -> listOf(listOf(p(0.0, 0.0), p(w, 0.0), p(w, h), p(0.0, h), p(inset, h / 2)))
        FlowNodeShape.PARALLELOGRAM -> listOf(listOf(p(inset, 0.0), p(w, 0.0), p(w - inset, h), p(0.0, h)))
        FlowNodeShape.PARALLELOGRAM_ALT -> listOf(listOf(p(0.0, 0.0), p(w - inset, 0.0), p(w, h), p(inset, h)))
        FlowNodeShape.TRAPEZOID -> listOf(listOf(p(inset, 0.0), p(w - inset, 0.0), p(w, h), p(0.0, h)))
        FlowNodeShape.TRAPEZOID_ALT -> listOf(listOf(p(0.0, 0.0), p(w, 0.0), p(w - inset, h), p(inset, h)))
        else -> emptyList()
    }
}

/** Where the ray from the node center toward [toward] leaves the node outline. */
internal fun boundaryPoint(r: SceneRect, shape: FlowNodeShape, toward: ScenePoint): ScenePoint {
    val c = rectCenter(r)
    val dx = toward.x - c.x; val dy = toward.y - c.y
    if (abs(dx) < 1e-9 && abs(dy) < 1e-9) return c
    val hw = r.width / 2; val hh = r.height / 2
    val outline = flowShapeOutline(shape, r)
    val t = when {
        outline.isNotEmpty() -> outline.flatMap { points ->
            (points + points.first()).zipWithNext().mapNotNull { (a, b) ->
                val sx = b.x - a.x; val sy = b.y - a.y; val det = dx * sy - dy * sx
                if (abs(det) < 1e-9) null else {
                    val ax = a.x - c.x; val ay = a.y - c.y
                    val t = (ax * sy - ay * sx) / det; val u = (ax * dy - ay * dx) / det
                    if (t >= 0 && u >= -1e-9 && u <= 1 + 1e-9) t else null
                }
            }
        }.maxOrNull() ?: 1.0
        shape == FlowNodeShape.DIAMOND -> 1.0 / (abs(dx) / hw + abs(dy) / hh)
        shape == FlowNodeShape.CIRCLE || shape == FlowNodeShape.DOUBLE_CIRCLE || shape == FlowNodeShape.ELLIPSE ->
            1.0 / sqrt((dx * dx) / (hw * hw) + (dy * dy) / (hh * hh))
        else -> min(if (abs(dx) < 1e-9) Double.MAX_VALUE else hw / abs(dx), if (abs(dy) < 1e-9) Double.MAX_VALUE else hh / abs(dy))
    }
    return ScenePoint(c.x + dx * t, c.y + dy * t)
}
