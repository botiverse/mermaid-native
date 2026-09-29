package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class RadarModelLayoutTest {
    @Test fun inferredMaximumFillsRadiusForSmallAndNamedData() {
        for (data in listOf("curve c{3,2,1}", "curve c{C:1,A:3,B:2}")) {
            val chart = assertIs<RadarChartDiagram>(assertIs<MermaidParseResult.Success>(
                MermaidParser.parse("radar-beta\naxis A,B,C\n$data")
            ).diagram)
            assertEquals(3.0, chart.maximum)
            assertEquals(listOf(3.0,2.0,1.0), chart.axisValues(chart.curves.single()))
            val scene = SimpleMermaidLayout.layout(chart, FixedWidthTextMeasurer, LayoutConfig())
            val marker = scene.commands.filterIsInstance<DrawPolygon>().first { it.fill.value == DiagramPalette.BLUE }
            assertEquals(98.0, marker.points.first().y)
            assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text == "3" })
        }
    }
    @Test fun invalidRangeAndClippingStayFinite() {
        assertEquals(0.0, RadarGeometry.relativeRadius(0.0,0.0,0.0,170.0))
        assertEquals(0.0, RadarGeometry.relativeRadius(5.0,10.0,0.0,170.0))
        assertEquals(0.0, RadarGeometry.relativeRadius(-5.0,0.0,10.0,100.0))
        assertEquals(100.0, RadarGeometry.relativeRadius(15.0,0.0,10.0,100.0))
        assertEquals(75.0, RadarGeometry.relativeRadius(5.0,-10.0,10.0,100.0))
    }
    @Test fun denseGridKeepsReadableTickLabelsAndTheMaximum() {
        val chart = assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "radar-beta\naxis A,B,C\ncurve Small{3,2,1}\nticks 33\nshowLegend false"
        )).diagram
        val scene = SimpleMermaidLayout.layout(chart, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(32, scene.commands.filterIsInstance<DrawEllipse>().size)
        val labels = scene.commands.filterIsInstance<DrawText>().filter { it.text.toDoubleOrNull() != null }
        assertTrue(labels.size in 2..9)
        assertTrue(labels.any { it.text == "3" })
        labels.sortedBy { it.origin.y }.zipWithNext().forEach { (a,b) ->
            assertTrue(b.origin.y - a.origin.y >= 18.0)
        }
    }

}
