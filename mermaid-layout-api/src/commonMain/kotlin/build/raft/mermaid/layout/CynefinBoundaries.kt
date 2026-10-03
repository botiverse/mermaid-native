package build.raft.mermaid.layout

/** Cubic geometry shared by Cynefin SVG data and sampled Native drawing. */
public data class CynefinCubic(val control1: ScenePoint, val control2: ScenePoint, val end: ScenePoint)
public data class CynefinBoundary(val start: ScenePoint, val curves: List<CynefinCubic>) {
    public fun svgData(): String = buildString {
        append("M${start.x},${start.y}")
        curves.forEach { append(" C${it.control1.x},${it.control1.y} ${it.control2.x},${it.control2.y} ${it.end.x},${it.end.y}") }
    }

    public fun sampledPoints(stepsPerCurve: Int = 24): List<ScenePoint> {
        require(stepsPerCurve > 0)
        val points = mutableListOf(start)
        var from = start
        for (curve in curves) {
            for (step in 1..stepsPerCurve) {
                val t = step.toDouble() / stepsPerCurve
                val s = 1.0 - t
                points += ScenePoint(
                    s*s*s*from.x + 3*s*s*t*curve.control1.x + 3*s*t*t*curve.control2.x + t*t*t*curve.end.x,
                    s*s*s*from.y + 3*s*s*t*curve.control1.y + 3*s*t*t*curve.control2.y + t*t*t*curve.end.y,
                )
            }
            from = curve.end
        }
        return points
    }
}

public data class CynefinConfusion(val center: ScenePoint, val radiusX: Double, val radiusY: Double) {
    public fun svgData(): String = "M${center.x-radiusX},${center.y} A$radiusX,$radiusY 0 1,1 ${center.x+radiusX},${center.y} A$radiusX,$radiusY 0 1,1 ${center.x-radiusX},${center.y} Z"
}

/** Options consumed by the actual Native Cynefin layout, independent of browser global state. */
public data class CynefinBoundaryConfig(
    val seed: Double? = null,
    val diagramId: String = "cynefin",
    val amplitude: Double = 8.0,
)

/** Port of Mermaid's MIT-licensed Cynefin boundary algorithms (pinned in compatibility evidence). */
public object CynefinBoundaries {
    private fun int32(value: Double): Int = if (value.isFinite()) (value % 4294967296.0).toLong().toInt() else 0

    public fun seededRandom(seed: Double): Double {
        var t = int32(seed + 0x6d2b79f5)
        t = (t xor (t ushr 15)) * (t or 1)
        t = t xor (t + (t xor (t ushr 7)) * (t or 61))
        return (t xor (t ushr 14)).toUInt().toDouble() / 4294967296.0
    }

    public fun hashString(value: String): Int {
        var hash = 0
        value.forEach { hash = hash * 31 + it.code }
        return hash
    }

    public fun resolveSeed(configuredSeed: Double?, id: String): Double =
        configuredSeed?.takeIf { it.isFinite() && it != 0.0 } ?: hashString(id).toDouble()

    private fun dimensions(width: Double, height: Double, amplitude: Double) {
        require(width.isFinite() && width > 0 && height.isFinite() && height > 0)
        require(amplitude.isFinite() && amplitude >= 0)
    }

    public fun fold(width: Double, height: Double, seed: Double, amplitude: Double? = null): CynefinBoundary {
        val a = amplitude ?: width * 0.015
        dimensions(width, height, a)
        val points = (0..7).map { i -> ScenePoint(width/2 + seededRandom(seed+i*17)*a*2-a, i*(height/7)) }
        val curves = (0..6).map { i ->
            val p0=points[i]; val p1=points[i+1]; val mid=(p0.y+p1.y)/2
            val offset=a*1.5*(if(i%2==0) 1 else -1)*seededRandom(seed+i*31+7)
            CynefinCubic(ScenePoint(p0.x+offset,mid),ScenePoint(p1.x-offset,mid),p1)
        }
        return CynefinBoundary(points.first(),curves)
    }

    public fun horizontal(width: Double, height: Double, seed: Double, amplitude: Double? = null): CynefinBoundary {
        val a = amplitude ?: height * 0.015
        dimensions(width, height, a)
        val points=(0..7).map { i -> ScenePoint(i*(width/7),height/2+seededRandom(seed+i*23)*a*2-a) }
        val curves=(0..6).map { i ->
            val p0=points[i]; val p1=points[i+1]; val mid=(p0.x+p1.x)/2
            val offset=a*1.5*(if(i%2==0) 1 else -1)*seededRandom(seed+i*37+11)
            CynefinCubic(ScenePoint(mid,p0.y+offset),ScenePoint(mid,p1.y-offset),p1)
        }
        return CynefinBoundary(points.first(),curves)
    }

    public fun cliff(width: Double, height: Double): CynefinBoundary {
        dimensions(width,height,0.0)
        val cx=width/2; val top=height*0.5; val span=height-top; val a=width*0.03
        return CynefinBoundary(ScenePoint(cx,top),listOf(
            CynefinCubic(ScenePoint(cx+a,top+span*0.2),ScenePoint(cx-a*1.5,top+span*0.55),ScenePoint(cx+a*0.5,top+span*0.75)),
            CynefinCubic(ScenePoint(cx-a,top+span*0.85),ScenePoint(cx+a*0.3,top+span*0.95),ScenePoint(cx,height)),
        ))
    }

    public fun confusion(cx: Double, cy: Double, rx: Double, ry: Double): CynefinConfusion {
        require(cx.isFinite() && cy.isFinite() && rx.isFinite() && ry.isFinite() && rx > 0 && ry > 0)
        return CynefinConfusion(ScenePoint(cx,cy),rx,ry)
    }
}
