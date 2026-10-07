package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.*

/** Real filled outlines, also used for endpoint intersection. All coordinates stay inside r. */
internal fun expandedFlowPolygons(shape: FlowNodeShape, r: SceneRect): List<List<ScenePoint>> {
    fun p(x: Double,y: Double)=ScenePoint(r.x+x,r.y+y)
    if(shape==FlowNodeShape.MANUAL_INPUT) return listOf(listOf(p(0.0,r.height/3),p(r.width,0.0),p(r.width,r.height),p(0.0,r.height)))
    if(shape !in listOf(FlowNodeShape.DOCUMENTS,FlowNodeShape.PROCESSES)) return emptyList()
    val offset=minOf(5.0,r.width/6,r.height/6)
    val w=r.width-2*offset;val h=r.height-2*offset
    return (2 downTo 0).map { layer ->
        val x=layer*offset;val y=(2-layer)*offset
        if(shape==FlowNodeShape.PROCESSES) listOf(p(x,y),p(x+w,y),p(x+w,y+h),p(x,y+h))
        else listOf(p(x,y),p(x+w,y))+(0..32).map { i ->
            val t=1.0-i/32.0
            p(x+w*t,y+h-offset+offset*sin(2*PI*t))
        }
    }
}

internal fun expandedFlowLabelRect(shape: FlowNodeShape,r: SceneRect): SceneRect = when(shape) {
    FlowNodeShape.MANUAL_INPUT -> r.copy(y=r.y+r.height/3,height=r.height*2/3)
    FlowNodeShape.DOCUMENTS,FlowNodeShape.PROCESSES -> r.copy(y=r.y+10,width=r.width-10,height=r.height-18)
    else -> r
}

/** Clip only new polygon families; keep the old routing contract for existing shapes. */
internal fun clipExpandedFlowShapes(diagram: FlowchartDiagram,rects:Map<String,SceneRect>,paths:Map<Int,List<ScenePoint>>):Map<Int,List<ScenePoint>> {
    val nodes=diagram.nodes.associateBy { it.id }
    fun boundary(id:String,toward:ScenePoint,old:ScenePoint):ScenePoint {
        val r=rects[id] ?: return old
        val polygons=expandedFlowPolygons(nodes[id]?.shape ?: return old,r)
        if(polygons.isEmpty())return old
        val label=expandedFlowLabelRect(nodes.getValue(id).shape,r)
        val center=ScenePoint(label.x+label.width/2,label.y+label.height/2)
        val dx=toward.x-center.x;val dy=toward.y-center.y
        if(abs(dx)+abs(dy)<1e-9)return old
        val hits=polygons.flatMap { points -> (points+points.first()).zipWithNext().mapNotNull { (a,b) ->
            val sx=b.x-a.x;val sy=b.y-a.y;val det=dx*sy-dy*sx
            if(abs(det)<1e-9)null else {
                val ax=a.x-center.x;val ay=a.y-center.y
                val t=(ax*sy-ay*sx)/det;val u=(ax*dy-ay*dx)/det
                if(t>=0 && u>=-1e-9 && u<=1+1e-9)t else null
            }
        } }
        val t=hits.maxOrNull() ?: return old
        return ScenePoint(center.x+t*dx,center.y+t*dy)
    }
    return paths.mapValues { (i,points) ->
        if(points.size<2)points else {
            val e=diagram.edges[i]
            points.toMutableList().also { out -> out[0]=boundary(e.sourceId,points[1],points.first());out[out.lastIndex]=boundary(e.targetId,points[points.lastIndex-1],points.last()) }
        }
    }
}
