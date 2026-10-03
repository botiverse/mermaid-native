package build.raft.mermaid.core

import kotlin.test.*

class EntityDocumentTest {
    @Test fun firstGroupOwnsMembersAndNestedParentsReachBothProjections() {
        val d = EntityRelationshipDocument()
        d.addSubgraph("outer", listOf("inner", "A", "A"), "Outer")
        d.addSubgraph("inner", listOf("B", "A"), "Inner")
        d.addEntity("A"); d.addEntity("B")
        val data = d.data()
        assertEquals(listOf("B"), d.subgraphs().last().nodeIds)
        assertEquals("outer", data.nodes.single { it.id == "inner" }.parentId)
        assertEquals("inner", data.nodes.single { it.label == "B" }.parentId)
        assertEquals(listOf("outer"), d.diagram().rootNodeIds)
        d.addRelationship("outer", "contains", "B", EntityCardinality.ONLY_ONE, EntityCardinality.ZERO_OR_MORE)
        assertEquals("outer", d.data().edges.single().start)
        assertEquals(d.entity("B")!!.id, d.data().edges.single().end)
        assertEquals("B", d.diagram().relationships.single().to)
    }
    @Test fun mutationsAndClearCannotChangePublishedSnapshots() {
        val d = EntityRelationshipDocument(); d.addSubgraph("g", listOf("A"), "Group"); d.addEntity("A")
        d.addStyles(listOf("g"), listOf("fill:red")); d.addClasses(listOf("g"), listOf("tag"))
        val data = d.data(); val diagram = d.diagram()
        d.addStyles(listOf("g"), listOf("stroke:blue")); d.clear()
        assertEquals(listOf("fill:red"), diagram.subgraphs.single().styles)
        assertEquals(listOf("tag"), data.nodes.single { it.isGroup }.classes)
        assertTrue(d.data().nodes.isEmpty()); assertTrue(d.diagram().relationships.isEmpty())
        assertEquals("entity-A-0", d.addEntity("A").id)
    }
    @Test fun cyclicAndDuplicateGroupsCannotEnterRenderer() {
        val d = EntityRelationshipDocument(); d.addSubgraph("a", listOf("b"), "A")
        assertFailsWith<IllegalArgumentException> { d.addSubgraph("a", emptyList(), "Again") }
        d.addSubgraph("b", listOf("a"), "B")
        assertFailsWith<IllegalArgumentException> { d.diagram() }
        assertFailsWith<IllegalArgumentException> { d.data() }
    }
}
