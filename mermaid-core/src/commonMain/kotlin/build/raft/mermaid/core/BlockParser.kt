package build.raft.mermaid.core

/** Block grammar consumed by the shared model and renderer; no browser database dependency. */
internal class BlockParser(private val source: String) {
    private data class Group(val id: String, val label: String, val span: Int = 1, var columns: Int = -1, var columnsDeclared: Boolean = false, var warningColumns: Int = -1, val children: MutableList<Any> = mutableListOf())
    private val root = Group("root", "")
    private val stack = mutableListOf(root)
    private val nodes = linkedMapOf<String, BlockNode>()
    private val edges = mutableListOf<BlockEdge>()
    private val classes = linkedMapOf<String, List<String>>()
    private val assigned = linkedMapOf<String, MutableList<String>>()
    private val styles = linkedMapOf<String, MutableList<String>>()
    private var generated = 0
    private var line = 1
    private fun reject(message: String): Nothing = throw IllegalArgumentException(message)
    private fun nextId(prefix: String): String { var id: String; do { id = "__${prefix}_${generated++}" } while (id in nodes); return id }
    private fun entries(): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>(); val text = StringBuilder(); var quote: Char? = null; var escaped = false; var physical = 1; var start = 1; var i = 0
        fun flush() { if (text.isNotBlank()) result += start to text.toString().trim(); text.clear(); start = physical }
        while (i < source.length) {
            val c = source[i]
            if (quote == null && source.startsWith("%%", i)) { while (i < source.length && source[i] != '\n') i++; continue }
            if (quote != null) { text.append(c); if (!escaped && c == quote) quote = null; escaped = !escaped && c == '\\' }
            else if (c == '"' || c == '\'') { quote = c; text.append(c) }
            else if (c == ';' || c == '\n' || c == '\r') { flush() }
            else { if (text.isBlank() && !c.isWhitespace()) start = physical; text.append(c) }
            if (c == '\n') { physical++; if (text.isEmpty()) start = physical }
            i++
        }
        if (quote != null) { line = physical; reject("Unclosed block label") }; flush(); return result
    }
    fun parse(): MermaidParseResult = try {
        val statements = entries()
        if (statements.firstOrNull()?.second !in setOf("block", "block-beta")) reject("Expected block or block-beta")
        for ((number, text) in statements.drop(1)) {
            line = number
            val command = text.split(Regex("\\s+"), limit = 2)
            val keyword = command.first()
            val body = command.getOrNull(1).orEmpty().trim()
            fun targetsAndValue(): Pair<List<String>, String> {
                val match = Regex("^([A-Za-z0-9_]+(?:\\s*,\\s*[A-Za-z0-9_]+)*)\\s+(.+)$").matchEntire(body)
                    ?: reject("Block $keyword requires identifiers and a value")
                return match.groupValues[1].split(',').map { it.trim() } to match.groupValues[2].trim()
            }
            when {
                text == "end" -> { if (stack.size == 1) reject("Unexpected block end"); stack.removeAt(stack.lastIndex) }
                text == "block" || text.startsWith("block:") -> {
                    val node = if (text == "block") BlockNode(nextId("block"), "") else Cursor(text.substring(6)).let { c -> val n = c.node(false); if (!c.done()) reject("Unexpected composite block suffix"); n }
                    if (node.id in nodes) reject("Duplicate composite block identifier")
                    nodes[node.id] = node
                    val group = Group(node.id, node.label, node.columnSpan); stack.last().children += group; stack += group
                }
                keyword == "columns" -> {
                    val count = if (body == "auto") -1 else body.toIntOrNull()?.takeIf { it in 1..512 } ?: reject("Block columns must be auto or 1..512")
                    if (!stack.last().columnsDeclared) { stack.last().warningColumns = count; stack.last().columnsDeclared = true }
                    stack.last().columns = count
                }
                keyword == "classDef" -> { val (ids, value) = targetsAndValue(); for (id in ids) classes[id] = value.split(',').map { it.trim() } }
                keyword == "class" -> { val (ids, value) = targetsAndValue(); for (id in ids) assigned.getOrPut(id) { mutableListOf() }.addAll(value.split(',').map { it.trim() }) }
                keyword == "style" -> { val (ids, value) = targetsAndValue(); for (id in ids) styles.getOrPut(id) { mutableListOf() }.addAll(value.split(',').map { it.trim() }) }
                else -> {
                    val cursor = Cursor(text); var previous: BlockNode? = null
                    while (!cursor.done()) {
                        if (cursor.hasArrow()) {
                            val from = previous ?: reject("Block edge needs a source")
                            val label = cursor.arrow(); val to = cursor.node(); edges += BlockEdge(from.id, to.id, label); previous = to
                        } else previous = cursor.node()
                    }
                }
            }
        }
        if (stack.size != 1) reject("Unclosed composite block")
        fun decorate(n: BlockNode) = n.copy(classes = assigned[n.id].orEmpty().toList(), styles = styles[n.id].orEmpty().toList())
        val warnings = mutableListOf<String>()
        fun materialize(group: Group): List<BlockNode> = group.children.map { child ->
            val node = if (child is Group) decorate(BlockNode(child.id, child.label, child.span, "composite", materialize(child), child.columns)) else decorate(nodes.getValue(child as String))
            if (group.warningColumns > 0 && node.columnSpan > group.warningColumns) warnings += "Block ${node.id} width ${node.columnSpan} exceeds configured column width ${group.warningColumns}"
            node
        }
        val result = materialize(root)
        MermaidParseResult.Success(BlockDiagram(root.columns, result, edges, classes, warnings))
    } catch (error: IllegalArgumentException) {
        MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, error.message ?: "Invalid block syntax", SourceLocation(line, 1))))
    }
    private inner class Cursor(private val text: String) {
        private var index = 0
        private fun space() { while (text.getOrNull(index)?.isWhitespace() == true) index++ }
        fun done(): Boolean { space(); return index == text.length }
        fun hasArrow(): Boolean { space(); return text.startsWith("--", index) }
        fun arrow(): String? {
            space()
            if (text.startsWith("-->", index)) { index += 3; return null }
            if (!text.startsWith("--", index)) reject("Expected block arrow")
            index += 2; space(); val label = quoted(); space()
            if (!text.startsWith("-->", index)) reject("Expected --> after block edge label")
            index += 3; return label
        }
        private fun quoted(): String {
            val quote = text.getOrNull(index)
            if (quote != '"') reject("Block labels require double quotes")
            index++; val result = StringBuilder(); var escaped = false
            while (index < text.length) { val c = text[index++]; if (!escaped && c == quote) return result.toString(); if (!escaped && c == '\\') escaped = true else { result.append(c); escaped = false } }
            reject("Unclosed block label")
        }
        fun node(register: Boolean = true): BlockNode {
            space(); val start = index
            while (text.getOrNull(index)?.let { it.isLetterOrDigit() || it == '_' || it == '-' } == true && !text.startsWith("--", index)) index++
            if (start == index) reject("Expected block identifier")
            val originalId = text.substring(start, index); var id = originalId; var label = id; var type = "na"; var directions = emptyList<String>(); var span = 1
            var close = "]"
            if (text.startsWith("((", index)) { index += 2; type = "circle"; close = "))" }
            else if (text.startsWith("[(", index)) { index += 2; type = "cylinder"; close = ")]" }
            else if (text.startsWith("<[", index)) { index += 2; type = "block_arrow" }
            else if (text.getOrNull(index) == '[') { index++; type = "square" }
            if (type != "na") {
                space()
                label = if (text.getOrNull(index) in listOf('"', '\'')) quoted() else { val end = text.indexOf(close, index); if (end < 0) reject("Unclosed block label"); text.substring(index, end).trim().also { if (it.any { c -> c.isWhitespace() } || it.contains('\'')) reject("Block labels containing whitespace require double quotes"); index = end } }
                space(); if (!text.startsWith(close, index)) reject("Expected $close after block label"); index += close.length
                if (type == "block_arrow") {
                    if (label.isEmpty()) reject("Block arrow requires a non-empty label")
                    if (text.getOrNull(index) != '>') reject("Block arrow requires directions"); index++; space()
                    if (text.getOrNull(index) != '(') reject("Block arrow requires directions"); index++
                    val end = text.indexOf(')', index); if (end < 0) reject("Unclosed block arrow directions")
                    directions = text.substring(index, end).split(',').map { it.trim() }
                    if (directions.any { it !in setOf("up", "down", "left", "right", "x", "y") }) reject("Unsupported block arrow direction")
                    index = end + 1
                }
            }
            if (text.getOrNull(index) == ':') { index++; val n = index; while (text.getOrNull(index)?.isDigit() == true) index++; span = text.substring(n, index).toIntOrNull()?.takeIf { it > 0 } ?: reject("Block span must be positive") }
            if (originalId == "space") { if (type != "na") reject("Block space cannot have a label or shape"); id = nextId("space"); type = "space"; label = "" }
            val node = BlockNode(id, label, span, type, directions = directions)
            if (register) {
                val previous = nodes[id]
                if (previous == null) { nodes[id] = node; stack.last().children += id }
                else if (type != "na") nodes[id] = node
                return nodes.getValue(id)
            }
            return node
        }
    }
}
