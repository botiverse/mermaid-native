package build.raft.mermaid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class TreemapHierarchyTest {
    @Test fun parserAndProgrammaticRowsUseTheSameHierarchyIncludingClassesAndTabs() {
        val source = "treemap\n\"Root\":::root\n\t\"Group\"\n\t  \"Leaf\": 5:::hot\n\"Empty\""
        val parsed = assertIs<TreemapDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
        val hierarchy = TreemapHierarchy()
        listOf(
            TreemapHierarchyItem(0, "Root", classSelector = "root"),
            TreemapHierarchyItem(4, "Group"),
            TreemapHierarchyItem(6, "Leaf", 5.0, "hot"),
            TreemapHierarchyItem(0, "Empty"),
        ).forEach(hierarchy::add)
        assertEquals(parsed.roots, hierarchy.build())
    }

    @Test fun failedAdditionDoesNotMutateBuilderAndValidSiblingCanFollow() {
        val hierarchy = TreemapHierarchy()
        hierarchy.add(TreemapHierarchyItem(0, "Root"))
        hierarchy.add(TreemapHierarchyItem(2, "Leaf", 1.0))
        val before = hierarchy.build()
        assertFailsWith<IllegalArgumentException> { hierarchy.add(TreemapHierarchyItem(4, "Invalid")) }
        assertEquals(before, hierarchy.build())
        hierarchy.add(TreemapHierarchyItem(2, "Sibling", 2.0))
        assertEquals(listOf("Leaf", "Sibling"), hierarchy.build().single().children.map { it.label })
        assertEquals(listOf("Leaf"), before.single().children.map { it.label })
    }

    @Test fun negativeIndentationCannotResetExistingHierarchy() {
        val hierarchy = TreemapHierarchy()
        hierarchy.add(TreemapHierarchyItem(0, "Root"))
        val before = hierarchy.build()
        assertFailsWith<IllegalArgumentException> { hierarchy.add(TreemapHierarchyItem(-1, "Invalid")) }
        assertEquals(before, hierarchy.build())
    }

    @Test fun parserReportsLeafNestingBeforeLaterSyntaxErrorsAtOriginalLine() {
        val source = "treemap\n%% comment\n\"Root\"\n  \"Leaf\": 2\n    \"Child\": 1\nnot a row"
        val failure = assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source))
        assertEquals("Treemap leaves cannot have children", failure.diagnostics.single().message)
        assertEquals(SourceLocation(5, 1), failure.diagnostics.single().location)
    }

    @Test fun parserRetainsFiniteTotalValidationAfterBuilding() {
        val source = "treemap\n\"Root\"\n  \"A\": 1e308\n  \"B\": 1e308"
        val failure = assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source))
        assertEquals("Treemap weights must have a finite sum", failure.diagnostics.single().message)
        assertEquals(SourceLocation(1, 1), failure.diagnostics.single().location)
    }
}
