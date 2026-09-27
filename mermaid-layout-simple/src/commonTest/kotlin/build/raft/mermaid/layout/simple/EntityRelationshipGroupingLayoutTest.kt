package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.MermaidParser
import build.raft.mermaid.core.MermaidParseResult
import build.raft.mermaid.layout.*
import kotlin.test.*

class EntityRelationshipGroupingLayoutTest {
    private fun scene(body: String): LayoutScene = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse("erDiagram\n$body")).diagram, FixedWidthTextMeasurer, LayoutConfig())
    private fun box(scene: LayoutScene, label: String): SceneRect {
        val text = scene.commands.filterIsInstance<DrawText>().single { it.text == label }
        return scene.commands.filterIsInstance<DrawRect>().map { it.rect }.filter {
            text.origin.x >= it.x && text.origin.x <= it.x + it.width && text.origin.y >= it.y && text.origin.y <= it.y + it.height
        }.minBy { it.width * it.height }
    }
    private fun contains(outer: SceneRect, inner: SceneRect) = inner.x > outer.x && inner.y > outer.y && inner.x + inner.width < outer.x + outer.width && inner.y + inner.height < outer.y + outer.height

    @Test fun rootDirectionsChangeRealTablePositionsAndLinkEndpoints() {
        for (direction in listOf("TB", "BT", "LR", "RL")) {
            val scene = scene("direction $direction\nA ||--o{ B : relates")
            val a = box(scene, "A"); val b = box(scene, "B")
            when (direction) {
                "TB" -> assertTrue(a.y + a.height < b.y)
                "BT" -> assertTrue(b.y + b.height < a.y)
                "LR" -> assertTrue(a.x + a.width < b.x)
                "RL" -> assertTrue(b.x + b.width < a.x)
            }
            val link = scene.commands.filterIsInstance<DrawLine>().first()
            if (direction == "LR" || direction == "RL") assertEquals(link.from.y, link.to.y)
            else assertEquals(link.from.x, link.to.x)
        }
    }

    @Test fun nestedGroupsContainChildrenWithIndependentDirections() {
        val scene = scene("direction LR\nsubgraph Outer\nA\nsubgraph Inner\ndirection BT\nB\nC\nend\nend\nD")
        val outer = box(scene, "Outer"); val inner = box(scene, "Inner")
        val a = box(scene, "A"); val b = box(scene, "B"); val c = box(scene, "C"); val d = box(scene, "D")
        assertTrue(contains(outer, inner)); assertTrue(contains(outer, a))
        assertTrue(contains(inner, b)); assertTrue(contains(inner, c))
        assertTrue(a.x + a.width < inner.x)
        assertTrue(c.y + c.height < b.y)
        assertTrue(outer.x + outer.width < d.x)
        assertEquals(6, scene.commands.filterIsInstance<DrawRect>().size)
    }

    @Test fun groupLinksUseContainerBoundsAndStayBehindTables() {
        val scene = scene("direction LR\nsubgraph G1\nA\nend\nsubgraph G2\nB\nend\nG1 ||--|| G2 : connects")
        val first = box(scene, "G1"); val second = box(scene, "G2")
        val link = scene.commands.filterIsInstance<DrawLine>().first()
        assertTrue(link.from.x > first.x + first.width)
        assertTrue(link.to.x < second.x)
        val table = scene.commands.filterIsInstance<DrawRect>().first { it.rect == box(scene, "A") }
        assertTrue(scene.commands.indexOf(link) < scene.commands.indexOf(table))
        assertEquals(4, scene.commands.filterIsInstance<DrawRect>().size)
    }

    @Test fun selfRelationRoutesOutsideItsTableAndFitsScene() {
        val scene = scene("A ||--o{ A : contains")
        val table = box(scene, "A")
        val loop = scene.commands.filterIsInstance<DrawPolyline>().single()
        assertTrue(loop.points.all { it.x > table.x + table.width })
        assertTrue(loop.points.all { it.x < scene.width && it.y < scene.height })
    }

    @Test fun selfRelationKeepsSpaceBeforeTheNextTable() {
        val scene = scene("direction LR\nA ||--o{ A : self\nA ||--|| B : next")
        val b = box(scene, "B")
        val loop = scene.commands.filterIsInstance<DrawPolyline>().single()
        val label = scene.commands.filterIsInstance<DrawText>().single { it.text == "self" }
        assertTrue(loop.points.all { it.x < b.x })
        assertTrue(label.origin.x + FixedWidthTextMeasurer.measure(label.text, label.style).width < b.x)
    }

    @Test fun emptyGroupIsVisibleAndItsTitleFits() {
        val scene = scene("subgraph Empty [A long empty group title]\nend")
        val rect = box(scene, "A long empty group title")
        assertTrue(rect.width > 160.0 && rect.height > 48.0)
    }
}
