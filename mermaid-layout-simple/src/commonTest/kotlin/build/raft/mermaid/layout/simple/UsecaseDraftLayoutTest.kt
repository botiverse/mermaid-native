package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class UsecaseDraftLayoutTest {
    private fun layout(diagram: UsecaseDiagram): LayoutScene =
        SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())

    @Test fun committedDraftUsesTheSameRendererAsParsedContent() {
        val doc = UsecaseDocument(); val draft = doc.createModel()
        draft.actors!!["User"] = UsecaseActor("User", "User")
        draft.useCases!!["Login"] = UsecaseNode("Login", "Sign in", UsecaseShape.ELLIPSE)
        draft.relationships!!.add(UsecaseRelationship("User", "Login", id = "edge_0", labelType = "text"))
        doc.commit(draft)
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse("usecase-beta\nactor User\nLogin(Sign in)\nUser --> Login")).diagram as UsecaseDiagram
        assertEquals(layout(parsed).commands, layout(doc.diagram!!).commands)
        val before = layout(doc.diagram!!)
        draft.useCases!!.clear(); draft.relationships!!.clear()
        assertEquals(before, layout(doc.diagram!!))
    }

    @Test fun shapePaddingReachesBothPlainAndStyledDrawingCommands() {
        val label = "A sufficiently long label"
        val measured = FixedWidthTextMeasurer.measure(label, TextStyle(fontSize = 13.0, fontWeight = 600)).width
        for (styled in listOf(false, true)) for (shape in UsecaseShape.entries) {
            val doc = UsecaseDocument(); val draft = doc.createModel()
            val node = UsecaseNode("N", label, shape)
            draft.useCases!![node.id] = node
            if (styled) draft.attributes!![node.id] = UsecaseAttributes(styles = mapOf("fill" to "#abc"))
            doc.commit(draft)
            assertEquals(node.labelPadding(), doc.usecaseLabelData().single()["padding"])
            val scene = layout(doc.diagram!!)
            val actual = if (shape == UsecaseShape.ELLIPSE) scene.commands.filterIsInstance<DrawEllipse>().single().radiusX * 2
                else scene.commands.filterIsInstance<DrawRect>().single().rect.width
            assertTrue(actual >= measured + node.labelPadding() * 2 - 1e-9)
            if (shape == UsecaseShape.RECTANGLE) assertEquals(measured + 20.0, actual, 1e-9)
        }
    }
}
