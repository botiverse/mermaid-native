package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class UsecaseGeneratedIdsLayoutTest {
    @Test fun generatedIdsPreserveLegacyAndExtendedDrawingCommands() {
        val measurer = TextMeasurer { text, _ -> SceneSize(text.length * 8.0, 14.0) }
        for (source in listOf(
            """usecase-beta
actor User
User --> Login
""",
            """usecase-beta
actor User
User --> Login
note for User "First note"
Login --> Logout
note for Logout "Second note"
""",
        )) {
            val model = assertIs<UsecaseDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
            val withoutGeneratedIds = model.copy(
                relationships = model.relationships.map { it.copy(id = null) },
                notes = model.notes.map { it.copy(id = null) },
            )
            assertEquals(
                SimpleMermaidLayout.layout(withoutGeneratedIds, measurer, LayoutConfig()),
                SimpleMermaidLayout.layout(model, measurer, LayoutConfig()),
            )
        }
    }
}
