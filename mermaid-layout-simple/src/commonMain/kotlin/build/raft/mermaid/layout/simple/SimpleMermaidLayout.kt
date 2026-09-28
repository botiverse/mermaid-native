package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.FlowDirection
import build.raft.mermaid.core.FlowEdgeStyle
import build.raft.mermaid.core.FlowchartDiagram
import build.raft.mermaid.core.FlowNodeShape
import build.raft.mermaid.core.ClassDiagram
import build.raft.mermaid.core.ClassDefinition
import build.raft.mermaid.core.ClassMarker
import build.raft.mermaid.core.ClassMember
import build.raft.mermaid.core.ClassVisibility
import build.raft.mermaid.core.ClassRelationshipKind
import build.raft.mermaid.core.EntityCardinality
import build.raft.mermaid.core.EntityRelationshipDiagram
import build.raft.mermaid.core.EntityKey
import build.raft.mermaid.core.MermaidDiagram
import build.raft.mermaid.core.MindmapDiagram
import build.raft.mermaid.core.MindmapNodeShape
import build.raft.mermaid.core.TimelineDiagram
import build.raft.mermaid.core.TimelineEvent
import build.raft.mermaid.core.UserJourneyDiagram
import build.raft.mermaid.core.SequenceDiagram
import build.raft.mermaid.core.PieDiagram
import build.raft.mermaid.core.SequenceArrowHead
import build.raft.mermaid.core.SequenceLineStyle
import build.raft.mermaid.core.SequenceNotePosition
import build.raft.mermaid.core.StateDiagram
import build.raft.mermaid.core.StateNodeKind
import build.raft.mermaid.core.StateNotePosition
import build.raft.mermaid.core.XyChartDiagram
import build.raft.mermaid.core.XySeriesKind
import build.raft.mermaid.core.GanttDiagram
import build.raft.mermaid.core.GanttTaskStatus
import build.raft.mermaid.core.QuadrantChartDiagram
import build.raft.mermaid.core.RadarChartDiagram
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
import build.raft.mermaid.core.C4Element
import build.raft.mermaid.core.C4ElementKind
import build.raft.mermaid.core.CynefinDiagram
import build.raft.mermaid.core.CynefinDomain
import build.raft.mermaid.core.IshikawaDiagram
import build.raft.mermaid.core.IshikawaNode
import build.raft.mermaid.core.SwimlaneDiagram
import build.raft.mermaid.core.SwimlaneNodeShape
import build.raft.mermaid.core.TreeViewDiagram
import build.raft.mermaid.core.TreeViewNode
import build.raft.mermaid.core.RailroadChoice
import build.raft.mermaid.core.RailroadSpecial
import build.raft.mermaid.core.RailroadDiagram
import build.raft.mermaid.core.ZenumlDiagram
import build.raft.mermaid.core.WardleyEvolution
import build.raft.mermaid.core.WardleyLink
import build.raft.mermaid.core.WardleyMapDiagram
import build.raft.mermaid.core.WardleyNode
import build.raft.mermaid.core.WardleyNote
import build.raft.mermaid.core.EventModelingDiagram
import build.raft.mermaid.core.EventModelingEntityKind
import build.raft.mermaid.core.ZenumlAsyncMessage
import build.raft.mermaid.core.ZenumlMessage
import build.raft.mermaid.core.ZenumlSyncMessage
import build.raft.mermaid.core.RailroadNode
import build.raft.mermaid.core.RailroadNonTerminal
import build.raft.mermaid.core.RailroadOneOrMore
import build.raft.mermaid.core.RailroadOptional
import build.raft.mermaid.core.RailroadSequence
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.round
import kotlin.math.ceil

/** Deterministic text metrics for goldens and hosts without platform font metrics. */
public object FixedWidthTextMeasurer : TextMeasurer {
    override fun measure(text: String, style: TextStyle): SceneSize = SceneSize(
        width = text.length * style.fontSize * 0.6,
        height = style.fontSize * 1.2,
    )
}

private const val PACKET_BITS_PER_ROW: Int = 32
private const val STATE_TERMINAL_SIZE = 14.0

/** Small deterministic layout with no DOM, JavaScript, ELK, or platform state. */
public object SimpleMermaidLayout : DiagramLayout {
    override fun layout(
        diagram: MermaidDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene = when (diagram) {
        is FlowchartDiagram -> layoutFlowchart(visibleFlow(diagram), textMeasurer, config)
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
        is RadarChartDiagram -> layoutRadar(diagram, textMeasurer, config)
        is UserJourneyDiagram -> layoutUserJourney(diagram, textMeasurer, config)
        is GitGraphDiagram -> layoutGitGraph(diagram, textMeasurer, config)
        is RequirementDiagram -> layoutRequirementGraph(diagram, textMeasurer, config)
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
        is IshikawaDiagram -> layoutIshikawa(diagram, textMeasurer, config)
        is SwimlaneDiagram -> layoutSwimlane(diagram, textMeasurer, config)
        is TreeViewDiagram -> layoutTreeView(diagram, textMeasurer, config)
        is RailroadDiagram -> layoutRailroad(diagram, textMeasurer, config)
        is ZenumlDiagram -> layoutZenuml(diagram, textMeasurer, config)
        is WardleyMapDiagram -> layoutWardleyMap(diagram, textMeasurer, config)
        is EventModelingDiagram -> layoutEventModeling(diagram, textMeasurer, config)
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

    private fun treeViewNodesWithOfficialRoot(nodes: List<TreeViewNode>): List<TreeViewNode> {
        if (nodes.isEmpty() || (nodes.first().label == "/" && nodes.first().depth == 0 && nodes.first().parentIndex == null)) {
            return nodes
        }
        return listOf(TreeViewNode("/", 0, null, true)) + nodes.map { node ->
            node.copy(depth = node.depth + 1, parentIndex = node.parentIndex?.plus(1) ?: 0)
        }
    }

    private fun layoutTreeView(diagram: TreeViewDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val nodes = treeViewNodesWithOfficialRoot(diagram.nodes)
        val labelStyle = TextStyle(fontSize = 13.0)
        val directoryStyle = TextStyle(fontSize = 13.0, fontWeight = 600)
        val rowHeight = 36.0
        val indent = 42.0
        val nodeRadius = 5.0
        val maxLabelRight = nodes.maxOf { node ->
            config.padding + node.depth * indent + 18.0 + textMeasurer.measure(node.label, if (node.directory) directoryStyle else labelStyle).width
        }
        val width = max(360.0, maxLabelRight + config.padding)
        val height = max(180.0, config.padding * 2.0 + nodes.size * rowHeight)
        val points = nodes.mapIndexed { index, node ->
            ScenePoint(config.padding + node.depth * indent, config.padding + index * rowHeight + rowHeight / 2.0)
        }
        val commands = mutableListOf<DrawCommand>()
        nodes.forEachIndexed { index, node ->
            val point = points[index]
            node.parentIndex?.let { parentIndex ->
                val parent = points[parentIndex]
                commands += DrawPolyline(
                    listOf(
                        ScenePoint(parent.x, parent.y + nodeRadius),
                        ScenePoint(parent.x, point.y),
                        ScenePoint(point.x - nodeRadius, point.y),
                    ).map { it.canonical() },
                    stroke = SceneColor("#94a3b8"),
                    strokeWidth = 1.5,
                )
            }
        }
        // Paint every connector before node bodies, including later siblings' trunks.
        nodes.forEachIndexed { index, node ->
            val point = points[index]
            commands += DrawEllipse(point, nodeRadius, nodeRadius, fill = if (node.directory) SceneColor("#f59e0b") else SceneColor("#3b82f6"), stroke = SceneColor("#475569"), strokeWidth = 1.0)
            commands += DrawText(node.label, ScenePoint(point.x + 14.0, point.y + 5.0).canonical(), style = if (node.directory) directoryStyle else labelStyle)
        }
        return LayoutScene(width.xyCoordinate(), height.xyCoordinate(), commands)
    }

    /**
     * Bounded deterministic railroad layout for official named rules: terminals as
     * rectangles, non-terminals as pills, special as dashed pills, sequence/choice
     * measured composition, and optional/repeat bypass polylines. Arrow markers,
     * comments, ABNF/EBNF/PEG, and full styling are not claimed.
     */
    private fun layoutRailroad(diagram: RailroadDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val commands = mutableListOf<DrawCommand>()
        var y = config.padding
        var width = 360.0
        val title = diagram.title
        if (title != null) {
            val titleStyle = TextStyle(fontSize = 18.0, fontWeight = 600)
            val measured = textMeasurer.measure(title, titleStyle)
            commands += DrawText(
                text = title,
                origin = ScenePoint(config.padding, y + measured.height),
                anchor = TextAnchor.START,
                style = titleStyle,
            )
            y += measured.height + 16.0
            width = max(width, config.padding * 2.0 + measured.width)
        }
        diagram.rules.forEach { rule ->
            val nameStyle = TextStyle(fontSize = 13.0, fontWeight = 600)
            val nameMeasured = textMeasurer.measure(rule.name, nameStyle)
            val box = buildRailroadBox(rule.definition, textMeasurer, mutableListOf())
            val nameWidth = max(48.0, nameMeasured.width + 8.0)
            val trackX = config.padding + nameWidth + 16.0
            val centerY = y + box.center
            commands += DrawText(
                text = rule.name,
                origin = ScenePoint(config.padding + nameWidth, centerY + nameMeasured.height * 0.35),
                anchor = TextAnchor.END,
                style = nameStyle,
            )
            commands += box.commands.map { command -> command.offsetBy(trackX, y) }
            width = max(width, trackX + box.width + config.padding)
            y += max(box.height, nameMeasured.height) + 20.0
        }
        val height = max(180.0, y + config.padding)
        return LayoutScene(width.xyCoordinate(), height.xyCoordinate(), commands.map { command -> command.canonical() })
    }

    private fun buildRailroadBox(node: RailroadNode, textMeasurer: TextMeasurer, commands: MutableList<DrawCommand>): RailroadBox =
        when (node) {
            is RailroadTerminal -> railroadLabelBox(node.label, cornerRadius = 0.0, fill = SceneColor("#ffffff"), textMeasurer, commands)
            is RailroadNonTerminal -> railroadLabelBox(node.label, cornerRadius = null, fill = SceneColor("#e2e8f0"), textMeasurer, commands)
            is RailroadSpecial -> railroadLabelBox(node.text, cornerRadius = null, fill = SceneColor("#f8fafc"), textMeasurer, commands)
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
            is RailroadChoice -> {
                val branches = node.children.map { child -> buildRailroadBox(child, textMeasurer, mutableListOf()) }
                val branchGap = 14.0
                val indent = 16.0
                val contentWidth = branches.maxOf { it.width }
                val width = indent + contentWidth + 12.0
                val spineLeft = 0.0
                val spineRight = width
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
                RailroadBox(width, height, branchCenters.first(), commands.toList().also { commands.clear() }.toMutableList())
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
        diagram.flowchart?.let { flow ->
            val simpleShapes=setOf(FlowNodeShape.RECTANGLE,FlowNodeShape.ROUNDED,FlowNodeShape.STADIUM,FlowNodeShape.CIRCLE,FlowNodeShape.DIAMOND)
            val groupIds = flow.subgraphs.map { it.id }.toSet()
            val needsRichLayout=flow.subgraphs.any { it.parentId!=null || it.collapsed || it.direction!=null } ||
                flow.nodes.any { it.shape !in simpleShapes || it.styles.isNotEmpty() || it.classes.isNotEmpty() || it.borders!=null } ||
                flow.classDefinitions.isNotEmpty() || flow.defaultEdgeStyles.isNotEmpty() ||
                flow.edges.any { it.sourceId in groupIds || it.targetId in groupIds || it.length > 1 || it.style!=FlowEdgeStyle.NORMAL || it.fromMarker!=build.raft.mermaid.core.FlowMarker.NONE || it.toMarker!=build.raft.mermaid.core.FlowMarker.POINT || it.styles.isNotEmpty() }
            if(needsRichLayout)return layoutFlowchart(visibleFlow(flow),textMeasurer,config)
        }
        val laneStyle = TextStyle(fontSize = 15.0, fontWeight = 600)
        val nodeStyle = TextStyle(fontSize = 13.0, fontWeight = 600)
        val edgeStyle = TextStyle(fontSize = 11.0)
        val nodeWidth = max(140.0, diagram.lanes.flatMap { it.nodes }.maxOf { textMeasurer.measure(it.label, nodeStyle).width } + 40.0)
        val nodeHeight = 64.0
        val nodeGap = 40.0
        val laneGap = 24.0
        val titleRail = 32.0
        val horizontal = diagram.direction == FlowDirection.LR || diagram.direction == FlowDirection.RL
        val maxNodes = diagram.lanes.maxOf { it.nodes.size }
        val laneLabelWidth = diagram.lanes.maxOf { textMeasurer.measure(it.label, laneStyle).width }
        val edgeLabelWidth = diagram.edges.mapNotNull { it.label }.maxOfOrNull { textMeasurer.measure(it, edgeStyle).width } ?: 0.0
        val laneWidth = if (horizontal) {
            max(max(laneLabelWidth + 32.0 + titleRail, maxNodes * nodeWidth + max(0, maxNodes - 1) * nodeGap + 40.0 + titleRail), edgeLabelWidth + 2.0 * config.padding)
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
                    val forwardX = rect.x + 20.0 + titleRail + nodeWidth / 2.0 + nodeIndex * (nodeWidth + nodeGap)
                    ScenePoint(if (diagram.direction == FlowDirection.RL) rect.x + rect.width - (forwardX - rect.x) else forwardX, rect.y + 92.0)
                } else {
                    val forwardY = rect.y + 54.0 + nodeHeight / 2.0 + nodeIndex * (nodeHeight + nodeGap)
                    ScenePoint(rect.x + rect.width / 2.0, if (diagram.direction == FlowDirection.BT) rect.y + rect.height - (forwardY - rect.y) else forwardY)
                }
                nodePoints[node.id] = point.canonical()
            }
        }
        val commands = mutableListOf<DrawCommand>()
        diagram.lanes.forEachIndexed { _, lane ->
            val rect = laneRects.getValue(lane.id)
            commands += DrawRect(rect.canonical(), 0.0, fill = SceneColor("#ffffff"), stroke = SceneColor("#707070"), strokeWidth = 1.0)
            commands += DrawRect(
                SceneRect(rect.x, rect.y, titleRail, rect.height).canonical(),
                0.0,
                fill = SceneColor("#fcfcfc"),
                stroke = SceneColor("#707070"),
                strokeWidth = 1.0,
            )
            commands += DrawText(lane.label, ScenePoint(rect.x + titleRail + 16.0, rect.y + 25.0).canonical(), style = laneStyle)
        }
        diagram.edges.forEach { edge ->
            val fromCenter = nodePoints.getValue(edge.sourceId)
            val toCenter = nodePoints.getValue(edge.targetId)
            if (edge.sourceId == edge.targetId) {
                val shape = nodeById.getValue(edge.sourceId).shape
                val upper = ScenePoint(fromCenter.x + nodeWidth, fromCenter.y - nodeHeight / 2)
                val lower = ScenePoint(fromCenter.x + nodeWidth, fromCenter.y + nodeHeight / 2)
                val from = swimlaneBoundaryPoint(fromCenter, upper, shape, nodeWidth, nodeHeight)
                val to = swimlaneBoundaryPoint(fromCenter, lower, shape, nodeWidth, nodeHeight)
                val right = fromCenter.x + nodeWidth / 2 + 16.0
                val points = listOf(from, ScenePoint(right, from.y), ScenePoint(right, to.y), to)
                commands += DrawPolyline(points, stroke = SceneColor("#666666"), strokeWidth = 1.0)
                commands += arrowHead(points[points.lastIndex - 1], to, fill = SceneColor("#333333"))
                edge.label?.let { label ->
                    commands += DrawText(label, ScenePoint(fromCenter.x, fromCenter.y - nodeHeight / 2 - 8), TextAnchor.MIDDLE, edgeStyle)
                }
                return@forEach
            }
            val from = swimlaneBoundaryPoint(fromCenter, toCenter, nodeById.getValue(edge.sourceId).shape, nodeWidth, nodeHeight)
            val to = swimlaneBoundaryPoint(toCenter, fromCenter, nodeById.getValue(edge.targetId).shape, nodeWidth, nodeHeight)
            commands += DrawLine(from.canonical(), to.canonical(), stroke = SceneColor("#666666"), strokeWidth = 1.0)
            commands += arrowHead(from, to, fill = SceneColor("#333333"))
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
                SwimlaneNodeShape.DECISION -> {
                    val diamond = listOf(
                        ScenePoint(point.x, point.y - nodeHeight / 2.0),
                        ScenePoint(point.x + nodeWidth / 2.0, point.y),
                        ScenePoint(point.x, point.y + nodeHeight / 2.0),
                        ScenePoint(point.x - nodeWidth / 2.0, point.y),
                    ).map { it.canonical() }
                    commands += DrawPolygon(diamond, fill = SceneColor("#fef3c7"))
                    commands += DrawPolyline(diamond + diamond.first(), stroke = SceneColor("#d97706"), strokeWidth = 1.5)
                }
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
        val descriptionStyle = TextStyle(fontSize = 11.0, color = SceneColor("#64748b"))
        val itemStyle = TextStyle(fontSize = 12.0)
        val titleStyle = TextStyle(fontSize = 20.0, fontWeight = 600)
        val itemsByDomain = diagram.domains.associate { it.domain to it.items }
        val measuredItemWidth = itemsByDomain.values.flatten().maxOfOrNull { textMeasurer.measure(it, itemStyle).width } ?: 0.0
        val measuredDescriptionWidth = CynefinDomain.entries.maxOf { domain ->
            max(
                cynefinDomainModel(domain)?.let { textMeasurer.measure(it, descriptionStyle).width } ?: 0.0,
                textMeasurer.measure(cynefinDomainPractice(domain), descriptionStyle).width,
            )
        }
        val quadrantWidth = max(280.0, max(measuredItemWidth, measuredDescriptionWidth) + 56.0)
        val quadrantHeight = max(220.0, (diagram.domains.maxOfOrNull { it.items.size } ?: 0) * 32.0 + 126.0)
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
        CynefinDomain.entries.forEach { domain ->
            val center = centers.getValue(domain)
            val items = itemsByDomain[domain].orEmpty()
            val visibleItems = if (domain == CynefinDomain.CONFUSION) items.take(3) else items
            val titleY = if (domain == CynefinDomain.CONFUSION) center.y - 28.0 else center.y - quadrantHeight / 2.0 + 30.0
            commands += DrawText(cynefinDomainLabel(domain), ScenePoint(center.x, titleY), TextAnchor.MIDDLE, domainStyle)
            var lineY = titleY + 20.0
            cynefinDomainModel(domain)?.let { model ->
                commands += DrawText(model, ScenePoint(center.x, lineY), TextAnchor.MIDDLE, descriptionStyle)
                lineY += 16.0
            }
            commands += DrawText(cynefinDomainPractice(domain), ScenePoint(center.x, lineY), TextAnchor.MIDDLE, descriptionStyle)
            val itemStartY = if (domain == CynefinDomain.CONFUSION) lineY + 20.0 else lineY + 22.0
            val itemStep = if (domain == CynefinDomain.CONFUSION) 19.0 else 30.0
            visibleItems.forEachIndexed { index, item ->
                commands += DrawText(item, ScenePoint(center.x, itemStartY + index * itemStep), TextAnchor.MIDDLE, itemStyle)
            }
            if (domain == CynefinDomain.CONFUSION && items.size > 3) {
                commands += DrawText("+${items.size - 3} more", ScenePoint(center.x, itemStartY + visibleItems.size * itemStep), TextAnchor.MIDDLE, itemStyle)
            }
        }
        return LayoutScene(width.xyCoordinate(), height.xyCoordinate(), commands)
    }

    private fun cynefinBoundaryPoint(center: ScenePoint, toward: ScenePoint, ellipse: Boolean, quadrantWidth: Double, quadrantHeight: Double): ScenePoint =
        usecaseBoundaryPoint(center, toward, if (ellipse) 92.0 else quadrantWidth / 2.0, if (ellipse) 66.0 else quadrantHeight / 2.0, ellipse)

    private fun cynefinDomainLabel(domain: CynefinDomain): String =
        domain.name.lowercase().replaceFirstChar { it.uppercase() }

    private fun cynefinDomainModel(domain: CynefinDomain): String? = when (domain) {
        CynefinDomain.COMPLEX -> "Probe → Sense → Respond"
        CynefinDomain.COMPLICATED -> "Sense → Analyse → Respond"
        CynefinDomain.CLEAR -> "Sense → Categorise → Respond"
        CynefinDomain.CHAOTIC -> "Act → Sense → Respond"
        CynefinDomain.CONFUSION -> null
    }

    private fun cynefinDomainPractice(domain: CynefinDomain): String = when (domain) {
        CynefinDomain.COMPLEX -> "Emergent Practices"
        CynefinDomain.COMPLICATED -> "Good Practices"
        CynefinDomain.CLEAR -> "Best Practices"
        CynefinDomain.CHAOTIC -> "Novel Practices"
        CynefinDomain.CONFUSION -> "Disorder"
    }

    private fun layoutC4(diagram: C4Diagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val style = TextStyle(fontSize = 13.0, fontWeight = 600)
        val bodyStyle = TextStyle(fontSize = 10.0)
        val titleStyle = TextStyle(fontSize = 18.0, fontWeight = 600)
        val stereotypeStyle = TextStyle(fontSize = 10.0, color = SceneColor("#64748b"))
        val contentWidth = diagram.elements.maxOf { element ->
            max(
                max(textMeasurer.measure(element.label, style).width, element.description?.let { description -> textMeasurer.measure(description, bodyStyle).width } ?: 0.0),
                textMeasurer.measure(c4Stereotype(element), stereotypeStyle).width,
            )
        }
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
            val mid = ScenePoint((fromCenter.x + toCenter.x) / 2.0, (fromCenter.y + toCenter.y) / 2.0)
            commands += DrawText(relationship.label, mid.copy(y = mid.y - 10.0).canonical(), anchor = TextAnchor.MIDDLE, style = TextStyle(fontSize = 11.0))
            relationship.technology?.let { technology ->
                commands += DrawText("[$technology]", mid.copy(y = mid.y + 12.0).canonical(), anchor = TextAnchor.MIDDLE, style = stereotypeStyle)
            }
            if (relationship.bidirectional) commands += arrowHead(to, from)
            commands += arrowHead(from, to)
        }
        diagram.elements.forEach { element ->
            val point = points.getValue(element.id)
            val fill = if (element.external) SceneColor("#fef3c7") else if (element.kind == C4ElementKind.PERSON) SceneColor("#dcfce7") else SceneColor("#dbeafe")
            val stroke = SceneColor("#2563eb")
            commands += DrawRect(SceneRect(point.x - cardWidth / 2.0, point.y - cardHeight / 2.0, cardWidth, cardHeight).canonical(), 8.0, fill = fill, stroke = stroke, strokeWidth = 1.5)
            if (element.kind == C4ElementKind.PERSON) {
                commands += DrawEllipse(ScenePoint(point.x, point.y - cardHeight / 2.0).canonical(), 10.0, 10.0, fill = fill, stroke = stroke, strokeWidth = 1.5)
            }
            commands += DrawText(element.label, point.copy(y = point.y - 16.0).canonical(), anchor = TextAnchor.MIDDLE, style = style)
            commands += DrawText(c4Stereotype(element), point.copy(y = point.y + 2.0).canonical(), anchor = TextAnchor.MIDDLE, style = stereotypeStyle)
            element.description?.let { commands += DrawText(it, point.copy(y = point.y + 20.0).canonical(), anchor = TextAnchor.MIDDLE, style = bodyStyle) }
        }
        return LayoutScene(width.xyCoordinate(), height.xyCoordinate(), commands)
    }

    private fun c4Stereotype(element: C4Element): String = when (element.kind) {
        C4ElementKind.PERSON -> "[Person]"
        C4ElementKind.SYSTEM -> "[Software System]"
    }

    private fun layoutArchitecture(diagram: ArchitectureDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val textStyle = TextStyle(fontSize = 13.0, fontWeight = 600)
        val groupStyle = TextStyle(fontSize = 15.0, fontWeight = 600)
        val iconStyle = TextStyle(fontSize = 10.0, color = SceneColor("#475569"))
        val serviceLabelWidth = diagram.services.maxOfOrNull { textMeasurer.measure(it.label, textStyle).width } ?: 0.0
        val serviceIconWidth = diagram.services.maxOfOrNull { architectureIconReserve(it.icon, textMeasurer, iconStyle) } ?: 0.0
        val groupLabelWidth = diagram.groups.maxOfOrNull { textMeasurer.measure(it.label, groupStyle).width } ?: 0.0
        val groupIconWidth = diagram.groups.maxOfOrNull { architectureIconReserve(it.icon, textMeasurer, iconStyle) } ?: 0.0
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
            commands += architectureIconCommands(
                icon = group.icon,
                center = if (isKnownArchitectureIcon(group.icon)) ScenePoint(rect.x + rect.width - 24.0, rect.y + 20.0) else ScenePoint(rect.x + rect.width - 14.0, rect.y + 24.0),
                unknownAnchor = TextAnchor.END,
                unknownStyle = iconStyle,
            )
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
            commands += architectureIconCommands(
                icon = service.icon,
                center = if (isKnownArchitectureIcon(service.icon)) point.copy(y = point.y - 14.0).canonical() else point.copy(y = point.y - 11.0).canonical(),
                unknownAnchor = TextAnchor.MIDDLE,
                unknownStyle = iconStyle,
            )
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

    private fun isKnownArchitectureIcon(icon: String): Boolean = when (icon.lowercase()) {
        "cloud", "database", "server" -> true
        else -> false
    }

    private fun architectureIconReserve(icon: String, textMeasurer: TextMeasurer, style: TextStyle): Double =
        if (isKnownArchitectureIcon(icon)) 22.0 else textMeasurer.measure(icon, style).width

    private fun architectureIconCommands(
        icon: String,
        center: ScenePoint,
        unknownAnchor: TextAnchor,
        unknownStyle: TextStyle,
    ): List<DrawCommand> {
        val origin = center.canonical()
        val fill = SceneColor("#ffffff")
        val stroke = SceneColor("#475569")
        return when (icon.lowercase()) {
            "cloud" -> architectureCloudIcon(origin, fill, stroke)
            "database" -> architectureDatabaseIcon(origin, fill, stroke)
            "server" -> architectureServerIcon(origin, fill, stroke)
            else -> listOf(DrawText(icon, origin, unknownAnchor, unknownStyle))
        }
    }

    private fun architectureCloudIcon(center: ScenePoint, fill: SceneColor, stroke: SceneColor): List<DrawCommand> = listOf(
        DrawEllipse(ScenePoint(center.x - 7.0, center.y + 2.0).canonical(), 8.0, 5.5, fill = fill, stroke = stroke, strokeWidth = 1.2),
        DrawEllipse(ScenePoint(center.x + 7.0, center.y + 2.0).canonical(), 8.0, 5.5, fill = fill, stroke = stroke, strokeWidth = 1.2),
        DrawEllipse(ScenePoint(center.x, center.y - 3.0).canonical(), 9.0, 6.0, fill = fill, stroke = stroke, strokeWidth = 1.2),
    )

    private fun architectureDatabaseIcon(center: ScenePoint, fill: SceneColor, stroke: SceneColor): List<DrawCommand> = listOf(
        DrawRect(SceneRect(center.x - 9.0, center.y - 5.0, 18.0, 12.0).canonical(), 0.0, fill = fill, stroke = stroke, strokeWidth = 1.2),
        DrawEllipse(ScenePoint(center.x, center.y + 7.0).canonical(), 9.0, 3.5, fill = fill, stroke = stroke, strokeWidth = 1.2),
        DrawEllipse(ScenePoint(center.x, center.y - 5.0).canonical(), 9.0, 3.5, fill = fill, stroke = stroke, strokeWidth = 1.2),
    )

    private fun architectureServerIcon(center: ScenePoint, fill: SceneColor, stroke: SceneColor): List<DrawCommand> = listOf(
        DrawRect(SceneRect(center.x - 10.0, center.y - 11.0, 20.0, 7.0).canonical(), 1.5, fill = fill, stroke = stroke, strokeWidth = 1.2),
        DrawRect(SceneRect(center.x - 10.0, center.y - 3.0, 20.0, 7.0).canonical(), 1.5, fill = fill, stroke = stroke, strokeWidth = 1.2),
        DrawRect(SceneRect(center.x - 10.0, center.y + 5.0, 20.0, 7.0).canonical(), 1.5, fill = fill, stroke = stroke, strokeWidth = 1.2),
    )

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
                ScenePoint(centerX - 92.0, 280.0 + titleOffset),
                ScenePoint(centerX + 92.0, 280.0 + titleOffset),
                ScenePoint(centerX, 148.0 + titleOffset),
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
            val valueText = (node.value ?: if (node.children.isNotEmpty()) node.treemapWeight() else null)?.canonicalNumber()
            if (node.children.isEmpty()) {
                commands += DrawText(
                    node.label,
                    ScenePoint(rect.x + rect.width / 2.0, rect.y + rect.height / 2.0 - 4.0).canonical(),
                    TextAnchor.MIDDLE,
                    labelStyle,
                )
                valueText?.let {
                    commands += DrawText(
                        it,
                        ScenePoint(rect.x + rect.width / 2.0, rect.y + rect.height / 2.0 + 14.0).canonical(),
                        TextAnchor.MIDDLE,
                        valueStyle,
                    )
                }
                return
            }
            commands += DrawText(node.label, ScenePoint(rect.x + 8.0, rect.y + 18.0).canonical(), style = labelStyle)
            valueText?.let {
                commands += DrawText(
                    it,
                    ScenePoint(rect.x + rect.width - 8.0, rect.y + 18.0).canonical(),
                    TextAnchor.END,
                    valueStyle,
                )
            }
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
        val maxValue = diagram.links.maxOf { it.value }
        val incoming = diagram.nodes.associate { node -> node.id to diagram.links.filter { it.targetId == node.id }.sumOf { it.value } }
        val outgoing = diagram.nodes.associate { node -> node.id to diagram.links.filter { it.sourceId == node.id }.sumOf { it.value } }
        fun nodeWeight(id: String): Double = max(incoming.getValue(id), outgoing.getValue(id))
        fun nodeCaption(label: String, id: String): String = "${label} ${nodeWeight(id).canonicalNumber()}"
        val nodeWidth = max(160.0, diagram.nodes.maxOf { textMeasurer.measure(nodeCaption(it.label, it.id), textStyle).width + 32.0 }).xyCoordinate()
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
                nodeCaption(node.label, node.id),
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
        val headerHeight = 40.0
        val cardHeight = 48.0
        val cardGap = 16.0
        val widths = diagram.columns.map { column ->
            max(180.0, max(
                textMeasurer.measure(column.title, titleStyle).width,
                column.cards.maxOf { textMeasurer.measure(it.label, cardStyle).width },
            ) + 32.0).xyCoordinate()
        }
        val columnHeights = diagram.columns.map { column ->
            headerHeight + 12.0 + column.cards.size * (cardHeight + cardGap) + 8.0
        }
        val height = (config.padding * 2 + (columnHeights.maxOrNull() ?: (headerHeight + 24.0))).xyCoordinate()
        val width = (config.padding * 2 + widths.sum() + gap * (widths.size - 1)).xyCoordinate()
        val commands = mutableListOf<DrawCommand>()
        var x = config.padding
        diagram.columns.forEachIndexed { index, column ->
            val columnWidth = widths[index]
            val columnHeight = columnHeights[index]
            commands += DrawRect(SceneRect(x, config.padding, columnWidth, columnHeight), cornerRadius = 8.0, fill = SceneColor("#f1f5f9"))
            commands += DrawText(column.title, ScenePoint(x + columnWidth / 2.0, config.padding + 26.0), TextAnchor.MIDDLE, titleStyle)
            commands += DrawLine(
                ScenePoint(x + 12.0, config.padding + headerHeight),
                ScenePoint(x + columnWidth - 12.0, config.padding + headerHeight),
                stroke = SceneColor("#cbd5e1"),
            )
            column.cards.forEachIndexed { cardIndex, card ->
                val y = config.padding + headerHeight + 12.0 + cardIndex * (cardHeight + cardGap)
                commands += DrawRect(SceneRect(x + 10.0, y, columnWidth - 20.0, cardHeight), cornerRadius = 6.0, fill = SceneColor("#ffffff"))
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
        val labelWidth = (diagram.branches.maxOf { textMeasurer.measure(it.name, labelStyle).width } + 36.0).xyCoordinate()
        val columnWidths = diagram.commits.map { commit ->
            max(
                80.0,
                max(
                    textMeasurer.measure(commit.id, commitStyle).width,
                    commit.tag?.let { textMeasurer.measure(it, tagStyle).width } ?: 0.0,
                ) + 28.0,
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
        val firstLaneY = config.padding + 52.0
        val height = firstLaneY + (diagram.branches.size - 1) * laneGap + 80.0
        val laneByName = diagram.branches.mapIndexed { index, branch -> branch.name to index }.toMap()
        val centerById = diagram.commits.mapIndexed { index, commit ->
            commit.id to ScenePoint(centersX[index], firstLaneY + laneByName.getValue(commit.branch) * laneGap)
        }.toMap()
        val colors = listOf("#2563eb", "#16a34a", "#ea580c", "#7c3aed", "#0891b2")
        val commands = mutableListOf<DrawCommand>()

        diagram.branches.forEachIndexed { index, branch ->
            val y = firstLaneY + index * laneGap
            val color = SceneColor(colors[index % colors.size])
            val chipWidth = textMeasurer.measure(branch.name, labelStyle).width + 16.0
            val branchStartX = branch.parentCommitId?.let { centerById.getValue(it).x } ?: startX
            val branchEndX = diagram.commits
                .filter { it.branch == branch.name }
                .maxOfOrNull { centerById.getValue(it.id).x }
                ?.let { max(it, branchStartX) }
                ?: branchStartX
            commands += DrawRect(
                SceneRect(config.padding - 2.0, y - 12.0, chipWidth, 22.0),
                cornerRadius = 4.0,
                fill = color,
                stroke = color,
            )
            commands += DrawText(
                branch.name,
                ScenePoint(config.padding + 6.0, y + 4.0),
                style = labelStyle.copy(color = SceneColor("#ffffff")),
            )
            commands += DrawLine(ScenePoint(branchStartX, y), ScenePoint(branchEndX, y), stroke = color, strokeWidth = 2.0)
        }
        diagram.commits.forEach { commit ->
            val center = centerById.getValue(commit.id)
            val color = SceneColor(colors[laneByName.getValue(commit.branch) % colors.size])
            commit.parentIds.forEach { parentId ->
                commands += DrawLine(centerById.getValue(parentId), center, stroke = color, strokeWidth = 2.0)
            }
        }
        // Later commits can connect back through earlier commit markers and labels.
        // Finish all edges before painting any commit body or decoration.
        diagram.commits.forEach { commit ->
            val center = centerById.getValue(commit.id)
            val color = SceneColor(colors[laneByName.getValue(commit.branch) % colors.size])
            when {
                commit.type == GitGraphCommitType.HIGHLIGHT -> {
                    commands += DrawRect(SceneRect(center.x - 12.0, center.y - 10.0, 24.0, 20.0), cornerRadius = 2.0, fill = color, stroke = color)
                    commands += DrawRect(SceneRect(center.x - 6.0, center.y - 6.0, 12.0, 12.0), cornerRadius = 1.0, fill = SceneColor("#ffffff"), stroke = color, strokeWidth = 1.0)
                }
                commit.isMerge -> {
                    commands += DrawEllipse(center, 9.0, 9.0, fill = color, stroke = color)
                    commands += DrawEllipse(center, 5.0, 5.0, fill = SceneColor("#ffffff"), stroke = SceneColor("#ffffff"))
                }
                else -> {
                    commands += DrawEllipse(center, 9.0, 9.0, fill = color, stroke = color)
                    if (commit.type == GitGraphCommitType.REVERSE) {
                        commands += DrawLine(ScenePoint(center.x - 5.0, center.y - 5.0), ScenePoint(center.x + 5.0, center.y + 5.0), stroke = SceneColor("#ffffff"), strokeWidth = 2.0)
                        commands += DrawLine(ScenePoint(center.x + 5.0, center.y - 5.0), ScenePoint(center.x - 5.0, center.y + 5.0), stroke = SceneColor("#ffffff"), strokeWidth = 2.0)
                    }
                }
            }
            commit.tag?.let { tag ->
                val tagWidth = textMeasurer.measure(tag, tagStyle).width
                val top = center.y - 34.0
                val bottom = center.y - 18.0
                val left = center.x - tagWidth / 2.0
                val right = center.x + tagWidth / 2.0 + 6.0
                val tipX = left - 10.0
                commands += DrawPolygon(
                    listOf(
                        ScenePoint(tipX + 4.0, center.y - 22.0),
                        ScenePoint(tipX + 4.0, center.y - 26.0),
                        ScenePoint(left, top),
                        ScenePoint(right, top),
                        ScenePoint(right, bottom),
                        ScenePoint(left, bottom),
                    ),
                    fill = SceneColor("#ede9fe"),
                )
                commands += DrawEllipse(ScenePoint(tipX + 8.0, center.y - 24.0), 2.0, 2.0, fill = SceneColor("#7c3aed"), stroke = SceneColor("#7c3aed"))
                commands += DrawText(tag, ScenePoint(center.x, center.y - 20.0), TextAnchor.MIDDLE, tagStyle)
            }
            val idWidth = textMeasurer.measure(commit.id, commitStyle).width
            commands += DrawRect(
                SceneRect(center.x - idWidth / 2.0 - 6.0, center.y + 16.0, idWidth + 12.0, 16.0),
                cornerRadius = 3.0,
                fill = SceneColor("#f8fafc"),
                stroke = SceneColor("#cbd5e1"),
                strokeWidth = 1.0,
            )
            commands += DrawText(commit.id, ScenePoint(center.x, center.y + 28.0), TextAnchor.MIDDLE, commitStyle)
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutPacket(diagram: PacketDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val labelStyle = TextStyle(fontSize = 11.0)
        val bitIndexStyle = TextStyle(fontSize = 9.0, fontWeight = 600)
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
        val indexBand = 14.0
        val blockHeight = 38.0
        val rowHeight = indexBand + blockHeight + 8.0
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
                commands += DrawText("$segmentStart", ScenePoint(x, y + 10.0), TextAnchor.START, bitIndexStyle)
                commands += DrawText("$segmentEnd", ScenePoint(x + segmentWidth, y + 10.0), TextAnchor.END, bitIndexStyle)
                commands += DrawRect(SceneRect(x, y + indexBand, segmentWidth, blockHeight), cornerRadius = 2.0, fill = SceneColor("#eff6ff"))
                commands += DrawText(field.label, ScenePoint(x + segmentWidth / 2.0, y + indexBand + 23.0), TextAnchor.MIDDLE, labelStyle)
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
        val quadrantTitle = TextStyle(fontSize = 12.0, fontWeight = 600)
        val commands = mutableListOf<DrawCommand>()
        diagram.title?.let { commands += DrawText(it, ScenePoint(width / 2.0, 26.0), TextAnchor.MIDDLE, TextStyle(fontSize = 18.0, fontWeight = 600)) }
        val fills = listOf(
            SceneColor("#dbeafe"),
            SceneColor("#dcfce7"),
            SceneColor("#fef3c7"),
            SceneColor("#fce7f3"),
        )
        val quadrantRects = listOf(
            SceneRect(midX, top, right - midX, midY - top),
            SceneRect(left, top, midX - left, midY - top),
            SceneRect(left, midY, midX - left, bottom - midY),
            SceneRect(midX, midY, right - midX, bottom - midY),
        )
        quadrantRects.forEachIndexed { index, rect ->
            commands += DrawRect(rect, cornerRadius = 0.0, fill = fills[index], stroke = fills[index], strokeWidth = 1.0)
        }
        commands += DrawLine(ScenePoint(midX, top), ScenePoint(midX, bottom))
        commands += DrawLine(ScenePoint(left, midY), ScenePoint(right, midY))
        commands += DrawPolyline(
            listOf(
                ScenePoint(left, top),
                ScenePoint(right, top),
                ScenePoint(right, bottom),
                ScenePoint(left, bottom),
                ScenePoint(left, top),
            ),
            stroke = SceneColor("#334155"),
        )
        commands += DrawText(diagram.xAxis.lowLabel, ScenePoint(left, bottom + 22.0), style = body)
        commands += DrawText(diagram.xAxis.highLabel, ScenePoint(right, bottom + 22.0), TextAnchor.END, body)
        commands += DrawText(diagram.yAxis.lowLabel, ScenePoint(left - 10.0, bottom), TextAnchor.END, body)
        commands += DrawText(diagram.yAxis.highLabel, ScenePoint(left - 10.0, top + 10.0), TextAnchor.END, body)
        val quadrantPositions = listOf(
            ScenePoint((midX + right) / 2.0, top + 20.0) to TextAnchor.MIDDLE,
            ScenePoint((left + midX) / 2.0, top + 20.0) to TextAnchor.MIDDLE,
            ScenePoint((left + midX) / 2.0, midY + 20.0) to TextAnchor.MIDDLE,
            ScenePoint((midX + right) / 2.0, midY + 20.0) to TextAnchor.MIDDLE,
        )
        diagram.quadrantLabels.forEachIndexed { index, label ->
            label?.let { commands += DrawText(it, quadrantPositions[index].first, quadrantPositions[index].second, quadrantTitle) }
        }
        diagram.points.forEach { point ->
            val x = (left + point.x * (right - left)).xyCoordinate()
            val y = (bottom - point.y * (bottom - top)).xyCoordinate()
            commands += DrawPolygon(listOf(ScenePoint(x, y - 5.0), ScenePoint(x + 5.0, y), ScenePoint(x, y + 5.0), ScenePoint(x - 5.0, y)), fill = SceneColor("#2563eb"))
            commands += DrawText(point.label, ScenePoint(x + 8.0, y - 7.0), style = body)
        }
        return LayoutScene(width, height, commands)
    }

    /** Deterministic polar-grid layout for the bounded radar-beta slice. */
    private fun layoutRadar(
        diagram: RadarChartDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene {
        val titleStyle = TextStyle(fontSize = 18.0, fontWeight = 600)
        val bodyStyle = TextStyle(fontSize = 12.0)
        val radius = 170.0
        val labelRadius = radius + 26.0
        val ringFractions = listOf(0.2, 0.4, 0.6, 0.8, 1.0)
        val axisCount = diagram.axes.size
        val axisAngles = List(axisCount) { index -> -PI / 2.0 + 2.0 * PI * index / axisCount }

        // Measure axis labels up front so the canvas grows instead of clipping.
        val axisLabelSizes = diagram.axes.map { textMeasurer.measure(it.label, bodyStyle) }
        // A single legend item wider than the default box also widens the canvas.
        val curveLabelSizes = diagram.curves.map { textMeasurer.measure(it.label, bodyStyle) }
        val widestLegendItem = curveLabelSizes.maxOfOrNull { 18.0 + it.width + 28.0 } ?: 0.0
        val baseWidth = max(640.0, widestLegendItem + 2.0 * config.padding)
        var extraLeft = 0.0
        var extraRight = 0.0
        axisAngles.forEachIndexed { index, angle ->
            val labelCenterX = baseWidth / 2.0 + labelRadius * cos(angle)
            extraLeft = max(extraLeft, config.padding - (labelCenterX - axisLabelSizes[index].width / 2.0))
            extraRight = max(extraRight, labelCenterX + axisLabelSizes[index].width / 2.0 + config.padding - baseWidth)
        }
        val width = baseWidth + max(0.0, extraLeft) + max(0.0, extraRight)
        val centerX = baseWidth / 2.0 + max(0.0, extraLeft)
        val centerY = 272.0

        fun vertex(axisIndex: Int, fraction: Double): ScenePoint {
            val angle = axisAngles[axisIndex]
            return ScenePoint(
                (centerX + radius * fraction * cos(angle)).radarCoordinate(),
                (centerY + radius * fraction * sin(angle)).radarCoordinate(),
            )
        }

        val commands = mutableListOf<DrawCommand>()
        diagram.title?.let {
            commands += DrawText(it, ScenePoint(centerX, 26.0), TextAnchor.MIDDLE, titleStyle)
        }
        // Official radar uses circular graticule rings, not axis polygons.
        ringFractions.forEach { fraction ->
            val ringRadius = radius * fraction
            commands += DrawEllipse(
                ScenePoint(centerX, centerY),
                ringRadius,
                ringRadius,
                fillOpacity = 0.0,
                stroke = SceneColor("#d4d4d4"),
                strokeWidth = 1.0,
            )
            commands += DrawText(
                (diagram.maximum * fraction).radarTickLabel(),
                ScenePoint(centerX + 8.0, (centerY - ringRadius + 4.0).radarCoordinate()),
                style = bodyStyle,
            )
        }
        // Spokes and outer axis labels.
        diagram.axes.forEachIndexed { index, axis ->
            commands += DrawLine(ScenePoint(centerX, centerY), vertex(index, 1.0))
            val angle = axisAngles[index]
            commands += DrawText(
                axis.label,
                ScenePoint(
                    (centerX + labelRadius * cos(angle)).radarCoordinate(),
                    (centerY + labelRadius * sin(angle)).radarCoordinate() + 4.0,
                ),
                TextAnchor.MIDDLE,
                bodyStyle,
            )
        }
        // Curves as filled polygons plus closed strokes with diamond vertices.
        diagram.curves.forEachIndexed { curveIndex, curve ->
            val fill = RADAR_CURVE_FILLS[curveIndex % RADAR_CURVE_FILLS.size]
            val stroke = RADAR_CURVE_STROKES[curveIndex % RADAR_CURVE_STROKES.size]
            val points = curve.values.mapIndexed { axisIndex, value ->
                vertex(axisIndex, (value / diagram.maximum).coerceIn(0.0, 1.0))
            }
            val curvePoints = closedCurveSamples(points)
            commands += DrawPolygon(curvePoints, fill = SceneColor(fill))
            commands += DrawPolyline(curvePoints + curvePoints.first(), stroke = SceneColor(stroke), strokeWidth = 2.0)
            points.forEach { point ->
                commands += DrawPolygon(
                    listOf(
                        ScenePoint(point.x, point.y - 4.0),
                        ScenePoint(point.x + 4.0, point.y),
                        ScenePoint(point.x, point.y + 4.0),
                        ScenePoint(point.x - 4.0, point.y),
                    ),
                    fill = SceneColor(stroke),
                )
            }
        }
        // Legend row(s) below the chart, wrapped inside the padded content box.
        var legendX = config.padding
        var legendY = centerY + radius + 46.0
        diagram.curves.forEachIndexed { curveIndex, curve ->
            val stroke = RADAR_CURVE_STROKES[curveIndex % RADAR_CURVE_STROKES.size]
            val itemWidth = 18.0 + textMeasurer.measure(curve.label, bodyStyle).width + 28.0
            if (legendX > config.padding && legendX + itemWidth > width - config.padding) {
                legendX = config.padding
                legendY += 22.0
            }
            commands += DrawRect(
                SceneRect(legendX, legendY - 10.0, 12.0, 12.0),
                fill = SceneColor(stroke),
                stroke = SceneColor(stroke),
                strokeWidth = 1.0,
            )
            commands += DrawText(curve.label, ScenePoint(legendX + 18.0, legendY), style = bodyStyle)
            legendX += itemWidth
        }
        val height = max(520.0, legendY + 12.0 + config.padding)
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
        val actors = diagram.sections.flatMap { it.tasks }.flatMap { it.actors }.distinct()
        val contentWidth = config.padding * 2 + sectionWidth + maxTasks * taskWidth + max(0, maxTasks - 1) * taskGap
        val titleWidth = diagram.title?.let { textMeasurer.measure(it, titleStyle).width + config.padding * 2 } ?: 0.0
        val legendWidth = if (actors.isEmpty()) 0.0 else actors.sumOf { 28.0 + textMeasurer.measure(it, body).width + 16.0 }
        val width = max(640.0, max(contentWidth, max(titleWidth, legendWidth + config.padding * 2)))
        val titleHeight = if (diagram.title == null) 18.0 else 42.0
        val cardHeight = 86.0
        val rowHeight = 110.0
        val legendHeight = if (actors.isEmpty()) 0.0 else 36.0
        val height = config.padding * 2 + titleHeight + diagram.sections.size * rowHeight + legendHeight
        val commands = mutableListOf<DrawCommand>()

        diagram.title?.let {
            commands += DrawText(it, ScenePoint(width / 2.0, config.padding + 18.0), TextAnchor.MIDDLE, titleStyle)
        }
        diagram.sections.forEachIndexed { sectionIndex, section ->
            val y = config.padding + titleHeight + sectionIndex * rowHeight
            commands += DrawRect(
                SceneRect(config.padding, y, sectionWidth - 12.0, cardHeight),
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
                    SceneRect(x, y, taskWidth, cardHeight),
                    cornerRadius = 8.0,
                    fill = SceneColor(fill),
                )
                commands += DrawText(task.label, ScenePoint(x + 10.0, y + 22.0), style = body)
                commands += DrawText(
                    "Score ${task.score} · ${task.actors.joinToString(", ")}",
                    ScenePoint(x + 10.0, y + 42.0),
                    style = body,
                )
                repeat(5) { index ->
                    commands += DrawEllipse(
                        ScenePoint(x + 16.0 + index * 14.0, y + 64.0),
                        5.0,
                        5.0,
                        fill = SceneColor(if (index < task.score) "#334155" else "#ffffff"),
                        stroke = SceneColor("#334155"),
                        strokeWidth = 1.0,
                    )
                }
                task.actors.forEachIndexed { actorIndex, actor ->
                    val color = JOURNEY_ACTOR_COLORS[actors.indexOf(actor) % JOURNEY_ACTOR_COLORS.size]
                    commands += DrawEllipse(
                        ScenePoint(x + taskWidth - 14.0 - actorIndex * 12.0, y + 16.0),
                        4.0,
                        4.0,
                        fill = SceneColor(color),
                        stroke = SceneColor("#334155"),
                        strokeWidth = 1.0,
                    )
                }
            }
        }
        if (actors.isNotEmpty()) {
            var legendX = config.padding
            val legendY = config.padding + titleHeight + diagram.sections.size * rowHeight + 8.0
            actors.forEachIndexed { index, actor ->
                val color = JOURNEY_ACTOR_COLORS[index % JOURNEY_ACTOR_COLORS.size]
                commands += DrawEllipse(
                    ScenePoint(legendX + 6.0, legendY),
                    6.0,
                    6.0,
                    fill = SceneColor(color),
                    stroke = SceneColor("#334155"),
                    strokeWidth = 1.0,
                )
                commands += DrawText(actor, ScenePoint(legendX + 16.0, legendY + 4.0), style = body)
                legendX += 28.0 + textMeasurer.measure(actor, body).width + 16.0
            }
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutTimeline(diagram: TimelineDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val body = TextStyle(fontSize = 12.0)
        val sectionStyle = TextStyle(fontSize = 13.0, fontWeight = 600)
        val eventLineHeight = 18.0
        fun eventHeight(event: TimelineEvent): Double =
            max(42.0, 16.0 + max(1, event.labels.size) * eventLineHeight)
        val labelWidth = diagram.events.maxOf { textMeasurer.measure(it.period, body).width } + 32.0
        val sectionWidth = diagram.events.mapNotNull { it.section }.maxOfOrNull { textMeasurer.measure(it, sectionStyle).width } ?: 0.0
        val detailWidth = diagram.events.maxOf { event ->
            event.labels.maxOfOrNull { textMeasurer.measure(it, body).width } ?: 0.0
        } + 40.0
        val width = max(420.0, config.padding * 2 + labelWidth + max(300.0, max(sectionWidth + 40.0, detailWidth)))
        val axisX = config.padding + labelWidth
        var cursor = config.padding + 52.0
        var previousSection: String? = null
        val rows = mutableListOf<Triple<Double, TimelineEvent, Boolean>>()
        diagram.events.forEach { event ->
            val newSection = event.section != null && event.section != previousSection
            if (newSection) cursor += 8.0
            rows += Triple(cursor, event, newSection)
            previousSection = event.section
            cursor += eventHeight(event)
        }
        val height = cursor + config.padding
        val commands = mutableListOf<DrawCommand>()
        diagram.title?.let { commands += DrawText(it, ScenePoint(config.padding, config.padding + 18.0), style = TextStyle(fontSize = 18.0, fontWeight = 600)) }
        commands += DrawLine(ScenePoint(axisX, config.padding + 32.0), ScenePoint(axisX, height - config.padding))
        rows.forEach { (y, event, newSection) ->
            if (newSection) {
                commands += DrawText(requireNotNull(event.section), ScenePoint(axisX + 18.0, y - 18.0), style = sectionStyle)
                commands += DrawLine(
                    ScenePoint(config.padding, y - 12.0),
                    ScenePoint(width - config.padding, y - 12.0),
                    stroke = SceneColor("#cbd5e1"),
                )
            }
            commands += DrawText(event.period, ScenePoint(config.padding, y + 5.0), style = body)
            commands += DrawPolygon(listOf(ScenePoint(axisX, y), ScenePoint(axisX + 7.0, y + 7.0), ScenePoint(axisX, y + 14.0), ScenePoint(axisX - 7.0, y + 7.0)), fill = SceneColor("#2563eb"))
            event.labels.forEachIndexed { labelIndex, label ->
                commands += DrawText(label, ScenePoint(axisX + 18.0, y + 11.0 + labelIndex * eventLineHeight), style = body)
            }
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
            val width = max(92.0, text.width + horizontalPadding)
            val height = max(42.0, text.height + 20.0)
            val side = if (node.shape == MindmapNodeShape.DOUBLE_CIRCLE) max(width, height) else 0.0
            node.id to SceneSize(
                if (node.shape == MindmapNodeShape.DOUBLE_CIRCLE) side else width,
                if (node.shape == MindmapNodeShape.DOUBLE_CIRCLE) side else height,
            )
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
            if (node.shape == MindmapNodeShape.DOUBLE_CIRCLE) {
                val center = ScenePoint(rect.x + rect.width / 2.0, rect.y + rect.height / 2.0)
                val radius = rect.width / 2.0
                commands += DrawEllipse(center, radius, radius)
                commands += DrawEllipse(center, radius - 4.0, radius - 4.0)
            } else {
                val radius = if (node.shape == MindmapNodeShape.RECTANGLE) 2.0 else 12.0
                commands += DrawRect(rect, cornerRadius = radius)
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
        val tickStyle = TextStyle(fontSize = 10.0)
        val tasks = diagram.sections.flatMap { section -> section.tasks.map { section.name to it } }
        val minDay = tasks.minOfOrNull { it.second.startDay } ?: 0
        val maxDay = tasks.maxOfOrNull { it.second.startDay + it.second.durationDays } ?: minDay + 1
        val tickLabelWidth = textMeasurer.measure("0000-00-00", tickStyle).width
        val span = max(1, maxDay - minDay)
        val scale = minOf(720.0 / span, max(28.0, tickLabelWidth + 8.0))
        val labelWidth = tasks.maxOfOrNull { textMeasurer.measure("${it.first}: ${it.second.name}", body).width }?.plus(16.0) ?: 120.0
        val rightPadding=max(config.padding,tickLabelWidth/2+2)
        val width = config.padding + rightPadding + labelWidth + span * scale
        val titleOffset = if (diagram.title == null) 0.0 else 28.0
        val axisY = config.padding + titleOffset + 8.0
        val tasksTop = axisY + 28.0
        val height = tasksTop + maxOf(1, tasks.size) * 34.0 + config.padding
        val axisStartX = (config.padding + labelWidth).xyCoordinate()
        val axisEndX = (width - rightPadding).xyCoordinate()
        val commands = mutableListOf<DrawCommand>()
        diagram.title?.let { commands += DrawText(it, ScenePoint(config.padding, config.padding + 16.0), style = TextStyle(fontSize = 18.0, fontWeight = 600)) }
        commands += DrawLine(ScenePoint(axisStartX, axisY.xyCoordinate()), ScenePoint(axisEndX, axisY.xyCoordinate()))
        val tickStep = max(1, ceil((tickLabelWidth + 8.0) / scale).toInt())
        val tickDays = ((minDay..maxDay step tickStep).toList() + maxDay).distinct().toMutableList()
        if (tickDays.size > 2 && (tickDays.last() - tickDays[tickDays.lastIndex - 1]) * scale < tickLabelWidth + 8.0) {
            tickDays.removeAt(tickDays.lastIndex - 1)
        }
        for (day in tickDays) {
            val x = (config.padding + labelWidth + (day - minDay) * scale).xyCoordinate()
            commands += DrawLine(ScenePoint(x, axisY.xyCoordinate()), ScenePoint(x, (axisY + 6.0).xyCoordinate()), strokeWidth = 1.0)
            commands += DrawText(isoDayToYmd(day), ScenePoint(x, axisY + 20.0), TextAnchor.MIDDLE, tickStyle)
        }
        var row = 0
        tasks.forEach { (section, task) ->
            val y = tasksTop + row * 34.0
            commands += DrawText("$section: ${task.name}", ScenePoint(config.padding, y + 13.0), style = body)
            val x = (config.padding + labelWidth + (task.startDay - minDay) * scale).xyCoordinate()
            val fill = when (task.status) { GanttTaskStatus.DONE -> "#16a34a"; GanttTaskStatus.ACTIVE -> "#2563eb"; GanttTaskStatus.CRITICAL -> "#dc2626"; GanttTaskStatus.TODO -> "#94a3b8" }
            if(task.milestone) {
                val center=x+task.durationDays*scale/2
                commands+=DrawPolygon(listOf(ScenePoint(center,y),ScenePoint(center+11,y+11),ScenePoint(center,y+22),ScenePoint(center-11,y+11)),fill=SceneColor(fill))
            }else commands += DrawRect(SceneRect(x, y, task.renderDurationDays * scale, 22.0), cornerRadius = 4.0, fill = SceneColor(fill))
            row++
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutXyChart(diagram: XyChartDiagram, config: LayoutConfig): LayoutScene {
        if (diagram.orientation == build.raft.mermaid.core.XyOrientation.HORIZONTAL) return layoutHorizontalXy(diagram, config)
        val bodyStyle = TextStyle(fontSize = 12.0)
        val tickStyle = TextStyle(fontSize = 11.0)
        val valueStyle = TextStyle(fontSize = 11.0, fontWeight = 600)
        val titleStyle = TextStyle(fontSize = 18.0, fontWeight = 600)
        val width = 640.0
        val height = 400.0
        val left = config.padding + 52.0
        val top = config.padding + 48.0
        val plotWidth = width - left - config.padding
        val plotHeight = height - top - config.padding - 52.0
        val bottom = top + plotHeight
        val categories = diagram.xAxis.categories
        val count = if(categories.isNotEmpty()) categories.size else maxOf(1, diagram.series.maxOfOrNull { it.values.size } ?: 0)
        val step = plotWidth / count
        val range = (diagram.yAxis.maximum - diagram.yAxis.minimum).takeIf { it != 0.0 } ?: 1.0
        fun x(index: Int, size: Int = count): Double = (if(categories.isNotEmpty()) left + step * (index + 0.5)
            else left + plotWidth * index / maxOf(1, size - 1)).xyCoordinate()
        fun y(value: Double): Double = (bottom - ((value - diagram.yAxis.minimum) / range) * plotHeight).xyCoordinate()

        val commands = mutableListOf<DrawCommand>()
        diagram.title?.let { commands += DrawText(it, ScenePoint(width / 2.0, config.padding + 18.0), TextAnchor.MIDDLE, titleStyle) }
        commands += DrawLine(ScenePoint(left, top), ScenePoint(left, bottom))
        commands += DrawLine(ScenePoint(left, bottom), ScenePoint(left + plotWidth, bottom))
        numericAxisTicks(minOf(diagram.yAxis.minimum, diagram.yAxis.maximum), maxOf(diagram.yAxis.minimum, diagram.yAxis.maximum)).forEach { tick ->
            val tickY = y(tick)
            commands += DrawLine(ScenePoint(left - 6.0, tickY), ScenePoint(left, tickY), strokeWidth = 1.0)
            commands += DrawText(tick.canonicalNumber(), ScenePoint(left - 8.0, tickY + 4.0), TextAnchor.END, tickStyle)
        }
        diagram.yAxis.title?.let { commands += DrawText(it, ScenePoint(left, top - 12.0), style = bodyStyle) }
        diagram.xAxis.title?.let { commands += DrawText(it, ScenePoint(left + plotWidth / 2.0, height - config.padding), TextAnchor.MIDDLE, bodyStyle) }
        categories.forEachIndexed { index, category ->
            val tickX = x(index)
            commands += DrawLine(ScenePoint(tickX, bottom), ScenePoint(tickX, bottom + 6.0), strokeWidth = 1.0)
            commands += DrawText(category, ScenePoint(tickX, bottom + 20.0), TextAnchor.MIDDLE, bodyStyle)
        }

        if(categories.isEmpty()) {
            val axis = diagram.xAxis.range
            val lo = axis?.minimum ?: 1.0; val hi = axis?.maximum ?: count.toDouble()
            val span = (hi - lo).takeIf { it != 0.0 } ?: 1.0
            numericAxisTicks(minOf(lo, hi), maxOf(lo, hi)).forEach { tick ->
                val tickX = left + (tick - lo) / span * plotWidth
                commands += DrawLine(ScenePoint(tickX, bottom), ScenePoint(tickX, bottom + 6.0), strokeWidth = 1.0)
                commands += DrawText(tick.canonicalNumber(), ScenePoint(tickX, bottom + 20.0), TextAnchor.MIDDLE, bodyStyle)
            }
        }
        val barSeries = diagram.series.filter { it.kind == XySeriesKind.BAR }
        val barWidth = (step * 0.64 / max(1, barSeries.size)).coerceAtMost(36.0)
        var barIndex = 0
        diagram.series.forEachIndexed { seriesIndex, original ->
            val series = if(categories.isNotEmpty()) original.copy(values = original.values.take(categories.size)) else original
            val color = SceneColor(XY_COLORS[seriesIndex % XY_COLORS.size])
            if(series.title.isNotEmpty()) commands += DrawText(series.title,
                ScenePoint(left + seriesIndex * 130.0, top - 28.0), style = bodyStyle.copy(color = color))
            when (series.kind) {
                XySeriesKind.BAR -> {
                    series.values.forEachIndexed { index, value ->
                        val baseline = y(0.0.coerceIn(minOf(diagram.yAxis.minimum, diagram.yAxis.maximum), maxOf(diagram.yAxis.minimum, diagram.yAxis.maximum)))
                        val valueY = y(value)
                        val barX = (x(index, series.values.size) - barSeries.size * barWidth / 2.0 + barIndex * barWidth).xyCoordinate()
                        commands += DrawRect(
                            rect = SceneRect(
                                x = barX,
                                y = minOf(baseline, valueY),
                                width = barWidth.xyCoordinate(),
                                height = max(1.0, kotlin.math.abs(valueY - baseline)).xyCoordinate(),
                            ),
                            fill = color,
                            stroke = color,
                            strokeWidth = 1.0,
                        )
                        commands += DrawText(
                            series.labels.getOrNull(index)?.takeIf { it.isNotEmpty() } ?: value.canonicalNumber(),
                            ScenePoint((barX + barWidth / 2.0).xyCoordinate(), (minOf(baseline, valueY) - 6.0).xyCoordinate()),
                            TextAnchor.MIDDLE,
                            valueStyle.copy(color = color),
                        )
                    }
                    barIndex += 1
                }
                XySeriesKind.LINE -> {
                    val points = series.values.mapIndexed { index, value -> ScenePoint(x(index, series.values.size), y(value)) }
                    commands += DrawPolyline(points = points, stroke = color, strokeWidth = 2.0)
                    series.values.forEachIndexed { index, value ->
                        commands += DrawText(
                            series.labels.getOrNull(index)?.takeIf { it.isNotEmpty() } ?: value.canonicalNumber(),
                            ScenePoint(x(index, series.values.size), (y(value) - 10.0).xyCoordinate()),
                            TextAnchor.MIDDLE,
                            valueStyle.copy(color = color),
                        )
                    }
                }
            }
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutHorizontalXy(diagram: XyChartDiagram, config: LayoutConfig): LayoutScene {
        val width = 640.0; val height = 400.0
        val left = config.padding + 72.0; val top = config.padding + 48.0
        val plotWidth = width - left - config.padding - 32.0
        val plotHeight = height - top - config.padding - 52.0; val bottom = top + plotHeight
        val style = TextStyle(fontSize = 12.0)
        val categories = diagram.xAxis.categories
        val count = if(categories.isNotEmpty()) categories.size else maxOf(1, diagram.series.maxOfOrNull { it.values.size } ?: 0)
        val step = plotHeight / count
        val range = (diagram.yAxis.maximum - diagram.yAxis.minimum).takeIf { it != 0.0 } ?: 1.0
        fun x(value: Double) = (left + (value - diagram.yAxis.minimum) / range * plotWidth).xyCoordinate()
        fun y(index: Int, size: Int = count) = (if(categories.isNotEmpty()) top + step * (index + 0.5)
            else top + plotHeight * index / maxOf(1, size - 1)).xyCoordinate()
        val commands = mutableListOf<DrawCommand>()
        diagram.title?.let { commands += DrawText(it, ScenePoint(width / 2, config.padding + 18), TextAnchor.MIDDLE, TextStyle(fontSize = 18.0, fontWeight = 600)) }
        commands += DrawLine(ScenePoint(left, top), ScenePoint(left, bottom))
        commands += DrawLine(ScenePoint(left, bottom), ScenePoint(left + plotWidth, bottom))
        numericAxisTicks(minOf(diagram.yAxis.minimum, diagram.yAxis.maximum), maxOf(diagram.yAxis.minimum, diagram.yAxis.maximum)).forEach { tick ->
            commands += DrawLine(ScenePoint(x(tick), bottom), ScenePoint(x(tick), bottom + 6), strokeWidth = 1.0)
            commands += DrawText(tick.canonicalNumber(), ScenePoint(x(tick), bottom + 20), TextAnchor.MIDDLE, style)
        }
        categories.forEachIndexed { i, category ->
            commands += DrawLine(ScenePoint(left - 6, y(i)), ScenePoint(left, y(i)), strokeWidth = 1.0)
            commands += DrawText(category, ScenePoint(left - 8, y(i) + 4), TextAnchor.END, style)
        }
        if(categories.isEmpty()) {
            val lo = diagram.xAxis.range?.minimum ?: 1.0; val hi = diagram.xAxis.range?.maximum ?: count.toDouble()
            val span = (hi - lo).takeIf { it != 0.0 } ?: 1.0
            numericAxisTicks(minOf(lo, hi), maxOf(lo, hi)).forEach { tick ->
                val tickY = top + (tick - lo) / span * plotHeight
                commands += DrawText(tick.canonicalNumber(), ScenePoint(left - 8, tickY + 4), TextAnchor.END, style)
            }
        }
        diagram.xAxis.title?.let { commands += DrawText(it, ScenePoint(left, top - 12), style = style) }
        diagram.yAxis.title?.let { commands += DrawText(it, ScenePoint(left + plotWidth / 2, height - config.padding), TextAnchor.MIDDLE, style) }
        val bars = diagram.series.count { it.kind == XySeriesKind.BAR }
        val thickness = minOf(36.0, step * 0.64 / maxOf(1, bars)); var barIndex = 0
        val zero = x(0.0.coerceIn(minOf(diagram.yAxis.minimum, diagram.yAxis.maximum), maxOf(diagram.yAxis.minimum, diagram.yAxis.maximum)))
        diagram.series.forEachIndexed { seriesIndex, series ->
            val values = if(categories.isNotEmpty()) series.values.take(categories.size) else series.values
            val color = SceneColor(XY_COLORS[seriesIndex % XY_COLORS.size])
            if(series.title.isNotEmpty()) commands += DrawText(series.title, ScenePoint(left + seriesIndex * 130.0, top - 28), style = style.copy(color = color))
            if(series.kind == XySeriesKind.LINE) commands += DrawPolyline(values.mapIndexed { i, value -> ScenePoint(x(value), y(i, values.size)) }, stroke = color, strokeWidth = 2.0)
            values.forEachIndexed { i, value ->
                val row = y(i, values.size)
                if(series.kind == XySeriesKind.BAR) commands += DrawRect(
                    SceneRect(minOf(zero, x(value)), row - bars * thickness / 2 + barIndex * thickness, maxOf(1.0, kotlin.math.abs(x(value) - zero)), thickness),
                    fill = color, stroke = color, strokeWidth = 1.0)
                commands += DrawText(series.labels.getOrNull(i)?.takeIf { it.isNotEmpty() } ?: value.canonicalNumber(),
                    ScenePoint(x(value) + 8, row + 4), style = style.copy(color = color, fontWeight = 600))
            }
            if(series.kind == XySeriesKind.BAR) barIndex++
        }
        return LayoutScene(width, height, commands)
    }

    private fun numericAxisTicks(minimum: Double, maximum: Double): List<Double> {
        val span = maximum - minimum
        if (span <= 0.0) return listOf(minimum.xyCoordinate())
        val raw = span / 8.0
        var magnitude = 1.0
        while (magnitude * 10.0 <= raw) magnitude *= 10.0
        while (magnitude > raw) magnitude /= 10.0
        val residual = raw / magnitude
        val step = when {
            residual <= 1.5 -> magnitude
            residual <= 3.0 -> 2.0 * magnitude
            residual <= 7.0 -> 5.0 * magnitude
            else -> 10.0 * magnitude
        }
        val ticks = mutableListOf<Double>()
        var current = ceil(minimum / step) * step
        if (current - minimum > step * 1e-9) ticks += minimum.xyCoordinate()
        while (current <= maximum + step * 1e-9) {
            ticks += current.xyCoordinate()
            current += step
        }
        if (maximum - ticks.last() > step * 1e-9) ticks += maximum.xyCoordinate()
        return ticks.distinct()
    }

    private fun layoutEntityRelationship(
        diagram: EntityRelationshipDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene {
        val nameStyle = TextStyle()
        val presentations = diagram.entities.associate { it.id to EntityRelationshipStyle(it, diagram) }
        val pad = 12.0
        val gutter = 10.0
        val sizes = diagram.entities.associate { entity ->
            val presentation = presentations.getValue(entity.id)
            val titleStyle = presentation.title
            val typeStyle = presentation.type
            val nameStyle = presentation.name
            val keyStyle = presentation.key
            val rowHeight = presentation.rowHeight
            val headerHeight = presentation.headerHeight
            val typeCol = entity.attributes.maxOfOrNull { textMeasurer.measure(it.type, typeStyle).width } ?: 0.0
            val nameCol = max(
                textMeasurer.measure(entity.alias ?: entity.id, titleStyle).width,
                entity.attributes.maxOfOrNull { textMeasurer.measure(it.name, nameStyle).width } ?: 0.0,
            )
            val keyCol = entity.attributes.maxOfOrNull { attribute ->
                if (attribute.key == EntityKey.NONE) 0.0 else textMeasurer.measure((listOf(attribute.key) + attribute.additionalKeys).filter { it != EntityKey.NONE }.joinToString(", ") { it.name }, keyStyle).width
            } ?: 0.0
            val contentWidth = if (entity.attributes.isEmpty()) {
                nameCol
            } else {
                typeCol + gutter + nameCol + if (keyCol > 0.0) gutter + keyCol else 0.0
            }
            entity.id to SceneSize(
                max(160.0, contentWidth + pad * 2),
                headerHeight + if (entity.attributes.isEmpty()) 8.0 else 8.0 + entity.attributes.size * rowHeight + 8.0,
            )
        }
        val groupPresentations = diagram.subgraphs.associate { group ->
            group.id to EntityRelationshipStyle(build.raft.mermaid.core.EntityDefinition(group.id, styles = group.styles, classes = group.classes), diagram)
        }
        val placement = EntityRelationshipPlacement(diagram, sizes, diagram.subgraphs.associate { group ->
            group.id to textMeasurer.measure(group.title, groupPresentations.getValue(group.id).title)
        }, diagram.relationships.filter { it.from == it.to }.groupBy { it.from }.mapValues { (_, relations) ->
            56.0 + relations.maxOf { textMeasurer.measure(it.label, nameStyle).width }
        }, config.padding, config.nodeGap, maxOf(config.nodeGap,
            (diagram.relationships.maxOfOrNull { textMeasurer.measure(it.label, nameStyle).width } ?: 0.0) + 24.0))
        val rects = placement.rects
        val commands = mutableListOf<DrawCommand>()
        val groups = diagram.subgraphs.associateBy { it.id }
        placement.groupOrder.forEach { id ->
            val group = groups.getValue(id)
            val rect = rects.getValue(id)
            val presentation = groupPresentations.getValue(id)
            commands += DrawRect(rect, cornerRadius = 6.0, fill = presentation.fill, stroke = presentation.stroke, strokeWidth = presentation.strokeWidth)
            commands += DrawText(group.title, ScenePoint(rect.x + 24.0, rect.y + 10.0 + presentation.title.fontSize), style = presentation.title)
        }
        var width = placement.width
        val height = placement.height
        diagram.relationships.forEach { relationship ->
            val source = rects[relationship.from] ?: return@forEach
            val target = rects[relationship.to] ?: return@forEach
            val pattern = if (relationship.identifying) StrokePattern.SOLID else StrokePattern.DASHED
            val horizontal = source.x + source.width <= target.x || target.x + target.width <= source.x
            val forward = if (horizontal) source.x < target.x else source.y < target.y
            val from = if (horizontal) ScenePoint(source.x + if (forward) source.width else 0.0, source.y + source.height / 2)
                else ScenePoint(source.x + source.width / 2, source.y + if (forward) source.height else 0.0)
            val to = if (horizontal) ScenePoint(target.x + if (forward) 0.0 else target.width, target.y + target.height / 2)
                else ScenePoint(target.x + target.width / 2, target.y + if (forward) 0.0 else target.height)
            if (relationship.from == relationship.to) {
                val first = ScenePoint(source.x + source.width, source.y + source.height / 3)
                val last = ScenePoint(source.x + source.width, source.y + source.height * 2 / 3)
                val bendX = source.x + source.width + 48.0
                commands += DrawPolyline(listOf(ScenePoint(first.x + 16.0, first.y), ScenePoint(bendX, first.y), ScenePoint(bendX, last.y), ScenePoint(last.x + 16.0, last.y)), pattern = pattern)
                commands += erCardinalityMarks(first, ScenePoint(bendX, first.y), relationship.fromCardinality)
                commands += erCardinalityMarks(last, ScenePoint(bendX, last.y), relationship.toCardinality)
                if (relationship.label.isNotEmpty()) commands += DrawText(relationship.label, ScenePoint(bendX + 8.0, (first.y + last.y) / 2), style = nameStyle)
                width = maxOf(width, bendX + 8.0 + textMeasurer.measure(relationship.label, nameStyle).width + config.padding)
                return@forEach
            }
            val fromDirection = if (horizontal) ScenePoint(from.x + if (forward) 32.0 else -32.0, from.y) else to
            val toDirection = if (horizontal) ScenePoint(to.x + if (forward) -32.0 else 32.0, to.y) else from
            val insetFrom = erInset(from, fromDirection, 16.0)
            val insetTo = erInset(to, toDirection, 16.0)
            if (horizontal && from.y != to.y) {
                val midX = (from.x + to.x) / 2
                commands += DrawPolyline(listOf(insetFrom, ScenePoint(midX, from.y), ScenePoint(midX, to.y), insetTo), pattern = pattern)
            } else commands += DrawLine(insetFrom, insetTo, pattern = pattern)
            commands += erCardinalityMarks(from, fromDirection, relationship.fromCardinality)
            commands += erCardinalityMarks(to, toDirection, relationship.toCardinality)
            if (relationship.label.isNotEmpty()) {
                commands += DrawText(
                    relationship.label,
                    if (horizontal) ScenePoint((from.x + to.x) / 2, minOf(from.y, to.y) - 12.0)
                    else ScenePoint((from.x + to.x) / 2 + 12.0, (from.y + to.y) / 2),
                    anchor = if (horizontal) TextAnchor.MIDDLE else TextAnchor.START,
                    style = nameStyle,
                )
            }
        }
        diagram.entities.forEach { entity ->
            val presentation = presentations.getValue(entity.id)
            val titleStyle = presentation.title
            val typeStyle = presentation.type
            val nameStyle = presentation.name
            val keyStyle = presentation.key
            val rowHeight = presentation.rowHeight
            val headerHeight = presentation.headerHeight
            val rect = rects.getValue(entity.id)
            val typeCol = entity.attributes.maxOfOrNull { textMeasurer.measure(it.type, typeStyle).width } ?: 0.0
            val keyCol = entity.attributes.maxOfOrNull { attribute ->
                if (attribute.key == EntityKey.NONE) 0.0 else textMeasurer.measure((listOf(attribute.key) + attribute.additionalKeys).filter { it != EntityKey.NONE }.joinToString(", ") { it.name }, keyStyle).width
            } ?: 0.0
            commands += DrawRect(rect, cornerRadius = 4.0, fill = presentation.fill, stroke = presentation.stroke, strokeWidth = presentation.strokeWidth)
            commands += DrawText(entity.alias ?: entity.id, ScenePoint(rect.x + pad, rect.y + headerHeight - 10.0), style = titleStyle)
            if (entity.attributes.isNotEmpty()) {
                val ruleY = rect.y + headerHeight
                commands += DrawLine(ScenePoint(rect.x, ruleY), ScenePoint(rect.x + rect.width, ruleY), stroke = presentation.stroke, strokeWidth = presentation.ruleStrokeWidth)
                val typeX = rect.x + pad
                val nameX = typeX + typeCol + gutter
                val keyX = rect.x + rect.width - pad - keyCol
                commands += DrawLine(ScenePoint(nameX - gutter / 2.0, ruleY), ScenePoint(nameX - gutter / 2.0, rect.y + rect.height), stroke = presentation.stroke, strokeWidth = presentation.ruleStrokeWidth)
                if (keyCol > 0.0) {
                    commands += DrawLine(ScenePoint(keyX - gutter / 2.0, ruleY), ScenePoint(keyX - gutter / 2.0, rect.y + rect.height), stroke = presentation.stroke, strokeWidth = presentation.ruleStrokeWidth)
                }
                entity.attributes.forEachIndexed { index, attribute ->
                    val rowY = ruleY + rowHeight - 6.0 + index * rowHeight
                    commands += DrawText(attribute.type, ScenePoint(typeX, rowY), style = typeStyle)
                    commands += DrawText(attribute.name, ScenePoint(nameX, rowY), style = nameStyle)
                    if (attribute.key != EntityKey.NONE) {
                        commands += DrawText((listOf(attribute.key) + attribute.additionalKeys).filter { it != EntityKey.NONE }.joinToString(", ") { it.name }, ScenePoint(keyX, rowY), style = keyStyle)
                    }
                }
            }
        }
        return LayoutScene(width, height, commands, diagram.accessibilityTitle, diagram.accessibilityDescription)
    }

    private fun layoutClass(diagram: ClassDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val style = TextStyle()
        val styles=diagram.classes.associate { it.id to ClassStyle(it,diagram) }
        val sizes = diagram.classes.associate { klass ->
            val lines = classCompartmentLines(klass)
            val css=styles.getValue(klass.id)
            klass.id to SceneSize(max(120.0, lines.maxOf { textMeasurer.measure(it, css.text).width } + 24.0), max(48.0, lines.size * css.lineHeight + 16.0))
        }
        val placement = ClassPlacement(diagram, sizes, config, textMeasurer).place()
        val width = placement.width
        val height = placement.height
        val rects = placement.classes
        val commands = mutableListOf<DrawCommand>()
        diagram.namespaces.forEach { ns ->
            val rect=placement.namespaces.getValue(ns.id)
            commands += DrawRect(rect,cornerRadius=4.0,fill=SceneColor("#f1f5f9"),stroke=SceneColor("#94a3b8"))
            commands += DrawText(ns.label,ScenePoint(rect.x+12,rect.y+19),style=TextStyle(fontWeight=600))
        }
        diagram.relationships.forEach { relation ->
            val source = rects[relation.from] ?: return@forEach
            val target = rects[relation.to] ?: return@forEach
            val horizontal=diagram.direction==FlowDirection.LR || diagram.direction==FlowDirection.RL
            val forward=if(horizontal) target.x>source.x else target.y>source.y
            val from=if(horizontal) ScenePoint(if(forward)source.x+source.width else source.x,source.y+source.height/2)
                else ScenePoint(source.x+source.width/2,if(forward)source.y+source.height else source.y)
            val to=if(horizontal) ScenePoint(if(forward)target.x else target.x+target.width,target.y+target.height/2)
                else ScenePoint(target.x+target.width/2,if(forward)target.y else target.y+target.height)
            val dashed=relation.dashed
            val startMarker=relation.fromMarker
            val endMarker=relation.toMarker
            // Retain the legacy direction of unmarked lines for stable checked-in SVGs.
            if(startMarker==ClassMarker.INHERITANCE && endMarker==ClassMarker.NONE) commands += DrawLine(to,from,pattern=if(dashed)StrokePattern.DASHED else StrokePattern.SOLID)
            else commands += DrawLine(from,to,pattern=if(dashed)StrokePattern.DASHED else StrokePattern.SOLID)
            fun marker(kind:ClassMarker, tip:ScenePoint, other:ScenePoint) {
                when(kind) {
                    ClassMarker.INHERITANCE -> commands += hollowArrowHead(other,tip)
                    ClassMarker.COMPOSITION -> commands += diamondMarker(tip,other,true)
                    ClassMarker.AGGREGATION -> commands += diamondMarker(tip,other,false)
                    ClassMarker.ARROW -> commands += arrowHead(other,tip)
                    ClassMarker.LOLLIPOP -> commands += DrawEllipse(tip,5.0,5.0,fill=SceneColor("#ffffff"),stroke=SceneColor("#475569"))
                    ClassMarker.NONE -> Unit
                }
            }
            marker(startMarker,from,to); marker(endMarker,to,from)
            relation.label?.takeIf { it.isNotEmpty() }?.let { label ->
                val mid = ScenePoint((from.x + to.x) / 2.0, (from.y + to.y) / 2.0 - 6.0)
                commands += DrawText(label, mid, TextAnchor.MIDDLE, style)
            }
            relation.fromCardinality?.takeIf { it.isNotEmpty() }?.let { card ->
                commands += DrawText(card, ScenePoint(from.x - 18.0, from.y - 6.0), TextAnchor.MIDDLE, style)
            }
            relation.toCardinality?.takeIf { it.isNotEmpty() }?.let { card ->
                commands += DrawText(card, ScenePoint(to.x + 18.0, to.y - 6.0), TextAnchor.MIDDLE, style)
            }
        }
        diagram.notes.forEachIndexed { index,note ->
            val rect=placement.notes.getValue(index)
            note.classId?.let { id -> rects[id.substringBefore('~')] }?.let { owner ->
                commands += DrawLine(ScenePoint(owner.x+owner.width/2,owner.y+owner.height),ScenePoint(rect.x+rect.width/2,rect.y),pattern=StrokePattern.DASHED)
            }
            commands += DrawRect(rect,fill=SceneColor("#fff7d6"),stroke=SceneColor("#c5a84c"))
            classNoteLines(note.text).forEachIndexed { lineIndex,line -> commands += DrawText(line,ScenePoint(rect.x+12,rect.y+20+lineIndex*22),style=style) }
        }
        diagram.classes.forEach { klass ->
            val rect = rects.getValue(klass.id)
            val css=styles.getValue(klass.id)
            commands += DrawRect(rect, cornerRadius = 4.0,fill=css.fill,stroke=css.stroke,strokeWidth=css.strokeWidth)
            val attributes = klass.members.filterNot { classMemberIsMethod(it) }
            val methods = klass.members.filter { classMemberIsMethod(it) }
            val lines = classHeaderLines(klass) + attributes.map { classMemberLabel(it) } + methods.map { classMemberLabel(it) }
            lines.forEachIndexed { index, line ->
                commands += DrawText(line, ScenePoint(rect.x + 12.0, rect.y + css.text.fontSize + 4.0 + index * css.lineHeight), style = css.text)
            }
            if (attributes.isNotEmpty() || methods.isNotEmpty()) {
                commands += classCompartmentRule(rect, classHeaderLines(klass).size - 1,css)
            }
            if (attributes.isNotEmpty() && methods.isNotEmpty()) {
                commands += classCompartmentRule(rect, classHeaderLines(klass).size + attributes.size - 1,css)
            }
        }
        return LayoutScene(width, height, commands, diagram.accessibilityTitle, diagram.accessibilityDescription)
    }

    private fun classMemberIsMethod(member: ClassMember): Boolean = member.signature.contains("(")

    private fun classMemberLabel(member: ClassMember): String {
        val prefix = when (member.visibility) {
            ClassVisibility.PUBLIC -> "+"
            ClassVisibility.PRIVATE -> "-"
            ClassVisibility.PROTECTED -> "#"
            ClassVisibility.PACKAGE -> "~"
        }
        return "${if(member.hasVisibility) prefix else ""}${classSignatureLabel(member.signature)}"
    }

    private fun classHeaderLines(klass:ClassDefinition):List<String> = klass.annotations.map { "«$it»" } +
        (klass.label + (klass.genericType?.let { "<$it>" } ?: ""))

    private fun classSignatureLabel(signature:String):String {
        var genericOpen=false
        val generic=signature.map { if(it=='~') {genericOpen=!genericOpen;if(genericOpen)'<' else '>'} else it }.joinToString("")
        val end=generic.lastIndexOf(')')
        if(end<0) return generic
        val suffix=generic.drop(end+1).trim()
        return generic.take(end+1) + if(suffix.isEmpty() || suffix=="$" || suffix=="*") suffix else " : $suffix"
    }

    private fun classCompartmentLines(klass: ClassDefinition): List<String> {
        val attributes = klass.members.filterNot { classMemberIsMethod(it) }
        val methods = klass.members.filter { classMemberIsMethod(it) }
        return classHeaderLines(klass) + attributes.map { classMemberLabel(it) } + methods.map { classMemberLabel(it) }
    }

    private fun classCompartmentRule(rect: SceneRect, afterLineIndex: Int,css:ClassStyle): DrawLine {
        val y = rect.y + css.lineHeight + 7.0 + afterLineIndex * css.lineHeight
        return DrawLine(ScenePoint(rect.x, y), ScenePoint(rect.x + rect.width, y), stroke = css.stroke, strokeWidth = minOf(1.0,css.strokeWidth))
    }

    private fun hollowArrowHead(from: ScenePoint, to: ScenePoint): DrawPolyline {
        val points = arrowHead(from, to).points
        return DrawPolyline(points + points.first(), stroke = SceneColor("#475569"), strokeWidth = 1.5)
    }

    private fun diamondMarker(from: ScenePoint, to: ScenePoint, filled: Boolean): DrawPolygon {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val length = sqrt(dx * dx + dy * dy).takeIf { it > 0.0 } ?: 1.0
        val ux = dx / length
        val uy = dy / length
        val px = -uy
        val py = ux
        val cx = from.x + ux * 5.0
        val cy = from.y + uy * 5.0
        return DrawPolygon(
            listOf(
                ScenePoint(cx + ux * 4.0, cy + uy * 4.0),
                ScenePoint(cx + px * 4.0, cy + py * 4.0),
                ScenePoint(cx - ux * 4.0, cy - uy * 4.0),
                ScenePoint(cx - px * 4.0, cy - py * 4.0),
            ),
            fill = if (filled) SceneColor("#475569") else SceneColor("#ffffff"),
        )
    }

    private fun layoutState(
        diagram: StateDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene {
        val style = TextStyle()
        val nodeIds=diagram.states.map { it.id }.toSet()
        val groupIds=diagram.states.filter { it.childIds.isNotEmpty() }.map { it.id }.toSet()
        val paintDiagram=FlowchartDiagram(diagram.direction,emptyList(),emptyList(),classDefinitions=diagram.classDefinitions)
        val paints=diagram.states.associate { state->state.id to FlowStyle(build.raft.mermaid.core.FlowNode(state.id,state.label,styles=state.styles,classes=state.classes),paintDiagram,
            if(state.id in groupIds)listOf("fill:#f7f7f7","stroke:#999999","stroke-width:1")else if(state.kind==StateNodeKind.NOTE)listOf("fill:#fff5ad","stroke:#aaaa33","stroke-width:1")else listOf("fill:#eeeeee","stroke:#999999","stroke-width:1")) }
        val sizes=diagram.states.associate { state->
            val paint=paints.getValue(state.id)
            val lines=classNoteLines(state.label)+state.description?.let(::classNoteLines).orEmpty()
            state.id to if(state.kind !in listOf(StateNodeKind.STATE, StateNodeKind.NOTE))SceneSize(STATE_TERMINAL_SIZE,STATE_TERMINAL_SIZE) else SceneSize(max(88.0,lines.maxOf { textMeasurer.measure(it,paint.text).width }+32),max(40.0,lines.size*paint.lineHeight+18))
        }
        val groupDefinitions=diagram.classDefinitions.toMutableMap()
        val groupClasses=diagram.states.filter { it.id in groupIds }.associate { state->
            val extra=if(state.styles.isEmpty())emptyList()else {
                var key="__state_inline_${state.id}";while(key in groupDefinitions)key+="_"
                groupDefinitions[key]=state.styles;listOf(key)
            }
            state.id to listOf("default")+state.classes+extra
        }
        val dividerIds = diagram.states.filter { it.kind == StateNodeKind.DIVIDER }.map { it.id }.toSet()
        val regionIds = mutableMapOf<String,List<String>>()
        val regionChildren = linkedMapOf<String,List<String>>()
        val regionParent = mutableMapOf<String,String>()
        val occupied = nodeIds.toMutableSet()
        diagram.states.filter { state -> state.childIds.any { it in dividerIds } }.forEach { state ->
            val regions = mutableListOf(mutableListOf<String>())
            state.childIds.forEach { id -> if(id in dividerIds)regions.add(mutableListOf()) else regions.last().add(id) }
            regionIds[state.id] = regions.mapIndexed { index, children ->
                var id = "__region_${state.id}_$index"
                while (!occupied.add(id)) id += "_"
                regionChildren[id] = children
                regionParent[id] = state.id
                id
            }
        }
        val parentsForPlacement = mutableMapOf<String,String>()
        diagram.states.forEach { state -> state.childIds.forEach { parentsForPlacement[it] = state.id } }
        regionChildren.forEach { (region,children) -> children.forEach { parentsForPlacement[it] = region } }
        val groups = diagram.states.filter { it.id in groupIds }.map { state ->
            val effectiveDirection = state.direction ?: diagram.direction
            val layoutDirection = if(state.id in regionIds) {
                if(effectiveDirection in listOf(FlowDirection.LR,FlowDirection.RL))FlowDirection.TB else FlowDirection.LR
            } else state.direction
            build.raft.mermaid.core.FlowSubgraph(state.id,state.label,regionIds[state.id] ?: state.childIds.filter { it in nodeIds && it !in dividerIds },layoutDirection,
                parentsForPlacement[state.id],classes=groupClasses.getValue(state.id))
        } + regionChildren.map { (id,children) ->
            val parent = regionParent.getValue(id)
            build.raft.mermaid.core.FlowSubgraph(id,"",children,diagram.states.first { it.id == parent }.direction ?: diagram.direction,parent)
        }
        val flow=FlowchartDiagram(diagram.direction,
            diagram.states.filter { it.id !in groupIds && it.id !in dividerIds }.map { build.raft.mermaid.core.FlowNode(it.id,it.label) },
            emptyList(),groups,classDefinitions=groupDefinitions)
        val placement=FlowPlacement(flow,sizes.filterKeys { it !in groupIds && it !in dividerIds },config,textMeasurer,regionChildren.keys).place()

        val initialRects=placement.nodes+placement.groups
        val selfLoops=diagram.transitions.filter { it.from==it.to }.groupBy { it.from }
        val selfLoopIds=selfLoops.keys
        val loopWidths=selfLoops.mapValues { (_,edges)->max(24.0,edges.maxOf { textMeasurer.measure(it.label,style).width }+28) }
        val noteRects=diagram.notes.mapNotNull { note->initialRects[note.targetId]?.let { target->
            val lines=classNoteLines(note.text);val w=lines.maxOf { textMeasurer.measure(it,style).width }+20;val h=lines.size*22.0+12
            note to SceneRect(if(note.position==StateNotePosition.LEFT_OF)target.x-w-10 else target.x+target.width+(loopWidths[note.targetId] ?: 0.0)+10,target.y,w,h)
        }}
        val dx=max(0.0,config.padding-(noteRects.minOfOrNull { it.second.x } ?: config.padding))
        val rects=initialRects.mapValues { (_,r)->r.copy(x=r.x+dx) }
        val selfLoopWidth=selfLoopIds.maxOfOrNull { id -> initialRects[id]?.let { it.x+it.width+loopWidths.getValue(id)+config.padding } ?: 0.0 } ?: 0.0
        val width=max(max(placement.width,selfLoopWidth),noteRects.maxOfOrNull { it.second.x+it.second.width+config.padding } ?: 0.0)+dx
        val height=max(placement.height,noteRects.maxOfOrNull { it.second.y+it.second.height+config.padding } ?: 0.0)
        val statesById = diagram.states.associateBy { it.id }
        val parents = diagram.states.flatMap { state -> state.childIds.map { it to state.id } }.groupBy({ it.first }, { it.second }).mapValues { it.value.first() }
        fun ancestors(id: String): List<String> {
            val result = mutableListOf<String>()
            var parent = parents[id]
            while (parent != null && parent !in result) {
                result += parent
                parent = parents[parent]
            }
            return result
        }

        val commands = mutableListOf<DrawCommand>()
        diagram.states.filter { it.id in groupIds }.sortedBy { rects[it.id]?.y }.forEach { state->
            val rect=rects.getValue(state.id);val paint=paints.getValue(state.id)
            commands+=paint.paint(DrawRect(rect,cornerRadius=5.0,fill=SceneColor("#f7f7f7"),stroke=SceneColor("#999999"),strokeWidth=1.0))
            commands+=DrawText(state.label,ScenePoint(rect.x+rect.width/2,rect.y+6+paint.text.fontSize),TextAnchor.MIDDLE,paint.text)
        }

        regionIds.forEach { (parent,regions) ->
            val outer = rects.getValue(parent)
            val direction = statesById.getValue(parent).direction ?: diagram.direction
            regions.zipWithNext().forEach { (first,second) ->
                val a = rects.getValue(first); val b = rects.getValue(second)
                val horizontalRegions = direction !in listOf(FlowDirection.LR,FlowDirection.RL)
                val from: ScenePoint; val to: ScenePoint
                if (horizontalRegions) {
                    val x = (a.x+a.width+b.x)/2
                    from=ScenePoint(x,outer.y+32);to=ScenePoint(x,outer.y+outer.height-8)
                } else {
                    val y = (a.y+a.height+b.y)/2
                    from=ScenePoint(outer.x+8,y);to=ScenePoint(outer.x+outer.width-8,y)
                }
                commands += DrawLine(from,to,stroke=SceneColor("#999999"),strokeWidth=1.0,pattern=StrokePattern.DASHED)
            }
        }
        diagram.transitions.forEach { transition ->
            val source = rects[transition.from] ?: return@forEach
            val target = rects[transition.to] ?: return@forEach
            if(transition.from==transition.to) {
                val from=ScenePoint(source.x+source.width,source.y+source.height*0.35)
                val to=ScenePoint(source.x+source.width,source.y+source.height*0.75)
                val right=source.x+source.width+24
                val points=listOf(from,ScenePoint(right,from.y),ScenePoint(right,to.y),to)
                commands+=DrawPolyline(points,stroke=SceneColor("#666666"),strokeWidth=1.0)
                commands+=arrowHead(points[points.lastIndex-1],to,fill=SceneColor("#333333"))
                if(transition.label.isNotEmpty())commands+=DrawText(transition.label,ScenePoint(right+4,(from.y+to.y)/2),style=style)
                return@forEach
            }
            val targetAncestors = ancestors(transition.to).toSet()
            val commonParent = ancestors(transition.from).firstOrNull { it in targetAncestors }
            val direction = statesById[commonParent]?.direction ?: diagram.direction
            val anchors = edgeAnchors(source, target, direction in listOf(FlowDirection.LR, FlowDirection.RL))
            commands += DrawLine(anchors.first, anchors.second, stroke = SceneColor("#666666"), strokeWidth = 1.0)
            commands += arrowHead(anchors.first, anchors.second, fill = SceneColor("#333333"))
            if (transition.label.isNotEmpty()) {
                commands += DrawText(
                    transition.label,
                    ScenePoint((anchors.first.x + anchors.second.x) / 2, (anchors.first.y + anchors.second.y) / 2 - 8.0),
                    TextAnchor.MIDDLE,
                    style,
                )
            }
        }
        diagram.states.filter { it.id !in groupIds && it.id !in dividerIds }.forEach { state ->
            val rect = rects.getValue(state.id)
            val paint=paints.getValue(state.id);val begin=commands.size
            when (state.kind) {
                StateNodeKind.STATE, StateNodeKind.NOTE -> {
                    commands += DrawRect(
                        rect = rect,
                        cornerRadius = 5.0,
                        fill = SceneColor("#eeeeee"),
                        stroke = SceneColor("#999999"),
                        strokeWidth = 1.0,
                    )
                    val lines=classNoteLines(state.label)+state.description?.let(::classNoteLines).orEmpty()
                    lines.forEachIndexed { index,line->commands+=DrawText(line,ScenePoint(rect.x+rect.width/2,rect.y+rect.height/2+(index-(lines.size-1)/2.0)*paint.lineHeight+paint.text.fontSize*0.35),TextAnchor.MIDDLE,paint.text) }

                }
                StateNodeKind.DIVIDER -> Unit
                StateNodeKind.START -> {
                    val radius = rect.width / 2.0
                    commands += DrawEllipse(
                        center = ScenePoint(rect.x + radius, rect.y + radius),
                        radiusX = radius,
                        radiusY = radius,
                        fill = SceneColor("#222222"),
                        stroke = SceneColor("#222222"),
                        strokeWidth = 1.0,
                    )
                }
                StateNodeKind.END -> {
                    val radius = rect.width / 2.0
                    val center = ScenePoint(rect.x + radius, rect.y + radius)
                    commands += DrawEllipse(
                        center = center,
                        radiusX = radius,
                        radiusY = radius,
                        fill = SceneColor("#ffffff"),
                        stroke = SceneColor("#222222"),
                        strokeWidth = 1.5,
                    )
                    val inner = radius * 5.0 / 14.0
                    commands += DrawEllipse(
                        center = center,
                        radiusX = inner,
                        radiusY = inner,
                        fill = SceneColor("#222222"),
                        stroke = SceneColor("#222222"),
                        strokeWidth = 1.0,
                    )
                }
                StateNodeKind.CHOICE -> {
                    val cx = rect.x + rect.width / 2.0
                    val cy = rect.y + rect.height / 2.0
                    commands += DrawPolygon(
                        listOf(
                            ScenePoint(cx, rect.y),
                            ScenePoint(rect.x + rect.width, cy),
                            ScenePoint(cx, rect.y + rect.height),
                            ScenePoint(rect.x, cy),
                        ),
                        fill = SceneColor("#ffffff"),
                    )
                }
                StateNodeKind.FORK, StateNodeKind.JOIN -> {
                    commands += DrawRect(
                        rect = SceneRect(rect.x, rect.y + rect.height / 2.0 - 3.0, rect.width, 6.0),
                        fill = SceneColor("#333333"),
                        stroke = SceneColor("#333333"),
                    )
                }
            }
            if(state.kind in listOf(StateNodeKind.STATE,StateNodeKind.NOTE) || state.classes.isNotEmpty() || state.styles.isNotEmpty()) {
                val drawn=commands.subList(begin,commands.size).toList();commands.subList(begin,commands.size).clear();drawn.forEach { commands+=paint.paint(it) }
            }
        }

        noteRects.forEach { (note,initial)->
            val rect=initial.copy(x=initial.x+dx)
            commands+=DrawRect(rect,cornerRadius=3.0,fill=SceneColor("#fff5ad"),stroke=SceneColor("#aaaa33"))
            classNoteLines(note.text).forEachIndexed { index,line->commands+=DrawText(line,ScenePoint(rect.x+rect.width/2,rect.y+20+index*22),TextAnchor.MIDDLE,style) }
        }
        return LayoutScene(width, height, commands)
    }

    private fun layoutFlowchart(
        diagram: FlowchartDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene {
        val style = TextStyle()
        val nodeStyles=diagram.nodes.associate { it.id to FlowStyle(it,diagram) }
        val sizes = diagram.nodes.associate { node ->
            val nodeStyle=nodeStyles.getValue(node.id)
            val lines=classNoteLines(node.label)
            val text=SceneSize(lines.maxOf { textMeasurer.measure(it,nodeStyle.text).width },max(nodeStyle.text.fontSize,lines.size*nodeStyle.lineHeight-8))
            val width=max(80.0,text.width+32.0)
            val height=max(40.0,text.height+20.0)
            node.id to if(node.shape==FlowNodeShape.CIRCLE || node.shape==FlowNodeShape.DOUBLE_CIRCLE)SceneSize(max(width,height),max(width,height))else SceneSize(width,height)
        }
        val horizontal = diagram.direction == FlowDirection.LR || diagram.direction == FlowDirection.RL
        val placement=FlowPlacement(diagram,sizes,config,textMeasurer).place()
        val width=placement.width
        val height=placement.height
        val rects=placement.nodes

        val commands = mutableListOf<DrawCommand>()
        val edgeStroke = SceneColor("#666666")
        val arrowFill = SceneColor("#333333")

        // Subgraph bounding boxes first so nodes/edges sit on top.
        diagram.subgraphs.sortedBy { group ->
            var depth=0;var parent=group.parentId;val seen=mutableSetOf<String>()
            while(parent!=null && seen.add(parent)){depth++;parent=diagram.subgraphs.firstOrNull { it.id==parent }?.parentId};depth
        }.forEach { subgraph ->
            val rect=placement.groups[subgraph.id] ?: return@forEach
            val groupStyle=flowGroupStyle(subgraph,diagram)
            commands += DrawRect(rect,cornerRadius=4.0,fill=groupStyle.fill,stroke=groupStyle.stroke,strokeWidth=groupStyle.strokeWidth)
            commands += DrawText(subgraph.label,ScenePoint(rect.x+rect.width/2,rect.y+6+groupStyle.text.fontSize),TextAnchor.MIDDLE,groupStyle.text)
        }

        diagram.edges.forEach { edge ->
            if (edge.style == FlowEdgeStyle.INVISIBLE) return@forEach
            val source = rects[edge.sourceId] ?: placement.groups[edge.sourceId] ?: return@forEach
            val target = rects[edge.targetId] ?: placement.groups[edge.targetId] ?: return@forEach
            val anchors = edgeAnchors(source, target, horizontal)
            val edgeStyle=flowEdgeStyle(edge,diagram)
            val markerColor=if(flowEdgeStyles(edge,diagram).any { it.substringBefore(':').trim()=="stroke" })edgeStyle.stroke else arrowFill
            commands += DrawLine(
                anchors.first,
                anchors.second,
                stroke = edgeStyle.stroke,
                strokeWidth = edgeStyle.strokeWidth,
                pattern = if (edge.style == FlowEdgeStyle.DOTTED) StrokePattern.DASHED else StrokePattern.SOLID,
            )
            if (edge.toMarker == build.raft.mermaid.core.FlowMarker.POINT) commands += arrowHead(anchors.first, anchors.second, fill = markerColor)
            else commands += flowMarker(edge.toMarker, anchors.first, anchors.second,markerColor,edgeStyle.strokeWidth)
            commands += flowMarker(edge.fromMarker, anchors.second, anchors.first,markerColor,edgeStyle.strokeWidth)
            edge.label?.takeIf { it.isNotEmpty() }?.let { label ->
                val mid = ScenePoint(
                    (anchors.first.x + anchors.second.x) / 2.0,
                    (anchors.first.y + anchors.second.y) / 2.0 - 6.0,
                )
                commands += DrawText(label, mid, TextAnchor.MIDDLE, style)
            }
        }
        diagram.nodes.forEach { node ->
            val rect = rects.getValue(node.id)
            val nodeStyle=nodeStyles.getValue(node.id)
            val glyphStart=commands.size
            when (node.shape) {
                FlowNodeShape.RECTANGLE -> {
                    commands += DrawRect(rect,cornerRadius=if(node.borders==null)5.0 else 0.0,fill=SceneColor("#eeeeee"),stroke=SceneColor(if(node.borders==null)"#999999"else"none"),strokeWidth=1.5)
                    node.borders?.let { borders ->
                        val tl=ScenePoint(rect.x,rect.y);val tr=ScenePoint(rect.x+rect.width,rect.y);val bl=ScenePoint(rect.x,rect.y+rect.height);val br=ScenePoint(rect.x+rect.width,rect.y+rect.height)
                        listOf(Triple('l',tl,bl),Triple('t',tl,tr),Triple('r',tr,br),Triple('b',bl,br)).filter { it.first in borders }.forEach { commands+=DrawLine(it.second,it.third,stroke=SceneColor("#999999"),strokeWidth=1.5) }
                    }
                }
                FlowNodeShape.ROUNDED, FlowNodeShape.STADIUM -> commands += DrawRect(rect, cornerRadius = rect.height / 2.0, fill = SceneColor("#eeeeee"), stroke = SceneColor("#999999"), strokeWidth = 1.5)
                FlowNodeShape.CIRCLE, FlowNodeShape.ELLIPSE -> commands += DrawEllipse(ScenePoint(rect.x + rect.width / 2.0, rect.y + rect.height / 2.0), rect.width / 2.0, rect.height / 2.0, fill = SceneColor("#eeeeee"), stroke = SceneColor("#999999"))
                FlowNodeShape.DOUBLE_CIRCLE -> {
                    val cx = rect.x + rect.width / 2.0
                    val cy = rect.y + rect.height / 2.0
                    commands += DrawEllipse(ScenePoint(cx, cy), rect.width / 2.0, rect.height / 2.0, fill = SceneColor("#eeeeee"), stroke = SceneColor("#999999"))
                    commands += DrawEllipse(ScenePoint(cx, cy), rect.width / 2.0 - 4.0, rect.height / 2.0 - 4.0, fill = SceneColor("#eeeeee"), stroke = SceneColor("#999999"))
                }
                FlowNodeShape.DIAMOND -> commands += DrawPolygon(
                    listOf(
                        ScenePoint(rect.x + rect.width / 2.0, rect.y),
                        ScenePoint(rect.x + rect.width, rect.y + rect.height / 2.0),
                        ScenePoint(rect.x + rect.width / 2.0, rect.y + rect.height),
                        ScenePoint(rect.x, rect.y + rect.height / 2.0),
                    ),
                    fill = SceneColor("#eeeeee"),
                )
                FlowNodeShape.PARALLELOGRAM, FlowNodeShape.PARALLELOGRAM_ALT, FlowNodeShape.TRAPEZOID, FlowNodeShape.TRAPEZOID_ALT, FlowNodeShape.SUBROUTINE, FlowNodeShape.CYLINDER, FlowNodeShape.HEXAGON, FlowNodeShape.ASYMMETRIC -> commands += flowSpecialShape(node.shape, rect)
            }
            val painted=commands.subList(glyphStart,commands.size).toList().flatMap(nodeStyle::paint)
            while(commands.size>glyphStart)commands.removeAt(commands.lastIndex)
            commands+=painted
            val lines=classNoteLines(node.label)
            lines.forEachIndexed { index,line -> commands+=DrawText(line,ScenePoint(rect.x+rect.width/2,rect.y+rect.height/2+(index-(lines.size-1)/2.0)*nodeStyle.lineHeight+nodeStyle.text.fontSize*0.35),TextAnchor.MIDDLE,nodeStyle.text) }

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

    private fun layoutSequence(diagram: SequenceDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene =
        sequenceLayout(diagram, textMeasurer, config, ::sequenceArrowHead)

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
                if (diagram.showData) {
                    val mid = (angle + end) / 2.0
                    val labelRadius = radius * 0.62
                    val percent = round(fraction * 100.0).toInt()
                    commands += DrawText(
                        "$percent%",
                        ScenePoint(
                            (center.x + labelRadius * cos(mid)).pieCoordinate(),
                            (center.y + labelRadius * sin(mid)).pieCoordinate(),
                        ),
                        TextAnchor.MIDDLE,
                        bodyStyle,
                    )
                }
            }
            val legendY = legendStartY + index * 28.0
            commands += DrawRect(SceneRect(legendX, legendY - 11.0, 14.0, 14.0), cornerRadius = 2.0, fill = SceneColor(PIE_COLORS[index % PIE_COLORS.size]))
            val legendLabel = if (diagram.showData) "${section.label} [${pieShowDataValue(section.value)}]" else section.label
            commands += DrawText(legendLabel, ScenePoint(legendX + 22.0, legendY), style = bodyStyle)
            angle = end
        }
        val legendWidth = diagram.sections.maxOfOrNull {
            textMeasurer.measure(if (diagram.showData) "${it.label} [${pieShowDataValue(it.value)}]" else it.label, bodyStyle).width
        } ?: 0.0
        return LayoutScene(
            width = maxOf(config.padding * 2 + 480.0, legendX + 24.0 + legendWidth + config.padding, textMeasurer.measure(diagram.title.orEmpty(), titleStyle).width + config.padding * 2),
            height = maxOf(config.padding * 2 + 2.0 * radius + 30.0, legendStartY + diagram.sections.size * 28.0 + config.padding),
            commands = commands,
            accessibilityTitle = diagram.accessibilityTitle,
            accessibilityDescription = diagram.accessibilityDescription,
        )
    }


    private fun pieShowDataValue(value: Double): String {
        val asInt = value.toInt()
        return if (value == asInt.toDouble()) asInt.toString() else value.toString()
    }
    private fun layoutEventModeling(diagram: EventModelingDiagram, textMeasurer: TextMeasurer, config: LayoutConfig): LayoutScene {
        val style = TextStyle(fontSize = 12.0, fontWeight = 600)
        val lanes = EventModelingEntityKind.entries.filter { kind -> diagram.frames.any { it.kind == kind } }
        val cardWidth = max(132.0, diagram.frames.maxOf { textMeasurer.measure(it.entityId, style).width } + 36.0)
        val cardHeight = 58.0; val laneHeight = 106.0; val laneLabel = 112.0; val gap = 44.0
        val titleOffset = if (diagram.title == null) 0.0 else 44.0
        val width = max(640.0, config.padding * 2 + laneLabel + diagram.frames.size * (cardWidth + gap))
        val height = max(300.0, config.padding * 2 + titleOffset + lanes.size * laneHeight)
        val points = diagram.frames.mapIndexed { i, frame -> frame.id to ScenePoint(config.padding + laneLabel + i * (cardWidth + gap) + cardWidth / 2, config.padding + titleOffset + lanes.indexOf(frame.kind) * laneHeight + laneHeight / 2) }.toMap()
        val commands = mutableListOf<DrawCommand>()
        diagram.title?.let { commands += DrawText(it, ScenePoint(config.padding, config.padding + 22.0), style = TextStyle(fontSize = 20.0, fontWeight = 600)) }
        lanes.forEachIndexed { i, kind -> val y=config.padding+titleOffset+i*laneHeight; commands += DrawRect(SceneRect(config.padding,y,width-config.padding*2,laneHeight),0.0,SceneColor(if(i%2==0) "#f8fafc" else "#ffffff"),SceneColor("#cbd5e1")); commands += DrawText(kind.name,ScenePoint(config.padding+12,y+58),style=style) }
        diagram.relations.forEach { relation -> val from=points.getValue(relation.sourceFrameId); val to=points.getValue(relation.targetFrameId); commands += DrawLine(from,to,stroke=SceneColor("#475569")); commands += arrowHead(from,to) }
        diagram.frames.forEach { frame -> val p=points.getValue(frame.id); commands += DrawRect(SceneRect(p.x-cardWidth/2,p.y-cardHeight/2,cardWidth,cardHeight),7.0,SceneColor("#dbeafe"),SceneColor(if(frame.reset) "#dc2626" else "#334155")); commands += DrawText(frame.entityId,p.copy(y=p.y-2),TextAnchor.MIDDLE,style); commands += DrawText(frame.id,p.copy(y=p.y+18),TextAnchor.MIDDLE,TextStyle(fontSize=10.0)) }
        return LayoutScene(width,height,commands)
    }

    /** Deterministic fishbone layout for the bounded ishikawa slice. */
    private fun layoutIshikawa(
        diagram: IshikawaDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene {
        val style = TextStyle(fontSize = 13.0)
        val headStyle = TextStyle(fontSize = 15.0, fontWeight = 600)
        val causeStyle = TextStyle(fontSize = 13.0, fontWeight = 600)
        val chromeFill = SceneColor("#e2e8f0")
        val spineColor = SceneColor("#334155")
        val boneColor = SceneColor("#475569")
        val subBoneColor = SceneColor("#94a3b8")
        val spineBase = 240.0
        val rowHeight = 26.0
        val boneSlope = 0.42
        val labelBoxHeight = 24.0
        val causes = diagram.effect.children

        fun descendants(node: IshikawaNode): Int = node.children.sumOf { 1 + descendants(it) }

        fun flatten(node: IshikawaNode, depth: Int = 0): List<Pair<IshikawaNode, Int>> =
            listOf(node to depth) + node.children.flatMap { flatten(it, depth + 1) }

        fun subtreeWidth(node: IshikawaNode): Double =
            flatten(node).maxOf { (entry, _) -> textMeasurer.measure(entry.text, style).width }

        val upperCauses = causes.filterIndexed { index, _ -> index % 2 == 0 }
        val lowerCauses = causes.filterIndexed { index, _ -> index % 2 == 1 }
        val upperTotal = upperCauses.sumOf(::descendants)
        val lowerTotal = lowerCauses.sumOf(::descendants)
        val descendantTotal = upperTotal + lowerTotal
        val pool = spineBase * 2.0
        val minSideLen = spineBase * 0.3
        val sideLength: (List<IshikawaNode>, Int, Int) -> Double = { side, total, all ->
            when {
                side.isEmpty() -> spineBase * 0.4
                total == 0 || all == 0 -> spineBase
                else -> max(minSideLen, max(pool * total / all, side.maxOf { flatten(it).size } * rowHeight))
            }
        }
        val upperLen = sideLength(upperCauses, upperTotal, descendantTotal) + labelBoxHeight
        val lowerLen = sideLength(lowerCauses, lowerTotal, descendantTotal) + labelBoxHeight

        val headLines = diagram.effect.text.split(Regex("\\s+")).filter { it.isNotEmpty() }.ifEmpty { listOf(diagram.effect.text) }
        val headLabelWidth = headLines.maxOf { textMeasurer.measure(it, headStyle).width }
        val headWidth = max(headLabelWidth + 56.0, 96.0)
        val headHeight = max(64.0, headLines.size * (headStyle.fontSize + 6.0) + 28.0)
        val slotGap = 44.0
        val slotPadding = 96.0
        val tail = 40.0
        // Cause labels are END-anchored and extend leftward from their sub-bone,
        // so each slot reserves the full measured subtree width before the bone.
        val contentWidths = causes.map { subtreeWidth(it) }
        val slots = contentWidths.map { it + slotPadding + slotGap }
        val width = config.padding * 2.0 + slots.sum() - (if (causes.isEmpty()) slotGap else 0.0) + tail + headWidth
        val spineY = config.padding + labelBoxHeight + upperLen
        val height = config.padding * 2.0 + labelBoxHeight * 2.0 + upperLen + lowerLen
        val headX = width - config.padding - headWidth

        val commands = mutableListOf<DrawCommand>()
        commands += DrawPolyline(
            listOf(ScenePoint(config.padding, spineY), ScenePoint(headX, spineY)),
            stroke = spineColor,
            strokeWidth = 2.0,
        )
        val headTop = ScenePoint(headX, spineY - headHeight / 2.0)
        val headBottom = ScenePoint(headX, spineY + headHeight / 2.0)
        val headControl = ScenePoint(headX + headWidth * 2.0, spineY)
        val headCurve = (0..8).map { step ->
            val t = step / 8.0
            val u = 1.0 - t
            ScenePoint(
                u * u * headBottom.x + 2.0 * u * t * headControl.x + t * t * headTop.x,
                u * u * headBottom.y + 2.0 * u * t * headControl.y + t * t * headTop.y,
            )
        }
        val headPoints = listOf(headTop) + headCurve
        commands += DrawPolygon(headPoints, fill = chromeFill)
        commands += DrawPolyline(headPoints + headTop, stroke = spineColor, strokeWidth = 2.0)
        val headLineGap = headStyle.fontSize + 6.0
        val headTextStart = spineY - (headLines.size - 1) * headLineGap / 2.0 + headStyle.fontSize * 0.35
        headLines.forEachIndexed { index, line ->
            commands += DrawText(
                line,
                ScenePoint(headX + 18.0, headTextStart + index * headLineGap),
                style = headStyle,
            )
        }

        var cursorX = config.padding
        causes.forEachIndexed { index, cause ->
            val slotCenter = cursorX + contentWidths[index] + slotPadding / 2.0
            cursorX += slots[index]
            val upper = index % 2 == 0
            val side = if (upper) -1.0 else 1.0
            val sideLen = if (upper) upperLen else lowerLen
            val anchor = ScenePoint(slotCenter, spineY)
            val boneEnd = ScenePoint(slotCenter - sideLen * boneSlope, spineY + side * sideLen)
            commands += DrawLine(anchor, boneEnd, stroke = boneColor)
            commands += arrowHead(boneEnd, anchor, spineColor)
            val causeWidth = textMeasurer.measure(cause.text, causeStyle).width
            val boxWidth = causeWidth + 16.0
            commands += DrawRect(
                SceneRect(boneEnd.x - boxWidth / 2.0, boneEnd.y - labelBoxHeight / 2.0, boxWidth, labelBoxHeight),
                cornerRadius = 2.0,
                fill = chromeFill,
                stroke = spineColor,
            )
            commands += DrawText(
                cause.text,
                ScenePoint(boneEnd.x, boneEnd.y + causeStyle.fontSize * 0.35),
                TextAnchor.MIDDLE,
                causeStyle,
            )
            val entries = flatten(cause).drop(1)
                .mapIndexed { entryIndex, pair -> Triple(pair.first, pair.second, entryIndex) }
                .sortedWith(compareBy({ it.second }, { it.third }))
            val usable = max(rowHeight, sideLen - labelBoxHeight)
            entries.forEachIndexed { _, (entry, depth, orderIndex) ->
                val y = spineY + side * min((orderIndex + 1) * rowHeight, usable)
                val t = abs(y - spineY) / sideLen
                val boneX = slotCenter - t * sideLen * boneSlope
                val stub = 20.0 + depth * 14.0
                val stubStart = ScenePoint(boneX, y)
                val stubEnd = ScenePoint(boneX - stub, y)
                commands += DrawLine(stubStart, stubEnd, stroke = subBoneColor)
                commands += arrowHead(stubEnd, stubStart, subBoneColor)
                commands += DrawText(
                    entry.text,
                    ScenePoint(boneX - stub - 6.0, y + style.fontSize * 0.4),
                    anchor = TextAnchor.END,
                    style = style,
                )
            }
        }
        return LayoutScene(width, height, commands)
    }

    /** Deterministic 2D map layout for the bounded wardley-beta slice. */
    private fun layoutWardleyMap(
        diagram: WardleyMapDiagram,
        textMeasurer: TextMeasurer,
        config: LayoutConfig,
    ): LayoutScene {
        val labelStyle = TextStyle(fontSize = 13.0)
        val anchorStyle = TextStyle(fontSize = 13.0, fontWeight = 600)
        val noteStyle = TextStyle(fontSize = 12.0, fontWeight = 600)
        val axisStyle = TextStyle(fontSize = 12.0, fontWeight = 600)
        val stageStyle = TextStyle(fontSize = 11.0)
        val evolveColor = SceneColor("#dc2626")
        val axisColor = SceneColor("#334155")
        val stageLineColor = SceneColor("#64748b")
        val commands = mutableListOf<DrawCommand>()
        var cursorY = config.padding
        diagram.title?.let { title ->
            val titleStyle = TextStyle(fontSize = 18.0, fontWeight = 600)
            commands += DrawText(title, ScenePoint(config.padding, cursorY + titleStyle.fontSize), style = titleStyle)
            cursorY += 30.0
        }
        val width = 720.0
        val stageNames = listOf("Genesis", "Custom Built", "Product", "Commodity")
        val axisBand = 44.0
        val height = cursorY + 480.0 + axisBand
        val plotLeft = config.padding + 40.0
        val plotRight = width - config.padding
        val plotTop = cursorY + 20.0
        val plotBottom = height - config.padding - axisBand
        // OWM axes: visibility grows bottom-to-top, evolution grows left-to-right.
        fun x(evolution: Double): Double = plotLeft + evolution * (plotRight - plotLeft)
        fun y(visibility: Double): Double = plotBottom - visibility * (plotBottom - plotTop)
        commands += DrawLine(ScenePoint(plotLeft, plotTop), ScenePoint(plotLeft, plotBottom), stroke = axisColor)
        commands += DrawLine(ScenePoint(plotLeft, plotBottom), ScenePoint(plotRight, plotBottom), stroke = axisColor)
        stageNames.forEachIndexed { index, name ->
            if (index > 0) {
                val dividerX = x(index / stageNames.size.toDouble())
                commands += DrawLine(
                    ScenePoint(dividerX, plotTop),
                    ScenePoint(dividerX, plotBottom),
                    stroke = stageLineColor,
                    strokeWidth = 1.0,
                    pattern = StrokePattern.DASHED,
                )
            }
            commands += DrawText(
                name,
                ScenePoint(x((index + 0.5) / stageNames.size), plotBottom + 16.0),
                TextAnchor.MIDDLE,
                stageStyle,
            )
        }
        commands += DrawText(
            "Evolution",
            ScenePoint((plotLeft + plotRight) / 2.0, plotBottom + 34.0),
            TextAnchor.MIDDLE,
            axisStyle,
        )
        commands += DrawText("Visibility", ScenePoint(plotLeft, plotTop - 8.0), TextAnchor.START, axisStyle)
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
            commands += DrawText(note.text, ScenePoint(x(note.evolution), y(note.visibility)), TextAnchor.START, noteStyle)
        }
        return LayoutScene(width, height, commands)
    }

    private fun isoDayToYmd(day: Int): String {
        var remaining = day
        var year = 0
        fun leap(value: Int) = value % 4 == 0 && (value % 100 != 0 || value % 400 == 0)
        while (true) {
            val yearLength = if (leap(year)) 366 else 365
            if (remaining < yearLength) break
            remaining -= yearLength
            year += 1
        }
        val monthDays = intArrayOf(31, if (leap(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var month = 1
        for (days in monthDays) {
            if (remaining < days) break
            remaining -= days
            month += 1
        }
        val date = remaining + 1
        return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${date.toString().padStart(2, '0')}"
    }

    private fun erInset(from: ScenePoint, to: ScenePoint, distance: Double): ScenePoint {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val length = sqrt(dx * dx + dy * dy).takeIf { it > 0.0 } ?: 1.0
        return ScenePoint(from.x + dx / length * distance, from.y + dy / length * distance)
    }

    private fun erCardinalityMarks(anchor: ScenePoint, toward: ScenePoint, cardinality: EntityCardinality): List<DrawCommand> {
        val dx = toward.x - anchor.x
        val dy = toward.y - anchor.y
        val length = sqrt(dx * dx + dy * dy).takeIf { it > 0.0 } ?: 1.0
        val unitX = dx / length
        val unitY = dy / length
        val perpX = -unitY
        val perpY = unitX
        fun along(distance: Double): ScenePoint = ScenePoint(anchor.x + unitX * distance, anchor.y + unitY * distance)
        fun bar(at: ScenePoint): DrawLine = DrawLine(
            ScenePoint(at.x + perpX * 6.0, at.y + perpY * 6.0),
            ScenePoint(at.x - perpX * 6.0, at.y - perpY * 6.0),
            strokeWidth = 1.5,
        )
        fun crow(): List<DrawLine> {
            val vertex = along(10.0)
            return listOf(
                DrawLine(vertex, anchor),
                DrawLine(vertex, ScenePoint(anchor.x + perpX * 6.0, anchor.y + perpY * 6.0)),
                DrawLine(vertex, ScenePoint(anchor.x - perpX * 6.0, anchor.y - perpY * 6.0)),
            )
        }
        val marks = when (cardinality) {
            EntityCardinality.ONLY_ONE -> listOf(bar(along(4.0)), bar(along(8.0)))
            EntityCardinality.ZERO_OR_ONE -> listOf(
                bar(along(4.0)), DrawEllipse(along(12.0), 4.0, 4.0, fill = SceneColor("#ffffff")),
            )
            EntityCardinality.ONE_OR_MORE -> crow() + bar(along(14.0))
            EntityCardinality.ZERO_OR_MORE -> crow() + DrawEllipse(along(14.0), 4.0, 4.0, fill = SceneColor("#ffffff"))
            EntityCardinality.MD_PARENT -> listOf(DrawPolygon(listOf(
                anchor,
                ScenePoint(along(8.0).x + perpX * 6.0, along(8.0).y + perpY * 6.0),
                along(16.0),
                ScenePoint(along(8.0).x - perpX * 6.0, along(8.0).y - perpY * 6.0),
            )))
        }
        return if (cardinality == EntityCardinality.MD_PARENT) marks
        else listOf(DrawLine(anchor, along(16.0))) + marks
    }

    private fun arrowHead(from: ScenePoint, to: ScenePoint, fill: SceneColor = SceneColor("#475569")): DrawPolygon {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val length = sqrt(dx * dx + dy * dy).takeIf { it > 0.0 } ?: 1.0
        val unitX = dx / length
        val unitY = dy / length
        val baseX = to.x - unitX * 9.0
        val baseY = to.y - unitY * 9.0
        val perpendicularX = -unitY * 4.5
        val perpendicularY = unitX * 4.5
        return DrawPolygon(listOf(to, ScenePoint(baseX + perpendicularX, baseY + perpendicularY), ScenePoint(baseX - perpendicularX, baseY - perpendicularY)), fill = fill)
    }

    /** Arrowhead dispatch for the sequence family: emits nothing for NONE,
     *  a filled triangle for FILLED, an open (hollow) chevron for OPEN, an
     *  X for CROSS and a small open circle for CIRCLE. */
    private fun sequenceArrowHead(from: ScenePoint, to: ScenePoint, head: SequenceArrowHead, fill: SceneColor): List<DrawCommand> {
        if (head == SequenceArrowHead.NONE) return emptyList()
        val dx = to.x - from.x
        val dy = to.y - from.y
        val length = sqrt(dx * dx + dy * dy).takeIf { it > 0.0 } ?: 1.0
        val ux = dx / length
        val uy = dy / length
        val px = -uy
        val py = ux
        return when (head) {
            SequenceArrowHead.HALF_FILLED_TOP, SequenceArrowHead.HALF_FILLED_BOTTOM,
            SequenceArrowHead.HALF_OPEN_TOP, SequenceArrowHead.HALF_OPEN_BOTTOM -> {
                val sign = if (head == SequenceArrowHead.HALF_FILLED_TOP || head == SequenceArrowHead.HALF_OPEN_TOP) -1.0 else 1.0
                val base = ScenePoint(to.x - ux * 9.0, to.y - uy * 9.0)
                val side = if (ux < 0.0) -sign else sign
                val wing = ScenePoint(base.x + px * 4.5 * side, base.y + py * 4.5 * side)
                if (head == SequenceArrowHead.HALF_FILLED_TOP || head == SequenceArrowHead.HALF_FILLED_BOTTOM) listOf(DrawPolygon(listOf(to,base,wing),fill))
                else listOf(DrawPolyline(listOf(wing,to),stroke=fill))
            }
            SequenceArrowHead.NONE -> emptyList()
            SequenceArrowHead.FILLED -> listOf(
                DrawPolygon(
                    listOf(
                        to,
                        ScenePoint(to.x - ux * 9.0 + px * 4.5, to.y - uy * 9.0 + py * 4.5),
                        ScenePoint(to.x - ux * 9.0 - px * 4.5, to.y - uy * 9.0 - py * 4.5),
                    ),
                    fill = fill,
                ),
            )
            SequenceArrowHead.OPEN -> listOf(
                DrawPolyline(
                    listOf(
                        ScenePoint(to.x - ux * 9.0 + px * 4.5, to.y - uy * 9.0 + py * 4.5),
                        to,
                        ScenePoint(to.x - ux * 9.0 - px * 4.5, to.y - uy * 9.0 - py * 4.5),
                    ),
                    stroke = fill,
                ),
            )
            SequenceArrowHead.CROSS -> listOf(
                DrawLine(
                    ScenePoint(to.x - ux * 4.5 + px * 4.5, to.y - uy * 4.5 + py * 4.5),
                    ScenePoint(to.x + ux * 4.5 - px * 4.5, to.y + uy * 4.5 - py * 4.5),
                    stroke = fill,
                ),
                DrawLine(
                    ScenePoint(to.x - ux * 4.5 - px * 4.5, to.y - uy * 4.5 - py * 4.5),
                    ScenePoint(to.x + ux * 4.5 + px * 4.5, to.y + uy * 4.5 + py * 4.5),
                    stroke = fill,
                ),
            )
            SequenceArrowHead.CIRCLE -> listOf(
                DrawEllipse(
                    center = ScenePoint(to.x - ux * 6.0, to.y - uy * 6.0),
                    radiusX = 3.5,
                    radiusY = 3.5,
                    fill = SceneColor("#ffffff"),
                    stroke = fill,
                ),
            )
        }
    }

    private val PIE_COLORS = listOf("#2563eb", "#16a34a", "#f59e0b", "#dc2626", "#9333ea", "#0891b2")
    private val RADAR_CURVE_FILLS = listOf("#dbeafe", "#dcfce7", "#fee2e2", "#fef3c7")
    private val RADAR_CURVE_STROKES = listOf("#2563eb", "#16a34a", "#dc2626", "#d97706")
    private val XY_COLORS = listOf("#2563eb", "#16a34a", "#f59e0b", "#dc2626", "#9333ea", "#0891b2")
    private val JOURNEY_SCORE_COLORS = listOf("#fee2e2", "#fecaca", "#fed7aa", "#fef3c7", "#dcfce7", "#bbf7d0")
    private val JOURNEY_ACTOR_COLORS = listOf("#8FBC8F", "#7CFC00", "#00FFFF", "#FFB6C1", "#DDA0DD")
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

    private fun closedCurveSamples(vertices: List<ScenePoint>, stepsPerSegment: Int = 8): List<ScenePoint> {
        if (vertices.size < 3) return vertices
        val count = vertices.size
        val samples = mutableListOf<ScenePoint>()
        for (index in 0 until count) {
            val p0 = vertices[(index - 1 + count) % count]
            val p1 = vertices[index]
            val p2 = vertices[(index + 1) % count]
            val p3 = vertices[(index + 2) % count]
            val control1 = ScenePoint(p1.x + (p2.x - p0.x) / 6.0, p1.y + (p2.y - p0.y) / 6.0)
            val control2 = ScenePoint(p2.x - (p3.x - p1.x) / 6.0, p2.y - (p3.y - p1.y) / 6.0)
            for (step in 0 until stepsPerSegment) {
                val t = step / stepsPerSegment.toDouble()
                val one = 1.0 - t
                samples += ScenePoint(
                    (one * one * one * p1.x + 3.0 * one * one * t * control1.x + 3.0 * one * t * t * control2.x + t * t * t * p2.x).radarCoordinate(),
                    (one * one * one * p1.y + 3.0 * one * one * t * control1.y + 3.0 * one * t * t * control2.y + t * t * t * p2.y).radarCoordinate(),
                )
            }
        }
        return samples
    }

    private fun Double.radarCoordinate(): Double = round(this * 1_000_000.0) / 1_000_000.0

    private fun Double.radarTickLabel(): String {
        val value = radarCoordinate()
        return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }
}
