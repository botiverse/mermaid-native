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
import build.raft.mermaid.core.RequirementDefinition
import build.raft.mermaid.core.RequirementDiagram
import build.raft.mermaid.core.RequirementElement
import build.raft.mermaid.core.RequirementRelationship
import build.raft.mermaid.core.RequirementRelationshipKind
import build.raft.mermaid.core.RequirementRisk
import build.raft.mermaid.core.RequirementVerifyMethod
import build.raft.mermaid.core.NumericAxis
import build.raft.mermaid.core.MindmapDiagram
import build.raft.mermaid.core.MindmapNode
import build.raft.mermaid.core.MindmapNodeShape
import build.raft.mermaid.core.TimelineDiagram
import build.raft.mermaid.core.TimelineEvent
import build.raft.mermaid.core.QuadrantAxis
import build.raft.mermaid.core.QuadrantChartDiagram
import build.raft.mermaid.core.QuadrantPoint
import build.raft.mermaid.core.UserJourneyDiagram
import build.raft.mermaid.core.UserJourneySection
import build.raft.mermaid.core.UserJourneyTask
import build.raft.mermaid.core.GitGraphBranch
import build.raft.mermaid.core.GitGraphCommit
import build.raft.mermaid.core.GitGraphCommitType
import build.raft.mermaid.core.GitGraphDiagram
import build.raft.mermaid.core.KanbanCard
import build.raft.mermaid.core.KanbanColumn
import build.raft.mermaid.core.KanbanDiagram
import build.raft.mermaid.core.PacketDiagram
import build.raft.mermaid.core.PacketField
import build.raft.mermaid.core.BlockDiagram
import build.raft.mermaid.core.BlockNode
import build.raft.mermaid.core.BlockEdge
import build.raft.mermaid.core.SankeyDiagram
import build.raft.mermaid.core.SankeyNode
import build.raft.mermaid.core.SankeyLink
import build.raft.mermaid.core.TreemapDiagram
import build.raft.mermaid.core.TreemapNode
import build.raft.mermaid.core.VennDiagram
import build.raft.mermaid.core.VennSet
import build.raft.mermaid.core.VennUnion
import build.raft.mermaid.core.UsecaseDiagram
import build.raft.mermaid.core.UsecaseActor
import build.raft.mermaid.core.UsecaseNode
import build.raft.mermaid.core.UsecaseShape
import build.raft.mermaid.core.UsecaseRelationship
import build.raft.mermaid.core.ArchitectureDiagram
import build.raft.mermaid.core.ArchitectureGroup
import build.raft.mermaid.core.ArchitectureService
import build.raft.mermaid.core.ArchitectureEdge
import build.raft.mermaid.core.ArchitecturePort
import build.raft.mermaid.core.C4Diagram
import build.raft.mermaid.core.C4Element
import build.raft.mermaid.core.C4ElementKind
import build.raft.mermaid.core.C4Relationship
import build.raft.mermaid.core.SwimlaneDiagram
import build.raft.mermaid.core.Swimlane
import build.raft.mermaid.core.SwimlaneNode
import build.raft.mermaid.core.SwimlaneNodeShape
import build.raft.mermaid.core.SwimlaneEdge
import build.raft.mermaid.layout.DrawLine
import build.raft.mermaid.layout.DrawPolyline
import build.raft.mermaid.layout.DrawRect
import build.raft.mermaid.layout.DrawEllipse
import build.raft.mermaid.layout.LayoutConfig
import build.raft.mermaid.layout.SceneRect
import build.raft.mermaid.layout.StrokePattern
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimpleMermaidLayoutTest {
    @Test
    fun swimlanesProduceDeterministicMeasuredLanesNodesAndEdges() {
        val long = "handoff-".repeat(20)
        val diagram = SwimlaneDiagram(
            FlowDirection.LR,
            listOf(
                Swimlane("customer", "Customer", listOf(SwimlaneNode("request", long, SwimlaneNodeShape.RECTANGLE))),
                Swimlane("support", "Support", listOf(SwimlaneNode("triage", "Triage", SwimlaneNodeShape.DECISION))),
            ),
            listOf(SwimlaneEdge("request", "triage", "handoff & review")),
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        val second = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, second)
        assertTrue(first.commands.filterIsInstance<DrawRect>().size >= 3)
        assertEquals(1, first.commands.filterIsInstance<DrawPolygon>().count { it.points.size == 4 })
        assertTrue(first.commands.filterIsInstance<DrawText>().any { it.text == long })
        val required = FixedWidthTextMeasurer.measure(long, build.raft.mermaid.layout.TextStyle(fontSize = 13.0, fontWeight = 600)).width + 40.0
        val nodeRect = first.commands.filterIsInstance<DrawRect>().first { it.rect.width >= required }
        assertTrue(nodeRect.rect.width >= required)
    }

    @Test
    fun requirementProducesDeterministicCardsAndRelationship() {
        val diagram = RequirementDiagram(
            requirements = listOf(RequirementDefinition("secure_login", "AUTH-1", "Users authenticate securely", RequirementRisk.HIGH, RequirementVerifyMethod.TEST)),
            elements = listOf(RequirementElement("mobile_client", "application", "docs/auth.md")),
            relationships = listOf(RequirementRelationship("mobile_client", "secure_login", RequirementRelationshipKind.SATISFIES)),
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        val second = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, second)
        assertEquals(2, first.commands.filterIsInstance<DrawRect>().size)
        assertTrue(first.commands.filterIsInstance<DrawText>().any { it.text == "satisfies" })
        assertTrue(first.commands.filterIsInstance<DrawText>().any { it.text == "id: AUTH-1" })
    }

    @Test
    fun requirementCardsMeasureLongHeadingsWithTheRenderedStyle() {
        val requirementName = "r".repeat(100)
        val elementName = "e".repeat(100)
        val diagram = RequirementDiagram(
            requirements = listOf(RequirementDefinition(requirementName, "REQ-1", "Text", RequirementRisk.LOW, RequirementVerifyMethod.TEST)),
            elements = listOf(RequirementElement(elementName, "application", "docs/example.md")),
            relationships = emptyList(),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        val heading = build.raft.mermaid.layout.TextStyle(fontSize = 14.0, fontWeight = 600)
        val requiredWidth = maxOf(
            FixedWidthTextMeasurer.measure("requirement $requirementName", heading).width,
            FixedWidthTextMeasurer.measure("element $elementName", heading).width,
        ) + 24.0
        assertTrue(scene.commands.filterIsInstance<DrawRect>().all { it.rect.width >= requiredWidth })
    }

    @Test
    fun sameColumnRequirementRelationshipsRouteOutsideCardsInBothDirections() {
        val diagram = RequirementDiagram(
            requirements = listOf(
                RequirementDefinition("r1", "REQ-1", "First", RequirementRisk.LOW, RequirementVerifyMethod.TEST),
                RequirementDefinition("r2", "REQ-2", "Second", RequirementRisk.MEDIUM, RequirementVerifyMethod.INSPECTION),
            ),
            elements = listOf(
                RequirementElement("e1", "application", "docs/one.md"),
                RequirementElement("e2", "service", "docs/two.md"),
            ),
            relationships = listOf(
                RequirementRelationship("r1", "r2", RequirementRelationshipKind.SATISFIES),
                RequirementRelationship("e2", "e1", RequirementRelationshipKind.VERIFIES),
            ),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        val routes = scene.commands.filterIsInstance<DrawPolyline>()
        assertEquals(2, routes.size)
        routes.forEach { route ->
            assertEquals(4, route.points.size)
            val (start, outerStart, outerEnd, end) = route.points
            assertEquals(start.x, end.x)
            assertTrue(outerStart.x > start.x)
            assertEquals(outerStart.x, outerEnd.x)
            assertEquals(start.y, outerStart.y)
            assertEquals(end.y, outerEnd.y)
            assertTrue(start.y != end.y)
        }
    }

    @Test
    fun packetProducesDeterministicBitRowsAndSplitFields() {
        val diagram = PacketDiagram(
            "Header",
            listOf(PacketField(0, 15, "Source"), PacketField(16, 40, "Cross-row payload")),
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        val second = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, second)
        assertEquals(3, first.commands.filterIsInstance<DrawRect>().size)
        assertEquals(2, first.commands.filterIsInstance<DrawText>().count { it.text == "Cross-row payload" })
        assertTrue(first.commands.filterIsInstance<DrawText>().any { it.text == "32-40" })
    }

    @Test
    fun packetCrossRowSegmentsMeasureRepeatedLabelsIndependently() {
        val label = "A".repeat(50)
        val scene = SimpleMermaidLayout.layout(
            PacketDiagram(null, listOf(PacketField(31, 32, label))),
            FixedWidthTextMeasurer,
            LayoutConfig(),
        )
        val labelStyle = build.raft.mermaid.layout.TextStyle(fontSize = 11.0)
        val requiredWidth = FixedWidthTextMeasurer.measure(label, labelStyle).width + 20.0
        assertEquals(2, scene.commands.filterIsInstance<DrawRect>().size)
        assertTrue(scene.commands.filterIsInstance<DrawRect>().all { it.rect.width >= requiredWidth })
    }

    @Test
    fun packetSceneMeasuresLongTitleWithRenderedStyle() {
        val title = "T".repeat(100)
        val config = LayoutConfig()
        val scene = SimpleMermaidLayout.layout(
            PacketDiagram(title, listOf(PacketField(0, 0, "F"))),
            FixedWidthTextMeasurer,
            config,
        )
        val titleStyle = build.raft.mermaid.layout.TextStyle(fontSize = 18.0, fontWeight = 600)
        val requiredWidth = FixedWidthTextMeasurer.measure(title, titleStyle).width + config.padding * 2
        assertTrue(scene.width >= requiredWidth)
    }

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
    fun timelineProducesDeterministicPeriodGeometry() {
        val diagram = TimelineDiagram("History", listOf(TimelineEvent("2024", listOf("Launch", "Users")), TimelineEvent("2025", listOf("Scale"))))
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(2, first.commands.filterIsInstance<DrawPolygon>().size)
        assertEquals(5, first.commands.filterIsInstance<DrawText>().size)
        assertTrue(first.width >= 420.0 && first.height > 0.0)
    }

    @Test
    fun userJourneyProducesDeterministicSectionAndTaskCards() {
        val diagram = UserJourneyDiagram(
            "Checkout journey",
            listOf(
                UserJourneySection(
                    "Discover",
                    listOf(
                        UserJourneyTask("Find product", 4, listOf("Shopper")),
                        UserJourneyTask("Review & compare", 3, listOf("Shopper", "Advisor")),
                    ),
                ),
                UserJourneySection("Purchase", listOf(UserJourneyTask("Pay securely", 5, listOf("Shopper")))),
            ),
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(5, first.commands.filterIsInstance<DrawRect>().size)
        assertTrue(first.commands.filterIsInstance<DrawText>().any { it.text == "Score 3 · Shopper, Advisor" })
        assertEquals(
            listOf("#dcfce7", "#fef3c7", "#bbf7d0"),
            first.commands.filterIsInstance<DrawRect>().filter { it.fill.value != "#e2e8f0" }.map { it.fill.value },
        )
        assertTrue(first.width >= 640.0 && first.height > 0.0)
    }

    @Test
    fun userJourneyWidthContainsLongTitle() {
        val title = "A".repeat(100)
        val diagram = UserJourneyDiagram(
            title,
            listOf(UserJourneySection("Section", listOf(UserJourneyTask("Task", 3, listOf("Actor"))))),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        val measuredTitle = FixedWidthTextMeasurer.measure(title, build.raft.mermaid.layout.TextStyle(fontSize = 18.0, fontWeight = 600))
        assertTrue(scene.width >= measuredTitle.width + 48.0)
    }

    @Test
    fun gitGraphProducesDeterministicLanesParentsAndCommitKinds() {
        val diagram = GitGraphDiagram(
            listOf(GitGraphBranch("main", null), GitGraphBranch("develop", "base")),
            listOf(
                GitGraphCommit("base", "main", emptyList()),
                GitGraphCommit("feature", "develop", listOf("base"), GitGraphCommitType.HIGHLIGHT, "beta"),
                GitGraphCommit("release", "main", listOf("base"), GitGraphCommitType.REVERSE),
                GitGraphCommit("merge", "main", listOf("release", "feature"), isMerge = true),
            ),
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(5, first.commands.filterIsInstance<DrawRect>().size)
        assertTrue(first.commands.filterIsInstance<DrawLine>().size >= 7)
        assertTrue(first.commands.filterIsInstance<DrawText>().map { it.text }.containsAll(listOf("main", "develop", "beta", "merge")))
        assertTrue(first.width >= 480.0 && first.height > 0.0)
    }

    @Test fun kanbanProducesMeasuredDeterministicColumnsAndCards() {
        val longLabel = "A".repeat(100)
        val diagram = KanbanDiagram(listOf(KanbanColumn("todo", "Todo", listOf(KanbanCard("a", longLabel))), KanbanColumn("done", "Done", listOf(KanbanCard("b", "Ship")))))
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(4, first.commands.filterIsInstance<DrawRect>().size)
        assertTrue(first.commands.filterIsInstance<DrawText>().any { it.text == longLabel })
        val measuredLabel = FixedWidthTextMeasurer.measure(longLabel, build.raft.mermaid.layout.TextStyle(fontSize = 12.0))
        assertTrue(first.commands.filterIsInstance<DrawRect>().first().rect.width >= measuredLabel.width + 32.0)
        assertTrue(first.width > 0.0 && first.height > 0.0)
    }

    @Test fun blockProducesMeasuredDeterministicGridSpansAndEdges() {
        val longLabel = "B".repeat(100)
        val diagram = BlockDiagram(
            3,
            listOf(BlockNode("api", longLabel, 2), BlockNode("db", "Database"), BlockNode("worker", "Worker")),
            listOf(BlockEdge("api", "worker"), BlockEdge("db", "worker")),
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(3, first.commands.filterIsInstance<DrawRect>().size)
        assertEquals(2, first.commands.filterIsInstance<DrawLine>().size)
        assertEquals(2, first.commands.filterIsInstance<DrawPolygon>().size)
        val measured = FixedWidthTextMeasurer.measure(longLabel, build.raft.mermaid.layout.TextStyle(fontSize = 14.0, fontWeight = 500))
        assertTrue(first.commands.filterIsInstance<DrawRect>().first().rect.width >= measured.width + 32.0)
    }

    @Test fun sankeyProducesMeasuredDeterministicLayersAndWeightedLinks() {
        val longLabel = "C".repeat(100)
        val diagram = SankeyDiagram(
            listOf(SankeyNode("source", "Source"), SankeyNode("middle", "Middle"), SankeyNode("target", longLabel)),
            listOf(SankeyLink("source", "middle", 10.0), SankeyLink("middle", "target", 2.0)),
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(3, first.commands.filterIsInstance<DrawRect>().size)
        val links = first.commands.filterIsInstance<DrawLine>()
        assertEquals(2, links.size)
        assertTrue(links[0].strokeWidth > links[1].strokeWidth)
        val measured = FixedWidthTextMeasurer.measure(longLabel, build.raft.mermaid.layout.TextStyle(fontSize = 12.0, fontWeight = 500))
        assertTrue(first.commands.filterIsInstance<DrawRect>().last().rect.width >= measured.width + 32.0)
    }

    @Test fun treemapProducesMeasuredDeterministicWeightedRectangles() {
        val longLabel = "D".repeat(100)
        val diagram = TreemapDiagram(
            listOf(TreemapNode("Root", children = listOf(TreemapNode(longLabel, 75.0), TreemapNode("Small", 25.0)))),
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(3, first.commands.filterIsInstance<DrawRect>().size)
        assertTrue(first.commands.filterIsInstance<DrawText>().map { it.text }.containsAll(listOf("Root", longLabel, "75", "Small", "25")))
        val leaves = first.commands.filterIsInstance<DrawRect>().drop(1)
        assertTrue(leaves[0].rect.width > leaves[1].rect.width)
        val measured = FixedWidthTextMeasurer.measure(longLabel, build.raft.mermaid.layout.TextStyle(fontSize = 13.0, fontWeight = 600))
        assertTrue(first.width >= measured.width + 48.0)
    }

    @Test fun treemapNeverProducesNegativeGeometryForDenseSmallWeightedNodes() {
        val tinyLeaves = (1..200).map { TreemapNode("Leaf $it", if (it == 1) 1.0 else 1e-12) }
        val roots = (1..150).map { index -> TreemapNode("Root $index", children = tinyLeaves.map { it.copy(label = "${it.label}-$index") }) }
        val scene = SimpleMermaidLayout.layout(TreemapDiagram(roots), FixedWidthTextMeasurer, LayoutConfig())
        scene.commands.filterIsInstance<DrawRect>().forEach { rectangle ->
            assertTrue(rectangle.rect.width >= 0.0)
            assertTrue(rectangle.rect.height >= 0.0)
        }
    }

    @Test fun vennProducesMeasuredDeterministicOverlappingEllipsesAndLabels() {
        val longLabel = "V".repeat(100)
        val diagram = VennDiagram(
            title = "Overlap",
            sets = listOf(VennSet("A", longLabel, 20.0), VennSet("B", "Beta", 12.0), VennSet("C", "Gamma")),
            unions = listOf(VennUnion(listOf("A", "B"), "AB"), VennUnion(listOf("A", "B", "C"), "All")),
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(3, first.commands.filterIsInstance<DrawEllipse>().size)
        assertTrue(first.commands.filterIsInstance<DrawEllipse>().all { it.radiusX > 0.0 && it.radiusY > 0.0 })
        assertTrue(first.commands.filterIsInstance<DrawText>().map { it.text }.containsAll(listOf("Overlap", longLabel, "Beta", "Gamma", "AB", "All")))
        val measured = FixedWidthTextMeasurer.measure(longLabel, build.raft.mermaid.layout.TextStyle(fontSize = 13.0, fontWeight = 600))
        assertTrue(first.width >= measured.width + 80.0)
    }

    @Test fun vennMeasuresLongTitleWithItsRenderedStyle() {
        val title = "T".repeat(100)
        val scene = SimpleMermaidLayout.layout(
            VennDiagram(title, listOf(VennSet("A", "Alpha"), VennSet("B", "Beta"))),
            FixedWidthTextMeasurer,
            LayoutConfig(),
        )
        val measured = FixedWidthTextMeasurer.measure(title, build.raft.mermaid.layout.TextStyle(fontSize = 18.0, fontWeight = 600))
        assertTrue(scene.width >= measured.width + LayoutConfig().padding * 2 + 80.0)
    }

    @Test fun usecaseProducesMeasuredDeterministicActorsShapesAndEdges() {
        val label = "U".repeat(100)
        val diagram = UsecaseDiagram(
            FlowDirection.LR,
            listOf(UsecaseActor("User", "User")),
            listOf(UsecaseNode("A", label, UsecaseShape.ELLIPSE), UsecaseNode("B", "Report", UsecaseShape.RECTANGLE)),
            listOf(UsecaseRelationship("User", "A", "opens"), UsecaseRelationship("A", "B")),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(2, scene.commands.filterIsInstance<DrawEllipse>().size)
        assertEquals(1, scene.commands.filterIsInstance<DrawRect>().size)
        assertTrue(scene.commands.filterIsInstance<DrawText>().map { it.text }.containsAll(listOf("User", label, "Report", "opens")))
        val measured = FixedWidthTextMeasurer.measure(label, build.raft.mermaid.layout.TextStyle(fontSize = 13.0, fontWeight = 600))
        assertTrue(scene.width >= measured.width * 2 + 180.0)
        val firstEdge = scene.commands.filterIsInstance<DrawLine>().first()
        assertTrue(firstEdge.from.x > LayoutConfig().padding + measured.width / 2.0)
        assertTrue(firstEdge.to.x < scene.width - LayoutConfig().padding - measured.width / 2.0)
    }

    @Test fun architectureProducesDeterministicGroupsServicesAndPortEdges() {
        val diagram = ArchitectureDiagram(
            groups = listOf(ArchitectureGroup("api", "cloud", "API")),
            services = listOf(ArchitectureService("db", "database", "Database", "api"), ArchitectureService("app", "server", "Server", "api")),
            edges = listOf(ArchitectureEdge("db", ArchitecturePort.BOTTOM, "app", ArchitecturePort.TOP, true)),
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(3, first.commands.filterIsInstance<DrawRect>().size)
        assertTrue(first.commands.filterIsInstance<DrawText>().map { it.text }.containsAll(listOf("API", "cloud", "Database", "database", "Server", "server")))
        val edge = first.commands.filterIsInstance<DrawPolyline>().single()
        assertTrue(edge.points.first().y < edge.points.last().y)
    }

    @Test fun architectureMeasuresLongGroupsAndKeepsServicesInsideTheirColumns() {
        val longGroup = "G".repeat(100)
        val diagram = ArchitectureDiagram(
            groups = listOf(ArchitectureGroup("one", "cloud", longGroup), ArchitectureGroup("two", "cloud", "Two")),
            services = listOf(ArchitectureService("a", "server", "A", "one"), ArchitectureService("b", "server", "B", "two")),
            edges = emptyList(),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        val rectangles = scene.commands.filterIsInstance<DrawRect>()
        val groupOne = rectangles[0].rect
        val groupTwo = rectangles[1].rect
        val serviceOne = rectangles[2].rect
        val serviceTwo = rectangles[3].rect
        assertTrue(serviceOne.x >= groupOne.x && serviceOne.x + serviceOne.width <= groupOne.x + groupOne.width)
        assertTrue(serviceTwo.x >= groupTwo.x && serviceTwo.x + serviceTwo.width <= groupTwo.x + groupTwo.width)
        val measured = FixedWidthTextMeasurer.measure(longGroup, build.raft.mermaid.layout.TextStyle(fontSize = 15.0, fontWeight = 600))
        assertTrue(scene.width >= measured.width + LayoutConfig().padding * 2 + 32.0)
    }

    @Test fun c4ProducesMeasuredDeterministicCardsAndBoundaryArrows() {
        val title = "T".repeat(100)
        val description = "D".repeat(100)
        val diagram = C4Diagram(title, listOf(C4Element("p", "Person", description, C4ElementKind.PERSON), C4Element("s", "System", null, C4ElementKind.SYSTEM, true)), listOf(C4Relationship("p", "s", "Uses")))
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(2, scene.commands.filterIsInstance<DrawRect>().size)
        val edge = scene.commands.filterIsInstance<DrawLine>().single()
        assertTrue(edge.from.x < edge.to.x)
        val measuredTitle = FixedWidthTextMeasurer.measure(title, build.raft.mermaid.layout.TextStyle(fontSize = 18.0, fontWeight = 600))
        val measuredDescription = FixedWidthTextMeasurer.measure(description, build.raft.mermaid.layout.TextStyle(fontSize = 10.0))
        assertTrue(scene.width >= measuredTitle.width + LayoutConfig().padding * 2)
        assertTrue(scene.width >= measuredDescription.width * 2 + 150.0)
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

    @Test
    fun quadrantChartProducesDeterministicAxesAndPoints() {
        val diagram = QuadrantChartDiagram(
            "Portfolio",
            QuadrantAxis("Low reach", "High reach"),
            QuadrantAxis("Low engagement", "High engagement"),
            listOf("Expand", "Promote", null, null),
            listOf(QuadrantPoint("A", 0.25, 0.75), QuadrantPoint("B", 1.0, 0.0)),
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(2, first.commands.filterIsInstance<DrawPolygon>().size)
        assertEquals(1, first.commands.filterIsInstance<DrawRect>().size)
        assertTrue(first.commands.filterIsInstance<DrawText>().any { it.text == "High engagement" })
    }

    private fun message(
        from: String,
        to: String,
        label: String,
        lineStyle: SequenceLineStyle,
    ): SequenceMessage = SequenceMessage(from, to, label, lineStyle, SequenceArrowHead.FILLED)

    private fun SceneRect.valid(): Boolean = width > 0.0 && height > 0.0
}
