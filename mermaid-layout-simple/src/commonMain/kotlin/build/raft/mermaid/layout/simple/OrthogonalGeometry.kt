package build.raft.mermaid.layout.simple

import build.raft.mermaid.layout.ScenePoint
import build.raft.mermaid.layout.SceneRect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Orthogonal route conflict predicates shared by production flow routing. */
public object OrthogonalGeometry {
    public data class RectEntry(val id: String, val rect: SceneRect)
    public data class Edge(val points: List<ScenePoint>, val isLayoutOnly: Boolean = false)

    public fun segmentsCross(a: ScenePoint, b: ScenePoint, c: ScenePoint, d: ScenePoint,
        epsilon: Double = 1e-3, endpointTolerance: Double = 1e-6): Boolean {
        val ah = abs(a.y-b.y)<epsilon; val av = abs(a.x-b.x)<epsilon
        val bh = abs(c.y-d.y)<epsilon; val bv = abs(c.x-d.x)<epsilon
        if ((ah && bh) || (av && bv) || !(ah || av) || !(bh || bv)) return false
        val h1=if(ah)a else c; val h2=if(ah)b else d
        val v1=if(av)a else c; val v2=if(av)b else d
        val x=v1.x; val y=h1.y
        if(x<min(h1.x,h2.x) || x>max(h1.x,h2.x) || y<min(v1.y,v2.y) || y>max(v1.y,v2.y))return false
        fun endpoint(p: ScenePoint)=abs(x-p.x)<endpointTolerance && abs(y-p.y)<endpointTolerance
        return !((endpoint(h1)||endpoint(h2)) && (endpoint(v1)||endpoint(v2)))
    }

    public fun sameAxisSegmentsOverlap(a: ScenePoint, b: ScenePoint, c: ScenePoint, d: ScenePoint,
        epsilon: Double = 1e-3): Boolean {
        fun overlap(a: Double,b: Double,c: Double,d: Double)=max(0.0,min(max(a,b),max(c,d))-max(min(a,b),min(c,d)))
        if(abs(a.x-b.x)<epsilon && abs(c.x-d.x)<epsilon && abs(a.x-c.x)<epsilon)
            return overlap(a.y,b.y,c.y,d.y)>epsilon
        if(abs(a.y-b.y)<epsilon && abs(c.y-d.y)<epsilon && abs(a.y-c.y)<epsilon)
            return overlap(a.x,b.x,c.x,d.x)>epsilon
        return false
    }

    /** Bounds intersection; callers routing diagonals need a precise segment/rect test. */
    public fun segmentHitsAnyRect(a: ScenePoint,b: ScenePoint,rects: List<RectEntry>,
        excludeIds: List<String> = emptyList(),shrink: Double = 0.0): Boolean = rects.any { entry ->
        val r=entry.rect
        entry.id !in excludeIds && max(a.x,b.x)>r.x+shrink && min(a.x,b.x)<r.x+r.width-shrink &&
            max(a.y,b.y)>r.y+shrink && min(a.y,b.y)<r.y+r.height-shrink
    }

    public fun segmentConflictsWithAnyEdge(a: ScenePoint,b: ScenePoint,edges: List<Edge>,
        excludeEdge: Edge? = null,epsilon: Double = 1e-3,skipDegenerateOther: Boolean = false): Boolean = edges.any { edge ->
        edge !== excludeEdge && !edge.isLayoutOnly && edge.points.zipWithNext().any { (c,d) ->
            !(skipDegenerateOther && abs(c.x-d.x)<epsilon && abs(c.y-d.y)<epsilon) &&
                (segmentsCross(a,b,c,d,epsilon) || sameAxisSegmentsOverlap(a,b,c,d,epsilon))
        }
    }
}
