package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.abs
import kotlin.test.*

class FlowLayeredLoopsTest {
    private fun layout(source: String) = SimpleMermaidLayout.layout(
        assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, FixedWidthTextMeasurer, LayoutConfig())

    private fun near(a: ScenePoint, b: ScenePoint) = abs(a.x - b.x) < 1e-9 && abs(a.y - b.y) < 1e-9

    @Test fun basisSplineFollowsD3Knots() {
        val a = ScenePoint(0.0, 0.0); val b = ScenePoint(60.0, 30.0); val c = ScenePoint(0.0, 90.0)
        val segments = basisSegments(listOf(a, b, c))
        assertEquals(PathMove(a), segments.first())
        assertTrue(near(ScenePoint(10.0, 5.0), (segments[1] as PathLine).to))
        // d3.curveBasis knot (a + 4b + c) / 6 after the first cubic (two quadratic halves).
        assertTrue(near(ScenePoint(40.0, 35.0), (segments[3] as PathQuadratic).to))
        assertTrue(near(ScenePoint(10.0, 80.0), (segments[5] as PathQuadratic).to))
        assertEquals(PathLine(c), segments.last())
        assertEquals(listOf(PathMove(a), PathLine(c)), basisSegments(listOf(a, c)))
    }

    @Test fun loopsUseMermaidCurveOptions() {
        val loop = "flowchart TD\nA-->B\nB-->C\nC-->|again|A"
        assertEquals(3, layout(loop).edgeCurves().size)
        val linear = layout("$loop\nlinkStyle default interpolate linear")
        assertTrue(linear.edgeCurves().isEmpty())
        val step = layout("$loop\nlinkStyle 2 interpolate step")
        assertEquals(2, step.edgeCurves().size)
        val stepped = step.commands.filterIsInstance<DrawPolyline>().filter { it.points.first() != it.points.last() }
        assertEquals(1, stepped.size)
        for ((p, q) in stepped.single().points.zipWithNext()) assertTrue(abs(p.x - q.x) < 1e-9 || abs(p.y - q.y) < 1e-9)
    }

    @Test fun backEdgeTakesItsOwnLaneInEveryDirection() {
        for (direction in listOf("TD", "BT", "LR", "RL")) {
            val s = layout("flowchart $direction\nA[Draft]-->B[Review]\nB-->C{Approved?}\nC-->|no|A\nC-->|yes|D[Publish]")
            val boxes = s.boxes()
            val draft = boxes[0]; val review = boxes[1]
            val back = s.edgeCurves()[2]
            val horizontal = direction == "LR" || direction == "RL"
            // The loop leaves the chain axis, so it never runs through Review.
            assertFalse(back.samples().entersBox(review), direction)
            val reviewCross = if (horizontal) review.y + review.height / 2 else review.x + review.width / 2
            val draftCross = if (horizontal) draft.y + draft.height / 2 else draft.x + draft.width / 2
            val laneCross = back.samples().map { if (horizontal) it.y else it.x }
            val side = laneCross.maxBy { abs(it - reviewCross) } - reviewCross
            assertTrue(abs(side) > (if (horizontal) review.height else review.width) / 2, "$direction lane beside Review")
            // Review shifts the other way from the lane.
            assertTrue((reviewCross - draftCross) * side < 0, direction)
            // The label sits on the lane.
            val label = s.commands.filterIsInstance<DrawText>().single { it.text == "no" }
            val labelCross = if (horizontal) label.origin.y else label.origin.x
            assertTrue((labelCross - reviewCross) * side > 0, direction)
        }
    }

    @Test fun selfLoopStaysBesideItsNode() {
        val s = layout("flowchart TD\nA[Idle]-->B[Poll]\nB-->|retry|B\nB-->C[Handle]")
        val poll = s.boxes()[1]
        val loop = s.edgeCurves()[1]
        assertTrue(loop.samples().all { it.x >= poll.x + poll.width - 1e-6 && it.y >= poll.y - 1e-6 && it.y <= poll.y + poll.height + 1e-6 })
        val label = s.commands.filterIsInstance<DrawText>().single { it.text == "retry" }
        assertTrue(label.origin.x > loop.samples().maxOf { it.x })
    }

    @Test fun subgraphTitlesStayClearOfEnteringEdges() {
        val s = layout("""
            flowchart TB
              subgraph Core["Core"]
                R1["first entry point"]
                R2["second entry point"]
                R3["third entry point"]
              end
              subgraph App["apps/artifacts (schema app_artifacts)"]
                A["Artifacts module"]
              end
              R1 --> A
              R2 --> A
              R3 --> A
        """.trimIndent())
        val title = s.commands.filterIsInstance<DrawText>().single { it.text.startsWith("apps/artifacts") }
        val size = FixedWidthTextMeasurer.measure(title.text, title.style)
        val bounds = SceneRect(title.origin.x - size.width / 2, title.origin.y - size.height, size.width, size.height)
        val routes = s.commands.mapNotNull { when (it) { is DrawLine -> listOf(it.from, it.to); is DrawPolyline -> it.points; else -> null } }
        for (route in routes) for ((a, b) in route.zipWithNext()) for (i in 0..20) {
            val t = i / 20.0
            val p = ScenePoint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
            assertFalse(p.x > bounds.x && p.x < bounds.x + bounds.width && p.y > bounds.y && p.y < bounds.y + bounds.height, "edge crosses title at $p")
        }
        val frame = s.commands.filterIsInstance<DrawRect>().first { r -> r.rect.x <= bounds.x && r.rect.x + r.rect.width >= bounds.x + bounds.width && r.rect.y <= bounds.y && r.cornerRadius == 4.0 && r.rect.y + 40 > bounds.y }
        assertTrue(frame.rect.x + frame.rect.width >= bounds.x + bounds.width)
    }
}
