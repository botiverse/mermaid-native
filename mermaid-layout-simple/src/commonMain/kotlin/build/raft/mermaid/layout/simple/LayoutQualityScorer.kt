package build.raft.mermaid.layout.simple

import build.raft.mermaid.layout.*
import kotlin.math.*
import kotlin.random.Random

/** Reference-compatible soft metrics on measured geometry; never repairs routes. */
public object LayoutQualityScorer {
    /** T intersections count; an endpoint shared by both segments does not. */
    public fun segmentsCross(a:ScenePoint,b:ScenePoint,c:ScenePoint,d:ScenePoint):Boolean =
        orthogonalSegmentsCross(OrthogonalSegment(a,b),OrthogonalSegment(c,d))

    public fun score(input:LayoutValidationInput,thresholds:Map<String,LayoutQualityThreshold>?=null):LayoutQualityResult {
        val leaves=input.nodes.filter { !it.isGroup };val byId=input.nodes.associateBy { it.id }
        val edges=input.edges.filter { it.points.size>=2 }
        val segments=edges.map { normalizeOrthogonalPolyline(it.points.zipWithNext(::OrthogonalSegment)) }
        val bends=segments.map { it.zipWithNext().count { (a,b)->a.orientation!=b.orientation } }
        val totalBends=bends.sum()
        val actual=edges.sumOf { e->e.points.zipWithNext().sumOf { (a,b)->geometryDistance(a,b) } }
        val theoretical=edges.sumOf { e->
            val a=byId[e.start];val b=byId[e.end]
            if(a==null || b==null)0.0 else abs(center(a).x-center(b).x)+abs(center(a).y-center(b).y)
        }
        val width=if(leaves.isEmpty())Double.NaN else leaves.maxOf { it.bounds.x+it.bounds.width }-leaves.minOf { it.bounds.x }
        val height=if(leaves.isEmpty())Double.NaN else leaves.maxOf { it.bounds.y+it.bounds.height }-leaves.minOf { it.bounds.y }
        var crossings=0
        for(i in segments.indices)for(j in i+1 until segments.size)for(a in segments[i])for(b in segments[j])if(orthogonalSegmentsCross(a,b))crossings++
        val scores=LayoutQualityScores(
            if(theoretical==0.0)Double.NaN else actual/theoretical,
            if(height==0.0)Double.POSITIVE_INFINITY else width/height,
            if(edges.isEmpty())Double.NaN else totalBends.toDouble()/edges.size,totalBends,crossings,
            rank(leaves,edges),neighborhood(leaves,edges),Double.NaN,width*height,
            if(edges.isEmpty())Double.NaN else bends.count { it==0 }.toDouble()/edges.size,
            edges.count { diagonalEndpoint(it,byId[it.start],byId[it.end]) },
        )
        val values=scores.values()
        val results=thresholds?.mapValues { (key,bounds)->
            require(key in values){"Unknown layout metric: $key"}
            val value=values.getValue(key)
            fun number(n:Double)=if(n.isFinite() && n==n.toLong().toDouble())n.toLong().toString()else n.toString()
            val description=listOfNotNull(bounds.min?.let { "min: ${number(it)}" },bounds.max?.let { "max: ${number(it)}" }).joinToString(", ")
            LayoutThresholdResult(value,description,!value.isNaN() && (bounds.min==null || value>=bounds.min) && (bounds.max==null || value<=bounds.max))
        }
        return LayoutQualityResult(scores,results)
    }
    private fun center(n:LayoutValidationNode)=ScenePoint(n.bounds.x+n.bounds.width/2,n.bounds.y+n.bounds.height/2)
    private fun ranks(values:List<Double>):List<Double> {
        val order=values.indices.sortedBy { values[it] };val result=MutableList(values.size){0.0};var i=0
        while(i<order.size){var j=i;while(j<order.size && values[order[j]]==values[order[i]])j++
            val rank=(i+1+j)/2.0;for(k in i until j)result[order[k]]=rank;i=j}
        return result
    }
    private fun rank(nodes:List<LayoutValidationNode>,edges:List<LayoutValidationEdge>):Double {
        if(nodes.size<2)return Double.NaN
        val byId=nodes.associateBy { it.id };val adjacency=byId.keys.associateWith { mutableListOf<String>() };val degree=byId.keys.associateWith { 0 }.toMutableMap()
        for(e in edges)if(e.start in byId && e.end in byId){adjacency.getValue(e.start!!).add(e.end!!);degree[e.end]=degree.getValue(e.end)+1}
        val roots=degree.filterValues { it==0 }.keys
        if(roots.isEmpty())return Double.NaN
        val depths=roots.associateWith { 0 }.toMutableMap();val queue=roots.toMutableList();var at=0
        while(at<queue.size){val current=queue[at++];for(next in adjacency.getValue(current))if(next !in depths){depths[next]=depths.getValue(current)+1;queue+=next}}
        if(depths.size<2)return Double.NaN
        val dr=ranks(depths.values.map { it.toDouble() });val yr=ranks(depths.keys.map { center(byId.getValue(it)).y })
        val n=depths.size.toDouble();return 1-6*dr.indices.sumOf { (dr[it]-yr[it]).pow(2) }/(n*(n*n-1))
    }
    private fun neighborhood(nodes:List<LayoutValidationNode>,edges:List<LayoutValidationEdge>):Double {
        if(nodes.size<2)return Double.NaN
        val byId=nodes.associateBy { it.id };val ids=byId.keys.sorted()
        fun key(a:String,b:String)=if(a<b)a to b else b to a
        val connected=edges.mapNotNull { e->if(e.start in byId && e.end in byId && e.start!=e.end)key(e.start!!,e.end!!)else null }.toSet()
        var connectedSum=0.0;var unconnectedSum=0.0;var connectedCount=0;var unconnectedCount=0
        fun add(a:String,b:String){val d=geometryDistance(center(byId.getValue(a)),center(byId.getValue(b)))
            if(key(a,b) in connected){connectedSum+=d;connectedCount++}else{unconnectedSum+=d;unconnectedCount++}}
        if(ids.size<=500){for(i in ids.indices)for(j in i+1 until ids.size)add(ids[i],ids[j])}
        else {
            connected.forEach { (a,b)->add(a,b) }
            // Fixed seed keeps Native diagnostics reproducible. The reference
            // samples randomly too; individual large-graph values may differ.
            val random=Random(0);var attempts=0
            while(unconnectedCount<200 && attempts++<2000){val a=ids[random.nextInt(ids.size)];val b=ids[random.nextInt(ids.size)];if(a!=b && key(a,b) !in connected)add(a,b)}
        }
        if(connectedCount==0 || unconnectedCount==0 || unconnectedSum==0.0)return Double.NaN
        return (1-(connectedSum/connectedCount)/(unconnectedSum/unconnectedCount)).coerceIn(0.0,1.0)
    }
    private fun intersect(n:LayoutValidationNode,p:ScenePoint):ScenePoint {
        val center=center(n);val dx=p.x-center.x;val dy=p.y-center.y
        var w=n.bounds.width/2;var h=n.bounds.height/2
        return if(abs(dy)*w>abs(dx)*h){if(dy<0)h=-h;ScenePoint(center.x+if(dy==0.0)0.0 else h*dx/dy,center.y+h)}
        else {if(dx<0)w=-w;ScenePoint(center.x+w,center.y+if(dx==0.0)0.0 else w*dy/dx)}
    }
    private fun diagonalEndpoint(e:LayoutValidationEdge,start:LayoutValidationNode?,end:LayoutValidationNode?):Boolean {
        val p=e.points
        fun diagonal(a:ScenePoint,b:ScenePoint)=abs(a.x-b.x)>0.5 && abs(a.y-b.y)>0.5
        fun duplicate(a:ScenePoint,b:ScenePoint)=abs(a.x-b.x)<0.5 && abs(a.y-b.y)<0.5
        if(p.size==2)return start!=null && end!=null && diagonal(intersect(start,p[0]),intersect(end,p[1]))
        var first=false;var last=false
        if(start!=null){val inner=p[1];val snap=intersect(start,inner);first=if(duplicate(snap,inner))diagonal(inner,p[2])else diagonal(snap,inner)}
        if(end!=null){val inner=p[p.lastIndex-1];val snap=intersect(end,inner);last=if(duplicate(snap,inner))diagonal(p[p.lastIndex-2],inner)else diagonal(inner,snap)}
        return first || last
    }
}
