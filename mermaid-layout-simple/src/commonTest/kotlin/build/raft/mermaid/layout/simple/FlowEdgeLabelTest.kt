package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.abs
import kotlin.test.*

class FlowEdgeLabelTest {
    private fun scene(source: String): LayoutScene {
        val d = assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram
        return SimpleMermaidLayout.layout(d, FixedWidthTextMeasurer, LayoutConfig())
    }
    private fun bounds(t: DrawText): SceneRect {
        val size = FixedWidthTextMeasurer.measure(t.text, t.style)
        return SceneRect(t.origin.x-size.width/2,t.origin.y-size.height,size.width,size.height)
    }
    private fun overlap(a: SceneRect,b: SceneRect) = a.x<b.x+b.width && b.x<a.x+a.width && a.y<b.y+b.height && b.y<a.y+a.height
    private fun enters(a: ScenePoint,b: ScenePoint,r: SceneRect): Boolean =
        if(abs(a.x-b.x)<1e-6) a.x>r.x+1e-6 && a.x<r.x+r.width-1e-6 && maxOf(a.y,b.y)>r.y+1e-6 && minOf(a.y,b.y)<r.y+r.height-1e-6
        else if(abs(a.y-b.y)<1e-6) a.y>r.y+1e-6 && a.y<r.y+r.height-1e-6 && maxOf(a.x,b.x)>r.x+1e-6 && minOf(a.x,b.x)<r.x+r.width-1e-6
        else error("These fixtures have orthogonal edges")

    // Like Mermaid, a label sits on its own edge over an opaque background.
    private fun assertOnItsEdge(s: LayoutScene, label: String, curve: List<ScenePoint>, message: String) {
        val r = s.labelBlock(label)
        assertTrue(curve.entersBox(r, 0.0), "$message: $label must sit on its edge")
        for (node in s.commands.filterIsInstance<DrawRect>().filter { it.cornerRadius == 5.0 }.map { it.rect })
            assertFalse(overlap(node, r), "$message: $label overlaps $node")
        assertTrue(r.x >= 0 && r.y >= 0 && r.x + r.width <= s.width && r.y + r.height <= s.height, message)
        val background = s.labelBackgrounds().single { overlap(it, r) }
        assertEquals(r.x + r.width / 2, background.x + background.width / 2, 1e-6, message)
        assertTrue(background.width >= r.width, message)
    }

    @Test fun verticalStraightLabelsClearTheirLinesAndStayInsideTheScene() {
        for(direction in listOf("TD","BT")) for(label in listOf("next step","Continue after every validation check completes")) {
            val source="flowchart $direction\nA[Start]-->|$label|B[Finish]"
            val s=scene(source)
            assertEquals(s,scene(source))
            val nodes=s.commands.filterIsInstance<DrawRect>().filter { it.cornerRadius==5.0 }.map { it.rect }
            val curve=s.edgeCurves().single()
            assertOnItsEdge(s,label,curve.samples(),direction)
            assertEquals(nodes[0].x+nodes[0].width/2,curve.start.x,1e-6)
            assertEquals(nodes[1].x+nodes[1].width/2,curve.end.x,1e-6)
            assertTrue(curve.samples().all { abs(it.x-curve.start.x)<1e-6 },"A straight chain stays straight")
        }
    }

    @Test fun verticalCycleLabelsAvoidBothStraightEdgesAndReturnDetours() {
        for(direction in listOf("TD","BT")) {
            val s=scene("flowchart $direction\nA[Entry]-->B[Branch one]\nA-->C[Branch two]\nB-->D[Join]\nC-->D\nD-->|retry|A\nB-->|switch|C\nC-->|return|B")
            val labels=listOf("retry","switch","return").map { s.labelBlock(it) }
            val curves=s.edgeSamples()
            // Only the label's own edge passes under it.
            for(label in labels)assertEquals(1,curves.count { it.entersBox(label,0.0) },direction)
            for(i in labels.indices)for(j in i+1 until labels.size)assertFalse(overlap(labels[i],labels[j]),direction)
        }
    }

    @Test fun aLabelStaysPutWhenBothSidesAreOccupied() {
        val diagram=FlowchartDiagram(FlowDirection.TD,listOf(FlowNode("A","A"),FlowNode("B","B")),listOf(FlowEdge("A","B",label="next")))
        val nodes=mapOf("A" to SceneRect(80.0,0.0,40.0,40.0),"B" to SceneRect(80.0,160.0,40.0,40.0),
            "left" to SceneRect(0.0,40.0,95.0,120.0),"right" to SceneRect(105.0,40.0,100.0,120.0))
        val paths=mapOf(0 to listOf(ScenePoint(100.0,40.0),ScenePoint(100.0,160.0)))
        assertTrue(flowStraightEdgeLabels(diagram,nodes,paths,emptyMap(),FixedWidthTextMeasurer).isEmpty())
    }

    @Test fun diagonalBranchLabelsClearTheirLinesInEveryDirection() {
        for (direction in listOf("TD", "BT", "LR", "RL")) {
            val source = "flowchart $direction\nA[Start]-->|one|B[Left]\nA-->|two|C[Right]\nB-->|done|D[Finish]\nC-->D"
            val rendered = scene(source)
            assertEquals(rendered, scene(source))
            val curves = rendered.edgeSamples()
            for ((index, name) in listOf("one", "two", "done").withIndex()) assertOnItsEdge(rendered, name, curves[index], "$direction $name")
            val labels = listOf("one", "two", "done").map { rendered.labelBlock(it) }
            for (i in labels.indices) for (j in i + 1 until labels.size) assertFalse(overlap(labels[i], labels[j]), direction)
        }
    }

    @Test fun diagonalLabelsKeepTheirOriginalPlacementWhenBothCandidatesAreBlocked() {
        val diagram = FlowchartDiagram(FlowDirection.TD,
            listOf(FlowNode("A", "A"), FlowNode("B", "B")),
            listOf(FlowEdge("A", "B", label = "blocked label")))
        val nodes = mapOf("obstacle" to SceneRect(-200.0, -200.0, 600.0, 600.0))
        val paths = mapOf(0 to listOf(ScenePoint(0.0, 0.0), ScenePoint(100.0, 100.0)))
        assertTrue(flowStraightEdgeLabels(diagram, nodes, paths, emptyMap(), FixedWidthTextMeasurer).isEmpty())
    }


    @Test fun wideDiagonalLabelsStayNearTheirEdgeHeight() {
        val name = "Continue after every validation check completes"
        // Mermaid wraps long edge labels instead of letting them run across the diagram.
        assertTrue(flowEdgeLabelLines(name, FixedWidthTextMeasurer).size >= 2)
        for (direction in listOf("TD", "BT", "LR", "RL")) {
            val rendered = scene("flowchart $direction\nA[Start]-->|$name|B[Left]\nA-->|alternative path|C[Right]\nB-->|done|D[Finish]\nC-->D")
            val curves = rendered.edgeSamples()
            assertOnItsEdge(rendered, name, curves[0], direction)
            assertOnItsEdge(rendered, "alternative path", curves[1], direction)
            assertFalse(overlap(rendered.labelBlock(name), rendered.labelBlock("alternative path")), direction)
        }
    }

}
