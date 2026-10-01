package build.raft.mermaid.core

/** One flat treemap row. A null [value] denotes a section; [level] is its indentation. */
public data class TreemapHierarchyItem(
    val level: Int,
    val label: String,
    val value: Double? = null,
    val classSelector: String? = null,
)

/**
 * Builds a treemap hierarchy in source order, using the nearest preceding row at a
 * smaller indentation as the parent. Indentation increments need not be uniform.
 *
 * This is also the parser's incremental builder, so an invalid child of a leaf is
 * rejected at that row. It deliberately retains Native's strict leaf rule.
 * Label/value validation belongs to the parser (or the caller constructing rows).
 */
public class TreemapHierarchy {
    private class Node(val item: TreemapHierarchyItem) {
        val children: MutableList<Node> = mutableListOf()
        fun freeze(): TreemapNode = TreemapNode(
            item.label, item.value, children.map { it.freeze() }, item.classSelector,
        )
    }

    private val roots = mutableListOf<Node>()
    private val stack = mutableListOf<Node>()

    /** Adds one row. A rejected row leaves the existing hierarchy unchanged. */
    public fun add(item: TreemapHierarchyItem) {
        require(item.level >= 0) { "Treemap indentation must be non-negative" }
        val parentIndex = stack.indexOfLast { it.item.level < item.level }
        require(parentIndex < 0 || stack[parentIndex].item.value == null) {
            "Treemap leaves cannot have children"
        }
        while (stack.size > parentIndex + 1) stack.removeAt(stack.lastIndex)
        val node = Node(item)
        if (stack.isEmpty()) roots += node else stack.last().children += node
        stack += node
    }

    /** Returns a detached snapshot; later additions do not mutate earlier results. */
    public fun build(): List<TreemapNode> = roots.map { it.freeze() }
}
