package build.raft.mermaid.kuikly

import build.raft.mermaid.layout.*
import kotlin.math.PI
import kotlin.test.*

class ScenePathTest {
    @Test fun nativeCanvasDrawsGapQuadraticAndBothArcDirections() {
        val p = DrawPath(listOf(PathMove(ScenePoint(1.0, 2.0)), PathQuadratic(ScenePoint(3.0, 4.0), ScenePoint(5.0, 6.0)), PathMove(ScenePoint(8.0, 6.0)), PathArc(ScenePoint(10.0, 6.0), 2.0, PI, PI), PathArc(ScenePoint(10.0, 6.0), 2.0, 0.0, -PI)), pattern = StrokePattern.DASHED)
        val ctx = MockCanvasContext()
        MermaidKuiklyRenderer.render(LayoutScene(20.0, 20.0, listOf(p)), ctx)
        assertTrue(ctx.log.contains("quadraticCurveTo(3.0, 4.0, 5.0, 6.0)"))
        assertTrue(ctx.log.contains("moveTo(8.0, 6.0)"))
        val arcs = ctx.log.filter { it.startsWith("arc(") }
        assertEquals(2, arcs.size)
        assertTrue(arcs[0].endsWith("false)"))
        assertTrue(arcs[1].endsWith("true)"))
        assertTrue(ctx.log.contains("setLineDash(6.0, 4.0)"))
        assertEquals(1, ctx.log.count { it == "stroke" })
        assertFalse(ctx.log.contains("closePath"))
    }
}
