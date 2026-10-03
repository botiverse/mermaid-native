package build.raft.mermaid.core

import kotlin.test.*

class UsecaseGrammarLocationTest {
    @Test fun pointsToTheRejectedTokenAfterCrLfAndARepeatedQuotedToken() {
        val source = "usecase-beta\r\nBefore(\"json Payload@ 😀\")\r\nsystemBoundary Auth\r\n  json Payload@{}\r\nend"
        val failure = assertIs<MermaidParseResult.Failure>(UsecaseDocument().parse(source))
        val start = source.lastIndexOf("json Payload@")
        val diagnostic = failure.diagnostics.single()
        assertEquals(SourceLocation(4, 3), diagnostic.location)
        assertTrue(diagnostic.message.contains("but found: 'json Payload@'"))
        assertTrue(diagnostic.message.endsWith("at line 4, column 3 [$start,${start + 13})"))
    }

    @Test fun rejectsStereotypeAfterEdgeMetadataWithoutPublishingPriorState() {
        val doc = UsecaseDocument()
        assertIs<MermaidParseResult.Success>(doc.parse("usecase-beta\nPrevious"))
        val source = "usecase-beta\nA link@--> B\nlink@{ animate: true } <<Link>>"
        val failure = assertIs<MermaidParseResult.Failure>(doc.parse(source))
        val start = source.indexOf("<<")
        assertTrue(failure.diagnostics.single().message.endsWith("at line 3, column 24 [$start,${start + 2})"))
        assertNull(doc.ast); assertNull(doc.diagram)
    }

    @Test fun acceptsDeclarationStereotypesAndQuotedReservedTextInsideBoundaries() {
        val doc = UsecaseDocument()
        assertIs<MermaidParseResult.Success>(doc.parse("usecase-beta\nsystemBoundary Auth\nactor User(\"note json Payload@ <<Text>>\") <<Human>>\nLogin(\"systemBoundary --> @{\") <<Primary>>\nend\nUser --> Login"))
        assertEquals("note json Payload@ <<Text>>", doc.diagram!!.actors.single().label)
        assertEquals("Primary", doc.diagram!!.attributes["Login"]?.stereotype)
    }
}
