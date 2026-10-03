package build.raft.mermaid.core

import kotlin.test.*

class UsecaseSemanticsTest {
    @Test fun relationshipErrorsUseTheActualRelationAfterUnicodeAndCrLf() {
        val source = "usecase-beta\r\n%% 😀 ..> repeated\r\nactor User\r\nLogin\r\nUser ..> : include Login"
        val start = source.lastIndexOf("..>")
        val failure = assertIs<MermaidParseResult.Failure>(UsecaseDocument().parse(source))
        assertEquals("include relationship requires use-case endpoints at line 5, column 6 [$start,${source.length})", failure.diagnostics.single().message)
        assertEquals(SourceLocation(5, 6), failure.diagnostics.single().location)
    }

    @Test fun jsonMarkerRulesReachThePublicParserOnEveryPlatform() {
        for ((operator, arrow) in listOf("--o" to 3, "--x" to 4, "o--" to 5, "x--" to 6)) {
            val source = "usecase-beta\njson Payload@{}\nInspect $operator Payload"
            val raw = assertIs<UsecaseDiagram>(assertIs<MermaidParseResult.Success>(UsecaseParser(source).parse()).diagram)
            assertEquals(arrow, raw.relationships.single().arrowType, operator)
            assertEquals(UsecaseRelationshipType.ASSOCIATION, raw.relationships.single().type)
            assertEquals(listOf("Payload"), raw.jsonNodes.map { it.id })
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), operator)
        }
        for (operator in listOf("-->", "<--", "--")) {
            assertIs<MermaidParseResult.Success>(MermaidParser.parse("usecase-beta\njson Payload@{}\nInspect $operator Payload"))
        }
    }

    @Test fun unknownTargetsAndInvalidNotesClearPublishedState() {
        val doc = UsecaseDocument()
        for (body in listOf("A --> B\nclass edge_0 selected", "A --> B\nstyle missing stroke:red", "json Data@{}\nnote for Data \"invalid\"", "systemBoundary Auth\nend\nnote for Auth \"invalid\"", "A edge@--> B\nnote for edge \"invalid\"")) {
            assertIs<MermaidParseResult.Success>(doc.parse("usecase-beta\nA --> B"))
            assertIs<MermaidParseResult.Failure>(doc.parse("usecase-beta\n$body"), body)
            assertNull(doc.diagram); assertNull(doc.ast)
        }
        assertIs<MermaidParseResult.Success>(doc.parse("usecase-beta\nclass later selected\nA later@--> B"))
    }

    @Test fun animationChecksEachTypedOccurrenceAndRetainsSpeedPrecedence() {
        val doc = UsecaseDocument()
        for (body in listOf("link@{ animate: \"true\" }", "link@{ animation: medium }\nlink@{ animation: fast }", "link@{ animation: fast, animation: medium }")) {
            assertIs<MermaidParseResult.Failure>(doc.parse("usecase-beta\nA link@--> B\n$body"))
        }
        assertIs<MermaidParseResult.Success>(doc.parse("usecase-beta\nA link@--> B\nlink@{ animation: slow }\nlink@{ animate: false }"))
        val edge = doc.diagram!!.relationships.single()
        assertTrue(edge.animate); assertEquals("slow", edge.animation)
        val astEdge = (doc.ast!!.asMap()["edges"] as List<*>).single() as Map<*, *>
        assertEquals(true, (astEdge["attrs"] as Map<*, *>)["animate"])
    }

    @Test fun relationshipModelRetainsMarkerDirectionLengthAndMarkdownLabelType() {
        val doc = UsecaseDocument()
        assertIs<MermaidParseResult.Success>(doc.parse("usecase-beta\nA <-- B\nB -- \"`**label**`\" ---> C\nC --x D"))
        val edges = doc.diagram!!.relationships
        assertEquals(listOf(1, 0, 4), edges.map { it.arrowType })
        assertEquals(listOf(1, 2, 1), edges.map { it.minlen })
        assertEquals("markdown", edges[1].labelType)
        assertEquals("**label**", edges[1].label)
    }

    @Test fun jsonAndInternalNoteEdgesAreDetachedSerializableAstValues() {
        val doc = UsecaseDocument()
        assertIs<MermaidParseResult.Success>(doc.parse("usecase-beta\njson Data@{\"obj\":{\"2\":true,\"1\":null},\"items\":[false,3]}\nA long@----> Data\nnote for A \"remember\""))
        val ast = doc.ast!!.asMap()
        val data = ((ast["nodes"] as Map<*, *>)["Data"] as Map<*, *>)["attrs"] as Map<*, *>
        assertEquals(listOf("2", "1"), (data["propertyOrder"] as Map<*, *>)["/obj"])
        assertEquals(mapOf("2" to true, "1" to null), (data["value"] as Map<*, *>)["obj"])
        val edges = ast["edges"] as List<*>
        assertEquals(3, ((edges[0] as Map<*, *>)["attrs"] as Map<*, *>)["minlen"])
        assertEquals(true, ((edges[1] as Map<*, *>)["attrs"] as Map<*, *>)["internal"])
        (data["value"] as MutableMap<*, *>).clear()
        val freshData = (((doc.ast!!.asMap()["nodes"] as Map<*, *>)["Data"] as Map<*, *>)["attrs"] as Map<*, *>)
        assertTrue((freshData["value"] as Map<*, *>).isNotEmpty())
    }
}
