package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.*

/** Subday data keeps millisecond precision through placement and axis labels. */
internal fun layoutPreciseGantt(diagram: GanttDiagram, measurer: TextMeasurer, config: LayoutConfig, dateLabel: (Int) -> String): LayoutScene {
    val rows = diagram.sections.flatMap { section -> section.tasks.map { section.name to it } }
    val first = rows.minOf { it.second.startEpochMillis }
    val last = rows.maxOf { it.second.startEpochMillis + it.second.durationMillis }
    val span = maxOf(1L, last - first)
    val body = TextStyle(fontSize = 12.0)
    val tickStyle = TextStyle(fontSize = 10.0)
    fun tickLabel(time: Long): String {
        val day = floor(time.toDouble() / 86_400_000).toInt() + 719528
        val offset = ((time % 86_400_000) + 86_400_000) % 86_400_000
        fun digits(value: Long, length: Int = 2) = value.toString().padStart(length, '0')
        val clock = "${digits(offset / 3_600_000)}:${digits(offset / 60_000 % 60)}:${digits(offset / 1000 % 60)}.${digits(offset % 1000, 3)}"
        return if (diagram.dateFormat !in setOf("x", "X") || floor(first.toDouble() / 86_400_000) != floor(last.toDouble() / 86_400_000)) "${dateLabel(day)} $clock" else clock
    }
    val labelWidth = rows.maxOf { measurer.measure("${it.first}: ${it.second.name}", body).width } + 16.0
    val ticks = (0..4).map { first + (span.toDouble() * it / 4).toLong() }.distinct()
    val tickWidth = ticks.maxOf { measurer.measure(tickLabel(it), tickStyle).width }
    val plotWidth = maxOf(520.0, (tickWidth + 12.0) * 4)
    val left = config.padding + maxOf(labelWidth, tickWidth / 2)
    val width = left + plotWidth + maxOf(config.padding, tickWidth / 2 + 2)
    val titleOffset = if (diagram.title == null) 0.0 else 28.0
    val axisY = config.padding + titleOffset + 8
    val top = axisY + 28
    fun x(time: Long) = left + (time - first).toDouble() / span * plotWidth
    val commands = mutableListOf<DrawCommand>()
    diagram.title?.let { commands += DrawText(it, ScenePoint(config.padding, config.padding + 16), style = TextStyle(fontSize = 18.0, fontWeight = 600)) }
    commands += DrawLine(ScenePoint(left, axisY), ScenePoint(left + plotWidth, axisY))
    for (time in ticks) {
        commands += DrawLine(ScenePoint(x(time), axisY), ScenePoint(x(time), axisY + 6), strokeWidth = 1.0)
        commands += DrawText(tickLabel(time), ScenePoint(x(time), axisY + 20), TextAnchor.MIDDLE, tickStyle)
    }
    rows.forEachIndexed { index, (section, task) ->
        val y = top + index * 34
        commands += DrawText("$section: ${task.name}", ScenePoint(config.padding, y + 13), style = body)
        val fill = SceneColor(when (task.status) { GanttTaskStatus.DONE -> "#16a34a"; GanttTaskStatus.ACTIVE -> "#2563eb"; GanttTaskStatus.CRITICAL -> "#dc2626"; GanttTaskStatus.TODO -> "#94a3b8" })
        if (task.milestone) {
            val center = x(task.startEpochMillis + task.durationMillis / 2)
            commands += DrawPolygon(listOf(ScenePoint(center, y), ScenePoint(center + 11, y + 11), ScenePoint(center, y + 22), ScenePoint(center - 11, y + 11)), fill)
        } else commands += DrawRect(SceneRect(x(task.startEpochMillis), y, task.renderDurationMillis.toDouble() / span * plotWidth, 22.0), 4.0, fill = fill)
    }
    return LayoutScene(width, top + rows.size * 34 + config.padding, commands, diagram.accessibilityTitle, diagram.accessibilityDescription)
}
