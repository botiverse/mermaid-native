package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*

/** Preserve the full Flow grammar while assigning every visible node to a lane. */
internal fun swimlaneFlow(diagram: SwimlaneDiagram): FlowchartDiagram {
    val parsed=diagram.flowchart ?: FlowchartDiagram(diagram.direction,
        diagram.lanes.flatMap { lane -> lane.nodes.map { n -> FlowNode(n.id,n.label,when(n.shape) {
            SwimlaneNodeShape.RECTANGLE->FlowNodeShape.RECTANGLE
            SwimlaneNodeShape.ROUNDED->FlowNodeShape.ROUNDED
            SwimlaneNodeShape.STADIUM->FlowNodeShape.STADIUM
            SwimlaneNodeShape.DECISION->FlowNodeShape.DIAMOND
            SwimlaneNodeShape.CIRCLE->FlowNodeShape.CIRCLE
        }) } },diagram.edges.map { FlowEdge(it.sourceId,it.targetId,label=it.label) },
        diagram.lanes.map { FlowSubgraph(it.id,it.label,it.nodes.map { n->n.id }) })
    val visible=visibleFlow(parsed)
    val unowned=visible.nodes.filter { n -> visible.subgraphs.none { n.id in it.nodeIds } }
    var result=visible
    if(unowned.isNotEmpty()) {
        var id="__swimlane_default__"
        val used=(visible.subgraphs.map { it.id }+visible.nodes.map { it.id }).toSet()
        while(id in used)id+="_"
        result=result.copy(subgraphs=result.subgraphs+FlowSubgraph(id,"",unowned.map { it.id }))
    }
    return result
}

internal data class SwimlanePlacement(val placement:FlowPlacement.Result,val titles:Map<String,SceneRect>)

internal fun placeSwimlanes(diagram:FlowchartDiagram,sizes:Map<String,SceneSize>,config:LayoutConfig,measurer:TextMeasurer):SwimlanePlacement {
    // Local direction declarations are expressed in final coordinates. Undo the
    // outer transform before canonical placement so an explicit LR stays LR.
    fun canonicalDirection(direction: FlowDirection): FlowDirection = when (diagram.direction) {
        FlowDirection.TB, FlowDirection.TD -> direction
        FlowDirection.BT -> when (direction) { FlowDirection.TB, FlowDirection.TD -> FlowDirection.BT; FlowDirection.BT -> FlowDirection.TB; else -> direction }
        FlowDirection.LR -> when (direction) { FlowDirection.LR -> FlowDirection.TB; FlowDirection.RL -> FlowDirection.BT; FlowDirection.TB, FlowDirection.TD -> FlowDirection.LR; FlowDirection.BT -> FlowDirection.RL }
        FlowDirection.RL -> when (direction) { FlowDirection.LR -> FlowDirection.BT; FlowDirection.RL -> FlowDirection.TB; FlowDirection.TB, FlowDirection.TD -> FlowDirection.LR; FlowDirection.BT -> FlowDirection.RL }
    }
    val canonical=diagram.copy(direction=FlowDirection.TB,subgraphs=diagram.subgraphs.map { it.copy(direction=it.direction?.let(::canonicalDirection)) })
    val base=FlowPlacement(canonical,sizes,config,measurer,rankByEdges=true,swimlaneRoots=true).place()
    val owners=diagram.nodes.associate { n -> n.id to diagram.subgraphs.firstOrNull { n.id in it.nodeIds }?.id }
    val rootIds=diagram.subgraphs.filter { it.parentId==null }.map { it.id }.toSet()
    val top=base.groups.filterKeys { it in rootIds }.values.minOfOrNull { it.y } ?: config.padding
    val bottom=base.groups.filterKeys { it in rootIds }.values.maxOfOrNull { it.y+it.height } ?: top
    val geometry=base.nodes.map { (id,r)->SwimlaneDirectionGeometry.Node(id,r.x+r.width/2,r.y+r.height/2,r.width,r.height,parentId=owners[id]) }+
        diagram.subgraphs.mapNotNull { g -> base.groups[g.id]?.let { old ->
            val r=if(g.id in rootIds)old.copy(y=top,height=bottom-top)else old
            SwimlaneDirectionGeometry.Node(g.id,r.x+r.width/2,r.y+r.height/2,r.width,r.height,true,g.parentId,if(g.id in rootIds)20.0 else maxOf(64.0,measurer.measure(g.label,flowGroupStyle(g,diagram).text).height*2+24),
                SceneRect(r.x,r.y,r.width,if(g.id in rootIds)21.0 else 28.0))
        } }
    val titleWidth=diagram.subgraphs.filter { it.id in rootIds }.maxOfOrNull { measurer.measure(it.label,flowGroupStyle(it,diagram).text).width+16.0 } ?: 36.0
    val result=SwimlaneDirectionGeometry.transform(geometry,emptyList(),diagram.direction,titleBandSize=maxOf(36.0,titleWidth),includeNestedBounds=true)
    val finalGeometry = reserveSwimlaneFrames(result.nodes, diagram, config, measurer = measurer)
    val nodes=finalGeometry.filterNot { it.isGroup }.associate { n -> n.id to SceneRect(n.x!!-n.width/2,n.y!!-n.height/2,n.width,n.height) }
    val groups=finalGeometry.filter { it.isGroup }.associate { n -> n.id to SceneRect(n.x!!-n.width/2,n.y!!-n.height/2,n.width,n.height) }
    val titles=finalGeometry.mapNotNull { n->n.title?.let { title ->
        n.id to title
    } }.toMap()
    val all=nodes.values+groups.values
    val dx=config.padding-(all.minOfOrNull { it.x } ?: config.padding)
    val dy=config.padding-(all.minOfOrNull { it.y } ?: config.padding)
    fun move(r:SceneRect)=r.copy(x=r.x+dx,y=r.y+dy)
    // Routing is recomputed from final upright nodes; canonical routes are never reused.
    return SwimlanePlacement(FlowPlacement.Result((all.maxOfOrNull { it.x+it.width } ?: 0.0)+dx+config.padding,
        (all.maxOfOrNull { it.y+it.height } ?: 0.0)+dy+config.padding,nodes.mapValues { move(it.value) },groups.mapValues { move(it.value) }),titles.mapValues { move(it.value) })
}

/** Native text is upright, so transformed clusters need a measured title band again.
 * Expand children first, then separate siblings and align the outer lane strips.
 * This also covers empty lanes and highly unequal node dimensions.
 */
private fun reserveSwimlaneFrames(
    transformed: List<SwimlaneDirectionGeometry.Node>, diagram: FlowchartDiagram,
    config: LayoutConfig, measurer: TextMeasurer,
): List<SwimlaneDirectionGeometry.Node> {
    val nodes = transformed.associateBy { it.id }.toMutableMap()
    val definitions = diagram.subgraphs.associateBy { it.id }
    val horizontal = diagram.direction in listOf(FlowDirection.LR, FlowDirection.RL)
    fun rect(n: SwimlaneDirectionGeometry.Node) = SceneRect(n.x!! - n.width / 2, n.y!! - n.height / 2, n.width, n.height)
    fun children(id: String) = nodes.values.filter { it.parentId == id }
    fun shift(id: String, dx: Double, dy: Double) {
        val pending = mutableListOf(id)
        var i = 0
        while (i < pending.size) {
            val current = nodes.getValue(pending[i++])
            pending += children(current.id).map { it.id }
            nodes[current.id] = current.copy(x = current.x!! + dx, y = current.y!! + dy,
                title = current.title?.let { it.copy(x = it.x + dx, y = it.y + dy) })
        }
    }
    fun bounds(list: List<SwimlaneDirectionGeometry.Node>): SceneRect {
        val boxes = list.map(::rect)
        val left = boxes.minOf { it.x }; val top = boxes.minOf { it.y }
        return SceneRect(left, top, boxes.maxOf { it.x + it.width } - left, boxes.maxOf { it.y + it.height } - top)
    }
    fun separate(ids: List<String>, alongX: Boolean) {
        val ordered = ids.sortedBy { rect(nodes.getValue(it)).let { r -> if (alongX) r.x else r.y } }
        for (i in ordered.indices) {
            val b = rect(nodes.getValue(ordered[i]))
            var displacement = 0.0
            for (prior in ordered.take(i)) {
                val a = rect(nodes.getValue(prior))
                val crossOverlap = if (alongX) minOf(a.y+a.height,b.y+b.height)>maxOf(a.y,b.y)
                    else minOf(a.x+a.width,b.x+b.width)>maxOf(a.x,b.x)
                if (crossOverlap) displacement = maxOf(displacement,
                    if (alongX) a.x+a.width+config.nodeGap-b.x else a.y+a.height+config.nodeGap-b.y)
            }
            if (displacement > 0) shift(ordered[i], if (alongX) displacement else 0.0, if (alongX) 0.0 else displacement)
        }
    }
    fun depth(n: SwimlaneDirectionGeometry.Node): Int {
        var d = 0; var parent = n.parentId
        while (parent != null) { d++; parent = nodes[parent]?.parentId }
        return d
    }
    val groups = transformed.filter { it.isGroup }.sortedByDescending(::depth)
    for (group in groups) {
        val childIds = children(group.id).map { it.id }
        val localDirection = definitions.getValue(group.id).direction ?: diagram.direction
        separate(childIds, localDirection in listOf(FlowDirection.LR, FlowDirection.RL))
        val titleSize = measurer.measure(definitions.getValue(group.id).label, flowGroupStyle(definitions.getValue(group.id), diagram).text)
        val header = maxOf(28.0, titleSize.height + 12.0)
        val b = if (childIds.isEmpty()) rect(nodes.getValue(group.id)).let { SceneRect(it.x+16,it.y+header,0.0,0.0) }
            else bounds(children(group.id))
        if (group.parentId != null) {
            val width = maxOf(b.width+32.0,titleSize.width+24.0)
            val height = maxOf(52.0,b.height+header+16.0)
            val frame = SceneRect(b.x+(b.width-width)/2,b.y-header,width,height)
            nodes[group.id] = group.copy(x=frame.x+width/2,y=frame.y+height/2,width=width,height=height,
                title=SceneRect(frame.x,frame.y,width,header))
        } else {
            // Outer lane bounds are aligned after every subtree is complete.
            val width = maxOf(b.width+32.0, if(horizontal)80.0 else titleSize.width+24.0)
            val height = maxOf(b.height+32.0,52.0)
            nodes[group.id] = group.copy(x=b.x+b.width/2,y=b.y+b.height/2,width=width,height=height,title=null)
        }
    }
    val roots = groups.filter { it.parentId == null }.map { it.id }
    if (roots.isEmpty()) return transformed
    separate(roots, !horizontal)
    val all = bounds(roots.map { nodes.getValue(it) })
    val titleBand = if(horizontal) maxOf(36.0,roots.maxOf { measurer.measure(definitions.getValue(it).label,flowGroupStyle(definitions.getValue(it),diagram).text).width+16 })
        else maxOf(28.0,roots.maxOf { measurer.measure(definitions.getValue(it).label,flowGroupStyle(definitions.getValue(it),diagram).text).height+12 })
    for (id in roots) {
        val n=nodes.getValue(id);val r=rect(n)
        val frame=if(horizontal)SceneRect(all.x-(if(diagram.direction==FlowDirection.LR)titleBand else 0.0),r.y,all.width+titleBand,r.height)
            else SceneRect(r.x,all.y-(if(diagram.direction==FlowDirection.BT)0.0 else titleBand),r.width,all.height+titleBand)
        val title=if(horizontal)SceneRect(if(diagram.direction==FlowDirection.RL)frame.x+frame.width-titleBand else frame.x,frame.y,titleBand,frame.height)
            else SceneRect(frame.x,if(diagram.direction==FlowDirection.BT)frame.y+frame.height-titleBand else frame.y,frame.width,titleBand)
        nodes[id]=n.copy(x=frame.x+frame.width/2,y=frame.y+frame.height/2,width=frame.width,height=frame.height,title=title)
    }
    return transformed.map { nodes.getValue(it.id) }
}

/** Return tracks belong to one lane/container; a cross-lane handoff is not a
 * backwards edge merely because its target happens to have a lower rank. */
internal fun swimlaneReturnRoutes(diagram: FlowchartDiagram, nodes: Map<String,SceneRect>, measurer: TextMeasurer, config: LayoutConfig): Map<Int,FlowReturnRoute> {
    val owners=diagram.nodes.associate { n -> n.id to diagram.subgraphs.firstOrNull { n.id in it.nodeIds }?.id }
    val result=mutableMapOf<Int,FlowReturnRoute>()
    for (owner in owners.values.toSet()) {
        val members=nodes.filterKeys { owners[it]==owner }
        val edges=diagram.edges.withIndex().filter { it.value.sourceId in members && it.value.targetId in members }
        val direction=diagram.subgraphs.firstOrNull { it.id==owner }?.direction ?: diagram.direction
        val local=diagram.copy(direction=direction,nodes=diagram.nodes.filter { it.id in members },subgraphs=emptyList(),edges=edges.map { it.value })
        result.putAll(flowReturnRoutes(local,members,measurer,config).mapKeys { edges[it.key].index })
    }
    return result
}

internal fun swimlaneCrossesLanes(diagram: FlowchartDiagram, source: String, target: String): Boolean {
    val groups=diagram.subgraphs.associateBy { it.id }
    fun root(id: String): String? {
        var owner=if(id in groups)id else diagram.subgraphs.firstOrNull { id in it.nodeIds }?.id
        val seen=mutableSetOf<String>()
        while(owner!=null && seen.add(owner)) owner=groups[owner]?.parentId ?: return owner
        return owner
    }
    return root(source)!=root(target)
}
