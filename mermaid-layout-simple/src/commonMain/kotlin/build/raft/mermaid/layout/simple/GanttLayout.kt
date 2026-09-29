package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.ceil
import kotlin.math.floor

/** One measured timeline for calendar and subday data. Milliseconds remain the geometry source. */
internal fun layoutGanttTimeline(
    diagram: GanttDiagram,
    measurer: TextMeasurer,
    config: LayoutConfig,
    dateLabel: (Int) -> String,
): LayoutScene {
    // Inspired by beautiful-mermaid's zinc text/surface hierarchy, with semantic status accents.
    val ink = SceneColor("#27272a")
    val muted = SceneColor("#71717a")
    val border = SceneColor("#e4e4e7")
    val grid = SceneColor("#f0f0f2")
    val body = TextStyle(fontSize = 13.0, color = ink)
    val heading = TextStyle(fontSize = 12.0, fontWeight = 600, color = ink)
    val tickStyle = TextStyle(fontSize = 11.0, color = muted)
    val titleStyle = TextStyle(fontSize = 20.0, fontWeight = 600, color = ink)
    val padding = maxOf(16.0, config.padding)
    val tasks = diagram.sections.flatMap { it.tasks }
    val dayMillis = 86_400_000L
    val first = tasks.minOfOrNull { it.startEpochMillis } ?: 0L
    val last = tasks.maxOfOrNull { it.startEpochMillis + it.durationMillis } ?: first + dayMillis
    val precise = tasks.any { it.startEpochMillis % dayMillis != 0L || it.durationMillis % dayMillis != 0L || it.renderDurationMillis % dayMillis != 0L }
    val span = maxOf(if (precise) 1L else dayMillis, last - first)
    fun day(time: Long) = floor(time.toDouble() / dayMillis).toInt() + 719528
    fun clock(time: Long): String {
        val offset = ((time % dayMillis) + dayMillis) % dayMillis
        fun digits(n: Long, count: Int = 2) = n.toString().padStart(count, '0')
        return "${digits(offset / 3_600_000)}:${digits(offset / 60_000 % 60)}:${digits(offset / 1000 % 60)}.${digits(offset % 1000, 3)}"
    }
    val plotWidth = 640.0
    val ticks = if (precise) {
        (0..4).map { first + (span.toDouble() * it / 4).toLong() }.distinct()
    } else {
        val days = (span / dayMillis).toInt()
        val labelWidth = measurer.measure(dateLabel(day(first)), tickStyle).width
        val intervals = maxOf(1, floor(plotWidth / (labelWidth + 16.0)).toInt())
        val step = maxOf(1, ceil(days.toDouble() / intervals).toInt())
        ((0..days step step).map { first + it * dayMillis } + (first + span)).distinct().toMutableList().also { list ->
            if (list.size > 2 && (list.last() - list[list.lastIndex - 1]).toDouble() / span * plotWidth < labelWidth + 16.0) {
                list.removeAt(list.lastIndex - 1)
            }
        }
    }
    val showDateAboveClock = precise && (diagram.dateFormat !in setOf("x", "X") || day(first) != day(last))
    val tickWidth = ticks.maxOf { measurer.measure(if (precise) clock(it) else dateLabel(day(it)), tickStyle).width }
    val fullTickWidth = if (showDateAboveClock) maxOf(tickWidth, ticks.maxOf { measurer.measure(dateLabel(day(it)), tickStyle).width }) else tickWidth
    val labelTextWidth = maxOf(128.0, minOf(220.0,
        maxOf(tasks.maxOfOrNull { measurer.measure(it.name, body).width } ?: 0.0,
            diagram.sections.maxOfOrNull { measurer.measure(it.name, heading).width } ?: 0.0)))
    val labelColumn = labelTextWidth + 42.0
    val left = padding + labelColumn + fullTickWidth / 2
    val right = left + plotWidth
    val width = right + maxOf(padding, fullTickWidth / 2 + 12.0)
    fun x(time: Long) = left + (time - first).toDouble() / span * plotWidth

    // Wrap at measured code-point boundaries, retaining all source text (including long IDs).
    fun lines(text: String, style: TextStyle, limit: Double): List<String> = text.split('\n').flatMap { line ->
        val result = mutableListOf<String>()
        var current = ""
        var i = 0
        while (i < line.length) {
            val end = if (line[i].isHighSurrogate() && i + 1 < line.length && line[i + 1].isLowSurrogate()) i + 2 else i + 1
            val next = line.substring(i, end)
            if (current.isNotEmpty() && measurer.measure(current + next, style).width > limit) {
                val breakAt = current.indexOfLast { it.isWhitespace() }
                if (breakAt > 0) {
                    result += current.substring(0, breakAt + 1)
                    current = current.substring(breakAt + 1)
                } else {
                    result += current; current = ""
                }
            }
            current += next; i = end
        }
        result + current
    }
    val commands = mutableListOf<DrawCommand>()
    val titleLines = diagram.title?.let { lines(it, titleStyle, width - padding * 2) }.orEmpty()
    titleLines.forEachIndexed { i, line -> commands += DrawText(line, ScenePoint(padding, padding + 20 + i * 26), style = titleStyle) }
    val axisTop = padding + if (titleLines.isEmpty()) 0.0 else titleLines.size * 26.0 + 18.0
    val axisBottom = axisTop + if (showDateAboveClock) 48.0 else 36.0
    val content = mutableListOf<DrawCommand>()
    var y = axisBottom
    for (section in diagram.sections) {
        if (section.tasks.isEmpty()) continue
        if (section.name.isNotEmpty()) {
            val sectionLines = lines(section.name, heading, labelTextWidth)
            val headerHeight = maxOf(32.0, sectionLines.size * 17.0 + 14.0)
            content += DrawRect(SceneRect(padding, y, right - padding + 12, headerHeight), 4.0,
                fill = SceneColor("#f7f7f8"), strokeWidth = 0.0)
            sectionLines.forEachIndexed { i, line -> content += DrawText(line, ScenePoint(padding + 12, y + 21 + i * 17), style = heading) }
            y += headerHeight
        }
        for (task in section.tasks) {
            val labelLines = lines(task.name, body, labelTextWidth)
            val rowHeight = maxOf(42.0, labelLines.size * 18.0 + 18.0)
            val centerY = y + rowHeight / 2
            val (fill, accent) = when (task.status) {
                GanttTaskStatus.DONE -> "#dcebe3" to "#537966"
                GanttTaskStatus.ACTIVE -> "#dce6f5" to "#526f9f"
                GanttTaskStatus.CRITICAL -> "#f4dfe1" to "#a66069"
                GanttTaskStatus.TODO -> "#e8e8ed" to "#858590"
            }
            content += DrawEllipse(ScenePoint(padding + 14, centerY), 3.0, 3.0,
                fill = SceneColor(accent), strokeWidth = 0.0)
            labelLines.forEachIndexed { i, line -> content += DrawText(line,
                ScenePoint(padding + 26, centerY - (labelLines.size - 1) * 9 + 4.5 + i * 18), style = body) }
            if (task.milestone) {
                val center = x(task.startEpochMillis + task.durationMillis / 2)
                content += DrawPolygon(listOf(ScenePoint(center, centerY - 8), ScenePoint(center + 8, centerY),
                    ScenePoint(center, centerY + 8), ScenePoint(center - 8, centerY)), SceneColor(accent))
            } else {
                content += DrawRect(SceneRect(x(task.startEpochMillis), centerY - 12,
                    task.renderDurationMillis.toDouble() / span * plotWidth, 24.0), 5.0,
                    fill = SceneColor(fill), stroke = SceneColor(accent), strokeWidth = 1.0)
            }
            y += rowHeight
            content += DrawLine(ScenePoint(padding, y), ScenePoint(right + 12, y), stroke = grid, strokeWidth = 1.0)
        }
        y += 12.0
    }
    val bottom = maxOf(axisBottom + 42.0, y - 12.0)
    // Faint full-height guides make dates readable without competing with the task bars.
    for (time in ticks) {
        val tickX = x(time)
        commands += DrawLine(ScenePoint(tickX, axisBottom), ScenePoint(tickX, bottom), stroke = grid, strokeWidth = 1.0)
        if (showDateAboveClock) commands += DrawText(dateLabel(day(time)), ScenePoint(tickX, axisTop + 14), TextAnchor.MIDDLE, tickStyle)
        commands += DrawText(if (precise) clock(time) else dateLabel(day(time)),
            ScenePoint(tickX, axisBottom - 12), TextAnchor.MIDDLE, tickStyle)
    }
    commands += DrawLine(ScenePoint(padding, axisBottom), ScenePoint(right + 12, axisBottom), stroke = border, strokeWidth = 1.0)
    commands += content
    return LayoutScene(width, bottom + padding, commands, diagram.accessibilityTitle, diagram.accessibilityDescription)
}
