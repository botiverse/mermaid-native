package build.raft.mermaid.core

import kotlin.test.*

class UsecaseActorProductionTest {
    private fun model(source: String) = assertIs<UsecaseDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)

    @Test fun resolvesFinalActorMetadataAtProductionBoundary() {
        val d = model("""usecase-beta
actor Normal
actor Hollow@{ type: hollow, business: true }
actor Awesome@{ type: awesome }
actor Icon@{ type: normal, icon: "missing:user" }
actor Overridden@{ type: hollow }
Overridden@{ type: awesome }
Normal --> Login
Login ..> : include Audit
Login --|> Base
""")
        assertEquals(listOf(UsecaseActorType.NORMAL, UsecaseActorType.HOLLOW, UsecaseActorType.AWESOME, UsecaseActorType.ICON, UsecaseActorType.AWESOME), d.actors.map { it.type })
        assertEquals(listOf(false, true, false, false, false), d.actors.map { it.business })
        assertEquals("missing:user", d.actors[3].icon)
        assertEquals(listOf(UsecaseRelationshipType.ASSOCIATION, UsecaseRelationshipType.INCLUDE, UsecaseRelationshipType.GENERALIZATION), d.relationships.map { it.type })
    }

    @Test fun compactRelationshipLabelsSurviveProductionParsing() {
        val d = model("usecase-beta\nactor Developer\nDeveloper --important--> WriteCode")
        assertEquals("important", d.relationships.single().label)
        assertEquals("WriteCode", d.relationships.single().targetId)
        assertEquals(listOf("WriteCode"), d.useCases.map { it.id })
        assertEquals("a--b", model("usecase-beta\nA -- \"a--b\" --> B").relationships.single().label)
        val destination = model("usecase-beta\nA -- \"Target Name\"")
        assertNull(destination.relationships.single().label)
        assertEquals("Target_Name", destination.relationships.single().targetId)
    }

    @Test fun rejectsIncompatibleActorGeometryBeforePublishing() {
        for (metadata in listOf("type: hollow, icon: user", "type: awesome, icon: user", "type: awesome, business: true", "icon: user, business: true", "type: icon", "type: unknown", "business: maybe")) {
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("usecase-beta\nactor User@{ $metadata }"), metadata)
        }
        assertEquals(UsecaseActorType.NORMAL, model("usecase-beta\nactor User@{ icon: \"\", business: false }").actors.single().type)
    }
}
