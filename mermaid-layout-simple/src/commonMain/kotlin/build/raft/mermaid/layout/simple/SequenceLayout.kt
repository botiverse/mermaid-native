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
    fun lines(text: String, wrap: Boolean? = null): List<String> = text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .split('\n').flatMap { line ->
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
    val actorWidths = diagram.actors.associate { it.id to max(88.0, textWidth(it.label, it.wrap) + 32.0) }
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
        val required = textWidth(message.label, message.wrap) + 24.0 + if (diagram.events.any { it is SequenceNumbering }) 40.0 else 0.0
        val extra = required - abs(centers.getValue(message.to) - centers.getValue(message.from))
        if (extra > 0) ids.drop(right).forEach { centers[it] = centers.getValue(it) + extra }
    }
    fun noteBounds(note: SequenceNote): Pair<Double, Double> {
        val xs = note.actorIds.map { centers.getValue(it) }
        val center = if (note.position == SequenceNotePosition.OVER) (xs.min() + xs.max()) / 2 else xs[0]
        val width = max(textWidth(note.text, note.wrap) + 24.0, if (xs.size > 1) xs.max() - xs.min() + 24.0 else 0.0)
        return (when (note.position) { SequenceNotePosition.LEFT_OF -> center - width - 12.0; SequenceNotePosition.RIGHT_OF -> center + 12.0; SequenceNotePosition.OVER -> center - width / 2 }) to width
    }
    val minimumX = minOf(config.padding, diagram.notes.minOfOrNull { noteBounds(it).first } ?: config.padding)
    val shift = config.padding - minimumX
    centers.keys.toList().forEach { centers[it] = centers.getValue(it) + shift }
    var width = max(config.padding * 2, diagram.actors.maxOfOrNull { centers.getValue(it.id) + actorWidths.getValue(it.id) / 2 + config.padding } ?: 0.0)
    diagram.notes.forEach { val (x,w) = noteBounds(it); width = max(width, x+w+config.padding) }
    diagram.messages.filter { it.from == it.to }.forEach { width = max(width, centers.getValue(it.from) + max(48.0,textWidth(it.label,it.wrap)+16.0) + config.padding) }
    diagram.events.filterIsInstance<SequenceFragment>().filter { it.boundary != SequenceFragmentBoundary.END }.forEach { width = max(width,textWidth(it.label,it.wrap)+104+config.padding*2) }
    diagram.title?.let { width = max(width, measurer.measure(it, TextStyle(fontSize=18.0,fontWeight=600)).width + config.padding*2) }
    val titleOffset = if (diagram.title.isNullOrEmpty()) 0.0 else 34.0
    val actorTop = config.padding + titleOffset
    val messageTop = actorTop + actorHeight + 40.0
    val positions = mutableListOf<Double>()
    var y = messageTop
    diagram.events.forEach { event ->
        positions += y
        y += when (event) {
            is SequenceMessage -> config.messageGap * (if (event.from == event.to) 2 else 1) + (lines(event.label,event.wrap).size-1)*lineHeight
            is SequenceNote -> max(28.0, lines(event.text,event.wrap).size*lineHeight+10.0)+20.0
            is SequenceFragment -> if (event.boundary == SequenceFragmentBoundary.END) 20.0 else 32.0 + (lines(event.label,event.wrap).size-1)*lineHeight
            else -> 0.0
        }
    }
    val actorBottom = max(messageTop+config.messageGap, y)
    val backgrounds = mutableListOf<DrawCommand>()
    val lifelines = mutableListOf<DrawCommand>()
    val bars = mutableListOf<DrawCommand>()
    val foreground = mutableListOf<DrawCommand>()
    val ink = SceneColor("#333333")
    fun drawLines(target: MutableList<DrawCommand>, text: List<String>, x: Double, baseline: Double, anchor: TextAnchor) {
        text.forEachIndexed { i,line -> target += DrawText(line,ScenePoint(x,baseline+i*lineHeight),anchor,style) }
    }
    diagram.actors.forEach { actor -> lifelines += DrawLine(ScenePoint(centers.getValue(actor.id),actorTop+actorHeight), ScenePoint(centers.getValue(actor.id),actorBottom), SceneColor("#999999"),1.0) }
    val starts = mutableMapOf<String, MutableList<Double>>()
    val frames = mutableListOf<Pair<Int,SequenceFragment>>()
    var sequence = 1.0; var step = 1.0; var numbered = false
    var lastSignalY = messageTop
    fun activationBar(id: String, start: Double, end: Double, depth: Int) {
        bars += DrawRect(SceneRect(centers.getValue(id)-4+depth*4,start,8.0,max(8.0,end-start)),fill=SceneColor("#f4f4f4"),stroke=SceneColor("#666666"),strokeWidth=1.0)
    }
    diagram.events.forEachIndexed { index,event ->
        val eventY = positions[index]
        when(event) {
            is SequenceNumbering -> { numbered=event.visible; event.start?.takeIf { it != 0.0 }?.let { sequence=it }; event.step?.takeIf { it != 0.0 }?.let { step=it } }
            is SequenceActivation -> {
                val stack = starts.getOrPut(event.actorId) { mutableListOf() }
                val at = if (index>0 && diagram.events[index-1] is SequenceMessage) lastSignalY else eventY-20.0
                if(event.activate) stack += at else if(stack.isNotEmpty()) activationBar(event.actorId,stack.removeAt(stack.lastIndex),at,stack.size)
            }
            is SequenceMessage -> {
                val label = (if(numbered) "${(round(sequence*100)/100).toString().removeSuffix(".0")}. " else "") + event.label
                sequence=round((sequence+step)*100)/100
                val text = lines(label,event.wrap)
                val signalY=eventY+(text.size-1)*lineHeight
                lastSignalY=signalY+if(event.from==event.to) 24.0 else 0.0
                val sourceCenter=centers.getValue(event.from); val targetCenter=centers.getValue(event.to)
                val nextActivation=diagram.events.getOrNull(index+1) as? SequenceActivation
                val fromDepth=starts[event.from]?.size ?: 0
                val toDepth=(starts[event.to]?.size ?: 0)+if(nextActivation?.activate==true && nextActivation.actorId==event.to) 1 else 0
                val forward=targetCenter>=sourceCenter
                val fromX=sourceCenter+if(fromDepth>0) (fromDepth-1)*4.0+if(forward) 4.0 else -4.0 else 0.0
                val toX=targetCenter+if(toDepth>0) (toDepth-1)*4.0+if(forward && event.from!=event.to) -4.0 else 4.0 else 0.0
                val pattern=if(event.lineStyle==SequenceLineStyle.DASHED) StrokePattern.DASHED else StrokePattern.SOLID
                val from=ScenePoint(fromX,signalY); val to=ScenePoint(toX,signalY)
                if(event.from==event.to){
                    val right=fromX+max(48.0,textWidth(event.label,event.wrap)+16.0)
                    val end=ScenePoint(toX,signalY+24)
                    foreground += DrawPolyline(listOf(from,ScenePoint(right,signalY),ScenePoint(right,end.y),end),stroke=ink,pattern=pattern)
                    foreground += if(event.headAtSource) arrow(ScenePoint(right,signalY),from,event.arrowHead,ink) else arrow(ScenePoint(right,end.y),end,event.arrowHead,ink)
                    if(event.bidirectional) foreground += arrow(ScenePoint(right,signalY),from,event.arrowHead,ink)
                    drawLines(foreground,text,fromX+8,eventY-8,TextAnchor.START)
                } else {
                    foreground += DrawLine(from,to,stroke=ink,pattern=pattern)
                    foreground += if(event.headAtSource) arrow(to,from,event.arrowHead,ink) else arrow(from,to,event.arrowHead,ink)
                    if(event.bidirectional) foreground += arrow(to,from,event.arrowHead,ink)
                    drawLines(foreground,text,(fromX+toX)/2,eventY-8,TextAnchor.MIDDLE)
                }
                if(event.centralConnection==SequenceCentralConnection.FROM || event.centralConnection==SequenceCentralConnection.BOTH) foreground += DrawEllipse(from,5.0,5.0,stroke=ink)
                if(event.centralConnection==SequenceCentralConnection.TO || event.centralConnection==SequenceCentralConnection.BOTH) foreground += DrawEllipse(to,5.0,5.0,stroke=ink)
            }
            is SequenceNote -> {
                val (x,w)=noteBounds(event); val text=lines(event.text,event.wrap); val h=max(28.0,text.size*lineHeight+10)
                foreground += DrawRect(SceneRect(x,eventY-12,w,h),3.0,SceneColor("#fff5ad"),SceneColor("#aaaa33"))
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
                        backgrounds.add(0,DrawRect(SceneRect(x,top,max(0.0,width-x-config.padding),eventY-top),fill=fill,stroke=SceneColor("#999999"),strokeWidth=1.0))
                        if(start.kind!=SequenceFragmentKind.RECT) {
                            drawLines(foreground,listOf(start.kind.name.lowercase()),x+8,top+16,TextAnchor.START)
                            drawLines(foreground,lines(start.label,start.wrap),x+92,top+16,TextAnchor.START)
                        }
                    }
                }
            }
        }
    }
    starts.forEach { (id,stack) -> stack.forEachIndexed { depth,start -> activationBar(id,start,actorBottom,depth) } }
    listOf(actorTop,actorBottom).forEach { actorY -> diagram.actors.forEach { actor ->
        val center=centers.getValue(actor.id); val actorWidth=actorWidths.getValue(actor.id)
        foreground += sequenceParticipant(actor,center,actorY,actorWidth,actorHeight,actorLines.getValue(actor.id),style,lineHeight)

    } }
    diagram.title?.let { foreground += DrawText(it,ScenePoint(config.padding,config.padding+18),style=TextStyle(fontSize=18.0,fontWeight=600)) }
    return LayoutScene(width,actorBottom+actorHeight+config.padding,backgrounds+lifelines+bars+foreground,diagram.accessibilityTitle,diagram.accessibilityDescription)
}

private fun sequenceRegionColor(text: String): SceneColor {
    val value=text.trim().lowercase()
    val rgb=Regex("rgba?\\(([0-9.]+),\\s*([0-9.]+),\\s*([0-9.]+)(?:,\\s*([0-9.]+))?\\)").matchEntire(value)
    if(rgb!=null){
        val parts=(1..3).map { rgb.groupValues[it].toDoubleOrNull()?.toInt()?.coerceIn(0,255) ?: 0 }
        val alpha=rgb.groupValues[4].toDoubleOrNull()?.coerceIn(0.0,1.0)
        return SceneColor("#"+parts.joinToString(""){it.toString(16).padStart(2,'0')}+(alpha?.let { (it*255).toInt().toString(16).padStart(2,'0') } ?: ""))
    }
    return SceneColor(when(value){"red"->"#ff0000";"green"->"#008000";"blue"->"#0000ff";"yellow"->"#ffff00";"transparent"->"none";else->"#f1f5f9"})
}
