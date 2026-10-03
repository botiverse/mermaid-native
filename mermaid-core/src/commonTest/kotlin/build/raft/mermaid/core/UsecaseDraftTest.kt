package build.raft.mermaid.core

import kotlin.test.*

class UsecaseDraftTest {
    @Test fun nestedDraftMutationsDoNotChangePublishedData() {
        val doc = UsecaseDocument(); val draft = doc.createModel()
        draft.actors!!["A"] = UsecaseActor("A", "Original")
        val classes = mutableListOf("important"); val styles = mutableMapOf("fill" to "red")
        draft.attributes!!["A"] = UsecaseAttributes(classes = classes, styles = styles)
        draft.classDefs!!["important"] = styles
        val json = mutableMapOf<String, UsecaseJsonValue>("a" to UsecaseJsonValue.StringValue("old"))
        val keys = mutableListOf("a")
        draft.jsonNodes!!["J"] = UsecaseJsonNode("J", "{}", UsecaseOrderedJsonObject(json, mapOf("" to keys)))
        val statements = mutableListOf<Any?>(mutableMapOf("kind" to "actor"))
        draft.ast = mapOf("source" to "original", "statements" to statements)
        draft.notes!!["N"] = UsecaseNote("A", "Original note", "N")
        draft.relationships!!.add(UsecaseRelationship("A", "A"))
        draft.accTitle = "Accessible"
        doc.commit(draft)
        draft.actors!!["A"] = UsecaseActor("A", "Changed"); draft.notes!!.clear(); draft.relationships!!.clear()
        classes.clear(); styles["fill"] = "blue"; json.clear(); keys.clear(); statements.clear()
        val published = assertNotNull(doc.diagram)
        assertEquals("Original", published.actors.single().label)
        assertEquals(1, published.notes.size); assertEquals(1, published.relationships.size)
        assertEquals(listOf("important"), published.attributes.getValue("A").classes)
        assertEquals("red", published.classDefs.getValue("important")["fill"])
        assertEquals(listOf("a"), published.jsonNodes.single().data!!.propertyOrder[""])
        assertEquals(UsecaseJsonValue.StringValue("old"), published.jsonNodes.single().data!!.value["a"])
        assertEquals(1, (doc.ast!!.asMap()["statements"] as List<*>).size)
        assertEquals("Accessible", doc.accessibilityTitle)
    }

    @Test fun everyIncompleteCollectionAndNegativeCounterPreservesPriorCommit() {
        val doc = UsecaseDocument(); val initial = doc.createModel()
        initial.actors!!["A"] = UsecaseActor("A", "A"); initial.ast = mapOf("source" to "A")
        doc.commit(initial)
        val invalid: List<(UsecaseDraft) -> Unit> = listOf(
            { it.actors = null }, { it.useCases = null }, { it.systemBoundaries = null },
            { it.relationships = null }, { it.notes = null }, { it.jsonNodes = null },
            { it.attributes = null }, { it.classDefs = null }, { it.symbols = null },
            { it.config = null }, { it.relationshipCounter = -1 }, { it.noteCounter = -1 },
        )
        invalid.forEach { invalidate ->
            val draft = doc.createModel(); invalidate(draft)
            assertEquals("Cannot commit an incomplete usecase model", assertFailsWith<IllegalArgumentException> { doc.commit(draft) }.message)
            assertEquals("A", doc.diagram!!.actors.single().id); assertEquals("A", doc.ast!!.asMap()["source"])
        }
        for (value in listOf(Any(), Double.NaN, mapOf(1 to "bad key"))) {
            val draft = doc.createModel(); draft.ast = mapOf("invalid" to value)
            assertFailsWith<IllegalArgumentException> { doc.commit(draft) }
            assertEquals("A", doc.diagram!!.actors.single().id)
        }
        val replacement = doc.createModel(); replacement.useCases!!["B"] = UsecaseNode("B", "B", UsecaseShape.RECTANGLE)
        doc.commit(replacement)
        assertTrue(doc.diagram!!.actors.isEmpty()); assertNull(doc.ast); assertEquals("B", doc.diagram!!.useCases.single().id)
    }

    @Test fun clearAndParseResetAllDocumentStateWithoutMutatingOldSnapshots() {
        val doc = UsecaseDocument(); val draft = doc.createModel()
        draft.direction = FlowDirection.RL; draft.noteCounter = 8; draft.relationshipCounter = 9
        draft.symbols!!["A"] = "actor"; draft.accTitle = "Title"; draft.accDescription = "Description"
        draft.actors!!["A"] = UsecaseActor("A", "A"); draft.ast = mapOf("source" to "A")
        doc.commit(draft); doc.title = "Diagram"; val old = doc.diagram
        doc.clear()
        assertNull(doc.diagram); assertNull(doc.ast); assertEquals(FlowDirection.LR, doc.direction)
        assertEquals("", doc.title); assertEquals("", doc.accessibilityTitle); assertEquals("", doc.accessibilityDescription)
        val reset = doc.createModel(); assertEquals(0, reset.noteCounter); assertEquals(0, reset.relationshipCounter)
        assertTrue(reset.symbols!!.isEmpty()); assertEquals(reset.config, doc.configuration())
        assertEquals("A", old!!.actors.single().id)
        doc.commit(draft); assertIs<MermaidParseResult.Failure>(doc.parse("usecase-beta\n@")); assertNull(doc.diagram)
        assertIs<MermaidParseResult.Success>(doc.parse("usecase-beta\nactor New")); assertEquals("New", doc.diagram!!.actors.single().id)
    }
}
