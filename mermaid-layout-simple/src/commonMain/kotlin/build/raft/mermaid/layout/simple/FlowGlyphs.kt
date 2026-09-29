package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.sqrt

internal fun flowMarker(marker:FlowMarker,from:ScenePoint,to:ScenePoint,fill:SceneColor=SceneColor(DiagramPalette.INK),strokeWidth:Double=1.5):List<DrawCommand> {
    val dx=to.x-from.x;val dy=to.y-from.y;val length=sqrt(dx*dx+dy*dy).coerceAtLeast(0.01)
    val ux=dx/length;val uy=dy/length
    return when(marker){
        FlowMarker.NONE->emptyList()
        FlowMarker.POINT->listOf(DrawPolygon(listOf(to,ScenePoint(to.x-ux*8-uy*4,to.y-uy*8+ux*4),ScenePoint(to.x-ux*8+uy*4,to.y-uy*8-ux*4)),fill=fill))
        FlowMarker.CIRCLE->listOf(DrawEllipse(ScenePoint(to.x-ux*4,to.y-uy*4),4.0,4.0,fill=SceneColor(DiagramPalette.CANVAS),stroke=fill,strokeWidth=strokeWidth))
        FlowMarker.CROSS->listOf(DrawLine(ScenePoint(to.x-5,to.y-5),ScenePoint(to.x+5,to.y+5),stroke=fill,strokeWidth=strokeWidth),DrawLine(ScenePoint(to.x-5,to.y+5),ScenePoint(to.x+5,to.y-5),stroke=fill,strokeWidth=strokeWidth))
    }
}
internal fun flowSpecialShape(shape:FlowNodeShape,r:SceneRect):List<DrawCommand> {
    val x=r.x;val y=r.y;val w=r.width;val h=r.height;val inset=12.0
    val fill=SceneColor(DiagramPalette.SURFACE);val stroke=SceneColor(DiagramPalette.OUTLINE)
    fun point(dx:Double,dy:Double)=ScenePoint(x+dx,y+dy)
    fun polygon(vararg p:ScenePoint)=listOf<DrawCommand>(DrawPolygon(p.toList(),fill=fill),DrawPolyline(p.toList()+p.first(),stroke=stroke,strokeWidth=1.5))
    return when(shape){
        FlowNodeShape.HEXAGON->polygon(point(inset,0.0),point(w-inset,0.0),point(w,h/2),point(w-inset,h),point(inset,h),point(0.0,h/2))
        FlowNodeShape.ASYMMETRIC->polygon(point(0.0,0.0),point(w,0.0),point(w,h),point(0.0,h),point(inset,h/2))
        FlowNodeShape.PARALLELOGRAM->polygon(point(inset,0.0),point(w,0.0),point(w-inset,h),point(0.0,h))
        FlowNodeShape.PARALLELOGRAM_ALT->polygon(point(0.0,0.0),point(w-inset,0.0),point(w,h),point(inset,h))
        FlowNodeShape.TRAPEZOID->polygon(point(inset,0.0),point(w-inset,0.0),point(w,h),point(0.0,h))
        FlowNodeShape.TRAPEZOID_ALT->polygon(point(0.0,0.0),point(w,0.0),point(w-inset,h),point(inset,h))
        FlowNodeShape.SUBROUTINE->listOf(DrawRect(r,fill=fill,stroke=stroke,strokeWidth=1.5),DrawLine(point(8.0,0.0),point(8.0,h),stroke=stroke),DrawLine(point(w-8,0.0),point(w-8,h),stroke=stroke))
        FlowNodeShape.CYLINDER->listOf(DrawEllipse(point(w/2,h-6),w/2,6.0,fill=fill,stroke=stroke),DrawRect(SceneRect(x,y+6,w,h-12),fill=fill,stroke=SceneColor("none")),DrawLine(point(0.0,6.0),point(0.0,h-6),stroke=stroke),DrawLine(point(w,6.0),point(w,h-6),stroke=stroke),DrawEllipse(point(w/2,6.0),w/2,6.0,fill=fill,stroke=stroke))
        else->emptyList()
    }
}
