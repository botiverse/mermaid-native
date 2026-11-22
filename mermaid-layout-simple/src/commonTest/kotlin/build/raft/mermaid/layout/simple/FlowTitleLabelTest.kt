package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

/** Subgraph titles: their line breaks and markdown, as Mermaid draws them. */
class FlowTitleLabelTest {
    private val measure = TextMeasurer { text, _ -> SceneSize(UnicodeGraphemes.split(text).size * 10.0, 14.0) }
    private fun scene(title: String, other: String = ""): LayoutScene {
        val source = "flowchart TD\nsubgraph one [$title]\nA[a] --> B[b]\nend\n$other"
        return SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, measure, LayoutConfig())
    }
    private fun texts(scene: LayoutScene) = scene.commands.filterIsInstance<DrawText>().filter { it.text !in setOf("a", "b", "c") }
    private fun frame(scene: LayoutScene) = scene.commands.filterIsInstance<DrawRect>().first { it.cornerRadius == 4.0 }.rect
    private fun top(scene: LayoutScene) = scene.commands.filterIsInstance<DrawRect>().first { it.cornerRadius == 5.0 }.rect.y

    @Test fun aMarkdownTitleKeepsItsEmphasis() {
        val runs = texts(scene("\"`plain **bold** *slanted*`\""))
        assertEquals(listOf("plain", "bold", "slanted"), runs.map { it.text.trim() })
        assertTrue(runs[1].style.fontWeight >= 700)
        assertTrue(runs[2].style.italic)
        assertEquals(1, runs.map { it.origin.y }.distinct().size)
    }

    @Test fun lineBreaksStackAndTheFrameMakesRoomForThem() {
        val one = scene("\"one\"")
        val two = scene("\"one<br/>two\"")
        val lines = texts(two)
        assertEquals(listOf("one", "two"), lines.map { it.text })
        assertEquals(lines[0].origin.x, lines[1].origin.x)
        assertTrue(lines[1].origin.y > lines[0].origin.y)
        assertTrue(lines[1].origin.y < top(two), "the second line is above the first node")
        val added = lines[1].origin.y - lines[0].origin.y
        assertEquals(added, (top(two) - frame(two).y) - (top(one) - frame(one).y), 1e-6)
    }

    @Test fun theFrameIsWideEnoughForTheWidestLine() {
        val scene = scene("\"a title much wider than its nodes<br/>short\"")
        assertTrue(frame(scene).width >= 320.0)
    }

    @Test fun aPlainOneLineTitleIsDrawnAsBefore() {
        val title = texts(scene("\"Plain title\"")).single()
        assertEquals("Plain title", title.text)
        assertEquals(TextAnchor.MIDDLE, title.anchor)
    }
}
