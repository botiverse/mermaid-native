package build.raft.mermaid.layout.simple

import build.raft.mermaid.layout.SceneRect
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Measured Block grid sizing, placement and bounds ported from Mermaid block/layout.ts.
 * Coordinates in [Size] are centers. Mutates only the supplied per-layout node tree.
 * [Node.headingHeight] is Native's optional composite heading band; zero follows upstream.
 */
public object BlockGridLayout {
    public data class Size(var width: Double, var height: Double, var x: Double = 0.0, var y: Double = 0.0)
    public data class Node(
        val id: String,
        val type: String = "square",
        val columns: Int = -1,
        val span: Int = 1,
        val children: List<Node> = emptyList(),
        var size: Size? = null,
        val headingHeight: Double = 0.0,
    )
    public data class Position(val px: Int, val py: Int)

    public fun position(columns: Int, position: Int): Position {
        require(columns != 0) { "Columns must be an integer !== 0." }
        require(position >= 0) { "Position must be a non-negative integer.$position" }
        return if (columns < 0) Position(position, 0) else Position(position % columns, position / columns)
    }

    public fun layout(root: Node, padding: Double = 8.0): SceneRect {
        require(padding.isFinite() && padding >= 0.0) { "Block padding must be finite and non-negative" }
        fun validate(node: Node, depth: Int) {
            require(depth <= 128) { "Block nesting exceeds 128 levels" }
            require(node.columns != 0 && node.span > 0) { "Invalid Block columns or span" }
            require(node.headingHeight.isFinite() && node.headingHeight >= 0.0)
            node.size?.let { require(listOf(it.width, it.height, it.x, it.y).all(Double::isFinite) && it.width >= 0 && it.height >= 0) }
            node.children.forEach { validate(it, depth + 1) }
        }
        validate(root, 0)
        var work = 0
        setSizes(root, 0.0, 0.0, padding) { require(++work <= 100_000) { "Block grid sizing exceeds work limit" } }
        place(root, padding)
        var minX = 0.0; var minY = 0.0; var maxX = 0.0; var maxY = 0.0
        fun bounds(node: Node) {
            node.size?.let { s -> require(listOf(s.width, s.height, s.x, s.y).all(Double::isFinite)) { "Block grid geometry is not finite" } }
            node.size?.takeIf { node.id != "root" }?.let { s ->
                minX = min(minX, s.x - s.width / 2); minY = min(minY, s.y - s.height / 2)
                maxX = max(maxX, s.x + s.width / 2); maxY = max(maxY, s.y + s.height / 2)
            }
            node.children.forEach(::bounds)
        }
        bounds(root)
        return SceneRect(minX, minY, maxX - minX, maxY - minY)
    }

    private fun setSizes(block: Node, siblingWidth: Double, siblingHeight: Double, padding: Double, tick: () -> Unit) {
        tick()
        if (block.size == null || block.size!!.width == 0.0) block.size = Size(siblingWidth, siblingHeight)
        if (block.children.isEmpty()) return
        block.children.forEach { setSizes(it, 0.0, 0.0, padding, tick) }
        val maxWidth = block.children.filter { it.type != "space" }.maxOfOrNull { (it.size?.width ?: 0.0) / it.span } ?: 0.0
        val maxHeight = block.children.filter { it.type != "space" }.maxOfOrNull { it.size?.height ?: 0.0 } ?: 0.0
        block.children.forEach { child -> child.size?.let {
            it.width = maxWidth * child.span + padding * (child.span - 1)
            it.height = maxHeight; it.x = 0.0; it.y = 0.0
        } }
        block.children.forEach { setSizes(it, maxWidth, maxHeight, padding, tick) }
        val numItems = block.children.sumOf { it.span.toLong() }
        val xSize = if (block.columns > 0 && block.columns < numItems) block.columns else block.children.size
        val ySize = ceil(numItems.toDouble() / xSize)
        var width = xSize * (maxWidth + padding) + padding
        var height = ySize * (maxHeight + padding) + padding + block.headingHeight
        if (width < siblingWidth) {
            width = siblingWidth; height = siblingHeight
            val childWidth = (siblingWidth - xSize * padding - padding) / xSize
            val childHeight = (siblingHeight - block.headingHeight - ySize * padding - padding) / ySize
            block.children.forEach { it.size?.apply { this.width = childWidth; this.height = childHeight; x = 0.0; y = 0.0 } }
        }
        if (width < block.size!!.width) {
            width = block.size!!.width
            val count = if (block.columns > 0) min(block.children.size, block.columns) else block.children.size
            if (count > 0) block.children.forEach { it.size?.width = (width - count * padding - padding) / count }
        }
        block.size = Size(width, height)
    }

    private fun place(block: Node, padding: Double) {
        if (block.children.isEmpty()) return
        val rowHeights = mutableMapOf<Int, Double>()
        var column = 0
        fun advance(child: Node) {
            val filled = if (block.columns > 0) min(child.span, block.columns - column % block.columns) else child.span
            require(column.toLong() + filled <= Int.MAX_VALUE) { "Block column occupancy overflow" }
            column += filled
        }
        block.children.forEach { child -> child.size?.let {
            val row = position(block.columns, column).py
            rowHeights[row] = max(rowHeights[row] ?: 0.0, it.height)
            advance(child)
        } }
        val offsets = mutableMapOf<Int, Double>(); var offset = 0.0
        rowHeights.keys.sorted().forEach { row -> offsets[row] = offset; offset += rowHeights.getValue(row) + padding }
        val parent = block.size ?: return
        fun left() = if (parent.x != 0.0) parent.x - parent.width / 2 else -padding
        var startX = left(); var rowPosition = 0; column = 0
        block.children.forEach { child -> child.size?.let { s ->
            val row = position(block.columns, column).py
            if (row != rowPosition) { rowPosition = row; startX = left() }
            s.x = startX + padding + s.width / 2
            startX = s.x + s.width / 2
            s.y = parent.y - parent.height / 2 + block.headingHeight + offsets.getValue(row) + rowHeights.getValue(row) / 2 + padding
            place(child, padding)
            advance(child)
        } }
    }
}
