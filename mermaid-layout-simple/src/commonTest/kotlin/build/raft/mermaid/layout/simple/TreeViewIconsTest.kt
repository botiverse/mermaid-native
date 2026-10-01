package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TreeViewIconsTest {
    private fun parse(source: String): TreeViewDiagram =
        assertIs<TreeViewDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
    private fun layout(diagram: TreeViewDiagram): LayoutScene =
        SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())

    @Test fun parsedBuiltinsBecomeGlyphsAndDescriptionsRemainReadable() {
        val scene = layout(parse("treeView-beta\n/\n  src/ icon(folder) ## Sources\n    main.kt icon(file)"))
        val labels = scene.commands.filterIsInstance<DrawText>()
        assertEquals(2, scene.commands.filterIsInstance<DrawPolygon>().size)
        assertTrue(labels.any { it.text == "Sources" })
        assertFalse(labels.any { it.text == "folder" || it.text == "file" || it.text.startsWith("mermaid-treeview:") })
        val glyphs = scene.commands.filterIsInstance<DrawPolygon>()
        for ((label, glyph) in listOf("src", "main.kt").zip(glyphs)) {
            val text = labels.single { it.text == label }
            assertTrue(text.origin.x > glyph.points.maxOf { it.x })
            assertTrue(text.origin.x + FixedWidthTextMeasurer.measure(text.text, text.style).width < scene.width)
        }
    }

    @Test fun filenameAndExtensionConfigurationReachActualRenderer() {
        val diagram = parse("treeView-beta\n/\n  src/\n    main.kt\n    notes.txt\n    Dockerfile").copy(
            iconConfig = TreeViewIconConfig(true, "custom", mapOf("Dockerfile" to "folder"), mapOf("kt" to "file", ".txt" to "none")),
        )
        val scene = layout(diagram)
        // Root, directory, Kotlin file, Dockerfile; notes.txt explicitly suppresses its icon.
        assertEquals(4, scene.commands.filterIsInstance<DrawPolygon>().size)
        assertFalse(scene.commands.filterIsInstance<DrawText>().any { it.text == "none" })
        assertEquals(5, scene.commands.filterIsInstance<DrawEllipse>().size) // connector dots survive
    }

    @Test fun explicitNoneOverridesAutomaticIconsAndRetainsConnectorAndDescription() {
        val diagram = parse("treeView-beta\n/ icon(none)\n  item icon(none) ## Keep me").copy(iconConfig = TreeViewIconConfig(showIcons = true))
        val scene = layout(diagram)
        assertTrue(scene.commands.filterIsInstance<DrawPolygon>().isEmpty())
        assertEquals(2, scene.commands.filterIsInstance<DrawEllipse>().size)
        assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text == "Keep me" })
    }

    @Test fun customReferencesRemainVisibleWithoutPretendingToLoadExternalPacks() {
        val diagram = parse("treeView-beta\n/\n  item icon(react) ## Component").copy(iconConfig = TreeViewIconConfig(defaultIconPack = "logos"))
        val scene = layout(diagram)
        assertTrue(scene.commands.filterIsInstance<DrawPolygon>().isEmpty())
        assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text == "logos:react · Component" })
    }
}
