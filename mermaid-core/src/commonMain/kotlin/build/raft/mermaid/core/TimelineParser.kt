package build.raft.mermaid.core

/** Token boundaries follow upstream timeline.jison; text whitespace remains in the model. */
internal class TimelineParser(private val source: String) {
    fun parse(): MermaidParseResult {
        var lineNumber = 1
        return try {
            val lines = source.replace("\r\n", "\n").lines()
            val headerIndex = lines.indexOfFirst { it.isNotBlank() && !it.trimStart().startsWith("%%") && !it.trimStart().startsWith('#') }
            require(headerIndex >= 0)
            val header = Regex("timeline(?:\\s+(LR|TD))?", RegexOption.IGNORE_CASE).matchEntire(lines[headerIndex].trim()) ?: error("Invalid timeline header")
            val explicit = header.groupValues[1].isNotEmpty()
            val direction = if (header.groupValues[1].equals("TD", true)) FlowDirection.TB else FlowDirection.LR
            var title: String? = null; var section: String? = null; var accTitle: String? = null; var accDescription: String? = null
            val events = mutableListOf<TimelineEvent>(); val sections = mutableListOf<String>()
            val document = lines.drop(headerIndex + 1).joinToString("\n")
            var offset = 0
            fun token(pattern: String): String? = Regex("^(?:$pattern)", RegexOption.IGNORE_CASE).find(document.substring(offset))?.value
            fun consume(value: String) { offset += value.length; lineNumber += value.count { it == '\n' } }
            lineNumber = headerIndex + 2
            while (offset < document.length) {
                val skip = token("%%(?!\\{)[^\n]*|[^}]%%[^\n]*|\\s+|#[^\n]*")
                if (skip != null) { consume(skip); continue }
                val titleToken = token("title\\s[^\n]+")
                if (titleToken != null) { title = titleToken.substring(6); consume(titleToken); continue }
                val acc = token("acc(?:Title|Descr)\\s*:\\s*")
                if (acc != null) {
                    consume(acc)
                    val value = document.substring(offset).substringBefore('\n')
                    if (acc.startsWith("accTitle", true)) accTitle = value.trim() else accDescription = value.trim()
                    consume(value); continue
                }
                val block = token("accDescr\\s*\\{\\s*")
                if (block != null) {
                    consume(block)
                    val end = document.indexOf('}', offset)
                    require(end >= 0) { "Unclosed accessibility description" }
                    val value = document.substring(offset, end)
                    accDescription = value.trim(); consume(value + "}"); continue
                }
                val sectionToken = token("section\\s[^:\n]+")
                if (sectionToken != null) {
                    section = sectionToken.substring(8); sections += section; consume(sectionToken); continue
                }
                val event = token(":\\s(?:[^:\n]|:(?!\\s))+")
                if (event != null) {
                    require(events.isNotEmpty()) { "An event requires a preceding period" }
                    events[events.lastIndex] = events.last().copy(labels = events.last().labels + event.substring(2))
                    consume(event); continue
                }
                val period = token("[^#:\n]+") ?: error("Invalid timeline statement")
                events += TimelineEvent(period, emptyList(), section, sections.lastIndex.takeIf { it >= 0 })
                consume(period)
            }
            MermaidParseResult.Success(TimelineDiagram(title, events, sections, direction, explicit, accTitle, accDescription))
        } catch (error: IllegalArgumentException) {
            MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.INVALID_VALUE, error.message ?: "Invalid timeline", SourceLocation(lineNumber, 1))))
        } catch (error: IllegalStateException) {
            MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, error.message ?: "Invalid timeline", SourceLocation(lineNumber, 1))))
        }
    }
}
