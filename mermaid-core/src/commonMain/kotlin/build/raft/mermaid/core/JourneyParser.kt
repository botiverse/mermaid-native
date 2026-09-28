package build.raft.mermaid.core

/** Read journey tokens without discarding optional actors or empty sections. */
internal class JourneyParser(private val source: String) {
    fun parse(): MermaidParseResult {
        var line = 1
        return try {
            var offset = 0
            fun token(pattern: String) = Regex("^(?:$pattern)", RegexOption.IGNORE_CASE).find(source.substring(offset))?.value
            fun consume(text: String) { offset += text.length; line += text.count { it == '\n' } }
            fun skip(): Boolean {
                val text = token("%%(?!\\{)[^\\n]*|[^}]%%[^\\n]*|\\s+|#[^\\n]*") ?: return false
                consume(text); return true
            }
            while (offset < source.length && skip()) Unit
            val header = token("journey") ?: error("Invalid journey header")
            consume(header)
            var title: String? = null; var accTitle: String? = null; var accDescription: String? = null
            val sections = mutableListOf<UserJourneySection>()
            while (offset < source.length) {
                if (skip()) continue
                val titleToken = token("title\\s[^#\\n;]+")
                if (titleToken != null) { title = titleToken.substring(6); consume(titleToken); continue }
                val acc = token("acc(?:Title|Descr)\\s*:\\s*")
                if (acc != null) {
                    consume(acc); val value = source.substring(offset).substringBefore('\n')
                    if (acc.startsWith("accTitle", true)) accTitle = value.trim() else accDescription = value.trim()
                    consume(value); continue
                }
                val block = token("accDescr\\s*\\{\\s*")
                if (block != null) {
                    consume(block); val end = source.indexOf('}', offset)
                    require(end >= 0) { "Unclosed accessibility description" }
                    val value = source.substring(offset, end)
                    accDescription = value.trim().replace(Regex("\\n\\s+"), "\n"); consume(value + "}"); continue
                }
                val section = token("section\\s[^#:\\n;]+")
                if (section != null) { sections += UserJourneySection(section.substring(8), emptyList()); consume(section); continue }
                val label = token("[^#:\\n;]+") ?: error("Invalid journey task")
                consume(label)
                val data = token(":[^#\\n;]+") ?: error("Expected journey score")
                consume(data)
                val parts = data.drop(1).split(':')
                val score = parts.first().trim().toIntOrNull() ?: error("Expected an integer journey score")
                val actors = if (parts.size == 1) emptyList() else parts[1].split(',').map { it.trim() }
                if (sections.isEmpty()) sections += UserJourneySection("", emptyList())
                val current = sections.last()
                sections[sections.lastIndex] = current.copy(tasks = current.tasks + UserJourneyTask(label, score, actors))
            }
            MermaidParseResult.Success(UserJourneyDiagram(title, sections, accTitle, accDescription))
        } catch (error: IllegalArgumentException) {
            MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.INVALID_VALUE, error.message ?: "Invalid journey", SourceLocation(line, 1))))
        } catch (error: IllegalStateException) {
            MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, error.message ?: "Invalid journey", SourceLocation(line, 1))))
        }
    }
}
