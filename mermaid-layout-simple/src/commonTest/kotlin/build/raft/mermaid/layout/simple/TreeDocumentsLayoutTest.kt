package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class TreeDocumentsLayoutTest {
    private fun layout(diagram: MermaidDiagram): LayoutScene =
        SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())

    @Test fun editedMindmapExportsTheSameHierarchyShapesAndIconsAsParsedInput() {
        val document = MindmapDocument()
        document.addNode(0, "root", "Root", 2).icon = "star"
        document.addNode(4, "child", "Child", 6)
        document.addNode(8, "leaf", "Leaf", 3)
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "mindmap\n  root[Root]\n  ::icon(star)\n    child{{Child}}\n      leaf((Leaf))",
        )).diagram
        val scene = layout(document.diagram())
        assertEquals(layout(parsed), scene)
        assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text == "Leaf" })
        document.clear()
        document.addNode(0, "fresh", "Fresh", 0)
        val fresh = layout(document.diagram())
        assertTrue(fresh.commands.filterIsInstance<DrawText>().any { it.text == "Fresh" })
        assertFalse(fresh.commands.filterIsInstance<DrawText>().any { it.text == "Leaf" })
    }

    @Test fun editedFileTreeExportsParentsIconsAndDescriptionsToActualLayout() {
        val document = TreeViewDocument()
        document.addNode(0, "src", true, icon = "folder", description = "Sources")
        document.addNode(4, "main.kt", false, icon = "file")
        document.addNode(0, "README.md", false)
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "treeView-beta\nsrc/ icon(folder) ## Sources\n    main.kt icon(file)\nREADME.md",
        )).diagram
        val scene = layout(document.diagram())
        assertEquals(layout(parsed), scene)
        assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text == "Sources" })
    }
}
