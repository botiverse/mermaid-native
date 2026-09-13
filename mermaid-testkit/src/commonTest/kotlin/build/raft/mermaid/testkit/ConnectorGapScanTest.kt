package build.raft.mermaid.testkit

import build.raft.mermaid.layout.DrawEllipse
import build.raft.mermaid.layout.DrawLine
import build.raft.mermaid.layout.DrawPolygon
import build.raft.mermaid.layout.DrawPolyline
import build.raft.mermaid.layout.DrawRect
import build.raft.mermaid.layout.LayoutConfig
import build.raft.mermaid.layout.LayoutScene
import build.raft.mermaid.layout.ScenePoint
import build.raft.mermaid.layout.simple.FixedWidthTextMeasurer
import build.raft.mermaid.layout.simple.SimpleMermaidLayout
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * task #364: connector-gap scan across all 32 diagram families.
 *
 * The railroad fix (task #342) closed 3-4px gaps where composite-box connector spines were
 * inset from the box edges that a wrapping parent connected to. This scans every checked-in
 * family for the same class of defect: a `DrawLine` endpoint that is not covered by ANY other
 * primitive vertex (line/polyline/polygon vertices, or a rect/ellipse boundary). Such an
 * endpoint is a dangling connector — a visible "disconnected line".
 *
 * The goal is to surface any remaining same-class gaps; families that legitimately terminate a
 * line in open space (e.g. arrow shafts into a label) are covered because arrowheads are drawn
 * as `DrawPolygon` whose vertices are included.
 */
class ConnectorGapScanTest {
    private val epsilon = 0.05

    private fun near(a: Double, b: Double) = abs(a - b) <= epsilon

    private fun onSegment(p: ScenePoint, a: ScenePoint, b: ScenePoint): Boolean {
        // Point lies on the segment a-b (collinear + within bounds).
        val cross = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
        if (abs(cross) > epsilon) return false
        val dot = (p.x - a.x) * (b.x - a.x) + (p.y - a.y) * (b.y - a.y)
        if (dot < -epsilon) return false
        val lenSq = (b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)
        return dot <= lenSq + epsilon
    }

    private fun covered(point: ScenePoint, scene: LayoutScene): Boolean {
        var vertexHits = 0
        var onAnySegment = false
        scene.commands.forEach { command ->
            when (command) {
                is DrawLine -> {
                    if (samePoint(point, command.from) || samePoint(point, command.to)) vertexHits++
                    if (onSegment(point, command.from, command.to)) onAnySegment = true
                }
                is DrawPolyline -> {
                    command.points.forEach { if (samePoint(point, it)) vertexHits++ }
                    for (i in 0 until command.points.lastIndex) {
                        if (onSegment(point, command.points[i], command.points[i + 1])) onAnySegment = true
                    }
                }
                is DrawPolygon -> {
                    command.points.forEach { if (samePoint(point, it)) vertexHits++ }
                    for (i in 0 until command.points.lastIndex) {
                        if (onSegment(point, command.points[i], command.points[i + 1])) onAnySegment = true
                    }
                }
                is DrawRect -> {
                    val r = command.rect
                    // Endpoint attached to this box: either on its edge or inside its footprint
                    // (many connectors are drawn center-to-center and painted under the card).
                    if (
                        point.x >= r.x - epsilon && point.x <= r.x + r.width + epsilon &&
                        point.y >= r.y - epsilon && point.y <= r.y + r.height + epsilon
                    ) vertexHits++
                }
                is DrawEllipse -> {
                    val nx = (point.x - command.center.x) / command.radiusX
                    val ny = (point.y - command.center.y) / command.radiusY
                    if (nx * nx + ny * ny <= 1.0 + 0.1) vertexHits++
                }
                else -> Unit
            }
        }
        // Covered when the endpoint sits on another segment (e.g. an arrow meeting a lifeline),
        // or when at least one neighbour primitive contains/触 it in addition to the line itself.
        return onAnySegment || vertexHits >= 2
    }

    private fun samePoint(a: ScenePoint, b: ScenePoint) = near(a.x, b.x) && near(a.y, b.y)

    @Test
    fun everyFamilyConnectorEndpointIsCovered() {
        val failures = mutableListOf<String>()
        MermaidExamples.all.forEach { example ->
            val scene = try {
                SimpleMermaidLayout.layout(example.expected, FixedWidthTextMeasurer, LayoutConfig())
            } catch (t: Throwable) {
                failures += "${example.path}: layout threw ${t.message}"
                return@forEach
            }
            val dangling = scene.commands
                .filterIsInstance<DrawLine>()
                .flatMap { listOf(it.from, it.to) }
                .filter { !covered(it, scene) }
                .distinct()
            if (dangling.isNotEmpty()) {
                failures += "${example.path}: ${dangling.size} dangling endpoint(s) e.g. ${dangling.first()}"
            }
        }
        assertTrue(
            failures.isEmpty(),
            "connector gaps found in ${failures.size} family(ies):\n" + failures.joinToString("\n"),
        )
    }
}
