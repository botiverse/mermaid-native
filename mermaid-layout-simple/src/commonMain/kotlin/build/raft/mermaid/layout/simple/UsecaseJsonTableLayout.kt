package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*

/** Measured once, then shared by placement, edge clipping and painting. */
internal class UsecaseJsonTableLayout(data: UsecaseOrderedJsonObject, title: List<String>, private val measurer: TextMeasurer) {
    private val cellStyle = TextStyle(fontSize = 12.0)
    private val titleStyle = TextStyle(fontSize = 13.0, fontWeight = 600)
    private fun wrap(text: String, limit: Double): List<String> = text.split('\n').flatMap { line ->
        val result = mutableListOf<String>(); var current = ""
        // Iterate code points so wrapping never separates a UTF-16 surrogate pair.
        var i = 0
        while (i < line.length) {
            val size = if (line[i].isHighSurrogate() && i + 1 < line.length && line[i + 1].isLowSurrogate()) 2 else 1
            val next = line.substring(i, i + size)
            if (current.isNotEmpty() && measurer.measure(current + next, cellStyle).width > limit) { result += current; current = "" }
            current += next; i += size
        }
        result + current
    }
    private data class Row(val keys: List<String>, val values: List<String>) { val height: Double get() = maxOf(keys.size, values.size) * 18.0 + 16.0 }
    private val titles = title
    private val rows = UsecaseJsonTable.rows(data).map { Row(wrap(it.key, 180.0), wrap(it.value, 240.0)) }
    private val measuredKey = maxOf(48.0, (rows.flatMap { it.keys }.maxOfOrNull { measurer.measure(it, cellStyle).width } ?: 0.0) + 24.0)
    private val measuredValue = maxOf(64.0, (rows.flatMap { it.values }.maxOfOrNull { measurer.measure(it, cellStyle).width } ?: 0.0) + 24.0)
    val width: Double = maxOf(180.0, measuredKey + measuredValue, (titles.maxOfOrNull { measurer.measure(it, titleStyle).width } ?: 0.0) + 32.0)
    private val keyWidth = measuredKey + (width - measuredKey - measuredValue) / 2
    private val titleHeight = titles.size * 20.0 + 16.0
    val height: Double = titleHeight + rows.sumOf { it.height }

    fun draw(center: ScenePoint, fill: SceneColor, stroke: SceneColor, text: SceneColor): List<DrawCommand> = buildList {
        val left = center.x - width / 2; val top = center.y - height / 2
        add(DrawRect(SceneRect(left, top, width, height), 4.0, fill = fill, stroke = stroke))
        titles.forEachIndexed { i, line -> add(DrawText(line, ScenePoint(center.x, top + 23 + i * 20), TextAnchor.MIDDLE, titleStyle.copy(color = text))) }
        var y = top + titleHeight
        add(DrawLine(ScenePoint(left + keyWidth, y), ScenePoint(left + keyWidth, top + height), stroke = stroke))
        for (row in rows) {
            add(DrawLine(ScenePoint(left, y), ScenePoint(left + width, y), stroke = stroke))
            row.keys.forEachIndexed { i, line -> add(DrawText(line, ScenePoint(left + 12, y + 21 + i * 18), style = cellStyle.copy(color = text))) }
            row.values.forEachIndexed { i, line -> add(DrawText(line, ScenePoint(left + keyWidth + 12, y + 21 + i * 18), style = cellStyle.copy(color = text))) }
            y += row.height
        }
    }
}
