package build.raft.mermaid.layout.simple

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private data class TerminalLane(
    val edge: MEdge, val nodeId: String, val atStart: Boolean, val isHorizontal: Boolean,
    val coord: Double, val min: Double, val max: Double,
    val boundary: MPoint, val railEnd: MPoint, val rect: MBounds,
)

internal fun MaterializedState.separateTerminalLanes() {
    fun laneFor(edge: MEdge, atStart: Boolean): TerminalLane? {
        val points=points(edge)
        if (points.size<2) return null
        val id=(if(atStart)edge.start else edge.end)?.takeIf { it.isNotEmpty() } ?: return null
        val node=nodes[id] ?: return null; val rect=node.bounds() ?: return null
        val endpoint=if(atStart)points.first() else points.last()
        val adjacent=if(atStart)points[1] else points[points.lastIndex-1]
        val x=node.x?:0.0; val y=node.y?:0.0; val dx=endpoint.x-x; val dy=endpoint.y-y
        var w=(node.width?:0.0)/2; var h=(node.height?:0.0)/2
        val boundary=if(abs(dy)*w>abs(dx)*h) {
            if(dy<0)h=-h
            MPoint(x+if(dy==0.0)0.0 else h*dx/dy,y+h)
        } else {
            if(dx<0)w=-w
            MPoint(x+w,y+if(dx==0.0)0.0 else w*dy/dx)
        }
        val end=if(aligned(adjacent,boundary))adjacent else endpoint
        return when {
            mx(boundary,end) -> TerminalLane(edge,id,atStart,false,boundary.x,min(boundary.y,end.y),max(boundary.y,end.y),boundary,end,rect)
            my(boundary,end) -> TerminalLane(edge,id,atStart,true,boundary.y,min(boundary.x,end.x),max(boundary.x,end.x),boundary,end,rect)
            else -> null
        }
    }
    fun projected(a: TerminalLane,b: TerminalLane)=max(0.0,min(a.max,b.max)-max(a.min,b.min))
    fun sameFace(a: TerminalLane,b: TerminalLane): Boolean {
        if(a.nodeId!=b.nodeId || a.isHorizontal!=b.isHorizontal)return false
        return if(a.isHorizontal) (abs(a.boundary.x-a.rect.left)<1 || abs(a.boundary.x-a.rect.right)<1) && mx(a.boundary,b.boundary,1.0)
        else (abs(a.boundary.y-a.rect.top)<1 || abs(a.boundary.y-a.rect.bottom)<1) && my(a.boundary,b.boundary,1.0)
    }
    fun exact(a: TerminalLane,b: TerminalLane)=a.nodeId==b.nodeId && a.isHorizontal==b.isHorizontal && projected(a,b)>=M_SHARED && abs(a.coord-b.coord)<0.5
    fun near(a: TerminalLane,b: TerminalLane): Boolean {
        if(a.nodeId!=b.nodeId || a.isHorizontal!=b.isHorizontal || !a.isHorizontal || a.atStart==b.atStart)return false
        val shared=projected(a,b); val span=a.rect.bottom-a.rect.top
        return shared>=M_SHARED && shared>=span && shared<=2*span && sameFace(a,b) && abs(a.coord-b.coord)<16
    }
    fun shifted(lane: TerminalLane,shift: Double): List<MPoint>? {
        val p=points(lane.edge); if(p.size<2)return null
        fun moved(p: MPoint)=if(lane.isHorizontal)p.copy(y=p.y+shift)else p.copy(x=p.x+shift)
        val boundary=moved(lane.boundary); val end=moved(lane.railEnd); val r=lane.rect
        val stays=when {
            abs(lane.boundary.y-r.top)<1 || abs(lane.boundary.y-r.bottom)<1 -> my(boundary,lane.boundary) && boundary.x>=r.left+1 && boundary.x<=r.right-1
            abs(lane.boundary.x-r.left)<1 || abs(lane.boundary.x-r.right)<1 -> mx(boundary,lane.boundary) && boundary.y>=r.top+1 && boundary.y<=r.bottom-1
            else -> false
        }
        if(!stays)return null
        if(lane.atStart) {
            val rest=p.drop(if(same(p[1],lane.railEnd))2 else 1)
            if(rest.firstOrNull()?.let { !aligned(it,end) }==true)return null
            return listOf(boundary,end)+rest
        }
        val before=p.dropLast(if(same(p[p.lastIndex-1],lane.railEnd))2 else 1)
        if(before.lastOrNull()?.let { !aligned(it,end) }==true)return null
        return before+listOf(end,boundary)
    }
    fun straight(lane: TerminalLane): Boolean {
        val p=points(lane.edge); if(p.size!=2)return false
        val start=nodes[lane.edge.start] ?: return false; val end=nodes[lane.edge.end] ?: return false
        val dx=abs((start.x?:0.0)-(end.x?:0.0)); val dy=abs((start.y?:0.0)-(end.y?:0.0))
        return (my(p[0],p[1]) && dy<1 && dx>1) || (mx(p[0],p[1]) && dx<1 && dy>1)
    }
    repeat(8) {
        val lanes=visible.flatMap { listOfNotNull(laneFor(it,true),laneFor(it,false)) }
        var fixed=false
        outer@ for(i in lanes.indices) for(j in i+1 until lanes.size) {
            val a=lanes[i]; val b=lanes[j]
            if(a.edge===b.edge || !(exact(a,b)||near(a,b)))continue
            val fixingNear=!exact(a,b)
            val candidates=listOf(a,b).sortedWith(compareBy<TerminalLane>{straight(it)}.thenBy { it.atStart })
            for(lane in candidates) for(shift in listOf(-7.0,7.0,-14.0,14.0,-21.0,21.0)) {
                val candidate=shifted(lane,shift) ?: continue
                val replacement=MEdge(lane.edge.input.copy(points=candidate))
                val next=laneFor(replacement,lane.atStart) ?: continue
                if(lanes.any { it.edge!==lane.edge && (exact(next,it)||(fixingNear&&near(next,it))) })continue
                lane.edge.points=candidate; fixed=true; break@outer
            }
        }
        if(!fixed)return
    }
}

internal fun MaterializedState.swapDestinationTails() {
    fun tail(edge: MEdge): Pair<MPoint,MPoint>? {
        val p=points(edge); if(p.size<4)return null
        return (p[p.lastIndex-1] to p.last()).takeIf { (a,b) -> horizontal(a,b)||vertical(a,b) }
    }
    fun candidate(edge: MEdge,tail: Pair<MPoint,MPoint>): List<MPoint>? {
        val p=points(edge); if(p.size<3)return null
        val (start,turn)=p; val (tailStart,terminal)=tail
        val connector=when {
            horizontal(start,turn) -> MPoint(turn.x,tailStart.y)
            vertical(start,turn) -> MPoint(tailStart.x,turn.y)
            else -> return null
        }
        return simplify(dedupe(listOf(start,turn,connector,tailStart,terminal))).takeIf { segments(it).size==it.size-1 }
    }
    repeat(4) {
        val current=crossings(); if(current==0)return
        var bestCrossings=current; var bestBends=totalBends(); var best: MReplacements?=null
        val groups=visible.filter { !it.end.isNullOrEmpty() && nodes.containsKey(it.end) && points(it).size>=4 }.groupBy { it.end }
        for(group in groups.values) for(i in group.indices) for(j in i+1 until group.size) {
            val a=group[i]; val b=group[j]; val at=tail(a)?:continue; val bt=tail(b)?:continue
            val ac=candidate(a,bt)?:continue; val bc=candidate(b,at)?:continue
            val replacements=mapOf(a to ac,b to bc)
            if(nodeHit(a,ac,labels=false)||nodeHit(b,bc,labels=false)||sharedTrack(a,ac,replacements)||sharedTrack(b,bc,replacements))continue
            val crossings=crossings(replacements); val bends=totalBends(replacements)
            if(crossings>=current || crossings>bestCrossings || (crossings==bestCrossings && bends>=bestBends))continue
            best=replacements; bestCrossings=crossings; bestBends=bends
        }
        commit(best ?: return)
    }
}
