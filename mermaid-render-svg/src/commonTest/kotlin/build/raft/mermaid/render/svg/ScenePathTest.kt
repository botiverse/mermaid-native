package build.raft.mermaid.render.svg

import build.raft.mermaid.layout.*
import kotlin.math.PI
import kotlin.test.*

class ScenePathTest {
    @Test fun serializesCurvesAndGapsWithoutJoiningSubpaths() {
        val p = DrawPath(listOf(PathMove(ScenePoint(1.0, 2.0)), PathQuadratic(ScenePoint(3.0, 4.0), ScenePoint(5.0, 6.0)), PathMove(ScenePoint(8.0, 6.0)), PathLine(ScenePoint(12.0, 6.0))), pattern = StrokePattern.DASHED)
        val svg = SvgRenderer.render(LayoutScene(20.0, 20.0, listOf(p)))
        assertTrue(svg.contains("d=\"M1,2 Q3,4 5,6 M8,6 L12,6\""))
        assertTrue(svg.contains("stroke-dasharray=\"6 4\""))
        assertTrue(svg.contains("fill=\"none\""))
    }
    @Test fun arcsPreserveSignedSweepAndRejectMarkupInStyles() {
        val p = DrawPath(listOf(PathMove(ScenePoint(12.0, 10.0)), PathArc(ScenePoint(10.0, 10.0), 2.0, 0.0, PI), PathArc(ScenePoint(10.0, 10.0), 2.0, PI, -PI)), stroke = SceneColor("red\" onload=\"bad"))
        val svg = SvgRenderer.render(LayoutScene(20.0, 20.0, listOf(p)))
        assertTrue(svg.contains("A2,2 0 0 1"))
        assertTrue(svg.contains("A2,2 0 0 0"))
        assertFalse(svg.contains(" onload=\""))
    }
}
