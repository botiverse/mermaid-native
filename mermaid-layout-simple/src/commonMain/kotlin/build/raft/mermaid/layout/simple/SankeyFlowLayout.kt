package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.SankeyDiagram
import build.raft.mermaid.layout.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.hypot

// DAG positioning adapted from d3-sankey 0.12.3 (BSD-3-Clause).
// See compatibility/d3-sankey/LICENSE. Cycles retain the existing bounded fallback.
private class FlowNode(val index: Int,val id: String,val label: String) {
    val incoming=mutableListOf<FlowLink>();val outgoing=mutableListOf<FlowLink>()
    var value=0.0;var depth=0;var layer=0;var x=0.0;var y=0.0;var bottom=0.0
}
private class FlowLink(val index: Int,val source: FlowNode,val target: FlowNode,val value: Double) {
    var width=0.0;var sourceY=0.0;var targetY=0.0
}
private val sankeyColors=listOf("#4e79a7","#f28e2c","#e15759","#76b7b2","#59a14f","#edc949","#af7aa1","#ff9da7","#9c755f","#bab0ab")

internal fun sankeyFlowLayout(diagram: SankeyDiagram,measurer: TextMeasurer,config: LayoutConfig): LayoutScene? {
    if(diagram.nodes.isEmpty())return LayoutScene(config.padding*2,config.padding*2,emptyList())
    val nodes=diagram.nodes.mapIndexed { i,n -> FlowNode(i,n.id,n.label) }
    val byId=nodes.associateBy { it.id }
    val links=diagram.links.mapIndexed { i,l ->
        FlowLink(i,byId[l.sourceId] ?: return null,byId[l.targetId] ?: return null,l.value).also {
            it.source.outgoing+=it;it.target.incoming+=it
        }
    }
    if(links.any { !it.value.isFinite() || it.value<0 })return null
    nodes.forEach { it.value=max(it.incoming.sumOf { l->l.value },it.outgoing.sumOf { l->l.value }) }
    var current=nodes.toSet();var depth=0
    while(current.isNotEmpty()) {
        val next=linkedSetOf<FlowNode>()
        current.forEach { n->n.depth=depth;n.outgoing.forEach { next+=it.target } }
        if(++depth>nodes.size)return null
        current=next
    }
    val last=nodes.maxOf { it.depth }
    nodes.forEach { it.layer=if(it.outgoing.isEmpty())last else it.depth }
    val columns=(0..last).map { layer->nodes.filter { it.layer==layer }.toMutableList() }
    val text=TextStyle(fontSize=14.0,fontFamily="trebuchet ms, verdana, arial, sans-serif",color=SceneColor("#000000"))
    fun caption(n: FlowNode): String {
        val value=round(n.value*100)/100
        val label=if(value==value.toLong().toDouble())value.toLong().toString()else value.toString()
        return "${n.label} $label"
    }
    val nodeWidth=10.0
    val maxCaption=nodes.maxOf { measurer.measure(caption(it),text).width }
    val width=max(600.0,(maxCaption+22.0)*max(2,last))
    val height=max(400.0,columns.maxOf { it.size }*42.0)
    val padding=min(27.0,height/max(1,columns.maxOf { it.size }-1))
    nodes.forEach { it.x=if(last==0)0.0 else it.layer*(width-nodeWidth)/last }
    val scale=columns.filter { it.isNotEmpty() && it.sumOf { n->n.value }>0 }.minOfOrNull {
        (height-(it.size-1)*padding)/it.sumOf { n->n.value }
    } ?: 0.0
    val bySource=compareBy<FlowLink> { it.source.y }.thenBy { it.index }
    val byTarget=compareBy<FlowLink> { it.target.y }.thenBy { it.index }
    fun reorder(n: FlowNode) {
        n.incoming.forEach { it.source.outgoing.sortWith(byTarget) }
        n.outgoing.forEach { it.target.incoming.sortWith(bySource) }
    }
    columns.forEach { column ->
        var y=0.0
        column.forEach { n->n.y=y;n.bottom=y+n.value*scale;y=n.bottom+padding;n.outgoing.forEach { it.width=it.value*scale } }
        val extra=(height-y+padding)/(column.size+1)
        column.forEachIndexed { i,n->n.y+=extra*(i+1);n.bottom+=extra*(i+1) }
        column.forEach { n->n.outgoing.sortWith(byTarget);n.incoming.sortWith(bySource) }
    }
    fun targetTop(source: FlowNode,target: FlowNode): Double {
        var y=source.y-(source.outgoing.size-1)*padding/2
        for(l in source.outgoing){if(l.target===target)break;y+=l.width+padding}
        for(l in target.incoming){if(l.source===source)break;y-=l.width}
        return y
    }
    fun sourceTop(source: FlowNode,target: FlowNode): Double {
        var y=target.y-(target.incoming.size-1)*padding/2
        for(l in target.incoming){if(l.source===source)break;y+=l.width+padding}
        for(l in source.outgoing){if(l.target===target)break;y-=l.width}
        return y
    }
    fun collide(column: MutableList<FlowNode>,alpha: Double) {
        if(column.isEmpty())return
        fun down(start: Int,initial: Double){var y=initial;for(i in start until column.size){val n=column[i];val d=(y-n.y)*alpha;if(d>1e-6){n.y+=d;n.bottom+=d};y=n.bottom+padding}}
        fun up(start: Int,initial: Double){var y=initial;for(i in start downTo 0){val n=column[i];val d=(n.bottom-y)*alpha;if(d>1e-6){n.y-=d;n.bottom-=d};y=n.y-padding}}
        val middle=column.size/2;val subject=column[middle]
        up(middle-1,subject.y-padding);down(middle+1,subject.bottom+padding)
        up(column.lastIndex,height);down(0,0.0)
    }
    repeat(6) { iteration ->
        val alpha=.99.pow(iteration);val beta=max(1-alpha,(iteration+1)/6.0)
        for(i in columns.lastIndex-1 downTo 0) {
            val column=columns[i]
            for(n in column){var y=0.0;var weight=0.0
                n.outgoing.forEach { l->val w=l.value*(l.target.layer-n.layer);y+=sourceTop(n,l.target)*w;weight+=w }
                if(weight>0){val dy=(y/weight-n.y)*alpha;n.y+=dy;n.bottom+=dy;reorder(n)}
            }
            column.sortBy { it.y };collide(column,beta)
        }
        for(i in 1 until columns.size) {
            val column=columns[i]
            for(n in column){var y=0.0;var weight=0.0
                n.incoming.forEach { l->val w=l.value*(n.layer-l.source.layer);y+=targetTop(l.source,n)*w;weight+=w }
                if(weight>0){val dy=(y/weight-n.y)*alpha;n.y+=dy;n.bottom+=dy;reorder(n)}
            }
            column.sortBy { it.y };collide(column,beta)
        }
    }
    nodes.forEach { n->var sy=n.y;var ty=n.y
        n.outgoing.forEach { it.sourceY=sy+it.width/2;sy+=it.width }
        n.incoming.forEach { it.targetY=ty+it.width/2;ty+=it.width }
    }
    fun color(n: FlowNode)=SceneColor(sankeyColors[n.index%sankeyColors.size])
    val commands=mutableListOf<DrawCommand>()
    // Sample cubic ribbon boundaries, retaining the same geometry for SVG and native Canvas.
    for(l in links) {
        if(l.width<=0)continue
        val x0=l.source.x+nodeWidth;val x1=l.target.x;val mid=(x0+x1)/2
        fun boundary(t: Double,offset: Double): ScenePoint {
            val u=1-t
            val dx=3*(u*u*(mid-x0)+t*t*(x1-mid))
            val dy=6*u*t*(l.targetY-l.sourceY)
            val length=hypot(dx,dy)
            // Offset perpendicular to the center curve, matching d3's constant-width stroke.
            return ScenePoint(u*u*u*x0+3*u*u*t*mid+3*u*t*t*mid+t*t*t*x1-dy/length*offset,
                u*u*u*l.sourceY+3*u*u*t*l.sourceY+3*u*t*t*l.targetY+t*t*t*l.targetY+dx/length*offset)
        }
        // Remove cancellation noise at zero before SVG serialization (no exponent notation).
        fun clean(p: ScenePoint)=ScenePoint(round(p.x*1e8)/1e8,round(p.y*1e8)/1e8)
        val points=(0..48).map { clean(boundary(it/48.0,-l.width/2)) }+(48 downTo 0).map { clean(boundary(it/48.0,l.width/2)) }
        commands+=DrawPolygon(points,color(l.source),SceneLinearGradient(ScenePoint(x0,0.0),ScenePoint(x1,0.0),color(l.source),color(l.target),.5))
    }
    nodes.forEach { n->commands+=DrawRect(SceneRect(n.x,round(n.y*1e8)/1e8,nodeWidth,round((n.bottom-n.y)*1e8)/1e8),fill=color(n),stroke=SceneColor("none"),strokeWidth=0.0) }
    nodes.forEach { n->val left=n.x<width/2
        commands+=DrawText(caption(n),ScenePoint(if(left)n.x+nodeWidth+6 else n.x-6,(n.y+n.bottom)/2),if(left)TextAnchor.START else TextAnchor.END,text)
    }
    return LayoutScene(width,height,commands)
}
