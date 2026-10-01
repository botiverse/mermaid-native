package build.raft.mermaid.core

/** Quoted rows, measured hierarchy and explicitly retained class declarations. */
internal class TreemapParser(private val source: String) {
    fun parse(): MermaidParseResult {
        val hierarchy = TreemapHierarchy(); val labels = mutableSetOf<String>()
        val classes = linkedMapOf<String, String>(); val assignments = linkedMapOf<String, String>()
        var title: String? = null; var accTitle: String? = null; var accDescription: String? = null
        var header = false; var description: StringBuilder? = null
        fun fail(message: String, line: Int) = MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.INVALID_VALUE, message, SourceLocation(line, 1))))
        source.lines().forEachIndexed { index, raw ->
            var text = raw.trim()
            if (description != null) {
                if ('}' !in text) { description!!.append('\n').append(text); return@forEachIndexed }
                description!!.append('\n').append(text.substringBefore('}')); accDescription = description.toString().trim(); description = null
                text = text.substringAfter('}').trim()
            }
            if (text.isEmpty() || text.startsWith("%%")) return@forEachIndexed
            if (!header) {
                if (text !in setOf("treemap", "treemap-beta")) return fail("Expected treemap or treemap-beta", index + 1)
                header = true; return@forEachIndexed
            }
            when {
                text.startsWith("title ") -> title = text.substring(6).substringBefore("%%").trim()
                Regex("^accTitle\\s*:").containsMatchIn(text) -> accTitle = text.substringAfter(':').substringBefore("%%").trim()
                Regex("^accDescr\\s*:").containsMatchIn(text) -> accDescription = text.substringAfter(':').substringBefore("%%").trim()
                Regex("^accDescr\\s*\\{").containsMatchIn(text) -> {
                    val body = text.substringAfter('{')
                    if ('}' in body) accDescription = body.substringBefore('}').trim() else description = StringBuilder(body)
                }
                text.startsWith("classDef ") -> {
                    val match = Regex("^classDef\\s+([A-Za-z_][A-Za-z0-9_]*)\\s+(.*?)\\s*;?$").matchEntire(text) ?: return fail("Invalid treemap class definition", index + 1)
                    classes[match.groupValues[1]] = match.groupValues[2].removeSuffix(";")
                }
                text.startsWith("class ") -> {
                    val match = Regex("^class\\s+(\\S+)\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*;?$").matchEntire(text) ?: return fail("Invalid treemap class assignment", index + 1)
                    assignments[match.groupValues[1]] = match.groupValues[2]
                }
                else -> {
                    val match = Regex("^([\"'])(.*?)\\1(?:\\s*[:,]\\s*([0-9.eE+_-]+))?(?:\\s*:::\\s*([A-Za-z_][A-Za-z0-9_]*))?\\s*(?:%%.*)?$").matchEntire(text) ?: return fail("Invalid treemap row", index + 1)
                    val label = match.groupValues[2]; val rawValue = match.groupValues[3]
                    val value = rawValue.takeIf { it.isNotEmpty() }?.replace("_", "")?.toDoubleOrNull()
                    if (label.isBlank() || !labels.add(label)) return fail("Treemap labels must be unique and non-empty", index + 1)
                    if (rawValue.isNotEmpty() && (value == null || !value.isFinite() || value <= 0.0)) return fail("Treemap leaf values must be finite and positive", index + 1)
                    val indent = raw.takeWhile { it == ' ' || it == '\t' }.fold(0) { total, ch -> total + if (ch == '\t') 4 else 1 }
                    try {
                        hierarchy.add(TreemapHierarchyItem(indent, label, value, match.groupValues[4].takeIf { it.isNotEmpty() }))
                    } catch (error: IllegalArgumentException) {
                        return fail(error.message ?: "Invalid treemap hierarchy", index + 1)
                    }
                }
            }
        }
        if (description != null) return fail("Unclosed treemap accessibility description", source.lines().size)
        val roots = hierarchy.build()
        fun TreemapNode.weight(): Double = value ?: children.sumOf { it.weight() }
        if (roots.any { !it.weight().isFinite() } || !roots.sumOf { it.weight() }.isFinite()) return fail("Treemap weights must have a finite sum", 1)
        return MermaidParseResult.Success(TreemapDiagram(roots, title, accTitle, accDescription, classes, assignments))
    }
}
