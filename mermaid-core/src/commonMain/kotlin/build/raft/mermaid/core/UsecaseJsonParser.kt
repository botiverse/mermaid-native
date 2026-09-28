package build.raft.mermaid.core

/** Inert JSON data; strings are never interpreted as markup or executable code. */
public sealed interface UsecaseJsonValue {
    public data object NullValue : UsecaseJsonValue
    public data class BooleanValue(val value: Boolean) : UsecaseJsonValue
    public data class NumberValue(val value: Double) : UsecaseJsonValue
    public data class StringValue(val value: String) : UsecaseJsonValue
    public data class ArrayValue(val value: List<UsecaseJsonValue>) : UsecaseJsonValue
    public data class ObjectValue(val value: Map<String, UsecaseJsonValue>) : UsecaseJsonValue
}

public data class UsecaseOrderedJsonObject(
    val value: Map<String, UsecaseJsonValue>,
    val propertyOrder: Map<String, List<String>>,
)

public class UsecaseJsonError(message: String, public val line: Int, public val column: Int) :
    IllegalArgumentException("$message (line $line, column $column)")

/** JSON parsing and source-order collection shared by the real Usecase admission path. */
public object UsecaseJsonParser {
    public fun parseOrderedJsonObject(text: String, startLine: Int, startColumn: Int): UsecaseOrderedJsonObject =
        Reader(text, startLine, startColumn).parse()

    private class Reader(val text: String, val startLine: Int, val startColumn: Int) {
        var offset = 0
        val orders = linkedMapOf<String, List<String>>()
        fun invalid(at: Int = offset): Nothing {
            var line = startLine
            var column = startColumn
            for (i in 0 until at.coerceIn(0, text.length)) {
                when (text[i]) {
                    '\r' -> { line++; column = 1 }
                    '\n' -> if (i == 0 || text[i - 1] != '\r') { line++; column = 1 }
                    else -> column++
                }
            }
            throw UsecaseJsonError("Invalid JSON", line, column)
        }
        fun ws() { while (text.getOrNull(offset) in listOf(' ', '\t', '\r', '\n')) offset++ }
        fun take(c: Char): Boolean = if (text.getOrNull(offset) == c) { offset++; true } else false
        fun require(c: Char) { if (!take(c)) invalid() }
        fun parse(): UsecaseOrderedJsonObject {
            val root = value("")
            ws()
            if (offset != text.length) invalid()
            if (root !is UsecaseJsonValue.ObjectValue) {
                throw UsecaseJsonError("JSON value must have an object root", startLine, startColumn)
            }
            return UsecaseOrderedJsonObject(root.value, orders.toMap())
        }
        fun value(pointer: String): UsecaseJsonValue {
            ws()
            return when (text.getOrNull(offset)) {
                '{' -> objectValue(pointer)
                '[' -> arrayValue(pointer)
                '"' -> UsecaseJsonValue.StringValue(string())
                't' -> { literal("true"); UsecaseJsonValue.BooleanValue(true) }
                'f' -> { literal("false"); UsecaseJsonValue.BooleanValue(false) }
                'n' -> { literal("null"); UsecaseJsonValue.NullValue }
                '-', in '0'..'9' -> number()
                else -> invalid()
            }
        }
        fun objectValue(pointer: String): UsecaseJsonValue.ObjectValue {
            require('{'); ws()
            val fields = linkedMapOf<String, UsecaseJsonValue>()
            if (!take('}')) while (true) {
                if (text.getOrNull(offset) != '"') invalid()
                val key = string()
                val child = pointer + "/" + key.replace("~", "~0").replace("/", "~1")
                if (key in fields) orders.keys.filter { it == child || it.startsWith("$child/") }.forEach { orders.remove(it) }
                ws(); require(':')
                fields[key] = value(child)
                ws()
                if (take('}')) break
                require(','); ws()
            }
            orders[pointer] = fields.keys.toList()
            return UsecaseJsonValue.ObjectValue(fields.toMap())
        }
        fun arrayValue(pointer: String): UsecaseJsonValue.ArrayValue {
            require('['); ws()
            val items = mutableListOf<UsecaseJsonValue>()
            if (!take(']')) while (true) {
                items += value("$pointer/${items.size}")
                ws()
                if (take(']')) break
                require(','); ws()
            }
            return UsecaseJsonValue.ArrayValue(items.toList())
        }
        fun string(): String {
            require('"')
            val out = StringBuilder()
            while (offset < text.length) {
                val at = offset
                val c = text[offset++]
                if (c == '"') return out.toString()
                if (c < ' ') invalid(at)
                if (c != '\\') { out.append(c); continue }
                val escapeAt = offset
                val escape = text.getOrNull(offset++) ?: invalid(text.length)
                out.append(when (escape) {
                    '"', '\\', '/' -> escape
                    'b' -> '\b'
                    'f' -> '\u000C'
                    'n' -> '\n'
                    'r' -> '\r'
                    't' -> '\t'
                    'u' -> {
                        if (offset + 4 > text.length) invalid(offset)
                        val code = text.substring(offset, offset + 4)
                        if (code.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) invalid(offset)
                        offset += 4
                        code.toInt(16).toChar()
                    }
                    else -> invalid(escapeAt)
                })
            }
            invalid()
        }
        fun literal(token: String) {
            for (c in token) { if (text.getOrNull(offset) != c) invalid(); offset++ }
        }
        fun digit(): Boolean = text.getOrNull(offset) in '0'..'9'
        fun number(): UsecaseJsonValue.NumberValue {
            val start = offset
            take('-')
            if (!take('0')) {
                if (text.getOrNull(offset) !in '1'..'9') invalid()
                while (digit()) offset++
            }
            if (take('.')) { if (!digit()) invalid(); while (digit()) offset++ }
            if (take('e') || take('E')) {
                if (!take('+')) take('-')
                if (!digit()) invalid()
                while (digit()) offset++
            }
            return UsecaseJsonValue.NumberValue(text.substring(start, offset).toDoubleOrNull() ?: invalid(start))
        }
    }
}
