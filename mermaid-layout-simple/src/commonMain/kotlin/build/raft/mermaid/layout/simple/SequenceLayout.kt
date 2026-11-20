package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

/** Measure and draw the same ordered stream, so notes, fragments and activation bars keep time. */
internal fun sequenceLayout(
    diagram: SequenceDiagram, measurer: TextMeasurer, config: LayoutConfig,
    arrow: (ScenePoint, ScenePoint, SequenceArrowHead, SceneColor) -> List<DrawCommand>,
): LayoutScene {
    val style = TextStyle()
    val lineHeight = 18.0
    fun lines(text: String, wrap: Boolean? = null): List<String> = MermaidText.splitBreaks(text).flatMap { it.split('\n') }.flatMap { line ->
            if (wrap != true) listOf(line) else buildList {
                var current = ""
                line.split(Regex("\\s+")).forEach { word ->
                    val candidate = if (current.isEmpty()) word else "$current $word"
                    if (current.isNotEmpty() && measurer.measure(candidate, style).width > 200.0) { add(current); current = word }
                    else current = candidate
                }
                add(current)
            }
        }
    fun textWidth(text: String, wrap: Boolean? = null) = lines(text, wrap).maxOfOrNull { measurer.measure(it, style).width } ?: 0.0
    val actorLines = diagram.actors.associate { it.id to lines(it.label, it.wrap) }
    val actorWidths = diagram.actors.associate { it.id to max(88.0, textWidth(it.label, it.wrap) + 32.0) }.toMutableMap()
    diagram.boxes.filter { it.actorIds.size == 1 }.forEach { box ->
        val id=box.actorIds.single()
        actorWidths[id]=max(actorWidths.getValue(id),textWidth(box.label)+24.0)
    }
    val actorHeight = max(40.0, (actorLines.values.maxOfOrNull { it.size } ?: 1) * lineHeight + 16.0 + if (diagram.actors.any { it.kind != SequenceActorKind.PARTICIPANT }) 44.0 else 0.0)
    val centers = linkedMapOf<String, Double>()
    var cursor = config.padding
    diagram.actors.forEach { actor ->
        centers[actor.id] = cursor + actorWidths.getValue(actor.id) / 2
        cursor += actorWidths.getValue(actor.id) + config.nodeGap
    }
    // Grow only the relevant span when a label would cross participant columns.
    val ids = diagram.actors.map { it.id }
    diagram.messages.filter { it.from != it.to }.forEach { message ->
        val a = ids.indexOf(message.from); val b = ids.indexOf(message.to)
        val right = maxOf(a, b)
        val required = textWidth(message.label, message.wrap) + 24.0
        val extra = required - abs(centers.getValue(message.to) - centers.getValue(message.from))
        if (extra > 0) ids.drop(right).forEach { centers[it] = centers.getValue(it) + extra }
    }
    // Like Mermaid, a self-message label is centred over its lifeline, so both neighbours need room for half of it.
    fun selfReach(message: SequenceMessage) = max(SELF_LOOP_REACH + 12.0, textWidth(message.label, message.wrap) / 2 + 12.0)
    diagram.messages.filter { it.from == it.to }.forEach { message ->
        val index = ids.indexOf(message.from)
        val reach = selfReach(message)
        ids.getOrNull(index - 1)?.let { previous ->
            val extra = textWidth(message.label, message.wrap) / 2 + 12.0 - (centers.getValue(message.from) - centers.getValue(previous))
            if (extra > 0) ids.drop(index).forEach { centers[it] = centers.getValue(it) + extra }
        }
        ids.getOrNull(index + 1)?.let { next ->
            val extra = reach - (centers.getValue(next) - centers.getValue(message.from))
            if (extra > 0) ids.drop(index + 1).forEach { centers[it] = centers.getValue(it) + extra }
        }
    }
    diagram.boxes.filter { it.actorIds.isNotEmpty() }.forEach { box ->
        val left = box.actorIds.minOf { centers.getValue(it)-actorWidths.getValue(it)/2 }
        val rightId = box.actorIds.maxBy { centers.getValue(it) }
        val right = centers.getValue(rightId)+actorWidths.getValue(rightId)/2
        val extra = textWidth(box.label)+24-(right-left)
        if(extra>0) ids.drop(ids.indexOf(rightId)).forEach { centers[it]=centers.getValue(it)+extra }
    }
    fun noteBounds(note: SequenceNote): Pair<Double, Double> {
        val xs = note.actorIds.map { centers.getValue(it) }
        val center = if (note.position == SequenceNotePosition.OVER) (xs.min() + xs.max()) / 2 else xs[0]
        val width = max(textWidth(note.text, note.wrap) + 24.0, if (xs.size > 1) xs.max() - xs.min() + 24.0 else 0.0)
        return (when (note.position) { SequenceNotePosition.LEFT_OF -> center - width - 12.0; SequenceNotePosition.RIGHT_OF -> center + 12.0; SequenceNotePosition.OVER -> center - width / 2 }) to width
    }
    val minimumX = minOf(config.padding, diagram.notes.minOfOrNull { noteBounds(it).first } ?: config.padding,
        diagram.messages.filter { it.from == it.to }.minOfOrNull { centers.getValue(it.from) - textWidth(it.label, it.wrap) / 2 } ?: config.padding)
    val shift = config.padding - minimumX
    centers.keys.toList().forEach { centers[it] = centers.getValue(it) + shift }
    var width = max(config.padding * 2, diagram.actors.maxOfOrNull { centers.getValue(it.id) + actorWidths.getValue(it.id) / 2 + config.padding } ?: 0.0)
    diagram.notes.forEach { val (x,w) = noteBounds(it); width = max(width, x+w+config.padding) }
    diagram.messages.filter { it.from == it.to }.forEach { width = max(width, centers.getValue(it.from) + selfReach(it) + config.padding) }
    diagram.events.filterIsInstance<SequenceFragment>().filter { it.boundary != SequenceFragmentBoundary.END }.forEach { width = max(width,textWidth(it.label,it.wrap)+104+config.padding*2) }
    diagram.title?.let { width = max(width, measurer.measure(it, TextStyle(fontSize=18.0,fontWeight=600)).width + config.padding*2) }
    val titleOffset = if (diagram.title.isNullOrEmpty()) 0.0 else 34.0
    val actorTop = config.padding + titleOffset + if(diagram.boxes.any { it.label.isNotEmpty() }) 28.0 else 0.0
    val messageTop = actorTop + actorHeight + 40.0
    val positions = mutableListOf<Double>()
    var y = messageTop
    diagram.events.forEach { event ->
        positions += y
        y += when (event) {
            is SequenceMessage -> config.messageGap + (if (event.from == event.to) SELF_LOOP_GAP + SELF_LOOP_DROP + 12.0 else 0.0) + (lines(event.label,event.wrap).size-1)*lineHeight
            is SequenceLifecycle -> if(event.create) actorHeight / 2 + 24.0 else 0.0
            is SequenceNote -> max(28.0, lines(event.text,event.wrap).size*lineHeight+10.0)+20.0
            is SequenceFragment -> if (event.boundary == SequenceFragmentBoundary.END) 20.0 else 32.0 + (lines(event.label,event.wrap).size-1)*lineHeight
            else -> 0.0
        }
    }
    val actorBottom = max(messageTop+config.messageGap, y)
    val createdAt = mutableMapOf<String, Double>()
    val creationMessages = mutableSetOf<Int>()
    val destroyedAt = mutableMapOf<String, Double>()
    diagram.events.forEachIndexed { index, event ->
        if(event is SequenceLifecycle) {
            val next = (index + 1 until diagram.events.size).firstOrNull { diagram.events[it] is SequenceMessage }
            if(next != null) {
                val message = diagram.events[next] as SequenceMessage
                val at = positions[next] + (lines(message.label,message.wrap).size-1)*lineHeight
                if(event.create) {
                    createdAt[event.actorId] = at - actorHeight / 2
                    creationMessages += next
                } else destroyedAt[event.actorId] = at
            }
        }
    }
    val boxBackgrounds = mutableListOf<DrawCommand>()
    val backgrounds = mutableListOf<DrawCommand>()
    val lifelines = mutableListOf<DrawCommand>()
    val bars = mutableListOf<DrawCommand>()
    val foreground = mutableListOf<DrawCommand>()
    val ink = SceneColor(DiagramPalette.INK)
    fun drawLines(target: MutableList<DrawCommand>, text: List<String>, x: Double, baseline: Double, anchor: TextAnchor) {
        text.forEachIndexed { i,line -> target += DrawText(line,ScenePoint(x,baseline+i*lineHeight),anchor,style) }
    }
    diagram.boxes.filter { it.actorIds.isNotEmpty() }.forEach { box ->
        val left = box.actorIds.minOf { centers.getValue(it)-actorWidths.getValue(it)/2 } - 12
        val right = box.actorIds.maxOf { centers.getValue(it)+actorWidths.getValue(it)/2 } + 12
        val top = config.padding + titleOffset
        boxBackgrounds += DrawRect(SceneRect(left,top,right-left,actorBottom+actorHeight-top+12),fill=sequenceRegionColor(box.color),stroke=SceneColor("none"))
        drawLines(foreground,lines(box.label),(left+right)/2,top+18,TextAnchor.MIDDLE)
    }
    diagram.actors.forEach { actor -> lifelines += DrawLine(ScenePoint(centers.getValue(actor.id),(createdAt[actor.id] ?: actorTop)+actorHeight), ScenePoint(centers.getValue(actor.id),destroyedAt[actor.id] ?: actorBottom), SceneColor(DiagramPalette.FAINT),1.0) }
    val starts = mutableMapOf<String, MutableList<Double>>()
    val frames = mutableListOf<Pair<Int,SequenceFragment>>()
    var sequence = 1.0; var step = 1.0; var numbered = false
    var lastSignalY = messageTop
    fun activationBar(id: String, start: Double, end: Double, depth: Int) {
        bars += DrawRect(SceneRect(centers.getValue(id)-4+depth*4,start,8.0,max(8.0,end-start)),fill=SceneColor(DiagramPalette.SURFACE),stroke=SceneColor(DiagramPalette.SECONDARY),strokeWidth=1.0)
    }
    diagram.events.forEachIndexed { index,event ->
        val eventY = positions[index]
        when(event) {
            is SequenceLifecycle -> Unit
            is SequenceNumbering -> { numbered=event.visible; event.start?.takeIf { it != 0.0 }?.let { sequence=it }; event.step?.takeIf { it != 0.0 }?.let { step=it } }
            is SequenceActivation -> {
                val stack = starts.getOrPut(event.actorId) { mutableListOf() }
                val at = if (index>0 && diagram.events[index-1] is SequenceMessage) lastSignalY else eventY-20.0
                if(event.activate) stack += at else if(stack.isNotEmpty()) activationBar(event.actorId,stack.removeAt(stack.lastIndex),at,stack.size)
            }
            is SequenceMessage -> {
                val number = if(numbered) (round(sequence*100)/100).toString().removeSuffix(".0") else null
                sequence=round((sequence+step)*100)/100
                val text = lines(event.label,event.wrap)
                // Self-messages keep Mermaid's box margin between the label and the loop.
                val signalY=eventY+(text.size-1)*lineHeight+if(event.from==event.to) SELF_LOOP_GAP else 0.0
                lastSignalY=signalY+if(event.from==event.to) SELF_LOOP_DROP else 0.0
                val sourceCenter=centers.getValue(event.from); val targetCenter=centers.getValue(event.to)
                val nextActivation=diagram.events.getOrNull(index+1) as? SequenceActivation
                val fromDepth=starts[event.from]?.size ?: 0
                val toDepth=(starts[event.to]?.size ?: 0)+if(nextActivation?.activate==true && nextActivation.actorId==event.to) 1 else 0
                val forward=targetCenter>=sourceCenter
                val fromX=sourceCenter+if(fromDepth>0) (fromDepth-1)*4.0+if(forward) 4.0 else -4.0 else 0.0
                val toX=targetCenter+if(index in creationMessages) (if(forward) -1 else 1)*actorWidths.getValue(event.to)/2 else if(toDepth>0) (toDepth-1)*4.0+if(forward && event.from!=event.to) -4.0 else 4.0 else 0.0
                val pattern=if(event.lineStyle==SequenceLineStyle.DASHED) StrokePattern.DASHED else StrokePattern.SOLID
                val from=ScenePoint(fromX,signalY); val to=ScenePoint(toX,signalY)
                if(event.from==event.to){
                    // Mermaid's loop is the cubic (x,y) C (x+60,y-10) (x+60,y+30) (x,y+20); these are its two halves as quadratics.
                    val end=ScenePoint(toX,signalY+SELF_LOOP_DROP)
                    val outgoing=ScenePoint(fromX+SELF_LOOP_REACH,signalY-4.4)
                    val middle=ScenePoint(fromX+SELF_LOOP_REACH,signalY+SELF_LOOP_DROP/2)
                    val returning=ScenePoint(fromX+SELF_LOOP_REACH,signalY+24.4)
                    foreground += DrawPath(listOf(PathMove(from),PathQuadratic(outgoing,middle),PathQuadratic(returning,end)),stroke=ink,pattern=pattern)
                    foreground += if(event.headAtSource) arrow(outgoing,from,event.arrowHead,ink) else arrow(returning,end,event.arrowHead,ink)
                    if(event.bidirectional) foreground += arrow(outgoing,from,event.arrowHead,ink)
                    drawLines(foreground,text,sourceCenter,eventY-8,TextAnchor.MIDDLE)
                } else {
                    foreground += DrawLine(from,to,stroke=ink,pattern=pattern)
                    foreground += if(event.headAtSource) arrow(to,from,event.arrowHead,ink) else arrow(from,to,event.arrowHead,ink)
                    if(event.bidirectional) foreground += arrow(to,from,event.arrowHead,ink)
                    drawLines(foreground,text,(fromX+toX)/2,eventY-8,TextAnchor.MIDDLE)
                }
                if(event.centralConnection==SequenceCentralConnection.FROM || event.centralConnection==SequenceCentralConnection.BOTH) foreground += DrawEllipse(from,5.0,5.0,stroke=ink)
                if(event.centralConnection==SequenceCentralConnection.TO || event.centralConnection==SequenceCentralConnection.BOTH) foreground += DrawEllipse(to,5.0,5.0,stroke=ink)
                // Mermaid marks numbered messages with a filled disc at the line start; wide decimals stretch it into a pill.
                if(number!=null) {
                    val numberWidth=measurer.measure(number,SEQUENCE_NUMBER_STYLE).width
                    foreground += DrawEllipse(from,max(SEQUENCE_NUMBER_RADIUS,numberWidth/2+3.0),SEQUENCE_NUMBER_RADIUS,fill=ink,stroke=ink,strokeWidth=1.0)
                    foreground += DrawText(number,ScenePoint(from.x,from.y+SEQUENCE_NUMBER_STYLE.fontSize*.35),TextAnchor.MIDDLE,SEQUENCE_NUMBER_STYLE)
                }
            }
            is SequenceNote -> {
                val (x,w)=noteBounds(event); val text=lines(event.text,event.wrap); val h=max(28.0,text.size*lineHeight+10)
                foreground += DrawRect(SceneRect(x,eventY-12,w,h),3.0,SceneColor(DiagramPalette.NOTE_SURFACE),SceneColor(DiagramPalette.NOTE_BORDER))
                drawLines(foreground,text,x+w/2,eventY-12+(h-(text.size-1)*lineHeight)/2+style.fontSize*.35,TextAnchor.MIDDLE)
            }
            is SequenceFragment -> when(event.boundary){
                SequenceFragmentBoundary.START -> frames += index to event
                SequenceFragmentBoundary.BRANCH -> {
                    foreground += DrawLine(ScenePoint(config.padding,eventY-16),ScenePoint(width-config.padding,eventY-16),pattern=StrokePattern.DASHED)
                    drawLines(foreground,lines(event.label,event.wrap),config.padding+12,eventY+2,TextAnchor.START)
                }
                SequenceFragmentBoundary.END -> {
                    if(frames.isNotEmpty()) {
                        val (startIndex,start)=frames.removeAt(frames.lastIndex)
                        val x=config.padding+frames.size*4; val top=positions[startIndex]-20
                        val fill=if(start.kind==SequenceFragmentKind.RECT) sequenceRegionColor(start.label) else SceneColor("none")
                        // Insert by start position so outer backgrounds remain behind nested regions.
                        backgrounds.add(0,DrawRect(SceneRect(x,top,max(0.0,width-x-config.padding),eventY-top),fill=fill,stroke=SceneColor(DiagramPalette.FAINT),strokeWidth=1.0))
                        if(start.kind!=SequenceFragmentKind.RECT) {
                            drawLines(foreground,listOf(start.kind.name.lowercase()),x+8,top+16,TextAnchor.START)
                            drawLines(foreground,lines(start.label,start.wrap),x+92,top+16,TextAnchor.START)
                        }
                    }
                }
            }
        }
    }
    starts.forEach { (id,stack) -> stack.forEachIndexed { depth,start -> activationBar(id,start,destroyedAt[id] ?: actorBottom,depth) } }
    diagram.actors.forEach { actor ->
        val center=centers.getValue(actor.id); val actorWidth=actorWidths.getValue(actor.id)
        foreground += sequenceParticipant(actor,center,createdAt[actor.id] ?: actorTop,actorWidth,actorHeight,actorLines.getValue(actor.id),style,lineHeight)
    }
    diagram.actors.forEach { actor ->
        val center=centers.getValue(actor.id); val actorWidth=actorWidths.getValue(actor.id)
        val destroyed = destroyedAt[actor.id]
        if(destroyed == null) foreground += sequenceParticipant(actor,center,actorBottom,actorWidth,actorHeight,actorLines.getValue(actor.id),style,lineHeight)
        else {
            foreground += DrawLine(ScenePoint(center-8,destroyed-8),ScenePoint(center+8,destroyed+8),ink,2.0)
            foreground += DrawLine(ScenePoint(center-8,destroyed+8),ScenePoint(center+8,destroyed-8),ink,2.0)
        }
    }
    diagram.title?.let { foreground += DrawText(it,ScenePoint(config.padding,config.padding+18),style=TextStyle(fontSize=18.0,fontWeight=600)) }
    return LayoutScene(width,actorBottom+actorHeight+config.padding,boxBackgrounds+backgrounds+lifelines+bars+foreground,diagram.accessibilityTitle,diagram.accessibilityDescription)
}

private const val SELF_LOOP_REACH = 45.0
private const val SELF_LOOP_DROP = 20.0
private const val SELF_LOOP_GAP = 10.0
private const val SEQUENCE_NUMBER_RADIUS = 9.0
private val SEQUENCE_NUMBER_STYLE = TextStyle(fontSize = 11.0, color = SceneColor(DiagramPalette.CANVAS))

private fun sequenceRegionColor(text: String): SceneColor {
    val value=text.trim().lowercase()
    val rgb=Regex("rgba?\\(([0-9.]+),\\s*([0-9.]+),\\s*([0-9.]+)(?:,\\s*([0-9.]+))?\\)").matchEntire(value)
    if(rgb!=null){
        val parts=(1..3).map { rgb.groupValues[it].toDoubleOrNull()?.toInt()?.coerceIn(0,255) ?: 0 }
        val alpha=rgb.groupValues[4].toDoubleOrNull()?.coerceIn(0.0,1.0)
        return SceneColor("#"+parts.joinToString(""){it.toString(16).padStart(2,'0')}+(alpha?.let { (it*255).toInt().toString(16).padStart(2,'0') } ?: ""))
    }
    return SceneColor(NAMED_COLORS[value] ?: if(value=="transparent") "none" else if(value.startsWith('#') && value.length in listOf(4,5,7,9) && value.drop(1).all { it in "0123456789abcdef" }) value else DiagramPalette.SURFACE)
}
