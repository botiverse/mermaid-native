package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.FlowDirection
import build.raft.mermaid.core.ClassDefinition
import build.raft.mermaid.core.ClassDiagram
import build.raft.mermaid.core.ClassMember
import build.raft.mermaid.core.ClassRelationship
import build.raft.mermaid.core.ClassRelationshipKind
import build.raft.mermaid.core.EntityAttribute
import build.raft.mermaid.core.EntityCardinality
import build.raft.mermaid.core.EntityDefinition
import build.raft.mermaid.core.EntityKey
import build.raft.mermaid.core.EntityRelationship
import build.raft.mermaid.core.EntityRelationshipDiagram
import build.raft.mermaid.core.FlowEdge
import build.raft.mermaid.core.FlowNode
import build.raft.mermaid.core.FlowchartDiagram
import build.raft.mermaid.core.SequenceActor
import build.raft.mermaid.core.SequenceArrowHead
import build.raft.mermaid.core.SequenceDiagram
import build.raft.mermaid.core.SequenceLineStyle
import build.raft.mermaid.core.SequenceMessage
import build.raft.mermaid.core.PieDiagram
import build.raft.mermaid.core.PieSection
import build.raft.mermaid.layout.DrawPolygon
import build.raft.mermaid.layout.DrawText
import build.raft.mermaid.core.StateDiagram
import build.raft.mermaid.core.StateNode
import build.raft.mermaid.core.StateNodeKind
import build.raft.mermaid.core.StateTransition
import build.raft.mermaid.core.XyAxis
import build.raft.mermaid.core.XyChartDiagram
import build.raft.mermaid.core.XySeries
import build.raft.mermaid.core.XySeriesKind
import build.raft.mermaid.core.GanttDiagram
import build.raft.mermaid.core.GanttSection
import build.raft.mermaid.core.GanttTask
import build.raft.mermaid.core.GanttTaskStatus
import build.raft.mermaid.core.NumericAxis
import build.raft.mermaid.core.MindmapDiagram
import build.raft.mermaid.core.MindmapNode
import build.raft.mermaid.core.MindmapNodeShape
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
    fun xyChartProducesDeterministicAxesBarsAndLine() {
        val diagram = XyChartDiagram(
            title = "Sales",
            xAxis = XyAxis("Quarter", listOf("Q1", "Q2", "Q3")),
            yAxis = NumericAxis("Revenue", 0.0, 100.0),
            series = listOf(
                XySeries(XySeriesKind.BAR, listOf(20.0, 50.0, 80.0)),
                XySeries(XySeriesKind.LINE, listOf(25.0, 45.0, 90.0)),
            ),
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(3, first.commands.filterIsInstance<DrawRect>().size)
        assertEquals(1, first.commands.filterIsInstance<DrawPolyline>().size)
        assertTrue(first.commands.filterIsInstance<DrawText>().any { it.text == "Q2" })
    }

    @Test
    fun entityRelationshipDiagramRendersCardinalityAndAttributesDeterministically() {
        val diagram = EntityRelationshipDiagram(
            entities = listOf(
                EntityDefinition("CUSTOMER", listOf(EntityAttribute("int", "id", EntityKey.PK))),
                EntityDefinition("ORDER", listOf(EntityAttribute("int", "customerId", EntityKey.FK))),
            ),
            relationships = listOf(
                EntityRelationship(
                    "CUSTOMER",
                    "ORDER",
                    EntityCardinality.ONLY_ONE,
                    EntityCardinality.ZERO_OR_MORE,
                    "places",
                ),
            ),
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(2, first.commands.filterIsInstance<DrawRect>().size)
        assertEquals(1, first.commands.filterIsInstance<DrawLine>().size)
        assertTrue(first.commands.filterIsInstance<DrawText>().any { it.text == "0..*" })
        assertTrue(first.commands.filterIsInstance<DrawText>().any { it.text == "int id PK" })
    }

    @Test
    fun classDiagramProducesDeterministicBoxAndRelationship() {
        val diagram = ClassDiagram(
            classes = listOf(
                ClassDefinition("Animal", members = listOf(ClassMember("String name"))),
                ClassDefinition("Duck"),
            ),
            relationships = listOf(ClassRelationship("Animal", "Duck", ClassRelationshipKind.INHERITANCE)),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(2, scene.commands.filterIsInstance<DrawRect>().size)
        assertEquals(1, scene.commands.filterIsInstance<DrawLine>().size)
    }

    @Test
    fun pieProducesDeterministicSlicesAndShowDataLegend() {
        val diagram = PieDiagram(title = "Pets", showData = true, sections = listOf(PieSection("Dogs", 3.0), PieSection("Cats", 1.0)))
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(2, first.commands.filterIsInstance<DrawPolygon>().size)
        assertTrue(first.commands.filterIsInstance<DrawText>().any { it.text == "Dogs: 3.0" })
    }

    @Test
    fun stateDiagramRendersTerminalStatesAndTransitionLabelsDeterministically() {
        val diagram = StateDiagram(
            direction = FlowDirection.LR,
            states = listOf(
                StateNode("__start_0", "", StateNodeKind.START),
                StateNode("Idle", "Idle"),
                StateNode("Working", "Processing request"),
                StateNode("__end_1", "", StateNodeKind.END),
            ),
            transitions = listOf(
                StateTransition("__start_0", "Idle"),
                StateTransition("Idle", "Working", "start"),
                StateTransition("Working", "__end_1", "finish"),
            ),
        )

        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        val second = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())

        assertEquals(first, second)
        val stateRects = first.commands.filterIsInstance<DrawRect>()
        assertEquals(5, stateRects.size)
        assertEquals(3, first.commands.filterIsInstance<DrawLine>().size)
        assertEquals("#334155", stateRects.first().fill.value)
        assertEquals("#334155", stateRects.last().fill.value)
        assertTrue(stateRects.first().cornerRadius > 0.0)
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

    @Test
    fun mindmapProducesDeterministicTreeGeometryAndShapeTreatment() {
        val diagram = MindmapDiagram(
            listOf(
                MindmapNode("root", "Mindmap", null, 0, MindmapNodeShape.DOUBLE_CIRCLE),
                MindmapNode("a", "Origins", "root", 1),
                MindmapNode("b", "History", "a", 2, MindmapNodeShape.RECTANGLE),
                MindmapNode("c", "Research", "root", 1),
            ),
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(5, first.commands.filterIsInstance<DrawRect>().size)
        assertEquals(3, first.commands.filterIsInstance<DrawLine>().size)
        assertEquals(4, first.commands.filterIsInstance<DrawText>().size)
        assertTrue(first.commands.filterIsInstance<DrawText>().any { it.text == "Mindmap" })
        assertTrue(first.width > 0.0 && first.height > 0.0)
    }

    @Test
    fun ganttProducesDeterministicTimelineBars() {
        val diagram = GanttDiagram("Plan", "YYYY-MM-DD", listOf(GanttSection("Build", listOf(
            GanttTask("Parser", "parse", 100, 2, GanttTaskStatus.DONE),
            GanttTask("Renderer", "render", 102, 3, GanttTaskStatus.ACTIVE),
        ))))
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val bars = first.commands.filterIsInstance<DrawRect>()
        assertEquals(listOf(56.0, 84.0), bars.map { it.rect.width })
        assertEquals(listOf("#16a34a", "#2563eb"), bars.map { it.fill.value })
    }

    private fun message(
        from: String,
        to: String,
        label: String,
        lineStyle: SequenceLineStyle,
    ): SequenceMessage = SequenceMessage(from, to, label, lineStyle, SequenceArrowHead.FILLED)

    private fun SceneRect.valid(): Boolean = width > 0.0 && height > 0.0
}
