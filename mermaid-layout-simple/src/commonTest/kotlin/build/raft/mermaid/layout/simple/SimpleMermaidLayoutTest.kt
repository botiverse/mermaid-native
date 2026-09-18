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
import build.raft.mermaid.core.FlowEdgeStyle
import build.raft.mermaid.core.FlowNode
import build.raft.mermaid.core.FlowchartDiagram
import build.raft.mermaid.core.IshikawaDiagram
import build.raft.mermaid.core.IshikawaNode
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
import build.raft.mermaid.core.RadarAxis
import build.raft.mermaid.core.RadarChartDiagram
import build.raft.mermaid.core.RadarCurve
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
import build.raft.mermaid.core.CynefinDiagram
import build.raft.mermaid.core.CynefinDomain
import build.raft.mermaid.core.CynefinDomainBlock
import build.raft.mermaid.core.SwimlaneDiagram
import build.raft.mermaid.core.Swimlane
import build.raft.mermaid.core.SwimlaneNode
import build.raft.mermaid.core.SwimlaneNodeShape
import build.raft.mermaid.core.SwimlaneEdge
import build.raft.mermaid.core.TreeViewDiagram
import build.raft.mermaid.core.TreeViewNode
import build.raft.mermaid.core.RailroadChoice
import build.raft.mermaid.core.RailroadDiagram
import build.raft.mermaid.core.RailroadRule
import build.raft.mermaid.core.WardleyEvolution
import build.raft.mermaid.core.WardleyLink
import build.raft.mermaid.core.WardleyMapDiagram
import build.raft.mermaid.core.WardleyNode
import build.raft.mermaid.core.ZenumlAsyncMessage
import build.raft.mermaid.core.ZenumlDiagram
import build.raft.mermaid.core.ZenumlParticipant
import build.raft.mermaid.core.ZenumlSyncMessage
import build.raft.mermaid.core.RailroadNonTerminal
import build.raft.mermaid.core.RailroadOneOrMore
import build.raft.mermaid.core.RailroadOptional
import build.raft.mermaid.core.RailroadSequence
import build.raft.mermaid.core.RailroadTerminal
import build.raft.mermaid.core.RailroadZeroOrMore
import build.raft.mermaid.layout.DrawLine
import build.raft.mermaid.layout.DrawPolyline
import build.raft.mermaid.layout.DrawRect
import build.raft.mermaid.layout.DrawEllipse
import build.raft.mermaid.layout.LayoutConfig
import build.raft.mermaid.layout.ScenePoint
import build.raft.mermaid.layout.SceneRect
import build.raft.mermaid.layout.StrokePattern
import build.raft.mermaid.layout.TextAnchor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimpleMermaidLayoutTest {
    @Test
    fun railroadProducesDeterministicMeasuredTracks() {
        val long = "step-".repeat(20)
        val diagram = RailroadDiagram(
            rules = listOf(
                RailroadRule(
                    name = "flow",
                    definition = RailroadSequence(
                        listOf(
                            RailroadChoice(
                                listOf(RailroadTerminal(long), RailroadOptional(RailroadNonTerminal("alternative path"))),
                            ),
                            RailroadOneOrMore(RailroadTerminal("next")),
                            RailroadZeroOrMore(RailroadTerminal("tail")),
                        ),
                    ),
                ),
            ),
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertTrue(first.commands.filterIsInstance<DrawRect>().isNotEmpty())
        assertTrue(first.commands.filterIsInstance<DrawPolyline>().size >= 2)
        assertTrue(first.commands.filterIsInstance<DrawLine>().isNotEmpty())
        val required = FixedWidthTextMeasurer.measure(long, build.raft.mermaid.layout.TextStyle(fontSize = 13.0)).width + 24.0 + 24.0
        assertTrue(first.width >= required, "width ${first.width} below required $required")
    }

    /**
     * task #342 regression: railroad connector lines must meet their neighbours exactly.
     *
     * The Choice composite boxes previously inset their connector spines by 3-4px
     * from the box edge, while a wrapping Sequence connected at the raw box edge, leaving
     * a visible 3-4px gap ("disconnected lines"). This asserts every horizontal connector
     * endpoint coincides with another primitive's vertex (so no dangling gap can return).
     */
    @Test
    fun railroadConnectorsMeetTheirNeighboursWithoutGaps() {
        val diagram = RailroadDiagram(
            rules = listOf(
                RailroadRule(
                    name = "auth",
                    definition = RailroadSequence(
                        listOf(
                            RailroadTerminal("token"),
                            RailroadChoice(
                                listOf(RailroadNonTerminal("session"), RailroadOptional(RailroadTerminal("refresh"))),
                            ),
                            RailroadSequence(listOf(RailroadTerminal("validate"), RailroadTerminal("store"))),
                        ),
                    ),
                ),
            ),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())

        val vertices = mutableListOf<ScenePoint>()
        scene.commands.forEach { command ->
            when (command) {
                is DrawLine -> {
                    vertices += command.from
                    vertices += command.to
                }
                is DrawPolyline -> vertices += command.points
                is DrawRect -> {
                    vertices += ScenePoint(command.rect.x, command.rect.y)
                    vertices += ScenePoint(command.rect.x + command.rect.width, command.rect.y)
                    vertices += ScenePoint(command.rect.x, command.rect.y + command.rect.height)
                    vertices += ScenePoint(command.rect.x + command.rect.width, command.rect.y + command.rect.height)
                }
                else -> Unit
            }
        }

        val lines = scene.commands.filterIsInstance<DrawLine>()
        assertTrue(lines.isNotEmpty(), "railroad layout produced no connector lines")

        fun coveredByVertex(point: ScenePoint): Boolean = vertices.count { it == point } >= 2
        fun coveredByRectEdge(point: ScenePoint): Boolean = scene.commands.filterIsInstance<DrawRect>().any { command ->
            val r = command.rect
            val onVerticalEdge = (point.x == r.x || point.x == r.x + r.width) && point.y in r.y..(r.y + r.height)
            val onHorizontalEdge = (point.y == r.y || point.y == r.y + r.height) && point.x in r.x..(r.x + r.width)
            onVerticalEdge || onHorizontalEdge
        }

        val dangling = lines.flatMap { listOf(it.from, it.to) }.filter { endpoint ->
            !coveredByVertex(endpoint) && !coveredByRectEdge(endpoint)
        }
        assertTrue(
            dangling.isEmpty(),
            "railroad connector endpoints dangle (gap) at: $dangling",
        )
    }

    @Test
    fun zenumlProducesDeterministicMeasuredLifelines() {
        val longParticipant = "participant-".repeat(20)
        val diagram = ZenumlDiagram(
            title = "Token handshake",
            participants = listOf(
                ZenumlParticipant("Client", longParticipant),
                ZenumlParticipant("Store", "Token store"),
            ),
            messages = listOf(
                ZenumlSyncMessage("Client", "Store", "submit"),
                ZenumlSyncMessage("Store", "Store", "persist"),
                ZenumlAsyncMessage("Client", "Store", "cancel"),
            ),
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(2, first.commands.filterIsInstance<DrawRect>().size)
        assertTrue(first.commands.count { it is DrawLine && it.pattern == StrokePattern.DASHED } >= 2)
        assertTrue(first.commands.filterIsInstance<DrawPolyline>().size == 1)
        val labels = first.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue(labels.contains("submit()"))
        assertTrue(labels.contains("cancel"))
        assertTrue(labels.contains(longParticipant))
        val required = FixedWidthTextMeasurer.measure(longParticipant, build.raft.mermaid.layout.TextStyle(fontSize = 13.0)).width + 32.0
        assertTrue(first.width >= required, "width ${first.width} below required $required")
    }

    @Test
    fun ishikawaProducesDeterministicMeasuredFishbone() {
        val long = "cause-".repeat(20)
        val diagram = IshikawaDiagram(
            IshikawaNode(
                "Blurry Photo",
                listOf(
                    IshikawaNode("Process", listOf(IshikawaNode("Out of focus"), IshikawaNode(long))),
                    IshikawaNode("Equipment", listOf(IshikawaNode("Dirty lens"))),
                    IshikawaNode("Environment", listOf(IshikawaNode("Too dark"))),
                    IshikawaNode("User", listOf(IshikawaNode("Shaky hands"))),
                ),
            ),
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        // The effect renders as the fish head polygon plus a horizontal spine.
        assertTrue(first.commands.filterIsInstance<DrawPolygon>().isNotEmpty())
        assertTrue(first.commands.filterIsInstance<DrawPolyline>().isNotEmpty())
        // Causes alternate sides of the spine: upper labels above, lower below.
        val labelY = { text: String ->
            first.commands.filterIsInstance<DrawText>().first { it.text == text }.origin.y
        }
        val spineY = first.commands.filterIsInstance<DrawPolyline>().first().points.first().y
        assertTrue(labelY("Process") < spineY, "first cause should sit above the spine")
        assertTrue(labelY("Equipment") > spineY, "second cause should sit below the spine")
        assertTrue(labelY("Environment") < spineY, "third cause should sit above the spine")
        // Measured layout: the widest sub-cause label must fit inside the scene.
        val required = FixedWidthTextMeasurer.measure(long, build.raft.mermaid.layout.TextStyle(fontSize = 13.0)).width + 160.0
        assertTrue(first.width >= required, "width ${first.width} below required $required")
    }

    @Test
    fun ishikawaDrawsCauseLabelBoxesAndFishHead() {
        val diagram = IshikawaDiagram(
            IshikawaNode(
                "Blurry Photo",
                listOf(
                    IshikawaNode("Process", listOf(IshikawaNode("Out of focus"))),
                    IshikawaNode("Equipment", listOf(IshikawaNode("Dirty lens"))),
                    IshikawaNode("Environment", listOf(IshikawaNode("Too dark"))),
                ),
            ),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val boxes = scene.commands.filterIsInstance<DrawRect>()
        assertEquals(3, boxes.size)
        assertTrue(boxes.all { it.fill.value == "#e2e8f0" })
        assertEquals("#e2e8f0", scene.commands.filterIsInstance<DrawPolygon>().first().fill.value)
        val labels = scene.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue(labels.containsAll(listOf("Blurry", "Photo", "Process", "Equipment", "Environment")))
        assertTrue(scene.commands.filterIsInstance<DrawPolygon>().size > 3)
    }

    @Test
    fun wardleyProducesDeterministicInvertedAxisMap() {
        val diagram = WardleyMapDiagram(
            title = "Tea Shop",
            nodes = listOf(
                WardleyNode("Business", 1.0, 0.9, anchor = true),
                WardleyNode("Cup of Tea", 0.0, 0.1, anchor = false),
            ),
            links = listOf(WardleyLink("Business", "Cup of Tea")),
            evolutions = listOf(WardleyEvolution("Cup of Tea", 0.9)),
            notes = listOf(build.raft.mermaid.core.WardleyNote("note text", 0.5, 0.5)),
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        // Only the non-anchor node renders as an ellipse.
        assertEquals(1, first.commands.filterIsInstance<DrawEllipse>().size)
        // Axes, three stage dividers, one dependency link, and one dashed evolution arrow.
        val lines = first.commands.filterIsInstance<DrawLine>()
        assertEquals(7, lines.size)
        assertEquals(4, lines.count { it.pattern == StrokePattern.DASHED })
        // Visibility inversion: the visibility-1.0 anchor sits above the visibility-0.0 component.
        val anchorLabel = first.commands.filterIsInstance<DrawText>().first { it.text == "Business" }
        val componentLabel = first.commands.filterIsInstance<DrawText>().first { it.text == "Cup of Tea" }
        assertTrue(anchorLabel.origin.y < componentLabel.origin.y)
        // Evolution arrow keeps visibility constant (horizontal) and points right.
        val evolutionLine = lines.first { it.stroke.value == "#dc2626" }
        assertEquals(StrokePattern.DASHED, evolutionLine.pattern)
        assertEquals(evolutionLine.from.y, evolutionLine.to.y)
        assertTrue(evolutionLine.to.x > evolutionLine.from.x)
    }

    @Test
    fun wardleyDrawsEvolutionVisibilityAxesAndStages() {
        val diagram = WardleyMapDiagram(
            title = "Tea Shop",
            nodes = listOf(
                WardleyNode("Business", 1.0, 0.9, anchor = true),
                WardleyNode("Cup of Tea", 0.0, 0.1, anchor = false),
            ),
            links = listOf(WardleyLink("Business", "Cup of Tea")),
            evolutions = listOf(WardleyEvolution("Cup of Tea", 0.9)),
            notes = listOf(build.raft.mermaid.core.WardleyNote("note text", 0.5, 0.5)),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val labels = scene.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue("Evolution" in labels)
        assertTrue("Visibility" in labels)
        assertEquals(listOf("Genesis", "Custom Built", "Product", "Commodity"), labels.filter { it in setOf("Genesis", "Custom Built", "Product", "Commodity") })
        assertTrue("note text" in labels)
        assertEquals(600, scene.commands.filterIsInstance<DrawText>().first { it.text == "note text" }.style.fontWeight)
        val evolve = scene.commands.filterIsInstance<DrawLine>().first { it.stroke.value == "#dc2626" }
        assertEquals(StrokePattern.DASHED, evolve.pattern)
        assertEquals(evolve.from.y, evolve.to.y)
        assertTrue(evolve.to.x > evolve.from.x)
    }

    @Test
    fun treeViewProducesDeterministicMeasuredHierarchy() {
        val long = "directory-".repeat(20)
        val diagram = TreeViewDiagram(
            listOf(
                TreeViewNode(long, 0, null, true),
                TreeViewNode("src", 1, 0, true),
                TreeViewNode("index.ts", 2, 1, false),
            ),
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(4, first.commands.filterIsInstance<DrawEllipse>().size)
        assertEquals(3, first.commands.filterIsInstance<DrawPolyline>().size)
        val required = FixedWidthTextMeasurer.measure(long, build.raft.mermaid.layout.TextStyle(fontSize = 13.0, fontWeight = 600)).width + 48.0
        assertTrue(first.width >= required)
    }

    @Test
    fun treeViewDrawsOfficialRootAndBoldDirectories() {
        val diagram = TreeViewDiagram(
            listOf(
                TreeViewNode("project", 0, null, true),
                TreeViewNode("src", 1, 0, true),
                TreeViewNode("index.ts", 2, 1, false),
                TreeViewNode("package.json", 0, null, false),
            ),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val labels = scene.commands.filterIsInstance<DrawText>()
        assertEquals("/", labels.first().text)
        assertEquals(600, labels.first().style.fontWeight)
        assertEquals(listOf("/", "project", "src"), labels.filter { it.style.fontWeight == 600 }.map { it.text })
        assertEquals(listOf("index.ts", "package.json"), labels.filter { it.style.fontWeight == 400 }.map { it.text })
        val dots = scene.commands.filterIsInstance<DrawEllipse>()
        assertEquals("#f59e0b", dots.first().fill.value)
        assertEquals("#3b82f6", dots.last().fill.value)
    }

    @Test
    fun treeViewConnectorsMeetCenteredEllipses() {
        val diagram = TreeViewDiagram(
            listOf(
                TreeViewNode("project", 0, null, true),
                TreeViewNode("src", 1, 0, true),
                TreeViewNode("index.ts", 2, 1, false),
                TreeViewNode("README.md", 1, 0, false),
            ),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        val ellipses = scene.commands.filterIsInstance<DrawEllipse>()
        val polylines = scene.commands.filterIsInstance<DrawPolyline>()
        assertEquals(5, ellipses.size)
        assertEquals(4, polylines.size)

        fun onEllipseBoundary(point: ScenePoint, ellipse: DrawEllipse): Boolean {
            val nx = (point.x - ellipse.center.x) / ellipse.radiusX
            val ny = (point.y - ellipse.center.y) / ellipse.radiusY
            return kotlin.math.abs(nx * nx + ny * ny - 1.0) <= 0.05
        }

        polylines.forEach { poly ->
            assertEquals(3, poly.points.size)
            val start = poly.points.first()
            val elbow = poly.points[1]
            val end = poly.points.last()
            assertEquals(start.x, elbow.x)
            assertEquals(elbow.y, end.y)
            assertTrue(
                ellipses.any { ellipse -> ellipse.center.x == start.x && onEllipseBoundary(start, ellipse) },
                "vertical spine start $start is not centered on a parent ellipse",
            )
            assertTrue(
                ellipses.any { ellipse -> ellipse.center.y == end.y && onEllipseBoundary(end, ellipse) },
                "horizontal run end $end does not meet a child ellipse",
            )
        }
    }

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
    fun swimlaneDrawsLaneTitleRailsAndDecisionChrome() {
        val diagram = SwimlaneDiagram(
            FlowDirection.LR,
            listOf(
                Swimlane("customer", "Customer", listOf(SwimlaneNode("request", "Request", SwimlaneNodeShape.RECTANGLE))),
                Swimlane("support", "Support", listOf(SwimlaneNode("triage", "Triage", SwimlaneNodeShape.DECISION))),
            ),
            listOf(SwimlaneEdge("request", "triage")),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val rects = scene.commands.filterIsInstance<DrawRect>()
        val rails = rects.filter { it.rect.width == 32.0 }
        assertEquals(2, rails.size)
        assertTrue(rails.all { it.fill.value == "#fcfcfc" && it.stroke.value == "#707070" })
        val bodies = rects.filter { it.rect.width > 32.0 && it.stroke.value == "#707070" }
        assertEquals(2, bodies.size)
        val nodes = rects.filter { it.fill.value == "#ffffff" && it.stroke.value == "#2563eb" }
        assertEquals(1, nodes.size)
        assertTrue(nodes.all { it.cornerRadius == 0.0 })
        val diamond = scene.commands.filterIsInstance<DrawPolygon>().single { it.points.size == 4 }
        assertEquals("#fef3c7", diamond.fill.value)
        val outline = scene.commands.filterIsInstance<DrawPolyline>().single { it.points.size == 5 }
        assertEquals("#d97706", outline.stroke.value)
        assertTrue(scene.commands.filterIsInstance<DrawLine>().all { it.stroke.value == "#666666" })
        assertTrue(scene.commands.filterIsInstance<DrawPolygon>().filter { it.points.size == 3 }.all { it.fill.value == "#333333" })
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
        val labels = first.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue(labels.containsAll(listOf("16", "31", "32", "40")))
        assertTrue(labels.none { "-" in it && it[0].isDigit() })
    }

    @Test
    fun packetDrawsStartAndEndBitIndexes() {
        val diagram = PacketDiagram(
            "UDP Packet",
            listOf(PacketField(0, 15, "Source Port"), PacketField(16, 31, "Destination Port")),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val indexes = scene.commands.filterIsInstance<DrawText>().filter { it.style.fontSize == 9.0 }
        assertEquals(listOf("0", "15", "16", "31"), indexes.map { it.text })
        assertEquals(TextAnchor.START, indexes[0].anchor)
        assertEquals(TextAnchor.END, indexes[1].anchor)
        assertEquals("#eff6ff", scene.commands.filterIsInstance<DrawRect>().first().fill.value)
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
    fun xyChartDrawsAxisTicksAndSeriesValueLabels() {
        val diagram = XyChartDiagram(
            title = "Sales",
            xAxis = XyAxis("Quarter", listOf("Q1", "Q2", "Q3", "Q4")),
            yAxis = NumericAxis("Revenue", 0.0, 100.0),
            series = listOf(
                XySeries(XySeriesKind.BAR, listOf(20.0, 45.0, 70.0, 85.0)),
                XySeries(XySeriesKind.LINE, listOf(25.0, 40.0, 75.0, 90.0)),
            ),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val labels = scene.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue(labels.containsAll(listOf("0", "10", "50", "100", "Q1", "Q4")))
        assertTrue("100.0" !in labels && "0.0" !in labels)
        assertTrue(labels.containsAll(listOf("20", "45", "70", "85", "25", "40", "75", "90")))
        assertEquals(4, scene.commands.filterIsInstance<DrawRect>().size)
        assertEquals("#2563eb", scene.commands.filterIsInstance<DrawRect>().first().fill.value)
        assertEquals("#16a34a", scene.commands.filterIsInstance<DrawPolyline>().first().stroke.value)
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
        assertTrue(first.commands.filterIsInstance<DrawLine>().size > 1)
        assertEquals(1, first.commands.filterIsInstance<DrawEllipse>().size)
        val labels = first.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue(labels.containsAll(listOf("CUSTOMER", "ORDER", "int", "id", "PK", "customerId", "FK", "places")))
        assertTrue(labels.none { it == "int id PK" || it == "0..*" || it == "1" })
    }

    @Test
    fun entityRelationshipSplitsTypedRowsAndDrawsCrowsFoot() {
        val diagram = EntityRelationshipDiagram(
            entities = listOf(
                EntityDefinition("CUSTOMER", listOf(EntityAttribute("int", "id", EntityKey.PK), EntityAttribute("string", "name"))),
                EntityDefinition("ORDER", listOf(EntityAttribute("int", "id", EntityKey.PK), EntityAttribute("int", "customerId", EntityKey.FK))),
            ),
            relationships = listOf(
                EntityRelationship("CUSTOMER", "ORDER", EntityCardinality.ONLY_ONE, EntityCardinality.ZERO_OR_MORE, "places"),
            ),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val labels = scene.commands.filterIsInstance<DrawText>().map { it.text }
        assertEquals(listOf("places", "CUSTOMER", "int", "id", "PK", "string", "name", "ORDER", "int", "id", "PK", "int", "customerId", "FK"), labels)
        assertEquals(1, scene.commands.filterIsInstance<DrawEllipse>().size)
        assertTrue(scene.commands.filterIsInstance<DrawLine>().size >= 8)
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
        assertEquals(2, scene.commands.filterIsInstance<DrawLine>().size)
        assertEquals(1, scene.commands.filterIsInstance<DrawPolyline>().size)
        assertEquals(0, scene.commands.filterIsInstance<DrawPolygon>().size)
    }

    @Test
    fun classDiagramSeparatesAttributesMethodsAndKeepsInheritanceHollow() {
        val diagram = ClassDiagram(
            classes = listOf(
                ClassDefinition("Animal", members = listOf(ClassMember("String name"), ClassMember("eat()"))),
                ClassDefinition("Duck", members = listOf(ClassMember("swim()"))),
            ),
            relationships = listOf(ClassRelationship("Animal", "Duck", ClassRelationshipKind.INHERITANCE)),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(2, scene.commands.filterIsInstance<DrawRect>().size)
        assertEquals(4, scene.commands.filterIsInstance<DrawLine>().size)
        assertEquals(1, scene.commands.filterIsInstance<DrawPolyline>().size)
        assertEquals(0, scene.commands.filterIsInstance<DrawPolygon>().size)
        val labels = scene.commands.filterIsInstance<DrawText>().map { it.text }
        assertEquals(listOf("Animal", "+String name", "+eat()", "Duck", "+swim()"), labels)
    }

    @Test
    fun pieProducesDeterministicSlicesAndShowDataLegend() {
        val diagram = PieDiagram(title = "Pets", showData = true, sections = listOf(PieSection("Dogs", 3.0), PieSection("Cats", 1.0)))
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(2, first.commands.filterIsInstance<DrawPolygon>().size)
        val labels = first.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue(labels.containsAll(listOf("Pets", "75%", "25%", "Dogs [3]", "Cats [1]")))
        assertTrue(labels.none { it.contains(": 3.0") || it.contains(": 1.0") })
    }

    @Test
    fun pieShowDataDrawsSlicePercentsAndBracketValues() {
        val diagram = PieDiagram(title = "Pets", showData = true, sections = listOf(PieSection("Dogs", 386.0), PieSection("Cats", 85.0), PieSection("Rats", 15.0)))
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val labels = scene.commands.filterIsInstance<DrawText>().map { it.text }
        assertEquals(listOf("Pets", "79%", "Dogs [386]", "17%", "Cats [85]", "3%", "Rats [15]"), labels)
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
        assertEquals(2, stateRects.size)
        assertEquals(3, first.commands.filterIsInstance<DrawLine>().size)
        assertEquals(3, first.commands.filterIsInstance<DrawEllipse>().size)
        assertTrue(stateRects.all { it.fill.value == "#eeeeee" && it.stroke.value == "#999999" && it.cornerRadius == 5.0 })
        val terminals = first.commands.filterIsInstance<DrawEllipse>()
        assertEquals("#222222", terminals.first().fill.value)
        assertEquals("#222222", terminals.last().fill.value)
        assertTrue(terminals.first().radiusX > 0.0)
    }

    @Test
    fun stateDrawsStartEndBulletsAndRoundedChrome() {
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
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val boxes = scene.commands.filterIsInstance<DrawRect>()
        assertEquals(2, boxes.size)
        assertTrue(boxes.all { it.fill.value == "#eeeeee" && it.stroke.value == "#999999" && it.cornerRadius == 5.0 && it.strokeWidth == 1.0 })
        val bullets = scene.commands.filterIsInstance<DrawEllipse>()
        assertEquals(3, bullets.size)
        val start = bullets.first()
        assertEquals("#222222", start.fill.value)
        assertEquals("#222222", start.stroke.value)
        assertEquals(7.0, start.radiusX)
        assertEquals(7.0, start.radiusY)
        val endOuter = bullets[1]
        val endInner = bullets[2]
        assertEquals("#ffffff", endOuter.fill.value)
        assertEquals("#222222", endOuter.stroke.value)
        assertEquals(7.0, endOuter.radiusX)
        assertEquals("#222222", endInner.fill.value)
        assertEquals(2.5, endInner.radiusX)
        val lines = scene.commands.filterIsInstance<DrawLine>()
        assertEquals(3, lines.size)
        assertTrue(lines.all { it.stroke.value == "#666666" && it.strokeWidth == 1.0 })
        val heads = scene.commands.filterIsInstance<DrawPolygon>()
        assertEquals(3, heads.size)
        assertTrue(heads.all { it.fill.value == "#333333" })
    }

    @Test
    fun thickFlowchartEdgesUseThickStroke() {
        val scene = SimpleMermaidLayout.layout(
            FlowchartDiagram(
                direction = FlowDirection.TD,
                nodes = listOf(FlowNode("A", "Start"), FlowNode("B", "Finish")),
                edges = listOf(FlowEdge("A", "B", FlowEdgeStyle.THICK)),
            ),
            FixedWidthTextMeasurer,
            LayoutConfig(),
        )

        assertEquals(3.0, scene.commands.filterIsInstance<DrawLine>().single().strokeWidth)
        assertEquals("#666666", scene.commands.filterIsInstance<DrawLine>().single().stroke.value)
        assertEquals("#333333", scene.commands.filterIsInstance<DrawPolygon>().single().fill.value)
        assertEquals(setOf("#eeeeee"), scene.commands.filterIsInstance<DrawRect>().map { it.fill.value }.toSet())
        assertEquals(setOf("#999999"), scene.commands.filterIsInstance<DrawRect>().map { it.stroke.value }.toSet())
        assertTrue(scene.commands.filterIsInstance<DrawRect>().all { it.cornerRadius == 5.0 })
    }

    @Test
    fun flowchartNodesUseOfficialNeutralChrome() {
        val scene = SimpleMermaidLayout.layout(
            FlowchartDiagram(
                direction = FlowDirection.TD,
                nodes = listOf(FlowNode("A", "Start"), FlowNode("B", "Finish")),
                edges = listOf(FlowEdge("A", "B")),
            ),
            FixedWidthTextMeasurer,
            LayoutConfig(),
        )
        assertEquals(scene, SimpleMermaidLayout.layout(
            FlowchartDiagram(
                direction = FlowDirection.TD,
                nodes = listOf(FlowNode("A", "Start"), FlowNode("B", "Finish")),
                edges = listOf(FlowEdge("A", "B")),
            ),
            FixedWidthTextMeasurer,
            LayoutConfig(),
        ))
        val nodes = scene.commands.filterIsInstance<DrawRect>()
        assertEquals(2, nodes.size)
        assertTrue(nodes.all { it.fill.value == "#eeeeee" && it.stroke.value == "#999999" && it.cornerRadius == 5.0 })
        assertEquals("#666666", scene.commands.filterIsInstance<DrawLine>().single().stroke.value)
        assertEquals("#333333", scene.commands.filterIsInstance<DrawPolygon>().single().fill.value)
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
        assertEquals(6, first.commands.filterIsInstance<DrawText>().size)
        assertTrue(first.width >= 420.0 && first.height > 0.0)
    }

    @Test
    fun timelineDrawsSeparateEventLabels() {
        val diagram = TimelineDiagram("History", listOf(TimelineEvent("2024", listOf("Launch", "First users")), TimelineEvent("2025", listOf("Scale"))))
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val labels = scene.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue("Launch" in labels && "First users" in labels)
        assertTrue(labels.none { " · " in it })
        assertEquals("#2563eb", scene.commands.filterIsInstance<DrawPolygon>().first().fill.value)
    }

    @Test
    fun timelineRendersSectionLabelsAndBoundaries() {
        val diagram = TimelineDiagram(
            "History",
            listOf(
                TimelineEvent("2024", listOf("Launch"), section = "Early"),
                TimelineEvent("2025", listOf("Scale"), section = "Later"),
            ),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        val texts = scene.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue("Early" in texts && "Later" in texts)
        assertTrue(scene.commands.filterIsInstance<DrawLine>().size >= 3)
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
    fun userJourneyDrawsScoreDotsAndActorLegend() {
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
            ),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val dots = scene.commands.filterIsInstance<DrawEllipse>()
        assertEquals(10 + 3 + 2, dots.size)
        assertEquals(4, dots.take(5).count { it.fill.value == "#334155" })
        assertEquals(1, dots.take(5).count { it.fill.value == "#ffffff" })
        val labels = scene.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue("Shopper" in labels && "Advisor" in labels)
        assertEquals("#8FBC8F", dots[dots.lastIndex - 1].fill.value)
        assertEquals("#7CFC00", dots.last().fill.value)
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
        assertTrue(first.commands.filterIsInstance<DrawLine>().size >= 7)
        assertTrue(first.commands.filterIsInstance<DrawText>().map { it.text }.containsAll(listOf("main", "develop", "beta", "merge")))
        assertTrue(first.width >= 480.0 && first.height > 0.0)
    }

    @Test
    fun gitGraphDrawsCommitCirclesAndTagFlags() {
        val diagram = GitGraphDiagram(
            listOf(GitGraphBranch("main", null), GitGraphBranch("develop", "base")),
            listOf(
                GitGraphCommit("base", "main", emptyList()),
                GitGraphCommit("feature", "develop", listOf("base"), GitGraphCommitType.HIGHLIGHT, "beta"),
                GitGraphCommit("release", "main", listOf("base"), GitGraphCommitType.REVERSE),
                GitGraphCommit("merge", "main", listOf("release", "feature"), isMerge = true),
            ),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val ellipses = scene.commands.filterIsInstance<DrawEllipse>()
        assertTrue(ellipses.size >= 4)
        assertEquals(1, scene.commands.filterIsInstance<DrawPolygon>().size)
        assertEquals("#ede9fe", scene.commands.filterIsInstance<DrawPolygon>().first().fill.value)
        assertEquals("#2563eb", scene.commands.filterIsInstance<DrawRect>().first().fill.value)
        assertEquals("beta", scene.commands.filterIsInstance<DrawText>().first { it.style.color.value == "#7c3aed" }.text)
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

    @Test
    fun kanbanDrawsPerColumnHeightAndCenteredTitles() {
        val diagram = KanbanDiagram(
            listOf(
                KanbanColumn("todo", "Todo", listOf(KanbanCard("a", "Spec"), KanbanCard("b", "Tests"))),
                KanbanColumn("done", "Done", listOf(KanbanCard("c", "Ship"))),
            ),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val columns = scene.commands.filterIsInstance<DrawRect>().filter { it.rect.height > 80.0 }
        assertEquals(2, columns.size)
        assertTrue(columns[0].rect.height > columns[1].rect.height)
        val todo = scene.commands.filterIsInstance<DrawText>().first { it.text == "Todo" }
        assertEquals(TextAnchor.MIDDLE, todo.anchor)
        assertEquals(2, scene.commands.filterIsInstance<DrawLine>().size)
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

    @Test
    fun sankeyDrawsNodeValues() {
        val diagram = SankeyDiagram(
            listOf(
                SankeyNode("grid", "Grid"),
                SankeyNode("industry", "Industry"),
                SankeyNode("homes", "Heating, homes"),
                SankeyNode("loss", "Losses & exports"),
            ),
            listOf(
                SankeyLink("grid", "industry", 12.5),
                SankeyLink("grid", "homes", 7.25),
                SankeyLink("industry", "loss", 2.5),
            ),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val labels = scene.commands.filterIsInstance<DrawText>().map { it.text }
        assertEquals(listOf("Grid 19.75", "Industry 12.5", "Heating, homes 7.25", "Losses & exports 2.5"), labels)
        assertEquals("#dbeafe", scene.commands.filterIsInstance<DrawRect>().first().fill.value)
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

    @Test
    fun treemapDrawsSectionAndLeafValues() {
        val diagram = TreemapDiagram(
            listOf(TreemapNode("Products", children = listOf(TreemapNode("Mobile", 45.0), TreemapNode("Web", 35.0), TreemapNode("API", 20.0)))),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val labels = scene.commands.filterIsInstance<DrawText>()
        assertTrue(labels.any { it.text == "Products" })
        assertTrue(labels.any { it.text == "100" && it.anchor == TextAnchor.END })
        assertEquals(setOf("45", "35", "20"), labels.filter { it.anchor == TextAnchor.MIDDLE && it.text.all { ch -> ch.isDigit() } }.map { it.text }.toSet())
        val mobile = labels.single { it.text == "Mobile" }
        assertEquals(TextAnchor.MIDDLE, mobile.anchor)
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
        assertEquals(7, first.commands.filterIsInstance<DrawRect>().size)
        assertEquals(5, first.commands.filterIsInstance<DrawEllipse>().size)
        val labels = first.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue(labels.containsAll(listOf("API", "Database", "Server")))
        assertTrue(labels.none { it in listOf("cloud", "database", "server") })
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
        val groupRects = rectangles.filter { it.fill.value == "#f8fafc" }.map { it.rect }
        val serviceCards = rectangles.filter { it.rect.height == 76.0 }.map { it.rect }
        val groupOne = groupRects[0]
        val groupTwo = groupRects[1]
        val serviceOne = serviceCards[0]
        val serviceTwo = serviceCards[1]
        assertTrue(serviceOne.x >= groupOne.x && serviceOne.x + serviceOne.width <= groupOne.x + groupOne.width)
        assertTrue(serviceTwo.x >= groupTwo.x && serviceTwo.x + serviceTwo.width <= groupTwo.x + groupTwo.width)
        val measured = FixedWidthTextMeasurer.measure(longGroup, build.raft.mermaid.layout.TextStyle(fontSize = 15.0, fontWeight = 600))
        assertTrue(scene.width >= measured.width + LayoutConfig().padding * 2 + 32.0)
    }

    @Test fun architectureUnknownIconsStayAsText() {
        val diagram = ArchitectureDiagram(
            groups = listOf(ArchitectureGroup("api", "mystery", "API")),
            services = listOf(ArchitectureService("app", "custom", "Server", "api")),
            edges = emptyList(),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(2, scene.commands.filterIsInstance<DrawRect>().size)
        assertEquals(0, scene.commands.filterIsInstance<DrawEllipse>().size)
        assertTrue(scene.commands.filterIsInstance<DrawText>().map { it.text }.containsAll(listOf("API", "mystery", "Server", "custom")))
    }

    @Test fun cynefinDrawsOfficialDomainDescriptionsOnEveryQuadrant() {
        val diagram = CynefinDiagram(
            title = "Incident response",
            domains = listOf(
                CynefinDomainBlock(CynefinDomain.COMPLEX, listOf("Investigate & learn")),
                CynefinDomainBlock(CynefinDomain.CONFUSION, listOf("Unknown failure")),
            ),
            transitions = emptyList(),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val labels = scene.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue(labels.containsAll(listOf(
            "Complex", "Complicated", "Clear", "Chaotic", "Confusion",
            "Probe → Sense → Respond", "Emergent Practices",
            "Sense → Analyse → Respond", "Good Practices",
            "Sense → Categorise → Respond", "Best Practices",
            "Act → Sense → Respond", "Novel Practices",
            "Disorder",
            "Investigate & learn",
            "Unknown failure",
        )))
        assertEquals(4, scene.commands.filterIsInstance<DrawRect>().size)
        assertEquals(1, scene.commands.filterIsInstance<DrawEllipse>().size)
    }

    @Test fun c4ProducesMeasuredDeterministicCardsAndBoundaryArrows() {
        val title = "T".repeat(100)
        val description = "D".repeat(100)
        val diagram = C4Diagram(title, listOf(C4Element("p", "Person", description, C4ElementKind.PERSON), C4Element("s", "System", null, C4ElementKind.SYSTEM, true)), listOf(C4Relationship("p", "s", "Uses", "HTTPS")))
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertEquals(2, scene.commands.filterIsInstance<DrawRect>().size)
        assertEquals(1, scene.commands.filterIsInstance<DrawEllipse>().size)
        val labels = scene.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue(labels.containsAll(listOf("[Person]", "[Software System]", "Uses", "[HTTPS]")))
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
        assertEquals(4, scene.commands.filterIsInstance<DrawRect>().size)
        assertTrue(scene.commands.filterIsInstance<DrawRect>().all { it.rect.valid() })
    }

    @Test
    fun sequenceDrawsTopAndBottomActorsAndFilledArrowheads() {
        val diagram = SequenceDiagram(
            actors = listOf(SequenceActor("A", "Alice"), SequenceActor("B", "Bob")),
            messages = listOf(
                message("A", "B", "Hello Bob!", SequenceLineStyle.SOLID),
                message("B", "A", "Hi Alice!", SequenceLineStyle.DASHED),
            ),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val boxes = scene.commands.filterIsInstance<DrawRect>()
        assertEquals(4, boxes.size)
        assertTrue(boxes.all { it.fill.value == "#eaeaea" && it.stroke.value == "#666666" && it.cornerRadius == 3.0 })
        val labels = scene.commands.filterIsInstance<DrawText>().map { it.text }
        assertEquals(2, labels.count { it == "Alice" })
        assertEquals(2, labels.count { it == "Bob" })
        val heads = scene.commands.filterIsInstance<DrawPolygon>()
        assertEquals(2, heads.size)
        assertTrue(heads.all { it.fill.value == "#333333" })
        val lifelines = scene.commands.filterIsInstance<DrawLine>().take(2)
        assertTrue(lifelines.all { it.stroke.value == "#999999" && it.pattern == StrokePattern.SOLID })
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
        assertEquals(3, first.commands.filterIsInstance<DrawRect>().size)
        assertEquals(2, first.commands.filterIsInstance<DrawEllipse>().size)
        assertEquals(3, first.commands.filterIsInstance<DrawLine>().size)
        assertEquals(4, first.commands.filterIsInstance<DrawText>().size)
        assertTrue(first.commands.filterIsInstance<DrawText>().any { it.text == "Mindmap" })
        assertTrue(first.width > 0.0 && first.height > 0.0)
    }

    @Test
    fun mindmapDoubleCircleUsesConcentricEllipses() {
        val diagram = MindmapDiagram(
            listOf(
                MindmapNode("root", "Mindmap", null, 0, MindmapNodeShape.DOUBLE_CIRCLE),
                MindmapNode("a", "Origins", "root", 1),
            ),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val circles = scene.commands.filterIsInstance<DrawEllipse>()
        assertEquals(2, circles.size)
        assertEquals(circles[0].center, circles[1].center)
        assertEquals(circles[0].radiusX, circles[0].radiusY)
        assertEquals(circles[1].radiusX, circles[1].radiusY)
        assertEquals(4.0, circles[0].radiusX - circles[1].radiusX)
        assertEquals(1, scene.commands.filterIsInstance<DrawRect>().size)
    }

    @Test
    fun ganttProducesDeterministicTimelineBars() {
        val diagram = GanttDiagram("Plan", "YYYY-MM-DD", listOf(GanttSection("Build", listOf(
            GanttTask("Parser", "parse", 740212, 2, GanttTaskStatus.DONE),
            GanttTask("Renderer", "render", 740214, 3, GanttTaskStatus.ACTIVE),
        ))))
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val bars = first.commands.filterIsInstance<DrawRect>()
        assertEquals(listOf(136.0, 204.0), bars.map { it.rect.width })
        assertEquals(listOf("#16a34a", "#2563eb"), bars.map { it.fill.value })
    }

    @Test
    fun ganttDrawsIsoDateAxisTicks() {
        val diagram = GanttDiagram("Plan", "YYYY-MM-DD", listOf(GanttSection("Build", listOf(
            GanttTask("Parser", "parse", 740212, 2, GanttTaskStatus.DONE),
            GanttTask("Renderer", "render", 740214, 3, GanttTaskStatus.ACTIVE),
        ))))
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val labels = scene.commands.filterIsInstance<DrawText>().map { it.text }
        assertEquals(
            listOf("2026-08-19", "2026-08-20", "2026-08-21", "2026-08-22", "2026-08-23", "2026-08-24"),
            labels.filter { it.startsWith("2026-") },
        )
        assertTrue(labels.containsAll(listOf("Plan", "Build: Parser", "Build: Renderer")))
        assertTrue(scene.commands.filterIsInstance<DrawLine>().size >= 7)
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

    @Test
    fun radarProducesDeterministicWebCurvesTicksAndLegend() {
        val diagram = RadarChartDiagram(
            "Skills",
            listOf(RadarAxis("m", "Math"), RadarAxis("s", "Science"), RadarAxis("e", "English")),
            listOf(
                RadarCurve("alice", "Alice", listOf(85.0, 60.0, 90.0)),
                RadarCurve("bob", "Bob", listOf(40.0, 75.0, 50.0)),
            ),
            100.0,
        )
        val first = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(first, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        // Per curve: one filled area polygon plus one diamond vertex marker per value.
        assertEquals(8, first.commands.filterIsInstance<DrawPolygon>().size)
        // Two closed curve outlines. Graticule is circular ellipses, not polygons.
        assertEquals(2, first.commands.filterIsInstance<DrawPolyline>().size)
        assertEquals(5, first.commands.filterIsInstance<DrawEllipse>().size)
        assertTrue(first.commands.filterIsInstance<DrawEllipse>().all { it.radiusX == it.radiusY && it.fillOpacity == 0.0 })
        // One spoke per axis.
        assertEquals(3, first.commands.filterIsInstance<DrawLine>().size)
        assertTrue(first.commands.filterIsInstance<DrawText>().any { it.text == "Science" })
        assertTrue(first.commands.filterIsInstance<DrawText>().any { it.text == "20" })
        assertTrue(first.commands.filterIsInstance<DrawText>().any { it.text == "Alice" })
    }

    @Test
    fun radarGeometryLocksValueMappingCenterOuterAndClockwiseAxisOrder() {
        val diagram = RadarChartDiagram(
            null,
            listOf(
                RadarAxis("a", "top"),
                RadarAxis("b", "right"),
                RadarAxis("c", "bottom"),
                RadarAxis("d", "left"),
            ),
            listOf(RadarCurve("x", "x", listOf(100.0, 0.0, 100.0, 50.0))),
            100.0,
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        // Closed curve outlines are the polylines with stroke-width 2.
        val outlines = scene.commands.filterIsInstance<DrawPolyline>().filter { it.strokeWidth == 2.0 }
        assertEquals(1, outlines.size)
        val vertices = listOf(
            ScenePoint(320.0, 102.0), // axis 0 starts at -90° (top); value=max → outer ring
            ScenePoint(320.0, 272.0), // axis 1 is at 0° (right); value=0 → center
            ScenePoint(320.0, 442.0), // axis 2 is at 90° (bottom); value=max → outer ring
            ScenePoint(235.0, 272.0), // axis 3 is at 180° (left); value=max/2 → half radius
        )
        val outline = outlines.single().points
        assertEquals(vertices.first(), outline.first())
        assertEquals(vertices.first(), outline.last())
        vertices.forEach { vertex ->
            assertTrue(outline.contains(vertex), "smoothed curve must still pass through $vertex")
        }
        assertTrue(outline.size > vertices.size + 1)
        assertEquals(5, scene.commands.filterIsInstance<DrawEllipse>().size)
    }

    @Test
    fun radarGrowsCanvasForLongAxisLabelsAndWrapsLegendWithoutClipping() {
        val longLabel = "An extraordinarily long radar axis label for measurement"
        val diagram = RadarChartDiagram(
            null,
            listOf(RadarAxis("a", longLabel), RadarAxis("b", longLabel), RadarAxis("c", longLabel)),
            listOf(
                RadarCurve("c1", "First curve with a fairly long legend label", listOf(80.0, 70.0, 60.0)),
                RadarCurve("c2", "Second curve with a fairly long legend label", listOf(70.0, 60.0, 50.0)),
                RadarCurve("c3", "Third curve with a fairly long legend label", listOf(60.0, 50.0, 40.0)),
                RadarCurve("c4", "Fourth curve with a fairly long legend label", listOf(50.0, 40.0, 30.0)),
                RadarCurve("c5", "Fifth curve with a fairly long legend label", listOf(40.0, 30.0, 20.0)),
            ),
            100.0,
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertTrue(scene.width > 640.0, "Canvas must grow for long axis labels")
        scene.commands.forEach { command ->
            val points: List<ScenePoint> = when (command) {
                is DrawRect -> listOf(
                    ScenePoint(command.rect.x, command.rect.y),
                    ScenePoint(command.rect.x + command.rect.width, command.rect.y + command.rect.height),
                )
                is DrawLine -> listOf(command.from, command.to)
                is DrawPolyline -> command.points
                is DrawPolygon -> command.points
                is DrawEllipse -> listOf(
                    ScenePoint(command.center.x - command.radiusX, command.center.y - command.radiusY),
                    ScenePoint(command.center.x + command.radiusX, command.center.y + command.radiusY),
                )
                is DrawText -> {
                    val textWidth = FixedWidthTextMeasurer.measure(command.text, command.style).width
                    val left = when (command.anchor) {
                        TextAnchor.MIDDLE -> command.origin.x - textWidth / 2.0
                        TextAnchor.END -> command.origin.x - textWidth
                        TextAnchor.START -> command.origin.x
                    }
                    listOf(
                        ScenePoint(left, command.origin.y - command.style.fontSize),
                        ScenePoint(left + textWidth, command.origin.y + command.style.fontSize * 0.25),
                    )
                }
            }
            points.forEach { point ->
                assertTrue(point.x >= 0.0 && point.x <= scene.width, "x ${point.x} escaped width ${scene.width}")
                assertTrue(point.y >= 0.0 && point.y <= scene.height, "y ${point.y} escaped height ${scene.height}")
            }
        }
        val swatchRows = scene.commands.filterIsInstance<DrawRect>().map { it.rect.y }.distinct()
        assertTrue(swatchRows.size >= 2, "Legend must wrap into multiple rows for many curves")
    }

    @Test
    fun radarWidensCanvasWhenSingleCurveLabelExceedsContentBox() {
        val veryLongLabel = "One single extraordinarily long radar curve legend label that cannot fit the default box"
        val diagram = RadarChartDiagram(
            null,
            listOf(RadarAxis("a", "a"), RadarAxis("b", "b"), RadarAxis("c", "c")),
            listOf(RadarCurve("x", veryLongLabel, listOf(80.0, 70.0, 60.0))),
            100.0,
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertTrue(scene.width > 640.0, "Canvas must widen for an over-wide single legend item")
        // The full measured line box of every text, including start-anchored ones, stays inside.
        scene.commands.filterIsInstance<DrawText>().forEach { command ->
            val textWidth = FixedWidthTextMeasurer.measure(command.text, command.style).width
            val left = when (command.anchor) {
                TextAnchor.MIDDLE -> command.origin.x - textWidth / 2.0
                TextAnchor.END -> command.origin.x - textWidth
                TextAnchor.START -> command.origin.x
            }
            assertTrue(left >= 0.0 && left + textWidth <= scene.width, "Text '${command.text}' escaped [0, ${scene.width}]")
            assertTrue(
                command.origin.y - command.style.fontSize >= 0.0 && command.origin.y + command.style.fontSize * 0.25 <= scene.height,
                "Text '${command.text}' escaped vertical bounds",
            )
        }
    }

    private fun message(
        from: String,
        to: String,
        label: String,
        lineStyle: SequenceLineStyle,
    ): SequenceMessage = SequenceMessage(from, to, label, lineStyle, SequenceArrowHead.FILLED)

    private fun SceneRect.valid(): Boolean = width > 0.0 && height > 0.0
}
