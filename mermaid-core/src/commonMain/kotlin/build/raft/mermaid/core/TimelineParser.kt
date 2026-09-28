package build.raft.mermaid.core

/** Raw timeline lines retain semicolons, URL colons and original event whitespace. */
internal class TimelineParser(private val source: String) {
    fun parse(): MermaidParseResult {
        var lineNumber = 1
        return try {
            val lines = source.replace("\r\n", "\n").lines()
            val headerIndex = lines.indexOfFirst { it.isNotBlank() && !it.trimStart().startsWith("%%") }
            require(headerIndex >= 0)
            val header = Regex("timeline(?:\\s+(LR|TD))?", RegexOption.IGNORE_CASE).matchEntire(lines[headerIndex].trim()) ?: error("Invalid timeline header")
            val explicit = header.groupValues[1].isNotEmpty()
            val direction = if(header.groupValues[1].equals("TD", true)) FlowDirection.TB else FlowDirection.LR
            var title: String? = null; var section: String? = null; var accTitle: String? = null; var accDescription: String? = null
            val events = mutableListOf<TimelineEvent>(); val sections = mutableListOf<String>()
            var i = headerIndex + 1
            while(i < lines.size) {
                lineNumber = i + 1
                val raw = lines[i++].trimStart()
                if(raw.isBlank() || raw.startsWith("%%") || raw.startsWith('#')) continue
                val line = raw.substringBefore("%%")
                when {
                    line.startsWith("title ", true) -> title = line.substring(6)
                    line.startsWith("section ", true) -> { section = line.substring(8); require(section.isNotBlank()); sections += section }
                    Regex("^accTitle\\s*:", RegexOption.IGNORE_CASE).containsMatchIn(line) -> accTitle = line.substringAfter(':').trim()
                    Regex("^accDescr\\s*:", RegexOption.IGNORE_CASE).containsMatchIn(line) -> accDescription = line.substringAfter(':').trim()
                    Regex("^accDescr\\s*\\{", RegexOption.IGNORE_CASE).containsMatchIn(line) -> {
                        val value = StringBuilder(line.substringAfter('{'))
                        while('}' !in value && i < lines.size) value.append('\n').append(lines[i++])
                        require('}' in value) { "Unclosed accessibility description" }
                        accDescription = value.toString().substringBefore('}').trim()
                    }
                    else -> {
                        val separators = Regex(":\\s").findAll(line).map { it.range.first }.toList()
                        val period = line.substring(0, separators.firstOrNull() ?: line.length)
                        require(':' !in period && '#' !in period) { "Invalid timeline period" }
                        if(period.isNotBlank()) events += TimelineEvent(period, emptyList(), section, sections.lastIndex.takeIf { it >= 0 })
                        if(separators.isNotEmpty()) {
                            require(events.isNotEmpty()) { "An event requires a preceding period" }
                            val labels = separators.mapIndexed { j, start -> line.substring(start + 2, separators.getOrNull(j + 1) ?: line.length).also { require(it.isNotEmpty()) } }
                            events[events.lastIndex] = events.last().copy(labels = events.last().labels + labels)
                        }
                    }
                }
            }
            MermaidParseResult.Success(TimelineDiagram(title, events, sections, direction, explicit, accTitle, accDescription))
        } catch(error: IllegalArgumentException) {
            MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.INVALID_VALUE, error.message ?: "Invalid timeline", SourceLocation(lineNumber, 1))))
        } catch(error: IllegalStateException) {
            MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, error.message ?: "Invalid timeline", SourceLocation(lineNumber, 1))))
        }
    }
}
