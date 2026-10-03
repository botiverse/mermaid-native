package build.raft.mermaid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UsecaseGeneratedIdsTest {
    private fun parse(source: String): UsecaseDiagram = assertIs<UsecaseDiagram>(
        assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram,
    )

    @Test fun explicitEdgesDoNotConsumeAnonymousIdsAndNotesUseTheirOwnSequence() {
        val source = """
            usecase-beta
            actor User
            User named@--> Login
            note for User "first"
            User --> Login
            Login dependency@--> Logout
            note for Logout "second"
            Logout --> User
            style named stroke:red
        """.trimIndent()
        val model = parse(source)
        assertEquals(listOf("named", "edge-0", "dependency", "edge-1"), model.relationships.map { it.id })
        assertEquals(listOf("note-0", "note-1"), model.notes.map { it.id })
        assertEquals(listOf("User", "Logout"), model.notes.map { it.targetId })
        assertEquals("red", model.attributes.getValue("named").styles["stroke"])
        assertEquals(model, parse(source))
    }

    @Test fun anotherParseStartsNumberingFromZeroAfterBothSuccessAndFailure() {
        parse("usecase-beta\nA --> B\nB --> C\nnote for A \"first\"")
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("usecase-beta\nA --> B\nnote for Missing \"invalid\""))
        val next = parse("usecase-beta\nX --> Y\nnote for Y \"next\"")
        assertEquals(listOf("edge-0"), next.relationships.map { it.id })
        assertEquals(listOf("note-0"), next.notes.map { it.id })
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("usecase-beta\nA --> B\nstyle Missing stroke:red"))
    }
}
