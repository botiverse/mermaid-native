package build.raft.mermaid.layout.simple

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private class ExternalRail(val edge: MEdge,val points: List<MPoint>,val index: Int,val horizontal: Boolean,
    val side: String,val coord: Double,val min: Double,val max: Double)

internal fun MaterializedState.reassignExternalChannels() {
    fun rails(): List<ExternalRail> = visible.flatMap { edge ->
        val p=points(edge)
        val source=nodes[edge.start]?.bounds();val target=nodes[edge.end]?.bounds()
        if(source==null || target==null)emptyList() else segments(p).mapNotNull { s ->
            if(s.index<=0 || s.index+1>=p.lastIndex)null else {
                val coord=if(s.vertical)s.a.x else s.a.y
                val low=if(s.vertical)min(source.left,target.left)else min(source.top,target.top)
                val high=if(s.vertical)max(source.right,target.right)else max(source.bottom,target.bottom)
                val side=when {
                    coord<low-M_EPS -> if(s.vertical)"left" else "top"
                    coord>high+M_EPS -> if(s.vertical)"right" else "bottom"
                    else -> null
                }
                side?.let { ExternalRail(edge,p,s.index,s.horizontal,it,coord,
                    if(s.vertical)min(s.a.y,s.b.y)else min(s.a.x,s.b.x),
                    if(s.vertical)max(s.a.y,s.b.y)else max(s.a.x,s.b.x)) }
            }
        }
    }
    fun components(rails: List<ExternalRail>): List<List<ExternalRail>> {
        val seen=mutableSetOf<ExternalRail>();val result=mutableListOf<List<ExternalRail>>()
        for(rail in rails) {
            if(rail in seen)continue
            val queue=mutableListOf(rail);val component=mutableListOf<ExternalRail>();seen+=rail
            while(queue.isNotEmpty()) {
                val current=queue.removeAt(queue.lastIndex);component+=current
                for(next in rails) if(next !in seen && current.edge!==next.edge && current.horizontal==next.horizontal && current.side==next.side && materializedOverlap(current.min,current.max,next.min,next.max)>=M_SHARED) {
                    seen+=next;queue+=next
                }
            }
            if(component.size>1)result+=component
        }
        return result
    }
    fun assignments(component: List<ExternalRail>): List<List<Double>> {
        val current=component.map { it.coord };val coords=mutableListOf<Double>()
        for(rail in component)if(coords.none { abs(it-rail.coord)<M_EPS })coords+=rail.coord
        while(coords.size<component.size) {
            coords+=if(component[0].side in listOf("left","top"))coords.minOrNull()!!-12*(component.size-coords.size)
                else coords.maxOrNull()!!+12*(component.size-coords.size)
        }
        val result=mutableListOf<List<Double>>()
        if(component.size<=6) {
            val used=BooleanArray(coords.size);val next=mutableListOf<Double>()
            fun visit() {
                if(next.size==component.size) {
                    if(next.indices.any { abs(next[it]-current[it])>=M_EPS })result+=next.toList()
                    return
                }
                for(i in coords.indices)if(!used[i]) {
                    used[i]=true;next+=coords[i];visit();next.removeAt(next.lastIndex);used[i]=false
                }
            }
            visit()
        } else for(i in current.indices)for(j in i+1 until current.size) {
            val candidate=current.toMutableList();candidate[i]=current[j];candidate[j]=current[i];result+=candidate
        }
        return result
    }
    fun replacements(component: List<ExternalRail>,assignment: List<Double>): MReplacements? {
        val drafts=linkedMapOf<MEdge,MutableList<MPoint>>()
        for((i,rail) in component.withIndex()) {
            val p=drafts.getOrPut(rail.edge){rail.points.toMutableList()};val coord=assignment[i]
            for(index in rail.index..rail.index+1)p[index]=if(rail.horizontal)p[index].copy(y=coord)else p[index].copy(x=coord)
        }
        val result=linkedMapOf<MEdge,List<MPoint>>()
        for((edge,path) in drafts) {
            val p=simplify(dedupe(path));if(segments(p).size!=p.size-1)return null
            result[edge]=p
        }
        return result
    }
    repeat(4) {
        val current=crossings();if(current==0)return
        var best: MReplacements?=null;var bestCrossings=current;var bestBends=totalBends();var bestDisplacement=Double.POSITIVE_INFINITY
        for(component in components(rails()))for(assignment in assignments(component)) {
            val replacements=replacements(component,assignment)?:continue
            if(replacements.any { (edge,path)->nodeHit(edge,path)||sharedTrack(edge,path,replacements) })continue
            val crossings=crossings(replacements);if(crossings>=current)continue
            val bends=totalBends(replacements);val displacement=component.indices.sumOf { abs(assignment[it]-component[it].coord) }
            if(crossings>bestCrossings || (crossings==bestCrossings && (bends>bestBends || (bends==bestBends && displacement>=bestDisplacement))))continue
            best=replacements;bestCrossings=crossings;bestBends=bends;bestDisplacement=displacement
        }
        commit(best?:return)
    }
}
