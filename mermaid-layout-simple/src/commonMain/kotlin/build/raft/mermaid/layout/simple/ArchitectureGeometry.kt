package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*

internal data class ArchitectureGeometry(
    val points: Map<String, ScenePoint>, val groups: Map<String, SceneRect>,
    val width: Double, val height: Double,
)

/** Packs nested containers from the inside out; hints constrain the actual node centers. */
internal fun architectureGeometry(
    diagram: ArchitectureDiagram, nodeWidth: Double, columnWidth: Double,
    padding: Double, titleOffset: Double,
): ArchitectureGeometry {
    val parents = diagram.services.associate { it.id to it.groupId } + diagram.junctions.associate { it.id to it.groupId }
    val points = linkedMapOf<String, ScenePoint>()
    val rectangles = linkedMapOf<String, SceneRect>()
    fun place(parent: String?, x: Double, y: Double): Pair<Double, Double> {
        val ids = parents.filterValues { it == parent }.keys.toList()
        val rowParent = ids.associateWith { it }.toMutableMap()
        val colParent = ids.associateWith { it }.toMutableMap()
        fun root(map: Map<String, String>, id: String): String { var r=id;while(map.getValue(r)!=r)r=map.getValue(r);return r }
        for (hint in diagram.layoutHints.filter { it.members.all { id -> id in ids } }) {
            val map = if (hint.direction == "row") rowParent else colParent
            val anchor = root(map, hint.members.first())
            hint.members.drop(1).forEach { map[root(map,it)] = anchor }
        }
        val rows = ids.groupBy { root(rowParent, it) }.values.toList()
        val colors = mutableMapOf<String, Int>()
        for (id in ids) {
            val column = root(colParent,id)
            if (column in colors) continue
            val neighbors = rows.filter { row -> row.any { root(colParent,it)==column } }.flatten().map { root(colParent,it) }.toSet()-column
            val unavailable = neighbors.mapNotNull { colors[it] }.toSet()
            colors[column] = generateSequence(0) { it+1 }.first { it !in unavailable }
        }
        val header = if(parent==null)0.0 else 48.0
        val columns = (colors.values.maxOrNull() ?: -1)+1
        var ownWidth = if(ids.isEmpty())0.0 else maxOf(columnWidth, columns*(nodeWidth+32.0))
        rows.forEachIndexed { row, members -> members.forEach { id ->
            points[id] = ScenePoint(x+16+nodeWidth/2+colors.getValue(root(colParent,id))*(nodeWidth+32), y+header+38+row*120)
        } }
        if(ids.any { id -> diagram.junctions.any { it.id==id } }) {
            val placed=mutableSetOf<String>()
            fun offset(port: ArchitecturePort): ScenePoint = when(port) {
                ArchitecturePort.TOP -> ScenePoint(0.0,-120.0)
                ArchitecturePort.BOTTOM -> ScenePoint(0.0,120.0)
                ArchitecturePort.LEFT -> ScenePoint(-nodeWidth-32,0.0)
                ArchitecturePort.RIGHT -> ScenePoint(nodeWidth+32,0.0)
            }
            for(seed in ids) {
                if(!placed.add(seed))continue
                var changed=true
                while(changed) {
                    changed=false
                    for(edge in diagram.edges.filter { it.sourceId in ids && it.targetId in ids }) {
                        val sourceKnown=edge.sourceId in placed;val targetKnown=edge.targetId in placed
                        if(sourceKnown==targetKnown)continue
                        val anchor=points.getValue(if(sourceKnown)edge.sourceId else edge.targetId)
                        val delta=offset(if(sourceKnown)edge.sourcePort else edge.targetPort)
                        val next=if(sourceKnown)edge.targetId else edge.sourceId
                        points[next]=ScenePoint(anchor.x+delta.x,anchor.y+delta.y);placed+=next;changed=true
                    }
                }
            }
            for(hint in diagram.layoutHints.filter { it.members.all { id -> id in ids } }) {
                val left=hint.members.minOf { points.getValue(it).x };val top=hint.members.minOf { points.getValue(it).y }
                hint.members.forEachIndexed { index,id -> points[id]=if(hint.direction=="row")ScenePoint(left+index*(nodeWidth+32),top) else ScenePoint(left,top+index*120) }
            }
            val dx=x+16+nodeWidth/2-ids.minOf { points.getValue(it).x }
            val dy=y+header+38-ids.minOf { points.getValue(it).y }
            ids.forEach { id -> val p=points.getValue(id);points[id]=ScenePoint(p.x+dx,p.y+dy) }
            ownWidth=maxOf(ownWidth,ids.maxOf { points.getValue(it).x }-x+nodeWidth/2+16)
        }
        var cursor = x+if(ownWidth>0)ownWidth+24 else 16.0
        var contentHeight = if(ids.isEmpty())0.0 else ids.maxOf { points.getValue(it).y }-y-header+38+20
        for (child in diagram.groups.filter { it.parentId==parent }) {
            val size=place(child.id,cursor,y+header+12)
            cursor+=size.first+24
            contentHeight=maxOf(contentHeight,size.second+24)
        }
        val width=maxOf(columnWidth,cursor-x,ownWidth+32)
        val height=maxOf(140.0,header+contentHeight+24)
        if(parent!=null)rectangles[parent]=SceneRect(x,y,width,height)
        return width to height
    }
    val size=place(null,padding,padding+titleOffset)
    // Align whole child subtrees at their common container. Moving only the named
    // node stretches its old frame across its siblings and makes containers overlap.
    val groupParents = diagram.groups.associate { it.id to it.parentId }
    val allParents = parents + groupParents
    fun below(id: String, parent: String?): Boolean {
        if (parent == null) return true
        var current = allParents[id]
        while (current != null) {
            if (current == parent) return true
            current = groupParents[current]
        }
        return false
    }
    fun childAt(id: String, parent: String?): String {
        var child = id
        while (allParents[child] != parent) child = allParents.getValue(child)!!
        return child
    }
    fun bounds(id: String): SceneRect = rectangles[id] ?: points.getValue(id).let {
        SceneRect(it.x - nodeWidth / 2, it.y - 38, nodeWidth, 76.0)
    }
    fun move(id: String, dx: Double, dy: Double) {
        if (id in rectangles) {
            for (node in points.keys.toList()) if (below(node, id)) {
                val old = points.getValue(node); points[node] = ScenePoint(old.x + dx, old.y + dy)
            }
            for (group in rectangles.keys.toList()) if (group == id || below(group, id)) {
                val old = rectangles.getValue(group)
                rectangles[group] = old.copy(x = old.x + dx, y = old.y + dy)
            }
        } else {
            val old = points.getValue(id); points[id] = ScenePoint(old.x + dx, old.y + dy)
        }
    }
    fun intersects(a: SceneRect, b: SceneRect) =
        a.x < b.x + b.width && b.x < a.x + a.width && a.y < b.y + b.height && b.y < a.y + a.height
    val containers = diagram.groups.sortedByDescending { groupDepth(it.id, diagram.groups) }.map { it.id } + listOf<String?>(null)
    for (parent in containers) {
        val children = allParents.filterValues { it == parent }.keys.toList()
        for (hint in diagram.layoutHints) {
            val members = hint.members.filter { below(it, parent) }
            val blocks = members.groupBy { childAt(it, parent) }
            if (blocks.size < 2) continue
            val anchors = blocks.mapValues { (_, ids) -> points.getValue(ids.first()) }
            if (hint.direction == "row") {
                val axis = anchors.values.maxOf { it.y }
                var cursor = blocks.keys.minOf { bounds(it).x }
                for (id in blocks.keys) {
                    val box = bounds(id)
                    move(id, cursor - box.x, axis - anchors.getValue(id).y)
                    cursor += box.width + 40.0
                }
            } else {
                val axis = anchors.values.maxOf { it.x }
                var cursor = blocks.keys.minOf { bounds(it).y }
                for (id in blocks.keys) {
                    val box = bounds(id)
                    move(id, axis - anchors.getValue(id).x, cursor - box.y)
                    cursor += box.height + 40.0
                }
            }
        }
        // Expanded child groups reserve their actual width before siblings are fit.
        // This preserves row alignment while keeping unrelated containers separate.
        val placed = mutableListOf<String>()
        for (id in children) {
            val box = bounds(id)
            if (placed.any { intersects(bounds(it), box) }) {
                val right = placed.maxOf { bounds(it).x + bounds(it).width }
                move(id, right + 40.0 - box.x, 0.0)
            }
            placed += id
        }
        if (parent != null && children.isNotEmpty()) {
            val boxes = children.map(::bounds)
            val left = boxes.minOf { it.x } - 16.0
            val top = boxes.minOf { it.y } - 48.0
            val right = boxes.maxOf { it.x + it.width } + 16.0
            val bottom = boxes.maxOf { it.y + it.height } + 16.0
            rectangles[parent] = SceneRect(left, top, maxOf(columnWidth, right - left), maxOf(140.0, bottom - top))
        }
    }
    val shiftX=maxOf(0.0,padding-(rectangles.values.minOfOrNull { it.x }?:padding))
    val shiftY=maxOf(0.0,padding+titleOffset-(rectangles.values.minOfOrNull { it.y }?:padding+titleOffset))
    val shiftedPoints=points.mapValues { (_,p)->ScenePoint(p.x+shiftX,p.y+shiftY) }
    val shiftedRects=rectangles.mapValues { (_,r)->SceneRect(r.x+shiftX,r.y+shiftY,r.width,r.height) }
    return ArchitectureGeometry(shiftedPoints,shiftedRects,
        maxOf(720.0,size.first+padding*2+shiftX,(shiftedRects.values.maxOfOrNull { it.x+it.width }?:0.0)+padding,(shiftedPoints.values.maxOfOrNull { it.x+nodeWidth/2 }?:0.0)+padding),
        maxOf(420.0+titleOffset,size.second+padding*2+titleOffset+shiftY,(shiftedRects.values.maxOfOrNull { it.y+it.height }?:0.0)+padding,(shiftedPoints.values.maxOfOrNull { it.y+38 }?:0.0)+padding))
}

internal fun groupDepth(id: String, groups: List<ArchitectureGroup>): Int {
    val byId=groups.associateBy { it.id };var parent=byId[id]?.parentId;var depth=0
    while(parent!=null){depth++;parent=byId[parent]?.parentId}
    return depth
}
