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
import build.raft.mermaid.layout.DiagramLayout
import build.raft.mermaid.layout.DrawCommand
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
        val contentLines = diagram.requirements.flatMap { requirement ->
            listOf(
                "requirement ${requirement.name}",
                "id: ${requirement.id}",
                "text: ${requirement.text}",
                "risk: ${requirement.risk.name.lowercase()}",
                "verify: ${requirement.verifyMethod.name.lowercase()}",
            )
        } + diagram.elements.flatMap { element ->
            listOf("element ${element.name}", "type: ${element.type}", "docref: ${element.docRef}")
        }
        val cardWidth = max(270.0, contentLines.maxOf { textMeasurer.measure(it, body).width } + 24.0)
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
        diagram.relationships.forEach { relationship ->
            val from = rects.getValue(relationship.from)
            val to = rects.getValue(relationship.to)
            val leftward = from.x > to.x
            val start = ScenePoint(if (leftward) from.x else from.x + from.width, from.y + from.height / 2.0)
            val end = ScenePoint(if (leftward) to.x + to.width else to.x, to.y + to.height / 2.0)
            commands += DrawLine(start, end)
            val direction = if (leftward) -1.0 else 1.0
            commands += DrawPolygon(
                listOf(
                    end,
                    ScenePoint(end.x - direction * 9.0, end.y - 5.0),
                    ScenePoint(end.x - direction * 9.0, end.y + 5.0),
                ),
                fill = SceneColor("#111827"),
            )
            val label = when (relationship.kind) {
                RequirementRelationshipKind.SATISFIES -> "satisfies"
                RequirementRelationshipKind.VERIFIES -> "verifies"
            }
            commands += DrawText(label, ScenePoint((start.x + end.x) / 2.0, (start.y + end.y) / 2.0 - 8.0), TextAnchor.MIDDLE, body)
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
        val width = rects.values.maxOf { it.x + it.width } + config.padding
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

    private fun Double.pieCoordinate(): Double = round(this * 1_000_000.0) / 1_000_000.0
    private fun Double.xyCoordinate(): Double = round(this * 1_000_000.0) / 1_000_000.0
}
