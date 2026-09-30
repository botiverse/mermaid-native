package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.max

/** A deterministic hierarchy layout; graph crossing minimization remains separate. */
internal class FlowPlacement(private val diagram:FlowchartDiagram,private val nodeSizes:Map<String,SceneSize>,private val config:LayoutConfig,private val measurer:TextMeasurer, private val unframedGroups:Set<String> = emptySet(), private val rankByEdges:Boolean = false) {
    data class Result(val width:Double,val height:Double,val nodes:Map<String,SceneRect>,val groups:Map<String,SceneRect>)
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
    // Rank only acyclic sibling graphs. Containers retain their own direction and padding.
    private fun layers(parent:String?):List<Layer>? = layerCache.getOrPut(parent) {
        if(!rankByEdges)return@getOrPut null
        val ids=children(parent)
        fun sibling(endpoint:String):String? {
            var id=endpoint
            val seen=mutableSetOf<String>()
            while(id !in ids && seen.add(id))id=(if(id in groups)parents[id]else owners[id]) ?: return null
            return id.takeIf { it in ids }
        }
        val links=diagram.edges.mapNotNull { edge ->
            val from=sibling(edge.sourceId);val to=sibling(edge.targetId)
            if(from==null || to==null || from==to)null else Triple(from,to,edge.length.coerceIn(1,10))
        }
        if(links.isEmpty())return@getOrPut null
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
    private fun measure(parent:String?):SceneSize {
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
    private fun locate(parent:String?,x:Double,y:Double){
        val content=measure(parent)
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
                    if(id in groups){groupRects[id]=r;locate(id,r.x+if(id in unframedGroups)0 else 16,r.y+headerHeight(id))}else rects[id]=r
                    cross+=(if(horizontal(parent))sz.height else sz.width)+config.nodeGap
                }
                main+=(if(horizontal(parent))layerSize.width else layerSize.height)
                if(index<layers.lastIndex)main+=config.nodeGap*kotlin.math.abs(layers[index+1].rank-layer.rank)
            }
            return
        }
        val ids=children(parent).let { if(direction(parent) in listOf(FlowDirection.BT,FlowDirection.RL))it.reversed()else it };var cursor=if(horizontal(parent))x else y
        ids.forEachIndexed { index,id->val sz=size(id);val r=if(horizontal(parent))SceneRect(cursor,y+(content.height-sz.height)/2,sz.width,sz.height)else SceneRect(x+(content.width-sz.width)/2,cursor,sz.width,sz.height)
            if(id in groups){groupRects[id]=r;locate(id,r.x+if(id in unframedGroups)0 else 16,r.y+headerHeight(id))}else rects[id]=r
            cursor+=(if(horizontal(parent))sz.width else sz.height)+(if(index<ids.lastIndex)gap(id,ids[index+1])else 0.0)
        }
    }
    fun place():Result {val content=measure(null);locate(null,config.padding,config.padding);return Result(content.width+2*config.padding,content.height+2*config.padding,rects,groupRects)}
}
