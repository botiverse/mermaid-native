package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.FlowchartDiagram
import build.raft.mermaid.core.FlowNodeShape
import build.raft.mermaid.layout.ScenePoint
import build.raft.mermaid.layout.SceneRect
import kotlin.math.abs

/** Clean already-orthogonal rectangle routes before edge-label placement and painting. */
internal fun cleanFlowEndpointPaths(
    diagram:FlowchartDiagram,
    rects:Map<String,SceneRect>,
    paths:Map<Int,List<ScenePoint>>,
):Map<Int,List<ScenePoint>> {
    val rectangles=diagram.nodes.filter { it.shape==FlowNodeShape.RECTANGLE }.map { it.id }.toSet()
    return paths.mapValues { (index,points)->
        val edge=diagram.edges[index]
        val eligible=edge.sourceId in rectangles && edge.targetId in rectangles && points.size>=2 &&
            points.all { it.x.isFinite() && it.y.isFinite() } &&
            points.zipWithNext().all { (a,b)->abs(a.x-b.x)<1e-3 || abs(a.y-b.y)<1e-3 }
        if(!eligible)points else {
            val route=OrthogonalEndpointRoute("edge-$index",points,edge.sourceId,edge.targetId)
            val cleaned=OrthogonalEndpointCleanup.clipToBoundaries(listOf(route),rects).single().points
            if(cleaned.size>=2)cleaned else points
        }
    }
}
