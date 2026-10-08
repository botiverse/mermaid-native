package build.raft.mermaid.layout.simple

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.max

private enum class MSide { TOP, BOTTOM, LEFT, RIGHT;
    val horizontal get() = this==LEFT || this==RIGHT
    val outward get() = if(this==LEFT || this==TOP)-1 else 1
}
private data class MNodeInfo(val id: String,val cx: Double,val cy: Double,val rect: MBounds) {
    fun port(side: MSide): MPoint = when(side) {
        MSide.TOP -> MPoint(cx,rect.top); MSide.BOTTOM -> MPoint(cx,rect.bottom)
        MSide.LEFT -> MPoint(rect.left,cy); MSide.RIGHT -> MPoint(rect.right,cy)
    }
}
private fun basePortPath(src: MPoint,ss: MSide,dst: MPoint,ds: MSide): List<MPoint>? {
    if(ss.horizontal && ds.horizontal) {
        val opposing=(ss==MSide.RIGHT && ds==MSide.LEFT && src.x<dst.x)||(ss==MSide.LEFT && ds==MSide.RIGHT && src.x>dst.x)
        if(opposing) {
            if(my(src,dst))return listOf(src,dst)
            val mid=(src.x+dst.x)/2;return listOf(src,MPoint(mid,src.y),MPoint(mid,dst.y),dst)
        }
        if(ss==ds) {
            if(my(src,dst))return null
            return sameSidePath(src,ss,dst,localTrack(src,ss,dst))
        }
        return null
    }
    if(!ss.horizontal && !ds.horizontal) {
        if(ss==ds) {
            if(mx(src,dst))return null
            return sameSidePath(src,ss,dst,localTrack(src,ss,dst))
        }
        val forward=(ss==MSide.BOTTOM && ds==MSide.TOP && src.y<dst.y)||(ss==MSide.TOP && ds==MSide.BOTTOM && src.y>dst.y)
        if(!forward)return null
        if(mx(src,dst))return listOf(src,dst)
        val mid=(src.y+dst.y)/2;return listOf(src,MPoint(src.x,mid),MPoint(dst.x,mid),dst)
    }
    if(ss.horizontal) {
        val forward=(ss==MSide.RIGHT && dst.x>src.x)||(ss==MSide.LEFT && dst.x<src.x)
        val arrives=(ds==MSide.TOP && src.y<dst.y)||(ds==MSide.BOTTOM && src.y>dst.y)
        return if(forward&&arrives)listOf(src,MPoint(dst.x,src.y),dst)else null
    }
    val forward=(ss==MSide.BOTTOM && dst.y>src.y)||(ss==MSide.TOP && dst.y<src.y)
    val arrives=(ds==MSide.LEFT && src.x<dst.x)||(ds==MSide.RIGHT && src.x>dst.x)
    return if(forward&&arrives)listOf(src,MPoint(src.x,dst.y),dst)else null
}
private fun localTrack(src: MPoint,side: MSide,dst: MPoint): Double = when(side) {
    MSide.LEFT -> min(src.x,dst.x)-20;MSide.RIGHT -> max(src.x,dst.x)+20
    MSide.TOP -> min(src.y,dst.y)-20;MSide.BOTTOM -> max(src.y,dst.y)+20
}
private fun sameSidePath(src: MPoint,side: MSide,dst: MPoint,track: Double): List<MPoint> =
    if(side.horizontal)listOf(src,MPoint(track,src.y),MPoint(track,dst.y),dst)
    else listOf(src,MPoint(src.x,track),MPoint(dst.x,track),dst)
private data class MCrossingPair(val first: MEdge,val second: MEdge,val count: Int)
private data class MSnapshot(val count: Int,val pairs: List<MCrossingPair>,val edges: List<MEdge>)
private fun countBetween(a: List<MSegment>,b: List<MSegment>): Int = a.sumOf { x -> b.count { y -> crosses(x,y) } }
private fun manhattan(p: List<MPoint>) = p.zipWithNext().sumOf { (a,b) -> abs(b.x-a.x)+abs(b.y-a.y) }
private data class MPairCandidate(val path: List<MPoint>,val segments: List<MSegment>,val conflicts: Set<MEdge>,val bends: Int,val length: Double,val crossings: Int)
private data class MPairScore(val replacements: MReplacements,val crossings: Int,val bends: Int,val length: Double) {
    fun betterThan(other: MPairScore) = crossings<other.crossings || (crossings==other.crossings && (bends<other.bends || (bends==other.bends && length<other.length)))
}

internal fun MaterializedState.resolveCrossings() {
    val realNodes=nodes.values.filter { !it.isGroup && !it.isEdgeLabel }.mapNotNull { n -> n.bounds()?.let { MNodeInfo(n.id?:"",n.x?:0.0,n.y?:0.0,it) } }
    if(realNodes.isEmpty())return
    MaterializedCrossingResolver(this,realNodes).resolve()
}

private class MaterializedCrossingResolver(val state: MaterializedState,val realNodes: List<MNodeInfo>) {
    val nodes=realNodes.associateBy { it.id }
    val visible=state.visible
    val edgeIndex=visible.withIndex().associate { it.value to it.index }
    val outside=mapOf(MSide.TOP to realNodes.minOf { it.rect.top }-20,MSide.BOTTOM to realNodes.maxOf { it.rect.bottom }+20,
        MSide.LEFT to realNodes.minOf { it.rect.left }-20,MSide.RIGHT to realNodes.maxOf { it.rect.right }+20)
    fun tracks(side: MSide)= (0..2).map { outside.getValue(side)+side.outward*20*it }
    fun xTracks()=tracks(MSide.LEFT)+tracks(MSide.RIGHT)
    fun yTracks()=tracks(MSide.TOP)+tracks(MSide.BOTTOM)
    fun push(out: MutableList<List<MPoint>>,p: List<MPoint>) {
        val candidate=simplify(dedupe(p));if(segments(candidate).size==candidate.size-1)out+=candidate
    }
    fun forSides(src: MPoint,ss: MSide,dst: MPoint,ds: MSide): List<List<MPoint>> {
        val out=mutableListOf<List<MPoint>>()
        basePortPath(src,ss,dst,ds)?.let { push(out,it) }
        if(ss==ds)for(seed in listOf(localTrack(src,ss,dst),outside.getValue(ss)))for(channel in 0..2)
            push(out,sameSidePath(src,ss,dst,seed+ss.outward*20*channel))
        when {
            ss.horizontal && !ds.horizontal -> for(x in tracks(ss))for(y in tracks(ds))push(out,listOf(src,MPoint(x,src.y),MPoint(x,y),MPoint(dst.x,y),dst))
            !ss.horizontal && ds.horizontal -> for(y in tracks(ss))for(x in tracks(ds))push(out,listOf(src,MPoint(src.x,y),MPoint(x,y),MPoint(x,dst.y),dst))
            ss.horizontal -> for(st in tracks(ss))for(dt in tracks(ds))for(y in yTracks())push(out,listOf(src,MPoint(st,src.y),MPoint(st,y),MPoint(dt,y),MPoint(dt,dst.y),dst))
            else -> for(st in tracks(ss))for(dt in tracks(ds))for(x in xTracks())push(out,listOf(src,MPoint(src.x,st),MPoint(x,st),MPoint(x,dt),MPoint(dst.x,dt),dst))
        }
        return out.map(::dedupe).filter { it.size>=2 }.distinctBy(::materializedPathKey)
    }
    fun preservingDeparture(edge: MEdge): List<List<MPoint>> {
        if(edge.start.isNullOrEmpty())return emptyList()
        val target=nodes[edge.end]?:return emptyList();val p=state.points(edge)
        if(p.size<4)return emptyList()
        val first=p[0];val departure=p[1];val out=mutableListOf<List<MPoint>>()
        if(vertical(first,departure)) {
            for(side in MSide.entries) {
                val dst=target.port(side);val targetTracks=if(!side.horizontal)tracks(side)else yTracks()
                for(track in xTracks()) {
                    push(out,listOf(first,departure,MPoint(track,departure.y),MPoint(track,dst.y),dst))
                    for(t in targetTracks)push(out,listOf(first,departure,MPoint(track,departure.y),MPoint(track,t),MPoint(dst.x,t),dst))
                }
            }
        } else if(horizontal(first,departure)) {
            for(side in MSide.entries) {
                val dst=target.port(side);val targetTracks=if(side.horizontal)tracks(side)else xTracks()
                for(track in yTracks()) {
                    push(out,listOf(first,departure,MPoint(departure.x,track),MPoint(dst.x,track),dst))
                    for(t in targetTracks)push(out,listOf(first,departure,MPoint(departure.x,track),MPoint(t,track),MPoint(t,dst.y),dst))
                }
            }
        }
        return out
    }
    fun candidates(edge: MEdge): List<List<MPoint>> {
        val src=nodes[edge.start]?:return emptyList();val dst=nodes[edge.end]?:return emptyList()
        val out=mutableListOf<List<MPoint>>()
        for(ss in MSide.entries)for(ds in MSide.entries)out+=forSides(src.port(ss),ss,dst.port(ds),ds)
        out+=preservingDeparture(edge)
        return out
    }
    fun snapshot(): MSnapshot {
        val pairs=mutableListOf<MCrossingPair>();val edges=linkedSetOf<MEdge>();var count=0
        for(i in visible.indices)for(j in i+1 until visible.size) {
            val a=visible[i];val b=visible[j];val c=countBetween(segments(state.points(a)),segments(state.points(b)))
            if(c>0){count+=c;pairs+=MCrossingPair(a,b,c);edges+=a;edges+=b}
        }
        return MSnapshot(count,pairs,edges.sortedBy { edgeIndex.getValue(it) })
    }
    fun groups(current: MSnapshot): List<List<MEdge>> {
        val neighbors=linkedMapOf<MEdge,MutableSet<MEdge>>()
        for(pair in current.pairs) { neighbors.getOrPut(pair.first){linkedSetOf()}.add(pair.second);neighbors.getOrPut(pair.second){linkedSetOf()}.add(pair.first) }
        val seen=mutableSetOf<MEdge>();val out=mutableListOf<List<MEdge>>()
        for(edge in current.edges) {
            if(edge in seen)continue
            val queue=mutableListOf(edge);val component=mutableListOf<MEdge>();seen+=edge
            while(queue.isNotEmpty()) {
                val e=queue.removeAt(queue.lastIndex);component+=e
                for(next in neighbors[e].orEmpty())if(next !in seen){seen+=next;queue+=next}
            }
            component.sortBy { edgeIndex.getValue(it) }
            if(component.size<2)continue
            val endpointIds=component.flatMap { it.endpointIds }.toSet()
            val group=component+visible.filter { it !in component && it.endpointIds.any { id -> id in endpointIds } }
            out+=group.sortedBy { edgeIndex.getValue(it) }
        }
        return out
    }
    fun pairCandidates(edge: MEdge,current: MSnapshot,base: Map<MEdge,List<MSegment>>): List<MPairCandidate> {
        val oldCount=current.pairs.filter { it.first===edge || it.second===edge }.sumOf { it.count }
        return candidates(edge).map { simplify(dedupe(it)) }.filter { !state.nodeHit(edge,it,labels=false) && it.size>=2 }
            .distinctBy(::materializedPathKey).map { path ->
                val s=segments(path)
                val conflicts=visible.filter { other -> other!==edge && s.any { a -> base.getValue(other).any { b -> shared(a,b)>=M_SHARED } } }.toSet()
                val replacementAffected=visible.filter { it!==edge }.sumOf { countBetween(s,base.getValue(it)) }
                MPairCandidate(path,s,conflicts,bends(path),manhattan(path),current.count-oldCount+replacementAffected)
            }.filter { it.crossings<=current.count }
            .sortedWith(compareBy<MPairCandidate>{it.crossings}.thenBy{it.bends}.thenBy{it.length}).take(48)
    }
    fun bestPair(current: MSnapshot): MReplacements? {
        val base=visible.associateWith { segments(state.points(it)) }
        val baseBends=visible.associateWith { bends(state.points(it)) };val baseLength=visible.associateWith { manhattan(state.points(it)) }
        val currentBends=baseBends.values.sum();val currentLength=baseLength.values.sum()
        val groups=groups(current);val options=linkedMapOf<MEdge,List<MPairCandidate>>()
        for(group in groups)for(edge in group)if(edge !in options) {
            val candidates=pairCandidates(edge,current,base);if(candidates.isNotEmpty())options[edge]=candidates
        }
        var best=MPairScore(emptyMap(),current.count,currentBends,currentLength)
        for(group in groups) {
            val edges=group.filter { it in options }
            for(i in edges.indices)for(j in i+1 until edges.size) {
                val a=edges[i];val b=edges[j]
                if(a !in current.edges && b !in current.edges)continue
                val oldCount=current.pairs.filter { it.first===a || it.second===a || it.first===b || it.second===b }.sumOf { it.count }
                for(ac in options.getValue(a))for(bc in options.getValue(b)) {
                    if(ac.conflicts.any { it!==b } || bc.conflicts.any { it!==a } || ac.segments.any { x -> bc.segments.any { y -> shared(x,y)>=M_SHARED } })continue
                    var replacementCount=countBetween(ac.segments,bc.segments)
                    for(other in visible)if(other!==a && other!==b)replacementCount+=countBetween(ac.segments,base.getValue(other))+countBetween(bc.segments,base.getValue(other))
                    val crossings=current.count-oldCount+replacementCount
                    if(crossings>=current.count)continue
                    val score=MPairScore(mapOf(a to ac.path,b to bc.path),crossings,
                        currentBends-baseBends.getValue(a)-baseBends.getValue(b)+ac.bends+bc.bends,
                        currentLength-baseLength.getValue(a)-baseLength.getValue(b)+ac.length+bc.length)
                    if(score.betterThan(best))best=score
                }
            }
        }
        return best.replacements.takeIf { it.isNotEmpty() }
    }
    fun resolve() {
        repeat(4) {
            val current=snapshot();if(current.count==0)return
            val base=visible.associateWith { segments(state.points(it)) }
            var bestEdge: MEdge?=null;var bestPath: List<MPoint>?=null;var bestCrossings=current.count;var bestBends=Int.MAX_VALUE
            for(edge in current.edges) {
                val oldBends=bends(state.points(edge))
                val oldCrossings=current.pairs.filter { it.first===edge || it.second===edge }.sumOf { it.count }
                for(candidate in candidates(edge)) {
                    if(state.nodeHit(edge,candidate,labels=false)||state.sharedTrack(edge,candidate))continue
                    val candidateSegments=segments(candidate)
                    val crossings=current.count-oldCrossings+visible.filter { it!==edge }.sumOf { countBetween(candidateSegments,base.getValue(it)) };val bends=bends(candidate)
                    if(!(crossings<current.count || (crossings==current.count && bends<oldBends)))continue
                    if(crossings>bestCrossings || (crossings==bestCrossings && bends>=bestBends))continue
                    bestEdge=edge;bestPath=candidate;bestCrossings=crossings;bestBends=bends
                }
            }
            if(bestEdge!=null && bestPath!=null)bestEdge.points=bestPath
            else state.commit(bestPair(current)?:return)
        }
    }
}
