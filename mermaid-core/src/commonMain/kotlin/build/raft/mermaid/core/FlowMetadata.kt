package build.raft.mermaid.core

/** Scalar YAML mapping used by flowchart node and edge metadata. No tag/object construction. */
internal fun flowMetadata(source: String): Map<String, String> {
    val values = linkedMapOf<String, String>()
    var at = 0
    fun fail(): Nothing = throw IllegalArgumentException("Invalid flowchart metadata")
    while (at < source.length) {
        while (at < source.length && (source[at].isWhitespace() || source[at] == ',')) at++
        if (at == source.length) break
        val start = at
        while (at < source.length && source[at] != ':' && source[at] != '\n') at++
        if (at == source.length || source[at] != ':') fail()
        val key = source.substring(start, at++).trim()
        if (key.isEmpty() || key in values) fail()
        while (at < source.length && source[at] in " \t") at++
        val value: String
        if (source.getOrNull(at) == '"' || source.getOrNull(at) == '\'') {
            val quote = source[at++]
            val result = StringBuilder()
            var closed = false
            while (at < source.length) {
                val c = source[at++]
                if (c == quote) { closed = true; break }
                if (c == '\\' && quote == '"') {
                    if (at == source.length) fail()
                    result.append(when (val escaped = source[at++]) { 'n' -> '\n'; 't' -> '\t'; 'r' -> '\r'; '"' -> '"'; '\\' -> '\\'; else -> fail() })
                } else if (c == '\n') {
                    result.append("<br/>")
                    while (at < source.length && source[at] in " \t") at++
                } else result.append(c)
            }
            if (!closed) fail()
            value = result.toString()
        } else if (source.getOrNull(at) in listOf('|', '>')) {
            val folded = source[at++] == '>'
            val strip = source.getOrNull(at) == '-'
            if (strip || source.getOrNull(at) == '+') at++
            while (at < source.length && source[at] in " \t") at++
            if (source.getOrNull(at) != '\n') fail()
            at++
            val lines = mutableListOf<String>()
            var indent: Int? = null
            while (at < source.length) {
                val lineStart = at
                val end = source.indexOf('\n', at).let { if (it < 0) source.length else it }
                val raw = source.substring(at, end)
                val spaces = raw.takeWhile { it == ' ' }.length
                if (raw.isNotBlank() && indent != null && spaces < indent!!) break
                if (raw.isNotBlank() && indent == null) indent = spaces
                lines += raw.drop(indent ?: spaces)
                at = end + if (end < source.length) 1 else 0
                if (at == lineStart) break
            }
            value = lines.joinToString(if (folded) " " else "\n").trimEnd('\n') + if (strip) "" else "\n"
        } else {
            val begin = at
            while (at < source.length && source[at] != ',' && source[at] != '\n') at++
            value = source.substring(begin, at).trim()
            if (value.isEmpty() || value.first() in "[{}!&*") fail()
        }
        values[key] = value
    }
    return values
}
