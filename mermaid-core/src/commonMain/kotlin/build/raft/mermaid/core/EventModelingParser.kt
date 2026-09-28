package build.raft.mermaid.core

/** Retains data as inert source text; it is never evaluated or inserted as HTML. */
internal class EventModelingParser(private val source: String) {
    private val lines = source.split(Regex("\r\n|\r|\n"))
    private var line = 0
    private fun fail(message: String): Nothing = throw IllegalArgumentException(message)
    private val framePattern = Regex("^(tf|timeframe|rf|resetframe)\\s+(\\d{1,3})\\s+(ui|cmd|command|evt|event|pcr|processor|rmo|readmodel)\\s+([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*)(.*)$")
    private val dataPattern = Regex("^data\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*(.*)$")
    private fun withoutComment(input: String): String {
        var quote: Char? = null
        var depth = 0
        input.forEachIndexed { index, c ->
            if (quote != null) { if (c == quote && (index == 0 || input[index - 1] != '\\')) quote = null }
            else when {
                c == '\'' || c == '"' -> quote = c
                c == '{' -> depth++
                c == '}' -> depth--
                c == '%' && input.getOrNull(index + 1) == '%' && depth == 0 -> return input.substring(0, index).trim()
            }
        }
        return input.trim()
    }
    private fun payload(raw: String, block: Boolean): EventModelingData {
        var text = raw.trim()
        while (text.isEmpty() && block && line + 1 < lines.size) text = lines[++line].trim()
        var type = "text"
        if (text.startsWith('`')) {
            val end = text.indexOf('`', 1); if (end < 0) fail("Unclosed data type")
            type = text.substring(1, end)
            if (type !in listOf("json", "jsobj", "figma", "salt", "uri", "md", "html", "text")) fail("Unknown data type")
            text = text.substring(end + 1).trim()
            while (text.isEmpty() && block && line + 1 < lines.size) text = lines[++line].trim()
        }
        if (block) {
            if (text != "{") fail("Data block requires an opening brace and newline")
            val content = mutableListOf<String>()
            while (++line < lines.size) {
                if (lines[line] == "}") return EventModelingData(type, content.joinToString("\n"))
                content += lines[line]
            }
            fail("Unclosed data block")
        }
        val first = text.firstOrNull()
        if (first !in listOf('{', '\'', '"') || text.length < 2 || text.last() != if (first == '{') '}' else first) fail("Invalid inline data")
        return EventModelingData(type, text.substring(1, text.length - 1))
    }
    fun parse(): MermaidParseResult = try {
        if (lines.firstOrNull()?.trim() != "eventmodeling") fail("Expected exact eventmodeling header")
        val frames = linkedMapOf<String, EventModelingFrame>()
        val data = linkedMapOf<String, EventModelingData>()
        val relations = mutableListOf<EventModelingRelation>()
        var previous: String? = null
        var title: String? = null
        var accTitle: String? = null
        var accDescription: String? = null
        line = 1
        while (line < lines.size) {
            val text = withoutComment(lines[line])
            when {
                text.isEmpty() || text.startsWith("%%") -> Unit
                text.startsWith("title ") -> title = text.substring(6).substringBefore("%%").trim()
                text.startsWith("accTitle:") -> accTitle = text.substringAfter(':').substringBefore("%%").trim()
                text.startsWith("accDescr:") -> accDescription = text.substringAfter(':').substringBefore("%%").trim()
                dataPattern.matches(text) -> {
                    val match = dataPattern.matchEntire(text)!!
                    if (match.groupValues[1] in data) fail("Duplicate data identifier")
                    data[match.groupValues[1]] = payload(match.groupValues[2], true)
                }
                else -> {
                    val match = framePattern.matchEntire(text) ?: fail("Unsupported Event Modeling syntax")
                    val id = match.groupValues[2]
                    if (id in frames) fail("Duplicate Event Modeling frame identifier")
                    val reset = match.groupValues[1] in listOf("rf", "resetframe")
                    val kind = when (match.groupValues[3]) { "ui" -> EventModelingEntityKind.UI; "cmd", "command" -> EventModelingEntityKind.COMMAND; "evt", "event" -> EventModelingEntityKind.EVENT; "pcr", "processor" -> EventModelingEntityKind.PROCESSOR; else -> EventModelingEntityKind.READ_MODEL }
                    var rest = match.groupValues[5].trim()
                    val sources = mutableListOf<String>()
                    while (rest.startsWith("->>")) {
                        val ref = Regex("^->>\\s+(\\d{1,3})(?=\\s|$)").find(rest) ?: fail("Invalid source frame")
                        sources += ref.groupValues[1]; rest = rest.substring(ref.value.length).trim()
                    }
                    if (sources.any { it !in frames }) fail("Event Modeling relation source must be declared first")
                    var dataRef: String? = null
                    if (rest.startsWith("[[")) {
                        val ref = Regex("^\\[\\[([A-Za-z_][A-Za-z0-9_]*)]]").find(rest) ?: fail("Invalid data reference")
                        dataRef = ref.groupValues[1]; rest = rest.substring(ref.value.length).trim()
                    }
                    val inline = rest.takeIf { it.isNotEmpty() }?.let { payload(it, false) }
                    frames[id] = EventModelingFrame(id, match.groupValues[4], kind, reset, inline, dataRef)
                    (if (sources.isNotEmpty()) sources else if (reset) emptyList() else previous?.let { listOf(it) }.orEmpty()).forEach { relations += EventModelingRelation(it, id) }
                    previous = id
                }
            }
            line++
        }
        frames.values.forEach { if (it.dataReference != null && it.dataReference !in data) fail("Unknown data reference: ${it.dataReference}") }
        MermaidParseResult.Success(EventModelingDiagram(title, frames.values.toList(), relations, data, accTitle, accDescription))
    } catch (e: IllegalArgumentException) {
        MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, e.message ?: "Invalid Event Modeling syntax", SourceLocation((line + 1).coerceAtMost(lines.size), 1))))
    }
}
