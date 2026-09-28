package build.raft.mermaid.core

internal class VennParser(private val source: String) {
    private val id = "(?:[A-Za-z_][A-Za-z0-9_-]*|\"[^\"\\r\\n]*\")"
    private val list = "$id(?:\\s*,\\s*$id)*"
    private fun unquote(value: String) = value.trim().removeSurrounding("\"")
    private fun ids(value: String) = Regex(id).findAll(value).map { unquote(it.value) }.toList()
    fun parse(): MermaidParseResult {
        val sets = linkedMapOf<String, VennSet>(); val unions = mutableListOf<VennUnion>()
        val texts = mutableListOf<VennText>(); val styles = mutableListOf<VennStyle>()
        var title: String? = null; var header = false; var current: List<String>? = null
        fun fail(message: String, line: Int) = MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, message, SourceLocation(line, 1))))
        source.lines().forEachIndexed { index, raw ->
            val line = index + 1; val text = raw.trim()
            if (text.isEmpty() || text.startsWith("%%")) return@forEachIndexed
            if (!header) { if (text != "venn-beta") return fail("Expected venn-beta", line); header = true; return@forEachIndexed }
            if (text.startsWith("title ")) { title = unquote(text.removePrefix("title ")); return@forEachIndexed }
            val subset = Regex("^(set|union)\\s+($list)(?:\\s*\\[([^]]*)])?(?:\\s*:\\s*([+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)))?$").matchEntire(text)
            if (subset != null) {
                val members = ids(subset.groupValues[2]); val isSet = subset.groupValues[1] == "set"
                if (isSet && members.size != 1) return fail("set requires single identifier", line)
                if (!isSet && members.size < 2) return fail("union requires multiple identifiers", line)
                if (!isSet && members.any { it !in sets }) return fail("unknown set identifier: ${members.filter { it !in sets }.joinToString()}", line)
                if (members.distinct().size != members.size) return fail("Venn union members must be unique", line)
                val size = subset.groupValues[4].takeIf { it.isNotEmpty() }?.toDoubleOrNull()
                if (size != null && (!size.isFinite() || size <= 0.0)) return fail("Venn size must be positive and finite", line)
                val label = subset.groups[3]?.value?.let(::unquote)
                if (isSet) {
                    val name = members.single()
                    if (name in sets) return fail("Duplicate venn set", line)
                    sets[name] = VennSet(name, label ?: name, size)
                } else {
                    if (unions.any { it.setIds.sorted() == members.sorted() }) return fail("Duplicate venn union", line)
                    unions += VennUnion(members, label, size)
                }
                current = members; return@forEachIndexed
            }
            val style = Regex("^style\\s+($list)\\s+(.+)$").matchEntire(text)
            if (style != null) {
                val props = linkedMapOf<String, String>(); val body = style.groupValues[2]
                val fields = Regex("(?:^|,\\s*)([A-Za-z_][A-Za-z0-9_-]*)\\s*:\\s*").findAll(body).toList()
                if (fields.isEmpty() || fields.first().range.first != 0) return fail("Invalid Venn style", line)
                fields.forEachIndexed { i, f ->
                    val end = fields.getOrNull(i + 1)?.range?.first ?: body.length
                    val value = unquote(body.substring(f.range.last + 1, end))
                    if (value.isEmpty()) return fail("Empty Venn style value", line)
                    props[f.groupValues[1]] = value
                }
                styles += VennStyle(ids(style.groupValues[1]), props); return@forEachIndexed
            }
            if (text.startsWith("text ")) {
                val content = text.removePrefix("text ")
                val explicit = Regex("^($list)\\s+($id|[0-9]+)(?:\\s*\\[([^]]*)])?$").matchEntire(content)
                val implicit = Regex("^($id|[0-9]+)(?:\\s*\\[([^]]*)])?$").matchEntire(content)
                when {
                    explicit != null -> texts += VennText(ids(explicit.groupValues[1]), unquote(explicit.groupValues[2]), explicit.groups[3]?.value?.let(::unquote))
                    implicit != null && raw.firstOrNull()?.isWhitespace() == true && current != null -> texts += VennText(current!!, unquote(implicit.groupValues[1]), implicit.groups[2]?.value?.let(::unquote))
                    else -> return fail("Venn text requires a set and identifier", line)
                }
                return@forEachIndexed
            }
            return fail("Unsupported venn syntax", line)
        }
        if (!header) return fail("Expected venn-beta", 1)
        if (sets.size > 3) return fail("Venn renderer supports at most three sets", 1)
        return MermaidParseResult.Success(VennDiagram(title, sets.values.toList(), unions, texts, styles))
    }
}
