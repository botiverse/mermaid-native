package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*

/** Collapse before placement so hidden members cannot affect bounds or intercept edges. */
internal fun visibleFlow(diagram: FlowchartDiagram): FlowchartDiagram {
    if (diagram.subgraphs.none { it.collapsed }) return diagram
    val groups = diagram.subgraphs.associateBy { it.id }
    val redirects = mutableMapOf<String, String>()
    val hiddenGroups = mutableSetOf<String>()
    val replacements = mutableListOf<FlowNode>()
    fun collapsedAncestor(group: FlowSubgraph): Boolean {
        val seen = mutableSetOf<String>()
        var parent = group.parentId
        while (parent != null && seen.add(parent)) {
            val owner = groups[parent] ?: break
            if (owner.collapsed) return true
            parent = owner.parentId
        }
        return false
    }
    fun hide(group: FlowSubgraph, destination: String, seen: MutableSet<String>) {
        if (!seen.add(group.id)) return
        hiddenGroups += group.id
        if (group.id != destination) redirects[group.id] = destination
        group.nodeIds.forEach { id -> redirects[id] = destination; groups[id]?.let { hide(it,destination,seen) } }
        diagram.subgraphs.filter { it.parentId == group.id }.forEach { hide(it,destination,seen) }
    }
    diagram.subgraphs.filter { it.collapsed && !collapsedAncestor(it) }.forEach {
        replacements += FlowNode(it.id,it.label,classes=it.classes)
        hide(it,it.id,mutableSetOf())
    }
    val surviving = diagram.subgraphs.filter { it.id !in hiddenGroups }.map { g ->
        g.copy(nodeIds = (g.nodeIds.map { redirects[it] ?: it } + diagram.subgraphs.filter { it.parentId==g.id && it.collapsed }.map { it.id }).distinct())
    }
    val nodes = diagram.nodes.filter { it.id !in redirects } + replacements
    val edges = diagram.edges.mapNotNull { edge ->
        val from = redirects[edge.sourceId] ?: edge.sourceId
        val to = redirects[edge.targetId] ?: edge.targetId
        if (from == to && (edge.sourceId in redirects || edge.targetId in redirects)) null
        else edge.copy(sourceId = from, targetId = to)
    }
    // Replacement nodes are appended above; place them by visible dependencies, not
    // their synthetic declaration position. Preserve declaration order for ties and cycles.
    val remaining = nodes.toMutableList()
    val ordered = mutableListOf<FlowNode>()
    while (remaining.isNotEmpty()) {
        val ids = remaining.map { it.id }.toSet()
        val next = remaining.firstOrNull { node ->
            edges.none { it.targetId == node.id && it.sourceId != node.id && it.sourceId in ids }
        } ?: break
        ordered += next
        remaining.remove(next)
    }
    return diagram.copy(nodes = ordered + remaining, subgraphs = surviving, edges = edges)
}
