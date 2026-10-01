package build.raft.mermaid.core

/** Whole-line Mermaid comment cleanup, retaining %%{...} directives. */
public object MermaidComments {
    public fun cleanup(source: String): String = mapped(source).text

    // Spell out ECMAScript whitespace so JVM and JS/Wasm use the same set.
    private const val SPACE = "\\t\\n\\u000B\\u000C\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF"
    private val comments = Regex("^[$SPACE]*%%(?!\\{)[^\\r\\n]+(?:\\r\\n|\\r|\\n)?", RegexOption.MULTILINE)
    private val leading = Regex("^[$SPACE]+")

    internal fun mapped(source: String): CommentSource {
        val text = StringBuilder()
        val slices = mutableListOf<CommentSlice>()
        var cursor = 0
        fun retain(end: Int) {
            if (end > cursor) {
                slices += CommentSlice(text.length, cursor)
                text.append(source, cursor, end)
            }
        }
        comments.findAll(source).forEach { match ->
            retain(match.range.first)
            cursor = match.range.last + 1
        }
        retain(source.length)
        val joined = text.toString()
        val trim = leading.find(joined)?.value?.length ?: 0
        val lineStarts = mutableListOf(0)
        var index = 0
        while (index < source.length) {
            val char = source[index++]
            if (char == '\r') {
                if (source.getOrNull(index) == '\n') index++
                lineStarts += index
            } else if (char == '\n') lineStarts += index
        }
        return CommentSource(joined.substring(trim), trim, slices, lineStarts)
    }
}

internal data class CommentSlice(val cleanedStart: Int, val originalStart: Int)

/** Compact retained-span map; diagnostics still refer to the unmodified input. */
internal class CommentSource(
    val text: String,
    private val trimmed: Int,
    private val slices: List<CommentSlice>,
    private val lineStarts: List<Int>,
) {
    fun location(offset: Int): SourceLocation {
        val cleaned = offset + trimmed
        val found = slices.binarySearchBy(cleaned) { it.cleanedStart }
        val slice = slices[if (found >= 0) found else -found - 2]
        val original = slice.originalStart + cleaned - slice.cleanedStart
        val lineFound = lineStarts.binarySearch(original)
        val line = if (lineFound >= 0) lineFound else -lineFound - 2
        return SourceLocation(line + 1, original - lineStarts[line] + 1)
    }
}
