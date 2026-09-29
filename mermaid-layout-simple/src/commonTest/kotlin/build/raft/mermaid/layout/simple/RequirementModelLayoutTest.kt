package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class RequirementModelLayoutTest {
    private fun scene(source: String): LayoutScene = SimpleMermaidLayout.layout(
        assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram,
        FixedWidthTextMeasurer, LayoutConfig(),
    )

    @Test fun accessibilityMetadataDoesNotChangeVisibleGeometry() {
        val body = "requirementDiagram\nrequirement Login {\ntext: Sign in\n}"
        val plain = scene(body)
        val accessible = scene(body + "\naccTitle: Authentication\naccDescr: User login requirement")
        assertEquals("Authentication", accessible.accessibilityTitle)
        assertEquals("User login requirement", accessible.accessibilityDescription)
        assertEquals(plain.commands, accessible.commands)
        assertEquals(plain.width, accessible.width)
        assertEquals(plain.height, accessible.height)
    }

    @Test fun classStylesReachBothCardKindsWithInlinePrecedence() {
        val source = """requirementDiagram
            requirement R {
            }
            element E {
            }
            classDef default fill:#eeeeee
            classDef hot color:red,stroke-width:4px,stroke:yellow
            class R,E hot
            style R color:blue
        """.trimIndent()
        val chart = assertIs<RequirementDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
        assertEquals(listOf("fill:#eeeeee","color:red","stroke-width:4px","stroke:yellow","color:blue"),
            chart.resolvedStyles(chart.requirements.single().classes, chart.requirements.single().styles))
        val output = scene(source)
        val cards = output.commands.filterIsInstance<DrawRect>()
        assertEquals(2, cards.size)
        assertTrue(cards.all { it.fill.value == "#eeeeee" && it.stroke.value == "#ffff00" && it.strokeWidth == 4.0 })
        val labels = output.commands.filterIsInstance<DrawText>()
        assertEquals("#0000ff", labels.first { it.text == "requirement R" }.style.color.value)
        assertEquals("#ff0000", labels.first { it.text == "element E" }.style.color.value)
    }
}
