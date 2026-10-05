package build.raft.mermaid.layout.simple

import build.raft.mermaid.layout.ScenePoint
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Pure route cleanup with the upstream swimlane tolerance, separate from validation scoring. */
public object OrthogonalPolylineCleanup {
    private const val EPS = 1e-3
    private fun sameX(a: ScenePoint,b: ScenePoint)=abs(a.x-b.x)<EPS
    private fun sameY(a: ScenePoint,b: ScenePoint)=abs(a.y-b.y)<EPS
    private fun same(a: ScenePoint,b: ScenePoint)=sameX(a,b)&&sameY(a,b)

    public fun deduplicate(points: List<ScenePoint>): List<ScenePoint> {
        val result=mutableListOf<ScenePoint>()
        for(point in points)if(result.isEmpty() || !same(result.last(),point))result+=point
        return result
    }
    /** Empty and singleton Native routes remain safe; a diagonal follows its incoming axis. */
    public fun orthogonalize(points: List<ScenePoint>): List<ScenePoint> {
        if(points.size<2)return points.toList()
        val result=mutableListOf(points.first())
        for(current in points.drop(1)) {
            val previous=result.last()
            if(!sameX(previous,current) && !sameY(previous,current)) {
                val incomingVertical=result.size>=2 && sameX(result[result.lastIndex-1],previous)
                result+=if(incomingVertical)ScenePoint(previous.x,current.y)else ScenePoint(current.x,previous.y)
            }
            result+=current
        }
        return deduplicate(result)
    }
    /** Removes out-and-back spikes and strictly intermediate collinear points, up to 32 passes. */
    public fun simplify(points: List<ScenePoint>): List<ScenePoint> {
        if(points.size<3)return points.toList()
        fun between(value:Double,a:Double,b:Double)=value>min(a,b)+EPS && value<max(a,b)-EPS
        fun intermediate(a:ScenePoint,b:ScenePoint,c:ScenePoint)=
            if(sameX(a,b)&&sameX(b,c))between(b.y,a.y,c.y)
            else if(sameY(a,b)&&sameY(b,c))between(b.x,a.x,c.x)else false
        var work=points.toList()
        repeat(32) {
            val result=mutableListOf<ScenePoint>();var changed=false;var index=0
            while(index<work.size) {
                val previous=result.lastOrNull();val current=work[index];val next=work.getOrNull(index+1)
                if(previous!=null && next!=null) {
                    if(same(previous,next)){index+=2;changed=true;continue}
                    if(intermediate(previous,current,next)){index++;changed=true;continue}
                }
                result+=current;index++
            }
            work=result
            if(!changed)return work
        }
        return work
    }
}
