package build.raft.mermaid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertIs

class FontSizeTest {
    @Test fun relativeUnitsRetainFractionalPrecisionAndResolveAgainstTheBase() {
        val size = FontSize.parse("1.5em")!!
        assertEquals(1.5, size.value)
        assertEquals("1.5em", size.cssValue)
        assertEquals(21.0, size.resolvePixels(14.0))
        assertEquals(24.0, size.resolvePixels(16.0))
        assertEquals(14.5, FontSize.parse("14.5px")!!.resolvePixels(14.0))
        assertEquals(100.0, FontSize.parse("1e2")!!.resolvePixels(14.0))
        assertEquals("14px", FontSize.parse(14)!!.cssValue)
    }

    @Test fun relativeUnitsApplyOnlyToFontSizesInStrictStyleParsers() {
        for (source in listOf(
            "classDiagram\nclass A\nstyle A PROPERTY:2em",
            "erDiagram\nA\nstyle A PROPERTY:2em",
        )) {
            assertIs<MermaidParseResult.Success>(MermaidParser.parse(source.replace("PROPERTY", "font-size")))
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source.replace("PROPERTY", "stroke-width")))
        }
    }

    @Test fun invalidInputsAndUnboundedResolvedSizesFallBack() {
        for (value in listOf(null, true, mapOf("fontSize" to 14), "bad", "14rem", "calc(2em)", Double.NaN, Double.POSITIVE_INFINITY)) {
            assertNull(FontSize.parse(value), value.toString())
        }
        for (value in listOf("0", "-2em", "513px", "40em")) {
            assertNull(FontSize.parse(value)!!.resolvePixels(14.0), value)
        }
        assertNull(FontSize.parse("2em")!!.resolvePixels(Double.POSITIVE_INFINITY))
        assertEquals(512.0, FontSize.parse("32em")!!.resolvePixels(16.0))
    }
}
