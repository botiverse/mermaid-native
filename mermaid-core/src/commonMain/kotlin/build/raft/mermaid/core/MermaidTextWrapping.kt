package build.raft.mermaid.core

/** A measured text token; wrapping preserves its formatting type. */
public data class MermaidWord(val content: String, val type: String = "normal")

/** Measurer-driven wrapping shared by Native cards and upstream compatibility replay. */
public object MermaidTextWrapping {
    public fun wrapLine(
        words: List<MermaidWord>,
        fits: (List<MermaidWord>) -> Boolean,
        graphemeClusters: Boolean = true,
    ): List<List<MermaidWord>> {
        require(words.none { '\n' in it.content }) { "splitLineToFitWidth does not support newlines in the line" }
        val remaining = ArrayDeque(words)
        val lines = mutableListOf<List<MermaidWord>>()
        var current = emptyList<MermaidWord>()
        while (remaining.isNotEmpty()) {
            val space = if (remaining.first().content == " ") remaining.removeFirst() else null
            val next = if (remaining.isEmpty()) MermaidWord(" ") else remaining.removeFirst()
            val candidate = current + (if (space == null) emptyList() else listOf(MermaidWord(" "))) + next
            if (fits(candidate)) {
                current = candidate
            } else if (current.isNotEmpty()) {
                lines += current; current = emptyList(); remaining.addFirst(next)
            } else if (next.content.isNotEmpty()) {
                val units = if (graphemeClusters) UnicodeGraphemes.split(next.content) else UnicodeGraphemes.codePoints(next.content)
                var used = 0; var prefix = ""
                for (unit in units) {
                    val added = prefix + unit
                    if (!fits(listOf(next.copy(content = added)))) {
                        if (used == 0) { used = 1; prefix = unit }
                        break
                    }
                    prefix = added; used++
                }
                lines += listOf(next.copy(content = prefix))
                val suffix = units.drop(used).joinToString("")
                if (suffix.isNotEmpty()) remaining.addFirst(next.copy(content = suffix))
            }
        }
        if (current.isNotEmpty()) lines += current
        return lines
    }

    /** Keep existing character-based card wrapping while making each grapheme indivisible. */
    public fun wrap(text: String, fits: (String) -> Boolean): List<String> = text.split('\n').flatMap { line ->
        wrapLine(listOf(MermaidWord(line)), { words -> fits(words.joinToString("") { it.content }) })
            .map { words -> words.joinToString("") { it.content } }
    }
}
