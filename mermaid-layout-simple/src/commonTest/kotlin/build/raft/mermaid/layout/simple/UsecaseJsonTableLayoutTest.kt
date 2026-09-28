package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class UsecaseJsonTableLayoutTest {
    private val measurer = TextMeasurer { text, style -> SceneSize(text.length * style.fontSize * .55, style.fontSize) }
    private fun scene(source: String): LayoutScene = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, measurer, LayoutConfig())

    @Test fun productionJsonTableUsesOrderedValuesAndClipsEdgesToMeasuredTable() {
        val source = """usecase-beta
actor User
json Payload@{"2":"second","1":"first","colors":["Red","Green"],"items":[{"name":"A"},{}],"empty":{},"duplicate":"old","duplicate":"new"}
User --> Payload
"""
        val diagram = assertIs<UsecaseDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
        assertEquals(listOf(
            UsecaseJsonRow("2", "2", "second"), UsecaseJsonRow("1", "1", "first"),
            UsecaseJsonRow("colors", "colors", "Red"), UsecaseJsonRow("", "colors", "Green"),
            UsecaseJsonRow("items[0].name", "items[0].name", "A"), UsecaseJsonRow("items[1]", "items[1]", "{}"),
            UsecaseJsonRow("empty", "empty", "{}"), UsecaseJsonRow("duplicate", "duplicate", "new"),
        ), UsecaseJsonTable.rows(diagram.jsonNodes.single().data!!))
        val result = scene(source)
        val table = result.commands.filterIsInstance<DrawRect>().single().rect
        val texts = result.commands.filterIsInstance<DrawText>()
        assertTrue(texts.none { it.text == "old" || it.text.contains("\"duplicate\"") })
        val cells = texts.filter { it.text in listOf("second", "first", "Red", "Green", "A", "{}", "new") }
        assertEquals(listOf("second", "first", "Red", "Green", "A", "{}", "{}", "new"), cells.map { it.text })
        assertTrue(cells.zipWithNext().all { (a,b) -> b.origin.y > a.origin.y })
        assertTrue(cells.all { it.origin.x > table.x && it.origin.x < table.x + table.width && it.origin.y > table.y && it.origin.y < table.y + table.height })
        assertTrue(result.commands.filterIsInstance<DrawLine>().any { it.to.x == table.x && it.to.y == table.y + table.height / 2 })
        assertTrue(table.y >= 0 && table.y + table.height <= result.height)
    }

    @Test fun longMultilineAndMarkupCellsRemainTextWithinTableForAllDirections() {
        val long = "🚀".repeat(35)
        for (direction in listOf("LR", "RL", "TB", "BT")) {
            val result = scene("usecase-beta\ndirection $direction\nactor User\njson Data@{\"a/b~c\":{\"title\":\"<script>safe</script>\\n$long\"},\"flags\":[true,false,null,2],\"empty\":[]}\nUser --> Data")
            val table = result.commands.filterIsInstance<DrawRect>().single().rect
            val texts = result.commands.filterIsInstance<DrawText>().filter { it.text != "User" }
            assertTrue(texts.any { it.text == "a/b~c.title" })
            assertTrue(texts.any { it.text == "<script>safe</script>" })
            assertTrue(texts.any { it.text == "true" }); assertTrue(texts.any { it.text == "null" }); assertTrue(texts.any { it.text == "2" })
            for (text in texts) {
                assertFalse(text.text.firstOrNull()?.isLowSurrogate() == true)
                assertFalse(text.text.lastOrNull()?.isHighSurrogate() == true)
                assertTrue(text.origin.y > table.y && text.origin.y < table.y + table.height)
                if (text.anchor == TextAnchor.START) assertTrue(text.origin.x + measurer.measure(text.text, text.style).width <= table.x + table.width)
            }
            assertTrue(table.x >= 0 && table.x + table.width <= result.width)
            assertTrue(table.y >= 0 && table.y + table.height <= result.height)
        }
        assertTrue(scene("usecase-beta\njson Empty@{}").commands.filterIsInstance<DrawText>().any { it.text == "{}" })
    }
}
