package build.raft.mermaid.layout.simple

import build.raft.mermaid.layout.*

/** Shared measurement/drawing contract so styled node text fits the node geometry. */
internal class FlowMarkdownLabel(source: String, private val base: TextStyle, private val measurer: TextMeasurer) {
    private val lines = MermaidMarkdown.lines(source).map { words ->
        val runs = mutableListOf<MermaidMarkdown.Word>()
        for (word in words) {
            val previous = runs.lastOrNull()
            if (previous?.type == word.type) {
                runs[runs.lastIndex] = previous.copy(content = previous.content + (if (word.spaceBefore) " " else "") + word.content)
            } else runs += word
        }
        runs
    }
    private fun style(word: MermaidMarkdown.Word) = when (word.type) {
        MermaidMarkdown.WordType.NORMAL -> base
        MermaidMarkdown.WordType.STRONG -> base.copy(fontWeight = maxOf(700, base.fontWeight))
        MermaidMarkdown.WordType.EM -> base.copy(italic = true)
    }
    private fun space(word: MermaidMarkdown.Word) = if (word.spaceBefore) measurer.measure(" ", base).width else 0.0
    private fun width(line: List<MermaidMarkdown.Word>) = line.sumOf { space(it) + measurer.measure(it.content, style(it)).width }
    val width: Double = lines.maxOf(::width)
    val lineCount: Int = lines.size
    fun draw(rect: SceneRect, lineHeight: Double): List<DrawText> = lines.flatMapIndexed { index, line ->
        var x = rect.x + (rect.width - width(line)) / 2
        val y = rect.y + rect.height / 2 + (index - (lines.size - 1) / 2.0) * lineHeight + base.fontSize * 0.35
        if (line.size == 1) {
            return@flatMapIndexed listOf(DrawText(line.single().content, ScenePoint(rect.x + rect.width / 2, y), TextAnchor.MIDDLE, style(line.single())))
        }
        line.map { word ->
            x += space(word)
            val paint = style(word)
            DrawText(word.content, ScenePoint(x, y), TextAnchor.START, paint).also {
                x += measurer.measure(word.content, paint).width
            }
        }
    }
}
