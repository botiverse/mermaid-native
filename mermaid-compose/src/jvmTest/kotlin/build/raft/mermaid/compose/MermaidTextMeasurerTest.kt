package build.raft.mermaid.compose

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import build.raft.mermaid.core.MermaidParseResult
import build.raft.mermaid.core.MermaidParser
import build.raft.mermaid.layout.DrawText
import build.raft.mermaid.layout.LayoutConfig
import build.raft.mermaid.layout.SceneColor
import build.raft.mermaid.layout.simple.SimpleMermaidLayout
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import build.raft.mermaid.layout.TextStyle as SceneTextStyle

class MermaidTextMeasurerTest {
    private val compose = TextMeasurer(createFontFamilyResolver(), Density(1f), LayoutDirection.Ltr)
    private val measurer = mermaidTextMeasurer(compose)

    private fun drawnWidth(text: String, style: SceneTextStyle): Double = compose.measure(
        text, mermaidTextStyle(TextStyle.Default, style)!!, softWrap = false, constraints = Constraints(), density = Density(1f),
    ).let { it.getLineRight(0) - it.getLineLeft(0) }.toDouble()

    @Test fun asciiAndCjkAreAsWideAsTheyAreDrawn() {
        for (style in listOf(SceneTextStyle(), SceneTextStyle(fontSize = 20.0, fontWeight = 700), SceneTextStyle(italic = true)))
            for (text in listOf("How to fix", "Approved?", "iiiiiiii", "WWWWWWWW", "发布到 App 商店", "héllo → wörld")) {
                val drawn = drawnWidth(text, style)
                val measured = measurer.measure(text, style).width
                assertTrue(abs(measured - drawn) <= maxOf(1.5, drawn * 0.04), "$text: measured $measured, drawn $drawn")
            }
    }

    @Test fun narrowAndWideLettersDiffer() {
        val style = SceneTextStyle()
        assertTrue(measurer.measure("WWWW", style).width > 2 * measurer.measure("iiii", style).width)
        assertTrue(measurer.measure("a b", style).width > measurer.measure("ab", style).width)
        assertEquals(0.0, measurer.measure("", style).width)
        assertEquals(style.fontSize * 1.2, measurer.measure("x", style).height, 1e-9)
        assertEquals(measurer.measure("x", style), measurer.measure("x", style.copy(color = SceneColor("none"))))
    }

    @Test fun aBoldWordFollowsTheWordsBeforeIt() {
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse("flowchart TD\nA[\"`How to fix **fix**`\"]"))
        val texts = SimpleMermaidLayout.layout(parsed.diagram, measurer, LayoutConfig()).commands.filterIsInstance<DrawText>()
        assertEquals(listOf("How to fix", "fix"), texts.map { it.text })
        val gap = texts[1].origin.x - texts[0].origin.x - drawnWidth("How to fix", texts[0].style)
        assertTrue(gap in 1.0..8.0, "gap before the bold word was $gap")
    }
}
