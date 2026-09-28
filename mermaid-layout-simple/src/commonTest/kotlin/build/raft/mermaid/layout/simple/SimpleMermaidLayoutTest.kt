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
import build.raft.mermaid.layout.LayoutScene
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
import build.raft.mermaid.layout.SceneColor
import build.raft.mermaid.layout.ScenePoint
import build.raft.mermaid.layout.SceneRect
import build.raft.mermaid.layout.StrokePattern
import build.raft.mermaid.layout.TextAnchor
import kotlin.test.Test
import build.raft.mermaid.core.MermaidParser
import build.raft.mermaid.core.MermaidParseResult
import kotlin.test.assertIs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimpleMermaidLayoutTest {
    @Test fun infoShowsMeasuredNativeVersionWithoutClipping() {
        val scene = SimpleMermaidLayout.layout(build.raft.mermaid.core.InfoDiagram(version = "123456789.987654321-long-build"), FixedWidthTextMeasurer, LayoutConfig())
        val text = scene.commands.filterIsInstance<DrawText>().single()
        assertEquals("Mermaid Native v123456789.987654321-long-build", text.text)
        assertTrue(scene.width >= FixedWidthTextMeasurer.measure(text.text, text.style).width + 48.0)
        assertEquals(text.text, scene.accessibilityTitle)
    }

    @Test fun architectureEmptyAndMetadataRenderWithoutInvalidGeometry() {
        fun render(source: String) = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, FixedWidthTextMeasurer, LayoutConfig())
        val empty = render("architecture-beta")
        assertEquals(720.0, empty.width); assertEquals(420.0, empty.height); assertTrue(empty.commands.isEmpty())
        val scene = render("architecture-beta title Sample\naccTitle: Accessible\naccDescr: Description\ngroup api(cloud)[API]\nservice db(database)[Database] in api")
        assertEquals("Accessible", scene.accessibilityTitle); assertEquals("Description", scene.accessibilityDescription)
        assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text == "Sample" })
        assertTrue(scene.commands.filterIsInstance<DrawRect>().all { it.rect.y >= 68.0 })
    }

    @Test fun quadrantHexColorsBecomeValidRenderedColors() {
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse("quadrantChart\nA: [0.5, 0.5] color:abc,stroke-color:123456"))
        val scene = SimpleMermaidLayout.layout(parsed.diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertTrue(scene.commands.filterIsInstance<DrawPolygon>().any { it.fill?.value == "#abc" })
        assertTrue(scene.commands.filterIsInstance<DrawPolyline>().any { it.stroke.value == "#123456" })
    }

    @Test fun mindmapBlankIconHasNoHeightOrTextSideEffects() {
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse("mindmap\nroot(Root)"))
        val normal = assertIs<MindmapDiagram>(parsed.diagram)
        val blank = normal.copy(nodes = normal.nodes.map { it.copy(icon = "") })
        assertEquals(SimpleMermaidLayout.layout(normal, FixedWidthTextMeasurer, LayoutConfig()), SimpleMermaidLayout.layout(blank, FixedWidthTextMeasurer, LayoutConfig()))
        val emptyDecoration = assertIs<MermaidParseResult.Success>(MermaidParser.parse("mindmap\nroot(Root)\n::icon()"))
        assertEquals(SimpleMermaidLayout.layout(normal, FixedWidthTextMeasurer, LayoutConfig()), SimpleMermaidLayout.layout(emptyDecoration.diagram, FixedWidthTextMeasurer, LayoutConfig()))
    }

    @Test fun treemapEmptySectionsAndClassesRenderFiniteGeometry() {
        val source = "treemap\ntitle Portfolio\naccTitle: Access\naccDescr: Allocation\nclassDef hot fill:red,stroke:#123456,stroke-width:2px\n\"First\":::hot\n\"Second\""
        val scene = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals("Access", scene.accessibilityTitle)
        assertEquals("Allocation", scene.accessibilityDescription)
        val boxes = scene.commands.filterIsInstance<DrawRect>()
        assertEquals(2, boxes.size)
        assertEquals("#ff0000", boxes[0].fill?.value)
        assertEquals("#123456", boxes[0].stroke?.value)
        assertEquals(2.0, boxes[0].strokeWidth)
        for (box in boxes) assertTrue(box.rect.width.isFinite() && box.rect.width > 0.0 && box.rect.y >= 68.0)
        val empty = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse("treemap")).diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertTrue(empty.commands.isEmpty() && empty.width.isFinite())
    }

    @Test fun sankeyFeedbackLinksHaveDistinctFiniteLanes() {
        val source = "sankey\n__proto__,A,0.597\nA,__proto__,0.403\nA,__proto__,0.2\nA,A,0.1"
        val scene = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, FixedWidthTextMeasurer, LayoutConfig())
        val loops = scene.commands.filterIsInstance<DrawPolyline>()
        assertEquals(3, loops.size)
        assertEquals(3, loops.map { it.points[1].y }.distinct().size)
        for (edge in loops) for (point in edge.points) {
            assertTrue(point.x.isFinite() && point.y.isFinite())
            assertTrue(point.x in 0.0..scene.width && point.y in 0.0..scene.height)
        }
        assertEquals(2, scene.commands.filterIsInstance<DrawRect>().size)
    }

    @Test fun c4NestedBoundariesContainCardsAndServeAsRelationshipEndpoints() {
        val source = "C4Container\nBoundary(bank, \"Bank\") {\nContainerDb(db, \"Ledger\", \"SQL\")\nBoundary(inner, \"Internal\") {\nComponent(worker, \"Worker\", \"Kotlin\")\n}\n}\nPerson(user, \"Customer\")\nRel(bank, user, \"Serves\")"
        val scene = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, FixedWidthTextMeasurer, LayoutConfig())
        val boxes = scene.commands.filterIsInstance<DrawRect>()
        assertEquals(5, boxes.size)
        val outer = boxes.first().rect
        val nested = boxes[1].rect
        assertTrue(nested.x > outer.x && nested.y > outer.y)
        assertTrue(nested.x + nested.width < outer.x + outer.width)
        assertTrue(nested.y + nested.height < outer.y + outer.height)
        assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text == "[SQL]" })
        assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text == "[Component]" })
        val edge = scene.commands.filterIsInstance<DrawLine>().single()
        assertEquals(outer.x, edge.from.x)
        assertTrue(scene.width.isFinite() && scene.height.isFinite())
    }

    @Test fun c4StackedRelationshipTextStaysBetweenCards() {
        val source = """C4Container
            Boundary(app, "Application") {
              Container(a, "First", "Kotlin")
              Container(b, "Second", "SQL")
            }
            Rel(a, b, "Stores", "SQL")
        """.trimIndent()
        val scene = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, FixedWidthTextMeasurer, LayoutConfig())
        val cards = scene.commands.filterIsInstance<DrawRect>().filter { it.fill == SceneColor("#dbeafe") }.map { it.rect }
        val labels = scene.commands.filterIsInstance<DrawText>().filter { it.text == "Stores" || it.text == "[SQL]" && it.origin.y < cards[1].y }
        assertEquals(2, labels.size)
        for (label in labels) {
            assertTrue(label.origin.y - label.style.fontSize > cards[0].y + cards[0].height)
            assertTrue(label.origin.y + 3.0 < cards[1].y)
        }
    }

    @Test fun mindmapOriginalShapesDrawDistinctGeometryWithMeasuredIconText() {
        val source = "mindmap\nroot(Root)\n cloud)Cloud(\n ::icon(star)\n bang))Burst((\n hex{{Hexagon}}"
        val scene = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(3, scene.commands.filterIsInstance<DrawPolygon>().size)
        assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text == "star" })
        for (polygon in scene.commands.filterIsInstance<DrawPolygon>()) for (point in polygon.points) {
            assertTrue(point.x >= 0.0 && point.x <= scene.width)
            assertTrue(point.y >= 0.0 && point.y <= scene.height)
        }
        val tallRoot = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse("mindmap\nroot((Long round root))\n child")).diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertTrue(tallRoot.commands.filterIsInstance<DrawEllipse>().all { it.center.y - it.radiusY >= 24.0 })
        val longText = "A".repeat(100)
        val longScene = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse("mindmap\nroot))$longText((")).diagram, FixedWidthTextMeasurer, LayoutConfig())
        val outline = longScene.commands.filterIsInstance<DrawPolygon>().single()
        val measured = FixedWidthTextMeasurer.measure(longText, build.raft.mermaid.layout.TextStyle()).width
        assertTrue((outline.points.maxOf { it.x } - outline.points.minOf { it.x }) * 0.80 >= measured + 32.0)
    }

    @Test fun packetAdjacentBitIndicesHaveAReadableGapAndSingleBitsAreNotDuplicated() {
        val diagram = assertIs<MermaidParseResult.Success>(MermaidParser.parse("""packet
+8: "Version"
+8: "Flags"
+1: "Flag"
""")).diagram
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        val indices = scene.commands.filterIsInstance<DrawText>().filter { it.style.fontSize == 9.0 }
        assertEquals(8.0, indices.single { it.text == "8" }.origin.x - indices.single { it.text == "7" }.origin.x)
        assertEquals(1, indices.count { it.text == "16" })
        assertEquals(TextAnchor.MIDDLE, indices.single { it.text == "16" }.anchor)
    }

    @Test
    fun emptyPacketAndRelativeMultirowFieldsUseActualPacketRenderer() {
        val empty = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse("packet")).diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertTrue(empty.width.isFinite() && empty.height.isFinite())
        assertTrue(empty.commands.isEmpty())
        val source = "packet\naccTitle: Frame\n+8: \"Header\"\n+64: \"Payload\""
        val scene = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals("Frame", scene.accessibilityTitle)
        assertEquals(4, scene.commands.filterIsInstance<DrawRect>().size)
        assertEquals(3, scene.commands.filterIsInstance<DrawText>().count { it.text == "Payload" })
    }

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
    fun timelineExplicitDirectionsChangeActualEventPositions() {
        fun scene(direction: String) = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "timeline $direction\nsection Phase\nFirst: Alpha\nSecond: Beta",
        )).diagram, FixedWidthTextMeasurer, LayoutConfig())
        val lr = scene("LR").commands.filterIsInstance<DrawPolygon>()
        val td = scene("TD").commands.filterIsInstance<DrawPolygon>()
        assertTrue(lr[1].points.first().x > lr[0].points.first().x)
        assertEquals(lr[0].points.first().y, lr[1].points.first().y)
        assertEquals(td[0].points.first().x, td[1].points.first().x)
        assertTrue(td[1].points.first().y > td[0].points.first().y)
        assertEquals("#2563eb", lr.first().fill.value)
    }

    @Test
    fun timelinePreservesInterveningAndRepeatedEmptySectionsWithMeasuredHeaders() {
        val name = "Wide section heading ".repeat(8)
        val measurer = build.raft.mermaid.layout.TextMeasurer { text, style ->
            val size = FixedWidthTextMeasurer.measure(text, style)
            size.copy(width = size.width * if (style.fontWeight >= 600) 1.7 else 1.0)
        }
        for (direction in listOf("LR", "TD")) {
            val chart = assertIs<MermaidParseResult.Success>(MermaidParser.parse(
                "timeline $direction\nsection Start\nFirst: Alpha\nsection $name\nsection Finish\nSecond: Beta\nsection Finish",
            )).diagram
            val scene = SimpleMermaidLayout.layout(chart, measurer, LayoutConfig())
            val texts = scene.commands.filterIsInstance<DrawText>()
            val empty = texts.single { it.text == name }
            val first = texts.single { it.text == "First" }; val last = texts.single { it.text == "Second" }
            assertEquals(2, texts.count { it.text == "Finish" })
            if (direction == "LR") {
                assertTrue(empty.origin.x > first.origin.x && empty.origin.x < last.origin.x)
                val half = measurer.measure(empty.text, empty.style).width / 2
                assertTrue(empty.origin.x - half >= 0 && empty.origin.x + half <= scene.width)
            } else assertTrue(empty.origin.y > first.origin.y && empty.origin.y < last.origin.y)
            assertEquals(2, scene.commands.filterIsInstance<DrawPolygon>().size)
        }
    }

    @Test
    fun timelineNextSectionClearsThePreviousMultilineEvent() {
        val chart = assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "timeline TD\nsection Planning\n2024: Launch: First users\nsection Research\nsection Delivery\n2025: Scale",
        )).diagram
        val scene = SimpleMermaidLayout.layout(chart, FixedWidthTextMeasurer, LayoutConfig())
        val texts = scene.commands.filterIsInstance<DrawText>()
        val lastEvent = texts.single { it.text == "First users" }
        val nextSection = texts.single { it.text == "Research" }
        assertTrue(nextSection.origin.y - nextSection.style.fontSize >= lastEvent.origin.y + 12.0)
    }

    @Test
    fun timelineEmptySectionsRenderWithoutCrashing() {
        val scene = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "timeline\nsection Planning\nsection Delivery",
        )).diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertTrue(scene.commands.filterIsInstance<DrawText>().map { it.text }.containsAll(listOf("Planning", "Delivery")))
    }

    @Test
    fun xyHorizontalPlotUsesRealHorizontalBarsLabelsAndPalette() {
        val diagram = assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "xychart horizontal\nx-axis [A,B]\ny-axis 0 --> 100\nbar \"Sales\" [20 \"First\", 80 \"Last\"]\nline [30,70]",
        )).diagram
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        val bars = scene.commands.filterIsInstance<DrawRect>()
        assertEquals(2, bars.size)
        assertEquals(bars[0].rect.x, bars[1].rect.x)
        assertTrue(bars[1].rect.y > bars[0].rect.y)
        assertTrue(bars[1].rect.width > bars[0].rect.width * 3)
        assertEquals("#2563eb", bars.first().fill.value)
        assertTrue(scene.commands.filterIsInstance<DrawText>().map { it.text }.containsAll(listOf("Sales", "First", "Last")))
        val line = scene.commands.filterIsInstance<DrawPolyline>().single()
        assertTrue(line.points[1].x > line.points[0].x && line.points[1].y > line.points[0].y)
    }

    @Test
    fun xyNumericAxesRenderDescendingRangesAndEmptyChartsWithoutInvalidCoordinates() {
        listOf("xychart", "xychart\nx-axis 45 --> 5\ny-axis 100 --> 0\nline [10,40,80]").forEach { source ->
            val diagram = assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram
            val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
            scene.commands.filterIsInstance<DrawLine>().forEach { line ->
                assertTrue(line.from.x.isFinite() && line.from.y.isFinite() && line.to.x.isFinite() && line.to.y.isFinite())
            }
            if(source.contains("45")) {
                val line = scene.commands.filterIsInstance<DrawPolyline>().single()
                assertTrue(line.points.last().x > line.points.first().x)
                assertTrue(line.points.last().y > line.points.first().y)
                assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text == "45" })
            }
        }
    }

    @Test
    fun xyNamedSeriesWrapBelowTitleWithoutOverlappingAxisTitle() {
        for (orientation in listOf("vertical", "horizontal")) {
            val names = listOf("Actual revenue for the first period", "Expected revenue for the next period", "Forecast")
            val source = "xychart $orientation\ntitle \"Sales by quarter\"\nx-axis \"Quarter\" [Q1,Q2]\ny-axis \"Revenue\" 0 --> 100\n" +
                names.joinToString("\n") { "bar \"$it\" [20,80]" }
            val scene = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram,
                FixedWidthTextMeasurer, LayoutConfig())
            val texts = scene.commands.filterIsInstance<DrawText>()
            val title = texts.single { it.text == "Sales by quarter" }
            val legend = texts.filter { it.text in names }
            val axisTitle = texts.single { it.text == if (orientation == "horizontal") "Quarter" else "Revenue" }
            assertTrue(legend.all { it.origin.y - it.style.fontSize > title.origin.y })
            assertTrue(legend.all { it.origin.y + 8.0 < axisTitle.origin.y })
            assertTrue(legend.map { it.origin.y }.distinct().size > 1)
            legend.forEach { assertTrue(it.origin.x + FixedWidthTextMeasurer.measure(it.text, it.style).width <= scene.width) }
            legend.groupBy { it.origin.y }.values.forEach { row -> row.zipWithNext().forEach { (left, right) ->
                assertTrue(left.origin.x + FixedWidthTextMeasurer.measure(left.text, left.style).width + 20 <= right.origin.x)
            } }
        }
    }

    @Test
    fun xyBandAxisTruncatesExtraValuesBeforeInferringRangeAndPlacement() {
        fun render(values: String) = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "xychart\nx-axis [A,B]\nbar [$values]",
        )).diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(render("10,20"), render("10,20,999"))
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
    fun erStylesChangeActualDrawingAndTextMeasurement() {
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "erDiagram\nA:::large { int id PK }\nclassDef default fill:#f9f\nclassDef large color:red,font-size:24px,font-weight:bold\nstyle A stroke:blue",
        ))
        val scene = SimpleMermaidLayout.layout(parsed.diagram, FixedWidthTextMeasurer, LayoutConfig())
        val box = scene.commands.filterIsInstance<DrawRect>().single()
        assertEquals("#f9f", box.fill.value)
        assertEquals("#0000ff", box.stroke.value)
        scene.commands.filterIsInstance<DrawText>().forEach {
            assertEquals("#ff0000", it.style.color.value)
            assertEquals(24.0, it.style.fontSize)
            assertEquals(700, it.style.fontWeight)
        }
        assertTrue(box.rect.height > 58.0)
    }

    @Test
    fun erManyMarkerBranchesMeetEntityAndParentUsesDiamond() {
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse("erDiagram\nA u--o{ B : has"))
        val scene = SimpleMermaidLayout.layout(parsed.diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(1, scene.commands.filterIsInstance<DrawPolygon>().size)
        val target = scene.commands.filterIsInstance<DrawRect>()[1].rect
        val endpointLines = scene.commands.filterIsInstance<DrawLine>().filter { it.to.y == target.y }
        assertEquals(3, endpointLines.size)
        assertEquals(3, endpointLines.map { it.to.x }.distinct().size)
    }

    @Test
    fun parsedEntityAliasesMultipleKeysAndDashedRelationshipsReachDrawing() {
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "erDiagram\nA[\"Customer accounts\"] { int id PK, FK }\nA ||..o{ B : has",
        ))
        val scene = SimpleMermaidLayout.layout(parsed.diagram, FixedWidthTextMeasurer, LayoutConfig())
        val labels = scene.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue("Customer accounts" in labels)
        assertTrue("A" !in labels)
        assertTrue("PK, FK" in labels)
        assertEquals(1, scene.commands.filterIsInstance<DrawLine>().count { it.pattern == StrokePattern.DASHED })
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
    fun journeyRendersEmptySectionsAndOptionalActorsWithoutPhantomLegend() {
        for (source in listOf("journey", "journey\ntitle Only title", "journey\nsection Empty")) {
            val diagram = assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram
            val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
            assertTrue(scene.width.isFinite() && scene.height.isFinite())
            if (source.contains("Empty")) assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text == "Empty" })
        }
        val chart = assertIs<MermaidParseResult.Success>(MermaidParser.parse("journey\nsection Work\nA: 5\nB: 3:\nC: -2" )).diagram
        val scene = SimpleMermaidLayout.layout(chart, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(15, scene.commands.filterIsInstance<DrawEllipse>().size)
        assertTrue(scene.commands.filterIsInstance<DrawText>().none { "·" in it.text })
        assertEquals(listOf("#bbf7d0", "#fef3c7", "#fee2e2"), scene.commands.filterIsInstance<DrawRect>().drop(1).map { it.fill.value })
    }

    @Test
    fun sectionlessJourneyStartsAtPaddingWithoutInventingSectionCard() {
        val diagram = assertIs<MermaidParseResult.Success>(MermaidParser.parse("journey\nTask: 5")).diagram
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        val cards = scene.commands.filterIsInstance<DrawRect>()
        assertEquals(1, cards.size)
        assertEquals(LayoutConfig().padding, cards.single().rect.x)
        assertEquals("#bbf7d0", cards.single().fill.value)
        assertTrue(scene.commands.filterIsInstance<DrawText>().none { it.text.isBlank() })
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
    fun gitGraphVerticalDirectionsPreserveLabelsOrderAndMeasuredBounds() {
        val source = "gitGraph TB:\ncommit id: \"base\" msg: \"Initial work\" tag: \"v1\" tag: \"stable\"\nbranch feature order: 2\ncommit id: \"next\""
        fun scene(source: String): LayoutScene = SimpleMermaidLayout.layout(
            assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram,
            FixedWidthTextMeasurer, LayoutConfig(),
        )
        val top = scene(source)
        val bottom = scene(source.replace("TB", "BT"))
        fun y(scene: LayoutScene, id: String) = scene.commands.filterIsInstance<DrawText>().first { it.text == id }.origin.y
        assertTrue(y(top, "base") < y(top, "next"))
        assertTrue(y(bottom, "base") > y(bottom, "next"))
        for (rendered in listOf(top, bottom)) {
            val texts = rendered.commands.filterIsInstance<DrawText>()
            assertTrue(texts.map { it.text }.containsAll(listOf("Initial work", "v1, stable", "main", "feature")))
            texts.forEach { assertTrue(it.origin.x + FixedWidthTextMeasurer.measure(it.text, it.style).width <= rendered.width) }
            assertEquals("#2563eb", rendered.commands.filterIsInstance<DrawRect>().first().fill.value)
            if (rendered == top) assertTrue(texts.first { it.text == "v1, stable" }.origin.y - texts.first { it.text == "main" }.origin.y >= 30.0)
        }
    }

    @Test
    fun gitGraphEmptyAndCherryPickRenderThroughRealParser() {
        val source = "gitGraph\ncommit id: \"base\"\nbranch feature\ncommit id: \"work\"\ncheckout main\ncherry-pick id: \"work\""
        val diagram = assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text == "cherry-pick:work" })
        assertTrue(scene.commands.filterIsInstance<DrawPolygon>().any { it.fill.value == "#2563eb" })
        val empty = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse("gitGraph")).diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertTrue(empty.width.isFinite() && empty.height.isFinite())
        assertTrue(empty.commands.filterIsInstance<DrawText>().any { it.text == "main" })
    }

    @Test
    fun gitGraphReplacementCommitDrawsAfterEarlierSequence() {
        for (header in listOf("gitGraph", "gitGraph TB:")) {
            val diagram = assertIs<MermaidParseResult.Success>(MermaidParser.parse(
                header + "\ncommit id: \"first\"\ncommit id: \"second\"\ncommit id: \"first\"",
            )).diagram
            val texts = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()).commands.filterIsInstance<DrawText>()
            val first = texts.first { it.text == "first" }.origin
            val second = texts.first { it.text == "second" }.origin
            if (header == "gitGraph") assertTrue(first.x > second.x) else assertTrue(first.y > second.y)
        }
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
    fun gitGraphConnectorsStayBehindEarlierCommitBodiesAndLabels() {
        val diagram = GitGraphDiagram(
            listOf(GitGraphBranch("main", null), GitGraphBranch("feature", "base")),
            listOf(
                GitGraphCommit("base", "main", emptyList()),
                GitGraphCommit("work", "feature", listOf("base"), GitGraphCommitType.HIGHLIGHT),
                GitGraphCommit("merge", "main", listOf("base", "work"), isMerge = true),
                GitGraphCommit("revert", "main", listOf("merge"), GitGraphCommitType.REVERSE),
            ),
        )
        val commands = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()).commands
        val lastConnector = commands.indexOfLast { it is DrawLine && it.stroke.value != "#ffffff" }
        val firstMarker = commands.indexOfFirst { it is DrawEllipse }
        assertTrue(lastConnector < firstMarker, "A later edge must not paint over an earlier commit")
        assertTrue(commands.indexOfFirst { it is DrawText && it.text == "base" } > lastConnector)
        // Revert's white cross belongs to the node decoration and must remain on top.
        assertTrue(commands.indexOfFirst { it is DrawLine && it.stroke.value == "#ffffff" } > firstMarker)
    }

    @Test
    fun treeViewSiblingTrunksStayBehindAllNodeDots() {
        val diagram = TreeViewDiagram(listOf(
            TreeViewNode("root", 0, null, true),
            TreeViewNode("first", 1, 0, true),
            TreeViewNode("leaf", 2, 1, false),
            TreeViewNode("second", 1, 0, false),
        ))
        val commands = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()).commands
        assertTrue(commands.indexOfLast { it is DrawPolyline } < commands.indexOfFirst { it is DrawEllipse })
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

    @Test fun kanbanEmptyColumnsAndMetadataUseActualMeasuredGeometry() {
        val source = "kanban\ntodo[Todo]\n  work[Implement]@{priority: high, assigned: Ada, ticket: K-10}\ndone[Done]"
        val diagram = (MermaidParser.parse(source) as MermaidParseResult.Success).diagram
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertTrue(scene.width.isFinite() && scene.height.isFinite())
        val texts = scene.commands.filterIsInstance<DrawText>()
        val detail = texts.single { it.text == "high · @Ada · K-10" }
        val label = texts.single { it.text == "Implement" }
        assertTrue(detail.origin.y > label.origin.y)
        val card = scene.commands.filterIsInstance<DrawRect>().single { it.fill == build.raft.mermaid.layout.SceneColor("#ffffff") }
        assertTrue(detail.origin.y < card.rect.y + card.rect.height)
        assertTrue(detail.origin.x + FixedWidthTextMeasurer.measure(detail.text, detail.style).width < card.rect.x + card.rect.width)
        assertTrue(texts.any { it.text == "Done" })
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
        val ellipses = first.commands.filterIsInstance<DrawEllipse>()
        assertTrue(ellipses[2].center.y < ellipses[0].center.y)
        assertTrue(ellipses[2].center.y < ellipses[1].center.y)
        assertEquals("#60a5fa", ellipses[0].fill.value)
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
    fun ganttFinalTicksDoNotOverlapAndThirtyDayBoundaryKeepsSameWidth() {
        val scenes = listOf(30, 31, 32).map { days ->
            SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(
                "gantt\ndateFormat YYYY-MM-DD\nTask :2024-01-01, ${days}d",
            )).diagram, FixedWidthTextMeasurer, LayoutConfig())
        }
        assertEquals(scenes[0].width, scenes[1].width)
        assertEquals(scenes[1].width, scenes[2].width)
        scenes.forEach { scene ->
            val ticks = scene.commands.filterIsInstance<DrawText>().filter { it.text.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")) }
            ticks.zipWithNext().forEach { (left, right) ->
                val halfWidths = (FixedWidthTextMeasurer.measure(left.text, left.style).width + FixedWidthTextMeasurer.measure(right.text, right.style).width) / 2
                assertTrue(right.origin.x - left.origin.x >= halfWidths + 7.9)
            }
        }
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
    fun quadrantMeasuresLongYAxisLabelsBeforePlacingPlot() {
        val label = "An extraordinarily long engagement label"
        val source = "quadrantChart\ny-axis $label --> $label\nCampaign: [1,0]"
        val scene = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, FixedWidthTextMeasurer, LayoutConfig())
        val labels = scene.commands.filterIsInstance<DrawText>().filter { it.text == label }
        assertEquals(2, labels.size)
        labels.forEach { assertTrue(it.origin.x - FixedWidthTextMeasurer.measure(it.text, it.style).width >= 24.0) }
        assertEquals(TextAnchor.END, scene.commands.filterIsInstance<DrawText>().single { it.text == "Campaign" }.anchor)
    }

    @Test
    fun quadrantPointClassAndInlineStylesReachActualGeometry() {
        val source = "quadrantChart\naccTitle: Portfolio\nclassDef special radius:10,color:#ff0000\nCampaign:::special: [0.5,0.5] color:#00ff00,stroke-color:#ff00ff,stroke-width:2px"
        val scene = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, FixedWidthTextMeasurer, LayoutConfig())
        val marker = scene.commands.filterIsInstance<DrawPolygon>().single()
        assertEquals("#00ff00", marker.fill.value)
        assertEquals(20.0, marker.points.maxOf { it.y } - marker.points.minOf { it.y })
        assertTrue(scene.commands.filterIsInstance<DrawPolyline>().any { it.stroke.value == "#ff00ff" && it.strokeWidth == 2.0 })
        assertEquals("Portfolio", scene.accessibilityTitle)
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
        assertEquals(4, first.commands.filterIsInstance<DrawRect>().size)
        assertTrue(first.commands.filterIsInstance<DrawText>().any { it.text == "High engagement" })
    }

    @Test
    fun quadrantChartDrawsFilledQuadrantsAndCenteredLabels() {
        val diagram = QuadrantChartDiagram(
            "Portfolio",
            QuadrantAxis("Low reach", "High reach"),
            QuadrantAxis("Low engagement", "High engagement"),
            listOf("Expand", "Promote", "Re-evaluate", "Improve"),
            listOf(QuadrantPoint("A", 0.25, 0.75)),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(scene, SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        val fills = scene.commands.filterIsInstance<DrawRect>().map { it.fill.value }
        assertEquals(listOf("#dbeafe", "#dcfce7", "#fef3c7", "#fce7f3"), fills)
        val expand = scene.commands.filterIsInstance<DrawText>().first { it.text == "Expand" }
        assertEquals(TextAnchor.MIDDLE, expand.anchor)
        assertEquals("#2563eb", scene.commands.filterIsInstance<DrawPolygon>().first().fill.value)
    }

    @Test
    fun radarNamedEntriesUseAxisIdentityAndOptionsReachActualDrawing() {
        val source = "radar-beta\naxis a, b, c\ncurve Sample{c 20, a 50, b 35}\nmin 20, max 50, ticks 3, graticule polygon, showLegend false"
        val diagram = assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals(3, scene.commands.filterIsInstance<DrawPolyline>().count { it.stroke.value == "#d4d4d4" })
        assertTrue(scene.commands.filterIsInstance<DrawEllipse>().isEmpty())
        assertTrue(scene.commands.filterIsInstance<DrawText>().none { it.text == "Sample" })
        assertTrue(scene.commands.filterIsInstance<DrawText>().map { it.text }.containsAll(listOf("30", "40", "50")))
        val marker = scene.commands.filterIsInstance<DrawPolygon>().first { it.fill.value == "#2563eb" }
        assertEquals(98.0, marker.points.first().y)
        assertTrue(scene.commands.filterIsInstance<DrawPolygon>().any { it.fill.value == "#dbeafe" })
        val lastCurve = scene.commands.indexOfLast { it is DrawPolygon }
        assertTrue(scene.commands.indexOfFirst { it is DrawText && it.text == "30" } > lastCurve)
        assertTrue(scene.commands.indexOfFirst { it is DrawText && it.text == "40" } > lastCurve)
    }

    @Test
    fun radarCapsActualGridAtOriginalLimitAndUsesFirstNamedEntry() {
        for ((requested, expected) in listOf(0 to 0, 12 to 12, 32 to 32, 33 to 32, 1000000 to 32)) {
            val source = "radar-beta\naxis a,b,c\ncurve c{a 50, a 0, b 35, c 20}\nmin 20\nmax 50\nticks $requested"
            val scene = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, FixedWidthTextMeasurer, LayoutConfig())
            assertEquals(expected, scene.commands.filterIsInstance<DrawEllipse>().size)
            val firstMarker = scene.commands.filterIsInstance<DrawPolygon>().first { it.fill.value == "#2563eb" }
            assertEquals(98.0, firstMarker.points.first().y)
        }
    }

    @Test
    fun radarEmptyAndShortAxisGraphsRenderWithoutInvalidGeometry() {
        for (source in listOf("radar-beta", "radar-beta\ncurve c{1}", "radar-beta\naxis a\ncurve c{0}\nmax 0", "radar-beta\naxis a,b\ncurve c{1,2,3}")) {
            val scene = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, FixedWidthTextMeasurer, LayoutConfig())
            assertTrue(scene.width.isFinite() && scene.height.isFinite())
            scene.commands.filterIsInstance<DrawPolygon>().flatMap { it.points }.forEach { assertTrue(it.x.isFinite() && it.y.isFinite()) }
        }
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
    @Test fun treeViewEmptyAndMeasuredAnnotationsStayFinite() {
        fun render(source: String) = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertTrue(render("treeView-beta").commands.isEmpty())
        val scene = render("treeView-beta\ntitle Project\naccTitle: Accessible\naccDescr: Files\nroot/\n  file.ts icon(logos:react) ## Entry point with long description")
        assertEquals("Accessible", scene.accessibilityTitle); assertEquals("Files", scene.accessibilityDescription)
        val detail = scene.commands.filterIsInstance<DrawText>().single { it.text.contains("Entry point") }
        assertTrue(detail.origin.x + FixedWidthTextMeasurer.measure(detail.text, detail.style).width <= scene.width)
        assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text == "Project" })
    }

    @Test
    fun railroadEmptyGrammarPreservesTitleAndAccessibility() {
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse("railroad-beta\ntitle Empty grammar\naccTitle: Grammar overview\naccDescr: No rules yet"))
        val scene = SimpleMermaidLayout.layout(parsed.diagram, FixedWidthTextMeasurer, LayoutConfig())
        assertEquals("Grammar overview", scene.accessibilityTitle)
        assertEquals("No rules yet", scene.accessibilityDescription)
        assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text == "Empty grammar" })
        assertTrue(scene.width > 0 && scene.height > 0)
    }
    @Test fun vennSingleSetStylesAndNotesRenderWithoutEmptyCrash() {
        fun render(source: String) = SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, FixedWidthTextMeasurer, LayoutConfig())
        val scene = render("venn-beta\nset A[Alpha]\n  text note[Visible note]\nstyle A fill:rgb(255, 0, 128)\nstyle note color:red")
        assertEquals(SceneColor("#ff0080"), scene.commands.filterIsInstance<DrawEllipse>().single().fill)
        assertEquals(SceneColor("#ff0000"), scene.commands.filterIsInstance<DrawText>().single { it.text == "Visible note" }.style.color)
        assertTrue(render("venn-beta").commands.isEmpty())
    }
    @Test fun blockCompositeLayoutContainsChildrenStylesAndSpace() {
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse("""
            block
            columns 1
            block:g["Services"]
              columns 2
              A["API"]
              B["Database"]
              space
              next<["Go"]>(right)
            end
            style A fill:#ff0000
        """.trimIndent()))
        val scene = SimpleMermaidLayout.layout(parsed.diagram, FixedWidthTextMeasurer, LayoutConfig())
        val rects = scene.commands.filterIsInstance<DrawRect>()
        val parent = rects.first().rect
        assertEquals(3, rects.size)
        assertTrue(rects.drop(1).all { it.rect.x >= parent.x && it.rect.y >= parent.y && it.rect.x + it.rect.width <= parent.x + parent.width && it.rect.y + it.rect.height <= parent.y + parent.height })
        assertTrue(rects.any { it.fill.value == "#ff0000" })
        assertTrue(scene.commands.filterIsInstance<DrawText>().any { it.text == "Go" })
        assertEquals(1, scene.commands.filterIsInstance<DrawPolygon>().size)
        val outline = scene.commands.filterIsInstance<DrawPolyline>().single()
        assertEquals(outline.points.first(), outline.points.last())
        val empty = SimpleMermaidLayout.layout(BlockDiagram(-1, emptyList(), emptyList()), FixedWidthTextMeasurer, LayoutConfig())
        assertTrue(empty.width.isFinite() && empty.height.isFinite())
    }

}
