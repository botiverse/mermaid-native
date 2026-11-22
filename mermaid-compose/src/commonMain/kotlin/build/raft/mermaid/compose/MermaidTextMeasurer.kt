package build.raft.mermaid.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import build.raft.mermaid.layout.SceneColor
import build.raft.mermaid.layout.SceneSize
import build.raft.mermaid.layout.TextMeasurer as SceneTextMeasurer
import build.raft.mermaid.layout.TextStyle as SceneTextStyle

/**
 * Remembers a [mermaidTextMeasurer] for [textStyle]. Pass the same style to [MermaidCanvas] or
 * [measureMermaidScene], so a diagram is laid out with the font it is drawn in.
 */
@Composable
public fun rememberMermaidTextMeasurer(textStyle: TextStyle = TextStyle.Default): SceneTextMeasurer {
    // The scene measurer keeps the widths itself, so Compose need not cache the layouts.
    val textMeasurer = rememberTextMeasurer(cacheSize = 0)
    return remember(textMeasurer, textStyle) { mermaidTextMeasurer(textMeasurer, textStyle) }
}

/**
 * A layout text measurer that uses the fonts of [textMeasurer] and [textStyle], in scene units.
 *
 * Wrapping measures many prefixes of a label, so ASCII and CJK text is the sum of its characters'
 * widths (without kerning), read from one measurement per font. Any other text is measured whole.
 * Keep the result while [textMeasurer] and [textStyle] are unchanged; it is not thread-safe.
 */
public fun mermaidTextMeasurer(
    textMeasurer: TextMeasurer,
    textStyle: TextStyle = TextStyle.Default,
): SceneTextMeasurer = ComposeSceneTextMeasurer(textMeasurer, textStyle)

private class ComposeSceneTextMeasurer(private val textMeasurer: TextMeasurer, private val base: TextStyle) : SceneTextMeasurer {
    private class Widths(val ascii: FloatArray, val fullWidth: Float)

    private val widths = mutableMapOf<SceneTextStyle, Widths>()
    private val whole = mutableMapOf<Pair<String, SceneTextStyle>, SceneSize>()

    override fun measure(text: String, style: SceneTextStyle): SceneSize {
        // The color has no effect on the size, and one that does not paint has no Compose style.
        val font = style.copy(color = SceneColor("#000000"))
        val lineHeight = style.fontSize * LINE_HEIGHT
        if (text.isEmpty()) return SceneSize(0.0, lineHeight)
        if (text.all { it in ASCII || it.isFullWidth() }) {
            val table = widths.getOrPut(font) { characterWidths(font) }
            return SceneSize(text.sumOf { (if (it in ASCII) table.ascii[it - ASCII.first] else table.fullWidth).toDouble() }, lineHeight)
        }
        return whole.getOrPut(text to font) { SceneSize(layout(text, font).size.width.toDouble(), lineHeight) }
    }

    /** One character per line, so a single measurement gives every width. */
    private fun characterWidths(style: SceneTextStyle): Widths {
        val visible = ('!'..ASCII.last).toList()
        val lines = visible.map(Char::toString) + listOf("中", "n n", "nn")
        val result = layout(lines.joinToString("\n"), style)
        fun width(line: Int) = result.getLineRight(line) - result.getLineLeft(line)
        val space = width(lines.size - 2) - width(lines.size - 1)
        return Widths(FloatArray(visible.size + 1) { if (it == 0) space else width(it - 1) }, width(visible.size))
    }

    private fun layout(text: String, style: SceneTextStyle) = textMeasurer.measure(
        text = text,
        style = mermaidTextStyle(base, style)!!,
        softWrap = false,
        constraints = Constraints(),
        layoutDirection = LayoutDirection.Ltr,
        density = SceneDensity,
    )
}

private val ASCII = ' '..'~'

/** Ideographs, kana and full-width forms: one em each. */
private fun Char.isFullWidth(): Boolean =
    this in '、'..'〩' || this in 'ぁ'..'ゖ' || this in 'ァ'..'ヺ' ||
        this in '㐀'..'䶿' || this in '一'..'鿿' || this in '！'..'｠'

private const val LINE_HEIGHT = 1.2
