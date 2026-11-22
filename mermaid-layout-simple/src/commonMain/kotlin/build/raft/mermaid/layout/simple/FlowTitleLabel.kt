package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.FlowSubgraph
import build.raft.mermaid.layout.*

/**
 * A subgraph title: its line breaks, and the emphasis of a markdown (backquoted) one. Like Mermaid's,
 * a title never wraps on its own; the frame widens for it.
 */
internal class FlowTitleLabel(subgraph: FlowSubgraph, private val style: TextStyle, private val measurer: TextMeasurer) {
    private val markdown = if (subgraph.labelType == "markdown") FlowMarkdownLabel(subgraph.label, style, measurer) else null
    private val lines = classNoteLines(subgraph.label)
    private val lineHeight = style.fontSize * 1.25
    val lineCount: Int = markdown?.lineCount ?: lines.size
    val width: Double = markdown?.width ?: lines.maxOf { measurer.measure(it, style).width }

    /** From the top of the first line to the last baseline. */
    val height: Double = style.fontSize + (lineCount - 1) * lineHeight

    /** [x] is the center, [baseline] the first line's. */
    fun draw(x: Double, baseline: Double): List<DrawText> =
        markdown?.drawCentered(ScenePoint(x, baseline), lineHeight)
            ?: lines.mapIndexed { index, line -> DrawText(line, ScenePoint(x, baseline + index * lineHeight), TextAnchor.MIDDLE, style) }
}
