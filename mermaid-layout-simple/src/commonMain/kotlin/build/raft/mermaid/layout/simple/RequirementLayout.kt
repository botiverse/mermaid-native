package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.max

internal fun layoutRequirementGraph(diagram:RequirementDiagram,measurer:TextMeasurer,config:LayoutConfig):LayoutScene {
    data class Card(val id:String,val heading:String,val lines:List<String>,val element:Boolean,val classes:List<String>,val styles:List<String>)
    val cards=diagram.requirements.map { r->Card(r.name,r.type.name.lowercase().replace('_',' ')+" ${r.name}",buildList {
        if(r.id.isNotEmpty())add("id: ${r.id}")
        if(r.text.isNotEmpty())addAll(("text: "+r.text).lines())
        if(r.risk!=RequirementRisk.UNSPECIFIED)add("risk: ${r.risk.name.lowercase()}")
        if(r.verifyMethod!=RequirementVerifyMethod.UNSPECIFIED)add("verify: ${r.verifyMethod.name.lowercase()}")
    },false,r.classes,r.styles) }.toMutableList()
    cards+=diagram.elements.map { e->Card(e.name,"element ${e.name}",buildList {if(e.type.isNotEmpty())addAll(("type: "+e.type).lines());if(e.docRef.isNotEmpty())addAll(("docref: "+e.docRef).lines())},true,e.classes,e.styles) }
    val missing=diagram.relationships.flatMap { listOf(it.from,it.to) }.distinct().filter { id->cards.none{it.id==id} }
    cards+=missing.map { Card(it,it,emptyList(),false,emptyList(),emptyList()) }
    if(diagram.direction!=null){
        val pending=cards.toMutableList();val ordered=mutableListOf<Card>()
        while(pending.isNotEmpty()){
            val candidate=pending.firstOrNull { card->diagram.relationships.none { e->e.to==card.id&&e.from!=card.id&&pending.any { it.id==e.from } } } ?: pending.first()
            ordered+=candidate;pending.remove(candidate)
        }
        cards.clear();cards+=ordered
    }
    val nodes=cards.map { FlowNode(it.id,it.heading,classes=it.classes.filterNot { c->c=="default" },styles=it.styles) }
    val flow=FlowchartDiagram(diagram.direction?:FlowDirection.TB,nodes,diagram.relationships.map { FlowEdge(it.from,it.to) },classDefinitions=diagram.classDefinitions)
    val paints=cards.associate { c->c.id to FlowStyle(nodes.first { it.id==c.id },flow,listOf("fill:"+if(c.element)DiagramPalette.BLUE_SURFACE else DiagramPalette.CANVAS,"stroke:${DiagramPalette.SECONDARY}","stroke-width:1.5","font-size:12px")) }
    val headings=cards.associate { c->c.id to paints.getValue(c.id).text.copy(fontSize=paints.getValue(c.id).text.fontSize+2,fontWeight=600) }
    val widths=cards.associate { c->c.id to max(270.0,max(measurer.measure(c.heading,headings.getValue(c.id)).width,c.lines.maxOfOrNull { measurer.measure(it,paints.getValue(c.id).text).width }?:0.0)+24) }
    val uniformWidth=widths.values.maxOrNull()?:270.0
    val sizes=cards.associate { c->val h=headings.getValue(c.id).fontSize;val p=paints.getValue(c.id);c.id to SceneSize(uniformWidth,max(if(c.element)96.0 else 132.0,h+28+c.lines.size*max(18.0,p.text.fontSize+6))) }
    val accessible=listOfNotNull(diagram.accessibilityTitle?.let{"accTitle: $it"},diagram.accessibilityDescription?.let{"accDescr: $it"}).flatMap(String::lines)
    val offset=accessible.size*18.0+if(accessible.isEmpty())0.0 else 12.0
    val rects=if(diagram.direction!=null){FlowPlacement(flow,sizes,config,measurer).place().nodes.mapValues { (_,r)->r.copy(y=r.y+offset) }.toMutableMap()}else{
        val result=linkedMapOf<String,SceneRect>();var left=config.padding+offset;var right=left;val leftWidth=uniformWidth
        for(c in cards){val size=sizes.getValue(c.id);val y=if(c.element)right else left;result[c.id]=SceneRect(config.padding+if(c.element)leftWidth+150 else 0.0,y,size.width,size.height);if(c.element)right+=size.height+28 else left+=size.height+28};result
    }
    val commands=mutableListOf<DrawCommand>();var right=rects.values.maxOfOrNull { it.x+it.width }?:config.padding
    val body=TextStyle(fontSize=12.0)
    accessible.forEachIndexed { i,text->commands+=DrawText(text,ScenePoint(config.padding,config.padding+14+i*18),style=body);right=max(right,config.padding+measurer.measure(text,body).width) }
    for(e in diagram.relationships){val a=rects.getValue(e.from);val b=rects.getValue(e.to);val label=e.kind.name.lowercase();val horizontal=diagram.direction in listOf(FlowDirection.LR,FlowDirection.RL) || (diagram.direction==null&&a.x!=b.x)
        val start:ScenePoint;val end:ScenePoint;val labelAt:ScenePoint;val arrow:ScenePoint
        if(e.from==e.to || (diagram.direction==null&&a.x==b.x)){
            start=ScenePoint(a.x+a.width,a.y+a.height*0.35);end=ScenePoint(b.x+b.width,if(e.from==e.to)b.y+b.height*0.75 else b.y+b.height/2);val outer=max(start.x,end.x)+36
            commands+=DrawPolyline(listOf(start,ScenePoint(outer,start.y),ScenePoint(outer,end.y),end));labelAt=ScenePoint(outer+8,(start.y+end.y)/2);arrow=ScenePoint(1.0,0.0);right=max(right,labelAt.x+measurer.measure(label,body).width)
        }else if(horizontal){val sign=if(b.x>a.x)1.0 else -1.0;start=ScenePoint(if(sign>0)a.x+a.width else a.x,a.y+a.height/2);end=ScenePoint(if(sign>0)b.x else b.x+b.width,b.y+b.height/2);commands+=DrawLine(start,end);labelAt=ScenePoint((start.x+end.x)/2,(start.y+end.y)/2-8);arrow=ScenePoint(-sign,0.0)
        }else{val sign=if(b.y>a.y)1.0 else -1.0;start=ScenePoint(a.x+a.width/2,if(sign>0)a.y+a.height else a.y);end=ScenePoint(b.x+b.width/2,if(sign>0)b.y else b.y+b.height);commands+=DrawLine(start,end);labelAt=ScenePoint((start.x+end.x)/2+8,(start.y+end.y)/2);arrow=ScenePoint(0.0,-sign);right=max(right,labelAt.x+measurer.measure(label,body).width)}
        commands+=DrawPolygon(listOf(end,ScenePoint(end.x+arrow.x*9+arrow.y*5,end.y+arrow.y*9-arrow.x*5),ScenePoint(end.x+arrow.x*9-arrow.y*5,end.y+arrow.y*9+arrow.x*5)),fill=SceneColor(DiagramPalette.INK))
        commands+=DrawText(label,labelAt,if(horizontal&&e.from!=e.to)TextAnchor.MIDDLE else TextAnchor.START,body)
    }
    for(c in cards){val r=rects.getValue(c.id);val paint=paints.getValue(c.id);val heading=headings.getValue(c.id);val separator=r.y+heading.fontSize+18
        commands+=paint.paint(DrawRect(r,cornerRadius=4.0));commands+=DrawText(c.heading,ScenePoint(r.x+12,r.y+heading.fontSize+8),style=heading)
        if(c.lines.isNotEmpty()){val line=DrawLine(ScenePoint(r.x,separator),ScenePoint(r.x+r.width,separator));commands+=if(c.styles.isEmpty()&&c.classes.all{it=="default"}&&diagram.classDefinitions["default"].isNullOrEmpty())listOf(line)else paint.paint(line);c.lines.forEachIndexed { i,text->commands+=DrawText(text,ScenePoint(r.x+12,separator+paint.text.fontSize+(if(c.element)10 else 8)+i*max(if(c.element)20.0 else 18.0,paint.text.fontSize+6)),style=paint.text) }}
    }
    return LayoutScene(max(1.0,right+config.padding),max(config.padding*2,(rects.values.maxOfOrNull { it.y+it.height }?:offset)+config.padding),commands)
}
