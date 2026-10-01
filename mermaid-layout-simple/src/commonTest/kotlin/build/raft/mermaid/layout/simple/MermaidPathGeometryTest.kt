package build.raft.mermaid.layout.simple

import build.raft.mermaid.layout.ScenePoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MermaidPathGeometryTest {
    @Test fun midpointFollowsUnequalSegmentsInsteadOfAveragingEndpoints() {
        val points = listOf(ScenePoint(0.0, 0.0), ScenePoint(30.0, 0.0), ScenePoint(30.0, 10.0))
        assertEquals(40.0, MermaidPathGeometry.length(points))
        assertEquals(ScenePoint(20.0, 0.0), MermaidPathGeometry.midpoint(points))
        assertEquals(points.last(), MermaidPathGeometry.pointAt(points, 40.0))
    }

    @Test fun repeatedVerticesDoNotTruncateTraversal() {
        val a = ScenePoint(1.0, 2.0)
        val b = ScenePoint(1.0, 12.0)
        assertEquals(ScenePoint(1.0, 7.0), MermaidPathGeometry.pointAt(listOf(a, a, b), 5.0))
        assertEquals(a, MermaidPathGeometry.midpoint(listOf(a, a)))
        assertEquals(a, MermaidPathGeometry.midpoint(listOf(a)))
    }

    @Test fun invalidDistancesAndEmptyPathsFailClearly() {
        val points = listOf(ScenePoint(0.0, 0.0), ScenePoint(3.0, 4.0))
        assertFailsWith<IllegalArgumentException> { MermaidPathGeometry.pointAt(points, Double.NaN) }
        assertFailsWith<IllegalArgumentException> { MermaidPathGeometry.pointAt(points, -1.0) }
        assertEquals("Could not find a suitable point for the given distance",
            assertFailsWith<IllegalStateException> { MermaidPathGeometry.pointAt(points, 6.0) }.message)
        assertFailsWith<IllegalStateException> { MermaidPathGeometry.midpoint(emptyList()) }
    }
}
