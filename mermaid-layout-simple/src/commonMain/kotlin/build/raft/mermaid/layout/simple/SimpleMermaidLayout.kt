package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.FlowDirection
import build.raft.mermaid.core.FlowchartDiagram
import build.raft.mermaid.core.MermaidDiagram
import build.raft.mermaid.core.SequenceDiagram
import build.raft.mermaid.core.SequenceLineStyle
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
import build.raft.mermaid.layout.SceneRect
import build.raft.mermaid.layout.SceneSize
import build.raft.mermaid.layout.StrokePattern
import build.raft.mermaid.layout.TextAnchor
import build.raft.mermaid.layout.TextMeasurer
import build.raft.mermaid.layout.TextStyle
import kotlin.math.max
import kotlin.math.sqrt

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
}
