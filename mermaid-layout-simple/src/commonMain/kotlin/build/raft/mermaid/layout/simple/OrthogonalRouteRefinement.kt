package build.raft.mermaid.layout.simple

import build.raft.mermaid.layout.ScenePoint
import build.raft.mermaid.layout.SceneRect
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** A measured route, including optional label identity for geometry-changing passes. */
public data class OrthogonalRefinementRoute(
    val id: String, val points: List<ScenePoint>, val start: String? = null,
    val end: String? = null, val isLayoutOnly: Boolean = false, val labelNodeId: String? = null,
)
public data class OrthogonalRefinementResult(
    val routes: List<OrthogonalRefinementRoute>, val nodes: List<OrthogonalGeometry.NodeBounds>,
)

/** Immutable Native equivalents of the upstream port, terminal-stub and shared-track passes. */
public object OrthogonalRouteRefinement {
    private const val EPS = 1e-3
    private val SceneRect.right get() = x + width
    private val SceneRect.bottom get() = y + height
    private val SceneRect.cx get() = x + width / 2
    private val SceneRect.cy get() = y + height / 2
    private fun same(a: ScenePoint, b: ScenePoint, epsilon: Double = EPS) = abs(a.x-b.x)<epsilon && abs(a.y-b.y)<epsilon
    private fun dedupe(p: List<ScenePoint>, epsilon: Double = EPS): List<ScenePoint> {
        val out=mutableListOf<ScenePoint>()
        p.forEach { if(out.isEmpty() || !same(out.last(),it,epsilon))out.add(it) }
        return out
    }
    private fun measured(n: OrthogonalGeometry.NodeBounds) = n.rect.width>0 && n.rect.height>0
    private fun entries(nodes: List<OrthogonalGeometry.NodeBounds>, labels: Boolean) = nodes
        .filter { !it.isGroup && it.isEdgeLabel==labels && measured(it) }.map { OrthogonalGeometry.RectEntry(it.id,it.rect) }
    private fun hits(a:ScenePoint,b:ScenePoint,rects:List<OrthogonalGeometry.RectEntry>,exclude:List<String>,shrink:Double) =
        OrthogonalGeometry.segmentHitsAnyRect(a,b,rects,exclude,shrink)

    // Exact three-decimal quantization of the binary input, matching JS toFixed's half-away rule.
    // The 53-bit significand times1000 still fits in a positive Long.
    private fun roundedMillipoint(value:Double):String {
        if(!value.isFinite() || abs(value)>=1e21)return value.toString()
        val bits=abs(value).toBits();val exponent=((bits ushr 52) and 2047).toInt()
        val significand=(bits and 0x000fffffffffffffL) or if(exponent==0)0L else (1L shl 52)
        val numerator=significand*1000
        val shift=if(exponent==0)1074 else 1075-exponent
        if(shift<=0)return value.toString() // Integral doubles at this magnitude already exceed millipoint precision.
        val rounded=if(shift>63)0L else {
            val whole=numerator ushr shift
            val remainder=numerator-(whole shl shift)
            whole+if(remainder >= (1L shl (shift-1)))1 else 0
        }
        return (if(value<0)"-" else "+")+rounded
    }

    public fun swapPorts(routes: List<OrthogonalRefinementRoute>, nodes: List<OrthogonalGeometry.NodeBounds>): List<OrthogonalRefinementRoute> {
        val out=routes.toMutableList()
        val bounds=nodes.filter { !it.isGroup && !it.isEdgeLabel && measured(it) }.associateBy { it.id }
        val rects=entries(nodes,false)
        for(i in out.indices) {
            val edge=out[i]
            if(edge.isLayoutOnly || edge.points.size<4)continue
            val route=OrthogonalGeometry.classifyThreeSegmentRoute(dedupe(edge.points,1e-6),1e-6) ?: continue
            val src=bounds[edge.start]?.rect ?: continue
            val dst=bounds[edge.end]?.rect ?: continue
            if(abs(src.cx-dst.cx)<1e-6 || abs(src.cy-dst.cy)<1e-6)continue
            val end=route.points.last()
            val other=out.filterIndexed { j,_->j!=i }.map { OrthogonalGeometry.Edge(it.points,it.isLayoutOnly) }
            for(delta in listOf(0.0,8.0,-8.0,16.0,-16.0)) {
                val start:ScenePoint
                val bend:ScenePoint
                if(route.kind==OrthogonalGeometry.RouteKind.HVH) {
                    val x=src.cx+delta
                    if(x<=src.x+1e-6 || x>=src.right-1e-6)continue
                    start=ScenePoint(x,if(dst.cy>src.cy)src.bottom else src.y)
                    bend=ScenePoint(x,end.y)
                } else {
                    val y=src.cy+delta
                    if(y<=src.y+1e-6 || y>=src.bottom-1e-6)continue
                    start=ScenePoint(if(dst.cx>src.cx)src.right else src.x,y)
                    bend=ScenePoint(end.x,y)
                }
                val first=same(start,bend,1e-6);val last=same(bend,end,1e-6)
                if(first && last)continue
                if(!first && hits(start,bend,rects,listOfNotNull(edge.start),1.0))continue
                if(!last && hits(bend,end,rects,listOfNotNull(edge.end),1.0))continue
                if(!first && OrthogonalGeometry.segmentConflictsWithAnyEdge(start,bend,other,null,1e-6,true))continue
                if(!last && OrthogonalGeometry.segmentConflictsWithAnyEdge(bend,end,other,null,1e-6,true))continue
                out[i]=edge.copy(points=if(first)listOf(bend,end) else if(last)listOf(start,bend) else listOf(start,bend,end))
                break
            }
        }
        return out
    }

    public fun collapseTerminalStubs(routes:List<OrthogonalRefinementRoute>, nodes:List<OrthogonalGeometry.NodeBounds>):OrthogonalRefinementResult {
        val out=routes.toMutableList();val bounds=nodes.associateBy { it.id }.toMutableMap()
        // Obstacles are the measured snapshot, as in the original pass; label movement is returned separately.
        val real=entries(nodes,false);val labels=entries(nodes,true)
        for(i in out.indices) {
            val edge=out[i];if(edge.isLayoutOnly || edge.points.size<4)continue
            val p=dedupe(edge.points);if(p.size<4)continue
            val end=p.last();val penult=p[p.lastIndex-1];val prev=p[p.lastIndex-2]
            val len=hypot(end.x-penult.x,end.y-penult.y)
            if(len>=10 || len<EPS || hypot(penult.x-prev.x,penult.y-prev.y)<EPS)continue
            val lastH=abs(end.y-penult.y)<EPS && abs(end.x-penult.x)>EPS
            val lastV=abs(end.x-penult.x)<EPS && abs(end.y-penult.y)>EPS
            val prevH=abs(prev.y-penult.y)<EPS && abs(prev.x-penult.x)>EPS
            val prevV=abs(prev.x-penult.x)<EPS && abs(prev.y-penult.y)>EPS
            if(!((lastH && prevV)||(lastV && prevH)))continue
            val dst=bounds[edge.end]?.takeIf { measured(it) }?.rect ?: continue
            val np:ScenePoint;val ne:ScenePoint
            if(prevV) {
                np=ScenePoint(dst.cx,prev.y);ne=ScenePoint(dst.cx,if(penult.y-prev.y<0)dst.bottom else dst.y)
            } else {
                np=ScenePoint(prev.x,dst.cy);ne=ScenePoint(if(penult.x-prev.x>0)dst.right else dst.x,dst.cy)
            }
            if(hits(np,ne,real,listOfNotNull(edge.end),-2.0) || hits(np,ne,labels,emptyList(),-2.0))continue
            val src=bounds[edge.start]?.takeIf { measured(it) }?.rect
            if(src!=null && np.x>src.x+2 && np.x<src.right-2 && np.y>src.y+2 && np.y<src.bottom-2)continue
            // Upstream identifies coincident own segments to three decimals, in their original direction.
            fun key(a:ScenePoint,b:ScenePoint)=listOf(a.x,a.y,b.x,b.y).map { roundedMillipoint(it) }
            val own=p.zipWithNext().map { (a,b)->key(a,b) }.toSet()
            fun crosses(a:ScenePoint,b:ScenePoint)=out.withIndex().any { (j,e)->j!=i && !e.isLayoutOnly &&
                e.points.zipWithNext().any { (c,d)->key(c,d) !in own && OrthogonalGeometry.segmentsStrictlyCross(a,b,c,d,EPS) } }
            if(crosses(np,ne))continue
            val before=p[p.lastIndex-3]
            if(hits(before,np,real,listOfNotNull(edge.start,edge.end),-2.0) || crosses(before,np))continue
            val result=p.take(p.size-3)+listOf(np,ne)
            out[i]=edge.copy(points=result)
            val label=bounds[edge.labelNodeId]
            if(label!=null && measured(label)) {
                var best=-1.0;var midpoint:ScenePoint?=null
                for((a,b) in result.zipWithNext()) {
                    val size=hypot(b.x-a.x,b.y-a.y)
                    if((abs(a.y-b.y)<EPS && size>=label.rect.width+2 || abs(a.x-b.x)<EPS && size>=label.rect.height+2) && size>best) {
                        best=size;midpoint=ScenePoint((a.x+b.x)/2,(a.y+b.y)/2)
                    }
                }
                midpoint?.let { bounds[label.id]=label.copy(rect=SceneRect(it.x-label.rect.width/2,it.y-label.rect.height/2,label.rect.width,label.rect.height)) }
            }
        }
        return OrthogonalRefinementResult(out,nodes.map { bounds.getValue(it.id) })
    }

    private data class Segment(val edge:Int,val index:Int,val a:ScenePoint,val b:ScenePoint,val horizontal:Boolean,val vertical:Boolean,val interior:Boolean)
    private fun segments(edge:Int,p:List<ScenePoint>):List<Segment> = p.zipWithNext().mapIndexedNotNull { index,(a,b)->
        val h=abs(a.y-b.y)<EPS;val v=abs(a.x-b.x)<EPS
        if(same(a,b) || !(h || v))null else Segment(edge,index,a,b,h,v,index>=1 && index<=p.size-3)
    }
    private fun overlap(a:Double,b:Double,c:Double,d:Double)=max(0.0,min(max(a,b),max(c,d))-max(min(a,b),min(c,d)))
    private fun crowded(a:Segment,b:Segment)=
        a.horizontal && b.horizontal && overlap(a.a.x,a.b.x,b.a.x,b.b.x)>=8 && abs(a.a.y-b.a.y)<7 ||
        a.vertical && b.vertical && overlap(a.a.y,a.b.y,b.a.y,b.b.y)>=8 && abs(a.a.x-b.a.x)<7

    public fun nudgeSharedTracks(routes:List<OrthogonalRefinementRoute>, nodes:List<OrthogonalGeometry.NodeBounds>, minimumTerminalRun:Double=0.0):List<OrthogonalRefinementRoute> {
        val out=routes.toMutableList();val bounds=nodes.associateBy { it.id }
        val real=entries(nodes,false);val labels=entries(nodes,true)
        fun safe(index:Int,p:List<ScenePoint>):Boolean {
            if(p.size<2)return false
            if(hypot(p[1].x-p[0].x,p[1].y-p[0].y)<minimumTerminalRun ||
                hypot(p.last().x-p[p.lastIndex-1].x,p.last().y-p[p.lastIndex-1].y)<minimumTerminalRun)return false
            val edge=out[index];val candidate=segments(index,p)
            if(candidate.size!=p.size-1)return false
            for(s in candidate) {
                if(hits(s.a,s.b,real,listOfNotNull(edge.start,edge.end),-2.0) || hits(s.a,s.b,labels,listOfNotNull(edge.labelNodeId),-2.0))return false
            }
            for(j in out.indices) {
                if(j==index || out[j].isLayoutOnly)continue
                for(a in candidate)for(b in segments(j,dedupe(out[j].points))) {
                    if(crowded(a,b) || OrthogonalGeometry.segmentsStrictlyCross(a.a,a.b,b.a,b.b,EPS))return false
                }
            }
            return true
        }
        fun shifted(s:Segment,shift:Double):List<ScenePoint>? {
            val p=dedupe(out[s.edge].points).toMutableList()
            if(p.size<4 || s.index>=p.size-1)return null
            for(k in s.index..s.index+1)p[k]=if(s.horizontal)ScenePoint(p[k].x,p[k].y+shift) else ScenePoint(p[k].x+shift,p[k].y)
            return p.takeIf { segments(s.edge,it).size==it.size-1 }
        }
        fun detoured(s:Segment,shift:Double):List<ScenePoint>? {
            val edge=out[s.edge];val p=dedupe(edge.points)
            if(p.size!=4 || s.index!=1)return null
            val src=bounds[edge.start]?.takeIf { measured(it) }?.rect ?: return null
            val dst=bounds[edge.end]?.takeIf { measured(it) }?.rect ?: return null
            val head:List<ScenePoint>
            if(s.vertical) {
                val below=dst.cy>=src.cy;val y=if(below)src.bottom else src.y;val stub=y+if(below)20 else -20
                if(below && s.b.y<=stub+EPS || !below && s.b.y>=stub-EPS)return null
                val rail=s.a.x+shift
                head=listOf(ScenePoint(src.cx,y),ScenePoint(src.cx,stub),ScenePoint(rail,stub),ScenePoint(rail,s.b.y))
            } else {
                val right=dst.cx>=src.cx;val x=if(right)src.right else src.x;val stub=x+if(right)20 else -20
                if(right && s.b.x<=stub+EPS || !right && s.b.x>=stub-EPS)return null
                val rail=s.a.y+shift
                head=listOf(ScenePoint(x,src.cy),ScenePoint(stub,src.cy),ScenePoint(stub,rail),ScenePoint(s.b.x,rail))
            }
            return dedupe(head+p.drop(s.index+2))
        }
        repeat(12) {
            val all=out.flatMapIndexed { i,e->if(e.isLayoutOnly)emptyList() else segments(i,dedupe(e.points)) }
            var fixed=false
            search@ for(i in all.indices)for(j in i+1 until all.size) {
                val a=all[i];val b=all[j]
                if(a.edge==b.edge || !crowded(a,b))continue
                for(s in listOf(a,b).filter { it.interior })for(shift in listOf(-7.0,7.0,-14.0,14.0,-21.0,21.0)) {
                    val direct=shifted(s,shift)
                    val candidate=if(direct!=null && safe(s.edge,direct))direct else detoured(s,shift)?.takeIf { safe(s.edge,it) }
                    if(candidate!=null) { out[s.edge]=out[s.edge].copy(points=candidate);fixed=true;break@search }
                }
            }
            if(!fixed)return out
        }
        return out
    }
}
