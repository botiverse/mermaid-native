package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.abs

/** Reroute aligned forward edges that cross a third node in a flat flowchart. */
internal fun flowObstacleRoutes(diagram: FlowchartDiagram,rects: Map<String,SceneRect>,
    existing: Map<Int,FlowReturnRoute>,measurer: TextMeasurer,config: LayoutConfig): Map<Int,FlowReturnRoute> {
    if(diagram.subgraphs.isNotEmpty() || rects.isEmpty())return emptyMap()
    val horizontal=diagram.direction in listOf(FlowDirection.LR,FlowDirection.RL)
    val reverse=diagram.direction in listOf(FlowDirection.RL,FlowDirection.BT)
    val sign=if(reverse)-1.0 else 1.0
    data class Box(val start: Double,val end: Double,val cross: Double)
    fun box(r: SceneRect): Box {
        val lo=if(horizontal)r.x else r.y
        val hi=lo+if(horizontal)r.width else r.height
        return Box(if(reverse)-hi else lo,if(reverse)-lo else hi,
            if(horizontal)r.y+r.height/2 else r.x+r.width/2)
    }
    fun point(main: Double,cross: Double)=if(horizontal)ScenePoint(main*sign,cross)else ScenePoint(cross,main*sign)
    val boxes=rects.mapValues { box(it.value) }
    val obstacles=rects.map { OrthogonalGeometry.RectEntry(it.key,it.value) }
    val edges=diagram.edges.mapIndexed { i,e ->
        val a=boxes[e.sourceId];val b=boxes[e.targetId]
        OrthogonalGeometry.Edge(existing[i]?.points ?: if(a!=null && b!=null)
            listOf(point(a.end,a.cross),point(b.start,b.cross))else emptyList(),e.style==FlowEdgeStyle.INVISIBLE)
    }.toMutableList()
    val allBounds=rects.values+existing.values.map { it.bounds }
    var low=allBounds.minOf { if(horizontal)it.y else it.x }
    var high=allBounds.maxOf { if(horizontal)it.y+it.height else it.x+it.width }
    val gap=(config.nodeGap/3).coerceIn(4.0,16.0)
    val result=linkedMapOf<Int,FlowReturnRoute>()
    diagram.edges.forEachIndexed { index,edge ->
        if(index in existing || edge.style==FlowEdgeStyle.INVISIBLE)return@forEachIndexed
        val a=boxes[edge.sourceId] ?: return@forEachIndexed
        val b=boxes[edge.targetId] ?: return@forEachIndexed
        if(b.start<=a.end || abs(a.cross-b.cross)>1e-6)return@forEachIndexed
        val original=edges[index]
        if(!OrthogonalGeometry.segmentHitsAnyRect(original.points[0],original.points[1],obstacles,
                listOf(edge.sourceId,edge.targetId)))return@forEachIndexed
        val aCenter=(a.start+a.end)/2;val bCenter=(b.start+b.end)/2
        val exit=boxes.values.filter { abs((it.start+it.end)/2-aCenter)<1e-3 }.maxOf { it.end }+gap
        val entry=boxes.values.filter { abs((it.start+it.end)/2-bCenter)<1e-3 }.minOf { it.start }-gap
        if(exit>=entry)return@forEachIndexed
        val labelSize=edge.label?.takeIf { it.isNotEmpty() }?.let { measurer.measure(it,TextStyle()) }
        fun candidate(track: Double,lower: Boolean): FlowReturnRoute? {
            val points=listOf(point(a.end,a.cross),point(exit,a.cross),point(exit,track),
                point(entry,track),point(entry,b.cross),point(b.start,b.cross))
            if(points.zipWithNext().any { (p,q)->OrthogonalGeometry.segmentHitsAnyRect(p,q,obstacles) })return null
            val label=labelSize?.let {
                if(horizontal)ScenePoint((entry+exit)/2*sign,track+if(lower)-8.0 else it.height+8.0)
                else ScenePoint(track+(if(lower)-1 else 1)*(8.0+it.width/2),(entry+exit)/2*sign+it.height*0.35)
            }
            val minX=minOf(points.minOf { it.x },label?.let { it.x-labelSize!!.width/2 } ?: Double.POSITIVE_INFINITY)
            val maxX=maxOf(points.maxOf { it.x },label?.let { it.x+labelSize!!.width/2 } ?: Double.NEGATIVE_INFINITY)
            val minY=minOf(points.minOf { it.y },label?.let { it.y-labelSize!!.height } ?: Double.POSITIVE_INFINITY)
            val maxY=maxOf(points.maxOf { it.y },label?.y ?: Double.NEGATIVE_INFINITY)
            return FlowReturnRoute(points,label,SceneRect(minX,minY,maxX-minX,maxY-minY))
        }
        // A shared endpoint stub is preferable to a true crossing. Rank strict crossings first,
        // then retain the broader conflict score (overlap and T-junctions) as a tie breaker.
        val otherSegments=edges.filter { it !== original && !it.isLayoutOnly }.flatMap { it.points.zipWithNext() }
        val chosen=listOfNotNull(candidate(low-24.0,true),candidate(high+24.0,false)).minWithOrNull(
            compareBy<FlowReturnRoute> { route ->
                route.points.zipWithNext().sumOf { (p,q) ->
                    otherSegments.count { (a,b) -> OrthogonalGeometry.segmentsStrictlyCross(p,q,a,b) }
                }
            }.thenBy { route ->
                route.points.zipWithNext().count { (p,q)->OrthogonalGeometry.segmentConflictsWithAnyEdge(p,q,edges,original) }
            }
        ) ?: return@forEachIndexed
        result[index]=chosen
        edges[index]=OrthogonalGeometry.Edge(chosen.points)
        low=minOf(low,if(horizontal)chosen.bounds.y else chosen.bounds.x)
        high=maxOf(high,if(horizontal)chosen.bounds.y+chosen.bounds.height else chosen.bounds.x+chosen.bounds.width)
    }
    return result
}
