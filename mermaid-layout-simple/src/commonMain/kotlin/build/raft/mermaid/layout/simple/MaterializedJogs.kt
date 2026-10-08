package build.raft.mermaid.layout.simple

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sign

/** Canonical key for JS toFixed(3), including binary-double ties and negative rounded zero.
 * Multiplying the 53-bit significand by 125 fits in Long; the remaining factor is a power of two.
 * Keep the rounded integer in normalized binary form rather than depending on platform formatters.
 */
internal fun materializedFixed3Key(value: Double): String {
    val magnitude=abs(value)
    if(!value.isFinite() || magnitude>=1e21)return "number:$value"
    val bits=magnitude.toBits();val rawExponent=((bits ushr 52) and 0x7ff).toInt()
    val mantissa=(bits and ((1L shl 52)-1)) + if(rawExponent==0)0L else (1L shl 52)
    var integer=mantissa*125L
    var exponent=(if(rawExponent==0)-1022 else rawExponent-1023)-49
    if(exponent<0) {
        val shift=-exponent
        integer=if(shift>=63)0L else {
            val quotient=integer ushr shift
            val remainder=integer and ((1L shl shift)-1)
            quotient + if(remainder >= (1L shl (shift-1)))1L else 0L
        }
        exponent=0
    }
    if(integer==0L)return if(value<0.0)"-0" else "0"
    while(integer and 1L == 0L) { integer=integer ushr 1;exponent++ }
    return "${if(value<0.0)"-" else "+"}$integer:$exponent"
}
internal fun materializedPathKey(points: List<MPoint>): String = points.joinToString("|") { "${materializedFixed3Key(it.x)},${materializedFixed3Key(it.y)}" }
internal fun pathLength(points: List<MPoint>): Double = segments(points).sumOf { hypot(it.a.x-it.b.x,it.a.y-it.b.y) }

internal fun MaterializedState.shortcutJogs() {
    fun border(s: MSegment,r: MBounds): Boolean = when {
        s.horizontal -> (abs(s.a.y-r.top)<1 || abs(s.a.y-r.bottom)<1) && materializedOverlap(s.a.x,s.b.x,r.left,r.right)>=M_SHARED
        s.vertical -> (abs(s.a.x-r.left)<1 || abs(s.a.x-r.right)<1) && materializedOverlap(s.a.y,s.b.y,r.top,r.bottom)>=M_SHARED
        else -> false
    }
    fun candidates(p: List<MPoint>,i: Int): List<List<MPoint>> {
        if(i+3>=p.size)return emptyList()
        val a=p[i];val b=p[i+1];val c=p[i+2];val d=p[i+3]
        val hvh=horizontal(a,b)&&vertical(b,c)&&horizontal(c,d)
        val vhv=vertical(a,b)&&horizontal(b,c)&&vertical(c,d)
        if(!(hvh||vhv) || !(if(hvh)sign(b.x-a.x)!=sign(d.x-c.x)else sign(b.y-a.y)!=sign(d.y-c.y)))return emptyList()
        val raw=if(aligned(a,d))listOf(p.take(i+1)+p.drop(i+3))else listOf(MPoint(a.x,d.y),MPoint(d.x,a.y)).map { p.take(i+1)+it+p.drop(i+3) }
        return raw.map { simplify(dedupe(it)) }.filter { segments(it).size==it.size-1 && it.any { point -> same(point,d) } }.distinctBy(::materializedPathKey)
    }
    repeat(8) {
        val currentCrossings=crossings();var bestEdge: MEdge?=null;var bestPath: List<MPoint>?=null
        var bestCrossings=currentCrossings;var bestBends=Int.MAX_VALUE;var bestLength=Double.POSITIVE_INFINITY
        for(edge in visible) {
            val p=points(edge);val currentBends=bends(p);val currentLength=pathLength(p)
            val endpointRects=edge.endpointIds.mapNotNull { nodes[it]?.bounds() }
            for(i in 0..p.size-4)for(candidate in candidates(p,i)) {
                val bends=bends(candidate);val length=pathLength(candidate)
                if(!(bends<currentBends || (bends==currentBends && length<currentLength-M_EPS)))continue
                if(nodeHit(edge,candidate) || segments(candidate).any { s -> endpointRects.any { border(s,it) } } || sharedTrack(edge,candidate))continue
                val crossings=crossings(mapOf(edge to candidate));if(crossings>currentCrossings)continue
                if(crossings>bestCrossings || (crossings==bestCrossings && (bends>bestBends || (bends==bestBends && length>=bestLength))))continue
                bestEdge=edge;bestPath=candidate;bestCrossings=crossings;bestBends=bends;bestLength=length
            }
        }
        (bestEdge?:return).points=bestPath?:return
    }
}
