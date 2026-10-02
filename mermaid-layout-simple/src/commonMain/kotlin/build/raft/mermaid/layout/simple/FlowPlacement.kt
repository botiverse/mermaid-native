package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.max

/** A deterministic hierarchy layout; graph crossing minimization remains separate. */
internal class FlowPlacement(private val diagram:FlowchartDiagram,private val nodeSizes:Map<String,SceneSize>,private val config:LayoutConfig,private val measurer:TextMeasurer, private val unframedGroups:Set<String> = emptySet(), private val rankByEdges:Boolean = false) {
    data class Result(val width:Double,val height:Double,val nodes:Map<String,SceneRect>,val groups:Map<String,SceneRect>,val returnRoutes:Map<Int,FlowReturnRoute> = emptyMap())
    private val groups=diagram.subgraphs.associateBy { it.id }
    private val parents=groups.mapValues { (_,group) ->
        val seen=mutableSetOf(group.id);var parent=group.parentId;var valid=true
        while(parent!=null){if(!seen.add(parent) || parent !in groups){valid=false;break};parent=groups[parent]?.parentId}
        group.parentId.takeIf { valid }
    }
    private val owners=diagram.nodes.associate { node->node.id to diagram.subgraphs.firstOrNull { node.id in it.nodeIds }?.id }
    private val sizes=mutableMapOf<String,SceneSize>()
    private val rects=linkedMapOf<String,SceneRect>()
    private val groupRects=linkedMapOf<String,SceneRect>()
    private val returnRoutes=linkedMapOf<Int,FlowReturnRoute>()
    private fun direction(parent:String?)=groups[parent]?.direction ?: diagram.direction
    private fun horizontal(parent:String?)=direction(parent) in listOf(FlowDirection.LR,FlowDirection.RL)
    private fun children(parent:String?):List<String> = diagram.subgraphs.filter { parents[it.id]==parent }.map { it.id } + diagram.nodes.filter { owners[it.id]==parent }.map { it.id }
    fun edgeDirection(source:String,target:String):FlowDirection {
        fun ancestors(id:String):List<String> {
            val result=mutableListOf<String>();var parent=if(id in groups)parents[id]else owners[id]
            while(parent!=null && parent !in result){result.add(parent);parent=parents[parent]}
            return result
        }
        val targets=ancestors(target).toSet()
        return direction(ancestors(source).firstOrNull { it in targets })
    }
    private fun gap(from:String,to:String):Double = config.nodeGap*max(1,diagram.edges.filter { (it.sourceId==from && it.targetId==to) || (it.sourceId==to && it.targetId==from) }.maxOfOrNull { it.length.coerceAtMost(10) } ?: 1)
    private data class Layer(val rank:Int,val ids:List<String>)
    private val layerCache=mutableMapOf<String?,List<Layer>?>()
    // Remove feedback arcs only from ranking where separate return routes are supported:
    // flat flowcharts and framed containers. Original edges remain in the diagram.
    private fun layers(parent:String?):List<Layer>? = layerCache.getOrPut(parent) {
        if(!rankByEdges)return@getOrPut null
        val ids=children(parent)
        fun sibling(endpoint:String):String? {
            var id=endpoint
            val seen=mutableSetOf<String>()
            while(id !in ids && seen.add(id))id=(if(id in groups)parents[id]else owners[id]) ?: return null
            return id.takeIf { it in ids }
        }
        var links=diagram.edges.mapNotNull { edge ->
            val from=sibling(edge.sourceId);val to=sibling(edge.targetId)
            if(from==null || to==null || from==to)null else Triple(from,to,edge.length.coerceIn(1,10))
        }
        if(links.isEmpty())return@getOrPut null
        if(diagram.subgraphs.isEmpty() || (parent!=null && parent !in unframedGroups)) {
            val outgoing=links.groupBy { it.first }
            val active=mutableSetOf<String>()
            val visited=mutableSetOf<String>()
            val feedback=mutableSetOf<Triple<String,String,Int>>()
            // Explicit stack avoids recursion limits on long flowcharts.
            // In compound containers, declaration order follows the first node
            // represented by each sibling, rather than putting every group first.
            val roots=if(ids.any { it in groups })ids.sortedBy { id ->
                diagram.nodes.indexOfFirst { sibling(it.id)==id }.let { if(it<0)Int.MAX_VALUE else it }
            }else ids
            roots.forEach { root ->
                if(visited.add(root)) {
                    active.add(root)
                    val stack=mutableListOf(root to outgoing[root].orEmpty().iterator())
                    while(stack.isNotEmpty()) {
                        val (id,edges)=stack.last()
                        if(!edges.hasNext()) { active.remove(id);stack.removeAt(stack.lastIndex);continue }
                        val edge=edges.next();val target=edge.second
                        if(target in active)feedback.add(edge)
                        else if(visited.add(target)) { active.add(target);stack.add(target to outgoing[target].orEmpty().iterator()) }
                    }
                }
            }
            links=links.filterNot { it in feedback }
        }
        val incoming=ids.associateWith { id->links.count { it.second==id } }.toMutableMap()
        val ranks=ids.associateWith { 0 }.toMutableMap()
        val queue=ids.filter { incoming.getValue(it)==0 }.toMutableList()
        var next=0
        while(next<queue.size){
            val id=queue[next++]
            links.filter { it.first==id }.forEach { (_,target,length)->
                ranks[target]=max(ranks.getValue(target),ranks.getValue(id)+length)
                incoming[target]=incoming.getValue(target)-1
                if(incoming.getValue(target)==0)queue.add(target)
            }
        }
        if(queue.size!=ids.size)return@getOrPut null
        ids.groupBy { ranks.getValue(it) }.entries.sortedBy { it.key }.map { (rank,members)->Layer(rank,members) }
    }
    private fun layerSize(layer:Layer,parent:String?):SceneSize {
        val values=layer.ids.map(::size)
        val crossGap=config.nodeGap*(values.size-1).coerceAtLeast(0)
        return if(horizontal(parent))SceneSize(values.maxOf { it.width },values.sumOf { it.height }+crossGap)
            else SceneSize(values.sumOf { it.width }+crossGap,values.maxOf { it.height })
    }
    private fun rawMeasure(parent:String?):SceneSize {
        layers(parent)?.let { layers ->
            val values=layers.map { layerSize(it,parent) }
            val spacing=layers.zipWithNext().sumOf { (a,b)->config.nodeGap*(b.rank-a.rank) }
            return if(horizontal(parent))SceneSize(values.sumOf { it.width }+spacing,values.maxOf { it.height })
                else SceneSize(values.maxOf { it.width },values.sumOf { it.height }+spacing)
        }

        val ids=children(parent);val values=ids.map(::size);val spacing=ids.zipWithNext().sumOf { gap(it.first,it.second) }
        return if(horizontal(parent))SceneSize(values.sumOf { it.width }+spacing,values.maxOfOrNull { it.height }?:0.0) else SceneSize(values.maxOfOrNull { it.width }?:0.0,values.sumOf { it.height }+spacing)
    }
    private fun headerHeight(id:String)=if(id in unframedGroups)0.0 else max(32.0,flowGroupStyle(groups.getValue(id),diagram).text.fontSize+18)
    private fun size(id:String):SceneSize = nodeSizes[id] ?: sizes.getOrPut(id){val content=measure(id);if(id in unframedGroups)content else SceneSize(max(content.width+32,measurer.measure(groups.getValue(id).label,flowGroupStyle(groups.getValue(id),diagram).text).width+24),max(52.0,content.height+headerHeight(id)+16))}
    private fun childRects(parent:String?,x:Double,y:Double):Map<String,SceneRect>{
        val result=linkedMapOf<String,SceneRect>()
        val content=rawMeasure(parent)
        layers(parent)?.let { original ->
            val layers=if(direction(parent) in listOf(FlowDirection.BT,FlowDirection.RL))original.reversed()else original
            var main=if(horizontal(parent))x else y
            layers.forEachIndexed { index,layer ->
                val layerSize=layerSize(layer,parent)
                var cross=if(horizontal(parent))y+(content.height-layerSize.height)/2 else x+(content.width-layerSize.width)/2
                layer.ids.forEach { id ->
                    val sz=size(id)
                    val r=if(horizontal(parent))SceneRect(main+(layerSize.width-sz.width)/2,cross,sz.width,sz.height)
                        else SceneRect(cross,main+(layerSize.height-sz.height)/2,sz.width,sz.height)
                    result[id]=r
                    cross+=(if(horizontal(parent))sz.height else sz.width)+config.nodeGap
                }
                main+=(if(horizontal(parent))layerSize.width else layerSize.height)
                if(index<layers.lastIndex)main+=config.nodeGap*kotlin.math.abs(layers[index+1].rank-layer.rank)
            }
            return result
        }
        val ids=children(parent).let { if(direction(parent) in listOf(FlowDirection.BT,FlowDirection.RL))it.reversed()else it };var cursor=if(horizontal(parent))x else y
        ids.forEachIndexed { index,id->val sz=size(id);val r=if(horizontal(parent))SceneRect(cursor,y+(content.height-sz.height)/2,sz.width,sz.height)else SceneRect(x+(content.width-sz.width)/2,cursor,sz.width,sz.height)
            result[id]=r
            cursor+=(if(horizontal(parent))sz.width else sz.height)+(if(index<ids.lastIndex)gap(id,ids[index+1])else 0.0)
        }
        return result
    }
    private data class Insets(val left:Double=0.0,val top:Double=0.0,val right:Double=0.0,val bottom:Double=0.0)
    private val insetCache=mutableMapOf<String?,Insets>()

    // Measure descendant geometry before reserving the current frame's gutters.
    // Child frames already include their own return routes and labels.
    private val compoundRouteCache=mutableMapOf<String,Map<Int,FlowReturnRoute>>()
    private fun compoundRoutes(parent:String,boxes:Map<String,SceneRect>):Map<Int,FlowReturnRoute> {
        val localBoxes=childRects(parent,0.0,0.0)
        val routes=compoundRouteCache.getOrPut(parent) { measureCompoundRoutes(parent,localBoxes) }
        val first=boxes.keys.firstOrNull() ?: return emptyMap()
        val dx=boxes.getValue(first).x-localBoxes.getValue(first).x
        val dy=boxes.getValue(first).y-localBoxes.getValue(first).y
        // Reuse the measured side/lane choice. Re-ranking equivalent candidates
        // after a translation can change a floating-point tie and escape gutters.
        fun move(p:ScenePoint)=ScenePoint(p.x+dx,p.y+dy)
        return routes.mapValues { (_,r) -> r.copy(points=r.points.map(::move),label=r.label?.let(::move),
            bounds=r.bounds.copy(x=r.bounds.x+dx,y=r.bounds.y+dy)) }
    }
    private fun measureCompoundRoutes(parent:String,boxes:Map<String,SceneRect>):Map<Int,FlowReturnRoute> {
        val nodes=linkedMapOf<String,SceneRect>()
        val frames=linkedMapOf<String,SceneRect>()
        fun collect(children:Map<String,SceneRect>) {
            children.forEach { (id,rect) ->
                if(id in groups) {
                    frames[id]=rect
                    val inset=insets(id)
                    collect(childRects(id,rect.x+(if(id in unframedGroups)0 else 16)+inset.left,
                        rect.y+headerHeight(id)+inset.top))
                }else nodes[id]=rect
            }
        }
        collect(boxes)
        val local=diagram.copy(direction=direction(parent),nodes=diagram.nodes.filter { it.id in nodes },
            subgraphs=diagram.subgraphs.filter { it.id in frames }.map { group ->
                group.copy(parentId=parents[group.id]?.takeIf { it in frames })
            })
        return flowCompoundRoutes(local,nodes,frames,emptyMap(),measurer,routeRootEdges=true)
    }

    private fun nestedRoutes(parent:String?,boxes:Map<String,SceneRect>):Map<Int,FlowReturnRoute> {
        if(!rankByEdges || parent==null || parent in unframedGroups)return emptyMap()
        if(boxes.keys.any { it in groups })return compoundRoutes(parent,boxes)
        val edges=diagram.edges.withIndex().filter { it.value.sourceId in boxes && it.value.targetId in boxes }
        if(edges.isEmpty())return emptyMap()
        val local=diagram.copy(direction=direction(parent),nodes=diagram.nodes.filter { it.id in boxes },
            edges=edges.map { it.value },subgraphs=emptyList())
        return flowReturnRoutes(local,boxes,measurer,config).mapKeys { edges[it.key].index }
    }

    private fun insets(parent:String?):Insets = insetCache.getOrPut(parent) {
        if(!rankByEdges || parent==null || parent in unframedGroups)return@getOrPut Insets()
        val raw=rawMeasure(parent)
        val routes=nestedRoutes(parent,childRects(parent,0.0,0.0)).values
        if(routes.isEmpty())return@getOrPut Insets()
        Insets(maxOf(0.0,-routes.minOf { it.bounds.x }),maxOf(0.0,-routes.minOf { it.bounds.y }),
            maxOf(0.0,routes.maxOf { it.bounds.x+it.bounds.width }-raw.width),
            maxOf(0.0,routes.maxOf { it.bounds.y+it.bounds.height }-raw.height))
    }

    private fun measure(parent:String?):SceneSize {
        val raw=rawMeasure(parent);val inset=insets(parent)
        return SceneSize(raw.width+inset.left+inset.right,raw.height+inset.top+inset.bottom)
    }

    private fun locate(parent:String?,x:Double,y:Double) {
        val inset=insets(parent)
        val children=childRects(parent,x+inset.left,y+inset.top)
        returnRoutes.putAll(nestedRoutes(parent,children))
        children.forEach { (id,r) ->
            if(id in groups) {
                groupRects[id]=r
                locate(id,r.x+(if(id in unframedGroups)0 else 16),r.y+headerHeight(id))
            } else rects[id]=r
        }
    }
    fun place():Result {val content=measure(null);locate(null,config.padding,config.padding);return Result(content.width+2*config.padding,content.height+2*config.padding,rects,groupRects,returnRoutes)}
}
