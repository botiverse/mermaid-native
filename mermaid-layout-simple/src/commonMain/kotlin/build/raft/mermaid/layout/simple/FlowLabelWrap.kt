package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.UnicodeGraphemes

/** Mermaid wraps flowchart node and edge labels at this width (`flowchart.wrappingWidth`). */
internal const val FLOW_LABEL_WRAP = 200.0

/** Corner radius of a rounded node: a pill around one line, and still clear of the text of several. */
internal const val FLOW_ROUND_RADIUS = 20.0

// CSS line breaking: no closing punctuation at the start of a line, no opening punctuation at its end.
private const val NO_LINE_START = "、。，．：；！？）】」』〉》〕｝”’…ー々・％%),.:;!?]}"
private const val NO_LINE_END = "（【「『〈《〔｛“‘([{"

private fun breaksAnywhere(grapheme: String): Boolean {
    val code = UnicodeGraphemes.codePoints(grapheme).first().let { point ->
        if (point.length == 2) 0x10000 + ((point[0].code - 0xD800) shl 10) + (point[1].code - 0xDC00) else point[0].code
    }
    return code in 0x2E80..0xA4CF || code in 0xAC00..0xD7A3 || code in 0xF900..0xFAFF || code in 0xFF01..0xFF60 || code >= 0x1F300
}

/** Whether a line may break between two graphemes with no space between them, as in a browser. */
private fun breaksBetween(before: String, after: String): Boolean = when {
    after in NO_LINE_START || before in NO_LINE_END -> false
    before == "-" -> after.first().isLetter()
    else -> breaksAnywhere(before) || breaksAnywhere(after)
}

/** [words] in unbreakable groups: at spaces, and inside and between words where a browser may break. */
private fun unbreakable(words: List<MermaidMarkdown.Word>): List<List<MermaidMarkdown.Word>> {
    val groups = mutableListOf<MutableList<MermaidMarkdown.Word>>()
    var last = ""
    for (word in words) {
        UnicodeGraphemes.split(word.content).forEachIndexed { index, grapheme ->
            val space = index == 0 && word.spaceBefore
            when {
                groups.isEmpty() || space || breaksBetween(last, grapheme) ->
                    groups.add(mutableListOf(MermaidMarkdown.Word(grapheme, word.type, space)))
                index == 0 -> groups.last().add(MermaidMarkdown.Word(grapheme, word.type))
                else -> groups.last().let { it[it.lastIndex] = it.last().copy(content = it.last().content + grapheme) }
            }
            last = grapheme
        }
    }
    return groups
}

/**
 * [words] over as many lines as [fits] needs. A word too long for a line stays whole on its own line,
 * as Mermaid's HTML labels leave it.
 */
internal fun flowWrapWords(
    words: List<MermaidMarkdown.Word>,
    fits: (List<MermaidMarkdown.Word>) -> Boolean,
): List<List<MermaidMarkdown.Word>> {
    if (words.isEmpty() || fits(words)) return listOf(words)
    val lines = mutableListOf<List<MermaidMarkdown.Word>>()
    var current = emptyList<MermaidMarkdown.Word>()
    for (group in unbreakable(words)) {
        if (current.isNotEmpty() && !fits(current + group)) { lines.add(current); current = emptyList() }
        current = if (current.isEmpty()) listOf(group.first().copy(spaceBefore = false)) + group.drop(1) else current + group
    }
    lines.add(current)
    return lines
}

/** One plain label line over as many lines as [fits] needs; a line that fits keeps its text as is. */
internal fun flowWrapLine(line: String, fits: (String) -> Boolean): List<String> {
    if (fits(line)) return listOf(line)
    fun text(words: List<MermaidMarkdown.Word>) = words.joinToString("") { (if (it.spaceBefore) " " else "") + it.content }
    val words = line.split(' ').filter { it.isNotEmpty() }
        .mapIndexed { index, word -> MermaidMarkdown.Word(word, MermaidMarkdown.WordType.NORMAL, index > 0) }
    return if (words.isEmpty()) listOf(line) else flowWrapWords(words) { fits(text(it)) }.map(::text)
}
