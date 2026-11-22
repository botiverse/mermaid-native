package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*

/**
 * Merge nodes that share an id with a subgraph into that subgraph.
 *
 * `A --> group`, `style group …` or a member list can name a subgraph before it
 * is declared, so the model holds both. Mermaid keeps the subgraph and applies
 * the styles and classes of the node to it. A subgraph listed as a member of
 * another one is nested in it; the first listing wins.
 */
internal fun mergeFlowGroupNodes(diagram: FlowchartDiagram): FlowchartDiagram {
    if (diagram.subgraphs.isEmpty()) return diagram
    val ids = diagram.subgraphs.map { it.id }.toSet()
    val shadowed = diagram.nodes.filter { it.id in ids }.associateBy { it.id }
    val parents = diagram.subgraphs.associate { it.id to it.parentId }.toMutableMap()
    fun insideOf(id: String, ancestor: String): Boolean {
        val seen = mutableSetOf<String>()
        var current: String? = id
        while (current != null && seen.add(current)) {
            if (current == ancestor) return true
            current = parents[current]
        }
        return false
    }
    for (group in diagram.subgraphs) {
        if (parents[group.id] != null) continue
        parents[group.id] = diagram.subgraphs.firstOrNull { group.id in it.nodeIds && !insideOf(it.id, group.id) }?.id
    }
    if (shadowed.isEmpty() && diagram.subgraphs.all { it.parentId == parents[it.id] }) return diagram
    return diagram.copy(
        nodes = diagram.nodes.filter { it.id !in ids },
        subgraphs = diagram.subgraphs.map { group ->
            val node = shadowed[group.id]
            group.copy(
                parentId = parents[group.id],
                classes = (group.classes + node?.classes.orEmpty()).distinct(),
                styles = group.styles + node?.styles.orEmpty(),
            )
        },
    )
}
