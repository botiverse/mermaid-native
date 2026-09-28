package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.max

internal class ClassPlacement(private val diagram:ClassDiagram,private val sizes:Map<String,SceneSize>,private val config:LayoutConfig,private val textMeasurer:TextMeasurer) {
    private data class Item(val id:String,val group:Boolean,val noteIndex:Int?=null)
    data class Result(val width:Double,val height:Double,val classes:Map<String,SceneRect>,val namespaces:Map<String,SceneRect>,val notes:Map<Int,SceneRect>)
    private val namespaceIds=diagram.namespaces.map { it.id }.toSet()
    private val groupSizes=mutableMapOf<String,SceneSize>()
    private val classRects=linkedMapOf<String,SceneRect>()
    private val noteRects=linkedMapOf<Int,SceneRect>()
    private val groupRects=linkedMapOf<String,SceneRect>()
    private val horizontal=diagram.direction==FlowDirection.LR || diagram.direction==FlowDirection.RL
    private val reverse=diagram.direction==FlowDirection.RL || diagram.direction==FlowDirection.BT
    private fun items(parent:String?):List<Item> = diagram.namespaces.filter { it.parentId?.takeIf { id -> id in namespaceIds }==parent }.map { Item(it.id,true) } +
        diagram.classes.filter { it.namespaceName?.takeIf { id -> id in namespaceIds }==parent }.map { Item(it.id,false) } +
        diagram.notes.withIndex().filter { it.value.namespaceName?.takeIf { id -> id in namespaceIds }==parent }.map { Item("",false,it.index) }
    private fun size(item:Item):SceneSize = if(item.group) groupSizes.getOrPut(item.id) {
        val content=measure(items(item.id)); val label=diagram.namespaces.first { it.id==item.id }.label
        SceneSize(max(80.0,max(content.width+32,textMeasurer.measure(label,TextStyle(fontWeight=600)).width+24)),max(50.0,content.height+48))
    } else if(item.noteIndex!=null) {
        val lines=classNoteLines(diagram.notes[item.noteIndex].text)
        SceneSize(max(80.0,lines.maxOf { textMeasurer.measure(it,TextStyle()).width }+24),max(40.0,lines.size*22.0+16))
    } else sizes.getValue(item.id)
    private fun measure(items:List<Item>):SceneSize {
        val values=items.map(::size);val gap=config.nodeGap*max(0,items.size-1)
        return if(horizontal) SceneSize(values.sumOf { it.width }+gap,values.maxOfOrNull { it.height } ?: 0.0)
        else SceneSize(values.maxOfOrNull { it.width } ?: 0.0,values.sumOf { it.height }+gap)
    }
    private fun locate(parent:String?,x:Double,y:Double) {
        var cursor=if(horizontal)x else y
        val children=items(parent).let { if(reverse) it.reversed() else it }
        children.forEach { item ->
            val size=size(item);val rect=SceneRect(if(horizontal)cursor else x,if(horizontal)y else cursor,size.width,size.height)
            if(item.group) { groupRects[item.id]=rect;locate(item.id,rect.x+16,rect.y+32) }
            else if(item.noteIndex!=null) noteRects[item.noteIndex]=rect
            else classRects[item.id]=rect
            cursor+=(if(horizontal)size.width else size.height)+config.nodeGap
        }
    }
    fun place():Result {
        val measured=measure(items(null));locate(null,config.padding,config.padding)
        return Result(measured.width+config.padding*2,measured.height+config.padding*2,classRects,groupRects,noteRects)
    }
}

internal fun classNoteLines(text:String):List<String> = MermaidText.splitBreaks(text.replace("\\n","\n")).flatMap { it.split('\n') }
