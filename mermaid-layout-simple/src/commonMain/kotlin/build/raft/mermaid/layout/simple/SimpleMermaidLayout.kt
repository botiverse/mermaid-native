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
import build.raft.mermaid.core.SequenceDiagram
import build.raft.mermaid.core.PieDiagram
import build.raft.mermaid.core.SequenceLineStyle
import build.raft.mermaid.core.StateDiagram
import build.raft.mermaid.core.StateNodeKind
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

    private fun Double.pieCoordinate(): Double = round(this * 1_000_000.0) / 1_000_000.0
}
