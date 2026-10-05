package build.raft.mermaid.layout.simple

import build.raft.mermaid.layout.ScenePoint
import build.raft.mermaid.layout.SceneRect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

public data class OrthogonalEndpointRoute(
    val id: String, val points: List<ScenePoint>, val start: String? = null,
    val end: String? = null, val isLayoutOnly: Boolean = false,
)

/** Rectangle-based endpoint cleanup. Renderer preparation explicitly models the upstream endpoint shim. */
public object OrthogonalEndpointCleanup {
    private const val EPS=1e-3
    private const val INSIDE=0.5
    private enum class Side { TOP,BOTTOM,LEFT,RIGHT }
    private data class Range(val low:Double,val high:Double)
    private val SceneRect.right get()=x+width
    private val SceneRect.bottom get()=y+height
    private fun sameX(a:ScenePoint,b:ScenePoint)=abs(a.x-b.x)<EPS
    private fun sameY(a:ScenePoint,b:ScenePoint)=abs(a.y-b.y)<EPS
    private fun same(a:ScenePoint,b:ScenePoint)=sameX(a,b)&&sameY(a,b)
    private fun horizontal(side:Side)=side==Side.LEFT || side==Side.RIGHT
    private fun bounds(id:String?,nodes:Map<String,SceneRect>)=id?.takeIf { it.isNotEmpty() }?.let { nodes[it] }?.takeIf { it.width>0 && it.height>0 }
    private fun inside(p:ScenePoint,r:SceneRect)=p.x>r.x+INSIDE && p.x<r.right-INSIDE && p.y>r.y+INSIDE && p.y<r.bottom-INSIDE
    private fun enter(outside:ScenePoint,inside:ScenePoint,r:SceneRect):ScenePoint = when {
        sameY(outside,inside)->ScenePoint(if(outside.x<r.x)r.x else r.right,outside.y)
        sameX(outside,inside)->ScenePoint(outside.x,if(outside.y<r.y)r.y else r.bottom)
        else->ScenePoint(outside.x.coerceIn(r.x,r.right),outside.y.coerceIn(r.y,r.bottom))
    }
    private fun clip(points:List<ScenePoint>,r:SceneRect,start:Boolean):List<ScenePoint> {
        val step=if(start)1 else -1;var outside=if(start)0 else points.lastIndex
        while(outside in points.indices && inside(points[outside],r))outside+=step
        val interior=outside-step
        if(outside !in points.indices || interior !in points.indices)return points
        val entry=enter(points[outside],points[interior],r)
        return if(start)listOf(entry)+points.drop(outside)else points.take(outside+1)+entry
    }
    private fun clearance(low:Double,high:Double):Range =
        if(low+4<=high-4)Range(low+4,high-4)else Range((low+high)/2,(low+high)/2)
    private fun range(r:SceneRect,side:Side)=if(horizontal(side))clearance(r.y,r.bottom)else clearance(r.x,r.right)
    private fun side(endpoint:ScenePoint,adjacent:ScenePoint,r:SceneRect):Side? {
        if(sameY(endpoint,adjacent) && endpoint.y>=r.y-EPS && endpoint.y<=r.bottom+EPS) {
            if(abs(endpoint.x-r.x)<EPS)return Side.LEFT
            if(abs(endpoint.x-r.right)<EPS)return Side.RIGHT
        }
        if(sameX(endpoint,adjacent) && endpoint.x>=r.x-EPS && endpoint.x<=r.right+EPS) {
            if(abs(endpoint.y-r.y)<EPS)return Side.TOP
            if(abs(endpoint.y-r.bottom)<EPS)return Side.BOTTOM
        }
        return null
    }
    private fun clearStraight(points:List<ScenePoint>,src:SceneRect?,dst:SceneRect?):List<ScenePoint> {
        if(points.size!=2)return points
        val a=points[0];val b=points[1];val h=when {sameY(a,b)->true;sameX(a,b)->false;else->return points}
        val ranges=listOfNotNull(src?.let { r->side(a,b,r)?.takeIf { horizontal(it)==h }?.let { range(r,it) } },
            dst?.let { r->side(b,a,r)?.takeIf { horizontal(it)==h }?.let { range(r,it) } })
        if(ranges.isEmpty())return points
        val low=ranges.maxOf { it.low };val high=ranges.minOf { it.high };if(low>high)return points
        val current=if(h)a.y else a.x;val next=current.coerceIn(low,high)
        if(abs(next-current)<EPS)return points
        return if(h)listOf(a.copy(y=next),b.copy(y=next))else listOf(a.copy(x=next),b.copy(x=next))
    }
    public fun clipToBoundaries(edges:List<OrthogonalEndpointRoute>,nodes:Map<String,SceneRect>):List<OrthogonalEndpointRoute> = edges.map { e->
        if(e.isLayoutOnly || e.points.size<2)e else {
            val src=bounds(e.start,nodes);val dst=bounds(e.end,nodes);var points=e.points
            if(src!=null)points=clip(points,src,true)
            if(dst!=null)points=clip(points,dst,false)
            points=OrthogonalPolylineCleanup.simplify(OrthogonalPolylineCleanup.orthogonalize(points))
            points=clearStraight(points,src,dst)
            e.copy(points=OrthogonalPolylineCleanup.simplify(OrthogonalPolylineCleanup.orthogonalize(points)))
        }
    }
    private fun adjacent(points:List<ScenePoint>,index:Int,step:Int):ScenePoint? {
        var next=index+step
        while(next in points.indices){if(!same(points[index],points[next]))return points[next];next+=step}
        return points.getOrNull(index+step)
    }
    private fun snap(inner:ScenePoint,endpoint:ScenePoint,r:SceneRect,approach:Boolean=false):ScenePoint {
        if(sameY(inner,endpoint)) {
            if(endpoint.y<r.y-EPS || endpoint.y>r.bottom+EPS)return endpoint
            if(approach && inner.x<r.x-EPS)return ScenePoint(r.x,inner.y)
            if(approach && inner.x>r.right+EPS)return ScenePoint(r.right,inner.y)
            return ScenePoint(if(abs(endpoint.x-r.x)<=abs(endpoint.x-r.right))r.x else r.right,inner.y)
        }
        if(sameX(inner,endpoint)) {
            if(endpoint.x<r.x-EPS || endpoint.x>r.right+EPS)return endpoint
            if(approach && inner.y<r.y-EPS)return ScenePoint(inner.x,r.y)
            if(approach && inner.y>r.bottom+EPS)return ScenePoint(inner.x,r.bottom)
            return ScenePoint(inner.x,if(abs(endpoint.y-r.y)<=abs(endpoint.y-r.bottom))r.y else r.bottom)
        }
        return endpoint
    }
    private fun border(a:ScenePoint,b:ScenePoint,r:SceneRect):Side? {
        val xWithin=min(a.x,b.x)>=r.x-EPS && max(a.x,b.x)<=r.right+EPS
        val yWithin=min(a.y,b.y)>=r.y-EPS && max(a.y,b.y)<=r.bottom+EPS
        return when {
            abs(a.y-r.y)<EPS && abs(b.y-r.y)<EPS && xWithin->Side.TOP
            abs(a.y-r.bottom)<EPS && abs(b.y-r.bottom)<EPS && xWithin->Side.BOTTOM
            abs(a.x-r.x)<EPS && abs(b.x-r.x)<EPS && yWithin->Side.LEFT
            abs(a.x-r.right)<EPS && abs(b.x-r.right)<EPS && yWithin->Side.RIGHT
            else->null
        }
    }
    private fun outward(side:Side,a:ScenePoint,b:ScenePoint,r:SceneRect)=when(side) {
        Side.TOP->sameX(a,b) && b.y<r.y-EPS
        Side.BOTTOM->sameX(a,b) && b.y>r.bottom+EPS
        Side.LEFT->sameY(a,b) && b.x<r.x-EPS
        Side.RIGHT->sameY(a,b) && b.x>r.right+EPS
    }
    private fun collapse(points:List<ScenePoint>,r:SceneRect,start:Boolean):List<ScenePoint> {
        if(points.size<3)return points
        val i=if(start)0 else points.lastIndex;val step=if(start)1 else -1
        val side=border(points[i],points[i+step],r)
        return if(side!=null && outward(side,points[i+step],points[i+2*step],r)) {
            if(start)points.drop(1)else points.dropLast(1)
        }else points
    }
    private fun clearEnd(points:List<ScenePoint>,r:SceneRect,start:Boolean):List<ScenePoint> {
        if(points.size<2)return points
        val i=if(start)0 else points.lastIndex;val step=if(start)1 else -1;val end=points[i]
        val adjacent=adjacent(points,i,step)?:return points;val side=side(end,adjacent,r)?:return points
        val range=range(r,side);val h=horizontal(side)
        val adjusted=if(h)end.copy(y=end.y.coerceIn(range.low,range.high))else end.copy(x=end.x.coerceIn(range.low,range.high))
        if(same(end,adjusted))return points
        val result=points.toMutableList();var index=i
        while(index in points.indices) {
            val point=points[index]
            if(if(h)!sameY(point,end)else !sameX(point,end))break
            result[index]=if(h)point.copy(y=adjusted.y)else point.copy(x=adjusted.x);index+=step
        }
        return result
    }
    /** Upstream renderer shim: snapped routes with bends receive duplicate terminal points. */
    public fun prepareForRenderer(edges:List<OrthogonalEndpointRoute>,nodes:Map<String,SceneRect>):List<OrthogonalEndpointRoute> = edges.map { e->
        if(e.isLayoutOnly || e.points.size<2)e else {
            val src=bounds(e.start,nodes);val dst=bounds(e.end,nodes)
            var points=OrthogonalPolylineCleanup.deduplicate(e.points)
            if(src!=null && points.isNotEmpty()) {
                adjacent(points,0,1)?.let { points=listOf(snap(it,points.first(),src))+points.drop(1) }
                points=collapse(points,src,true)
            }
            if(dst!=null && points.isNotEmpty()) {
                adjacent(points,points.lastIndex,-1)?.let { points=points.dropLast(1)+snap(it,points.last(),dst,true) }
                points=collapse(points,dst,false)
            }
            if(points.size==2)points=clearStraight(points,src,dst) else {
                if(src!=null)points=clearEnd(points,src,true)
                if(dst!=null)points=clearEnd(points,dst,false)
            }
            if(points.size>=3)points=listOf(points.first())+points+points.last()
            e.copy(points=points)
        }
    }
}
