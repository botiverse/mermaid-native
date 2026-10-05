package build.raft.mermaid.core

/** Inert, typed configuration data, shared with the Native JSON boundary. */
public typealias MermaidConfigValue = UsecaseJsonValue

public data class MermaidFrontmatterMetadata(
    val title: String? = null,
    val displayMode: String? = null,
    val config: MermaidConfigValue? = null,
)

public data class MermaidFrontmatterResult(
    val text: String,
    val metadata: MermaidFrontmatterMetadata,
    /** UTF-16 offset of the diagram body in the original source. */
    val bodyOffset: Int = 0,
)

public class MermaidFrontmatterError(message: String, public val location: SourceLocation) :
    IllegalArgumentException(message)

/**
 * YAML frontmatter extraction. Supports mappings, sequences, flow collections,
 * quoted/plain scalars and literal/folded blocks. Tags, anchors, aliases, merge
 * keys and explicit complex keys fail explicitly; this is not a general YAML loader.
 * Document size/depth are bounded before recursive parsing.
 */
public object MermaidFrontmatter {
    public fun extract(source: String): MermaidFrontmatterResult {
        val match = delimiter.find(source) ?: return MermaidFrontmatterResult(source, MermaidFrontmatterMetadata())
        val indent = match.groupValues[1]
        val yaml = match.groupValues[2].split('\n').joinToString("\n") { if (it.startsWith(indent)) it.drop(indent.length) else it }
        val yamlStart = match.groups[2]!!.range.first
        val firstLine = source.take(yamlStart).replace("\r\n", "\n").replace('\r', '\n').count { it == '\n' } + 1
        val value = YamlReader(yaml, firstLine, indent.length).parse()
        val fields = (value as? UsecaseJsonValue.ObjectValue)?.value.orEmpty()
        val metadata = MermaidFrontmatterMetadata(
            fields["title"]?.takeIf { it.truthy() }?.jsString(),
            fields["displayMode"]?.takeIf { it.truthy() }?.jsString(),
            fields["config"]?.takeIf { it.truthy() },
        )
        val end = match.range.last + 1
        return MermaidFrontmatterResult(source.substring(end), metadata, end)
    }

    private val delimiter = Regex("^([^\\S\\n\\r]*)-{3}\\s*[\\n\\r]([\\s\\S]*?)[\\n\\r]\\1-{3}\\s*[\\n\\r]+")
}

private fun UsecaseJsonValue.truthy(): Boolean = when (this) {
    UsecaseJsonValue.NullValue -> false
    is UsecaseJsonValue.BooleanValue -> value
    is UsecaseJsonValue.NumberValue -> value != 0.0 && !value.isNaN()
    is UsecaseJsonValue.StringValue -> value.isNotEmpty()
    else -> true
}
private fun UsecaseJsonValue.jsString(): String = when (this) {
    UsecaseJsonValue.NullValue -> "null"
    is UsecaseJsonValue.BooleanValue -> value.toString()
    is UsecaseJsonValue.NumberValue -> if (value.isFinite() && value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    is UsecaseJsonValue.StringValue -> value
    is UsecaseJsonValue.ArrayValue -> value.joinToString(",") { if (it == UsecaseJsonValue.NullValue) "" else it.jsString() }
    is UsecaseJsonValue.ObjectValue -> "[object Object]"
}

private class YamlReader(source: String, val firstLine: Int, val dedent: Int) {
    val lines = source.replace("\r\n", "\n").replace('\r', '\n').split('\n')
    var index = 0
    init { if (source.length > 262144) fail("Frontmatter exceeds 256 KiB") }
    fun fail(message: String, line: Int = index, column: Int = 0): Nothing =
        throw MermaidFrontmatterError(message, SourceLocation(firstLine + line, dedent + column + 1))
    fun indentation(line: Int): Int {
        val prefix = lines[line].takeWhile { it == ' ' || it == '\t' }
        if ('\t' in prefix) fail("Tabs cannot indent YAML content", line, prefix.indexOf('\t'))
        return prefix.length
    }
    fun skip() { while (index < lines.size && (lines[index].isBlank() || lines[index].trimStart().startsWith('#'))) index++ }
    fun parse(): UsecaseJsonValue {
        skip(); if (index == lines.size) return UsecaseJsonValue.NullValue
        val value = block(indentation(index), 0)
        skip(); if (index != lines.size) fail("Unexpected YAML indentation or trailing content")
        return value
    }
    fun block(indent: Int, depth: Int): UsecaseJsonValue {
        if (depth > 64) fail("Frontmatter nesting exceeds 64 levels")
        skip(); if (index == lines.size || indentation(index) < indent) return UsecaseJsonValue.NullValue
        val first = lines[index].substring(indent)
        if (first == "-" || first.startsWith("- ")) {
            val items = mutableListOf<UsecaseJsonValue>()
            while (true) {
                skip(); if (index == lines.size || indentation(index) < indent) break
                if (indentation(index) != indent) fail("Unexpected sequence indentation")
                val text = lines[index].substring(indent)
                if (text != "-" && !text.startsWith("- ")) fail("Expected YAML sequence item")
                val line = index++; val tail = text.drop(1).trimStart()
                if (mappingColon(tail) >= 0) fail("Inline block mappings in sequences are not supported; use a flow mapping", line, indent + 2)
                items += entry(tail, indent, line, indent + text.indexOf(tail), depth + 1)
            }
            return UsecaseJsonValue.ArrayValue(items.toList())
        }
        if (mappingColon(first) >= 0) {
            val fields = linkedMapOf<String, UsecaseJsonValue>()
            while (true) {
                skip(); if (index == lines.size || indentation(index) < indent) break
                if (indentation(index) != indent) fail("Unexpected mapping indentation")
                val text = lines[index].substring(indent); val colon = mappingColon(text)
                if (colon < 0) fail("Expected YAML mapping")
                val line = index++; val rawKey = text.take(colon).trim(); val key = scalar(rawKey, line, indent, depth + 1).jsString()
                if (key == "<<") fail("YAML merge keys are not supported", line, indent)
                if (key in fields) fail("Duplicated YAML mapping key '$key'", line, indent)
                val tail = text.drop(colon + 1).trimStart()
                fields[key] = entry(tail, indent, line, indent + colon + 1 + text.drop(colon + 1).length - tail.length, depth + 1)
            }
            return UsecaseJsonValue.ObjectValue(fields.toMap())
        }
        // Plain multi-line scalar (e.g. a non-mapping YAML document).
        val line = index++; val parts = mutableListOf(first)
        while (index < lines.size && !lines[index].isBlank() && indentation(index) >= indent) {
            val next = lines[index].substring(indent)
            if (mappingColon(next) >= 0) fail("Unexpected mapping after scalar")
            parts += next.trim(); index++
        }
        return scalar(stripComment(parts.joinToString(" ")).trimEnd(), line, indent, depth + 1)
    }
    fun entry(text: String, parentIndent: Int, line: Int, column: Int, depth: Int): UsecaseJsonValue {
        val clean = stripComment(text).trimEnd()
        if (clean.isEmpty()) {
            skip()
            return if (index < lines.size && indentation(index) > parentIndent) block(indentation(index), depth) else UsecaseJsonValue.NullValue
        }
        if (clean.first() == '|' || clean.first() == '>') {
            if (!Regex("[|>][+-]?").matches(clean)) fail("Unsupported YAML block scalar indicator", line, column)
            val chunks = mutableListOf<String>(); var contentIndent: Int? = null
            while (index < lines.size) {
                val next = lines[index]
                if (next.isNotBlank() && indentation(index) <= parentIndent) break
                if (next.isNotBlank() && contentIndent == null) contentIndent = indentation(index)
                if (next.isNotBlank() && indentation(index) < contentIndent!!) fail("Invalid block scalar indentation")
                chunks += if (next.isBlank()) "" else next.drop(contentIndent!!)
                index++
            }
            var value = if (clean.first() == '|') chunks.joinToString("\n") else buildString {
                for (i in chunks.indices) { append(chunks[i]); if (i < chunks.lastIndex) append(if (chunks[i].isBlank() || chunks[i+1].isBlank() || chunks[i].startsWith(' ') || chunks[i+1].startsWith(' ')) '\n' else ' ') }
            }
            value += "\n"
            value = when (clean.last()) { '-' -> value.trimEnd('\n'); '+' -> value; else -> if (chunks.any { it.isNotEmpty() }) value.trimEnd('\n') + "\n" else "" }
            return UsecaseJsonValue.StringValue(value)
        }
        return scalar(clean, line, column, depth)
    }
    fun scalar(text: String, line: Int, column: Int, depth: Int): UsecaseJsonValue {
        if (depth > 64) fail("Frontmatter nesting exceeds 64 levels", line, column)
        if (text.startsWith("!!!")) fail("tag suffix cannot contain exclamation marks", line, column)
        if (text.firstOrNull() in listOf('!', '&', '*', '?', '@', '`')) fail("Unsupported YAML tag, anchor, alias or complex key", line, column)
        val parser = FlowReader(text, line, column, depth)
        val result = parser.value(); parser.ws()
        if (parser.pos != text.length) fail("Unexpected YAML scalar content", line, column + parser.pos)
        return result
    }
    inner class FlowReader(val text: String, val line: Int, val column: Int, val depth: Int) {
        var pos = 0
        fun ws() { while (pos < text.length && text[pos].isWhitespace()) pos++ }
        fun error(message: String): Nothing = fail(message, line, column + pos)
        fun value(level: Int = depth): UsecaseJsonValue {
            if (level > 64) error("Frontmatter nesting exceeds 64 levels")
            ws()
            return when (text.getOrNull(pos)) {
                '[' -> {
                    pos++; ws(); val values = mutableListOf<UsecaseJsonValue>()
                    while (text.getOrNull(pos) != ']') {
                        if (pos == text.length) error("Unclosed YAML sequence")
                        if (text[pos] == ',') error("Missing YAML sequence value")
                        values += value(level + 1); ws()
                        if (text.getOrNull(pos) == ']') break
                        if (text.getOrNull(pos++) != ',') error("Expected comma in YAML sequence")
                        ws()
                    }
                    pos++; UsecaseJsonValue.ArrayValue(values.toList())
                }
                '{' -> {
                    pos++; ws(); val fields = linkedMapOf<String, UsecaseJsonValue>()
                    while (text.getOrNull(pos) != '}') {
                        val key = if (text.getOrNull(pos) in listOf('\'', '"')) quoted() else {
                            val start = pos; while (pos < text.length && text[pos] !in ":,}") pos++
                            text.substring(start, pos).trim()
                        }
                        ws(); if (key.isEmpty() || text.getOrNull(pos++) != ':') error("Expected YAML mapping key")
                        if (key == "<<" || key in fields) error("Unsupported merge or duplicate YAML key")
                        fields[key] = value(level + 1); ws()
                        if (text.getOrNull(pos) == '}') break
                        if (text.getOrNull(pos++) != ',') error("Expected comma in YAML mapping")
                        ws()
                    }
                    pos++; UsecaseJsonValue.ObjectValue(fields.toMap())
                }
                '\'', '"' -> UsecaseJsonValue.StringValue(quoted())
                else -> {
                    val start = pos
                    while (pos < text.length && (level == depth || text[pos] !in ",]}")) pos++
                    val raw = text.substring(start, pos).trim()
                    if (mappingColon(raw) >= 0) error("A colon followed by whitespace requires a quoted YAML scalar")
                    if (Regex("[-+]?(?:0[xob].*|\\.(?:inf|nan))", RegexOption.IGNORE_CASE).matches(raw))
                        error("Non-decimal or non-finite YAML numbers are not supported")
                    if (raw.firstOrNull() in listOf('!', '&', '*', '?', '@', '`')) error("Unsupported YAML tag, anchor, alias or complex key")
                    when {
                        raw.isEmpty() || raw in listOf("null", "Null", "NULL", "~") -> UsecaseJsonValue.NullValue
                        raw in listOf("true", "True", "TRUE") -> UsecaseJsonValue.BooleanValue(true)
                        raw in listOf("false", "False", "FALSE") -> UsecaseJsonValue.BooleanValue(false)
                        number.matches(raw) -> UsecaseJsonValue.NumberValue(raw.replace("_", "").toDoubleOrNull() ?: error("Invalid YAML number"))
                        else -> UsecaseJsonValue.StringValue(raw)
                    }
                }
            }
        }
        fun quoted(): String {
            val quote = text[pos++]; val out = StringBuilder()
            while (pos < text.length) {
                val c = text[pos++]
                if (c == quote) {
                    if (quote == '\'' && text.getOrNull(pos) == '\'') { pos++; out.append('\''); continue }
                    return out.toString()
                }
                if (quote == '"' && c == '\\') {
                    val escape = text.getOrNull(pos++) ?: error("Incomplete YAML escape")
                    out.append(when (escape) {
                        '0' -> '\u0000'; 'a' -> '\u0007'; 'b' -> '\b'; 't', '\t' -> '\t'; 'n' -> '\n'; 'v' -> '\u000B'; 'f' -> '\u000C'; 'r' -> '\r'; 'e' -> '\u001B'; ' ', '"', '/', '\\' -> escape
                        'N' -> '\u0085'; '_' -> '\u00A0'; 'L' -> '\u2028'; 'P' -> '\u2029'
                        'x', 'u' -> { val size = if (escape == 'x') 2 else 4; val hex = text.substring(pos, minOf(pos + size, text.length)); if (hex.length != size) error("Incomplete YAML unicode escape"); pos += size; (hex.toIntOrNull(16) ?: error("Invalid YAML unicode escape")).toChar() }
                        else -> error("Unsupported YAML escape")
                    })
                } else out.append(c)
            }
            error("Unclosed YAML quoted scalar")
        }
    }
    companion object {
        val number = Regex("[-+]?(?:[0-9][0-9_]*(?:\\.[0-9_]*)?|\\.[0-9_]+)(?:[eE][-+]?[0-9]+)?")
        fun mappingColon(text: String): Int {
            var quote: Char? = null; var nested = 0; var escaped = false
            text.forEachIndexed { i, c ->
                if (escaped) { escaped = false; return@forEachIndexed }
                if (quote != null) { if (c == '\\' && quote == '"') escaped = true else if (c == quote) quote = null }
                else when (c) { '\'', '"' -> quote = c; '[', '{' -> nested++; ']', '}' -> nested--; ':' -> if (nested == 0 && (i == text.lastIndex || text[i+1].isWhitespace())) return i }
            }
            return -1
        }
        fun stripComment(text: String): String {
            var quote: Char? = null; var escaped = false
            text.forEachIndexed { i, c ->
                if (escaped) { escaped = false; return@forEachIndexed }
                if (quote != null) { if (c == '\\' && quote == '"') escaped = true else if (c == quote) quote = null }
                else if (c == '\'' || c == '"') quote = c else if (c == '#' && (i == 0 || text[i-1].isWhitespace())) return text.take(i)
            }
            return text
        }
    }
}
