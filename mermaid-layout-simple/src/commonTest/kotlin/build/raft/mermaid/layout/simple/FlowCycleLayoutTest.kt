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

    private fun segmentEnters(a: ScenePoint,b: ScenePoint,r: SceneRect): Boolean =
        if(abs(a.x-b.x)<1e-6) a.x>r.x+1e-6 && a.x<r.x+r.width-1e-6 && max(a.y,b.y)>r.y+1e-6 && min(a.y,b.y)<r.y+r.height-1e-6
        else if(abs(a.y-b.y)<1e-6) a.y>r.y+1e-6 && a.y<r.y+r.height-1e-6 && max(a.x,b.x)>r.x+1e-6 && min(a.x,b.x)<r.x+r.width-1e-6
        else error("Feedback route must be orthogonal")

    @Test fun cyclesKeepBranchesTogetherAndRouteFeedbackOutsideNodesInEveryDirection() {
        FlowDirection.entries.forEach { direction ->
            val s=scene(direction)
            assertEquals(s,scene(direction),direction.name)
            val nodes=s.commands.filterIsInstance<DrawRect>().map { it.rect }
            val horizontal=direction in listOf(FlowDirection.LR,FlowDirection.RL)
            fun center(r:SceneRect)=if(horizontal)r.x+r.width/2 else r.y+r.height/2
            assertEquals(center(nodes[1]),center(nodes[2]),0.000001,direction.name)
            val forward=if(direction in listOf(FlowDirection.BT,FlowDirection.RL))-1 else 1
            assertTrue((center(nodes[1])-center(nodes[0]))*forward>0,direction.name)
            val route=s.commands.filterIsInstance<DrawPolyline>().single()
            for((a,b) in route.points.zipWithNext())for(node in nodes)assertFalse(segmentEnters(a,b,node),direction.name)
            val labels=s.commands.filterIsInstance<DrawText>().filter { it.text in listOf("yes","no","retry") }.map(::textBounds)
            for(i in labels.indices)for(j in i+1 until labels.size)assertFalse(intersects(labels[i],labels[j]),direction.name)
            for(label in labels)for(node in nodes)assertFalse(intersects(label,node),direction.name)
            for(p in route.points)assertTrue(p.x>=0 && p.y>=0 && p.x<=s.width && p.y<=s.height,direction.name)
            for(label in labels)assertTrue(label.x>=0 && label.y>=0 && label.x+label.width<=s.width && label.y+label.height<=s.height,direction.name)
        }
    }

    @Test fun severalFeedbackLabelsHaveSeparateExteriorTracks() {
        val s=scene(FlowDirection.TD,listOf(FlowEdge("A","B"),FlowEdge("A","C"),
            FlowEdge("B","A",label="First retry with a long label"),FlowEdge("C","A",label="Second retry")))
        val routes=s.commands.filterIsInstance<DrawPolyline>();assertEquals(2,routes.size)
        val labels=s.commands.filterIsInstance<DrawText>().filter { "retry" in it.text }.map(::textBounds)
        assertFalse(intersects(labels[0],labels[1]))
        val nodes=s.commands.filterIsInstance<DrawRect>().map { it.rect }
        for(route in routes)for((a,b) in route.points.zipWithNext())for(node in nodes)assertFalse(segmentEnters(a,b,node))
    }

    @Test fun selfLoopRemainsVisibleAndItsLabelFitsTheScene() {
        val d=FlowchartDiagram(FlowDirection.LR,listOf(FlowNode("A","Work")),listOf(FlowEdge("A","A",label="Try again")))
        val s=SimpleMermaidLayout.layout(d,FixedWidthTextMeasurer,LayoutConfig())
        val route=s.commands.filterIsInstance<DrawPolyline>().single();assertTrue(route.points.distinct().size>=4)
        val node=s.commands.filterIsInstance<DrawRect>().single().rect
        for((a,b) in route.points.zipWithNext())assertFalse(segmentEnters(a,b,node))
        val label=textBounds(s.commands.filterIsInstance<DrawText>().single { it.text=="Try again" })
        assertFalse(intersects(label,node));assertTrue(label.x>=0 && label.y>=0 && label.x+label.width<=s.width)
    }

    @Test fun feedbackRetainsStrokePatternAndCustomPaint() {
        val s=scene(FlowDirection.TD,listOf(FlowEdge("A","B"),FlowEdge("B","A",FlowEdgeStyle.DOTTED,
            styles=listOf("stroke:#123456","stroke-width:3px"))))
        val route=s.commands.filterIsInstance<DrawPolyline>().single()
        assertEquals(StrokePattern.DASHED,route.pattern);assertEquals("#123456",route.stroke.value);assertEquals(3.0,route.strokeWidth)
    }
}
