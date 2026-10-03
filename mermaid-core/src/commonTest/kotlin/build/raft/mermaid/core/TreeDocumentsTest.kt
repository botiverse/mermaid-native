package build.raft.mermaid.core

import kotlin.test.*

class TreeDocumentsTest {
    @Test fun treeClearResetsIdentityAndMetadataWithoutChangingOldSnapshots() {
        val doc = TreeViewDocument()
        doc.title = "Files"; doc.accessibilityTitle = "Project"; doc.accessibilityDescription = "Description"
        assertEquals(1, doc.addNode(0, "src", true))
        assertEquals(2, doc.addNode(4, "A.kt", false, "highlight", "logos:kotlin", "entry"))
        val diagram = doc.diagram(); val oldRoot = doc.rootSnapshot()
        assertEquals(0, diagram.nodes[1].parentIndex); assertEquals(1, diagram.nodes[1].depth)
        assertEquals("highlight", diagram.nodes[1].classAnnotation)
        doc.clear()
        assertEquals(1, doc.count); assertEquals("", doc.title); assertEquals("", doc.accessibilityTitle); assertEquals("", doc.accessibilityDescription)
        assertTrue(doc.diagram().nodes.isEmpty()); assertEquals(2, diagram.nodes.size)
        assertEquals(1, (oldRoot["children"] as List<*>).size)
        assertEquals(1, doc.addNode(0, "README.md", false))
    }

    @Test fun mindmapKeepsMutablePropertiesAndAssignsInheritedWrappingSections() {
        val doc = MindmapDocument()
        val root = doc.addNode(4, "root", "Root", 2)
        root.width = 150.0; root.height = 75.0; root.padding = 15.0; root.cssClass = "custom"; root.icon = "star"
        repeat(12) { i -> doc.addNode(8, "child$i", "Child$i", 0); doc.addNode(12, "leaf$i", "Leaf$i", 0) }
        val data = doc.layoutData(); val rows = data["nodes"] as List<*>
        val rootRow = rows[0] as Map<*, *>
        assertEquals(150.0, rootRow["width"]); assertEquals(75.0, rootRow["height"]); assertEquals(15.0, rootRow["padding"])
        assertEquals("mindmap-node section-root section--1 custom", rootRow["cssClasses"])
        assertFalse(rootRow.containsKey("section")); assertEquals("star", rootRow["icon"])
        assertEquals(0, (rows[23] as Map<*, *>)["section"]); assertEquals(0, (rows[24] as Map<*, *>)["section"])
        val diagram = doc.diagram(); assertEquals("23", diagram.nodes[24].parentId); assertEquals(2, diagram.nodes[24].depth)
        assertEquals(MindmapNodeShape.RECTANGLE, diagram.nodes.first().shape)
        doc.clear(); assertNull(doc.root); assertTrue((doc.layoutData()["nodes"] as List<*>).isEmpty())
        assertEquals(25, rows.size); assertEquals(0, doc.addNode(9, "next", "Next", 0).id)
    }

    @Test fun invalidStructureDoesNotCorruptTheExistingDocument() {
        val doc = MindmapDocument(); doc.addNode(0, "a", "A", 0)
        assertFailsWith<IllegalArgumentException> { doc.addNode(0, "b", "B", 0) }
        assertEquals(1, doc.diagram().nodes.size); assertEquals(1, doc.addNode(1, "c", "C", 0).id)
        val tree = TreeViewDocument(); assertFailsWith<IllegalArgumentException> { tree.addNode(-1, "bad", false) }
        assertEquals(1, tree.count)
    }
}
