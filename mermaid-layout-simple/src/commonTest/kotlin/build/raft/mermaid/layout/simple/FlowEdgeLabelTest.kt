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

    @Test fun verticalStraightLabelsClearTheirLinesAndStayInsideTheScene() {
        for(direction in listOf("TD","BT")) for(label in listOf("next step","Continue after every validation check completes")) {
            val source="flowchart $direction\nA[Start]-->|$label|B[Finish]"
            val s=scene(source)
            assertEquals(s,scene(source))
            val text=bounds(s.commands.filterIsInstance<DrawText>().single { it.text==label })
            val nodes=s.commands.filterIsInstance<DrawRect>().map { it.rect }
            val line=s.commands.filterIsInstance<DrawLine>().single()
            assertFalse(enters(line.from,line.to,text),direction)
            for(node in nodes)assertFalse(overlap(node,text))
            assertTrue(text.x>=0 && text.y>=0 && text.x+text.width<=s.width && text.y+text.height<=s.height)
            assertEquals(nodes[0].x+nodes[0].width/2,line.from.x,1e-6)
            assertEquals(nodes[1].x+nodes[1].width/2,line.to.x,1e-6)
        }
    }

    @Test fun verticalCycleLabelsAvoidBothStraightEdgesAndReturnDetours() {
        for(direction in listOf("TD","BT")) {
            val s=scene("flowchart $direction\nA[Entry]-->B[Branch one]\nA-->C[Branch two]\nB-->D[Join]\nC-->D\nD-->|retry|A\nB-->|switch|C\nC-->|return|B")
            val labels=s.commands.filterIsInstance<DrawText>().map(::bounds)
            val edges=s.commands.filterIsInstance<DrawPolyline>().flatMap { it.points.zipWithNext() }+
                s.commands.filterIsInstance<DrawLine>().map { it.from to it.to }
            for((a,b) in edges)for(label in labels)assertFalse(enters(a,b,label),direction)
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
            val lines = rendered.commands.filterIsInstance<DrawLine>()
            val texts = rendered.commands.filterIsInstance<DrawText>()
            for ((index, name) in listOf("one", "two", "done").withIndex()) {
                val r = bounds(texts.single { it.text == name })
                val line = lines[index]
                val dx = line.to.x - line.from.x
                val dy = line.to.y - line.from.y
                // Every corner must be strictly on the same side of its edge's line.
                val sides = listOf(r.x, r.x + r.width).flatMap { x ->
                    listOf(r.y, r.y + r.height).map { y -> dx * (y - line.from.y) - dy * (x - line.from.x) }
                }
                assertTrue(sides.all { it > 0 } || sides.all { it < 0 }, "$direction $name")
                for (node in rendered.commands.filterIsInstance<DrawRect>()) {
                    assertFalse(overlap(r, node.rect), "$direction $name overlaps node")
                }
                assertTrue(r.x >= 0 && r.y >= 0 && r.x + r.width <= rendered.width && r.y + r.height <= rendered.height)
            }
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
        for (direction in listOf("TD", "BT", "LR", "RL")) {
            val rendered = scene("flowchart $direction\nA[Start]-->|$name|B[Left]\nA-->|alternative path|C[Right]\nB-->|done|D[Finish]\nC-->D")
            val line = rendered.commands.filterIsInstance<DrawLine>().first()
            val r = bounds(rendered.commands.filterIsInstance<DrawText>().single { it.text == name })
            val centerY = r.y + r.height / 2
            assertTrue(centerY in minOf(line.from.y, line.to.y)..maxOf(line.from.y, line.to.y), direction)
            val dx = line.to.x - line.from.x
            val dy = line.to.y - line.from.y
            val sides = listOf(r.x, r.x + r.width).flatMap { x ->
                listOf(r.y, r.y + r.height).map { y -> dx * (y - line.from.y) - dy * (x - line.from.x) }
            }
            assertTrue(sides.all { it > 0 } || sides.all { it < 0 }, direction)
            for (node in rendered.commands.filterIsInstance<DrawRect>()) assertFalse(overlap(r, node.rect), direction)
            assertTrue(r.x >= 0 && r.x + r.width <= rendered.width, direction)
        }
    }

}
