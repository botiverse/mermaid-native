package build.raft.mermaid.core

/** The original duration token, kept separate from calendar-month/year arithmetic. */
public data class GanttDurationValue(val amount: Double, val unit: String)
public object GanttDuration {
    public fun parse(text: String): GanttDurationValue {
        val match = Regex("^(\\d+(?:\\.\\d+)?)([Mdhmswy]|ms)$").matchEntire(text.trim())
            ?: return GanttDurationValue(Double.NaN, "ms")
        return GanttDurationValue(match.groupValues[1].toDoubleOrNull() ?: Double.NaN, match.groupValues[2])
    }
}
internal const val GANTT_DAY_MILLIS: Long = 86_400_000L
internal const val GANTT_EPOCH_DAY: Int = 719528
internal fun ganttFloorDay(epochMillis: Long): Int {
    val days = epochMillis / GANTT_DAY_MILLIS - if (epochMillis < 0 && epochMillis % GANTT_DAY_MILLIS != 0L) 1 else 0
    return (days + GANTT_EPOCH_DAY).toInt()
}
