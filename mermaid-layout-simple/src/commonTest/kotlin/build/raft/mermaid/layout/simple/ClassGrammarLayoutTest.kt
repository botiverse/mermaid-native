package build.raft.mermaid.layout.simple
import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class ClassGrammarLayoutTest {
    private fun layout(text:String):LayoutScene = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse("classDiagram\n$text")).diagram,FixedWidthTextMeasurer,LayoutConfig())
    @Test fun namespacesContainChildrenAndDirectionMovesRealBoxes() {
        val scene=layout("direction LR\nnamespace Outer {\nnamespace Inner { class A }\nclass B\n}\nclass C\nA --> C")
        val groups=scene.commands.filterIsInstance<DrawRect>().filter { it.fill.value=="#f1f5f9" }
        assertEquals(2,groups.size)
        val outer=groups[0].rect;val inner=groups[1].rect
        assertTrue(inner.x>outer.x && inner.y>outer.y && inner.x+inner.width<outer.x+outer.width && inner.y+inner.height<outer.y+outer.height)
        val labels=scene.commands.filterIsInstance<DrawText>().associateBy { it.text }
        assertTrue(labels.getValue("C").origin.x>outer.x+outer.width)
        assertTrue(labels.getValue("B").origin.x>labels.getValue("A").origin.x)
    }
    @Test fun genericHeadersAnnotationsAndUnmarkedMembersReachDrawCommands() {
        val scene=layout("class Box~T~ <<interface>> {\nitems List~T~\n+get() T\n}")
        val text=scene.commands.filterIsInstance<DrawText>().map { it.text }
        assertEquals(listOf("«interface»","Box<T>","items List<T>","+get() : T"),text)
        assertEquals(2,scene.commands.filterIsInstance<DrawLine>().size)
    }
    @Test fun notesAreMeasuredAndActuallyDrawnInsideTheirNamespace() {
        val scene=layout("namespace Team {\nclass A\nnote for A \"long note text\\nsecond line\"\n}")
        val note=scene.commands.filterIsInstance<DrawRect>().single { it.fill.value=="#fff7d6" }.rect
        val group=scene.commands.filterIsInstance<DrawRect>().single { it.fill.value=="#f1f5f9" }.rect
        assertTrue(note.x>=group.x && note.y>=group.y && note.x+note.width<=group.x+group.width && note.y+note.height<=group.y+group.height)
        assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text=="second line" })
        assertTrue(scene.commands.filterIsInstance<DrawLine>().any { it.pattern==StrokePattern.DASHED })
    }
    @Test fun legacyManualNamespaceNameWithoutGroupMetadataStillRenders() {
        val diagram=ClassDiagram(listOf(ClassDefinition("A",namespaceName="Legacy")),emptyList())
        val scene=SimpleMermaidLayout.layout(diagram,FixedWidthTextMeasurer,LayoutConfig())
        assertEquals("A",scene.commands.filterIsInstance<DrawText>().single().text)
        assertEquals(1,scene.commands.filterIsInstance<DrawRect>().size)
    }
    @Test fun orphanManualNotesAndNamespaceParentsFallBackToRoot() {
        val diagram=ClassDiagram(
            listOf(ClassDefinition("A",namespaceName="Child")),emptyList(),
            notes=listOf(ClassNote("Orphan note",namespaceName="Missing")),
            namespaces=listOf(ClassNamespace("Child",parentId="Missing")),
        )
        val scene=SimpleMermaidLayout.layout(diagram,FixedWidthTextMeasurer,LayoutConfig())
        val labels=scene.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue(labels.containsAll(listOf("A","Child","Orphan note")))
        assertEquals(3,scene.commands.filterIsInstance<DrawRect>().size)
    }
    @Test fun rightHandInheritanceMarkerIsAtTheTarget() {
        val scene=layout("class A\nclass B\nA --|> B")
        val boxes=scene.commands.filterIsInstance<DrawRect>()
        val head=scene.commands.filterIsInstance<DrawPolyline>().single()
        assertTrue(head.points.any { it.y==boxes.last().rect.y })
        assertTrue(head.points.all { it.y<=boxes.last().rect.y })
    }
}
