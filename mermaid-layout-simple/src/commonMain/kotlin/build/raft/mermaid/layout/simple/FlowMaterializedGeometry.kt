package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.abs

internal data class FlowMaterializedResult(
    val paths: Map<Int,List<ScenePoint>>,
    val groups: Map<String,SceneRect>,
)

/** Admit atomic materialized passes only where this renderer provides the matching geometry.
 * Labeled edges retain their existing routes until label relocation participates in the pass.
 * All group titles in this renderer are horizontal, including LR diagrams.
 */
internal fun materializeFlowGeometry(
    diagram: FlowchartDiagram, rects: Map<String,SceneRect>, groups: Map<String,SceneRect>,
    paths: Map<Int,List<ScenePoint>>, measurer: TextMeasurer,
): FlowMaterializedResult {
    val original=FlowMaterializedResult(paths,groups)
    val rectangular=diagram.nodes.filter { it.shape==FlowNodeShape.RECTANGLE }.map { it.id }.toSet()
    if(diagram.edges.any { !it.label.isNullOrEmpty() } || paths.any { (i,p) ->
        val e=diagram.edges[i]
        e.sourceId !in rectangular || e.targetId !in rectangular || p.size<2 ||
            p.any { !it.x.isFinite() || !it.y.isFinite() } ||
            p.zipWithNext().any { (a,b) -> !aligned(a,b) }
    })return original
    val indices=paths.keys.toList()
    val edges=indices.map { i -> val e=diagram.edges[i]
        MaterializedGeometry.Edge("edge-$i",e.sourceId,e.targetId,paths.getValue(i),e.style==FlowEdgeStyle.INVISIBLE)
    }
    val nodes=rects.mapValues { (id,r) -> MaterializedGeometry.Node(id,r.x+r.width/2,r.y+r.height/2,r.width,r.height) }.toMutableMap()
    for(g in diagram.subgraphs) {
        val r=groups[g.id]?:continue;val style=flowGroupStyle(g,diagram).text
        val size=measurer.measure(g.label,style);val baseline=r.y+6+style.fontSize
        nodes[g.id]=MaterializedGeometry.Node(g.id,r.x+r.width/2,r.y+r.height/2,r.width,r.height,
            isGroup=true,parentId=g.parentId,direction="TD",
            groupTitleRect=MaterializedGeometry.Bounds(r.x,r.x+r.width,baseline-size.height,baseline))
    }
    val obstacles=OrthogonalGeometry.collectRealNodeBounds(rects.map { (id,r) -> OrthogonalGeometry.NodeBounds(id,r) })
    fun onBorder(p:ScenePoint,r:SceneRect)=p.x>=r.x-M_EPS && p.x<=r.x+r.width+M_EPS && p.y>=r.y-M_EPS && p.y<=r.y+r.height+M_EPS &&
        (abs(p.x-r.x)<M_EPS || abs(p.x-r.x-r.width)<M_EPS || abs(p.y-r.y)<M_EPS || abs(p.y-r.y-r.height)<M_EPS)
    fun safe(e:MaterializedGeometry.Edge):Boolean {
        val p=e.points?:return false;val a=rects[e.start]?:return false;val b=rects[e.end]?:return false
        if(p.size<2 || !onBorder(p.first(),a) || !onBorder(p.last(),b))return false
        fun length(a:ScenePoint,b:ScenePoint)=abs(a.x-b.x)+abs(a.y-b.y)
        if(length(p[0],p[1])<8-M_EPS || length(p[p.lastIndex-1],p.last())<8-M_EPS)return false
        return p.all { it.x.isFinite() && it.y.isFinite() } && p.zipWithNext().all { (a,b) ->
            aligned(a,b) && !OrthogonalGeometry.segmentHitsAnyRect(a,b,obstacles,emptyList(),M_EPS)
        }
    }
    fun crossings(es:List<MaterializedGeometry.Edge>)=MaterializedState(es,emptyMap()).crossings()
    fun area(a:MaterializedGeometry.Bounds?,b:MaterializedGeometry.Bounds?):Double =
        if(a==null || b==null)0.0 else materializedOverlap(a.left,a.right,b.left,b.right)*materializedOverlap(a.top,a.bottom,b.top,b.bottom)
    fun related(first:String,second:String):Boolean {
        var next:String?=first;val seen=mutableSetOf<String>()
        while(next!=null && seen.add(next)) {
            if(next==second)return true
            next=nodes[next]?.parentId
        }
        return false
    }
    var result=MaterializedGeometry.Result(edges,nodes)
    fun safeTitles(candidate:MaterializedGeometry.Result):Boolean {
        for((id,n) in candidate.nodes) {
            val previous=result.nodes.getValue(id)
            if(n==previous)continue
            val title=n.groupTitleRect?:continue
            for((otherId,other) in candidate.nodes) {
                if(otherId==id)continue
                val oldOther=result.nodes.getValue(otherId)
                if(!other.isGroup && area(title,other.bounds())>area(previous.groupTitleRect,oldOther.bounds())+M_EPS)return false
                if(other.isGroup) {
                    if(area(title,other.groupTitleRect)>area(previous.groupTitleRect,oldOther.groupTitleRect)+M_EPS)return false
                    if(!related(id,otherId) && !related(otherId,id) &&
                        area(n.bounds(),other.bounds())>area(previous.bounds(),oldOther.bounds())+M_EPS)return false
                }
            }
        }
        return true
    }
    fun diagnose(value:MaterializedGeometry.Result):LayoutValidationResult {
        val boxes=groups.mapValues { (id,r) -> value.nodes[id]?.let { n ->
            SceneRect(n.x!!-n.width!!/2,n.y!!-n.height!!/2,n.width,n.height)
        }?:r }
        val titles=diagram.subgraphs.mapNotNull { group -> boxes[group.id]?.let { box ->
            val style=flowGroupStyle(group,diagram).text
            group.id to DrawText(group.label,ScenePoint(box.x+box.width/2,box.y+6+style.fontSize),TextAnchor.MIDDLE,style)
        } }.toMap()
        val edgePaths=indices.mapIndexed { i,index -> index to value.edges[i].points!! }.toMap()
        return flowLayoutValidation(diagram,rects,boxes,edgePaths,emptyMap(),titles,measurer,0.0,0.0).result
    }
    fun accept(operation:MaterializedGeometry.Operation) {
        val candidate=MaterializedGeometry.apply(operation,result.edges,result.nodes)
        if(candidate==result)return
        if(candidate.edges.indices.any { candidate.edges[it]!=result.edges[it] && !safe(candidate.edges[it]) })return
        if(crossings(candidate.edges)>crossings(result.edges) || !safeTitles(candidate))return
        result=candidate
    }
    listOf(MaterializedGeometry.Operation.SEPARATE_TERMINAL_LANES,MaterializedGeometry.Operation.COLLAPSE_DOGLEGS,
        MaterializedGeometry.Operation.LIFT_OBSTACLE_RAILS,MaterializedGeometry.Operation.SWAP_DESTINATION_TAILS,
        MaterializedGeometry.Operation.RESOLVE_CROSSINGS,MaterializedGeometry.Operation.REASSIGN_EXTERNAL_CHANNELS,
        MaterializedGeometry.Operation.SHORTCUT_JOGS).forEach(::accept)
    // Title movement is terminal: painting and validation derive the same translated baseline.
    accept(MaterializedGeometry.Operation.LIFT_TOP_TITLES)
    if(result==MaterializedGeometry.Result(edges,nodes))return original
    // The oracle counts strict H/V crossings; rendered diagnostics also count T-junctions
    // and parallel clearance. Judge the complete transaction after its title movement,
    // because lifting a rail and moving the title away from it must be accepted together.
    val before=diagnose(MaterializedGeometry.Result(edges,nodes))
    val after=diagnose(result)
    if(after.breakdown.crossings>before.breakdown.crossings)return original
    val oldIssues=before.issues.groupingBy { it.type }.eachCount()
    if(after.issues.groupingBy { it.type }.eachCount().any { (type,count) -> count>(oldIssues[type]?:0) })return original
    val finalGroups=groups.mapValues { (id,r) -> result.nodes[id]?.let { n ->
        SceneRect(n.x!!-n.width!!/2,n.y!!-n.height!!/2,n.width,n.height)
    }?:r }
    return FlowMaterializedResult(indices.mapIndexed { i,index -> index to result.edges[i].points!! }.toMap(),finalGroups)
}
