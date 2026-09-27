package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.SequenceActor
import build.raft.mermaid.core.SequenceActorKind
import build.raft.mermaid.layout.*

/** Participant stereotypes share measured labels while preserving their distinct native outlines. */
internal fun sequenceParticipant(actor: SequenceActor, x: Double, y: Double, width: Double, height: Double,
    labels: List<String>, style: TextStyle, lineHeight: Double): List<DrawCommand> = buildList {
    val fill=SceneColor("#eaeaea")
    val stroke=SceneColor("#666666")
    fun line(x1: Double,y1: Double,x2: Double,y2: Double) { add(DrawLine(ScenePoint(x+x1,y+y1),ScenePoint(x+x2,y+y2),stroke=stroke)) }
    fun ellipse(dx: Double,dy: Double,rx: Double,ry: Double) { add(DrawEllipse(ScenePoint(x+dx,y+dy),rx,ry,fill=fill,stroke=stroke)) }
    fun rect(dx: Double,dy: Double,w: Double,h: Double) { add(DrawRect(SceneRect(x+dx,y+dy,w,h),fill=fill,stroke=stroke)) }
    when(actor.kind) {
        SequenceActorKind.PARTICIPANT -> add(DrawRect(SceneRect(x-width/2,y,width,height),3.0,fill,stroke))
        SequenceActorKind.ACTOR -> {
            ellipse(0.0,9.0,8.0,8.0)
            line(0.0,17.0,0.0,34.0); line(-14.0,23.0,14.0,23.0)
            line(-12.0,45.0,0.0,34.0); line(0.0,34.0,12.0,45.0)
        }
        SequenceActorKind.BOUNDARY -> {
            line(-23.0,3.0,-23.0,33.0); line(-23.0,18.0,-13.0,18.0)
            ellipse(0.0,18.0,13.0,13.0)
        }
        SequenceActorKind.CONTROL -> {
            ellipse(0.0,20.0,13.0,13.0)
            line(0.0,7.0,-7.0,2.0); line(0.0,7.0,-7.0,12.0)
        }
        SequenceActorKind.ENTITY -> {
            ellipse(0.0,16.0,13.0,13.0); line(-17.0,33.0,17.0,33.0)
        }
        SequenceActorKind.DATABASE -> {
            rect(-22.0,8.0,44.0,27.0)
            ellipse(0.0,35.0,22.0,7.0)
            // Cover the top half of the lower oval so only its exposed rim remains.
            add(DrawRect(SceneRect(x-21,y+26,42.0,9.0),fill=fill,stroke=SceneColor("none"),strokeWidth=0.0))
            ellipse(0.0,8.0,22.0,7.0)
            line(-22.0,8.0,-22.0,35.0); line(22.0,8.0,22.0,35.0)
        }
        SequenceActorKind.COLLECTIONS -> {
            rect(-14.0,2.0,38.0,29.0); rect(-19.0,7.0,38.0,29.0); rect(-24.0,12.0,38.0,29.0)
        }
        SequenceActorKind.QUEUE -> {
            ellipse(-23.0,22.0,7.0,15.0)
            rect(-23.0,7.0,46.0,30.0)
            ellipse(23.0,22.0,7.0,15.0)
        }
    }
    val labelY=if(actor.kind==SequenceActorKind.PARTICIPANT) y+(height-(labels.size-1)*lineHeight)/2+style.fontSize*.35 else y+60
    labels.forEachIndexed { i,label -> add(DrawText(label,ScenePoint(x,labelY+i*lineHeight),TextAnchor.MIDDLE,style)) }
}
