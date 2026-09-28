package build.raft.mermaid.core

/** Grammar projection is separate from product reference admission. */
internal class ArchitectureParser(private val source: String) {
    fun parse(): MermaidParseResult = parse(false)
    fun parseValidated(): MermaidParseResult = parse(true)
    private fun parse(validateReferences: Boolean): MermaidParseResult {
        val groups = linkedMapOf<String, ArchitectureGroup>()
        val services = linkedMapOf<String, ArchitectureService>()
        val edges = mutableListOf<ArchitectureEdge>()
        val locations = mutableMapOf<String, Int>()
        var title: String? = null; var accTitle: String? = null; var accDescription: String? = null
        var description: StringBuilder? = null; var header = false
        fun fail(message: String, line: Int) = MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, message, SourceLocation(line, 1))))
        source.lines().forEachIndexed { index, raw ->
            var text = raw.trim()
            if (description != null) {
                if ('}' !in text) { description!!.append('\n').append(text); return@forEachIndexed }
                description!!.append('\n').append(text.substringBefore('}')); accDescription = description.toString().trim(); description = null
                text = text.substringAfter('}').trim()
            }
            if (text.isEmpty() || text.startsWith("%%")) return@forEachIndexed
            if (!header) {
                val match = Regex("^architecture-beta(?:\\s+(.*))?$").matchEntire(text) ?: return fail("Expected architecture-beta", index + 1)
                header = true; text = match.groupValues[1].trim()
                if (text.isEmpty()) return@forEachIndexed
            }
            when {
                Regex("^title\\s+").containsMatchIn(text) -> { title = text.substringAfter(' ').trim(); return@forEachIndexed }
                Regex("^accTitle\\s*:").containsMatchIn(text) -> { accTitle = text.substringAfter(':').trim(); return@forEachIndexed }
                Regex("^accDescr\\s*:").containsMatchIn(text) -> { accDescription = text.substringAfter(':').trim(); return@forEachIndexed }
                Regex("^accDescr\\s*\\{").containsMatchIn(text) -> {
                    val body = text.substringAfter('{')
                    if ('}' in body) accDescription = body.substringBefore('}').trim() else description = StringBuilder(body)
                    return@forEachIndexed
                }
            }
            ENTITY.matchEntire(text)?.let { match ->
                val kind = match.groupValues[1]; val id = match.groupValues[2]
                val icon = match.groupValues[3]; val rawLabel = match.groupValues[4]
                val label = if (rawLabel.isEmpty()) id else decodeLabel(rawLabel)
                val parent = match.groupValues[5].takeIf { it.isNotEmpty() }
                if (id in groups || id in services) return fail("Duplicate architecture identifier", index + 1)
                if (kind == "group" && parent != null) return fail("Nested architecture groups are not supported", index + 1)
                if (kind == "group") groups[id] = ArchitectureGroup(id, icon, label)
                else services[id] = ArchitectureService(id, icon, label, parent)
                locations[id] = index + 1
                return@forEachIndexed
            }
            EDGE.matchEntire(text)?.let { match ->
                val edge = ArchitectureEdge(match.groupValues[1], port(match.groupValues[2]), match.groupValues[5], port(match.groupValues[4]), match.groupValues[3] == "-->")
                if (edge.sourceId == edge.targetId) return fail("Architecture self edges are not supported", index + 1)
                if (edge in edges) return fail("Duplicate architecture edge", index + 1)
                edges += edge; locations["edge${edges.lastIndex}"] = index + 1
                return@forEachIndexed
            }
            return fail("Unsupported architecture syntax", index + 1)
        }
        if (!header) return fail("Expected architecture-beta", 1)
        if (description != null) return fail("Unclosed architecture accessibility description", source.lines().size)
        if (validateReferences) {
            services.values.firstOrNull { it.groupId != null && it.groupId !in groups }?.let { return fail("Architecture service group does not exist", locations.getValue(it.id)) }
            edges.forEachIndexed { index, edge -> if (edge.sourceId !in services || edge.targetId !in services) return fail("Architecture edge service does not exist", locations.getValue("edge$index")) }
        }
        return MermaidParseResult.Success(ArchitectureDiagram(groups.values.toList(), services.values.toList(), edges, title, accTitle, accDescription))
    }
    private fun decodeLabel(text: String): String {
        if (text.length < 2 || text.first() !in "\"'" || text.last() != text.first()) return text
        val body = text.substring(1, text.lastIndex); val output = StringBuilder(); var i = 0
        while (i < body.length) { if (body[i] == '\\' && i + 1 < body.length) i++; output.append(body[i]); i++ }
        return output.toString()
    }
    private fun port(text: String): ArchitecturePort = when (text) { "T" -> ArchitecturePort.TOP; "B" -> ArchitecturePort.BOTTOM; "L" -> ArchitecturePort.LEFT; else -> ArchitecturePort.RIGHT }
    companion object {
        private val ENTITY = Regex("""^(group|service)\s+([A-Za-z0-9_]+)(?:\(([A-Za-z0-9_:-]+)\))?(?:\[((?:"(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*'|[^\[\]\r\n]+))])?(?:\s+in\s+([A-Za-z0-9_]+))?\s*(?:%%.*)?$""")
        private val EDGE = Regex("""^([A-Za-z0-9_]+):(T|B|L|R)\s*(-->|--)\s*(T|B|L|R):([A-Za-z0-9_]+)\s*(?:%%.*)?$""")
    }
}
