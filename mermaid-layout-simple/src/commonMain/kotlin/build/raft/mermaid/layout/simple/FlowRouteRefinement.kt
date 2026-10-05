package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.FlowchartDiagram
import build.raft.mermaid.core.FlowNodeShape
import build.raft.mermaid.layout.ScenePoint
import build.raft.mermaid.layout.SceneRect
import kotlin.math.abs

/** Refinement is deliberately limited to fully orthogonal, rectangle-only leaf graphs. */
internal fun refineFlowOrthogonalPaths(
    diagram:FlowchartDiagram, rects:Map<String,SceneRect>, paths:Map<Int,List<ScenePoint>>,
):Map<Int,List<ScenePoint>> {
    val ids=diagram.nodes.filter { it.shape==FlowNodeShape.RECTANGLE }.map { it.id }.toSet()
    if(paths.any { (i,p)->
        val e=diagram.edges[i]
        e.sourceId !in ids || e.targetId !in ids || p.size<2 || p.any { !it.x.isFinite() || !it.y.isFinite() } ||
            p.zipWithNext().any { (a,b)->abs(a.x-b.x)>=1e-3 && abs(a.y-b.y)>=1e-3 }
    })return paths
    val indices=paths.keys.toList()
    var routes=indices.map { i->val e=diagram.edges[i];OrthogonalRefinementRoute("edge-$i",paths.getValue(i),e.sourceId,e.targetId) }
    val nodeIds=diagram.nodes.map { it.id }.toSet()
    val nodes=rects.filterKeys { it in nodeIds }.map { (id,r)->OrthogonalGeometry.NodeBounds(id,r) }
    val obstacles=OrthogonalGeometry.collectRealNodeBounds(nodes)
    fun onBorder(p:ScenePoint,r:SceneRect)=p.x>=r.x-1e-3 && p.x<=r.x+r.width+1e-3 && p.y>=r.y-1e-3 && p.y<=r.y+r.height+1e-3 &&
        (abs(p.x-r.x)<1e-3 || abs(p.x-r.x-r.width)<1e-3 || abs(p.y-r.y)<1e-3 || abs(p.y-r.y-r.height)<1e-3)
    fun safe(e:OrthogonalRefinementRoute):Boolean {
        val p=e.points;val src=rects[e.start] ?: return false;val dst=rects[e.end] ?: return false
        if(p.size<2 || !onBorder(p.first(),src) || !onBorder(p.last(),dst))return false
        return p.zipWithNext().all { (a,b)->a.x.isFinite() && a.y.isFinite() && b.x.isFinite() && b.y.isFinite() &&
            (abs(a.x-b.x)<1e-3 || abs(a.y-b.y)<1e-3) &&
            !OrthogonalGeometry.segmentHitsAnyRect(a,b,obstacles,emptyList(),1e-3) }
    }
    fun crossings(edges:List<OrthogonalRefinementRoute>):Int {
        var total=0
        for(i in edges.indices)for(j in i+1 until edges.size)
            for((a,b) in edges[i].points.zipWithNext())for((c,d) in edges[j].points.zipWithNext())
                if(OrthogonalGeometry.segmentsStrictlyCross(a,b,c,d))total++
        return total
    }
    fun accept(candidate:List<OrthogonalRefinementRoute>) {
        if(candidate==routes)return
        // Accept the pass atomically: a partial set could invalidate another route's clearance.
        if(candidate.indices.all { candidate[it]==routes[it] || safe(candidate[it]) } && crossings(candidate)<=crossings(routes))routes=candidate
    }
    accept(OrthogonalRouteRefinement.swapPorts(routes,nodes))
    accept(OrthogonalRouteRefinement.collapseTerminalStubs(routes,nodes).routes)
    accept(OrthogonalRouteRefinement.nudgeSharedTracks(routes,nodes,8.0))
    return indices.mapIndexed { i,index->index to routes[i].points }.toMap()
}
