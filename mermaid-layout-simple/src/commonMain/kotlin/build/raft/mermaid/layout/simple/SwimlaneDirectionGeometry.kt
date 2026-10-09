package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.FlowDirection
import build.raft.mermaid.layout.*

/** Direction conversion for already measured canonical TB swimlane geometry.
 * Node sizes remain upright. Nested groups and lane title bands are recomputed.
 * Based on Mermaid direction/lrTransform.ts (MIT; see NOTICE).
 */
public object SwimlaneDirectionGeometry {
    public data class Node(
        val id: String, val x: Double? = null, val y: Double? = null,
        val width: Double = 0.0, val height: Double = 0.0,
        val isGroup: Boolean = false, val parentId: String? = null,
        val padding: Double? = null, val title: SceneRect? = null, val contentTop: Double? = null,
    )
    public data class Result(val nodes: List<Node>, val paths: List<List<ScenePoint>>)

    public fun transform(nodes: List<Node>, paths: List<List<ScenePoint>>, direction: FlowDirection, titleBandSize: Double = 36.0, includeNestedBounds: Boolean = false): Result {
        require(titleBandSize.isFinite() && titleBandSize >= 0)
        require(nodes.size <= 10000 && paths.sumOf { it.size.toLong() } <= 100000) { "Swimlane geometry limit exceeded" }
        require(nodes.map { it.id }.toSet().size == nodes.size) { "Duplicate swimlane node" }
        nodes.forEach { n ->
            require(listOfNotNull(n.x,n.y,n.width,n.height,n.padding).all { it.isFinite() }) { "Nonfinite swimlane geometry" }
            require(n.width >= 0 && n.height >= 0 && (n.padding ?: 0.0) >= 0) { "Negative swimlane dimensions" }
            n.title?.let { require(listOf(it.x,it.y,it.width,it.height).all { v -> v.isFinite() } && it.width >= 0 && it.height >= 0) }
        }
        require(paths.flatten().all { it.x.isFinite() && it.y.isFinite() })
        val map = nodes.associateBy { it.id }.toMutableMap()
        fun ancestors(node: Node): List<String> {
            val ids = mutableListOf<String>(); var parent = node.parentId
            while (parent != null) {
                require(parent != node.id && parent !in ids && ids.size < 128) { "Invalid swimlane hierarchy" }
                val owner = map[parent]?.takeIf { it.isGroup } ?: break
                ids += parent; parent = owner.parentId
            }
            return ids
        }
        nodes.forEach(::ancestors)
        var routes = paths.map { it.toList() }
        val content = nodes.filterNot { it.isGroup }
        fun mirror(horizontal: Boolean) {
            val values = content.mapNotNull { n -> map.getValue(n.id).let { if(horizontal)it.x else it.y } }
            if(values.isEmpty())return
            val sum = values.min() + values.max()
            fun point(p: ScenePoint) = if(horizontal)p.copy(x=sum-p.x) else p.copy(y=sum-p.y)
            map.keys.toList().forEach { id -> val n=map.getValue(id)
                map[id] = n.copy(x=if(horizontal)n.x?.let { sum-it }else n.x,
                    y=if(horizontal)n.y else n.y?.let { sum-it },
                    title=n.title?.let { if(horizontal)it.copy(x=sum-it.x-it.width) else it.copy(y=sum-it.y-it.height) })
            }
            routes=routes.map { it.map(::point) }
        }
        if(direction == FlowDirection.BT)mirror(false)
        if(direction in listOf(FlowDirection.LR,FlowDirection.RL) && content.isNotEmpty()) {
            val minX=content.minOf { it.x ?: 0.0 }; val minY=content.minOf { it.y ?: 0.0 }
            val totalHeight=content.sumOf { it.height }
            val scale=if(totalHeight>0)maxOf(1.0,content.sumOf { it.width }/totalHeight)else 1.0
            fun point(p:ScenePoint)=ScenePoint((p.y-minY)*scale+titleBandSize,p.x-minX)
            content.forEach { n -> val p=point(ScenePoint(n.x ?: 0.0,n.y ?: 0.0));map[n.id]=n.copy(x=p.x,y=p.y) }
            routes=routes.map { it.map(::point) }
            fun bounds(children:List<Node>):SceneRect? {
                val measured=children.filter { it.x!=null && it.y!=null };if(measured.isEmpty())return null
                val left=measured.minOf { it.x!!-it.width/2 };val top=measured.minOf { it.y!!-it.height/2 }
                return SceneRect(left,top,measured.maxOf { it.x!!+it.width/2 }-left,measured.maxOf { it.y!!+it.height/2 }-top)
            }
            nodes.filter { it.isGroup && it.parentId!=null }.sortedByDescending { ancestors(it).size }.forEach { g ->
                bounds(map.values.filter { it.parentId==g.id })?.let { b -> val pad=g.padding ?: 20.0
                    map[g.id]=g.copy(x=b.x+b.width/2,y=b.y+b.height/2,width=b.width+pad,height=b.height+pad)
                }
            }
            val lanes=nodes.filter { it.isGroup && it.parentId==null }
            val laneBounds=lanes.mapNotNull { lane -> bounds((if(includeNestedBounds)nodes.filter { it.parentId==lane.id } else content.filter { ancestors(it).lastOrNull()==lane.id }).map { map.getValue(it.id) })?.let { lane to it } ?: if(includeNestedBounds)lane to SceneRect(titleBandSize,(lane.x ?: minX)-minX-maxOf(40.0,lane.width)/2,0.0,maxOf(40.0,lane.width))else null }.sortedBy { it.second.y+it.second.height/2 }
            if(laneBounds.isNotEmpty()) {
                val pad=lanes.maxOf { it.padding ?: 0.0 };val margin=maxOf(pad,10.0)
                val left=laneBounds.minOf { it.second.x }-margin-titleBandSize
                val right=laneBounds.maxOf { it.second.x+it.second.width }+margin
                laneBounds.forEachIndexed { i,(lane,b) ->
                    val top=if(i==0)b.y-maxOf(pad,36.0)else laneBounds[i-1].second.let { (it.y+it.height+b.y)/2 }
                    val bottom=if(i==laneBounds.lastIndex)b.y+b.height+maxOf(pad,36.0)else (b.y+b.height+laneBounds[i+1].second.y)/2
                    map[lane.id]=lane.copy(x=(left+right)/2,y=(top+bottom)/2,width=right-left,height=maxOf(0.0,bottom-top),title=SceneRect(left,top,titleBandSize,maxOf(0.0,bottom-top)),contentTop=b.y)
                }
            }
            if(direction==FlowDirection.RL)mirror(true)
        }
        return Result(nodes.map { map.getValue(it.id) },routes)
    }
}
