package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*

/** Validate the exact routed paths and measured text used by the draw commands. */
internal fun flowLayoutValidation(
    diagram: FlowchartDiagram,
    nodes: Map<String, SceneRect>,
    groups: Map<String, SceneRect>,
    paths: Map<Int, List<ScenePoint>>,
    labels: Map<Int, DrawText>,
    titles: Map<String, DrawText>,
    measurer: TextMeasurer,
    dx: Double,
    dy: Double,
): LayoutValidationReport {
    fun offset(r: SceneRect)=r.copy(x=r.x+dx,y=r.y+dy)
    // TextMeasurer supplies width/height, not glyph ascent/descent; the title
    // and label boxes conservatively end at the draw command's baseline.
    fun bounds(t: DrawText): SceneRect {
        val size=measurer.measure(t.text,t.style)
        return offset(SceneRect(t.origin.x-size.width/2,t.origin.y-size.height,size.width,size.height))
    }
    fun parent(id: String): String? {
        val candidates=diagram.subgraphs.filter { id in it.nodeIds }
        return candidates.firstOrNull { candidate->candidates.none { it.parentId==candidate.id } }?.id
    }
    fun marker(m: FlowMarker)=when(m){FlowMarker.NONE->"none";FlowMarker.POINT->"arrow_point";FlowMarker.CROSS->"arrow_cross";FlowMarker.CIRCLE->"arrow_circle"}
    val measured=LayoutValidationInput(
        diagram.nodes.mapNotNull { n->nodes[n.id]?.let { LayoutValidationNode(n.id,offset(it),parent(n.id)) } }+
            diagram.subgraphs.mapNotNull { g->groups[g.id]?.let { LayoutValidationNode(g.id,offset(it),g.parentId,isGroup=true,groupTitleBounds=titles[g.id]?.let(::bounds)) } },
        diagram.edges.mapIndexedNotNull { index,e->
            if(e.style==FlowEdgeStyle.INVISIBLE)return@mapIndexedNotNull null
            paths[index]?.let { points->LayoutValidationEdge("edge-$index",points.map { ScenePoint(it.x+dx,it.y+dy) },e.sourceId,e.targetId,
                label=e.label,labelBounds=labels[index]?.let(::bounds),arrowTypeStart=marker(e.fromMarker),arrowTypeEnd=marker(e.toMarker)) }
        },
    )
    return LayoutValidationReport(measured,OrthogonalLayoutValidator.validate(measured),LayoutQualityScorer.score(measured))
}
