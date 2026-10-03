package build.raft.mermaid.core

import kotlin.test.*

class UsecaseMetadataValidationTest {
    @Test fun locatesTheActualQuotedKeyAfterCrLfEmojiAndRepeatedLabelText() {
        val source = "usecase-beta\r\n%% 😀\r\nactor User(\"type\")@{ \"type\": giant }"
        val start = source.lastIndexOf("\"type\"")
        val column = start - source.lastIndexOf('\n', start)
        val failure = assertIs<MermaidParseResult.Failure>(UsecaseDocument().parse(source))
        assertEquals("Metadata property 'type' is invalid for actor 'User' at line 3, column $column [$start,${start + 6})", failure.diagnostics.single().message)
        assertEquals(SourceLocation(3, column), failure.diagnostics.single().location)
    }

    @Test fun validatesEveryOccurrenceAndPreservesBooleanTypes() {
        val doc = UsecaseDocument()
        for (body in listOf("actor User@{ type: giant }\nUser@{ type: normal }", "actor User@{ business: \"true\" }", "actor User@{ icon: true }", "systemBoundary Auth@{ animate: true }\nend")) {
            assertIs<MermaidParseResult.Success>(doc.parse("usecase-beta\nPrevious"))
            assertIs<MermaidParseResult.Failure>(doc.parse("usecase-beta\n$body"), body)
            assertNull(doc.ast); assertNull(doc.diagram)
        }
    }

    @Test fun crossPropertyErrorsPointToTheDeclaration() {
        val source = "usecase-beta\nactor User@{ type: awesome }\nUser@{ business: true }"
        val failure = assertIs<MermaidParseResult.Failure>(UsecaseDocument().parse(source))
        val start = source.indexOf("User")
        assertEquals("Business actor 'User' must use normal or hollow geometry at line 2, column 7 [$start,${start + 4})", failure.diagnostics.single().message)
    }

    @Test fun validTypesOverridesAndGeometryRemainAccepted() {
        val doc = UsecaseDocument()
        assertIs<MermaidParseResult.Success>(doc.parse("usecase-beta\nsystemBoundary Auth@{ type: package }\nactor User@{ type: hollow, business: true }\nLogin@{ business: true }\nend\nactor Icon@{ icon: \"\", business: false }\nUser@{ type: normal }"))
        assertEquals(UsecaseActorType.NORMAL, doc.diagram!!.actors.first().type)
        assertTrue(doc.diagram!!.actors.first().business)
        assertEquals("package", doc.diagram!!.attributes["Auth"]?.properties?.get("type"))
        assertEquals("true", doc.diagram!!.attributes["Login"]?.properties?.get("business"))
    }
}
