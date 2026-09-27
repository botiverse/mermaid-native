package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class PieUpstreamLayoutTest {
    @Test fun accessibilitySurvivesTheProductLayoutPath() {
        val diagram = assertIs<MermaidParseResult.Success>(MermaidParser.parse("pie accTitle: Pets\naccDescr { Dogs and cats }\n\"dogs\":60\n\"cats\":40")).diagram
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals("Pets", scene.accessibilityTitle)
        assertEquals("Dogs and cats", scene.accessibilityDescription)
        assertEquals(2, scene.commands.filterIsInstance<DrawPolygon>().size)
    }

    @Test fun longTitleAndDisplayedValuesFitTheScene() {
        val title = "This chart has a deliberately long title that must remain inside the exported image"
        val diagram = PieDiagram(title, true, listOf(PieSection("Long account balance with many digits", 123456789012345.0)))
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        for (text in scene.commands.filterIsInstance<DrawText>().filter { it.anchor == TextAnchor.START }) {
            assertTrue(text.origin.x + FixedWidthTextMeasurer.measure(text.text, text.style).width <= scene.width, text.text)
        }
    }
}
