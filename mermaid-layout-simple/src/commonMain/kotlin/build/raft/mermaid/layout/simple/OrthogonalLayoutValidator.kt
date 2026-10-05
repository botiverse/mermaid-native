package build.raft.mermaid.layout.simple

import build.raft.mermaid.layout.*
import kotlin.math.*

/**
 * Native port of Mermaid validateLayout/geometry (04ee336), retaining its fixed
 * tolerances and zero-on-invalid score. Uses box bounds even for non-box nodes.
 * This diagnoses measured routes; it does not modify or repair a scene.
 */
public object OrthogonalLayoutValidator {
    public fun validate(input: LayoutValidationInput): LayoutValidationResult = Validation(input).run()
}

internal data class OrthogonalSegment(val a: ScenePoint, val b: ScenePoint, val orientation: Char = when {
        abs(b.x-a.x)<=1 && abs(b.y-a.y)<=1 -> 'Z'
        abs(b.y-a.y)<=1 -> 'H'
        else -> 'V'
    })
private data class EdgeGeometry(val edge: LayoutValidationEdge) {
    val raw = edge.points.zipWithNext(::OrthogonalSegment)
    val segments = normalizeOrthogonalPolyline(raw)
    val points = if(segments.isEmpty()) edge.points else listOf(segments.first().a)+segments.map { it.b }
}
internal fun normalizeOrthogonalPolyline(raw: List<OrthogonalSegment>): List<OrthogonalSegment> {
    val result = mutableListOf<OrthogonalSegment>()
    for (next in raw.filter { it.orientation!='Z' }) {
        val current=result.lastOrNull()
        val merge=current!=null && current.orientation==next.orientation &&
            if(next.orientation=='H') abs(current.b.y-next.a.y)<=1 && abs(current.a.y-next.a.y)<=1
            else abs(current.b.x-next.a.x)<=1 && abs(current.a.x-next.a.x)<=1
        if(merge)result[result.lastIndex]=current!!.copy(b=next.b) else result+=next
    }
    return result
}
private val SceneRect.right get()=x+width
private val SceneRect.bottom get()=y+height
private fun SceneRect.corners()=listOf(ScenePoint(x,y),ScenePoint(right,y),ScenePoint(right,bottom),ScenePoint(x,bottom))
private fun SceneRect.sides(): List<OrthogonalSegment> { val p=corners();return p.indices.map { OrthogonalSegment(p[it],p[(it+1)%4]) } }
private fun SceneRect.valid()=listOf(x,y,right,bottom).all { it.isFinite() } && width>0 && height>0
private fun close(a:Double,b:Double)=abs(a-b)<=1e-6
internal fun geometryDistance(a:ScenePoint,b:ScenePoint)=sqrt((a.x-b.x).pow(2)+(a.y-b.y).pow(2))
private fun overlap(a:Double,b:Double,c:Double,d:Double)=max(0.0,min(max(a,b),max(c,d))-max(min(a,b),min(c,d)))
private fun overlaps(a:SceneRect,b:SceneRect): Map<String,Any?>? {
    val x=overlap(a.x,a.right,b.x,b.right);val y=overlap(a.y,a.bottom,b.y,b.bottom)
    return if(x>0 && y>0) mapOf("overlapX" to x,"overlapY" to y) else null
}
private fun intersects(s:OrthogonalSegment,r:SceneRect):Boolean {
    // The reference helper includes a line lying on a boundary, but excludes
    // zero-length, diagonal and mere endpoint touches along the segment axis.
    if(s.a.x==s.b.x && s.a.y==s.b.y)return false
    if(s.a.y==s.b.y)return s.a.y>=r.y && s.a.y<=r.bottom && max(s.a.x,s.b.x)>r.x && min(s.a.x,s.b.x)<r.right
    if(s.a.x==s.b.x)return s.a.x>=r.x && s.a.x<=r.right && max(s.a.y,s.b.y)>r.y && min(s.a.y,s.b.y)<r.bottom
    return false
}
private fun direction(a:ScenePoint,b:ScenePoint,epsilon:Double=1.0):String? {
    val dx=b.x-a.x;val dy=b.y-a.y
    if(abs(dx)<=epsilon && abs(dy)<=epsilon)return null
    if(abs(dy)<=epsilon)return if(dx>0)"E" else "W"
    if(abs(dx)<=epsilon)return if(dy>0)"S" else "N"
    return null
}
private fun side(p:ScenePoint,r:SceneRect):String?=when {
    close(p.x,r.x)->"W";close(p.x,r.right)->"E";close(p.y,r.y)->"N";close(p.y,r.bottom)->"S";else->null
}
private fun sameCorridor(s:OrthogonalSegment,e:LayoutValidationEdge)=listOf(e.points.first(),e.points.last()).any { geometryDistance(s.a,it)<=8 && geometryDistance(s.b,it)<=8 }
private fun eitherCorridor(s:OrthogonalSegment,e:LayoutValidationEdge)=listOf(s.a,s.b).all { p->geometryDistance(p,e.points.first())<=8 || geometryDistance(p,e.points.last())<=8 }
private fun hug(s:OrthogonalSegment,r:SceneRect):Double=when {
    s.orientation=='H' && (abs(s.a.y-r.y)<=2 || abs(s.a.y-r.bottom)<=2)->overlap(s.a.x,s.b.x,r.x,r.right)
    s.orientation=='V' && (abs(s.a.x-r.x)<=2 || abs(s.a.x-r.right)<=2)->overlap(s.a.y,s.b.y,r.y,r.bottom)
    else->0.0
}
private fun parallelGap(a:OrthogonalSegment,b:OrthogonalSegment):Double?=when {
    a.orientation!=b.orientation || a.orientation=='Z'->null
    a.orientation=='H'->abs(a.a.y-b.a.y)
    else->abs(a.a.x-b.a.x)
}
private fun projectedOverlap(a:OrthogonalSegment,b:OrthogonalSegment):Double=when {
    a.orientation!=b.orientation || a.orientation=='Z'->0.0
    a.orientation=='H'->overlap(a.a.x,a.b.x,b.a.x,b.b.x)
    else->overlap(a.a.y,a.b.y,b.a.y,b.b.y)
}
internal fun orthogonalSegmentsCross(a:OrthogonalSegment,b:OrthogonalSegment):Boolean {
    if(a.orientation=='Z' || b.orientation=='Z' || a.orientation==b.orientation)return false
    val h=if(a.orientation=='H')a else b;val v=if(a.orientation=='V')a else b
    val p=ScenePoint(v.a.x,h.a.y)
    if(p.x !in min(h.a.x,h.b.x)..max(h.a.x,h.b.x) || p.y !in min(v.a.y,v.b.y)..max(v.a.y,v.b.y))return false
    fun endpoint(s:OrthogonalSegment)=listOf(s.a,s.b).any { abs(it.x-p.x)<1e-6 && abs(it.y-p.y)<1e-6 }
    return !(endpoint(h) && endpoint(v))
}
private fun labelDummy(n:LayoutValidationNode)=n.isEdgeLabel || n.isDummy || n.id.startsWith("edge-label-")
private fun band(s:OrthogonalSegment,side:String,r:SceneRect):Double? {
    val vertical=side=="W" || side=="E"
    if(s.orientation!=if(vertical)'V' else 'H')return null
    val d=when(side){"W"->r.x-s.a.x;"E"->s.a.x-r.right;"N"->r.y-s.a.y;else->s.a.y-r.bottom}
    val o=if(vertical)overlap(s.a.y,s.b.y,r.y,r.bottom)else overlap(s.a.x,s.b.x,r.x,r.right)
    return if(d>=-1 && d<=19 && o>1)max(0.0,d)else null
}
private fun marker(e:LayoutValidationEdge,start:Boolean):Boolean {
    val explicit=(if(start)e.arrowTypeStart else e.arrowTypeEnd)?.trim()
    if(!explicit.isNullOrEmpty() && explicit!="none" && explicit!="arrow_open")return true
    return e.type?.let { if(start)it.startsWith("double_") else Regex("arrow_(point|cross|circle|barb)|double_arrow").containsMatchIn(it) } ?: false
}
private fun markerRect(points:List<ScenePoint>,start:Boolean):SceneRect? {
    if(points.size<2)return null
    val tip=if(start)points.first()else points.last();val inner=if(start)points[1]else points[points.lastIndex-1]
    val dx=inner.x-tip.x;val dy=inner.y-tip.y
    if(abs(dx)<=1 && abs(dy)<=1)return null
    if(abs(dy)<=1)return SceneRect(min(tip.x,tip.x+sign(dx)*10),tip.y-7,10.0,14.0)
    if(abs(dx)<=1)return SceneRect(tip.x-7,min(tip.y,tip.y+sign(dy)*10),14.0,10.0)
    return null
}
private fun penalty(n:Int):Double=when { n<=3->0.0;n==4->5.0;n==5->12.0;n==6->30.0;else->30.0*2.0.pow(n-6) }

private class Validation(val input:LayoutValidationInput) {
    val issues=mutableListOf<LayoutIssue>()
    val byId=input.nodes.associateBy { it.id }
    val obstacles=byId.filterValues { !it.isGroup || labelDummy(it) }
    val groups=byId.filterValues { it.isGroup && !labelDummy(it) }
    val edges=mutableListOf<EdgeGeometry>()
    fun issue(type:String,edge:String?=null,nodes:List<String> = emptyList(),details:Map<String,Any?> = emptyMap()) {
        issues+=LayoutIssue(type, type.replace('-',' '),nodes,edge,details)
    }
    fun ancestor(id:String,node:LayoutValidationNode?):Boolean {
        var parent=node?.parentId;val seen=mutableSetOf<String>()
        while(parent!=null && seen.add(parent)){if(parent==id)return true;parent=byId[parent]?.parentId}
        return false
    }
    fun hit(g:EdgeGeometry,r:SceneRect,skip:(OrthogonalSegment)->Boolean={false}):Map<String,Any?>? {
        val i=g.raw.indexOfFirst { !sameCorridor(it,g.edge) && !skip(it) && intersects(it,r) }
        return if(i<0)null else mapOf("segmentIndex" to i,"a" to g.raw[i].a,"b" to g.raw[i].b)
    }
    fun run():LayoutValidationResult {
        val nodes=byId.values.sortedBy { it.id }
        for(i in nodes.indices)for(j in i+1 until nodes.size){
            val a=nodes[i];val b=nodes[j]
            if((a.isGroup && ancestor(a.id,b)) || (b.isGroup && ancestor(b.id,a)))continue
            overlaps(a.bounds,b.bounds)?.let { issue("node-overlap",nodes=listOf(a.id,b.id),details=it) }
        }
        for(n in input.nodes.filter { !it.isGroup && !labelDummy(it) })for(g in groups.values){
            if(ancestor(g.id,n) && g.shape!="swimlane")continue
            val length=n.bounds.sides().maxOf { hug(it,g.bounds) }
            if(length>=12){issue("node-border-hugging",nodes=listOf(n.id,g.id),details=mapOf("hugLength" to length));break}
        }
        for(e in input.edges) {
            if(e.points.size<2){issue("edge-missing-points",e.id);continue}
            val g=EdgeGeometry(e);edges+=g
            perEdge(g)
        }
        labels()
        ports()
        var crossings=0
        val sorted=edges.sortedBy { it.edge.id }
        for(i in sorted.indices)for(j in i+1 until sorted.size){
            val a=sorted[i];val b=sorted[j]
            for(s in a.segments)for(t in b.segments){
                if(orthogonalSegmentsCross(s,t))crossings++
                val gap=parallelGap(s,t) ?: continue
                val overlap=projectedOverlap(s,t)
                val corridor=eitherCorridor(s,a.edge) && eitherCorridor(t,b.edge)
                if(gap<=1 && overlap>=8 && !corridor)issue("edge-shared-subpath",details=mapOf("edgeIds" to listOf(a.edge.id,b.edge.id),"overlapLength" to overlap))
                if(gap>1 && gap<7 && overlap>=8 && !corridor && !sharedTerminal(a,s,b,t))issue("edge-parallel-segment-too-close",details=mapOf("edgeIds" to listOf(a.edge.id,b.edge.id),"gap" to gap,"threshold" to 7,"overlapLength" to overlap,"minOverlap" to 8))
            }
        }
        val penalties=edges.map { LayoutEdgePenalty(it.edge.id,it.points.size,penalty(it.points.size)) }.sortedByDescending { it.bendPenalty }
        val bends=penalties.sumOf { it.bendPenalty };val crossingPenalty=crossings*3.0
        val histogram=linkedMapOf("2" to 0,"3" to 0,"4" to 0,"5" to 0,"6" to 0,"7+" to 0)
        for(e in edges){val key=if(e.points.size>=7)"7+" else max(2,e.points.size).toString();histogram[key]=histogram.getValue(key)+1}
        return LayoutValidationResult(issues.isEmpty(),issues.toList(),if(issues.isEmpty())(1000-bends-crossingPenalty).coerceIn(0.0,1000.0)else 0.0,
            LayoutValidationBreakdown(input.nodes.count { !it.isGroup },edges.size,crossings,penalties.sumOf { it.points },bends,crossingPenalty,penalties,histogram))
    }
    fun perEdge(g:EdgeGeometry) {
        val e=g.edge;val p=e.points;val start=p.first();val end=p.last();val sNode=byId[e.start];val tNode=byId[e.end]
        if(g.segments.size>=2){
            val first=geometryDistance(g.segments.first().a,g.segments.first().b);val last=geometryDistance(g.segments.last().a,g.segments.last().b)
            for((which,len)in listOf("start" to first,"end" to last))if(len<10)issue("edge-bend-near-endpoint",e.id,details=mapOf("which" to which,"length" to len,"threshold" to 10))
            if(tNode!=null && last>=10)side(end,tNode.bounds)?.let { side->band(g.segments[g.segments.lastIndex-1],side,tNode.bounds) }?.let { d->issue("edge-bend-near-endpoint",e.id,listOf(tNode.id),mapOf("which" to "end-band","distance" to d,"threshold" to 18)) }
        }
        val nonOrtho=g.raw.indexOfFirst { !close(it.a.x,it.b.x) && !close(it.a.y,it.b.y) }
        if(nonOrtho>=0)issue("edge-non-orthogonal",e.id,details=mapOf("segmentIndex" to nonOrtho,"a" to g.raw[nonOrtho].a,"b" to g.raw[nonOrtho].b))
        for(n in obstacles.values)if(n.id!=e.labelNodeId)hit(g,n.bounds)?.let { issue("edge-intersects-obstacle",e.id,listOf(n.id),it) }
        for(n in groups.values){
            val title=n.groupTitleBounds?.takeIf { it.valid() } ?: continue
            val hit=hit(g,title) { s->(ancestor(n.id,sNode) && (geometryDistance(s.a,start)<=1 || geometryDistance(s.b,start)<=1)) || (ancestor(n.id,tNode) && (geometryDistance(s.a,end)<=1 || geometryDistance(s.b,end)<=1)) }
            if(hit!=null){issue("edge-intersects-group-title",e.id,listOf(n.id),hit+mapOf("titleRect" to title));break}
        }
        for((node,point)in listOf(sNode to start,tNode to end))if(node!=null && node.bounds.corners().minOf { geometryDistance(point,it) }<=3)issue("edge-corner-connection",e.id,listOf(node.id),mapOf("point" to point))
        if(sNode!=null && tNode!=null){
            for((node,pair)in listOf(sNode to (start to p[1]),tNode to (end to p[p.lastIndex-1]))){
                val side=side(pair.first,node.bounds);val dir=direction(pair.first,pair.second,1e-6)
                if(side!=null && dir!=null && side!=dir)issue("edge-port-direction-mismatch",e.id,listOf(node.id),mapOf("side" to side,"direction" to dir))
            }
        }
        e.labelNodeId?.let { byId[it] }?.let { if(g.raw.none { s->intersects(s,it.bounds) })issue("edge-label-off-edge",e.id,listOf(it.id),mapOf("labelRect" to it.bounds)) }
        for((which,point)in listOf("start" to start,"end" to end))for(n in byId.values){
            if(n.isGroup || n.id==e.labelNodeId)continue
            val r=n.bounds
            if(point.x>r.x+0.5 && point.x<r.right-0.5 && point.y>r.y+0.5 && point.y<r.bottom-0.5){issue("edge-endpoint-inside-node",e.id,listOf(n.id),mapOf("which" to which,"point" to point,"rect" to r));break}
        }
        for(n in (obstacles+groups).values){
            if(n.id==e.labelNodeId)continue
            for(s in g.segments){
                if(sameCorridor(s,e))continue
                val length=hug(s,n.bounds)
                if(length>=12){issue("edge-border-hugging",e.id,listOf(n.id),mapOf("hugLength" to length));break}
            }
        }
    }
    fun labels() {
        data class Label(val rect:SceneRect,val owner:String,val node:String?)
        val labels=input.edges.mapNotNull { e->e.labelBounds?.takeIf { !e.label.isNullOrEmpty() && it.valid() }?.let { Label(it,e.id,null) } }.toMutableList()
        val owners=input.edges.mapNotNull { e->e.labelNodeId?.let { it to e.id } }.toMap()
        labels+=input.nodes.filter(::labelDummy).map { Label(it.bounds,owners[it.id] ?: "",it.id) }
        val byEdge=input.edges.associateBy { it.id }
        for(l in labels){
            val ids=listOfNotNull(l.node);val owner=byEdge[l.owner];val geometry=edges.firstOrNull { it.edge.id==l.owner }
            if(owner!=null && geometry!=null)for(start in listOf(true,false)){
                if(!marker(owner,start))continue
                val rect=markerRect(geometry.points,start) ?: continue
                val overlap=overlaps(l.rect,rect) ?: continue
                issue("edge-label-overlaps-own-arrowhead",l.owner,ids,overlap+mapOf("terminal" to if(start)"start" else "end","labelRect" to l.rect,"markerRect" to rect,"markerClearanceLength" to 10));break
            }
            for(e in edges){
                if(l.owner.isNotEmpty() && l.owner==e.edge.id)continue
                val i=e.raw.indexOfFirst { intersects(it,l.rect) }
                if(i>=0){issue("edge-label-overlaps-foreign-edge",e.edge.id,ids,mapOf("ownerEdgeId" to l.owner,"segmentIndex" to i));break}
            }
            for(g in groups.values)if(g.bounds.sides().any { intersects(it,l.rect) }){
                issue("edge-label-overlaps-group-border",l.owner.takeIf { it.isNotEmpty() },ids+g.id,mapOf("groupId" to g.id));break
            }
        }
    }
    fun ports() {
        val incident=linkedMapOf<String,MutableList<EdgeGeometry>>()
        for(g in edges)for(id in listOfNotNull(g.edge.start,g.edge.end).filter { it.isNotEmpty() }.distinct())incident.getOrPut(id){mutableListOf()}.add(g)
        for((id,list)in incident)for(i in list.indices)for(j in i+1 until list.size){
            val a=list[i].edge;val b=list[j].edge
            fun port(e:LayoutValidationEdge)=if(e.start==id)e.points.first() to e.points[1]else e.points.last() to e.points[e.points.lastIndex-1]
            val pa=port(a);val pb=port(b);val d=geometryDistance(pa.first,pb.first)
            val da=direction(pa.first,pa.second);val db=direction(pb.first,pb.second)
            val same=d<=2 && da!=null && da==db
            val details=mapOf("edgeIds" to listOf(a.id,b.id),"attachPoints" to listOf(pa.first,pb.first))
            if(same)issue("edge-same-port-departure",nodes=listOf(id),details=details+mapOf("direction" to da))
            if(d<=3)issue("edge-shared-attachment-point",nodes=listOf(id),details=details+mapOf("distance" to d,"alsoSamePortDeparture" to same))
            else byId[id]?.bounds?.let { r->
                fun project(p:ScenePoint)=ScenePoint(min(max(p.x,r.x),r.right),min(max(p.y,r.y),r.bottom))
                val p1=project(pa.first);val p2=project(pb.first);val pd=geometryDistance(p1,p2)
                if(pd<=3)issue("edge-shared-projected-port",nodes=listOf(id),details=details+mapOf("projectedPorts" to listOf(p1,p2),"rawDistance" to d,"projectedDistance" to pd))
            }
        }
    }
    fun sharedTerminal(a:EdgeGeometry,s:OrthogonalSegment,b:EdgeGeometry,t:OrthogonalSegment):Boolean {
        fun terminal(g:EdgeGeometry,seg:OrthogonalSegment,id:String):Boolean {
            fun touches(p:ScenePoint)=geometryDistance(seg.a,p)<=1 || geometryDistance(seg.b,p)<=1
            return (g.edge.start==id && touches(g.points.first())) || (g.edge.end==id && touches(g.points.last()))
        }
        return listOfNotNull(a.edge.start,a.edge.end).filter { it.isNotEmpty() && (it==b.edge.start || it==b.edge.end) }.any { terminal(a,s,it) && terminal(b,t,it) }
    }
}
