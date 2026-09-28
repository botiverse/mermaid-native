package build.raft.mermaid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TreeViewValuesTest {
    @Test
    fun productionKeepsWhitespaceAndAnnotationValues() {
        val source = """
            treeView-beta
            'my folder/' :::my-class icon(folder) ##  Root description
                But  _  _ton💓.tsx icon(logos:react) ## entry point
                "" icon()
        """.trimIndent()
        val d = assertIs<TreeViewDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
        assertEquals(TreeViewNode("my folder", 0, null, true, null, "my-class", "folder", "Root description"), d.nodes[0])
        assertEquals(TreeViewNode("But  _  _ton💓.tsx", 1, 0, false, 4, null, "logos:react", "entry point"), d.nodes[1])
        assertEquals("", d.nodes[2].label)
        assertEquals("", d.nodes[2].iconAnnotation)
    }
}
