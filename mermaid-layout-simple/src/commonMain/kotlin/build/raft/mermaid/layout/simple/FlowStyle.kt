package build.raft.mermaid.layout.simple
import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*

internal class FlowStyle(node:FlowNode,diagram:FlowchartDiagram,defaults:List<String> = listOf("fill:#eeeeee","stroke:#999999")) {
    private val raw=(listOf("default")+node.classes).flatMap { diagram.classDefinitions[it].orEmpty() }+node.styles
    private val normalized=raw.flatMap { item ->
        val key=item.substringBefore(':').trim();val value=item.substringAfter(':').trim()
        when(key){
            "background"->listOf("fill:$value")
            "border"->{val parts=value.split(Regex("\\s+"));if(parts.size==3)listOf("stroke-width:${parts[0]}","stroke:${parts[2]}")else emptyList()}
            "font-size"->listOf("font-size:"+if(value.endsWith('%'))((value.dropLast(1).toDoubleOrNull()?:100.0)*14/100).toString()else value)
            else->listOf(item)
        }
    }
    private val resolved=ClassStyle(ClassDefinition(node.id,styles=defaults+normalized),ClassDiagram(emptyList(),emptyList()))
    val text=resolved.text
    val lineHeight=resolved.lineHeight
    private val dashed=raw.lastOrNull { it.substringBefore(':').trim()=="stroke-dasharray" }?.substringAfter(':')?.trim()?.let { it!="0" && it!="none" }==true
    fun paint(command:DrawCommand):List<DrawCommand> = when(command){
        is DrawRect->{val rect=command.copy(fill=resolved.fill,stroke=if(command.stroke.value=="none" || dashed)SceneColor("none")else resolved.stroke,strokeWidth=resolved.strokeWidth)
            if(dashed && command.stroke.value!="none") {val r=command.rect;listOf(rect,DrawPolyline(listOf(ScenePoint(r.x,r.y),ScenePoint(r.x+r.width,r.y),ScenePoint(r.x+r.width,r.y+r.height),ScenePoint(r.x,r.y+r.height),ScenePoint(r.x,r.y)),stroke=resolved.stroke,strokeWidth=resolved.strokeWidth,pattern=StrokePattern.DASHED))}else listOf(rect)}
        is DrawEllipse->listOf(command.copy(fill=resolved.fill,stroke=resolved.stroke,strokeWidth=resolved.strokeWidth))
        is DrawPolygon->listOf(command.copy(fill=resolved.fill))
        is DrawLine->listOf(command.copy(stroke=resolved.stroke,strokeWidth=resolved.strokeWidth,pattern=if(dashed)StrokePattern.DASHED else command.pattern))
        is DrawPolyline->listOf(command.copy(stroke=resolved.stroke,strokeWidth=resolved.strokeWidth,pattern=if(dashed)StrokePattern.DASHED else command.pattern))
        else->listOf(command)
    }
}
internal fun flowEdgeStyle(edge:FlowEdge,diagram:FlowchartDiagram)=ClassStyle(ClassDefinition("edge",styles=listOf("stroke:#666666","stroke-width:"+if(edge.style==FlowEdgeStyle.THICK)"3"else"1.5")+flowEdgeStyles(edge,diagram)),ClassDiagram(emptyList(),emptyList()))

internal fun flowEdgeStyles(edge:FlowEdge,diagram:FlowchartDiagram)=diagram.defaultEdgeStyles+edge.classes.flatMap { diagram.classDefinitions[it].orEmpty() }+edge.styles
internal fun flowGroupStyle(group:FlowSubgraph,diagram:FlowchartDiagram)=ClassStyle(ClassDefinition(group.id,styles=listOf("fill:#f7f7f7","stroke:#aaaaaa","stroke-width:1")+group.classes.flatMap { diagram.classDefinitions[it].orEmpty() }),ClassDiagram(emptyList(),emptyList()))
