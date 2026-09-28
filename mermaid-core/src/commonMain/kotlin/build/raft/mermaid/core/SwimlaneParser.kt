package build.raft.mermaid.core

/** Swimlane uses the upstream Flowchart grammar; lane presentation remains a renderer concern. */
internal fun parseSwimlaneFlow(source:String):MermaidParseResult {
    val result=FlowParser(source).parse()
    if(result is MermaidParseResult.Failure)return result
    val flow=(result as MermaidParseResult.Success).diagram as FlowchartDiagram
    if(flow.nodes.isEmpty())return MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,"Swimlane requires a node",SourceLocation(1,1))))
    fun node(n:FlowNode)=SwimlaneNode(n.id,n.label,when(n.shape){FlowNodeShape.ROUNDED->SwimlaneNodeShape.ROUNDED;FlowNodeShape.STADIUM->SwimlaneNodeShape.STADIUM;FlowNodeShape.CIRCLE->SwimlaneNodeShape.CIRCLE;FlowNodeShape.DIAMOND->SwimlaneNodeShape.DECISION;else->SwimlaneNodeShape.RECTANGLE})
    val owners=flow.nodes.associate { n->n.id to flow.subgraphs.firstOrNull { n.id in it.nodeIds }?.id }
    val lanes=flow.subgraphs.map { g->Swimlane(g.id,g.label,flow.nodes.filter { owners[it.id]==g.id }.map(::node)) }.toMutableList()
    val outside=flow.nodes.filter { owners[it.id]==null }
    if(outside.isNotEmpty()) {
        var id="__default_lane";while(flow.subgraphs.any { it.id==id })id+="_"
        lanes+=Swimlane(id,"",outside.map(::node))
    }
    return MermaidParseResult.Success(SwimlaneDiagram(flow.direction,lanes,flow.edges.map { SwimlaneEdge(it.sourceId,it.targetId,it.label) },flow))
}
