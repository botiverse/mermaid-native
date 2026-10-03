package build.raft.mermaid.core

/**
 * Editable file tree whose [diagram] snapshots can be passed directly to Native layout.
 * Node IDs restart at 1 after [clear]; the virtual root always has ID 0.
 * This document exposes default spacing, not the browser global configuration store.
 */
public class TreeViewDocument {
    private data class Entry(val id: Int, val level: Int, val name: String, val directory: Boolean,
        val cssClass: String?, val icon: String?, val description: String?, val parent: Int?)
    private val entries = mutableListOf<Entry>()
    private val stack = mutableListOf<Int>()
    public var title: String = ""
    public var accessibilityTitle: String = ""
    public var accessibilityDescription: String = ""
    public val count: Int get() = entries.size + 1

    public fun clear() {
        entries.clear(); stack.clear(); title = ""; accessibilityTitle = ""; accessibilityDescription = ""
    }

    public fun addNode(level: Int, name: String, directory: Boolean, cssClass: String? = null,
        icon: String? = null, description: String? = null): Int {
        require(level >= 0) { "Tree level must be non-negative" }
        while (stack.isNotEmpty() && entries[stack.last()].level >= level) stack.removeAt(stack.lastIndex)
        val id = count
        entries += Entry(id, level, name, directory, cssClass, icon, description, stack.lastOrNull())
        stack += entries.lastIndex
        return id
    }

    public fun configuration(): Map<String, Any> = mapOf("paddingX" to 5, "paddingY" to 5, "rowIndent" to 10, "lineThickness" to 1)

    public fun rootSnapshot(): Map<String, Any?> {
        fun children(parent: Int?): List<Map<String, Any?>> = entries.mapIndexedNotNull { index, n ->
            if (n.parent != parent) null else mapOf("id" to n.id, "level" to n.level, "name" to n.name,
                "nodeType" to if (n.directory) "directory" else "file", "cssClass" to n.cssClass,
                "icon" to n.icon, "description" to n.description, "children" to children(index)).filterValues { it != null }
        }
        return mapOf("id" to 0, "level" to -1, "name" to "/", "nodeType" to "directory", "children" to children(null))
    }

    public fun diagram(): TreeViewDiagram {
        val nodes = mutableListOf<TreeViewNode>()
        for (n in entries) nodes += TreeViewNode(n.name, n.parent?.let { nodes[it].depth + 1 } ?: 0,
            n.parent, n.directory, n.level.takeIf { it > 0 }, n.cssClass, n.icon, n.description)
        return TreeViewDiagram(nodes, title.takeIf { it.isNotEmpty() }, accessibilityTitle.takeIf { it.isNotEmpty() }, accessibilityDescription.takeIf { it.isNotEmpty() })
    }
}

/** Editable node attributes; structure and numeric identities belong to its document. */
public class MindmapDocumentNode internal constructor(
    public val id: Int, public val sourceId: String, public val label: String,
    public val level: Int, public val type: Int, public val parentId: Int?,
    public var width: Double, public var padding: Double,
) {
    public var height: Double? = null
    public var section: Int? = null
    public var cssClass: String? = null
    public var icon: String? = null
    public var x: Double? = null
    public var y: Double? = null
}

/** Direct model construction and layout-data export, without generating parser input. */
public class MindmapDocument(public val padding: Double = 10.0, public val maxNodeWidth: Double = 200.0) {
    private val nodes = mutableListOf<MindmapDocumentNode>()
    private var baseLevel: Int? = null
    public fun clear() { nodes.clear(); baseLevel = null }
    public fun node(id: Int): MindmapDocumentNode = nodes[id]
    public val root: MindmapDocumentNode? get() = nodes.firstOrNull()

    public fun addNode(level: Int, id: String, label: String, type: Int): MindmapDocumentNode {
        require(type in 0..6) { "Unknown mindmap node type $type" }
        val normalized = if (nodes.isEmpty()) 0 else level - baseLevel!!
        val parent = nodes.lastOrNull { it.level < normalized }
        require(nodes.isEmpty() || parent != null) { "There can be only one root. No parent could be found for (\"$label\")" }
        if (nodes.isEmpty()) baseLevel = level
        val node = MindmapDocumentNode(nodes.size, id, label, normalized, type, parent?.id,
            maxNodeWidth, padding * if (type in listOf(1, 2, 6)) 2 else 1)
        nodes += node
        return node
    }

    private fun children(node: MindmapDocumentNode): List<MindmapDocumentNode> = nodes.filter { it.parentId == node.id }
    private fun shape(type: Int): MindmapNodeShape = when (type) {
        1 -> MindmapNodeShape.ROUNDED_RECTANGLE; 2 -> MindmapNodeShape.RECTANGLE
        3 -> MindmapNodeShape.DOUBLE_CIRCLE; 4 -> MindmapNodeShape.CLOUD
        5 -> MindmapNodeShape.BANG; 6 -> MindmapNodeShape.HEXAGON; else -> MindmapNodeShape.DEFAULT
    }

    /**
     * Export structure, labels, shapes, classes and icons to the existing Native renderer.
     * That renderer measures its own dimensions; width/height/padding/x/y edits are exported
     * by [layoutData] only and do not override Native placement or text measurement.
     */
    public fun diagram(): MindmapDiagram = MindmapDiagram(nodes.map { n ->
        MindmapNode(n.id.toString(), n.label, n.parentId?.toString(),
            generateSequence(n.parentId) { nodes[it].parentId }.count(), shape(n.type), n.sourceId, n.icon, n.cssClass)
    })

    public fun rootSnapshot(): Map<String, Any?>? {
        fun snapshot(n: MindmapDocumentNode): Map<String, Any?> = mapOf("id" to n.id, "nodeId" to n.sourceId,
            "descr" to n.label, "level" to n.level, "type" to n.type, "isRoot" to (n.parentId == null),
            "width" to n.width, "height" to n.height, "padding" to n.padding, "section" to n.section,
            "class" to n.cssClass, "icon" to n.icon, "x" to n.x, "y" to n.y,
            "children" to children(n).map { snapshot(it) }).filterValues { it != null }
        return root?.let { snapshot(it) }
    }

    /**
     * Export layout input with stable numeric identities and inherited section classes.
     * This is data for layout consumers, not a browser layout engine or theme registry.
     * Recomputes sections on the live nodes; returned maps are detached snapshots.
     */
    public fun layoutData(): Map<String, Any?> {
        val config = mapOf("layout" to "cose-bilkent", "mindmap" to mapOf("padding" to padding,
            "maxNodeWidth" to maxNodeWidth, "layoutAlgorithm" to "cose-bilkent", "useMaxWidth" to true))
        val r = root ?: return mapOf("nodes" to emptyList<Any>(), "edges" to emptyList<Any>(), "config" to config)
        val flat = mutableListOf<Map<String, Any?>>()
        val edges = mutableListOf<Map<String, Any?>>()
        fun visit(n: MindmapDocumentNode, section: Int?) {
            n.section = section
            val classes = if (n.parentId == null) "mindmap-node section-root section--1" else "mindmap-node section-$section"
            val shapeName = when(n.type) { 1 -> "rounded"; 2 -> "rect"; 3 -> "mindmapCircle"; 4 -> "cloud"; 5 -> "bang"; 6 -> "hexagon"; else -> "defaultMindmapNode" }
            flat += mapOf("id" to n.id.toString(), "domId" to "node_${n.id}", "label" to n.label,
                "labelType" to "markdown", "isGroup" to false, "shape" to shapeName,
                "width" to n.width, "height" to (n.height ?: 0.0), "padding" to n.padding,
                "cssClasses" to (classes + (n.cssClass?.takeIf { it.isNotEmpty() }?.let { " $it" } ?: "")),
                "cssStyles" to emptyList<String>(), "icon" to n.icon, "x" to n.x, "y" to n.y,
                "level" to n.level, "nodeId" to n.sourceId, "type" to n.type, "section" to section).filterValues { it != null }
            children(n).forEachIndexed { index, child ->
                val childSection = if (n.parentId == null) index % 11 else section
                edges += mapOf("id" to "edge_${n.id}_${child.id}", "start" to n.id.toString(), "end" to child.id.toString(),
                    "type" to "normal", "curve" to "basis", "thickness" to "normal",
                    "classes" to "edge section-edge-$childSection edge-depth-${n.level + 1}",
                    "depth" to n.level, "section" to childSection)
                visit(child, childSection)
            }
        }
        visit(r, null)
        return mapOf("nodes" to flat, "edges" to edges, "config" to config, "rootNode" to rootSnapshot(),
            "markers" to listOf("point"), "direction" to "TB", "nodeSpacing" to 50, "rankSpacing" to 50,
            "shapes" to flat.associate { it.getValue("id") to it.filterKeys { k -> k in listOf("shape", "width", "height", "padding") } }, "type" to "mindmap")
    }
}
