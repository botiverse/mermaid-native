package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.EntityRelationshipDiagram
import build.raft.mermaid.core.FlowDirection
import build.raft.mermaid.layout.SceneRect
import build.raft.mermaid.layout.SceneSize

/** Measures nested containers before placing children; directions never rotate text. */
internal class EntityRelationshipPlacement(
    diagram: EntityRelationshipDiagram,
    entitySizes: Map<String, SceneSize>,
    groupTitleSizes: Map<String, SceneSize>,
    rightMargins: Map<String, Double>,
    padding: Double,
    gap: Double,
) {
    val rects = linkedMapOf<String, SceneRect>()
    val groupOrder = mutableListOf<String>()
    val width: Double
    val height: Double

    init {
        val groups = diagram.subgraphs.associateBy { it.id }
        val allIds = (entitySizes.keys + groups.keys).toSet()
        val contained = groups.values.flatMap { it.nodeIds }.toSet()
        val rootIds = (diagram.rootNodeIds + allIds).distinct().filter { it !in contained && it in allIds }
        val sizes = entitySizes.toMutableMap()
        val directions = mutableMapOf<String, FlowDirection>()
        val groupPadding = 24.0
        fun header(id: String) = maxOf(32.0, (groupTitleSizes[id]?.height ?: 0.0) + 16.0)
        fun children(id: String) = groups.getValue(id).nodeIds.distinct().filter { it in allIds && it != id }
        fun contentSize(ids: List<String>, direction: FlowDirection): SceneSize {
            val horizontal = direction == FlowDirection.LR || direction == FlowDirection.RL
            val values = ids.mapNotNull { id -> sizes[id]?.let { SceneSize(it.width + (rightMargins[id] ?: 0.0), it.height) } }
            val gaps = gap * maxOf(0, values.size - 1)
            return if (horizontal) SceneSize(values.sumOf { it.width } + gaps, values.maxOfOrNull { it.height } ?: 0.0)
            else SceneSize(values.maxOfOrNull { it.width } ?: 0.0, values.sumOf { it.height } + gaps)
        }
        fun measure(id: String, inherited: FlowDirection) {
            val group = groups[id] ?: return
            val direction = group.direction ?: inherited
            directions[id] = direction
            val children = children(id)
            children.forEach { measure(it, direction) }
            val content = contentSize(children, direction)
            sizes[id] = SceneSize(maxOf(content.width, groupTitleSizes[id]?.width ?: 0.0) + groupPadding * 2,
                maxOf(16.0, content.height) + groupPadding * 2 + header(id))
        }
        rootIds.forEach { measure(it, diagram.direction) }
        fun place(ids: List<String>, direction: FlowDirection, x: Double, y: Double) {
            var cursor = 0.0
            val horizontal = direction == FlowDirection.LR || direction == FlowDirection.RL
            val ordered = if (direction == FlowDirection.BT || direction == FlowDirection.RL) ids.reversed() else ids
            for (id in ordered) {
                val size = sizes[id] ?: continue
                val rect = SceneRect(x + if (horizontal) cursor else 0.0, y + if (horizontal) 0.0 else cursor, size.width, size.height)
                rects[id] = rect
                if (id in groups) {
                    groupOrder += id
                    place(children(id), directions.getValue(id), rect.x + groupPadding, rect.y + groupPadding + header(id))
                }
                cursor += (if (horizontal) size.width + (rightMargins[id] ?: 0.0) else size.height) + gap
            }
        }
        val content = contentSize(rootIds, diagram.direction)
        width = content.width + padding * 2
        height = content.height + padding * 2
        place(rootIds, diagram.direction, padding, padding)
    }
}
