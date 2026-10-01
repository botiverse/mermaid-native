package build.raft.mermaid.core

/** Supported font-size units in the platform-neutral style model. */
public enum class FontSizeUnit { PX, EM }

/** A parsed size retaining its unit until the host style supplies a base size. */
public data class FontSize(val value: Double, val unit: FontSizeUnit) {
    public val cssValue: String get() = value.toString().removeSuffix(".0") + if (unit == FontSizeUnit.EM) "em" else "px"

    /** Resolve relative sizes using the existing design's base font, retaining its 512px limit. */
    public fun resolvePixels(baseFontSize: Double): Double? {
        val pixels = if (unit == FontSizeUnit.EM) value * baseFontSize else value
        return pixels.takeIf { it.isFinite() && it > 0.0 && it <= 512.0 &&
            (unit != FontSizeUnit.EM || (baseFontSize.isFinite() && baseFontSize > 0.0)) }
    }

    public companion object {
        private val syntax = Regex("([+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:e[+-]?[0-9]+)?)(px|em)?")

        /** Parse numbers as pixels; accept px/em strings, including fractional sizes. */
        public fun parse(input: Any?): FontSize? = when (input) {
            is Number -> input.toDouble().takeIf { it.isFinite() }?.let { FontSize(it, FontSizeUnit.PX) }
            is String -> syntax.matchEntire(input.trim().lowercase())?.let { match ->
                match.groupValues[1].toDoubleOrNull()?.takeIf { it.isFinite() }?.let { value ->
                    FontSize(value, if (match.groupValues[2] == "em") FontSizeUnit.EM else FontSizeUnit.PX)
                }
            }
            else -> null
        }
    }
}
