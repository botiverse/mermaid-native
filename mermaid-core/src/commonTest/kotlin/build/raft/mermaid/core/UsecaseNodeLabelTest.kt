package build.raft.mermaid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UsecaseNodeLabelTest {
    private fun parse(source: String): UsecaseDiagram =
        assertIs<UsecaseDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)

    @Test fun distinguishesMarkdownFromLiteralMarkersForActorsAndNodes() {
        val d = parse("""usecase-beta
actor Formatted("`**Reader**`")
actor Literal("**Reader**")
Formatted --> Markdown["`*Open*`"]
Literal --> Plain["*Open*"]
""")
        assertEquals(listOf("**Reader**", "**Reader**"), d.actors.map { it.label })
        assertEquals(listOf("markdown", "text"), d.actors.map { it.labelType })
        assertEquals(listOf("*Open*", "*Open*"), d.useCases.map { it.label })
        assertEquals(listOf("markdown", "text"), d.useCases.map { it.labelType })
    }

    @Test fun preservesDerivedIdsAndPhysicalNewlinesInMarkdownLabels() {
        val d = parse("""usecase-beta
actor "`Customer Service`"
Login("`Sign
in`")
""")
        assertEquals("Customer_Service", d.actors.single().id)
        assertEquals("markdown", d.actors.single().labelType)
        assertEquals("Sign\nin", d.useCases.single().label)
        assertEquals("markdown", d.useCases.single().labelType)
    }

    @Test fun forwardReferenceRefinementKeepsMarkdownWhenLaterReferencedById() {
        val d = parse("""usecase-beta
User --> Login
actor User("`**Reader**`")
Login["`*Sign in*`"]
User --> Login
""")
        assertEquals("markdown", d.actors.single().labelType)
        assertEquals("**Reader**", d.actors.single().label)
        assertEquals("markdown", d.useCases.single().labelType)
        assertEquals("*Sign in*", d.useCases.single().label)
        assertEquals(UsecaseShape.RECTANGLE, d.useCases.single().shape)
    }
}
