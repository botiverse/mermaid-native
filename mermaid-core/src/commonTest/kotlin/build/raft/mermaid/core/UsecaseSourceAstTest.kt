package build.raft.mermaid.core

import kotlin.test.*

class UsecaseSourceAstTest {
    @Test fun recordsHeaderFinalStatementAndUtf16JsonOccurrenceWithoutSearchingNames() {
        val source = "usecase-beta\r\n%% 😀 prior comment\r\njson son @{\"a\": 1}"
        val doc = UsecaseDocument()
        assertIs<MermaidParseResult.Success>(doc.parse(source))
        val ast = assertNotNull(doc.ast).asMap()
        assertEquals(source, ast["source"])
        assertEquals(listOf(0, 12), (ast["header"] as Map<*, *>)["span"])
        val statements = ast["statements"] as List<*>
        val last = statements.last() as Map<*, *>
        val idStart = source.indexOf("json son") + 5
        val node = (last["nodes"] as List<*>).single() as Map<*, *>
        assertEquals(listOf(idStart, idStart + 3), node["idSpan"])
        assertEquals(listOf(source.indexOf("json son"), source.length), last["span"])
        assertEquals("son", node["id"])
    }

    @Test fun nestedStatementsKeepDeclarationAndMetadataOccurrences() {
        val source = "usecase-beta\nsystemBoundary Auth\nactor User@{ type: hollow }\nLogin[Sign in]\nend"
        val doc = UsecaseDocument()
        assertIs<MermaidParseResult.Success>(doc.parse(source))
        val group = ((doc.ast!!.asMap()["statements"] as List<*>).single() as Map<*, *>)
        assertEquals("group", group["kind"])
        assertEquals(listOf(source.indexOf("systemBoundary"), source.length), group["span"])
        val children = group["children"] as List<*>
        assertEquals(2, children.size)
        val actor = (((children[0] as Map<*, *>)["nodes"] as List<*>).single() as Map<*, *>)
        val metadata = (actor["metadata"] as List<*>).single() as Map<*, *>
        assertEquals(listOf(source.indexOf("type"), source.indexOf("type") + 4), metadata["keySpan"])
        assertEquals(UsecaseActorType.HOLLOW, doc.diagram!!.actors.single().type)
    }

    @Test fun failureAndClearDiscardPublishedStateAndNextParseStartsFresh() {
        val doc = UsecaseDocument()
        assertIs<MermaidParseResult.Success>(doc.parse("usecase-beta\nactor User\naccTitle: First\nUser --> Login"))
        assertEquals("First", doc.accessibilityTitle)
        assertIs<MermaidParseResult.Failure>(doc.parse("usecase-beta\nactor Draft\nnote for Missing \"bad\""))
        assertNull(doc.diagram); assertNull(doc.ast)
        assertEquals("", doc.accessibilityTitle); assertEquals(FlowDirection.LR, doc.direction)
        assertIs<MermaidParseResult.Success>(doc.parse("usecase-beta\nactor Second"))
        assertEquals(listOf("Second"), doc.diagram!!.actors.map { it.id })
        doc.clear(); assertNull(doc.ast); assertNull(doc.diagram)
    }

    @Test fun exportedAstIsDetachedAndRendererModelsStayEqual() {
        val source = "usecase-beta\nactor User"
        val doc = UsecaseDocument()
        val plain = assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram
        assertIs<MermaidParseResult.Success>(doc.parse(source)); assertEquals<MermaidDiagram>(plain, assertNotNull(doc.diagram))
        val first = doc.ast!!.asMap()
        @Suppress("UNCHECKED_CAST")
        val header = first["header"] as MutableMap<String, Any?>
        header["direction"] = "broken"
        assertEquals("LR", (doc.ast!!.asMap()["header"] as Map<*, *>)["direction"])
    }
}
