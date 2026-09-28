package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.max

/** A deterministic hierarchy layout; graph crossing minimization remains separate. */
internal class FlowPlacement(private val diagram:FlowchartDiagram,private val nodeSizes:Map<String,SceneSize>,private val config:LayoutConfig,private val measurer:TextMeasurer) {
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
    private fun gap(from:String,to:String):Double = config.nodeGap*max(1,diagram.edges.filter { (it.sourceId==from && it.targetId==to) || (it.sourceId==to && it.targetId==from) }.maxOfOrNull { it.length.coerceAtMost(10) } ?: 1)
    private fun measure(parent:String?):SceneSize {
        val ids=children(parent);val values=ids.map(::size);val spacing=ids.zipWithNext().sumOf { gap(it.first,it.second) }
        return if(horizontal(parent))SceneSize(values.sumOf { it.width }+spacing,values.maxOfOrNull { it.height }?:0.0) else SceneSize(values.maxOfOrNull { it.width }?:0.0,values.sumOf { it.height }+spacing)
    }
    private fun size(id:String):SceneSize = nodeSizes[id] ?: sizes.getOrPut(id){val content=measure(id);SceneSize(max(content.width+32,measurer.measure(groups.getValue(id).label,TextStyle()).width+24),max(52.0,content.height+48))}
    private fun locate(parent:String?,x:Double,y:Double){
        val content=measure(parent);val ids=children(parent).let { if(direction(parent) in listOf(FlowDirection.BT,FlowDirection.RL))it.reversed()else it };var cursor=if(horizontal(parent))x else y
        ids.forEachIndexed { index,id->val sz=size(id);val r=if(horizontal(parent))SceneRect(cursor,y+(content.height-sz.height)/2,sz.width,sz.height)else SceneRect(x+(content.width-sz.width)/2,cursor,sz.width,sz.height)
            if(id in groups){groupRects[id]=r;locate(id,r.x+16,r.y+32)}else rects[id]=r
            cursor+=(if(horizontal(parent))sz.width else sz.height)+(if(index<ids.lastIndex)gap(id,ids[index+1])else 0.0)
        }
    }
    fun place():Result {val content=measure(null);locate(null,config.padding,config.padding);return Result(content.width+2*config.padding,content.height+2*config.padding,rects,groupRects)}
}
