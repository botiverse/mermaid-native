package build.raft.mermaid.core

/** Plain text transformations shared by model formatting and Native layout. No HTML evaluation. */
public object MermaidText {
    private val lineBreak = Regex("</?br\\s*/?>", RegexOption.IGNORE_CASE)
    public fun hasBreaks(text: String): Boolean = lineBreak.containsMatchIn(text)
    public fun splitBreaks(text: String): List<String> = if (hasBreaks(text)) lineBreak.split(text) else listOf(text)
    public fun countOccurrence(text: String, substring: String): Int {
        if (substring.isEmpty()) return (text.length - 1).coerceAtLeast(0)
        var count = 0; var start = 0
        while (start <= text.length) {
            val index = text.indexOf(substring, start)
            if (index < 0) break
            count++; start = index + substring.length
        }
        return count
    }

    public fun parseGenericTypes(value: String): String {
        val parts = mutableListOf<String>()
        var start = 0
        value.forEachIndexed { index, c -> if (c == ',') { parts += value.substring(start, index); parts += ","; start = index + 1 } }
        parts += value.substring(start)
        val output = mutableListOf<String>()
        var i = 0
        while (i < parts.size) {
            var part = parts[i]
            if (part == "," && i > 0 && i + 1 < parts.size && countOccurrence(parts[i - 1], "~") == 1 && countOccurrence(parts[i + 1], "~") == 1) {
                part = parts[i - 1] + "," + parts[i + 1]; i++; output.removeAt(output.lastIndex)
            }
            output += genericSet(part); i++
        }
        return output.joinToString("")
    }

    private fun genericSet(value: String): String {
        val count = countOccurrence(value, "~")
        if (count <= 1) return value
        val prefix = count % 2 != 0 && value.startsWith('~')
        val chars = (if (prefix) value.drop(1) else value).toCharArray()
        var first = chars.indexOf('~'); var last = chars.lastIndexOf('~')
        while (first >= 0 && first != last) {
            chars[first] = '<'; chars[last] = '>'
            first = chars.indexOf('~'); last = chars.lastIndexOf('~')
        }
        return (if (prefix) "~" else "") + chars.concatToString()
    }
}
