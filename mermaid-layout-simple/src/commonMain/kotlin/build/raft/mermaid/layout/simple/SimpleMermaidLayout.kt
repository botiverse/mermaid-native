package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.FlowDirection
import build.raft.mermaid.core.FlowchartDiagram
import build.raft.mermaid.core.ClassDiagram
import build.raft.mermaid.core.ClassVisibility
import build.raft.mermaid.core.ClassRelationshipKind
import build.raft.mermaid.core.EntityCardinality
import build.raft.mermaid.core.EntityRelationshipDiagram
import build.raft.mermaid.core.EntityKey
import build.raft.mermaid.core.MermaidDiagram
import build.raft.mermaid.core.MindmapDiagram
import build.raft.mermaid.core.MindmapNodeShape
import build.raft.mermaid.core.TimelineDiagram
import build.raft.mermaid.core.UserJourneyDiagram
import build.raft.mermaid.core.SequenceDiagram
import build.raft.mermaid.core.PieDiagram
import build.raft.mermaid.core.SequenceLineStyle
import build.raft.mermaid.core.StateDiagram
import build.raft.mermaid.core.StateNodeKind
import build.raft.mermaid.core.XyChartDiagram
import build.raft.mermaid.core.XySeriesKind
import build.raft.mermaid.core.GanttDiagram
import build.raft.mermaid.core.GanttTaskStatus
import build.raft.mermaid.core.QuadrantChartDiagram
import build.raft.mermaid.core.GitGraphCommitType
import build.raft.mermaid.core.GitGraphDiagram
import build.raft.mermaid.core.RequirementDiagram
import build.raft.mermaid.core.RequirementRelationshipKind
import build.raft.mermaid.core.KanbanDiagram
import build.raft.mermaid.core.PacketDiagram
import build.raft.mermaid.core.BlockDiagram
import build.raft.mermaid.core.SankeyDiagram
import build.raft.mermaid.core.TreemapDiagram
import build.raft.mermaid.core.TreemapNode
import build.raft.mermaid.core.VennDiagram
import build.raft.mermaid.core.UsecaseDiagram
import build.raft.mermaid.core.UsecaseShape
import build.raft.mermaid.core.ArchitectureDiagram
import build.raft.mermaid.core.ArchitecturePort
import build.raft.mermaid.core.C4Diagram
import build.raft.mermaid.core.C4ElementKind
import build.raft.mermaid.core.CynefinDiagram
import build.raft.mermaid.core.CynefinDomain
import build.raft.mermaid.core.SwimlaneDiagram
import build.raft.mermaid.core.SwimlaneNodeShape
import build.raft.mermaid.core.TreeViewDiagram
import build.raft.mermaid.core.RailroadChoice
import build.raft.mermaid.core.RailroadDiagram
import build.raft.mermaid.core.ZenumlDiagram
import build.raft.mermaid.core.WardleyEvolution
import build.raft.mermaid.core.WardleyLink
import build.raft.mermaid.core.WardleyMapDiagram
import build.raft.mermaid.core.WardleyNode
import build.raft.mermaid.core.WardleyNote
import build.raft.mermaid.core.ZenumlAsyncMessage
import build.raft.mermaid.core.ZenumlMessage
import build.raft.mermaid.core.ZenumlSyncMessage
import build.raft.mermaid.core.RailroadEnd
import build.raft.mermaid.core.RailroadNode
import build.raft.mermaid.core.RailroadNonTerminal
import build.raft.mermaid.core.RailroadOneOrMore
import build.raft.mermaid.core.RailroadOptional
import build.raft.mermaid.core.RailroadSequence
import build.raft.mermaid.core.RailroadSkip
import build.raft.mermaid.core.RailroadStack
import build.raft.mermaid.core.RailroadStart
import build.raft.mermaid.core.RailroadTerminal
import build.raft.mermaid.core.RailroadZeroOrMore
import build.raft.mermaid.layout.DiagramLayout
import build.raft.mermaid.layout.DrawCommand
import build.raft.mermaid.layout.DrawEllipse
import build.raft.mermaid.layout.DrawLine
import build.raft.mermaid.layout.DrawPolygon
import build.raft.mermaid.layout.DrawPolyline
import build.raft.mermaid.layout.DrawRect
import build.raft.mermaid.layout.DrawText
import build.raft.mermaid.layout.LayoutConfig
import build.raft.mermaid.layout.LayoutScene
import build.raft.mermaid.layout.ScenePoint
import build.raft.mermaid.layout.SceneColor
import build.raft.mermaid.layout.SceneRect
import build.raft.mermaid.layout.SceneSize
import build.raft.mermaid.layout.StrokePattern
import build.raft.mermaid.layout.TextAnchor
import build.raft.mermaid.layout.TextMeasurer
import build.raft.mermaid.layout.TextStyle
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.round

/** Deterministic text metrics for goldens and hosts without platform font metrics. */
public object FixedWidthTextMeasurer : TextMeasurer {
    override fun measure(text: String, style: TextStyle): SceneSize = SceneSize(
        width = text.length * style.fontSize * 0.6,
        height = style.fontSize * 1.2,
    )
}

private const val PACKET_BITS_PER_ROW: Int = 32

/** Small deterministic layout with no DOM, JavaScript, ELK, or platform state. */
public object SimpleMermaidLayout : DiagramLayout {
    override fun layout(
        diagram: MermaidDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene = when (diagram) {
        is FlowchartDiagram -> layoutFlowchart(diagram, textMeasurer, config)
        is SequenceDiagram -> layoutSequence(diagram, textMeasurer, config)
        is PieDiagram -> layoutPie(diagram, textMeasurer, config)
        is StateDiagram -> layoutState(diagram, textMeasurer, config)
        is ClassDiagram -> layoutClass(diagram, textMeasurer, config)
        is EntityRelationshipDiagram -> layoutEntityRelationship(diagram, textMeasurer, config)
        is XyChartDiagram -> layoutXyChart(diagram, config)
        is MindmapDiagram -> layoutMindmap(diagram, textMeasurer, config)
        is GanttDiagram -> layoutGantt(diagram, textMeasurer, config)
        is TimelineDiagram -> layoutTimeline(diagram, textMeasurer, config)
        is QuadrantChartDiagram -> layoutQuadrantChart(diagram, config)
        is UserJourneyDiagram -> layoutUserJourney(diagram, textMeasurer, config)
        is GitGraphDiagram -> layoutGitGraph(diagram, textMeasurer, config)
        is RequirementDiagram -> layoutRequirement(diagram, textMeasurer, config)
        is KanbanDiagram -> layoutKanban(diagram, textMeasurer, config)
        is PacketDiagram -> layoutPacket(diagram, textMeasurer, config)
        is BlockDiagram -> layoutBlock(diagram, textMeasurer, config)
        is SankeyDiagram -> layoutSankey(diagram, textMeasurer, config)
        is TreemapDiagram -> layoutTreemap(diagram, textMeasurer, config)
        is VennDiagram -> layoutVenn(diagram, textMeasurer, config)
        is UsecaseDiagram -> layoutUsecase(diagram, textMeasurer, config)
        is ArchitectureDiagram -> layoutArchitecture(diagram, textMeasurer, config)
        is C4Diagram -> layoutC4(diagram, textMeasurer, config)
        is CynefinDiagram -> layoutCynefin(diagram, textMeasurer, config)
        is SwimlaneDiagram -> layoutSwimlane(diagram, textMeasurer, config)
        is TreeViewDiagram -> layoutTreeView(diagram, textMeasurer, config)
        is RailroadDiagram -> layoutRailroad(diagram, textMeasurer, config)
        is ZenumlDiagram -> layoutZenuml(diagram, textMeasurer, config)
        is WardleyMapDiagram -> layoutWardleyMap(diagram, textMeasurer, config)
    }

    /** Deterministic sequence-style layout for the bounded zenuml slice. */
    private fun layoutZenuml(
        diagram: ZenumlDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene {
        val style = TextStyle()
        val titleStyle = TextStyle(fontSize = 18.0, fontWeight = 600)
        val actorHeight = 40.0
        var cursorY = config.padding
        val commands = mutableListOf<DrawCommand>()
        diagram.title?.let { title ->
            commands += DrawText(title, ScenePoint(config.padding, cursorY + titleStyle.fontSize), style = titleStyle)
            cursorY += 30.0
        }
        val actorWidths = diagram.participants.associate { participant ->
            participant.id to max(88.0, textMeasurer.measure(participant.label, style).width + 32.0)
        }
        val centers = linkedMapOf<String, Double>()
        var cursorX = config.padding
        diagram.participants.forEach { participant ->
            val actorWidth = actorWidths.getValue(participant.id)
            centers[participant.id] = cursorX + actorWidth / 2
            cursorX += actorWidth + config.nodeGap
        }
        val width = max(config.padding * 2, cursorX - config.nodeGap + config.padding)
        val actorTop = cursorY
        val messageTop = actorTop + actorHeight + 40.0
        val messageRows = diagram.messages.sumOf { if (it.from == it.to) 2L else 1L }.toInt()
        val height = messageTop + max(1, messageRows) * config.messageGap + config.padding
        diagram.participants.forEach { participant ->
            val center = centers.getValue(participant.id)
            commands += DrawLine(
                ScenePoint(center, actorTop + actorHeight),
                ScenePoint(center, height - config.padding),
                pattern = StrokePattern.DASHED,
            )
        }
        var messageY = messageTop
        diagram.messages.forEach { message ->
            val fromX = centers.getValue(message.from)
            val toX = centers.getValue(message.to)
            val label = when (message) {
                is ZenumlSyncMessage -> "${message.method}()"
                is ZenumlAsyncMessage -> message.label
            }
            if (fromX == toX) {
                val loopRight = minOf(width - config.padding, fromX + 48.0)
                val endY = messageY + 24.0
                val points = listOf(ScenePoint(fromX, messageY), ScenePoint(loopRight, messageY), ScenePoint(loopRight, endY), ScenePoint(fromX, endY))
                commands += DrawPolyline(points)
                commands += arrowHead(points[points.lastIndex - 1], points.last())
                commands += DrawText(label, ScenePoint(fromX + 8.0, messageY - 8.0), style = style)
                messageY += config.messageGap * 2
            } else {
                val from = ScenePoint(fromX, messageY)
                val to = ScenePoint(toX, messageY)
                val pattern = if (message is ZenumlSyncMessage) StrokePattern.SOLID else StrokePattern.DASHED
                commands += DrawLine(from, to, pattern = pattern)
                commands += arrowHead(from, to)
                commands += DrawText(label, ScenePoint((fromX + toX) / 2, messageY - 8.0), TextAnchor.MIDDLE, style)
                messageY += config.messageGap
            }
        }
        diagram.participants.forEach { participant ->
            val actorWidth = actorWidths.getValue(participant.id)
            val center = centers.getValue(participant.id)
            val rect = SceneRect(center - actorWidth / 2, actorTop, actorWidth, actorHeight)
            commands += DrawRect(rect, cornerRadius = 4.0)
            commands += DrawText(participant.label, ScenePoint(center, actorTop + actorHeight / 2 + style.fontSize * 0.35), TextAnchor.MIDDLE, style)
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutTreeView(diagram: TreeViewDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val labelStyle = TextStyle(fontSize = 13.0)
        val directoryStyle = TextStyle(fontSize = 13.0, fontWeight = 600)
        val rowHeight = 36.0
        val indent = 42.0
        val maxLabelRight = diagram.nodes.maxOf { node ->
            config.padding + node.depth * indent + 18.0 + textMeasurer.measure(node.label, if (node.directory) directoryStyle else labelStyle).width
        }
        val width = max(360.0, maxLabelRight + config.padding)
        val height = max(180.0, config.padding * 2.0 + diagram.nodes.size * rowHeight)
        val points = diagram.nodes.mapIndexed { index, node ->
            ScenePoint(config.padding + node.depth * indent, config.padding + index * rowHeight + rowHeight / 2.0)
        }
        val commands = mutableListOf<DrawCommand>()
        diagram.nodes.forEachIndexed { index, node ->
            val point = points[index]
            node.parentIndex?.let { parentIndex ->
                val parent = points[parentIndex]
                commands += DrawPolyline(
                    listOf(
                        ScenePoint(parent.x + 5.0, parent.y + 7.0),
                        ScenePoint(parent.x + 5.0, point.y),
                        ScenePoint(point.x - 8.0, point.y),
                    ).map { it.canonical() },
                    stroke = SceneColor("#94a3b8"),
                    strokeWidth = 1.5,
                )
            }
            commands += DrawEllipse(point, 5.0, 5.0, fill = if (node.directory) SceneColor("#f59e0b") else SceneColor("#3b82f6"), stroke = SceneColor("#475569"), strokeWidth = 1.0)
            commands += DrawText(node.label, ScenePoint(point.x + 14.0, point.y + 5.0).canonical(), style = if (node.directory) directoryStyle else labelStyle)
        }
        return LayoutScene(width.xyCoordinate(), height.xyCoordinate(), commands)
    }

    /**
     * Bounded deterministic railroad layout: terminals as rectangles, non-terminals
     * as pills, sequence/stack/choice measured composition, and optional/repeat
     * bypass polylines. Arrow markers, labels, comments, and styling are not claimed.
     */
    private fun layoutRailroad(diagram: RailroadDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val root = buildRailroadBox(diagram.root, textMeasurer, mutableListOf())
        val commands = root.commands.map { command -> command.offsetBy(config.padding, config.padding).canonical() }
        val width = max(360.0, root.width + config.padding * 2.0)
        val height = max(180.0, root.height + config.padding * 2.0)
        return LayoutScene(width.xyCoordinate(), height.xyCoordinate(), commands)
    }

    private fun buildRailroadBox(node: RailroadNode, textMeasurer: TextMeasurer, commands: MutableList<DrawCommand>): RailroadBox =
        when (node) {
            is RailroadTerminal -> railroadLabelBox(node.label, cornerRadius = 0.0, fill = SceneColor("#ffffff"), textMeasurer, commands)
            is RailroadNonTerminal -> railroadLabelBox(node.label, cornerRadius = null, fill = SceneColor("#e2e8f0"), textMeasurer, commands)
            RailroadSkip -> {
                val local = mutableListOf<DrawCommand>()
                local += DrawLine(ScenePoint(0.0, 4.0), ScenePoint(28.0, 4.0))
                RailroadBox(width = 28.0, height = 8.0, center = 4.0, commands = local)
            }
            RailroadStart -> {
                val local = mutableListOf<DrawCommand>()
                local += DrawEllipse(ScenePoint(7.0, 7.0), radiusX = 5.0, radiusY = 5.0, fill = SceneColor("#111827"))
                RailroadBox(width = 14.0, height = 14.0, center = 7.0, commands = local)
            }
            RailroadEnd -> {
                val local = mutableListOf<DrawCommand>()
                local += DrawEllipse(ScenePoint(8.0, 8.0), radiusX = 6.0, radiusY = 6.0)
                local += DrawEllipse(ScenePoint(8.0, 8.0), radiusX = 3.0, radiusY = 3.0, fill = SceneColor("#111827"), strokeWidth = 1.0)
                RailroadBox(width = 16.0, height = 16.0, center = 8.0, commands = local)
            }
            is RailroadSequence -> {
                val children = node.children.map { child -> buildRailroadBox(child, textMeasurer, mutableListOf()) }
                val gap = 18.0
                val width = children.sumOf { it.width } + gap * (children.size - 1)
                val center = children.maxOf { it.center }
                var x = 0.0
                var previousRight: Double? = null
                children.forEach { child ->
                    val dy = center - child.center
                    commands += child.commands.map { command -> command.offsetBy(x, dy) }
                    previousRight?.let { right -> commands += DrawLine(ScenePoint(right, center), ScenePoint(x, center)) }
                    previousRight = x + child.width
                    x += child.width + gap
                }
                val height = max(center * 2.0, children.maxOf { it.height + (center - it.center) })
                RailroadBox(width, height, center, commands.toList().also { commands.clear() }.toMutableList())
            }
            is RailroadStack -> {
                val rows = node.children.map { child -> buildRailroadBox(child, textMeasurer, mutableListOf()) }
                val rowGap = 14.0
                val inset = 10.0
                val width = inset * 2.0 + rows.maxOf { it.width }
                var yOffset = 0.0
                val rowCenters = rows.mapIndexed { rowIndex, row ->
                    val centerY = yOffset + row.center
                    commands += row.commands.map { command -> command.offsetBy(inset, yOffset) }
                    if (rowIndex < rows.lastIndex) yOffset += row.height + rowGap
                    centerY
                }
                val leftSpine = 3.0
                val rightSpine = width - 3.0
                commands += DrawLine(ScenePoint(leftSpine, rowCenters.first()), ScenePoint(leftSpine, rowCenters.last()))
                commands += DrawLine(ScenePoint(rightSpine, rowCenters.first()), ScenePoint(rightSpine, rowCenters.last()))
                commands += DrawLine(ScenePoint(leftSpine, rowCenters.first()), ScenePoint(inset, rowCenters.first()))
                rows.forEachIndexed { rowIndex, row ->
                    commands += DrawLine(ScenePoint(inset + row.width, rowCenters[rowIndex]), ScenePoint(rightSpine, rowCenters[rowIndex]))
                }
                val height = yOffset + rows.last().height
                RailroadBox(width, height, rowCenters.first(), commands.toList().also { commands.clear() }.toMutableList())
            }
            is RailroadChoice -> {
                val branches = node.children.map { child -> buildRailroadBox(child, textMeasurer, mutableListOf()) }
                val branchGap = 14.0
                val indent = 16.0
                val contentWidth = branches.maxOf { it.width }
                val width = indent + contentWidth + 12.0
                val spineLeft = 4.0
                val spineRight = width - 4.0
                var yOffset = 0.0
                val branchCenters = branches.mapIndexed { branchIndex, branch ->
                    val centerY = yOffset + branch.center
                    commands += branch.commands.map { command -> command.offsetBy(indent, yOffset) }
                    commands += DrawLine(ScenePoint(spineLeft, centerY), ScenePoint(indent, centerY))
                    commands += DrawLine(ScenePoint(indent + branch.width, centerY), ScenePoint(spineRight, centerY))
                    if (branchIndex < branches.lastIndex) yOffset += branch.height + branchGap
                    centerY
                }
                commands += DrawLine(ScenePoint(spineLeft, branchCenters.first()), ScenePoint(spineLeft, branchCenters.last()))
                commands += DrawLine(ScenePoint(spineRight, branchCenters.first()), ScenePoint(spineRight, branchCenters.last()))
                val height = yOffset + branches.last().height
                val priorityCenter = branchCenters.getOrElse(node.priority) { branchCenters.first() }
                RailroadBox(width, height, priorityCenter, commands.toList().also { commands.clear() }.toMutableList())
            }
            is RailroadOptional -> railroadWrapped(node.child, textMeasurer, commands, loopTop = 20.0, arrowHead = false, bottomBypass = false)
            is RailroadOneOrMore -> railroadWrapped(node.child, textMeasurer, commands, loopTop = 22.0, arrowHead = true, bottomBypass = false)
            is RailroadZeroOrMore -> railroadWrapped(node.child, textMeasurer, commands, loopTop = 22.0, arrowHead = true, bottomBypass = true)
        }

    private fun railroadWrapped(
        child: RailroadNode,
        textMeasurer: TextMeasurer,
        commands: MutableList<DrawCommand>,
        loopTop: Double,
        arrowHead: Boolean,
        bottomBypass: Boolean,
    ): RailroadBox {
        val inner = buildRailroadBox(child, textMeasurer, mutableListOf())
        val bottomGap = if (bottomBypass) 18.0 else 0.0
        val width = inner.width
        val height = loopTop + inner.height + bottomGap
        val center = loopTop + inner.center
        commands += inner.commands.map { command -> command.offsetBy(0.0, loopTop) }
        commands += DrawPolyline(
            listOf(
                ScenePoint(0.0, center),
                ScenePoint(0.0, loopTop / 2.0),
                ScenePoint(width, loopTop / 2.0),
                ScenePoint(width, center),
            ),
        )
        if (arrowHead) {
            commands += DrawPolygon(
                listOf(
                    ScenePoint(0.0, loopTop / 2.0),
                    ScenePoint(8.0, loopTop / 2.0 - 4.5),
                    ScenePoint(8.0, loopTop / 2.0 + 4.5),
                ),
            )
        }
        if (bottomBypass) {
            val bypassY = height - 9.0
            commands += DrawPolyline(
                listOf(
                    ScenePoint(0.0, center),
                    ScenePoint(0.0, bypassY),
                    ScenePoint(width, bypassY),
                    ScenePoint(width, center),
                ),
            )
        }
        return RailroadBox(width, height, center, commands.toList().also { commands.clear() }.toMutableList())
    }

    private fun railroadLabelBox(
        label: String,
        cornerRadius: Double?,
        fill: SceneColor,
        textMeasurer: TextMeasurer,
        commands: MutableList<DrawCommand>,
    ): RailroadBox {
        val style = TextStyle(fontSize = 13.0)
        val measured = textMeasurer.measure(label, style)
        val width = measured.width + 24.0
        val height = max(30.0, measured.height + 16.0)
        val center = height / 2.0
        val radius = cornerRadius ?: height / 2.0
        commands += DrawRect(
            rect = SceneRect(0.0, 0.0, width, height),
            cornerRadius = radius,
            fill = fill,
        )
        commands += DrawText(
            text = label,
            origin = ScenePoint(width / 2.0, center + measured.height * 0.35),
            anchor = TextAnchor.MIDDLE,
            style = style,
        )
        return RailroadBox(width, height, center, commands.toList().also { commands.clear() }.toMutableList())
    }

    private class RailroadBox(
        val width: Double,
        val height: Double,
        /** Vertical offset of this box's main track from its top edge. */
        val center: Double,
        val commands: List<DrawCommand> = emptyList(),
    )

    private fun DrawCommand.offsetBy(dx: Double, dy: Double): DrawCommand = when (this) {
        is DrawRect -> copy(rect = SceneRect(rect.x + dx, rect.y + dy, rect.width, rect.height))
        is DrawEllipse -> copy(center = ScenePoint(center.x + dx, center.y + dy))
        is DrawLine -> copy(from = ScenePoint(from.x + dx, from.y + dy), to = ScenePoint(to.x + dx, to.y + dy))
        is DrawPolyline -> copy(points = points.map { point -> ScenePoint(point.x + dx, point.y + dy) })
        is DrawPolygon -> copy(points = points.map { point -> ScenePoint(point.x + dx, point.y + dy) })
        is DrawText -> copy(origin = ScenePoint(origin.x + dx, origin.y + dy))
    }

    private fun DrawCommand.canonical(): DrawCommand = when (this) {
        is DrawRect -> copy(rect = rect.canonical())
        is DrawEllipse -> copy(center = center.canonical())
        is DrawLine -> copy(from = from.canonical(), to = to.canonical())
        is DrawPolyline -> copy(points = points.map { point -> point.canonical() })
        is DrawPolygon -> copy(points = points.map { point -> point.canonical() })
        is DrawText -> copy(origin = origin.canonical())
    }

    private fun layoutSwimlane(diagram: SwimlaneDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val laneStyle = TextStyle(fontSize = 15.0, fontWeight = 600)
        val nodeStyle = TextStyle(fontSize = 13.0, fontWeight = 600)
        val edgeStyle = TextStyle(fontSize = 11.0)
        val nodeWidth = max(140.0, diagram.lanes.flatMap { it.nodes }.maxOf { textMeasurer.measure(it.label, nodeStyle).width } + 40.0)
        val nodeHeight = 64.0
        val nodeGap = 40.0
        val laneGap = 24.0
        val horizontal = diagram.direction == FlowDirection.LR || diagram.direction == FlowDirection.RL
        val maxNodes = diagram.lanes.maxOf { it.nodes.size }
        val laneLabelWidth = diagram.lanes.maxOf { textMeasurer.measure(it.label, laneStyle).width }
        val edgeLabelWidth = diagram.edges.mapNotNull { it.label }.maxOfOrNull { textMeasurer.measure(it, edgeStyle).width } ?: 0.0
        val laneWidth = if (horizontal) {
            max(max(laneLabelWidth + 32.0, maxNodes * nodeWidth + max(0, maxNodes - 1) * nodeGap + 40.0), edgeLabelWidth + 2.0 * config.padding)
        } else {
            max(nodeWidth + 40.0, laneLabelWidth + 32.0)
        }
        val laneHeight = if (horizontal) 148.0 else maxNodes * nodeHeight + max(0, maxNodes - 1) * nodeGap + 84.0
        val width = if (horizontal) {
            config.padding * 2.0 + laneWidth
        } else {
            max(edgeLabelWidth + 2.0 * config.padding, config.padding * 2.0 + diagram.lanes.size * laneWidth + max(0, diagram.lanes.size - 1) * laneGap)
        }
        val height = if (horizontal) {
            config.padding * 2.0 + diagram.lanes.size * laneHeight + max(0, diagram.lanes.size - 1) * laneGap
        } else {
            config.padding * 2.0 + laneHeight
        }
        val laneRects = linkedMapOf<String, SceneRect>()
        val nodePoints = linkedMapOf<String, ScenePoint>()
        val nodeById = diagram.lanes.flatMap { it.nodes }.associateBy { it.id }
        diagram.lanes.forEachIndexed { laneIndex, lane ->
            val rect = if (horizontal) {
                SceneRect(config.padding, config.padding + laneIndex * (laneHeight + laneGap), laneWidth, laneHeight)
            } else {
                SceneRect(config.padding + laneIndex * (laneWidth + laneGap), config.padding, laneWidth, laneHeight)
            }
            laneRects[lane.id] = rect
            lane.nodes.forEachIndexed { nodeIndex, node ->
                val point = if (horizontal) {
                    val forwardX = rect.x + 20.0 + nodeWidth / 2.0 + nodeIndex * (nodeWidth + nodeGap)
                    ScenePoint(if (diagram.direction == FlowDirection.RL) rect.x + rect.width - (forwardX - rect.x) else forwardX, rect.y + 92.0)
                } else {
                    val forwardY = rect.y + 54.0 + nodeHeight / 2.0 + nodeIndex * (nodeHeight + nodeGap)
                    ScenePoint(rect.x + rect.width / 2.0, if (diagram.direction == FlowDirection.BT) rect.y + rect.height - (forwardY - rect.y) else forwardY)
                }
                nodePoints[node.id] = point.canonical()
            }
        }
        val commands = mutableListOf<DrawCommand>()
        diagram.lanes.forEachIndexed { index, lane ->
            val rect = laneRects.getValue(lane.id)
            commands += DrawRect(rect.canonical(), 10.0, fill = if (index % 2 == 0) SceneColor("#f8fafc") else SceneColor("#f1f5f9"), stroke = SceneColor("#94a3b8"), strokeWidth = 1.5)
            commands += DrawText(lane.label, ScenePoint(rect.x + 16.0, rect.y + 25.0).canonical(), style = laneStyle)
        }
        diagram.edges.forEach { edge ->
            val fromCenter = nodePoints.getValue(edge.sourceId)
            val toCenter = nodePoints.getValue(edge.targetId)
            val from = swimlaneBoundaryPoint(fromCenter, toCenter, nodeById.getValue(edge.sourceId).shape, nodeWidth, nodeHeight)
            val to = swimlaneBoundaryPoint(toCenter, fromCenter, nodeById.getValue(edge.targetId).shape, nodeWidth, nodeHeight)
            commands += DrawLine(from.canonical(), to.canonical(), stroke = SceneColor("#475569"), strokeWidth = 1.5)
            commands += arrowHead(from, to)
            edge.label?.let { label ->
                commands += DrawText(label, ScenePoint((from.x + to.x) / 2.0, (from.y + to.y) / 2.0 - 8.0).canonical(), TextAnchor.MIDDLE, edgeStyle)
            }
        }
        diagram.lanes.flatMap { it.nodes }.forEach { node ->
            val point = nodePoints.getValue(node.id)
            when (node.shape) {
                SwimlaneNodeShape.RECTANGLE -> commands += DrawRect(SceneRect(point.x - nodeWidth / 2.0, point.y - nodeHeight / 2.0, nodeWidth, nodeHeight).canonical(), 0.0, fill = SceneColor("#ffffff"), stroke = SceneColor("#2563eb"), strokeWidth = 1.5)
                SwimlaneNodeShape.ROUNDED -> commands += DrawRect(SceneRect(point.x - nodeWidth / 2.0, point.y - nodeHeight / 2.0, nodeWidth, nodeHeight).canonical(), 12.0, fill = SceneColor("#ffffff"), stroke = SceneColor("#2563eb"), strokeWidth = 1.5)
                SwimlaneNodeShape.STADIUM -> commands += DrawRect(SceneRect(point.x - nodeWidth / 2.0, point.y - nodeHeight / 2.0, nodeWidth, nodeHeight).canonical(), nodeHeight / 2.0, fill = SceneColor("#ffffff"), stroke = SceneColor("#2563eb"), strokeWidth = 1.5)
                SwimlaneNodeShape.DECISION -> commands += DrawPolygon(listOf(ScenePoint(point.x, point.y - nodeHeight / 2.0), ScenePoint(point.x + nodeWidth / 2.0, point.y), ScenePoint(point.x, point.y + nodeHeight / 2.0), ScenePoint(point.x - nodeWidth / 2.0, point.y)).map { it.canonical() }, fill = SceneColor("#fef3c7"))
                SwimlaneNodeShape.CIRCLE -> commands += DrawEllipse(point, nodeHeight / 2.0, nodeHeight / 2.0, fill = SceneColor("#dcfce7"), stroke = SceneColor("#15803d"), strokeWidth = 1.5)
            }
            commands += DrawText(node.label, ScenePoint(point.x, point.y + 5.0).canonical(), TextAnchor.MIDDLE, nodeStyle)
        }
        return LayoutScene(width.xyCoordinate(), height.xyCoordinate(), commands)
    }

    private fun swimlaneBoundaryPoint(center: ScenePoint, toward: ScenePoint, shape: SwimlaneNodeShape, nodeWidth: Double, nodeHeight: Double): ScenePoint =
        usecaseBoundaryPoint(
            center,
            toward,
            if (shape == SwimlaneNodeShape.CIRCLE) nodeHeight / 2.0 else nodeWidth / 2.0,
            nodeHeight / 2.0,
            shape == SwimlaneNodeShape.CIRCLE || shape == SwimlaneNodeShape.STADIUM,
        )

    private fun layoutCynefin(diagram: CynefinDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val domainStyle = TextStyle(fontSize = 16.0, fontWeight = 600)
        val itemStyle = TextStyle(fontSize = 12.0)
        val titleStyle = TextStyle(fontSize = 20.0, fontWeight = 600)
        val labels = diagram.domains.flatMap { it.items }
        val measuredItemWidth = labels.maxOfOrNull { textMeasurer.measure(it, itemStyle).width } ?: 0.0
        val quadrantWidth = max(280.0, measuredItemWidth + 56.0)
        val quadrantHeight = max(220.0, (diagram.domains.maxOfOrNull { it.items.size } ?: 0) * 32.0 + 86.0)
        val quadrantGap = 40.0
        val titleOffset = if (diagram.title == null) 0.0 else 48.0
        val width = max(720.0, config.padding * 2.0 + quadrantWidth * 2.0 + quadrantGap)
        val height = max(560.0, config.padding * 2.0 + titleOffset + quadrantHeight * 2.0 + quadrantGap)
        val left = config.padding
        val top = config.padding + titleOffset
        val centers = mapOf(
            CynefinDomain.COMPLEX to ScenePoint(left + quadrantWidth / 2.0, top + quadrantHeight / 2.0),
            CynefinDomain.COMPLICATED to ScenePoint(left + quadrantWidth + quadrantGap + quadrantWidth / 2.0, top + quadrantHeight / 2.0),
            CynefinDomain.CHAOTIC to ScenePoint(left + quadrantWidth / 2.0, top + quadrantHeight + quadrantGap + quadrantHeight / 2.0),
            CynefinDomain.CLEAR to ScenePoint(left + quadrantWidth + quadrantGap + quadrantWidth / 2.0, top + quadrantHeight + quadrantGap + quadrantHeight / 2.0),
            CynefinDomain.CONFUSION to ScenePoint(left + quadrantWidth + quadrantGap / 2.0, top + quadrantHeight + quadrantGap / 2.0),
        )
        val fills = mapOf(
            CynefinDomain.COMPLEX to SceneColor("#dbeafe"),
            CynefinDomain.COMPLICATED to SceneColor("#dcfce7"),
            CynefinDomain.CLEAR to SceneColor("#fef3c7"),
            CynefinDomain.CHAOTIC to SceneColor("#fee2e2"),
            CynefinDomain.CONFUSION to SceneColor("#ede9fe"),
        )
        val commands = mutableListOf<DrawCommand>()
        diagram.title?.let { commands += DrawText(it, ScenePoint(config.padding, config.padding + 22.0), style = titleStyle) }
        listOf(CynefinDomain.COMPLEX, CynefinDomain.COMPLICATED, CynefinDomain.CHAOTIC, CynefinDomain.CLEAR).forEach { domain ->
            val center = centers.getValue(domain)
            commands += DrawRect(SceneRect(center.x - quadrantWidth / 2.0, center.y - quadrantHeight / 2.0, quadrantWidth, quadrantHeight), 0.0, fill = fills.getValue(domain), stroke = SceneColor("#64748b"), strokeWidth = 1.5)
        }
        val confusionCenter = centers.getValue(CynefinDomain.CONFUSION)
        commands += DrawEllipse(confusionCenter, 92.0, 66.0, fill = fills.getValue(CynefinDomain.CONFUSION), stroke = SceneColor("#6d28d9"), strokeWidth = 1.5)
        diagram.transitions.forEach { transition ->
            val from = centers.getValue(transition.from)
            val to = centers.getValue(transition.to)
            val start = cynefinBoundaryPoint(from, to, transition.from == CynefinDomain.CONFUSION, quadrantWidth, quadrantHeight)
            val end = cynefinBoundaryPoint(to, from, transition.to == CynefinDomain.CONFUSION, quadrantWidth, quadrantHeight)
            commands += DrawLine(start.canonical(), end.canonical(), stroke = SceneColor("#475569"), strokeWidth = 1.5)
            commands += arrowHead(start, end)
            transition.label?.let { label -> commands += DrawText(label, ScenePoint((from.x + to.x) / 2.0, (from.y + to.y) / 2.0 - 8.0).canonical(), TextAnchor.MIDDLE, TextStyle(fontSize = 11.0)) }
        }
        diagram.domains.forEach { block ->
            val center = centers.getValue(block.domain)
            val visibleItems = if (block.domain == CynefinDomain.CONFUSION) block.items.take(3) else block.items
            val titleY = if (block.domain == CynefinDomain.CONFUSION) center.y - 28.0 else center.y - quadrantHeight / 2.0 + 30.0
            commands += DrawText(block.domain.name.lowercase().replaceFirstChar { it.uppercase() }, ScenePoint(center.x, titleY), TextAnchor.MIDDLE, domainStyle)
            visibleItems.forEachIndexed { index, item ->
                val y = if (block.domain == CynefinDomain.CONFUSION) center.y - 3.0 + index * 19.0 else titleY + 34.0 + index * 30.0
                commands += DrawText(item, ScenePoint(center.x, y), TextAnchor.MIDDLE, itemStyle)
            }
            if (block.domain == CynefinDomain.CONFUSION && block.items.size > 3) {
                commands += DrawText("+${block.items.size - 3} more", ScenePoint(center.x, center.y + 54.0), TextAnchor.MIDDLE, itemStyle)
            }
        }
        return LayoutScene(width.xyCoordinate(), height.xyCoordinate(), commands)
    }

    private fun cynefinBoundaryPoint(center: ScenePoint, toward: ScenePoint, ellipse: Boolean, quadrantWidth: Double, quadrantHeight: Double): ScenePoint =
        usecaseBoundaryPoint(center, toward, if (ellipse) 92.0 else quadrantWidth / 2.0, if (ellipse) 66.0 else quadrantHeight / 2.0, ellipse)

    private fun layoutC4(diagram: C4Diagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val style = TextStyle(fontSize = 13.0, fontWeight = 600)
        val bodyStyle = TextStyle(fontSize = 10.0)
        val titleStyle = TextStyle(fontSize = 18.0, fontWeight = 600)
        val contentWidth = diagram.elements.maxOf { max(textMeasurer.measure(it.label, style).width, it.description?.let { description -> textMeasurer.measure(description, bodyStyle).width } ?: 0.0) }
        val cardWidth = max(180.0, contentWidth + 36.0)
        val columns = 3
        val cardHeight = 92.0
        val titleOffset = if (diagram.title == null) 0.0 else 44.0
        val points = diagram.elements.mapIndexed { index, element -> element.id to ScenePoint(config.padding + (index % columns) * (cardWidth + 32.0) + cardWidth / 2.0, config.padding + titleOffset + (index / columns) * (cardHeight + 36.0) + cardHeight / 2.0) }.toMap()
        val titleWidth = diagram.title?.let { textMeasurer.measure(it, titleStyle).width + config.padding * 2 } ?: 0.0
        val width = max(max(720.0, titleWidth), config.padding * 2 + minOf(columns, diagram.elements.size) * cardWidth + (minOf(columns, diagram.elements.size) - 1) * 32.0)
        val rows = (diagram.elements.size + columns - 1) / columns
        val height = max(360.0, config.padding * 2 + rows * cardHeight + max(0, rows - 1) * 36.0 + titleOffset)
        val commands = mutableListOf<DrawCommand>()
        diagram.title?.let { commands += DrawText(it, ScenePoint(config.padding, config.padding + 20.0), style = titleStyle) }
        diagram.relationships.forEach { relationship ->
            val fromCenter = points.getValue(relationship.sourceId)
            val toCenter = points.getValue(relationship.targetId)
            val from = usecaseBoundaryPoint(fromCenter, toCenter, cardWidth / 2.0, cardHeight / 2.0, false)
            val to = usecaseBoundaryPoint(toCenter, fromCenter, cardWidth / 2.0, cardHeight / 2.0, false)
            commands += DrawLine(from.canonical(), to.canonical(), stroke = SceneColor("#475569"), strokeWidth = 1.5)
            commands += DrawText(relationship.label, ScenePoint((fromCenter.x + toCenter.x) / 2.0, (fromCenter.y + toCenter.y) / 2.0 - 8.0).canonical(), anchor = TextAnchor.MIDDLE, style = TextStyle(fontSize = 11.0))
            if (relationship.bidirectional) commands += arrowHead(to, from)
            commands += arrowHead(from, to)
        }
        diagram.elements.forEach { element ->
            val point = points.getValue(element.id)
            val fill = if (element.external) SceneColor("#fef3c7") else if (element.kind == C4ElementKind.PERSON) SceneColor("#dcfce7") else SceneColor("#dbeafe")
            commands += DrawRect(SceneRect(point.x - cardWidth / 2.0, point.y - cardHeight / 2.0, cardWidth, cardHeight).canonical(), 8.0, fill = fill, stroke = SceneColor("#2563eb"), strokeWidth = 1.5)
            commands += DrawText(element.label, point.copy(y = point.y - 10.0).canonical(), anchor = TextAnchor.MIDDLE, style = style)
            element.description?.let { commands += DrawText(it, point.copy(y = point.y + 16.0).canonical(), anchor = TextAnchor.MIDDLE, style = bodyStyle) }
        }
        return LayoutScene(width.xyCoordinate(), height.xyCoordinate(), commands)
    }

    private fun layoutArchitecture(diagram: ArchitectureDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val textStyle = TextStyle(fontSize = 13.0, fontWeight = 600)
        val groupStyle = TextStyle(fontSize = 15.0, fontWeight = 600)
        val iconStyle = TextStyle(fontSize = 10.0, color = SceneColor("#475569"))
        val serviceLabelWidth = diagram.services.maxOfOrNull { textMeasurer.measure(it.label, textStyle).width } ?: 0.0
        val serviceIconWidth = diagram.services.maxOfOrNull { textMeasurer.measure(it.icon, iconStyle).width } ?: 0.0
        val groupLabelWidth = diagram.groups.maxOfOrNull { textMeasurer.measure(it.label, groupStyle).width } ?: 0.0
        val groupIconWidth = diagram.groups.maxOfOrNull { textMeasurer.measure(it.icon, iconStyle).width } ?: 0.0
        val nodeWidth = max(150.0, max(serviceLabelWidth, serviceIconWidth) + 42.0)
        val columnWidth = max(nodeWidth + 32.0, groupLabelWidth + groupIconWidth + 60.0)
        val hasStandalone = diagram.services.any { it.groupId == null }
        val columns = diagram.groups.map { it.id } + if (hasStandalone) listOf<String?>(null) else emptyList()
        val columnIndex = columns.withIndex().associate { it.value to it.index }
        val servicePoints = diagram.services.map { service ->
            val localIndex = diagram.services.filter { it.groupId == service.groupId }.indexOf(service)
            val index = columnIndex.getValue(service.groupId)
            service.id to ScenePoint(config.padding + index * (columnWidth + 40.0) + columnWidth / 2.0, config.padding + 80.0 + localIndex * 120.0)
        }.toMap()
        val groupRects = diagram.groups.mapIndexed { index, group ->
            val members = diagram.services.filter { it.groupId == group.id }
            group.id to SceneRect(config.padding + index * (columnWidth + 40.0), config.padding, columnWidth, max(140.0, members.size * 120.0 + 56.0))
        }.toMap()
        val maxRows = columns.maxOf { column -> diagram.services.count { it.groupId == column } }
        val width = max(720.0, config.padding * 2 + columns.size * columnWidth + (columns.size - 1) * 40.0)
        val height = max(420.0, config.padding * 2 + maxRows * 120.0 + 56.0)
        val commands = mutableListOf<DrawCommand>()
        diagram.groups.forEach { group ->
            val rect = groupRects.getValue(group.id)
            commands += DrawRect(rect.canonical(), 8.0, fill = SceneColor("#f8fafc"), stroke = SceneColor("#64748b"), strokeWidth = 1.5)
            commands += DrawText(group.label, ScenePoint(rect.x + 14.0, rect.y + 24.0), style = groupStyle)
            commands += DrawText(group.icon, ScenePoint(rect.x + rect.width - 14.0, rect.y + 24.0), anchor = TextAnchor.END, style = iconStyle)
        }
        diagram.edges.forEach { edge ->
            val from = servicePoints.getValue(edge.sourceId)
            val to = servicePoints.getValue(edge.targetId)
            val start = architecturePortPoint(from, edge.sourcePort, nodeWidth / 2.0, 38.0)
            val end = architecturePortPoint(to, edge.targetPort, nodeWidth / 2.0, 38.0)
            val startVector = architecturePortVector(edge.sourcePort)
            val endVector = architecturePortVector(edge.targetPort)
            val startOutside = ScenePoint(start.x + startVector.x * 12.0, start.y + startVector.y * 12.0)
            val endOutside = ScenePoint(end.x + endVector.x * 12.0, end.y + endVector.y * 12.0)
            val bridge = if (startVector.x != 0.0) ScenePoint(endOutside.x, startOutside.y) else ScenePoint(startOutside.x, endOutside.y)
            val points = listOf(start, startOutside, bridge, endOutside, end).map { it.canonical() }.fold(emptyList<ScenePoint>()) { result, point ->
                if (result.lastOrNull() == point) result else result + point
            }
            commands += DrawPolyline(points, stroke = SceneColor("#475569"), strokeWidth = 1.5)
            if (edge.directed) {
                val arrow = arrowHead(endOutside, end)
                commands += arrow.copy(points = arrow.points.map { it.canonical() })
            }
        }
        diagram.services.forEach { service ->
            val point = servicePoints.getValue(service.id)
            commands += DrawRect(SceneRect(point.x - nodeWidth / 2.0, point.y - 38.0, nodeWidth, 76.0).canonical(), 6.0, fill = SceneColor("#dbeafe"), stroke = SceneColor("#2563eb"), strokeWidth = 1.5)
            commands += DrawText(service.icon, point.copy(y = point.y - 11.0).canonical(), anchor = TextAnchor.MIDDLE, style = iconStyle)
            commands += DrawText(service.label, point.copy(y = point.y + 13.0).canonical(), anchor = TextAnchor.MIDDLE, style = textStyle)
        }
        return LayoutScene(width.xyCoordinate(), height.xyCoordinate(), commands)
    }

    private fun architecturePortPoint(center: ScenePoint, port: ArchitecturePort, halfWidth: Double, halfHeight: Double): ScenePoint = when (port) {
        ArchitecturePort.TOP -> ScenePoint(center.x, center.y - halfHeight)
        ArchitecturePort.BOTTOM -> ScenePoint(center.x, center.y + halfHeight)
        ArchitecturePort.LEFT -> ScenePoint(center.x - halfWidth, center.y)
        ArchitecturePort.RIGHT -> ScenePoint(center.x + halfWidth, center.y)
    }

    private fun architecturePortVector(port: ArchitecturePort): ScenePoint = when (port) {
        ArchitecturePort.TOP -> ScenePoint(0.0, -1.0)
        ArchitecturePort.BOTTOM -> ScenePoint(0.0, 1.0)
        ArchitecturePort.LEFT -> ScenePoint(-1.0, 0.0)
        ArchitecturePort.RIGHT -> ScenePoint(1.0, 0.0)
    }

    private fun layoutUsecase(diagram: UsecaseDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val style = TextStyle(fontSize = 13.0, fontWeight = 600)
        val labels = diagram.actors.map { it.label } + diagram.useCases.map { it.label }
        val nodeWidth = max(150.0, labels.maxOf { textMeasurer.measure(it, style).width } + 40.0)
        val horizontal = diagram.direction == FlowDirection.LR || diagram.direction == FlowDirection.RL
        val rows = max(diagram.actors.size, diagram.useCases.size)
        val width = if (horizontal) max(720.0, nodeWidth * 2 + 180.0) else max(720.0, nodeWidth * rows + 32.0 * (rows - 1) + config.padding * 2)
        val height = if (horizontal) max(360.0, rows * 110.0 + config.padding * 2) else 430.0
        val actorFirst = diagram.direction != FlowDirection.RL && diagram.direction != FlowDirection.BT
        val actorPoints = diagram.actors.indices.map { index ->
            if (horizontal) ScenePoint(if (actorFirst) config.padding + nodeWidth / 2 else width - config.padding - nodeWidth / 2, config.padding + 58.0 + index * 110.0)
            else ScenePoint(config.padding + nodeWidth / 2 + index * (nodeWidth + 32.0), if (actorFirst) 88.0 else height - 88.0)
        }
        val usecasePoints = diagram.useCases.indices.map { index ->
            if (horizontal) ScenePoint(if (actorFirst) width - config.padding - nodeWidth / 2 else config.padding + nodeWidth / 2, config.padding + 58.0 + index * 110.0)
            else ScenePoint(config.padding + nodeWidth / 2 + index * (nodeWidth + 32.0), if (actorFirst) height - 92.0 else 92.0)
        }
        val pointById = (diagram.actors.mapIndexed { i, item -> item.id to actorPoints[i] } + diagram.useCases.mapIndexed { i, item -> item.id to usecasePoints[i] }).toMap()
        val usecaseById = diagram.useCases.associateBy { it.id }
        val actorIds = diagram.actors.mapTo(mutableSetOf()) { it.id }
        val commands = mutableListOf<DrawCommand>()
        diagram.relationships.forEach { relationship ->
            val fromCenter = pointById.getValue(relationship.sourceId)
            val toCenter = pointById.getValue(relationship.targetId)
            val fromNode = usecaseById[relationship.sourceId]
            val toNode = usecaseById[relationship.targetId]
            val from = usecaseBoundaryPoint(
                center = fromCenter,
                toward = toCenter,
                halfWidth = if (relationship.sourceId in actorIds) 18.0 else nodeWidth / 2.0,
                halfHeight = if (relationship.sourceId in actorIds) 38.0 else 38.0,
                ellipse = fromNode?.shape == UsecaseShape.ELLIPSE,
            )
            val to = usecaseBoundaryPoint(
                center = toCenter,
                toward = fromCenter,
                halfWidth = if (relationship.targetId in actorIds) 18.0 else nodeWidth / 2.0,
                halfHeight = if (relationship.targetId in actorIds) 38.0 else 38.0,
                ellipse = toNode?.shape == UsecaseShape.ELLIPSE,
            )
            commands += DrawLine(from.canonical(), to.canonical(), stroke = SceneColor("#475569"), strokeWidth = 1.5)
            commands += arrowHead(from, to)
            relationship.label?.let { label ->
                commands += DrawText(label, ScenePoint((fromCenter.x + toCenter.x) / 2.0, (fromCenter.y + toCenter.y) / 2.0 - 8.0).canonical(), anchor = TextAnchor.MIDDLE, style = TextStyle(fontSize = 11.0))
            }
        }
        diagram.actors.forEachIndexed { index, actor ->
            val point = actorPoints[index]
            commands += DrawEllipse(ScenePoint(point.x, point.y - 20.0).canonical(), 10.0, 10.0, fill = SceneColor("#ffffff"), strokeWidth = 1.5)
            commands += DrawLine(ScenePoint(point.x, point.y - 10.0), ScenePoint(point.x, point.y + 20.0))
            commands += DrawLine(ScenePoint(point.x - 15.0, point.y), ScenePoint(point.x + 15.0, point.y))
            commands += DrawLine(ScenePoint(point.x, point.y + 20.0), ScenePoint(point.x - 13.0, point.y + 38.0))
            commands += DrawLine(ScenePoint(point.x, point.y + 20.0), ScenePoint(point.x + 13.0, point.y + 38.0))
            commands += DrawText(actor.label, ScenePoint(point.x, point.y + 58.0).canonical(), anchor = TextAnchor.MIDDLE, style = style)
        }
        diagram.useCases.forEachIndexed { index, node ->
            val point = usecasePoints[index]
            if (node.shape == UsecaseShape.ELLIPSE) {
                commands += DrawEllipse(point.canonical(), nodeWidth / 2.0, 38.0, fill = SceneColor("#eff6ff"), stroke = SceneColor("#2563eb"), strokeWidth = 1.5)
            } else {
                commands += DrawRect(SceneRect(point.x - nodeWidth / 2.0, point.y - 38.0, nodeWidth, 76.0).canonical(), 4.0, fill = SceneColor("#eff6ff"), stroke = SceneColor("#2563eb"), strokeWidth = 1.5)
            }
            commands += DrawText(node.label, ScenePoint(point.x, point.y + 5.0).canonical(), anchor = TextAnchor.MIDDLE, style = style)
        }
        return LayoutScene(width.xyCoordinate(), height.xyCoordinate(), commands)
    }

    private fun usecaseBoundaryPoint(
        center: ScenePoint,
        toward: ScenePoint,
        halfWidth: Double,
        halfHeight: Double,
        ellipse: Boolean,
    ): ScenePoint {
        val dx = toward.x - center.x
        val dy = toward.y - center.y
        if (dx == 0.0 && dy == 0.0) return center
        val scale = if (ellipse) {
            1.0 / sqrt(dx * dx / (halfWidth * halfWidth) + dy * dy / (halfHeight * halfHeight))
        } else {
            minOf(
                if (dx == 0.0) Double.POSITIVE_INFINITY else halfWidth / kotlin.math.abs(dx),
                if (dy == 0.0) Double.POSITIVE_INFINITY else halfHeight / kotlin.math.abs(dy),
            )
        }
        return ScenePoint(center.x + dx * scale, center.y + dy * scale).canonical()
    }

    private fun layoutVenn(diagram: VennDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val titleStyle = TextStyle(fontSize = 18.0, fontWeight = 600)
        val labelStyle = TextStyle(fontSize = 13.0, fontWeight = 600)
        val unionStyle = TextStyle(fontSize = 12.0, fontWeight = 600, color = SceneColor("#334155"))
        val labels = diagram.sets.map { it.label } + diagram.unions.mapNotNull { it.label }
        val measuredWidth = max(
            labels.maxOf { textMeasurer.measure(it, labelStyle).width },
            diagram.title?.let { textMeasurer.measure(it, titleStyle).width } ?: 0.0,
        )
        val width = max(720.0, measuredWidth + config.padding * 2 + 80.0).xyCoordinate()
        val height = if (diagram.sets.size == 2) 420.0 else 500.0
        val centerX = width / 2.0
        val titleOffset = if (diagram.title == null) 0.0 else 34.0
        val centers = if (diagram.sets.size == 2) {
            listOf(ScenePoint(centerX - 82.0, 220.0 + titleOffset), ScenePoint(centerX + 82.0, 220.0 + titleOffset))
        } else {
            listOf(
                ScenePoint(centerX - 92.0, 205.0 + titleOffset),
                ScenePoint(centerX + 92.0, 205.0 + titleOffset),
                ScenePoint(centerX, 337.0 + titleOffset),
            )
        }
        val maxSize = diagram.sets.mapNotNull { it.size }.maxOrNull()
        val radii = diagram.sets.map { set ->
            val size = set.size
            if (maxSize == null || size == null) 132.0 else (88.0 + 44.0 * sqrt(size / maxSize)).xyCoordinate()
        }
        val centroid = ScenePoint(centers.sumOf { it.x } / centers.size, centers.sumOf { it.y } / centers.size)
        val commands = mutableListOf<DrawCommand>()
        diagram.title?.let {
            commands += DrawText(it, ScenePoint(centerX, 32.0), anchor = TextAnchor.MIDDLE, style = titleStyle)
        }
        diagram.sets.forEachIndexed { index, set ->
            val center = centers[index]
            val radius = radii[index]
            commands += DrawEllipse(
                center = center.canonical(),
                radiusX = radius,
                radiusY = radius,
                fill = SceneColor(VENN_COLORS[index % VENN_COLORS.size]),
                fillOpacity = 0.28,
                stroke = SceneColor(VENN_STROKES[index % VENN_STROKES.size]),
                strokeWidth = 2.0,
            )
            val dx = center.x - centroid.x
            val dy = center.y - centroid.y
            val length = sqrt(dx * dx + dy * dy).coerceAtLeast(1.0)
            val labelPoint = ScenePoint(center.x + dx / length * radius * 0.48, center.y + dy / length * radius * 0.48 + 4.0)
            commands += DrawText(set.label, labelPoint.canonical(), anchor = TextAnchor.MIDDLE, style = labelStyle)
        }
        val centerById = diagram.sets.mapIndexed { index, set -> set.id to centers[index] }.toMap()
        diagram.unions.forEach { union ->
            union.label?.let { label ->
                val memberCenters = union.setIds.map { centerById.getValue(it) }
                val point = ScenePoint(
                    memberCenters.sumOf { it.x } / memberCenters.size,
                    memberCenters.sumOf { it.y } / memberCenters.size + 4.0,
                )
                commands += DrawText(label, point.canonical(), anchor = TextAnchor.MIDDLE, style = unionStyle)
            }
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutTreemap(diagram: TreemapDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val labelStyle = TextStyle(fontSize = 13.0, fontWeight = 600)
        val valueStyle = TextStyle(fontSize = 11.0)
        val maxLabelWidth = diagram.roots.flattenTreemap().maxOf { textMeasurer.measure(it.label, labelStyle).width }
        val width = max(720.0, maxLabelWidth + config.padding * 2 + 32.0).xyCoordinate()
        val height = 420.0
        val commands = mutableListOf<DrawCommand>()
        val content = SceneRect(config.padding, config.padding, width - config.padding * 2, height - config.padding * 2)

        fun render(node: TreemapNode, rect: SceneRect, depth: Int) {
            val fill = TREEMAP_COLORS[depth % TREEMAP_COLORS.size]
            commands += DrawRect(rect.canonical(), cornerRadius = 3.0, fill = fill, stroke = SceneColor("#334155"), strokeWidth = 1.0)
            commands += DrawText(node.label, ScenePoint(rect.x + 8.0, rect.y + 18.0).canonical(), style = labelStyle)
            node.value?.let {
                commands += DrawText(it.canonicalNumber(), ScenePoint(rect.x + 8.0, rect.y + 34.0).canonical(), style = valueStyle)
            }
            if (node.children.isEmpty()) return
            val inner = SceneRect(
                rect.x + 4.0,
                rect.y + 26.0,
                (rect.width - 8.0).coerceAtLeast(0.0),
                (rect.height - 30.0).coerceAtLeast(0.0),
            )
            val total = node.children.sumOf { it.treemapWeight() }
            var offset = 0.0
            val horizontal = depth % 2 == 0
            val axisExtent = if (horizontal) inner.width else inner.height
            val gap = treemapGap(axisExtent, node.children.size, 4.0)
            val available = axisExtent - gap * (node.children.size - 1)
            node.children.forEachIndexed { index, child ->
                val extent = if (index == node.children.lastIndex) {
                    axisExtent - offset
                } else {
                    (available * child.treemapWeight() / total).xyCoordinate()
                }
                val childRect = if (horizontal) {
                    SceneRect(inner.x + offset, inner.y, extent, inner.height)
                } else {
                    SceneRect(inner.x, inner.y + offset, inner.width, extent)
                }
                render(child, childRect, depth + 1)
                offset += extent + gap
            }
        }

        val total = diagram.roots.sumOf { it.treemapWeight() }
        var x = content.x
        val gap = treemapGap(content.width, diagram.roots.size, 6.0)
        val available = content.width - gap * (diagram.roots.size - 1)
        diagram.roots.forEachIndexed { index, root ->
            val rootWidth = if (index == diagram.roots.lastIndex) content.x + content.width - x else (available * root.treemapWeight() / total).xyCoordinate()
            render(root, SceneRect(x, content.y, rootWidth, content.height), 0)
            x += rootWidth + gap
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutSankey(diagram: SankeyDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val textStyle = TextStyle(fontSize = 12.0, fontWeight = 500)
        val layerGap = 120.0
        val nodeGap = 24.0
        val nodeWidth = max(160.0, diagram.nodes.maxOf { textMeasurer.measure(it.label, textStyle).width + 32.0 }).xyCoordinate()
        val maxValue = diagram.links.maxOf { it.value }
        val incoming = diagram.nodes.associate { node -> node.id to diagram.links.filter { it.targetId == node.id }.sumOf { it.value } }
        val outgoing = diagram.nodes.associate { node -> node.id to diagram.links.filter { it.sourceId == node.id }.sumOf { it.value } }
        val nodeHeights = diagram.nodes.associate { node ->
            node.id to max(40.0, max(incoming.getValue(node.id), outgoing.getValue(node.id)) / maxValue * 100.0).xyCoordinate()
        }
        val indegree = diagram.nodes.associate { it.id to 0 }.toMutableMap()
        val adjacent = diagram.nodes.associate { it.id to mutableListOf<String>() }
        diagram.links.forEach { link ->
            indegree[link.targetId] = indegree.getValue(link.targetId) + 1
            adjacent.getValue(link.sourceId) += link.targetId
        }
        val depths = diagram.nodes.associate { it.id to 0 }.toMutableMap()
        val queue = ArrayDeque(indegree.filterValues { it == 0 }.keys)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            adjacent.getValue(node).forEach { target ->
                depths[target] = max(depths.getValue(target), depths.getValue(node) + 1)
                val next = indegree.getValue(target) - 1
                indegree[target] = next
                if (next == 0) queue.addLast(target)
            }
        }
        val groupedLayers = diagram.nodes.groupBy { depths.getValue(it.id) }
        val layers = groupedLayers.keys.sorted().associateWith { groupedLayers.getValue(it) }
        val placements = linkedMapOf<String, SceneRect>()
        layers.forEach { (depth, nodes) ->
            var y = config.padding
            nodes.forEach { node ->
                placements[node.id] = SceneRect(
                    x = (config.padding + depth * (nodeWidth + layerGap)).xyCoordinate(),
                    y = y.xyCoordinate(),
                    width = nodeWidth,
                    height = nodeHeights.getValue(node.id),
                )
                y += nodeHeights.getValue(node.id) + nodeGap
            }
        }
        val width = (config.padding * 2 + layers.size * nodeWidth + max(0, layers.size - 1) * layerGap).xyCoordinate()
        val height = (config.padding * 2 + layers.values.maxOf { nodes ->
            nodes.sumOf { nodeHeights.getValue(it.id) } + max(0, nodes.size - 1) * nodeGap
        }).xyCoordinate()
        val commands = mutableListOf<DrawCommand>()
        diagram.links.forEach { link ->
            val source = placements.getValue(link.sourceId)
            val target = placements.getValue(link.targetId)
            commands += DrawLine(
                ScenePoint((source.x + source.width).xyCoordinate(), (source.y + source.height / 2).xyCoordinate()),
                ScenePoint(target.x.xyCoordinate(), (target.y + target.height / 2).xyCoordinate()),
                stroke = SceneColor("#60a5fa"),
                strokeWidth = max(1.5, link.value / maxValue * 12.0).xyCoordinate(),
            )
        }
        diagram.nodes.forEach { node ->
            val rect = placements.getValue(node.id)
            commands += DrawRect(rect, cornerRadius = 4.0, fill = SceneColor("#dbeafe"), stroke = SceneColor("#2563eb"))
            commands += DrawText(
                node.label,
                ScenePoint(rect.x + rect.width / 2, rect.y + rect.height / 2 + 4.0),
                anchor = TextAnchor.MIDDLE,
                style = textStyle,
            )
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutBlock(diagram: BlockDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val textStyle = TextStyle(fontSize = 14.0, fontWeight = 500)
        val columnGap = 24.0
        val rowGap = 40.0
        val nodeHeight = 64.0
        val cellWidth = max(
            160.0,
            diagram.nodes.maxOf { node ->
                val measured = textMeasurer.measure(node.label, textStyle).width + 32.0
                (measured - columnGap * (node.columnSpan - 1)) / node.columnSpan
            },
        ).xyCoordinate()
        val placements = linkedMapOf<String, SceneRect>()
        var row = 0
        var column = 0
        diagram.nodes.forEach { node ->
            if (column + node.columnSpan > diagram.columns) {
                row += 1
                column = 0
            }
            val x = config.padding + column * (cellWidth + columnGap)
            val y = config.padding + row * (nodeHeight + rowGap)
            val width = cellWidth * node.columnSpan + columnGap * (node.columnSpan - 1)
            placements[node.id] = SceneRect(x.xyCoordinate(), y.xyCoordinate(), width.xyCoordinate(), nodeHeight)
            column += node.columnSpan
            if (column == diagram.columns) {
                row += 1
                column = 0
            }
        }
        val rowCount = if (column == 0) row else row + 1
        val width = (config.padding * 2 + diagram.columns * cellWidth + (diagram.columns - 1) * columnGap).xyCoordinate()
        val height = (config.padding * 2 + rowCount * nodeHeight + max(0, rowCount - 1) * rowGap).xyCoordinate()
        val commands = mutableListOf<DrawCommand>()
        diagram.edges.forEach { edge ->
            val fromRect = placements.getValue(edge.from)
            val toRect = placements.getValue(edge.to)
            val fromCenter = ScenePoint(fromRect.x + fromRect.width / 2, fromRect.y + fromRect.height / 2)
            val toCenter = ScenePoint(toRect.x + toRect.width / 2, toRect.y + toRect.height / 2)
            val (from, to) = when {
                toCenter.y > fromCenter.y -> ScenePoint(fromCenter.x, fromRect.y + fromRect.height) to ScenePoint(toCenter.x, toRect.y)
                toCenter.y < fromCenter.y -> ScenePoint(fromCenter.x, fromRect.y) to ScenePoint(toCenter.x, toRect.y + toRect.height)
                toCenter.x > fromCenter.x -> ScenePoint(fromRect.x + fromRect.width, fromCenter.y) to ScenePoint(toRect.x, toCenter.y)
                else -> ScenePoint(fromRect.x, fromCenter.y) to ScenePoint(toRect.x + toRect.width, toCenter.y)
            }
            commands += DrawLine(from, to)
            val head = arrowHead(from, to)
            commands += head.copy(points = head.points.map { ScenePoint(it.x.xyCoordinate(), it.y.xyCoordinate()) })
        }
        diagram.nodes.forEach { node ->
            val rect = placements.getValue(node.id)
            commands += DrawRect(rect, cornerRadius = 6.0, fill = SceneColor("#f8fafc"))
            commands += DrawText(
                node.label,
                ScenePoint(rect.x + rect.width / 2, rect.y + 38.0),
                anchor = TextAnchor.MIDDLE,
                style = textStyle,
            )
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutKanban(diagram: KanbanDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val titleStyle = TextStyle(fontSize = 14.0, fontWeight = 600)
        val cardStyle = TextStyle(fontSize = 12.0)
        val gap = 16.0
        val widths = diagram.columns.map { column ->
            max(180.0, max(
                textMeasurer.measure(column.title, titleStyle).width,
                column.cards.maxOf { textMeasurer.measure(it.label, cardStyle).width },
            ) + 32.0).xyCoordinate()
        }
        val height = config.padding * 2 + 48.0 + diagram.columns.maxOf { it.cards.size } * 64.0
        val width = (config.padding * 2 + widths.sum() + gap * (widths.size - 1)).xyCoordinate()
        val commands = mutableListOf<DrawCommand>()
        var x = config.padding
        diagram.columns.forEachIndexed { index, column ->
            val columnWidth = widths[index]
            commands += DrawRect(SceneRect(x, config.padding, columnWidth, height - config.padding * 2), cornerRadius = 8.0, fill = SceneColor("#f1f5f9"))
            commands += DrawText(column.title, ScenePoint(x + 16.0, config.padding + 28.0), style = titleStyle)
            column.cards.forEachIndexed { cardIndex, card ->
                val y = config.padding + 48.0 + cardIndex * 64.0
                commands += DrawRect(SceneRect(x + 10.0, y, columnWidth - 20.0, 48.0), cornerRadius = 6.0, fill = SceneColor("#ffffff"))
                commands += DrawText(card.label, ScenePoint(x + 22.0, y + 29.0), style = cardStyle)
            }
            x += columnWidth + gap
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutGitGraph(
        diagram: GitGraphDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene {
        val labelStyle = TextStyle(fontSize = 13.0, fontWeight = 600)
        val commitStyle = TextStyle(fontSize = 12.0)
        val tagStyle = TextStyle(fontSize = 11.0, color = SceneColor("#7c3aed"))
        val labelWidth = (diagram.branches.maxOf { textMeasurer.measure(it.name, labelStyle).width } + 28.0).xyCoordinate()
        val columnWidths = diagram.commits.map { commit ->
            max(
                80.0,
                max(
                    textMeasurer.measure(commit.id, commitStyle).width,
                    commit.tag?.let { textMeasurer.measure(it, tagStyle).width } ?: 0.0,
                ) + 20.0,
            )
        }
        val startX = (config.padding + labelWidth).xyCoordinate()
        val centersX = mutableListOf<Double>()
        var cursor = startX
        columnWidths.forEach { width ->
            centersX += (cursor + width / 2.0).xyCoordinate()
            cursor = (cursor + width + 20.0).xyCoordinate()
        }
        val width = max(480.0, cursor + config.padding - 20.0).xyCoordinate()
        val laneGap = 92.0
        val firstLaneY = config.padding + 42.0
        val height = firstLaneY + (diagram.branches.size - 1) * laneGap + 72.0
        val laneByName = diagram.branches.mapIndexed { index, branch -> branch.name to index }.toMap()
        val centerById = diagram.commits.mapIndexed { index, commit ->
            commit.id to ScenePoint(centersX[index], firstLaneY + laneByName.getValue(commit.branch) * laneGap)
        }.toMap()
        val colors = listOf("#2563eb", "#16a34a", "#ea580c", "#7c3aed", "#0891b2")
        val commands = mutableListOf<DrawCommand>()

        diagram.branches.forEachIndexed { index, branch ->
            val y = firstLaneY + index * laneGap
            val color = SceneColor(colors[index % colors.size])
            val branchStartX = branch.parentCommitId?.let { centerById.getValue(it).x } ?: startX
            val branchEndX = diagram.commits
                .filter { it.branch == branch.name }
                .maxOfOrNull { centerById.getValue(it.id).x }
                ?.let { max(it, branchStartX) }
                ?: branchStartX
            commands += DrawText(branch.name, ScenePoint(config.padding, y + 5.0), style = labelStyle.copy(color = color))
            commands += DrawLine(ScenePoint(branchStartX, y), ScenePoint(branchEndX, y), stroke = color, strokeWidth = 2.0)
        }
        diagram.commits.forEach { commit ->
            val center = centerById.getValue(commit.id)
            val color = SceneColor(colors[laneByName.getValue(commit.branch) % colors.size])
            commit.parentIds.forEach { parentId ->
                commands += DrawLine(centerById.getValue(parentId), center, stroke = color, strokeWidth = 2.0)
            }
            val rect = when (commit.type) {
                GitGraphCommitType.HIGHLIGHT -> SceneRect(center.x - 12.0, center.y - 10.0, 24.0, 20.0)
                else -> SceneRect(center.x - 9.0, center.y - 9.0, 18.0, 18.0)
            }
            commands += DrawRect(rect, cornerRadius = if (commit.type == GitGraphCommitType.HIGHLIGHT) 2.0 else 9.0, fill = color, stroke = color)
            if (commit.isMerge) {
                commands += DrawRect(SceneRect(center.x - 5.0, center.y - 5.0, 10.0, 10.0), cornerRadius = 5.0, fill = SceneColor("#ffffff"), stroke = SceneColor("#ffffff"), strokeWidth = 1.0)
            }
            if (commit.type == GitGraphCommitType.REVERSE) {
                commands += DrawLine(ScenePoint(center.x - 5.0, center.y - 5.0), ScenePoint(center.x + 5.0, center.y + 5.0), stroke = SceneColor("#ffffff"), strokeWidth = 2.0)
                commands += DrawLine(ScenePoint(center.x + 5.0, center.y - 5.0), ScenePoint(center.x - 5.0, center.y + 5.0), stroke = SceneColor("#ffffff"), strokeWidth = 2.0)
            }
            commit.tag?.let { commands += DrawText(it, ScenePoint(center.x, center.y - 18.0), TextAnchor.MIDDLE, tagStyle) }
            commands += DrawText(commit.id, ScenePoint(center.x, center.y + 27.0), TextAnchor.MIDDLE, commitStyle)
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutPacket(diagram: PacketDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val labelStyle = TextStyle(fontSize = 11.0)
        val rangeStyle = TextStyle(fontSize = 9.0, color = SceneColor("#475569"))
        val titleStyle = TextStyle(fontSize = 18.0, fontWeight = 600)
        val bitWidth = max(24.0, diagram.fields.maxOf { field ->
            val firstRow = field.startBit / PACKET_BITS_PER_ROW
            val lastRow = field.endBit / PACKET_BITS_PER_ROW
            val narrowestSegmentBits = (firstRow..lastRow).minOf { row ->
                val rowStart = row * PACKET_BITS_PER_ROW
                val segmentStart = maxOf(field.startBit, rowStart)
                val segmentEnd = minOf(field.endBit, rowStart + PACKET_BITS_PER_ROW - 1)
                segmentEnd - segmentStart + 1
            }
            (textMeasurer.measure(field.label, labelStyle).width + 20.0) / narrowestSegmentBits
        })
        val titleHeight = if (diagram.title == null) 0.0 else 34.0
        val rowHeight = 60.0
        val rowCount = diagram.fields.maxOf { it.endBit } / PACKET_BITS_PER_ROW + 1
        val gridWidth = config.padding * 2 + PACKET_BITS_PER_ROW * bitWidth
        val titleWidth = diagram.title?.let { textMeasurer.measure(it, titleStyle).width + config.padding * 2 } ?: 0.0
        val width = max(gridWidth, titleWidth)
        val height = config.padding * 2 + titleHeight + rowCount * rowHeight
        val commands = mutableListOf<DrawCommand>()
        diagram.title?.let {
            commands += DrawText(it, ScenePoint(config.padding, config.padding + 18.0), style = titleStyle)
        }
        diagram.fields.forEach { field ->
            val firstRow = field.startBit / PACKET_BITS_PER_ROW
            val lastRow = field.endBit / PACKET_BITS_PER_ROW
            (firstRow..lastRow).forEach { row ->
                val rowStart = row * PACKET_BITS_PER_ROW
                val segmentStart = maxOf(field.startBit, rowStart)
                val segmentEnd = minOf(field.endBit, rowStart + PACKET_BITS_PER_ROW - 1)
                val x = config.padding + (segmentStart - rowStart) * bitWidth
                val y = config.padding + titleHeight + row * rowHeight
                val segmentWidth = (segmentEnd - segmentStart + 1) * bitWidth
                commands += DrawRect(SceneRect(x, y, segmentWidth, 38.0), cornerRadius = 2.0, fill = SceneColor("#eff6ff"))
                commands += DrawText(field.label, ScenePoint(x + segmentWidth / 2.0, y + 23.0), TextAnchor.MIDDLE, labelStyle)
                val range = if (segmentStart == segmentEnd) "$segmentStart" else "$segmentStart-$segmentEnd"
                commands += DrawText(range, ScenePoint(x + segmentWidth / 2.0, y + 53.0), TextAnchor.MIDDLE, rangeStyle)
            }
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutQuadrantChart(diagram: QuadrantChartDiagram, config: LayoutConfig): LayoutScene {
        val width = 640.0
        val height = 460.0
        val left = 92.0
        val top = 58.0
        val right = width - config.padding
        val bottom = height - 52.0
        val midX = (left + right) / 2.0
        val midY = (top + bottom) / 2.0
        val body = TextStyle(fontSize = 12.0)
        val commands = mutableListOf<DrawCommand>()
        diagram.title?.let { commands += DrawText(it, ScenePoint(width / 2.0, 26.0), TextAnchor.MIDDLE, TextStyle(fontSize = 18.0, fontWeight = 600)) }
        commands += DrawRect(SceneRect(left, top, right - left, bottom - top), cornerRadius = 0.0)
        commands += DrawLine(ScenePoint(midX, top), ScenePoint(midX, bottom))
        commands += DrawLine(ScenePoint(left, midY), ScenePoint(right, midY))
        commands += DrawText(diagram.xAxis.lowLabel, ScenePoint(left, bottom + 22.0), style = body)
        commands += DrawText(diagram.xAxis.highLabel, ScenePoint(right, bottom + 22.0), TextAnchor.END, body)
        commands += DrawText(diagram.yAxis.lowLabel, ScenePoint(left - 10.0, bottom), TextAnchor.END, body)
        commands += DrawText(diagram.yAxis.highLabel, ScenePoint(left - 10.0, top + 10.0), TextAnchor.END, body)
        val quadrantPositions = listOf(
            ScenePoint(right - 10.0, top + 20.0) to TextAnchor.END,
            ScenePoint(left + 10.0, top + 20.0) to TextAnchor.START,
            ScenePoint(left + 10.0, bottom - 12.0) to TextAnchor.START,
            ScenePoint(right - 10.0, bottom - 12.0) to TextAnchor.END,
        )
        diagram.quadrantLabels.forEachIndexed { index, label -> label?.let { commands += DrawText(it, quadrantPositions[index].first, quadrantPositions[index].second, body) } }
        diagram.points.forEach { point ->
            val x = (left + point.x * (right - left)).xyCoordinate()
            val y = (bottom - point.y * (bottom - top)).xyCoordinate()
            commands += DrawPolygon(listOf(ScenePoint(x, y - 5.0), ScenePoint(x + 5.0, y), ScenePoint(x, y + 5.0), ScenePoint(x - 5.0, y)), fill = SceneColor("#2563eb"))
            commands += DrawText(point.label, ScenePoint(x + 8.0, y - 7.0), style = body)
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutUserJourney(
        diagram: UserJourneyDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene {
        val body = TextStyle(fontSize = 12.0)
        val sectionStyle = TextStyle(fontSize = 13.0, fontWeight = 600)
        val titleStyle = TextStyle(fontSize = 18.0, fontWeight = 600)
        val sectionWidth = max(120.0, diagram.sections.maxOf { textMeasurer.measure(it.name, sectionStyle).width } + 24.0)
        val taskWidth = max(
            132.0,
            diagram.sections.flatMap { it.tasks }.maxOf { task ->
                max(
                    textMeasurer.measure(task.label, body).width,
                    textMeasurer.measure("Score ${task.score} · ${task.actors.joinToString(", ")}", body).width,
                ) + 20.0
            },
        )
        val taskGap = 14.0
        val maxTasks = diagram.sections.maxOf { it.tasks.size }
        val contentWidth = config.padding * 2 + sectionWidth + maxTasks * taskWidth + max(0, maxTasks - 1) * taskGap
        val titleWidth = diagram.title?.let { textMeasurer.measure(it, titleStyle).width + config.padding * 2 } ?: 0.0
        val width = max(640.0, max(contentWidth, titleWidth))
        val titleHeight = if (diagram.title == null) 18.0 else 42.0
        val rowHeight = 92.0
        val height = config.padding * 2 + titleHeight + diagram.sections.size * rowHeight
        val commands = mutableListOf<DrawCommand>()

        diagram.title?.let {
            commands += DrawText(it, ScenePoint(width / 2.0, config.padding + 18.0), TextAnchor.MIDDLE, titleStyle)
        }
        diagram.sections.forEachIndexed { sectionIndex, section ->
            val y = config.padding + titleHeight + sectionIndex * rowHeight
            commands += DrawRect(
                SceneRect(config.padding, y, sectionWidth - 12.0, 68.0),
                cornerRadius = 8.0,
                fill = SceneColor("#e2e8f0"),
            )
            commands += DrawText(
                section.name,
                ScenePoint(config.padding + 12.0, y + 38.0),
                style = sectionStyle,
            )
            section.tasks.forEachIndexed { taskIndex, task ->
                val x = config.padding + sectionWidth + taskIndex * (taskWidth + taskGap)
                val fill = JOURNEY_SCORE_COLORS[task.score]
                commands += DrawRect(
                    SceneRect(x, y, taskWidth, 68.0),
                    cornerRadius = 8.0,
                    fill = SceneColor(fill),
                )
                commands += DrawText(task.label, ScenePoint(x + 10.0, y + 24.0), style = body)
                commands += DrawText(
                    "Score ${task.score} · ${task.actors.joinToString(", ")}",
                    ScenePoint(x + 10.0, y + 48.0),
                    style = body,
                )
            }
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutRequirement(
        diagram: RequirementDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene {
        val body = TextStyle(fontSize = 12.0)
        val heading = TextStyle(fontSize = 14.0, fontWeight = 600)
        val headingLines = diagram.requirements.map { "requirement ${it.name}" } +
            diagram.elements.map { "element ${it.name}" }
        val bodyLines = diagram.requirements.flatMap { requirement ->
            listOf(
                "id: ${requirement.id}",
                "text: ${requirement.text}",
                "risk: ${requirement.risk.name.lowercase()}",
                "verify: ${requirement.verifyMethod.name.lowercase()}",
            )
        } + diagram.elements.flatMap { element ->
            listOf("type: ${element.type}", "docref: ${element.docRef}")
        }
        val measuredHeadingWidth = headingLines.maxOf { textMeasurer.measure(it, heading).width }
        val measuredBodyWidth = bodyLines.maxOf { textMeasurer.measure(it, body).width }
        val cardWidth = max(270.0, max(measuredHeadingWidth, measuredBodyWidth) + 24.0)
        val requirementHeight = 132.0
        val elementHeight = 96.0
        val columnGap = 150.0
        val rowGap = 28.0
        val requirementX = config.padding
        val elementX = config.padding + cardWidth + columnGap
        val rects = linkedMapOf<String, SceneRect>()
        diagram.requirements.forEachIndexed { index, requirement ->
            rects[requirement.name] = SceneRect(requirementX, config.padding + index * (requirementHeight + rowGap), cardWidth, requirementHeight)
        }
        diagram.elements.forEachIndexed { index, element ->
            rects[element.name] = SceneRect(elementX, config.padding + index * (elementHeight + rowGap), cardWidth, elementHeight)
        }
        val commands = mutableListOf<DrawCommand>()
        var relationshipRightExtent = rects.values.maxOf { it.x + it.width }
        diagram.relationships.forEach { relationship ->
            val from = rects.getValue(relationship.from)
            val to = rects.getValue(relationship.to)
            val sameColumn = from.x == to.x
            val leftward = from.x > to.x
            val start = ScenePoint(if (leftward) from.x else from.x + from.width, from.y + from.height / 2.0)
            val end = ScenePoint(if (leftward || sameColumn) to.x + to.width else to.x, to.y + to.height / 2.0)
            val label = when (relationship.kind) {
                RequirementRelationshipKind.SATISFIES -> "satisfies"
                RequirementRelationshipKind.VERIFIES -> "verifies"
            }
            val labelPosition = if (sameColumn) {
                val outerX = max(from.x + from.width, to.x + to.width) + 36.0
                commands += DrawPolyline(
                    listOf(
                        start,
                        ScenePoint(outerX, start.y),
                        ScenePoint(outerX, end.y),
                        end,
                    ),
                )
                relationshipRightExtent = max(
                    relationshipRightExtent,
                    outerX + 8.0 + textMeasurer.measure(label, body).width,
                )
                ScenePoint(outerX + 8.0, (start.y + end.y) / 2.0 - 4.0)
            } else {
                commands += DrawLine(start, end)
                ScenePoint((start.x + end.x) / 2.0, (start.y + end.y) / 2.0 - 8.0)
            }
            val direction = if (leftward || sameColumn) -1.0 else 1.0
            commands += DrawPolygon(
                listOf(
                    end,
                    ScenePoint(end.x - direction * 9.0, end.y - 5.0),
                    ScenePoint(end.x - direction * 9.0, end.y + 5.0),
                ),
                fill = SceneColor("#111827"),
            )
            commands += DrawText(
                label,
                labelPosition,
                if (sameColumn) TextAnchor.START else TextAnchor.MIDDLE,
                body,
            )
        }
        diagram.requirements.forEach { requirement ->
            val rect = rects.getValue(requirement.name)
            commands += DrawRect(rect, cornerRadius = 4.0)
            commands += DrawText("requirement ${requirement.name}", ScenePoint(rect.x + 12.0, rect.y + 22.0), style = heading)
            commands += DrawLine(ScenePoint(rect.x, rect.y + 32.0), ScenePoint(rect.x + rect.width, rect.y + 32.0))
            listOf(
                "id: ${requirement.id}",
                "text: ${requirement.text}",
                "risk: ${requirement.risk.name.lowercase()}",
                "verify: ${requirement.verifyMethod.name.lowercase()}",
            ).forEachIndexed { index, line ->
                commands += DrawText(line, ScenePoint(rect.x + 12.0, rect.y + 52.0 + index * 18.0), style = body)
            }
        }
        diagram.elements.forEach { element ->
            val rect = rects.getValue(element.name)
            commands += DrawRect(rect, cornerRadius = 4.0, fill = SceneColor("#eff6ff"))
            commands += DrawText("element ${element.name}", ScenePoint(rect.x + 12.0, rect.y + 22.0), style = heading)
            commands += DrawLine(ScenePoint(rect.x, rect.y + 32.0), ScenePoint(rect.x + rect.width, rect.y + 32.0))
            commands += DrawText("type: ${element.type}", ScenePoint(rect.x + 12.0, rect.y + 54.0), style = body)
            commands += DrawText("docref: ${element.docRef}", ScenePoint(rect.x + 12.0, rect.y + 74.0), style = body)
        }
        val width = relationshipRightExtent + config.padding
        val height = rects.values.maxOf { it.y + it.height } + config.padding
        return LayoutScene(width, height, commands)
    }

    private fun layoutTimeline(diagram: TimelineDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val body = TextStyle(fontSize = 12.0)
        val labelWidth = diagram.events.maxOf { textMeasurer.measure(it.period, body).width } + 32.0
        val width = max(420.0, config.padding * 2 + labelWidth + 300.0)
        val height = config.padding * 2 + 44.0 + diagram.events.size * 42.0
        val axisX = config.padding + labelWidth
        val commands = mutableListOf<DrawCommand>()
        diagram.title?.let { commands += DrawText(it, ScenePoint(config.padding, config.padding + 18.0), style = TextStyle(fontSize = 18.0, fontWeight = 600)) }
        commands += DrawLine(ScenePoint(axisX, config.padding + 32.0), ScenePoint(axisX, height - config.padding))
        diagram.events.forEachIndexed { index, event ->
            val y = config.padding + 52.0 + index * 42.0
            commands += DrawText(event.period, ScenePoint(config.padding, y + 5.0), style = body)
            commands += DrawPolygon(listOf(ScenePoint(axisX, y), ScenePoint(axisX + 7.0, y + 7.0), ScenePoint(axisX, y + 14.0), ScenePoint(axisX - 7.0, y + 7.0)), fill = SceneColor("#2563eb"))
            commands += DrawText(event.labels.joinToString(" · "), ScenePoint(axisX + 18.0, y + 11.0), style = body)
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutMindmap(
        diagram: MindmapDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene {
        val style = TextStyle()
        val nodesById = diagram.nodes.associateBy { it.id }
        val children = diagram.nodes.groupBy { it.parentId }
        val sizes = diagram.nodes.associate { node ->
            val text = textMeasurer.measure(node.label, style)
            val horizontalPadding = if (node.shape == MindmapNodeShape.DOUBLE_CIRCLE) 44.0 else 32.0
            node.id to SceneSize(max(92.0, text.width + horizontalPadding), max(42.0, text.height + 20.0))
        }
        val depths = diagram.nodes.maxOfOrNull { it.depth } ?: 0
        val columnWidths = (0..depths).map { depth ->
            diagram.nodes.filter { it.depth == depth }.maxOfOrNull { sizes.getValue(it.id).width } ?: 0.0
        }
        val columnX = mutableListOf<Double>()
        var x = config.padding
        columnWidths.forEach { width ->
            columnX += x
            x += width + config.nodeGap
        }

        val centersY = mutableMapOf<String, Double>()
        var leafCursor = config.padding
        fun place(id: String): Double {
            val childNodes = children[id].orEmpty()
            val center = if (childNodes.isEmpty()) {
                val height = sizes.getValue(id).height
                val value = leafCursor + height / 2.0
                leafCursor += height + config.nodeGap / 2.0
                value
            } else {
                val childCenters = childNodes.map { place(it.id) }
                (childCenters.first() + childCenters.last()) / 2.0
            }
            centersY[id] = center
            return center
        }
        val root = diagram.nodes.single { it.parentId == null }
        place(root.id)

        val rects = diagram.nodes.associate { node ->
            val size = sizes.getValue(node.id)
            node.id to SceneRect(
                x = columnX[node.depth],
                y = centersY.getValue(node.id) - size.height / 2.0,
                width = size.width,
                height = size.height,
            )
        }
        val commands = mutableListOf<DrawCommand>()
        diagram.nodes.filter { it.parentId != null }.forEach { node ->
            val parent = rects.getValue(requireNotNull(node.parentId))
            val child = rects.getValue(node.id)
            commands += DrawLine(
                ScenePoint(parent.x + parent.width, parent.y + parent.height / 2.0),
                ScenePoint(child.x, child.y + child.height / 2.0),
            )
        }
        diagram.nodes.forEach { node ->
            val rect = rects.getValue(node.id)
            val radius = when (node.shape) {
                MindmapNodeShape.DEFAULT -> 12.0
                MindmapNodeShape.RECTANGLE -> 2.0
                MindmapNodeShape.DOUBLE_CIRCLE -> rect.height / 2.0
            }
            commands += DrawRect(rect, cornerRadius = radius)
            if (node.shape == MindmapNodeShape.DOUBLE_CIRCLE) {
                val inset = 4.0
                commands += DrawRect(
                    SceneRect(rect.x + inset, rect.y + inset, rect.width - inset * 2, rect.height - inset * 2),
                    cornerRadius = (rect.height - inset * 2) / 2.0,
                )
            }
            commands += DrawText(
                node.label,
                ScenePoint(rect.x + rect.width / 2.0, rect.y + rect.height / 2.0 + style.fontSize * 0.35),
                TextAnchor.MIDDLE,
                style,
            )
        }
        val width = rects.values.maxOf { it.x + it.width } + config.padding
        val height = maxOf(
            rects.values.maxOf { it.y + it.height } + config.padding,
            leafCursor - config.nodeGap / 2.0 + config.padding,
        )
        check(nodesById.size == diagram.nodes.size)
        return LayoutScene(width, height, commands)
    }

    private fun layoutGantt(diagram: GanttDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val body = TextStyle(fontSize = 12.0)
        val tasks = diagram.sections.flatMap { section -> section.tasks.map { section.name to it } }
        val minDay = tasks.minOfOrNull { it.second.startDay } ?: 0
        val maxDay = tasks.maxOfOrNull { it.second.startDay + it.second.durationDays } ?: minDay + 1
        val scale = 28.0
        val labelWidth = tasks.maxOfOrNull { textMeasurer.measure(it.second.name, body).width }?.plus(24.0) ?: 120.0
        val width = config.padding * 2 + labelWidth + (maxDay - minDay) * scale
        val height = config.padding * 2 + maxOf(1, tasks.size) * 34.0 + 28.0
        val commands = mutableListOf<DrawCommand>()
        commands += DrawLine(ScenePoint((config.padding + labelWidth).xyCoordinate(), (config.padding + 24.0).xyCoordinate()), ScenePoint((width - config.padding).xyCoordinate(), (config.padding + 24.0).xyCoordinate()))
        diagram.title?.let { commands += DrawText(it, ScenePoint(config.padding, config.padding + 16.0), style = TextStyle(fontSize = 18.0, fontWeight = 600)) }
        var row = 0
        tasks.forEach { (section, task) ->
            val y = config.padding + 42.0 + row * 34.0
            commands += DrawText("$section: ${task.name}", ScenePoint(config.padding, y + 13.0), style = body)
            val x = (config.padding + labelWidth + (task.startDay - minDay) * scale).xyCoordinate()
            val fill = when (task.status) { GanttTaskStatus.DONE -> "#16a34a"; GanttTaskStatus.ACTIVE -> "#2563eb"; GanttTaskStatus.CRITICAL -> "#dc2626"; GanttTaskStatus.TODO -> "#94a3b8" }
            commands += DrawRect(SceneRect(x, y, task.durationDays * scale, 22.0), cornerRadius = 4.0, fill = SceneColor(fill))
            row++
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutXyChart(diagram: XyChartDiagram, config: LayoutConfig): LayoutScene {
        val bodyStyle = TextStyle(fontSize = 12.0)
        val titleStyle = TextStyle(fontSize = 18.0, fontWeight = 600)
        val width = 640.0
        val height = 400.0
        val left = config.padding + 52.0
        val top = config.padding + 48.0
        val plotWidth = width - left - config.padding
        val plotHeight = height - top - config.padding - 52.0
        val bottom = top + plotHeight
        val categories = diagram.xAxis.categories
        val step = plotWidth / categories.size
        val range = diagram.yAxis.maximum - diagram.yAxis.minimum
        fun x(index: Int): Double = (left + step * (index + 0.5)).xyCoordinate()
        fun y(value: Double): Double = (bottom - ((value - diagram.yAxis.minimum) / range) * plotHeight).xyCoordinate()

        val commands = mutableListOf<DrawCommand>()
        diagram.title?.let { commands += DrawText(it, ScenePoint(width / 2.0, config.padding + 18.0), TextAnchor.MIDDLE, titleStyle) }
        commands += DrawLine(ScenePoint(left, top), ScenePoint(left, bottom))
        commands += DrawLine(ScenePoint(left, bottom), ScenePoint(left + plotWidth, bottom))
        commands += DrawText(diagram.yAxis.maximum.toString(), ScenePoint(left - 8.0, top + 4.0), TextAnchor.END, bodyStyle)
        commands += DrawText(diagram.yAxis.minimum.toString(), ScenePoint(left - 8.0, bottom + 4.0), TextAnchor.END, bodyStyle)
        diagram.yAxis.title?.let { commands += DrawText(it, ScenePoint(left, top - 12.0), style = bodyStyle) }
        diagram.xAxis.title?.let { commands += DrawText(it, ScenePoint(left + plotWidth / 2.0, height - config.padding), TextAnchor.MIDDLE, bodyStyle) }
        categories.forEachIndexed { index, category ->
            commands += DrawText(category, ScenePoint(x(index), bottom + 20.0), TextAnchor.MIDDLE, bodyStyle)
        }

        val barSeries = diagram.series.filter { it.kind == XySeriesKind.BAR }
        val barWidth = (step * 0.64 / max(1, barSeries.size)).coerceAtMost(36.0)
        var barIndex = 0
        diagram.series.forEachIndexed { seriesIndex, series ->
            val color = SceneColor(XY_COLORS[seriesIndex % XY_COLORS.size])
            when (series.kind) {
                XySeriesKind.BAR -> {
                    series.values.forEachIndexed { index, value ->
                        val baseline = y(diagram.yAxis.minimum.coerceAtLeast(0.0).coerceAtMost(diagram.yAxis.maximum))
                        val valueY = y(value)
                        commands += DrawRect(
                            rect = SceneRect(
                                x = (x(index) - barSeries.size * barWidth / 2.0 + barIndex * barWidth).xyCoordinate(),
                                y = minOf(baseline, valueY),
                                width = barWidth.xyCoordinate(),
                                height = max(1.0, kotlin.math.abs(valueY - baseline)).xyCoordinate(),
                            ),
                            fill = color,
                            stroke = color,
                            strokeWidth = 1.0,
                        )
                    }
                    barIndex += 1
                }
                XySeriesKind.LINE -> {
                    commands += DrawPolyline(
                        points = series.values.mapIndexed { index, value -> ScenePoint(x(index), y(value)) },
                        stroke = color,
                        strokeWidth = 2.0,
                    )
                }
            }
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutEntityRelationship(
        diagram: EntityRelationshipDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene {
        val style = TextStyle()
        fun attributeLine(type: String, name: String, key: EntityKey): String = buildString {
            append(type).append(' ').append(name)
            if (key != EntityKey.NONE) append(' ').append(key.name)
        }
        fun cardinalityLabel(cardinality: EntityCardinality): String = when (cardinality) {
            EntityCardinality.ONLY_ONE -> "1"
            EntityCardinality.ZERO_OR_ONE -> "0..1"
            EntityCardinality.ONE_OR_MORE -> "1..*"
            EntityCardinality.ZERO_OR_MORE -> "0..*"
        }
        val sizes = diagram.entities.associate { entity ->
            val lines = listOf(entity.id) + entity.attributes.map { attributeLine(it.type, it.name, it.key) }
            entity.id to SceneSize(
                max(140.0, lines.maxOf { textMeasurer.measure(it, style).width } + 24.0),
                max(48.0, lines.size * 22.0 + 16.0),
            )
        }
        val width = (sizes.values.maxOfOrNull { it.width } ?: 0.0) + config.padding * 2
        val height = sizes.values.sumOf { it.height } + config.nodeGap * max(0, sizes.size - 1) + config.padding * 2
        val rects = linkedMapOf<String, SceneRect>()
        var y = config.padding
        diagram.entities.forEach { entity ->
            val size = sizes.getValue(entity.id)
            rects[entity.id] = SceneRect(config.padding, y, size.width, size.height)
            y += size.height + config.nodeGap
        }
        val commands = mutableListOf<DrawCommand>()
        diagram.relationships.forEach { relationship ->
            val source = rects[relationship.from] ?: return@forEach
            val target = rects[relationship.to] ?: return@forEach
            val from = ScenePoint(source.x + source.width / 2, source.y + source.height)
            val to = ScenePoint(target.x + target.width / 2, target.y)
            commands += DrawLine(from, to)
            commands += DrawText(cardinalityLabel(relationship.fromCardinality), ScenePoint(from.x + 8.0, from.y + 16.0), style = style)
            commands += DrawText(cardinalityLabel(relationship.toCardinality), ScenePoint(to.x + 8.0, to.y - 8.0), style = style)
            if (relationship.label.isNotEmpty()) {
                commands += DrawText(
                    relationship.label,
                    ScenePoint((from.x + to.x) / 2 + 12.0, (from.y + to.y) / 2),
                    style = style,
                )
            }
        }
        diagram.entities.forEach { entity ->
            val rect = rects.getValue(entity.id)
            commands += DrawRect(rect, cornerRadius = 4.0)
            val lines = listOf(entity.id) + entity.attributes.map { attributeLine(it.type, it.name, it.key) }
            lines.forEachIndexed { index, line ->
                commands += DrawText(line, ScenePoint(rect.x + 12.0, rect.y + 18.0 + index * 22.0), style = style)
            }
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutClass(diagram: ClassDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val style = TextStyle()
        val sizes = diagram.classes.associate { klass ->
            val lines = listOf(klass.label) + klass.members.map { member ->
                val prefix = when (member.visibility) {
                    ClassVisibility.PUBLIC -> "+"
                    ClassVisibility.PRIVATE -> "-"
                    ClassVisibility.PROTECTED -> "#"
                    ClassVisibility.PACKAGE -> "~"
                }
                "$prefix${member.signature}"
            }
            klass.id to SceneSize(max(120.0, lines.maxOf { textMeasurer.measure(it, style).width } + 24.0), max(48.0, lines.size * 22.0 + 16.0))
        }
        val width = (sizes.values.maxOfOrNull { it.width } ?: 0.0) + config.padding * 2
        val height = sizes.values.sumOf { it.height } + config.nodeGap * max(0, sizes.size - 1) + config.padding * 2
        val rects = linkedMapOf<String, SceneRect>()
        var y = config.padding
        diagram.classes.forEach { klass ->
            val size = sizes.getValue(klass.id)
            rects[klass.id] = SceneRect(config.padding, y, size.width, size.height)
            y += size.height + config.nodeGap
        }
        val commands = mutableListOf<DrawCommand>()
        diagram.relationships.forEach { relation ->
            val source = rects[relation.from] ?: return@forEach
            val target = rects[relation.to] ?: return@forEach
            val sourceBottom = ScenePoint(source.x + source.width / 2, source.y + source.height)
            val targetTop = ScenePoint(target.x + target.width / 2, target.y)
            val (from, to) = when (relation.kind) {
                ClassRelationshipKind.INHERITANCE -> targetTop to sourceBottom
                ClassRelationshipKind.ASSOCIATION -> sourceBottom to targetTop
            }
            commands += DrawLine(from, to)
            commands += arrowHead(from, to)
        }
        diagram.classes.forEach { klass ->
            val rect = rects.getValue(klass.id)
            commands += DrawRect(rect, cornerRadius = 4.0)
            val lines = listOf(klass.label) + klass.members.map { member ->
                val prefix = when (member.visibility) {
                    ClassVisibility.PUBLIC -> "+"
                    ClassVisibility.PRIVATE -> "-"
                    ClassVisibility.PROTECTED -> "#"
                    ClassVisibility.PACKAGE -> "~"
                }
                "$prefix${member.signature}"
            }
            lines.forEachIndexed { index, line ->
                commands += DrawText(line, ScenePoint(rect.x + 12.0, rect.y + 18.0 + index * 22.0), style = style)
            }
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutState(
        diagram: StateDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene {
        val style = TextStyle()
        val sizes = diagram.states.associate { state ->
            state.id to if (state.kind != StateNodeKind.STATE) {
                SceneSize(24.0, 24.0)
            } else {
                val text = textMeasurer.measure(state.label, style)
                SceneSize(max(88.0, text.width + 32.0), max(40.0, text.height + 20.0))
            }
        }
        val horizontal = diagram.direction == FlowDirection.LR || diagram.direction == FlowDirection.RL
        val contentWidth = if (horizontal) {
            sizes.values.sumOf { it.width } + config.nodeGap * max(0, sizes.size - 1)
        } else {
            sizes.values.maxOfOrNull { it.width } ?: 0.0
        }
        val contentHeight = if (horizontal) {
            sizes.values.maxOfOrNull { it.height } ?: 0.0
        } else {
            sizes.values.sumOf { it.height } + config.nodeGap * max(0, sizes.size - 1)
        }
        val width = contentWidth + config.padding * 2
        val height = contentHeight + config.padding * 2
        val rects = linkedMapOf<String, SceneRect>()
        var cursor = config.padding
        diagram.states.forEach { state ->
            val size = sizes.getValue(state.id)
            val forward = if (horizontal) {
                SceneRect(cursor, config.padding + (contentHeight - size.height) / 2, size.width, size.height)
            } else {
                SceneRect(config.padding + (contentWidth - size.width) / 2, cursor, size.width, size.height)
            }
            rects[state.id] = when (diagram.direction) {
                FlowDirection.RL -> forward.copy(x = width - config.padding - (forward.x - config.padding) - size.width)
                FlowDirection.BT -> forward.copy(y = height - config.padding - (forward.y - config.padding) - size.height)
                else -> forward
            }
            cursor += (if (horizontal) size.width else size.height) + config.nodeGap
        }

        val commands = mutableListOf<DrawCommand>()
        diagram.transitions.forEach { transition ->
            val source = rects[transition.from] ?: return@forEach
            val target = rects[transition.to] ?: return@forEach
            val anchors = edgeAnchors(source, target, horizontal)
            commands += DrawLine(anchors.first, anchors.second)
            commands += arrowHead(anchors.first, anchors.second)
            if (transition.label.isNotEmpty()) {
                commands += DrawText(
                    transition.label,
                    ScenePoint((anchors.first.x + anchors.second.x) / 2, (anchors.first.y + anchors.second.y) / 2 - 8.0),
                    TextAnchor.MIDDLE,
                    style,
                )
            }
        }
        diagram.states.forEach { state ->
            val rect = rects.getValue(state.id)
            when (state.kind) {
                StateNodeKind.STATE -> {
                    commands += DrawRect(rect = rect, cornerRadius = 8.0)
                    commands += DrawText(
                        state.label,
                        ScenePoint(rect.x + rect.width / 2, rect.y + rect.height / 2 + style.fontSize * 0.35),
                        TextAnchor.MIDDLE,
                        style,
                    )
                }
                StateNodeKind.START -> commands += DrawRect(
                    rect = rect,
                    cornerRadius = rect.width / 2,
                    fill = SceneColor("#334155"),
                    stroke = SceneColor("#334155"),
                )
                StateNodeKind.END -> {
                    commands += DrawRect(rect = rect, cornerRadius = rect.width / 2)
                    val inset = 5.0
                    commands += DrawRect(
                        rect = SceneRect(
                            x = rect.x + inset,
                            y = rect.y + inset,
                            width = rect.width - inset * 2,
                            height = rect.height - inset * 2,
                        ),
                        cornerRadius = (rect.width - inset * 2) / 2,
                        fill = SceneColor("#334155"),
                        stroke = SceneColor("#334155"),
                    )
                }
            }
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutFlowchart(
        diagram: FlowchartDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene {
        val style = TextStyle()
        val sizes = diagram.nodes.associate { node ->
            val text = textMeasurer.measure(node.label, style)
            node.id to SceneSize(max(80.0, text.width + 32.0), max(40.0, text.height + 20.0))
        }
        val horizontal = diagram.direction == FlowDirection.LR || diagram.direction == FlowDirection.RL
        val contentWidth = if (horizontal) {
            sizes.values.sumOf { it.width } + config.nodeGap * max(0, sizes.size - 1)
        } else {
            sizes.values.maxOfOrNull { it.width } ?: 0.0
        }
        val contentHeight = if (horizontal) {
            sizes.values.maxOfOrNull { it.height } ?: 0.0
        } else {
            sizes.values.sumOf { it.height } + config.nodeGap * max(0, sizes.size - 1)
        }
        val width = contentWidth + config.padding * 2
        val height = contentHeight + config.padding * 2
        val rects = linkedMapOf<String, SceneRect>()
        var cursor = config.padding
        diagram.nodes.forEach { node ->
            val size = sizes.getValue(node.id)
            val forward = if (horizontal) {
                SceneRect(cursor, config.padding + (contentHeight - size.height) / 2, size.width, size.height)
            } else {
                SceneRect(config.padding + (contentWidth - size.width) / 2, cursor, size.width, size.height)
            }
            val rect = when (diagram.direction) {
                FlowDirection.RL -> forward.copy(x = width - config.padding - (forward.x - config.padding) - size.width)
                FlowDirection.BT -> forward.copy(y = height - config.padding - (forward.y - config.padding) - size.height)
                else -> forward
            }
            rects[node.id] = rect
            cursor += (if (horizontal) size.width else size.height) + config.nodeGap
        }

        val commands = mutableListOf<DrawCommand>()
        diagram.edges.forEach { edge ->
            val source = rects[edge.sourceId] ?: return@forEach
            val target = rects[edge.targetId] ?: return@forEach
            val anchors = edgeAnchors(source, target, horizontal)
            commands += DrawLine(anchors.first, anchors.second)
            commands += arrowHead(anchors.first, anchors.second)
        }
        diagram.nodes.forEach { node ->
            val rect = rects.getValue(node.id)
            commands += DrawRect(rect = rect, cornerRadius = 4.0)
            commands += DrawText(
                text = node.label,
                origin = ScenePoint(rect.x + rect.width / 2, rect.y + rect.height / 2 + style.fontSize * 0.35),
                anchor = TextAnchor.MIDDLE,
                style = style,
            )
        }
        return LayoutScene(width, height, commands)
    }

    private fun edgeAnchors(source: SceneRect, target: SceneRect, horizontal: Boolean): Pair<ScenePoint, ScenePoint> =
        if (horizontal) {
            val targetAfter = target.x + target.width / 2 >= source.x + source.width / 2
            if (targetAfter) {
                ScenePoint(source.x + source.width, source.y + source.height / 2) to
                    ScenePoint(target.x, target.y + target.height / 2)
            } else {
                ScenePoint(source.x, source.y + source.height / 2) to
                    ScenePoint(target.x + target.width, target.y + target.height / 2)
            }
        } else {
            val targetAfter = target.y + target.height / 2 >= source.y + source.height / 2
            if (targetAfter) {
                ScenePoint(source.x + source.width / 2, source.y + source.height) to
                    ScenePoint(target.x + target.width / 2, target.y)
            } else {
                ScenePoint(source.x + source.width / 2, source.y) to
                    ScenePoint(target.x + target.width / 2, target.y + target.height)
            }
        }

    private fun layoutSequence(
        diagram: SequenceDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene {
        val style = TextStyle()
        val actorHeight = 40.0
        val actorWidths = diagram.actors.associate { actor ->
            actor.id to max(88.0, textMeasurer.measure(actor.label, style).width + 32.0)
        }
        val centers = linkedMapOf<String, Double>()
        var cursor = config.padding
        diagram.actors.forEach { actor ->
            val actorWidth = actorWidths.getValue(actor.id)
            centers[actor.id] = cursor + actorWidth / 2
            cursor += actorWidth + config.nodeGap
        }
        val width = max(config.padding * 2, cursor - config.nodeGap + config.padding)
        val actorTop = config.padding
        val messageTop = actorTop + actorHeight + 40.0
        val messageRows = diagram.messages.sumOf { if (it.from == it.to) 2L else 1L }.toInt()
        val height = messageTop + max(1, messageRows) * config.messageGap + config.padding
        val commands = mutableListOf<DrawCommand>()
        diagram.actors.forEach { actor ->
            val center = centers.getValue(actor.id)
            commands += DrawLine(
                ScenePoint(center, actorTop + actorHeight),
                ScenePoint(center, height - config.padding),
                pattern = StrokePattern.DASHED,
            )
        }
        var messageY = messageTop
        diagram.messages.forEach { message ->
            val fromX = centers.getValue(message.from)
            val toX = centers.getValue(message.to)
            val pattern = if (message.lineStyle == SequenceLineStyle.DASHED) StrokePattern.DASHED else StrokePattern.SOLID
            if (fromX == toX) {
                val loopRight = minOf(width - config.padding, fromX + 48.0)
                val endY = messageY + 24.0
                val points = listOf(ScenePoint(fromX, messageY), ScenePoint(loopRight, messageY), ScenePoint(loopRight, endY), ScenePoint(fromX, endY))
                commands += DrawPolyline(points, pattern = pattern)
                commands += arrowHead(points[points.lastIndex - 1], points.last())
                commands += DrawText(message.label, ScenePoint(fromX + 8.0, messageY - 8.0), style = style)
                messageY += config.messageGap * 2
            } else {
                val from = ScenePoint(fromX, messageY)
                val to = ScenePoint(toX, messageY)
                commands += DrawLine(from, to, pattern = pattern)
                commands += arrowHead(from, to)
                commands += DrawText(message.label, ScenePoint((fromX + toX) / 2, messageY - 8.0), TextAnchor.MIDDLE, style)
                messageY += config.messageGap
            }
        }
        diagram.actors.forEach { actor ->
            val actorWidth = actorWidths.getValue(actor.id)
            val center = centers.getValue(actor.id)
            val rect = SceneRect(center - actorWidth / 2, actorTop, actorWidth, actorHeight)
            commands += DrawRect(rect, cornerRadius = 4.0)
            commands += DrawText(actor.label, ScenePoint(center, actorTop + actorHeight / 2 + style.fontSize * 0.35), TextAnchor.MIDDLE, style)
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutPie(diagram: PieDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val titleStyle = TextStyle(fontSize = 18.0, fontWeight = 600)
        val bodyStyle = TextStyle(fontSize = 13.0)
        val center = ScenePoint(config.padding + 150.0, config.padding + 170.0)
        val radius = 120.0
        val legendX = config.padding + 320.0
        val legendStartY = config.padding + 48.0
        val total = diagram.sections.sumOf { it.value }
        val commands = mutableListOf<DrawCommand>()
        diagram.title?.takeIf { it.isNotEmpty() }?.let { commands += DrawText(it, ScenePoint(config.padding, config.padding + titleStyle.fontSize), style = titleStyle) }
        var angle = -PI / 2.0
        diagram.sections.forEachIndexed { index, section ->
            val fraction = if (total > 0.0) section.value / total else 0.0
            val end = angle + fraction * 2.0 * PI
            if (fraction > 0.0) {
                val points = buildList {
                    add(center)
                    add(ScenePoint((center.x + radius * cos(angle)).pieCoordinate(), (center.y + radius * sin(angle)).pieCoordinate()))
                    val steps = maxOf(2, (fraction * 48.0).toInt())
                    for (step in 1..steps) {
                        val a = angle + (end - angle) * step / steps
                        add(ScenePoint((center.x + radius * cos(a)).pieCoordinate(), (center.y + radius * sin(a)).pieCoordinate()))
                    }
                }
                commands += DrawPolygon(points, fill = SceneColor(PIE_COLORS[index % PIE_COLORS.size]))
            }
            val legendY = legendStartY + index * 28.0
            commands += DrawRect(SceneRect(legendX, legendY - 11.0, 14.0, 14.0), cornerRadius = 2.0, fill = SceneColor(PIE_COLORS[index % PIE_COLORS.size]))
            commands += DrawText(if (diagram.showData) "${section.label}: ${section.value}" else section.label, ScenePoint(legendX + 22.0, legendY), style = bodyStyle)
            angle = end
        }
        val legendWidth = diagram.sections.maxOfOrNull { textMeasurer.measure(it.label, bodyStyle).width } ?: 0.0
        return LayoutScene(
            width = maxOf(config.padding * 2 + 480.0, legendX + 24.0 + legendWidth + config.padding),
            height = maxOf(config.padding * 2 + 2.0 * radius + 30.0, legendStartY + diagram.sections.size * 28.0 + config.padding),
            commands = commands,
        )
    }

    /** Deterministic 2D map layout for the bounded wardley-beta slice. */
    private fun layoutWardleyMap(
        diagram: WardleyMapDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene {
        val labelStyle = TextStyle(fontSize = 13.0)
        val anchorStyle = TextStyle(fontSize = 13.0, fontWeight = 600)
        val noteStyle = TextStyle(fontSize = 12.0, fontWeight = 400, color = SceneColor("#475569"))
        val evolveColor = SceneColor("#dc2626")
        val commands = mutableListOf<DrawCommand>()
        var cursorY = config.padding
        diagram.title?.let { title ->
            val titleStyle = TextStyle(fontSize = 18.0, fontWeight = 600)
            commands += DrawText(title, ScenePoint(config.padding, cursorY + titleStyle.fontSize), style = titleStyle)
            cursorY += 30.0
        }
        val width = 720.0
        val height = cursorY + 480.0
        val plotLeft = config.padding + 40.0
        val plotRight = width - config.padding
        val plotTop = cursorY + 16.0
        val plotBottom = height - config.padding - 24.0
        // OWM axes: visibility grows bottom-to-top, evolution grows left-to-right.
        fun x(evolution: Double): Double = plotLeft + evolution * (plotRight - plotLeft)
        fun y(visibility: Double): Double = plotBottom - visibility * (plotBottom - plotTop)
        commands += DrawLine(
            ScenePoint(plotLeft, plotTop),
            ScenePoint(plotLeft, plotBottom),
            stroke = SceneColor("#334155"),
        )
        commands += DrawLine(
            ScenePoint(plotLeft, plotBottom),
            ScenePoint(plotRight, plotBottom),
            stroke = SceneColor("#334155"),
        )
        val centers = diagram.nodes.associateWith { node -> ScenePoint(x(node.evolution), y(node.visibility)) }
        diagram.links.forEach { link: WardleyLink ->
            val from = centers.getValue(diagram.nodes.first { it.name == link.from })
            val to = centers.getValue(diagram.nodes.first { it.name == link.to })
            commands += DrawLine(from, to)
            commands += arrowHead(from, to)
        }
        diagram.evolutions.forEach { evolution: WardleyEvolution ->
            val node = diagram.nodes.first { it.name == evolution.component }
            val from = centers.getValue(node)
            val to = ScenePoint(x(evolution.evolution), y(node.visibility))
            commands += DrawLine(from, to, stroke = evolveColor, pattern = StrokePattern.DASHED)
            commands += DrawPolygon(listOf(to, ScenePoint(to.x - 9.0, to.y - 4.5), ScenePoint(to.x - 9.0, to.y + 4.5)), fill = evolveColor)
        }
        diagram.nodes.forEach { node ->
            val center = centers.getValue(node)
            if (!node.anchor) {
                commands += DrawEllipse(center, radiusX = 7.0, radiusY = 7.0)
            }
            commands += DrawText(node.name, ScenePoint(center.x, center.y - 12.0), TextAnchor.MIDDLE, if (node.anchor) anchorStyle else labelStyle)
        }
        diagram.notes.forEach { note ->
            commands += DrawText(note.text, ScenePoint(x(note.evolution), y(note.visibility)), TextAnchor.MIDDLE, noteStyle)
        }
        return LayoutScene(width, height, commands)
    }

    private fun arrowHead(from: ScenePoint, to: ScenePoint): DrawPolygon {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val length = sqrt(dx * dx + dy * dy).takeIf { it > 0.0 } ?: 1.0
        val unitX = dx / length
        val unitY = dy / length
        val baseX = to.x - unitX * 9.0
        val baseY = to.y - unitY * 9.0
        val perpendicularX = -unitY * 4.5
        val perpendicularY = unitX * 4.5
        return DrawPolygon(listOf(to, ScenePoint(baseX + perpendicularX, baseY + perpendicularY), ScenePoint(baseX - perpendicularX, baseY - perpendicularY)))
    }

    private val PIE_COLORS = listOf("#2563eb", "#16a34a", "#f59e0b", "#dc2626", "#9333ea", "#0891b2")
    private val XY_COLORS = listOf("#2563eb", "#16a34a", "#f59e0b", "#dc2626", "#9333ea", "#0891b2")
    private val JOURNEY_SCORE_COLORS = listOf("#fee2e2", "#fecaca", "#fed7aa", "#fef3c7", "#dcfce7", "#bbf7d0")
    private val TREEMAP_COLORS = listOf(SceneColor("#dbeafe"), SceneColor("#dcfce7"), SceneColor("#fef3c7"), SceneColor("#fce7f3"))
    private val VENN_COLORS = listOf("#60a5fa", "#34d399", "#fbbf24")
    private val VENN_STROKES = listOf("#2563eb", "#059669", "#d97706")

    private fun TreemapNode.treemapWeight(): Double = value ?: children.sumOf { it.treemapWeight() }
    private fun List<TreemapNode>.flattenTreemap(): List<TreemapNode> = flatMap { listOf(it) + it.children.flattenTreemap() }
    private fun treemapGap(axisExtent: Double, itemCount: Int, preferred: Double): Double =
        if (itemCount <= 1) 0.0 else preferred.coerceAtMost(axisExtent / (itemCount - 1))
    private fun ScenePoint.canonical(): ScenePoint = ScenePoint(x.xyCoordinate(), y.xyCoordinate())
    private fun SceneRect.canonical(): SceneRect = SceneRect(x.xyCoordinate(), y.xyCoordinate(), width.xyCoordinate(), height.xyCoordinate())
    private fun Double.canonicalNumber(): String {
        val value = xyCoordinate()
        return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }

    private fun Double.pieCoordinate(): Double = round(this * 1_000_000.0) / 1_000_000.0
    private fun Double.xyCoordinate(): Double = round(this * 1_000_000.0) / 1_000_000.0
}
