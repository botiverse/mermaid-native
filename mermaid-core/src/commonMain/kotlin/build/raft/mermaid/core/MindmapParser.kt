package build.raft.mermaid.core

/** Preserve source node identifiers separately from unique rendering identifiers. */
internal class MindmapParser(private val source: String) {
    fun parse(): MermaidParseResult {
        val nodes = mutableListOf<MindmapNode>()
        val indents = mutableListOf<Int>()
        val ids = mutableSetOf<String>()
        var header = false
        fun fail(message: String, line: Int) = MermaidParseResult.Failure(listOf(
            MermaidDiagnostic(MermaidDiagnosticCode.INVALID_VALUE, message, SourceLocation(line, 1)),
        ))
        source.lines().forEachIndexed { index, raw ->
            val line = index + 1
            val text = stripComment(raw).trim()
            if (text.isEmpty()) return@forEachIndexed
            if (!header) {
                if (!text.equals("mindmap", ignoreCase = true)) return fail("Expected mindmap", line)
                header = true; return@forEachIndexed
            }
            if (text.startsWith("::icon(") && text.endsWith(')')) {
                if (nodes.isEmpty()) return fail("Mindmap decoration requires a node", line)
                nodes[nodes.lastIndex] = nodes.last().copy(icon = text.substring(7, text.length - 1))
                return@forEachIndexed
            }
            if (text.startsWith(":::")) {
                if (nodes.isEmpty()) return fail("Mindmap decoration requires a node", line)
                nodes[nodes.lastIndex] = nodes.last().copy(cssClasses = text.substring(3).trim())
                return@forEachIndexed
            }
            val indent = raw.takeWhile { it == ' ' || it == '\t' }.length
            val at = text.indexOfFirst { it in "[(){" }
            var explicit: String? = null
            val label: String
            val shape: MindmapNodeShape
            if (at >= 0) {
                val open = listOf("))", "((", "{{", "(", ")", "[").firstOrNull { text.startsWith(it, at) }
                    ?: return fail("Unsupported mindmap node shape", line)
                val close = when (open) { "))" -> "(("; "((" -> "))"; "{{" -> "}}"; "(" -> ")"; ")" -> "("; else -> "]" }
                if (!text.endsWith(close)) return fail("Unclosed mindmap node label", line)
                label = unquote(text.substring(at + open.length, text.length - close.length))
                explicit = text.substring(0, at).trim().takeIf { it.isNotEmpty() }
                shape = when (open) {
                    "))" -> MindmapNodeShape.BANG
                    "((" -> MindmapNodeShape.DOUBLE_CIRCLE
                    "{{" -> MindmapNodeShape.HEXAGON
                    "(" -> MindmapNodeShape.ROUNDED_RECTANGLE
                    ")" -> MindmapNodeShape.CLOUD
                    else -> MindmapNodeShape.RECTANGLE
                }
            } else { label = text; shape = MindmapNodeShape.DEFAULT }
            if (label.isEmpty()) return fail("Mindmap node requires a label", line)
            if (explicit?.startsWith("__mindmap_") == true) return fail("Mindmap explicit ids cannot use the reserved generated prefix", line)
            val parentIndex = indents.indexOfLast { it < indent }
            if (nodes.isNotEmpty() && parentIndex < 0) return fail("There can be only one root. No parent could be found for (\"$label\")", line)
            val parent = nodes.getOrNull(parentIndex)
            val preferred = explicit ?: "__mindmap_${nodes.size}"
            val id = if (ids.add(preferred)) preferred else "__mindmap_${nodes.size}".also { ids.add(it) }
            nodes += MindmapNode(id, label, parent?.id, (parent?.depth ?: -1) + 1, shape, explicit ?: label)
            indents += indent
        }
        if (nodes.isEmpty()) return fail("Mindmap requires one root node", 1)
        return MermaidParseResult.Success(MindmapDiagram(nodes))
    }
    private fun unquote(value: String): String = when {
        value.startsWith("\"`") && value.endsWith("`\"") && value.length >= 4 -> value.substring(2, value.length - 2)
        value.startsWith('"') && value.endsWith('"') && value.length >= 2 -> value.substring(1, value.length - 1)
        else -> value
    }
    private fun stripComment(value: String): String {
        var quoted = false
        for (i in value.indices) {
            if (value[i] == '"') quoted = !quoted
            if (!quoted && value.startsWith("%%", i)) return value.substring(0, i)
        }
        return value
    }
}
