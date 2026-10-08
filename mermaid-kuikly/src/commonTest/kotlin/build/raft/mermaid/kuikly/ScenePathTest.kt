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
    @Test fun actualSwimlaneProducerReachesKuiklyCanvas() {
        val source = "swimlane-beta\nsubgraph L1\nA\nB\nend\nsubgraph L2\nC\nD\nend\nA-->D\nB-->C"
        val diagram = (build.raft.mermaid.core.MermaidParser.parse(source) as build.raft.mermaid.core.MermaidParseResult.Success).diagram as build.raft.mermaid.core.SwimlaneDiagram
        for (mode in build.raft.mermaid.core.SwimlaneLineHops.entries) {
            val scene = build.raft.mermaid.layout.simple.SimpleMermaidLayout.layout(diagram.copy(lineHops = mode), build.raft.mermaid.layout.simple.FixedWidthTextMeasurer, LayoutConfig())
            val ctx = MockCanvasContext()
            MermaidKuiklyRenderer.render(scene, ctx)
            assertEquals(mode == build.raft.mermaid.core.SwimlaneLineHops.ARC, ctx.log.any { it.startsWith("arc(") })
            val paths = scene.commands.filterIsInstance<DrawPath>()
            if (mode == build.raft.mermaid.core.SwimlaneLineHops.GAP) {
                val moves = paths.single().segments.filterIsInstance<PathMove>()
                assertEquals(2, moves.size)
                for (move in moves) assertTrue(ctx.log.any { it.startsWith("moveTo(${move.to.x.toFloat()}, ${move.to.y.toFloat()}") })
            }
        }
    }
}
