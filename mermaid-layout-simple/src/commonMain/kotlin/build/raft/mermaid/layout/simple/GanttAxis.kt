package build.raft.mermaid.layout.simple

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * The axis of upstream's ganttRenderer `makeGrid`: d3-time ticks and d3-time-format labels
 * (en-US locale), in UTC like the rest of the Native Gantt model.
 */
internal object GanttAxis {
    private const val SECOND = 1000L
    private const val MINUTE = 60 * SECOND
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR
    private const val WEEK = 7 * DAY
    private const val MONTH = 30 * DAY
    private const val YEAR = 365 * DAY
    /** Upstream's MAX_TICK_COUNT; also the most ticks Native generates. */
    private const val MAX_TICKS = 10_000
    /** Native bound on the candidate times examined for one interval. */
    private const val MAX_CANDIDATES = 1_000_000L
    private val INTERVAL = Regex("^([1-9][0-9]*)(millisecond|second|minute|hour|day|week|month)$")
    private val WEEKDAYS = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private val MONTHS = listOf("January", "February", "March", "April", "May", "June", "July", "August",
        "September", "October", "November", "December")

    /** d3's automatic steps: unit, count and nominal duration. */
    private val AUTO = listOf(
        Triple("second", 1, SECOND), Triple("second", 5, 5 * SECOND), Triple("second", 15, 15 * SECOND), Triple("second", 30, 30 * SECOND),
        Triple("minute", 1, MINUTE), Triple("minute", 5, 5 * MINUTE), Triple("minute", 15, 15 * MINUTE), Triple("minute", 30, 30 * MINUTE),
        Triple("hour", 1, HOUR), Triple("hour", 3, 3 * HOUR), Triple("hour", 6, 6 * HOUR), Triple("hour", 12, 12 * HOUR),
        Triple("day", 1, DAY), Triple("day", 2, 2 * DAY), Triple("week", 1, WEEK),
        Triple("month", 1, MONTH), Triple("month", 3, 3 * MONTH), Triple("year", 1, YEAR),
    )

    /** Ticks of a `tickInterval`, or null when upstream ignores it (malformed, or too many ticks). */
    fun intervalTicks(interval: String, weekday: String, start: Long, end: Long): List<Long>? {
        val match = INTERVAL.matchEntire(interval) ?: return null
        val every = match.groupValues[1].toIntOrNull() ?: return null
        val unit = match.groupValues[2]
        // dayjs durations: a month is a twelfth of 365 days.
        val unitMillis = when (unit) {
            "millisecond" -> 1L; "second" -> SECOND; "minute" -> MINUTE; "hour" -> HOUR
            "day" -> DAY; "week" -> WEEK; else -> YEAR / 12
        }
        if (ceil((end - start).toDouble() / (unitMillis.toDouble() * every)) > MAX_TICKS) return null
        val weekStart = WEEKDAYS.indexOfFirst { it.lowercase() == weekday }.coerceAtLeast(0)
        return ticks(unit, every.toLong(), weekStart, start, end)
    }

    /** `scaleTime().ticks(10)`. */
    fun autoTicks(start: Long, end: Long): List<Long> {
        val target = abs(end - start) / 10.0
        val i = AUTO.indexOfFirst { it.third > target }.let { if (it < 0) AUTO.size else it }
        val result = when (i) {
            AUTO.size -> ticks("year", tickStep(start.toDouble() / YEAR, end.toDouble() / YEAR).toLong(), 0, start, end)
            0 -> ticks("millisecond", maxOf(tickStep(start.toDouble(), end.toDouble()), 1.0).toLong(), 0, start, end)
            else -> AUTO[if (target / AUTO[i - 1].third < AUTO[i].third / target) i - 1 else i]
                .let { (unit, every) -> ticks(unit, every.toLong(), 0, start, end) }
        }
        return result.orEmpty()
    }

    /** d3-array `tickStep(start, stop, 10)`. */
    private fun tickStep(start: Double, stop: Double): Double {
        val step = (stop - start) / 10
        val power = floor(log10(step))
        val error = step / 10.0.pow(power)
        val factor = when { error >= sqrt(50.0) -> 10; error >= sqrt(10.0) -> 5; error >= sqrt(2.0) -> 2; else -> 1 }
        return 10.0.pow(power) * factor
    }

    /** `interval.every(every).range(start, end + 1)`. */
    private fun ticks(unit: String, every: Long, weekStart: Int, start: Long, end: Long): List<Long>? {
        val result = mutableListOf<Long>()
        fun add(time: Long): Boolean {
            if (time in start..end) result += time
            return result.size <= MAX_TICKS
        }
        if (unit == "month" || unit == "year") {
            val from = civil(start.floorDiv(DAY))
            val to = civil(end.floorDiv(DAY))
            for (year in from.year..to.year) {
                if (unit == "year") {
                    if (year.mod(every) == 0L && !add(epochDay(year, 1, 1) * DAY)) return null
                } else {
                    for (month in 0 until 12) {
                        if (month % every == 0L && !add(epochDay(year, month + 1, 1) * DAY)) return null
                    }
                }
            }
            return result
        }
        // The epoch was a Thursday; week boundaries start on `weekStart`.
        val offset = if (unit == "week") ((weekStart - 4).mod(7) - 7) % 7 * DAY else 0L
        val step = when (unit) {
            "millisecond" -> every; "second" -> SECOND; "minute" -> MINUTE; "hour" -> HOUR; "day" -> DAY; else -> WEEK
        }
        if ((end - start) / step > MAX_CANDIDATES) return null
        var time = offset + -((offset - start).floorDiv(step)) * step
        while (time <= end) {
            val index = (time - offset).floorDiv(step)
            val field = when (unit) {
                "millisecond" -> 0L
                "second", "minute" -> index.mod(60L)
                "hour" -> index.mod(24L)
                "day" -> civil(index).day - 1L
                else -> index
            }
            if ((every == 1L || field.mod(every) == 0L) && !add(time)) return null
            time += step
        }
        return result
    }

    /** d3 `timeFormat(specifier)` for a UTC time. */
    fun format(specifier: String, time: Long): String {
        val day = time.floorDiv(DAY)
        val ofDay = time.mod(DAY)
        val date = civil(day)
        val weekday = (day + 4).mod(7L).toInt()
        val hour = ofDay / HOUR
        val dayOfYear = day - epochDay(date.year, 1, 1)
        // The Thursday of the ISO week decides its year.
        val isoThursday = day + 3 - (weekday + 6) % 7
        val isoYear = civil(isoThursday).year
        val out = StringBuilder()
        var i = 0
        while (i < specifier.length) {
            if (specifier[i] != '%') { out.append(specifier[i++]); continue }
            i++
            var pad: Char? = '0'
            var explicit = false
            if (i < specifier.length && specifier[i] in "-_0") {
                pad = when (specifier[i]) { '-' -> null; '_' -> ' '; else -> '0' }
                explicit = true
                i++
            }
            if (i >= specifier.length) break
            val code = specifier[i++]
            if (!explicit && code == 'e') pad = ' '
            val fill = pad
            fun number(value: Long, width: Int): String {
                val digits = abs(value).toString()
                return (if (value < 0) "-" else "") + if (fill == null) digits else digits.padStart(width, fill)
            }
            out.append(when (code) {
                'a' -> WEEKDAYS[weekday].take(3)
                'A' -> WEEKDAYS[weekday]
                'b' -> MONTHS[date.month - 1].take(3)
                'B' -> MONTHS[date.month - 1]
                'c' -> format("%-m/%-d/%Y, %-I:%M:%S %p", time)
                'd', 'e' -> number(date.day.toLong(), 2)
                'f' -> number(ofDay % SECOND, 3) + "000"
                'g' -> number(isoYear % 100, 2)
                'G' -> number(isoYear % 10000, 4)
                'H' -> number(hour, 2)
                'I' -> number(if (hour % 12 == 0L) 12L else hour % 12, 2)
                'j' -> number(dayOfYear + 1, 3)
                'L' -> number(ofDay % SECOND, 3)
                'm' -> number(date.month.toLong(), 2)
                'M' -> number(ofDay / MINUTE % 60, 2)
                'p' -> if (hour >= 12) "PM" else "AM"
                'q' -> ((date.month - 1) / 3 + 1).toString()
                'Q' -> time.toString()
                's' -> time.floorDiv(SECOND).toString()
                'S' -> number(ofDay / SECOND % 60, 2)
                'u' -> (if (weekday == 0) 7 else weekday).toString()
                'U' -> number((dayOfYear + 7 - weekday) / 7, 2)
                'V' -> number((isoThursday - epochDay(isoYear, 1, 1)) / 7 + 1, 2)
                'w' -> weekday.toString()
                'W' -> number((dayOfYear + 7 - (weekday + 6) % 7) / 7, 2)
                'x' -> format("%-m/%-d/%Y", time)
                'X' -> format("%-I:%M:%S %p", time)
                'y' -> number(date.year % 100, 2)
                'Y' -> number(date.year % 10000, 4)
                'Z' -> "+0000"
                else -> code.toString() // `%%`, and unknown directives as in d3
            })
        }
        return out.toString()
    }

    private class CivilDate(val year: Long, val month: Int, val day: Int)

    /** Proleptic Gregorian date of a day counted from 1970-01-01. */
    private fun civil(day: Long): CivilDate {
        val z = day + 719468
        val era = z.floorDiv(146097L)
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
        return CivilDate(yoe + era * 400 + if (month <= 2) 1 else 0, month, (doy - (153 * mp + 2) / 5 + 1).toInt())
    }

    private fun epochDay(year: Long, month: Int, day: Int): Long {
        val y = if (month <= 2) year - 1 else year
        val era = y.floorDiv(400L)
        val yoe = y - era * 400
        val doy = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
        return era * 146097 + yoe * 365 + yoe / 4 - yoe / 100 + doy - 719468
    }
}
