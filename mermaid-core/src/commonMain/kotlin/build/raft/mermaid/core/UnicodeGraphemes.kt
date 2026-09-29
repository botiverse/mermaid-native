package build.raft.mermaid.core

/** Unicode 17.0 extended grapheme clusters (UAX #29, revision 47). No normalization. */
public object UnicodeGraphemes {
    private data class Point(val value: Int, val offset: Int)
    private fun points(text: String): List<Point> = buildList {
        var i = 0
        while (i < text.length) {
            val first = text[i]
            if (first.isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()) {
                add(Point(0x10000 + ((first.code - 0xD800) shl 10) + text[i + 1].code - 0xDC00, i))
                i += 2
            } else { add(Point(first.code, i)); i++ }
        }
    }

    /** UTF-16 pairs stay together; isolated surrogates are preserved without data loss. */
    public fun codePoints(text: String): List<String> {
        val points = points(text)
        return points.mapIndexed { i, p -> text.substring(p.offset, points.getOrNull(i + 1)?.offset ?: text.length) }
    }

    private fun property(code: Int, table: IntArray): Int {
        var lo = 0; var hi = table.size / 3 - 1
        while (lo <= hi) {
            val mid = (lo + hi) / 2; val offset = mid * 3
            when {
                code < table[offset] -> hi = mid - 1
                code > table[offset + 1] -> lo = mid + 1
                else -> return table[offset + 2]
            }
        }
        return 0
    }

    private fun breakProperty(code: Int): Int = if (code in 0xAC00..0xD7A3) {
        if ((code - 0xAC00) % 28 == 0) 12 else 13 // LV / LVT syllables
    } else property(code, UnicodeGraphemeData.breaks)

    public fun split(text: String): List<String> {
        val points = points(text)
        if (points.isEmpty()) return emptyList()
        val properties = points.map { breakProperty(it.value) }
        val indic = points.map { property(it.value, UnicodeGraphemeData.indic) }
        val pictographic = points.map { property(it.value, UnicodeGraphemeData.pictographic) != 0 }
        val result = mutableListOf<String>(); var start = 0
        var regionalRun = if (properties[0] == 6) 1 else 0
        for (i in 1 until points.size) {
            val left = properties[i - 1]; val right = properties[i]
            val joins = when {
                left == 1 && right == 2 -> true // GB3: CR LF
                left in 1..3 || right in 1..3 -> false // GB4/5: controls
                left == 9 && (right == 9 || right == 10 || right == 12 || right == 13) -> true // GB6
                (left == 12 || left == 10) && (right == 10 || right == 11) -> true // GB7
                (left == 13 || left == 11) && right == 11 -> true // GB8
                right == 4 || right == 5 || right == 8 || left == 7 -> true // GB9/9a/9b
                indic[i] == 1 && indicConjunct(indic, i) -> true // GB9c
                pictographic[i] && left == 5 && emojiSequence(properties, pictographic, i) -> true // GB11
                left == 6 && right == 6 && regionalRun % 2 == 1 -> true // GB12/13
                else -> false
            }
            if (!joins) { result += text.substring(start, points[i].offset); start = points[i].offset }
            regionalRun = if (right == 6) regionalRun + 1 else 0
        }
        result += text.substring(start)
        return result
    }

    private fun indicConjunct(properties: List<Int>, index: Int): Boolean {
        var i = index - 1; var linker = false
        while (i >= 0 && properties[i] in 2..3) { if (properties[i] == 3) linker = true; i-- }
        return linker && i >= 0 && properties[i] == 1
    }

    private fun emojiSequence(properties: List<Int>, pictographic: List<Boolean>, index: Int): Boolean {
        var i = index - 2 // before the ZWJ
        while (i >= 0 && properties[i] == 4) i--
        return i >= 0 && pictographic[i]
    }
}
