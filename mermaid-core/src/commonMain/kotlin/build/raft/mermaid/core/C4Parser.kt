package build.raft.mermaid.core

/** Typed C4 macros, named attributes and nested boundaries. */
internal class C4Parser(private val source: String) {
    private data class Argument(val value: String, val name: String? = null)
    fun parse(): MermaidParseResult {
        val elements = linkedMapOf<String, C4Element>()
        val boundaries = linkedMapOf<String, C4Boundary>()
        val relationships = mutableListOf<C4Relationship>()
        val stack = mutableListOf("global")
        var title: String? = null
        var type: String? = null
        var pending = ""
        fun fail(message: String, line: Int) = MermaidParseResult.Failure(listOf(
            MermaidDiagnostic(MermaidDiagnosticCode.INVALID_VALUE, message, SourceLocation(line, 1)),
        ))
        source.lines().forEachIndexed { index, raw ->
            val line = index + 1
            var text = stripComment(raw).trim()
            if (text.isEmpty()) return@forEachIndexed
            if (type == null) {
                if (text !in setOf("C4Context", "C4Container", "C4Component", "C4Dynamic", "C4Deployment")) return fail("Expected C4 diagram header", line)
                type = text; return@forEachIndexed
            }
            if (pending.isNotEmpty()) { text = pending + "\n" + text; pending = "" }
            if (text.startsWith("title ") || text.startsWith("title\t")) { title = text.substring(5).trim(); return@forEachIndexed }
            if (text == "}") {
                if (stack.size == 1) return fail("Unmatched C4 boundary close", line)
                stack.removeAt(stack.lastIndex); return@forEachIndexed
            }
            if (parenthesisBalance(text) > 0) { pending = text; return@forEachIndexed }
            val match = Regex("^([A-Za-z_]+)\\s*\\(([\\s\\S]*)\\)\\s*(\\{)?$").matchEntire(text)
                ?: return fail("Unsupported C4 macro syntax", line)
            val macro = match.groupValues[1]
            val args = arguments(match.groupValues[2]) ?: return fail("Invalid C4 arguments", line)
            val id = args.firstOrNull()?.value ?: return fail("C4 macro requires an identifier", line)
            fun at(n: Int): String? = args.getOrNull(n)?.takeIf { it.name == null }?.value
            val label = args.getOrNull(1) ?: Argument("")
            val attrs = linkedMapOf<String, String>()
            if (macro in setOf("Boundary", "System_Boundary", "Enterprise_Boundary", "Container_Boundary")) {
                if (match.groupValues[3].isEmpty()) return fail("C4 boundary requires an opening brace", line)
                if (id in stack || id in elements) return fail("C4 boundary identifier collides with an ancestor or element", line)
                val defaultType = if (macro == "Container_Boundary") "container" else "system"
                args.drop(2).forEachIndexed { i, arg -> attrs[arg.name ?: listOf("type", "tags", "link").getOrElse(i) { "extra$i" }] = arg.value }
                boundaries[id] = C4Boundary(id, label.value, at(2) ?: defaultType, stack.last(), label.name, attrs.filterKeys { it != "type" })
                stack += id; return@forEachIndexed
            }
            if (macro == "Rel" || macro == "BiRel" || macro in setOf("Rel_U", "Rel_D", "Rel_L", "Rel_R", "Rel_Back")) {
                if (args.size < 3) return fail("C4 relationship requires two endpoints and a label", line)
                val to = at(1) ?: return fail("C4 relationship endpoint must be positional", line)
                if (id !in elements && id !in boundaries || to !in elements && to !in boundaries) return fail("C4 relationship endpoints must be declared first", line)
                if (id == to) return fail("C4 self relationships are not supported", line)
                relationships += if (macro == "Rel_Back") C4Relationship(to, id, args[2].value, at(3), false)
                    else C4Relationship(id, to, args[2].value, at(3), macro == "BiRel")
                return@forEachIndexed
            }
            val base = macro.removeSuffix("_Ext")
            val kind = when {
                base == "Person" -> C4ElementKind.PERSON
                base in setOf("System", "SystemDb", "SystemQueue") -> C4ElementKind.SYSTEM
                base in setOf("Container", "ContainerDb", "ContainerQueue") -> C4ElementKind.CONTAINER
                base in setOf("Component", "ComponentDb", "ComponentQueue") -> C4ElementKind.COMPONENT
                else -> return fail("Unsupported C4 macro: $macro", line)
            }
            if (match.groupValues[3].isNotEmpty() || id in boundaries || id == "global") return fail("Invalid C4 element boundary or identifier", line)
            val hasTechnology = kind == C4ElementKind.CONTAINER || kind == C4ElementKind.COMPONENT
            val fields = if (hasTechnology) listOf("technology", "description", "sprite", "tags", "link") else listOf("description", "sprite", "tags", "link")
            args.drop(2).forEachIndexed { i, arg -> attrs[arg.name ?: fields.getOrElse(i) { "extra$i" }] = arg.value }
            elements[id] = C4Element(id, label.value, attrs["description"], kind, macro.endsWith("_Ext"), attrs["technology"],
                when { base.endsWith("Db") -> "database"; base.endsWith("Queue") -> "queue"; else -> "" }, stack.last(), label.name, attrs.filterKeys { it != "description" && it != "technology" })
        }
        if (pending.isNotEmpty()) return fail("Unclosed C4 macro", source.lines().size)
        if (stack.size != 1) return fail("Unclosed C4 boundary", source.lines().size)
        if (elements.isEmpty() && boundaries.isEmpty()) return fail("C4 requires at least one element or boundary", 1)
        return MermaidParseResult.Success(C4Diagram(title, elements.values.toList(), relationships, boundaries.values.toList(), type ?: "C4Context"))
    }
    private fun parenthesisBalance(value: String): Int {
        var quote = false; var escape = false; var count = 0
        for (c in value) {
            if (escape) { escape = false; continue }
            if (quote && c == '\\') { escape = true; continue }
            if (c == '"') quote = !quote
            if (!quote) { if (c == '(') count++; if (c == ')') count-- }
        }
        return count
    }
    private fun arguments(value: String): List<Argument>? {
        val parts = mutableListOf<String>(); var start = 0; var quoted = false; var escaped = false
        for (i in value.indices) {
            val c = value[i]
            if (escaped) { escaped = false; continue }
            if (quoted && c == '\\') { escaped = true; continue }
            if (c == '"') quoted = !quoted
            if (!quoted && c == ',') { parts += value.substring(start, i).trim(); start = i + 1 }
        }
        if (quoted) return null
        parts += value.substring(start).trim()
        return parts.map { token ->
            var text = token; var name: String? = null
            if (text.startsWith('$')) {
                val equal = text.indexOf('='); if (equal < 2) return null
                name = text.substring(1, equal).trim(); text = text.substring(equal + 1).trim()
            }
            val result = if (text.startsWith('"') && text.endsWith('"') && text.length >= 2) text.substring(1, text.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
                else if (Regex("[A-Za-z0-9_.-]+").matches(text)) text else return null
            Argument(result, name)
        }
    }
    private fun stripComment(value: String): String {
        var quoted = false; var escaped = false
        for (i in value.indices) {
            val c = value[i]
            if (escaped) { escaped = false; continue }
            if (quoted && c == '\\') { escaped = true; continue }
            if (c == '"') quoted = !quoted
            if (!quoted && value.startsWith("%%", i)) return value.substring(0, i)
        }
        return value
    }
}
