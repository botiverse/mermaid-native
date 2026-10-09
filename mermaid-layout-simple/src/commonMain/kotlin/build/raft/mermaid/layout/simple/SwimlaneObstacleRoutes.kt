package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.abs

/** Route in final, upright coordinates. Lane frames are permeable; their titles and leaf nodes are not. */
internal fun swimlaneObstacleRoutes(
    diagram: FlowchartDiagram,
    nodes: Map<String, SceneRect>,
    groups: Map<String, SceneRect>,
    titles: Map<String, SceneRect>,
    existing: Map<Int, FlowReturnRoute>,
    measurer: TextMeasurer,
): Map<Int, FlowReturnRoute> {
    // Preserve the existing bounded fallback for oversized documents.
    if (nodes.size + groups.size > 256 || diagram.edges.size > 256) return emptyMap()
    val shapeById = diagram.nodes.associate { it.id to it.shape }
    val result = linkedMapOf<Int, FlowReturnRoute>()
    val occupiedLabels = mutableListOf<SceneRect>()
    val routed = mutableListOf<List<ScenePoint>>()
    val definitions = diagram.subgraphs.associateBy { it.id }
    fun ancestors(id: String): Set<String> {
        val seen = linkedSetOf<String>()
        var parent = if (id in groups) id else diagram.subgraphs.firstOrNull { id in it.nodeIds }?.id
        while (parent != null && seen.add(parent)) parent = definitions[parent]?.parentId
        return seen
    }
    for ((index, edge) in diagram.edges.withIndex()) {
        if (edge.style == FlowEdgeStyle.INVISIBLE) continue
        val source = nodes[edge.sourceId] ?: groups[edge.sourceId] ?: continue
        val target = nodes[edge.targetId] ?: groups[edge.targetId] ?: continue
        if (edge.sourceId == edge.targetId) {
            existing[index]?.let { result[index] = it; routed += it.points }
            continue
        }
        val related = ancestors(edge.sourceId) + ancestors(edge.targetId)
        val obstacles = nodes.values + titles.filterKeys { it != edge.sourceId && it != edge.targetId }.values +
            groups.filterKeys { it !in related }.values + occupiedLabels
        // Existing same-lane return tracks may remain only when safe in the final geometry.
        val old = existing[index]
        val points = if (old != null && old.points.zipWithNext().all { (a,b) ->
            axisAligned(a,b) && obstacles.none { segmentEntersRect(a,b,it) }
        }) old.points else swimlaneOrthogonalPath(source, target, obstacles, routed,
            shapeById[edge.sourceId] == FlowNodeShape.RECTANGLE, shapeById[edge.targetId] == FlowNodeShape.RECTANGLE) ?: continue
        val labelSize = edge.label?.takeIf { it.isNotEmpty() }?.let { measurer.measure(it, TextStyle()) }
        var label: ScenePoint? = null
        var labelBounds: SceneRect? = null
        if (labelSize != null) {
            // Try the longest segment first; do not place text over a node or lane heading.
            val segments = points.zipWithNext().sortedByDescending { (a,b) -> abs(a.x-b.x)+abs(a.y-b.y) }
            for ((a,b) in segments) {
                for (side in listOf(-1,1)) {
                    val x = (a.x+b.x)/2 + if (abs(a.x-b.x)<1e-6) side*(labelSize.width/2+6) else 0.0
                    val y = (a.y+b.y)/2 + if (abs(a.y-b.y)<1e-6) (if(side<0)-6.0 else labelSize.height+6) else labelSize.height/2
                    val box = SceneRect(x-labelSize.width/2,y-labelSize.height,labelSize.width,labelSize.height)
                    if (obstacles.none { rectanglesOverlap(box,it) }) {
                        label = ScenePoint(x,y); labelBounds = box; break
                    }
                }
                if (label != null) break
            }
        }
        labelBounds?.let { occupiedLabels += it }
        val left = minOf(points.minOf { it.x }, labelBounds?.x ?: Double.POSITIVE_INFINITY)
        val top = minOf(points.minOf { it.y }, labelBounds?.y ?: Double.POSITIVE_INFINITY)
        val right = maxOf(points.maxOf { it.x }, labelBounds?.let { it.x+it.width } ?: Double.NEGATIVE_INFINITY)
        val bottom = maxOf(points.maxOf { it.y }, labelBounds?.let { it.y+it.height } ?: Double.NEGATIVE_INFINITY)
        result[index] = FlowReturnRoute(points,label,SceneRect(left,top,right-left,bottom-top))
        routed += points
    }
    return result
}

private fun axisAligned(a: ScenePoint,b: ScenePoint) = abs(a.x-b.x)<1e-6 || abs(a.y-b.y)<1e-6
private fun rectanglesOverlap(a: SceneRect,b: SceneRect) = minOf(a.x+a.width,b.x+b.width)>maxOf(a.x,b.x)+1e-6 && minOf(a.y+a.height,b.y+b.height)>maxOf(a.y,b.y)+1e-6

/** Strict interior intersection permits ports on borders and paths along clear boundaries. */
internal fun segmentEntersRect(a: ScenePoint,b: ScenePoint,r: SceneRect): Boolean {
    val e = 1e-6
    return if(abs(a.y-b.y)<e) a.y>r.y+e && a.y<r.y+r.height-e && maxOf(a.x,b.x)>r.x+e && minOf(a.x,b.x)<r.x+r.width-e
    else if(abs(a.x-b.x)<e) a.x>r.x+e && a.x<r.x+r.width-e && maxOf(a.y,b.y)>r.y+e && minOf(a.y,b.y)<r.y+r.height-e
    else true
}

/** Bounded rectilinear visibility grid, with bend/overlap costs. No browser or upstream runtime dependency. */
internal fun swimlaneOrthogonalPath(
    source: SceneRect, target: SceneRect, obstacles: List<SceneRect>, previous: List<List<ScenePoint>> = emptyList(),
    distributeSource: Boolean = true, distributeTarget: Boolean = true,
): List<ScenePoint>? {
    val boxes = (obstacles + source + target).distinct()
    if (boxes.any { r -> listOf(r.x,r.y,r.width,r.height).any { !it.isFinite() } || r.width<0 || r.height<0 }) return null
    fun ports(r:SceneRect,distribute:Boolean) = (0..3).flatMap { side ->
        val offset=if(distribute)minOf(8.0,(if(side<2)r.height else r.width)/4)else 0.0
        listOf(0.0,-offset,offset).map { shift -> when(side) {
            0 -> ScenePoint(r.x,r.y+r.height/2+shift)
            1 -> ScenePoint(r.x+r.width,r.y+r.height/2+shift)
            2 -> ScenePoint(r.x+r.width/2+shift,r.y)
            else -> ScenePoint(r.x+r.width/2+shift,r.y+r.height)
        } }
    }
    val sourcePorts=ports(source,distributeSource);val targetPorts=ports(target,distributeTarget)
    fun facing(a:SceneRect,b:SceneRect):Int {
        val dx=b.x+b.width/2-a.x-a.width/2;val dy=b.y+b.height/2-a.y-a.height/2
        return if(abs(dy)>1e-6 && abs(dy)*3>=abs(dx)) {if(dy>0)3 else 2} else if(dx>=0)1 else 0
    }
    val preferredStart=facing(source,target);val preferredEnd=facing(target,source)
    fun anchors(ports:List<ScenePoint>)=ports.mapIndexed { i,p -> when(i/3) {
        0 -> p.copy(x=p.x-8);1 -> p.copy(x=p.x+8);2 -> p.copy(y=p.y-8);else -> p.copy(y=p.y+8)
    } }
    val starts=anchors(sourcePorts);val ends=anchors(targetPorts)
    val xs=(boxes.flatMap { listOf(it.x-8,it.x+it.width+8) }+(starts+ends).map { it.x }).distinct().sorted()
    val ys=(boxes.flatMap { listOf(it.y-8,it.y+it.height+8) }+(starts+ends).map { it.y }).distinct().sorted()
    // Bound both memory and intersection work for adversarial large inputs.
    if(xs.size.toLong()*ys.size>16384 || boxes.size>256) return null
    val nx=xs.size;val count=nx*ys.size
    fun point(i:Int)=ScenePoint(xs[i%nx],ys[i/nx])
    fun id(p:ScenePoint)=ys.binarySearch(p.y)*nx+xs.binarySearch(p.x)
    val allowed=BooleanArray(count) { i -> val p=point(i);boxes.none { p.x>it.x+1e-6 && p.x<it.x+it.width-1e-6 && p.y>it.y+1e-6 && p.y<it.y+it.height-1e-6 } }
    val horizontal=BooleanArray(count);val vertical=BooleanArray(count)
    for(i in 0 until count) if(allowed[i]) {
        if(i%nx+1<nx && allowed[i+1])horizontal[i]=boxes.none { segmentEntersRect(point(i),point(i+1),it) }
        if(i+nx<count && allowed[i+nx])vertical[i]=boxes.none { segmentEntersRect(point(i),point(i+nx),it) }
    }
    val targets=ends.withIndex().filter { (j,p) -> (distributeTarget || j%3==0) && boxes.none { segmentEntersRect(p,targetPorts[j],it) } }.associate { id(it.value) to it.index }
    val distance=DoubleArray(count*2) { Double.POSITIVE_INFINITY };val parent=IntArray(count*2) { -1 }
    data class Entry(val state:Int,val cost:Double)
    val heap=mutableListOf<Entry>()
    fun push(e:Entry) {
        heap+=e;var i=heap.lastIndex
        while(i>0) {val p=(i-1)/2;if(heap[p].cost<=e.cost)break;heap[i]=heap[p];i=p};heap[i]=e
    }
    fun pop():Entry {
        val answer=heap[0];val last=heap.removeAt(heap.lastIndex)
        if(heap.isNotEmpty()) { var i=0
            while(i*2+1<heap.size) { var c=i*2+1;if(c+1<heap.size && heap[c+1].cost<heap[c].cost)c++
                if(heap[c].cost>=last.cost)break;heap[i]=heap[c];i=c };heap[i]=last
        };return answer
    }
    for((j,p) in starts.withIndex()) {
        if(!distributeSource && j%3!=0)continue
        val i=id(p);val d=if(j/3<2)0 else 1
        if(allowed[i] && boxes.none { segmentEntersRect(sourcePorts[j],p,it) }) {
            val cost=8.0+(if(j/3==preferredStart)0 else 80)+(if(j%3==0)0 else 12)+
                (if(previous.any { it.firstOrNull()==sourcePorts[j] })100 else 0)
            distance[i*2+d]=cost;push(Entry(i*2+d,cost))
        }
    }
    val prior=previous.flatMap { it.zipWithNext() }
    var final=-1;var best=Double.POSITIVE_INFINITY
    while(heap.isNotEmpty()) {
        val entry=pop();val state=entry.state;if(entry.cost!=distance[state])continue
        if(entry.cost>=best)break
        val i=state/2;val incoming=state%2
        if(i in targets){
            val port=targets.getValue(i);val side=port/3
            val score=entry.cost+8+(if(side==preferredEnd)0 else 80)+(if(incoming==(if(side<2)0 else 1))0 else 12)+
                (if(port%3==0)0 else 12)+(if(previous.any { it.lastOrNull()==targetPorts[port] })100 else 0)
            if(score<best){final=state;best=score}
        }
        val neighbors=buildList {
            if(i%nx>0 && horizontal[i-1])add((i-1) to 0)
            if(i%nx+1<nx && horizontal[i])add((i+1) to 0)
            if(i>=nx && vertical[i-nx])add((i-nx) to 1)
            if(i+nx<count && vertical[i])add((i+nx) to 1)
        }
        for((j,d) in neighbors) {
            val a=point(i);val b=point(j)
            val length=abs(a.x-b.x)+abs(a.y-b.y)
            val crossings=prior.count { (c,e) -> OrthogonalGeometry.segmentsStrictlyCross(a,b,c,e) }
            val overlap=prior.any { (c,e) ->
                if(d==0) abs(c.y-a.y)<1e-6 && abs(e.y-a.y)<1e-6 && minOf(maxOf(a.x,b.x),maxOf(c.x,e.x))>maxOf(minOf(a.x,b.x),minOf(c.x,e.x))+1e-6
                else abs(c.x-a.x)<1e-6 && abs(e.x-a.x)<1e-6 && minOf(maxOf(a.y,b.y),maxOf(c.y,e.y))>maxOf(minOf(a.y,b.y),minOf(c.y,e.y))+1e-6
            }
            val cost=entry.cost+length+(if(d==incoming)0 else 12)+crossings*1000+(if(overlap)length*2 else 0.0)
            val next=j*2+d
            if(cost<distance[next]){distance[next]=cost;parent[next]=state;push(Entry(next,cost))}
        }
    }
    if(final<0)return null
    val path=mutableListOf<ScenePoint>();var state=final
    while(state>=0){path+=point(state/2);state=parent[state]}
    path.reverse()
    val firstPort=sourcePorts[starts.indexOf(path.first())]
    val lastPort=targetPorts[targets.getValue(final/2)]
    return OrthogonalPolylineCleanup.simplify(listOf(firstPort)+path+lastPort)
}
