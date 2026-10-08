package build.raft.mermaid.layout.simple

import kotlin.math.max

internal fun MaterializedState.collapseDoglegs() {
    fun without(points: List<MPoint>,i: Int): List<MPoint>? {
        if(i+4>=points.size)return null
        val a=points[i];val b=points[i+1];val c=points[i+2];val d=points[i+3];val e=points[i+4]
        val terminalV=horizontal(a,b)&&vertical(b,c)&&horizontal(c,d)&&vertical(d,e)&&mx(a,d)&&mx(a,e)&&mx(b,c)&&(b.x-a.x)*(d.x-c.x)<0
        val terminalH=vertical(a,b)&&horizontal(b,c)&&vertical(c,d)&&horizontal(d,e)&&my(a,d)&&my(a,e)&&my(b,c)&&(b.y-a.y)*(d.y-c.y)<0
        if(terminalV||terminalH)return dedupe(points.take(i+1)+e+points.drop(i+5))
        val f=points.getOrNull(i+5)?:return null
        val v=vertical(a,b)&&horizontal(b,c)&&vertical(c,d)&&horizontal(d,e)&&vertical(e,f)&&mx(a,e)&&mx(a,f)&&mx(c,d)&&(c.x-b.x)*(e.x-d.x)<0
        val h=horizontal(a,b)&&vertical(b,c)&&horizontal(c,d)&&vertical(d,e)&&horizontal(e,f)&&my(a,e)&&my(a,f)&&my(c,d)&&(c.y-b.y)*(e.y-d.y)<0
        return if(v||h)dedupe(points.take(i+1)+f+points.drop(i+6))else null
    }
    repeat(8) {
        var fixed=false
        outer@ for(edge in visible) {
            val p=points(edge)
            for(i in 0..p.size-5) {
                val candidate=without(p,i)?:continue;val candidateSegments=segments(candidate)
                if(candidateSegments.size!=candidate.size-1 || nodeHit(edge,candidate))continue
                if(visible.any { other -> other!==edge && candidateSegments.any { a -> segments(points(other)).any { b -> shared(a,b)>=M_SHARED || crosses(a,b) } } })continue
                edge.points=candidate;fixed=true;break@outer
            }
        }
        if(!fixed)return
    }
}

internal fun MaterializedState.liftObstacleRails() {
    repeat(8) {
        val currentCrossings=crossings();var fixed=false
        outer@ for(edge in visible) {
            val p=points(edge);val s=segments(p)
            if(s.size!=3)continue
            val rail=s[1]
            if(s[0].horizontal==rail.horizontal || s[2].horizontal==rail.horizontal)continue
            val blockers=rects().filter { entry ->
                val r=entry.rect
                entry.id !in edge.endpointIds && if(rail.horizontal) {
                    materializedOverlap(rail.a.x,rail.b.x,r.left,r.right)>=M_SHARED && rail.a.y>=r.top-2 && rail.a.y<=r.bottom+2
                } else materializedOverlap(rail.a.y,rail.b.y,r.top,r.bottom)>=M_SHARED && rail.a.x>=r.left-2 && rail.a.x<=r.right+2
            }
            if(blockers.isEmpty())continue
            val coords=if(rail.horizontal) listOf(blockers.minOf { it.rect.top }-20,blockers.maxOf { it.rect.bottom }+20)
                else listOf(blockers.minOf { it.rect.left }-20,blockers.maxOf { it.rect.right }+20)
            for(coord in coords) {
                val candidate=simplify(dedupe(p.mapIndexed { i,point ->
                    if(i==rail.index || i==rail.index+1) {
                        if(rail.horizontal)point.copy(y=coord)else point.copy(x=coord)
                    }else point
                }))
                if(segments(candidate).size!=candidate.size-1 || nodeHit(edge,candidate) || sharedTrack(edge,candidate))continue
                if(crossings(mapOf(edge to candidate))>currentCrossings)continue
                edge.points=candidate;fixed=true;break@outer
            }
        }
        if(!fixed)return
    }
}

/** Title passes expand groups outward and translate every eligible top-level title by the same delta. */
internal fun MaterializedState.shiftTitles(left: Boolean) {
    val lanes=nodes.filterValues { n ->
        val r=n.groupTitleRect
        val axis=if(left)n.x else n.y;val size=if(left)n.width else n.height
        n.isGroup && n.parentId.isNullOrEmpty() &&
            (if(left)n.direction=="LR" else n.direction?.uppercase() !in listOf("LR","RL","BT")) &&
            r!=null && r.valid() && axis!=null && axis.isFinite() && size!=null && size.isFinite() && size>0 &&
            (if(left)r.bottom-r.top>=r.right-r.left else r.right-r.left>=r.bottom-r.top)
    }
    var delta=0.0
    for(edge in visible) for(s in segments(points(edge))) for(node in lanes.values) {
        val r=node.groupTitleRect!!
        val h=s.horizontal && s.a.y>r.top+M_EPS && s.a.y<r.bottom-M_EPS && materializedOverlap(s.a.x,s.b.x,r.left,r.right)>=M_SHARED
        val v=s.vertical && s.a.x>r.left+M_EPS && s.a.x<r.right-M_EPS && materializedOverlap(s.a.y,s.b.y,r.top,r.bottom)>=M_SHARED
        if(left) {
            if(v)delta=max(delta,r.right-s.a.x+4)
            else if(h)delta=max(delta,r.right-minOf(s.a.x,s.b.x)+4)
        } else if(h)delta=max(delta,r.bottom-s.a.y+4)
    }
    if(delta<=M_EPS)return
    for((key,n) in lanes) {
        val r=n.groupTitleRect!!
        nodes[key]=if(left)n.copy(x=n.x!!-delta/2,width=n.width!!+delta,groupTitleRect=r.copy(left=r.left-delta,right=r.right-delta))
        else n.copy(y=n.y!!-delta/2,height=n.height!!+delta,groupTitleRect=r.copy(top=r.top-delta,bottom=r.bottom-delta))
    }
}
