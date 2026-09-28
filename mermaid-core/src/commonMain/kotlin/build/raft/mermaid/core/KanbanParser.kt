package build.raft.mermaid.core

/** Indented sections with deeper items flattened into their current section. */
internal class KanbanParser(private val source: String) {
    fun parse(): MermaidParseResult {
        val columns = mutableListOf<KanbanColumn>()
        var sectionIndent: Int? = null
        var lastWasCard = false
        var header = false
        val lines = source.lines()
        var index = 0
        fun fail(message: String) = MermaidParseResult.Failure(listOf(
            MermaidDiagnostic(MermaidDiagnosticCode.INVALID_VALUE, message, SourceLocation(index.coerceAtLeast(1), 1)),
        ))
        fun decorate(change: (KanbanMetadata) -> KanbanMetadata): Boolean {
            if (columns.isEmpty()) return false
            val column = columns.last()
            columns[columns.lastIndex] = if (lastWasCard) {
                column.copy(cards = column.cards.dropLast(1) + column.cards.last().let { it.copy(metadata = change(it.metadata)) })
            } else column.copy(metadata = change(column.metadata))
            return true
        }
        while (index < lines.size) {
            val raw = lines[index++]
            var text = stripComment(raw).trim()
            if (text.isEmpty()) continue
            if (!header) {
                if (!text.equals("kanban", ignoreCase = true)) return fail("Expected kanban")
                header = true; continue
            }
            if (text.startsWith("::icon(") && text.endsWith(')')) {
                if (!decorate { it.copy(icon = text.substring(7, text.length - 1)) }) return fail("Kanban decoration requires a node")
                continue
            }
            if (text.startsWith(":::")) {
                if (!decorate { it.copy(cssClasses = text.substring(3).trim()) }) return fail("Kanban decoration requires a node")
                continue
            }
            val indent = raw.takeWhile { it == ' ' || it == '\t' }.length
            var metadata = KanbanMetadata()
            var labelOverride: String? = null
            val dataAt = outsideIndex(text, "@{")
            if (dataAt >= 0) {
                var data = text.substring(dataAt + 2)
                text = text.substring(0, dataAt).trimEnd()
                while (outsideIndex(data, "}") < 0 && index < lines.size) data += "\n" + lines[index++]
                val end = outsideIndex(data, "}")
                if (end < 0 || data.substring(end + 1).trim().isNotEmpty()) return fail("Unclosed or trailing kanban metadata")
                val fields = splitFields(data.substring(0, end)) ?: return fail("Invalid kanban metadata")
                labelOverride = fields["label"]?.takeIf { it.isNotEmpty() }
                val shape = fields["shape"]
                if (shape != null && (shape != shape.lowercase() || '_' in shape)) return fail("No such shape: $shape. Shape names should be lowercase.")
                metadata = KanbanMetadata(fields["icon"], assigned = fields["assigned"], ticket = fields["ticket"], priority = fields["priority"])
            }
            val shapeAt = text.indexOfFirst { it in "[(){" }
            val id: String
            val label: String
            if (shapeAt >= 0) {
                val opening = listOf("((", "{{", "(", "[").firstOrNull { text.startsWith(it, shapeAt) }
                    ?: return fail("Unsupported kanban node shape")
                val closing = when (opening) { "((" -> "))"; "{{" -> "}}"; "(" -> ")"; else -> "]" }
                if (!text.endsWith(closing)) return fail("Unclosed kanban node label")
                label = unquote(text.substring(shapeAt + opening.length, text.length - closing.length))
                id = text.substring(0, shapeAt).trim().ifEmpty { label }
            } else { id = text; label = text }
            if (id.isEmpty() || label.isEmpty()) return fail("Kanban node requires an ID and label")
            val displayed = labelOverride ?: label
            if (sectionIndent == null) sectionIndent = indent
            if (indent < sectionIndent) return fail("Items without section detected, found section (\"$displayed\")")
            lastWasCard = indent > sectionIndent
            if (!lastWasCard) columns += KanbanColumn(id, displayed, emptyList(), metadata)
            else {
                val column = columns.last()
                columns[columns.lastIndex] = column.copy(cards = column.cards + KanbanCard(id, displayed, metadata))
            }
        }
        if (!header || columns.isEmpty()) return fail("Kanban requires at least one section")
        return MermaidParseResult.Success(KanbanDiagram(columns))
    }

    private fun unquote(value: String): String {
        if (value.length >= 4 && value.startsWith("\"`") && value.endsWith("`\"")) return value.substring(2, value.length - 2)
        if (value.length >= 2 && value.first() in "\"'" && value.last() == value.first()) return value.substring(1, value.length - 1)
        return value
    }
    private fun outsideIndex(value: String, needle: String): Int {
        var quote: Char? = null
        var escaped = false
        for (i in value.indices) {
            val c = value[i]
            if (escaped) { escaped = false; continue }
            if (c == '\\' && quote != null) { escaped = true; continue }
            if (quote != null) { if (c == quote) quote = null; continue }
            if (c in "\"'") { quote = c; continue }
            if (value.startsWith(needle, i)) return i
        }
        return -1
    }
    private fun stripComment(value: String): String = outsideIndex(value, "%%").let { if (it < 0) value else value.substring(0, it) }
    private fun splitFields(value: String): Map<String, String>? {
        val result = linkedMapOf<String, String>()
        var remaining = value.trim()
        while (remaining.isNotEmpty()) {
            val comma = outsideIndex(remaining, ",")
            val newline = outsideIndex(remaining, "\n")
            val end = listOf(comma, newline).filter { it >= 0 }.minOrNull() ?: remaining.length
            val part = remaining.substring(0, end).trim()
            remaining = if (end == remaining.length) "" else remaining.substring(end + 1).trim()
            if (part.isEmpty()) continue
            val colon = outsideIndex(part, ":")
            if (colon < 1) return null
            val raw = part.substring(colon + 1).trim()
            if (raw.isEmpty()) return null
            result[part.substring(0, colon).trim()] = unquote(raw)
        }
        return result
    }
}
