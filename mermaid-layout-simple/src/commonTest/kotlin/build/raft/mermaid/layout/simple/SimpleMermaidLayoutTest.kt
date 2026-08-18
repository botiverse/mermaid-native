package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.FlowDirection
import build.raft.mermaid.core.ClassDefinition
import build.raft.mermaid.core.ClassDiagram
import build.raft.mermaid.core.ClassMember
import build.raft.mermaid.core.ClassRelationship
import build.raft.mermaid.core.ClassRelationshipKind
import build.raft.mermaid.core.FlowEdge
import build.raft.mermaid.core.FlowNode
import build.raft.mermaid.core.FlowchartDiagram
import build.raft.mermaid.core.SequenceActor
import build.raft.mermaid.core.SequenceArrowHead
import build.raft.mermaid.core.SequenceDiagram
import build.raft.mermaid.core.SequenceLineStyle
import build.raft.mermaid.core.SequenceMessage
import build.raft.mermaid.layout.DrawLine
import build.raft.mermaid.layout.DrawPolyline
import build.raft.mermaid.layout.DrawRect
import build.raft.mermaid.layout.LayoutConfig
import build.raft.mermaid.layout.SceneRect
import build.raft.mermaid.layout.StrokePattern
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimpleMermaidLayoutTest {
    @Test
    fun classDiagramProducesDeterministicBoxAndRelationship() {
        val scene = SimpleMermaidLayout.layout(
            ClassDiagram(
                classes = listOf(
                    ClassDefinition("Animal", members = listOf(ClassMember("String name"))),
                    ClassDefinition("Duck"),
                ),
                relationships = listOf(ClassRelationship("Animal", "Duck", ClassRelationshipKind.INHERITANCE)),
            ),
            FixedWidthTextMeasurer,
            LayoutConfig(),
        )
        assertEquals(scene, SimpleMermaidLayout.layout(
            ClassDiagram(
                classes = listOf(ClassDefinition("Animal", members = listOf(ClassMember("String name"))), ClassDefinition("Duck")),
                relationships = listOf(ClassRelationship("Animal", "Duck", ClassRelationshipKind.INHERITANCE)),
            ), FixedWidthTextMeasurer, LayoutConfig(),
        ))
        assertEquals(2, scene.commands.filterIsInstance<DrawRect>().size)
        assertEquals(1, scene.commands.filterIsInstance<DrawLine>().size)
    }

    @Test
    fun flowchartDirectionsProduceDeterministicOrderedGeometry() {
        FlowDirection.entries.forEach { direction ->
            val diagram = FlowchartDiagram(
                direction = direction,
                nodes = listOf(FlowNode("A", "Start"), FlowNode("B", "Finish")),
                edges = listOf(FlowEdge("A", "B")),
            )
            val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
            val second = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
            assertEquals(first, second, direction.name)
            assertTrue(first.width > 0.0 && first.height > 0.0, direction.name)
            val nodeRects = first.commands.filterIsInstance<DrawRect>().map { it.rect }
            val edge = first.commands.filterIsInstance<DrawLine>().single()
            assertEquals(2, nodeRects.size, direction.name)
            when (direction) {
                FlowDirection.TD,
                FlowDirection.TB,
                -> {
                    assertTrue(nodeRects[0].y < nodeRects[1].y, direction.name)
                    assertTrue(edge.from.y < edge.to.y, direction.name)
                }
                FlowDirection.BT -> {
                    assertTrue(nodeRects[0].y > nodeRects[1].y, direction.name)
                    assertTrue(edge.from.y > edge.to.y, direction.name)
                }
                FlowDirection.LR -> {
                    assertTrue(nodeRects[0].x < nodeRects[1].x, direction.name)
                    assertTrue(edge.from.x < edge.to.x, direction.name)
                }
                FlowDirection.RL -> {
                    assertTrue(nodeRects[0].x > nodeRects[1].x, direction.name)
                    assertTrue(edge.from.x > edge.to.x, direction.name)
                }
            }
        }
    }

    @Test
    fun sequenceUsesDashedReturnAndSelfMessagePolyline() {
        val diagram = SequenceDiagram(
            actors = listOf(SequenceActor("A", "Alice"), SequenceActor("B", "Bob")),
            messages = listOf(
                message("A", "B", "request", SequenceLineStyle.SOLID),
                message("B", "A", "response", SequenceLineStyle.DASHED),
                message("A", "A", "self", SequenceLineStyle.SOLID),
            ),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        val messageLines = scene.commands.filterIsInstance<DrawLine>().drop(2)
        assertEquals(listOf(StrokePattern.SOLID, StrokePattern.DASHED), messageLines.map { it.pattern })
        assertEquals(1, scene.commands.filterIsInstance<DrawPolyline>().size)
        assertTrue(scene.commands.filterIsInstance<DrawRect>().all { it.rect.valid() })
    }

    private fun message(
        from: String,
        to: String,
        label: String,
        lineStyle: SequenceLineStyle,
    ): SequenceMessage = SequenceMessage(from, to, label, lineStyle, SequenceArrowHead.FILLED)

    private fun SceneRect.valid(): Boolean = width > 0.0 && height > 0.0
}
