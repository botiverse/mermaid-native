package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.*

/** Non-overlapping boundary bands; plain legacy diagrams retain their existing layout. */
internal fun layoutUsecaseExtended(d: UsecaseDiagram, measurer: TextMeasurer, config: LayoutConfig): LayoutScene {
    val textStyle = TextStyle(fontSize = 13.0, fontWeight = 600)
    fun wrapped(text: String): List<String> = MermaidTextWrapping.wrap(text) {
        measurer.measure(it, textStyle).width <= 240.0
    }
    data class Item(val id: String, val label: String, val actor: Boolean = false, val ellipse: Boolean = false, val body: String? = null)
    val items = d.actors.map { Item(it.id, it.label, actor = true) } + d.useCases.map { Item(it.id, it.label, ellipse = it.shape == UsecaseShape.ELLIPSE) } + d.jsonNodes.map { Item(it.id, it.id, body = if (it.data == null) it.source else null) }
    val lines = items.associate { item -> item.id to buildList {
        d.attributes[item.id]?.stereotype?.let { add("«$it»") }
        if (d.attributes[item.id]?.properties?.get("business") == "true") add("«business»")
        addAll(wrapped(item.label)); item.body?.let { addAll(wrapped(it)) }
    } }
    val tables = d.jsonNodes.mapNotNull { node -> node.data?.let { node.id to UsecaseJsonTableLayout(it, lines.getValue(node.id), measurer) } }.toMap()
    val nodeW = maxOf(180.0, (lines.values.flatten().maxOfOrNull { measurer.measure(it, textStyle).width } ?: 0.0) + 40.0)
    val nodeH = maxOf(76.0, (lines.values.maxOfOrNull { it.size } ?: 1) * 20.0 + 28.0)
    val slotW = maxOf(nodeW, tables.values.maxOfOrNull { it.width } ?: 0.0)
    val slotH = maxOf(nodeH, tables.values.maxOfOrNull { it.height } ?: 0.0)
    fun itemWidth(item: Item): Double = tables[item.id]?.width ?: nodeW
    fun itemHeight(item: Item): Double = tables[item.id]?.height ?: nodeH
    val actorExtent = items.filter { it.actor }.maxOfOrNull { 56.0 + (lines.getValue(it.id).size - 1) * 20.0 + 20.0 } ?: 0.0
    val rowH = maxOf(140.0, slotH + 60.0, actorExtent * 2.0)
    val horizontal = d.direction == FlowDirection.LR || d.direction == FlowDirection.RL
    val reverse = d.direction == FlowDirection.RL || d.direction == FlowDirection.BT
    val groupIds: List<String?> = listOf(null) + d.boundaries.map { it.id }
    val maxColumns = groupIds.maxOf { id -> val group = items.filter { d.attributes[it.id]?.parentId == id }; maxOf(group.count { it.actor }, group.count { !it.actor }, 1) }
    val graphW = if (horizontal) maxOf(720.0, slotW * 2 + 180.0) else maxOf(720.0, maxColumns * (slotW + 40.0) + config.padding * 2)
    val noteW = 290.0
    val width = graphW + if (d.notes.isNotEmpty()) noteW + 60.0 else 0.0
    val points = linkedMapOf<String, ScenePoint>()
    val groupBoxes = mutableListOf<Pair<UsecaseBoundary, SceneRect>>()
    var y = config.padding
    for (id in groupIds) {
        val group = items.filter { d.attributes[it.id]?.parentId == id }
        val boundary = d.boundaries.firstOrNull { it.id == id }
        if (group.isEmpty() && boundary == null) continue
        val a = group.filter { it.actor }; val n = group.filter { !it.actor }
        val headerH = if (boundary != null) wrapped(boundary.label).size * 20.0 + 26.0 else 0.0
        val contentY = y + headerH
        val height = if (horizontal) maxOf(a.size, n.size, 1) * rowH else rowH * 2
        for ((lane, members) in listOf(a, n).withIndex()) for ((index, item) in members.withIndex()) {
            val side = if (reverse) 1 - lane else lane
            points[item.id] = if (horizontal) ScenePoint(if (side == 0) config.padding + slotW / 2 + 24.0 else graphW - config.padding - slotW / 2 - 24.0, contentY + rowH / 2 + index * rowH)
            else ScenePoint(config.padding + slotW / 2 + 20.0 + index * (slotW + 40.0), contentY + rowH / 2 + side * rowH)
        }
        if (boundary != null) groupBoxes += boundary to SceneRect(config.padding, y, graphW - config.padding * 2, height + headerH + 12.0)
        y = contentY + height + 32.0
    }
    val commands = mutableListOf<DrawCommand>()
    fun properties(id: String): Map<String, String> { val attrs = d.attributes[id]; return (listOf("default") + attrs?.classes.orEmpty()).flatMap { d.classDefs[it].orEmpty().entries }.associate { it.key to it.value } + attrs?.styles.orEmpty() }
    fun color(id: String, key: String, default: String): SceneColor { val value = properties(id)[key]?.lowercase(); val hex = value != null && value.startsWith('#') && value.length in listOf(4,5,7,9) && value.drop(1).all { it in "0123456789abcdef" }; return SceneColor(if (hex) value!! else value?.let { NAMED_COLORS[it] } ?: if (value == "transparent") "#00000000" else default) }
    for ((boundary, box) in groupBoxes) {
        commands += DrawRect(box, 8.0, fill = color(boundary.id, "fill", DiagramPalette.SURFACE), stroke = color(boundary.id, "stroke", DiagramPalette.FAINT))
        if (d.attributes[boundary.id]?.properties?.get("type") == "package") commands += DrawRect(SceneRect(box.x + 12.0, box.y - 10.0, minOf(box.width - 24.0, 180.0), 20.0), fill = SceneColor(DiagramPalette.SURFACE), stroke = SceneColor(DiagramPalette.FAINT))
        wrapped(boundary.label).forEachIndexed { i, line -> commands += DrawText(line, ScenePoint(box.x + 16.0, box.y + 25.0 + i * 18.0), style = textStyle) }
    }
    fun actorHalfWidth(id: String) = if (d.actors.first { it.id == id }.type == UsecaseActorType.NORMAL) 20.0 else 28.0
    fun anchor(item: Item, toward: ScenePoint): ScenePoint {
        val c = points.getValue(item.id); val dx = toward.x - c.x; val dy = toward.y - c.y
        val hw = if (item.actor) actorHalfWidth(item.id) else itemWidth(item) / 2; val hh = if (item.actor) 38.0 else itemHeight(item) / 2
        val scale = if (dx == 0.0 && dy == 0.0) 0.0 else if (item.ellipse) 1.0 / sqrt(dx * dx / (hw * hw) + dy * dy / (hh * hh)) else minOf(if (dx == 0.0) Double.POSITIVE_INFINITY else hw / abs(dx), if (dy == 0.0) Double.POSITIVE_INFINITY else hh / abs(dy))
        return ScenePoint(c.x + dx * scale, c.y + dy * scale)
    }
    fun marker(kind: String, from: ScenePoint, to: ScenePoint) {
        if (kind == "generalization") {
            val triangle = flowMarker(FlowMarker.POINT, from, to).filterIsInstance<DrawPolygon>().single()
            commands += triangle.copy(fill = SceneColor(DiagramPalette.CANVAS)); commands += DrawPolyline(triangle.points + triangle.points.first(), stroke = SceneColor(DiagramPalette.SECONDARY))
        } else commands += flowMarker(when(kind) { "arrow" -> FlowMarker.POINT; "circle" -> FlowMarker.CIRCLE; "cross" -> FlowMarker.CROSS; else -> FlowMarker.NONE }, from, to, SceneColor(DiagramPalette.SECONDARY))
    }
    val byId = items.associateBy { it.id }
    for (edge in d.relationships) {
        val a = byId[edge.sourceId] ?: continue; val b = byId[edge.targetId] ?: continue
        if (a.id == b.id) {
            val center = points.getValue(a.id); val hw = if (a.actor) actorHalfWidth(a.id) else itemWidth(a) / 2; val hh = if (a.actor) 38.0 else itemHeight(a) / 2
            val loop = listOf(ScenePoint(center.x + hw, center.y), ScenePoint(center.x + hw + 20, center.y), ScenePoint(center.x + hw + 20, center.y - hh - 24), ScenePoint(center.x, center.y - hh - 24), ScenePoint(center.x, center.y - hh))
            commands += DrawPolyline(loop, pattern = if (edge.dashed) StrokePattern.DASHED else StrokePattern.SOLID)
            marker(edge.startMarker, loop[1], loop.first()); marker(edge.endMarker, loop[loop.lastIndex - 1], loop.last())
            edge.label?.let { commands += DrawText(if (edge.type == UsecaseRelationshipType.INCLUDE || edge.type == UsecaseRelationshipType.EXTEND) "«$it»" else it, ScenePoint(center.x + hw / 2, center.y - hh - 30), anchor = TextAnchor.MIDDLE, style = TextStyle(fontSize = 11.0)) }
            continue
        }
        val from = anchor(a, points.getValue(b.id)); val to = anchor(b, points.getValue(a.id))
        commands += DrawLine(from, to, pattern = if (edge.dashed) StrokePattern.DASHED else StrokePattern.SOLID)
        marker(edge.startMarker, to, from); marker(edge.endMarker, from, to)
        edge.label?.let { commands += DrawText(if (edge.type == UsecaseRelationshipType.INCLUDE || edge.type == UsecaseRelationshipType.EXTEND) "«$it»" else it, ScenePoint((from.x + to.x) / 2, (from.y + to.y) / 2 - 10), anchor = TextAnchor.MIDDLE, style = TextStyle(fontSize = 11.0)) }
    }
    for (item in items) {
        val p = points.getValue(item.id); val fill = color(item.id, "fill", DiagramPalette.BLUE_SURFACE); val stroke = color(item.id, "stroke", DiagramPalette.BLUE)
        val table = tables[item.id]
        if (table != null) {
            commands += table.draw(p, fill, stroke, color(item.id, "color", DiagramPalette.INK))
            continue
        }
        if (item.actor) {
            commands += drawUsecaseActor(d.actors.first { it.id == item.id }, p, fill, stroke)
        } else if (item.ellipse) commands += DrawEllipse(p, nodeW / 2, nodeH / 2, fill = fill, stroke = stroke)
        else commands += DrawRect(SceneRect(p.x - nodeW / 2, p.y - nodeH / 2, nodeW, nodeH), 4.0, fill = fill, stroke = stroke)
        val rows = lines.getValue(item.id)
        rows.forEachIndexed { index, line -> commands += DrawText(line, ScenePoint(p.x, if (item.actor) p.y + 56 + index * 20 else p.y - (rows.size - 1) * 10 + 5 + index * 20), anchor = TextAnchor.MIDDLE, style = textStyle.copy(color = color(item.id, "color", DiagramPalette.INK))) }
    }
    var noteY = config.padding
    for (note in d.notes) {
        val rows = wrapped(note.label); val target = points[note.targetId] ?: continue
        noteY = maxOf(noteY, target.y - 30); val height = maxOf(60.0, rows.size * 20.0 + 24.0); val x = graphW + 24.0
        commands += DrawLine(target, ScenePoint(x, noteY + height / 2), pattern = StrokePattern.DASHED)
        commands += DrawRect(SceneRect(x, noteY, noteW, height), 4.0, fill = SceneColor(DiagramPalette.AMBER_SURFACE), stroke = SceneColor(DiagramPalette.AMBER))
        rows.forEachIndexed { i, line -> commands += DrawText(line, ScenePoint(x + 14, noteY + 24 + i * 20), style = textStyle) }
        noteY += height + 20
    }
    return LayoutScene(width, maxOf(200.0, y + config.padding, noteY + config.padding), commands, d.accTitle, d.accDescription)
}
