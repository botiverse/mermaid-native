package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.abs
import kotlin.test.*

class FlowLayeredTest {
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

    @Test fun selfLoopHangsFromItsNodeLikeMermaid() {
        val s = layout("flowchart TD\nA[Idle]-->B[Poll]\nB-->|retry|B\nB-->C[Handle]")
        val poll = s.boxes()[1]
        val loop = s.edgeCurves()[1]
        val samples = loop.samples()
        // Mermaid 11 hangs the loop under the node: it leaves and re-enters the bottom side.
        val bottom = poll.y + poll.height
        assertEquals(bottom, loop.start.y, 1e-6); assertEquals(bottom, s.edgeTip(loop).y, 1e-6)
        assertTrue(loop.start.x < loop.end.x)
        assertTrue(samples.all { it.y >= bottom - 1e-6 && it.x > poll.x && it.x < poll.x + poll.width })
        assertTrue(samples.maxOf { it.y } > bottom + 16)
        // Its label sits below the loop, and the forward edge keeps clear of it.
        val label = s.labelBackgrounds().single()
        assertTrue(label.y >= samples.maxOf { it.y } - 1e-6)
        assertEquals(0, crossings(samples, s.edgeCurves()[2].samples()))
        for (direction in listOf("LR", "RL", "BT")) {
            val turned = layout("flowchart $direction\nA[Idle]-->B[Poll]\nB-->|retry|B\nB-->C[Handle]")
            val node = turned.boxes()[1]; val curve = turned.edgeCurves()[1].samples()
            val far = when (direction) { "LR" -> curve.all { it.x >= node.x + node.width - 1e-6 }; "RL" -> curve.all { it.x <= node.x + 1e-6 }; else -> curve.all { it.y <= node.y + 1e-6 } }
            assertTrue(far, "$direction loop hangs from the node's far side")
        }
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

    private fun frames(s: LayoutScene) = s.commands.filterIsInstance<DrawRect>().filter { it.cornerRadius == 4.0 }.map { it.rect }
    private fun nodeBoxes(s: LayoutScene) = s.commands.filterIsInstance<DrawRect>().filter { it.cornerRadius == 5.0 }.map { it.rect }
    private fun SceneRect.holds(r: SceneRect) = r.x >= x - 1e-6 && r.y >= y - 1e-6 && r.x + r.width <= x + width + 1e-6 && r.y + r.height <= y + height + 1e-6

    @Test fun flowchartsWithoutLoopsUseTheLayeredCurves() {
        for (direction in listOf("TD", "BT", "LR", "RL")) {
            val s = layout("flowchart $direction\nA[Start]-->B[Middle]-->C[End]\nA-->D[Side]")
            assertEquals(3, s.edgeCurves().size, direction)
            assertTrue(s.commands.none { it is DrawLine || it is DrawPolyline }, direction)
            // A chain of single links is one straight column.
            val middle = nodeBoxes(s)[1]; val end = nodeBoxes(s)[2]
            val horizontal = direction == "LR" || direction == "RL"
            if (horizontal) assertEquals(middle.y + middle.height / 2, end.y + end.height / 2, 1e-6, direction)
            else assertEquals(middle.x + middle.width / 2, end.x + end.width / 2, 1e-6, direction)
        }
    }

    @Test fun subgraphNoEdgeCrossesIsLaidOutOnItsOwnLikeMermaid() {
        // Mermaid lays such a subgraph out separately and switches TB to LR.
        val s = layout("flowchart TD\nsubgraph S[Group]\na[First]-->b[Second]\nend\nc[Outside]-->S")
        val (a, b, c) = nodeBoxes(s)
        assertEquals(a.y, b.y, 1e-6); assertTrue(a.x + a.width < b.x)
        val frame = frames(s).single()
        assertTrue(frame.holds(a) && frame.holds(b) && !frame.holds(c))
        // The edge to the subgraph ends on its frame.
        val edge = s.edgeCurves()[1]
        assertEquals(frame.y, s.edgeTip(edge).y, 1e-6)
        // An explicit direction wins.
        val own = layout("flowchart TD\nsubgraph S[Group]\ndirection BT\na[First]-->b[Second]\nend")
        val (first, second) = nodeBoxes(own)
        assertTrue(first.y > second.y + second.height)
    }

    @Test fun crossedSubgraphsKeepMembersTogetherWithoutOverlappingFrames() {
        val s = layout("""
            flowchart TB
              c1-->a2
              subgraph one
                a1-->a2
              end
              subgraph two
                b1-->b2
              end
              subgraph three
                c1-->c2
              end
              one --> two
              three --> two
              two --> c2
        """.trimIndent())
        val frames = frames(s)
        assertEquals(3, frames.size)
        for (i in frames.indices) for (j in i + 1 until frames.size) assertFalse(frames[i].intersects(frames[j]))
        val boxes = nodeBoxes(s)
        fun box(name: String): SceneRect {
            val t = s.commands.filterIsInstance<DrawText>().single { it.text == name }
            return boxes.single { it.x < t.origin.x && t.origin.x < it.x + it.width && it.y < t.origin.y && t.origin.y < it.y + it.height }
        }
        fun frame(title: String) = (s.commands[s.commands.indexOfFirst { it is DrawText && it.text == title } - 1] as DrawRect).rect
        for ((title, names) in listOf("one" to listOf("a1", "a2"), "two" to listOf("b1", "b2"), "three" to listOf("c1", "c2")))
            for (name in names) assertTrue(frame(title).holds(box(name)), "$name inside $title")
        for (curve in s.edgeSamples()) for (box in boxes) assertFalse(curve.entersBox(box))
    }

    @Test fun anEdgeToTheOwnSubgraphStaysInTheLayeredLayout() {
        val s = layout("flowchart TB\nsubgraph G[Group]\nA[Start]-->B[Finish]\nend\nB-->G\nC[Outside]-->A")
        assertEquals(3, s.edgeCurves().size)
        assertTrue(s.commands.none { it is DrawPolyline })
        val frame = frames(s).single(); val finish = nodeBoxes(s)[1]
        val inward = s.edgeCurves()[1]
        assertEquals(finish.y + finish.height, inward.start.y, 1e-6)
        assertEquals(frame.y + frame.height, s.edgeTip(inward).y, 1e-6)
    }
}
