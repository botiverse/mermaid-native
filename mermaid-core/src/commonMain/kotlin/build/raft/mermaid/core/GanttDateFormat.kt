package build.raft.mermaid.core

/**
 * Gantt dates as upstream parses them: `dayjs(text, dateFormat, true)` (customParseFormat,
 * strict: the parsed date must format back to the same text), in UTC. Parts the format does not
 * contain default as in dayjs: no year, month or day means today; a year without a month uses
 * the current month, and a missing day is the 1st.
 */
internal object GanttDateFormat {
    private val TOKEN = Regex("\\[[^\\]]*]|YYYY|YY|MMMM|MMM|MM|M|Do|DD|D|HH|H|hh|h|mm|m|ss|s|SSS|SS|S|A|a|.", RegexOption.DOT_MATCHES_ALL)
    private val MONTHS = listOf("january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december")
    private const val NUM12 = "(0|[1-9]\\d?)"

    fun parse(text: String, format: String, todayEpochMillis: Long): Long? {
        val tokens = TOKEN.findAll(format.trim()).map { it.value }.toList()
        val pattern = StringBuilder("^")
        tokens.forEach { token ->
            pattern.append(when (token) {
                "YYYY" -> "(\\d{4})"; "YY" -> "(\\d{2})"
                "MMMM" -> "([A-Za-z]{3,9})"; "MMM" -> "([A-Za-z]{3})"
                "MM", "DD", "HH", "hh", "mm", "ss", "SS" -> "(\\d{2})"
                "M", "D", "H", "h", "m", "s" -> NUM12
                "Do" -> "(\\d{1,2})(?:st|nd|rd|th)"
                "SSS" -> "(\\d{3})"; "S" -> "(\\d)"
                "A" -> "(AM|PM)"; "a" -> "(am|pm)"
                else -> Regex.escape(if (token.startsWith("[") && token.length > 1) token.substring(1, token.length - 1) else token)
            })
        }
        val match = Regex(pattern.append('$').toString()).matchEntire(text) ?: return null
        var year: Int? = null; var month: Int? = null; var day: Int? = null
        var hour = 0; var minute = 0; var second = 0; var millis = 0; var pm: Boolean? = null; var twelve = false
        var group = 1
        tokens.forEach { token ->
            if (token.length == 1 && token !in setOf("M", "D", "H", "h", "m", "s", "S", "A", "a") || token.startsWith("[")) return@forEach
            val value = match.groupValues[group++]
            when (token) {
                "YYYY" -> year = value.toInt()
                "YY" -> year = value.toInt().let { if (it > 68) 1900 + it else 2000 + it }
                "MMMM", "MMM" -> month = MONTHS.indexOfFirst { if (token == "MMM") it.startsWith(value.lowercase()) && value.length == 3 else it == value.lowercase() }.takeIf { it >= 0 }?.plus(1) ?: return null
                "MM", "M" -> month = value.toInt()
                "DD", "D", "Do" -> day = value.toInt()
                "HH", "H" -> hour = value.toInt()
                "hh", "h" -> { hour = value.toInt(); twelve = true }
                "mm", "m" -> minute = value.toInt()
                "ss", "s" -> second = value.toInt()
                "SSS" -> millis = value.toInt()
                "SS" -> millis = value.toInt() * 10
                "S" -> millis = value.toInt() * 100
                "A", "a" -> pm = value.lowercase() == "pm"
            }
        }
        if (twelve || pm != null) {
            if (hour !in 1..12) return null
            if (pm == true && hour < 12) hour += 12 else if (pm != true && hour == 12) hour = 0
        }
        if (hour !in 0..23 || minute !in 0..59 || second !in 0..59) return null
        val today = ganttFloorDay(todayEpochMillis)
        val todayIso = isoOfDay(today)
        val y = year ?: todayIso.first
        val m = month ?: todayIso.second
        val d = day ?: if (year == null && month == null) todayIso.third else 1
        val epochDay = parseIsoDay("${y.toString().padStart(4, '0')}-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}") ?: return null
        return (epochDay.toLong() - GANTT_EPOCH_DAY) * GANTT_DAY_MILLIS + hour * 3_600_000L + minute * 60_000L + second * 1000L + millis
    }

    private fun isoOfDay(epochDay: Int): Triple<Int, Int, Int> {
        // Civil-from-days on the proleptic Gregorian calendar (day 0 = 0000-01-01).
        val z = epochDay.toLong() - GANTT_EPOCH_DAY + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
        val y = (yoe + era * 400 + if (m <= 2) 1 else 0).toInt()
        return Triple(y, m, d)
    }
}
