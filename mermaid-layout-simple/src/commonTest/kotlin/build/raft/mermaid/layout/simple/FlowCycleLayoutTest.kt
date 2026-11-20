package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.test.*

class FlowCycleLayoutTest {
    private fun scene(direction: FlowDirection, edges: List<FlowEdge> = listOf(
        FlowEdge("A", "B", label="yes"), FlowEdge("A", "C", label="no"), FlowEdge("B", "A", label="retry"),
    )) = SimpleMermaidLayout.layout(FlowchartDiagram(direction,
        listOf(FlowNode("A", "Choose"), FlowNode("B", "Short"), FlowNode("C", "Longer destination")),edges),
        FixedWidthTextMeasurer,LayoutConfig())

    private fun intersects(a: SceneRect,b: SceneRect) =
        a.x < b.x+b.width && b.x < a.x+a.width && a.y < b.y+b.height && b.y < a.y+a.height

    private fun textBounds(text: DrawText): SceneRect {
        val size=FixedWidthTextMeasurer.measure(text.text,text.style)
        return SceneRect(text.origin.x-size.width/2,text.origin.y-size.height,size.width,size.height)
    }

    // Flat cyclic flowcharts use the layered loop layout: every edge is a basis
    // spline through its dummy vertices, and loops run in their own lanes.
    private fun assertCurvesClear(s: LayoutScene, message: String) {
        val nodes = s.boxes()
        for (curve in s.edgeCurves()) {
            val samples = curve.samples()
            for (node in nodes) assertFalse(samples.entersBox(node), "$message: curve enters $node")
            for (p in samples) assertTrue(p.x >= 0 && p.y >= 0 && p.x <= s.width && p.y <= s.height, "$message: $p outside scene")
        }
    }

    private fun assertLabelsClear(s: LayoutScene, texts: List<String>, message: String) {
        val labels = texts.map { s.labelBlock(it) }
        assertEquals(texts.size, labels.size, message)
        for (i in labels.indices) for (j in i + 1 until labels.size) assertFalse(intersects(labels[i], labels[j]), message)
        for (label in labels) for (node in s.boxes()) assertFalse(intersects(label, node), message)
        for (label in labels) assertTrue(label.x >= 0 && label.y >= 0 && label.x + label.width <= s.width && label.y + label.height <= s.height, message)
    }

    @Test fun cyclesKeepBranchesTogetherAndRouteFeedbackOutsideNodesInEveryDirection() {
        FlowDirection.entries.forEach { direction ->
            val s=scene(direction)
            assertEquals(s,scene(direction),direction.name)
            val nodes=s.boxes()
            val horizontal=direction in listOf(FlowDirection.LR,FlowDirection.RL)
            fun center(r:SceneRect)=if(horizontal)r.x+r.width/2 else r.y+r.height/2
            assertEquals(center(nodes[1]),center(nodes[2]),0.000001,direction.name)
            val forward=if(direction in listOf(FlowDirection.BT,FlowDirection.RL))-1 else 1
            assertTrue((center(nodes[1])-center(nodes[0]))*forward>0,direction.name)
            assertEquals(3,s.edgeCurves().size,direction.name)
            assertCurvesClear(s,direction.name)
            assertLabelsClear(s,listOf("yes","no","retry"),direction.name)
        }
    }

    @Test fun severalFeedbackLabelsHaveSeparateExteriorTracks() {
        val s=scene(FlowDirection.TD,listOf(FlowEdge("A","B"),FlowEdge("A","C"),
            FlowEdge("B","A",label="First retry with a long label"),FlowEdge("C","A",label="Second retry")))
        assertEquals(4,s.edgeCurves().size)
        assertLabelsClear(s,listOf("First retry with a long label","Second retry"),"TD")
        assertCurvesClear(s,"TD")
    }

    @Test fun selfLoopRemainsVisibleAndItsLabelFitsTheScene() {
        val d=FlowchartDiagram(FlowDirection.LR,listOf(FlowNode("A","Work")),listOf(FlowEdge("A","A",label="Try again")))
        val s=SimpleMermaidLayout.layout(d,FixedWidthTextMeasurer,LayoutConfig())
        val route=s.edgeCurves().single();assertTrue(route.samples().distinct().size>=4)
        val node=s.boxes().single()
        assertFalse(route.samples().entersBox(node))
        // The loop leaves and re-enters the node itself.
        for(p in listOf(route.start,route.end))assertTrue(p.x>=node.x-1e-6 && p.x<=node.x+node.width+1e-6 && p.y>=node.y-1e-6 && p.y<=node.y+node.height+1e-6)
        assertLabelsClear(s,listOf("Try again"),"LR")
    }

    @Test fun feedbackRetainsStrokePatternAndCustomPaint() {
        val s=scene(FlowDirection.TD,listOf(FlowEdge("A","B"),FlowEdge("B","A",FlowEdgeStyle.DOTTED,
            styles=listOf("stroke:#123456","stroke-width:3px"))))
        val route=s.edgeCurves().single { it.stroke.value=="#123456" }
        assertEquals(StrokePattern.DASHED,route.pattern);assertEquals(3.0,route.strokeWidth)
    }

    @Test fun forwardDetoursDoNotMoveAcrossExistingReturnLabels() {
        val source = "flowchart LR\nA[Entry]-->B[Branch one]\nA-->C[Branch two]\nB-->D[Join]\nC-->D\nD-->|retry|A\nB-->|switch|C\nC-->|return|B"
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram
        val s = SimpleMermaidLayout.layout(parsed, FixedWidthTextMeasurer, LayoutConfig())
        assertLabelsClear(s, listOf("retry", "return", "switch"), "LR")
        // A label sits on its own edge; no other edge passes under it.
        val backgrounds = s.labelBackgrounds()
        assertEquals(3, backgrounds.size)
        val curves = s.edgeCurves().map { it.samples() }
        for (bg in backgrounds) assertEquals(1, curves.count { it.entersBox(bg, 0.0) }, "$bg")
    }

    @Test fun overlappingCyclesAndSelfLoopsUseBothSidesWithoutStrictCrossings() {
        for (direction in listOf("TD", "LR", "RL", "BT")) for (body in listOf(
            "A[Entry]-->B[First]\nB-->C[Second]\nC-->|retry one|A\nC-->D[Third]\nD-->E[Exit]\nE-->|retry two|C",
            "A[Start]-->B[Work]\nB-->|again|B\nB-->C[Done]\nC-->|restart|A\nA-->|wait|A",
        )) {
            val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse("flowchart $direction\n$body")).diagram
            val s = SimpleMermaidLayout.layout(parsed, FixedWidthTextMeasurer, LayoutConfig())
            assertEquals(s, SimpleMermaidLayout.layout(parsed, FixedWidthTextMeasurer, LayoutConfig()))
            assertCurvesClear(s, direction)
            val curves = s.edgeCurves().map { it.samples() }
            for (i in curves.indices) for (j in i + 1 until curves.size)
                assertEquals(0, crossings(curves[i], curves[j]), "$direction $body: curves $i and $j cross")
            val labels = s.commands.filterIsInstance<DrawText>().map(::textBounds)
            for (i in labels.indices) for (j in i+1 until labels.size) assertFalse(intersects(labels[i],labels[j]))
            for (label in labels) assertTrue(label.x>=0 && label.y>=0 && label.x+label.width<=s.width && label.y+label.height<=s.height)
        }
    }
    @Test fun mixedForwardDetoursAndNestedReturnsReduceCrossingsWithoutObscuringLabels() {
        for (direction in listOf("TD", "LR", "RL", "BT")) for (retry in listOf("retry", "A long retry label around the entire cycle")) {
            val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse("""
                flowchart $direction
                A[Entry]-->B[Branch one]
                A-->C[Branch two]
                B-->D[Join]
                C-->D
                D-->|$retry|A
                B-->|switch|C
                C-->|return|B
            """.trimIndent())).diagram
            val s = SimpleMermaidLayout.layout(parsed, FixedWidthTextMeasurer, LayoutConfig())
            assertEquals(s, SimpleMermaidLayout.layout(parsed, FixedWidthTextMeasurer, LayoutConfig()))
            assertEquals(7, s.edgeCurves().size)
            assertCurvesClear(s, "$direction $retry")
            val curves = s.edgeCurves().map { it.samples() }
            var total = 0
            for (i in curves.indices) for (j in i + 1 until curves.size) total += crossings(curves[i], curves[j])
            assertTrue(total <= 1, "$direction $retry: $total crossings")
            assertLabelsClear(s, listOf(retry, "return", "switch"), "$direction $retry")
        }
    }

}
