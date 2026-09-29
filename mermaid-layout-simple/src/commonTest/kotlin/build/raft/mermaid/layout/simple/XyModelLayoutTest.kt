package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class XyModelLayoutTest {
    private fun scene(source: String): LayoutScene = SimpleMermaidLayout.layout(
        assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram,
        FixedWidthTextMeasurer, LayoutConfig(),
    )

    @Test fun orphanValuesCannotChangeVisibleGeometryOrScale() {
        for (orientation in listOf("vertical", "horizontal")) {
            val source = "xychart-beta $orientation\nx-axis [Q1, Q2]\nbar [10, 50]\nline [20, 40]"
            val extra = source.replace("[10, 50]", "[10, 50, 999, -800]")
                .replace("[20, 40]", "[20, 40, 2000]")
            assertEquals(scene(source), scene(extra), orientation)
        }
    }

    @Test fun missingBandValuesDoNotPaintFakePoints() {
        for (orientation in listOf("vertical", "horizontal")) {
            val output = scene("xychart-beta $orientation\nx-axis [A, B, C, D]\nbar [1, 2]\nline [1, 2]")
            assertEquals(2, output.commands.filterIsInstance<DrawRect>().size)
            assertEquals(2, output.commands.filterIsInstance<DrawPolyline>().single().points.size)
            assertTrue(output.commands.filterIsInstance<DrawText>().map { it.text }.containsAll(listOf("A", "B", "C", "D")))
        }
    }

    @Test fun surroundingLegendWhitespaceDoesNotChangeLayout() {
        for (orientation in listOf("vertical", "horizontal")) {
            val source = "xychart-beta $orientation\nx-axis [A, B]\nline \"avg\" [10, 20]\nbar \"p95\" [30, 40]"
            val padded = source.replace("\"avg\"", "\" avg \"").replace("\"p95\"", "\" p95 \"")
            assertEquals(scene(source), scene(padded), orientation)
        }
    }

    @Test fun coincidentSeriesLabelsRemainReadableInBothOrientations() {
        for (orientation in listOf("vertical", "horizontal")) {
            val output = scene("xychart-beta $orientation\nx-axis [A, B, C, D]\nbar [1, 2]\nline [1, 2]")
            val labels = output.commands.filterIsInstance<DrawText>().filter { it.style.fontWeight == 600 }
            assertEquals(4, labels.size)
            for (value in listOf("1", "2")) {
                val pair = labels.filter { it.text == value }
                assertEquals(2, pair.size)
                val height = FixedWidthTextMeasurer.measure(value, pair.first().style).height
                assertTrue(kotlin.math.abs(pair[0].origin.y - pair[1].origin.y) >= height + 3.0, orientation)
                assertTrue(pair.all { it.origin.y - height >= LayoutConfig().padding && it.origin.y <= output.height - LayoutConfig().padding })
            }
        }
    }
    @Test fun valueLabelsAvoidFilledBarsRegardlessOfSeriesOrder() {
        for (orientation in listOf("vertical", "horizontal")) {
            for (series in listOf("bar [45]\nline [40]", "line [40]\nbar [45]")) {
                val output = scene("xychart-beta $orientation\nx-axis [Q2]\ny-axis 0 --> 100\n$series")
                val bars = output.commands.filterIsInstance<DrawRect>().map { it.rect }
                val labels = output.commands.filterIsInstance<DrawText>().filter { it.style.fontWeight == 600 }
                assertEquals(2, labels.size)
                for (label in labels) {
                    val size = FixedWidthTextMeasurer.measure(label.text, label.style)
                    val left = label.origin.x - if (label.anchor == TextAnchor.MIDDLE) size.width / 2 else 0.0
                    assertTrue(bars.none { bar ->
                        left < bar.x + bar.width && left + size.width > bar.x &&
                            label.origin.y - size.height < bar.y + bar.height && label.origin.y > bar.y
                    }, "$orientation $series ${label.text}")
                }
            }
        }
    }
}
