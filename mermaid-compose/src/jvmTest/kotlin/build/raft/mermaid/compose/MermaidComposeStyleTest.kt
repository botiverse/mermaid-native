package build.raft.mermaid.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import build.raft.mermaid.layout.SceneColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import build.raft.mermaid.layout.TextStyle as SceneTextStyle

class MermaidComposeStyleTest {
    @Test fun parsesHexColorsInEveryLength() {
        assertEquals(Color(0xff, 0x00, 0x00, 0xff), SceneColor("#f00").composeColor())
        assertEquals(Color(0x00, 0xff, 0x00, 0x88), SceneColor("#0f08").composeColor())
        assertEquals(Color(0x12, 0x34, 0x56, 0xff), SceneColor(" #123456 ").composeColor())
        assertEquals(Color(0x12, 0x34, 0x56, 0x80), SceneColor("#12345680").composeColor())
        assertEquals(Color(0xAB, 0xCD, 0xEF, 0xff), SceneColor("#ABCDEF").composeColor())
    }

    @Test fun multipliesAlpha() {
        assertEquals(Color(0, 0, 0, 127), SceneColor("#000000").composeColor(0.5f))
        assertEquals(0.25f, assertNotNull(SceneColor("#00000080").composeColor(0.5f)).alpha, 0.01f)
        assertEquals(Color(0, 0, 0, 255), SceneColor("#000").composeColor(2f))
        assertEquals(Color(0, 0, 0, 0), SceneColor("#000").composeColor(-1f))
    }

    @Test fun doesNotPaintNoneOrUnparseableColors() {
        assertNull(SceneColor("none").composeColor())
        assertNull(SceneColor("NONE").composeColor())
        assertNull(SceneColor("red").composeColor())
        assertNull(SceneColor("").composeColor())
        assertNull(SceneColor("#12").composeColor())
        assertNull(SceneColor("#12345").composeColor())
        assertNull(SceneColor("#zzzzzz").composeColor())
        assertNull(SceneColor("#-12").composeColor())
    }

    @Test fun safeScaleKeepsTheLargestSideWithinTheLimit() {
        assertEquals(2f, mermaidSafeScale(400f, 300f, 2f, 3f))
        val clamped = mermaidSafeScale(20_000f, 100f, 1f, 2f)
        assertEquals(MermaidMaxSidePx / 40_000f, clamped)
        assertEquals(MermaidMaxSidePx, 20_000f * clamped * 2f, 0.1f)
        assertEquals(MermaidMaxSidePx / 30_000f, mermaidSafeScale(100f, 10_000f, 4f, 3f))
    }

    @Test fun safeScaleRejectsInvalidInput() {
        assertEquals(0f, mermaidSafeScale(100f, 100f, 0f, 1f))
        assertEquals(0f, mermaidSafeScale(100f, 100f, -1f, 1f))
        assertEquals(0f, mermaidSafeScale(100f, 100f, Float.NaN, 1f))
        assertEquals(0f, mermaidSafeScale(100f, 100f, 1f, 0f))
        assertEquals(0f, mermaidSafeScale(100f, 100f, 1f, Float.POSITIVE_INFINITY))
        assertEquals(0f, mermaidSafeScale(0f, 0f, 1f, 1f))
        assertEquals(0f, mermaidSafeScale(Float.NaN, 10f, 1f, 1f))
    }

    @Test fun mergesSceneTextIntoTheHostStyle() {
        val host = TextStyle(fontFamily = FontFamily.Cursive, letterSpacing = 1.sp, background = Color.Red, textAlign = TextAlign.End)
        val style = assertNotNull(mermaidTextStyle(host, SceneTextStyle(fontSize = 20.0, fontFamily = "monospace", fontWeight = 600, color = SceneColor("#336699"), italic = true)))
        assertEquals(FontFamily.Cursive, style.fontFamily, "the host font family wins")
        assertEquals(1.sp, style.letterSpacing)
        assertEquals(20.sp, style.fontSize)
        assertEquals(24f, style.lineHeight.value, 0.001f)
        assertEquals(FontWeight(600), style.fontWeight)
        assertEquals(FontStyle.Italic, style.fontStyle)
        assertEquals(Color(0x33, 0x66, 0x99), style.color)
        assertEquals(TextAlign.Start, style.textAlign)
        assertEquals(Color.Unspecified, style.background)
    }

    @Test fun usesTheSceneFontFamilyWhenTheHostHasNone() {
        fun family(name: String) = mermaidTextStyle(TextStyle.Default, SceneTextStyle(fontFamily = name))?.fontFamily
        assertEquals(FontFamily.Monospace, family("monospace"))
        assertEquals(FontFamily.Serif, family(" Serif "))
        assertEquals(FontFamily.SansSerif, family("sans-serif"))
        assertEquals(FontFamily.SansSerif, family("trebuchet ms, verdana, arial"))
        val plain = assertNotNull(mermaidTextStyle(TextStyle.Default, SceneTextStyle()))
        assertEquals(FontStyle.Normal, plain.fontStyle)
        assertEquals(FontWeight.Normal, plain.fontWeight)
    }

    @Test fun skipsTextThatDoesNotPaint() {
        assertNull(mermaidTextStyle(TextStyle.Default, SceneTextStyle(color = SceneColor("none"))))
    }
}
