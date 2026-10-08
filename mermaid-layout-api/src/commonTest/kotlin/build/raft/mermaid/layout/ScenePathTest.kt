package build.raft.mermaid.layout

import build.raft.mermaid.layout.*
import kotlin.math.PI
import kotlin.test.*

class ScenePathTest {
    @Test fun validatesFiniteCoordinatesAndSubpathStart() {
        assertFailsWith<IllegalArgumentException> { DrawPath(listOf(PathLine(ScenePoint(1.0, 2.0)))) }
        assertFailsWith<IllegalArgumentException> { DrawPath(listOf(PathMove(ScenePoint(Double.NaN, 0.0)))) }
        assertFailsWith<IllegalArgumentException> { PathArc(ScenePoint(0.0, 0.0), -1.0, 0.0, PI) }
        assertFailsWith<IllegalArgumentException> { PathArc(ScenePoint(0.0, 0.0), 1.0, 0.0, 2 * PI) }
    }
    @Test fun translationPreservesGapsCurvatureAndStyles() {
        val p = DrawPath(listOf(PathMove(ScenePoint(0.0, 0.0)), PathQuadratic(ScenePoint(2.0, -3.0), ScenePoint(4.0, 0.0)), PathMove(ScenePoint(8.0, 0.0)), PathArc(ScenePoint(10.0, 0.0), 2.0, PI, PI)), pattern = StrokePattern.DASHED)
        val q = p.translated(7.0, 11.0)
        assertEquals(PathMove(ScenePoint(7.0, 11.0)), q.segments.first())
        assertEquals(PathQuadratic(ScenePoint(9.0, 8.0), ScenePoint(11.0, 11.0)), q.segments[1])
        assertEquals(PathMove(ScenePoint(15.0, 11.0)), q.segments[2])
        assertEquals(ScenePoint(17.0, 11.0), (q.segments[3] as PathArc).center)
        assertEquals(StrokePattern.DASHED, q.pattern)
        assertEquals(PathMove(ScenePoint(0.0, 0.0)), p.segments.first())
    }
    @Test fun conservativeBoundsContainControlHullArcAndStroke() {
        val p = DrawPath(listOf(PathMove(ScenePoint(0.0, 0.0)), PathQuadratic(ScenePoint(2.0, -3.0), ScenePoint(4.0, 0.0)), PathArc(ScenePoint(10.0, 0.0), 2.0, PI, PI)), strokeWidth = 2.0)
        assertEquals(SceneRect(-1.0, -4.0, 14.0, 7.0), p.conservativeBounds())
        assertEquals(SceneRect(6.0, 7.0, 14.0, 7.0), p.translated(7.0, 11.0).conservativeBounds())
        assertNull(DrawPath(emptyList()).conservativeBounds())
    }
}
