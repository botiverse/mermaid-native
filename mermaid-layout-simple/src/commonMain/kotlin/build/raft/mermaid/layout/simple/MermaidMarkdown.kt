package build.raft.mermaid.layout.simple

/** Inline emphasis used by Native Markdown labels; HTML stays literal text. */
public object MermaidMarkdown {
    public enum class WordType { NORMAL, STRONG, EM }
    public data class Word(val content: String, val type: WordType, val spaceBefore: Boolean = false)
    private data class Span(val content: String, val type: WordType)

    /** Splits explicit lines into styled words while retaining adjacency across emphasis boundaries. */
    public fun lines(source: String): List<List<Word>> {
        val text = source.replace("<br/>", "\n").replace(Regex("\n{2,}"), "\n").trimIndent()
        val spans = mutableListOf<Span>()
        fun inline(value: String, type: WordType) {
            val plain = StringBuilder()
            fun flush() { if (plain.isNotEmpty()) { spans += Span(plain.toString(), type); plain.clear() } }
            var at = 0
            while (at < value.length) {
                if (value[at] == '\\' && at + 1 < value.length && value[at + 1] in "\\*_`") {
                    plain.append(value[at + 1]); at += 2; continue
                }
                val ch = value[at]
                if (ch == '*' || ch == '_') {
                    val count = if (value.getOrNull(at + 1) == ch) 2 else 1
                    val delimiter = ch.toString().repeat(count)
                    val begin = at + count
                    val intraword = ch == '_' && at > 0 && value[at - 1].isLetterOrDigit()
                    if (!intraword && value.getOrNull(begin)?.isWhitespace() == false) {
                        var end = value.indexOf(delimiter, begin)
                        while (end >= 0 && (end == begin || value[end - 1].isWhitespace() || value[end - 1] == '\\' ||
                                (ch == '_' && value.getOrNull(end + count)?.isLetterOrDigit() == true))) {
                            end = value.indexOf(delimiter, end + count)
                        }
                        if (end >= 0) {
                            flush(); inline(value.substring(begin, end), if (count == 2) WordType.STRONG else WordType.EM)
                            at = end + count; continue
                        }
                    }
                }
                plain.append(ch); at++
            }
            flush()
        }
        inline(text, WordType.NORMAL)
        val result = mutableListOf(mutableListOf<Word>())
        var space = false
        for (span in spans) {
            for (part in Regex("\\n|[^\\S\\n]+|[^\\s]+").findAll(span.content)) {
                val content = part.value
                when {
                    content == "\n" -> { result += mutableListOf<Word>(); space = false }
                    content.first().isWhitespace() -> space = true
                    else -> { result.last() += Word(content.replace("&#39;", "'"), span.type, space && result.last().isNotEmpty()); space = false }
                }
            }
        }
        return result
    }
}
