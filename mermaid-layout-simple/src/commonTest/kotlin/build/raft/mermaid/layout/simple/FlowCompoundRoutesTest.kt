package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.*
import kotlin.test.*

class FlowCompoundRoutesTest {
    private fun scene(direction: String, extra: String = "", middle: String = ""): LayoutScene {
        val source="flowchart $direction\nsubgraph G[Group one]\nA[Start]-->B[Process]\nend\n$middle\nsubgraph H[Group two]\nC[Review]-->D[End]\nend\nB-->C\nD-->|retry|A\nC-->|rework|B\n$extra"
        val diagram=assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram
        return SimpleMermaidLayout.layout(diagram,FixedWidthTextMeasurer,LayoutConfig())
    }
    private fun enters(a: ScenePoint,b: ScenePoint,r: SceneRect): Boolean {
        val eps=.001
        return if(abs(a.x-b.x)<eps)a.x>r.x+eps && a.x<r.x+r.width-eps && max(a.y,b.y)>r.y+eps && min(a.y,b.y)<r.y+r.height-eps
        else if(abs(a.y-b.y)<eps)a.y>r.y+eps && a.y<r.y+r.height-eps && max(a.x,b.x)>r.x+eps && min(a.x,b.x)<r.x+r.width-eps
        else error("Expected orthogonal route")
    }
    private fun bounds(t:DrawText):SceneRect {
        val size=FixedWidthTextMeasurer.measure(t.text,t.style)
        return SceneRect(t.origin.x-size.width/2,t.origin.y-size.height,size.width,size.height)
    }
    private fun overlaps(a:SceneRect,b:SceneRect)=a.x<b.x+b.width && b.x<a.x+a.width && a.y<b.y+b.height && b.y<a.y+a.height
    @Test fun crossGroupReturnsAvoidNodesAndSeparateLabelsInEveryDirection() {
        for(direction in listOf("TD","LR","RL","BT")) {
            val s=scene(direction);assertEquals(s,scene(direction))
            val nodes=s.commands.filterIsInstance<DrawRect>().takeLast(4).map { it.rect }
            val routes=s.edgeSamples();assertEquals(5,routes.size,direction)
            for(route in routes)for(node in nodes)assertFalse(route.entersBox(node),direction)
            val labels=s.commands.filterIsInstance<DrawText>().filter { it.text in listOf("retry","rework") }.map(::bounds)
            assertFalse(overlaps(labels[0],labels[1]),direction)
            for(label in labels)for(node in nodes)assertFalse(overlaps(label,node),direction)
            for(route in routes)for(p in route)assertTrue(p.x>=0 && p.y>=0 && p.x<=s.width && p.y<=s.height,direction)
            for(label in labels)assertTrue(label.x>=0 && label.y>=0 && label.x+label.width<=s.width && label.y+label.height<=s.height,direction)
        }
    }
    @Test fun longParallelLabelsRemainInsideCanvasWithoutOverlapping() {
        val s=scene("LR","D-->|retry after all validation checks have finished|A")
        val labels=s.commands.filterIsInstance<DrawText>().filter { it.text.startsWith("retry") || it.text=="rework" }.map(::bounds)
        assertEquals(3,labels.size)
        for(i in labels.indices)for(j in i+1 until labels.size)assertFalse(overlaps(labels[i],labels[j]))
        for(label in labels)assertTrue(label.x>=0 && label.y>=0 && label.x+label.width<=s.width && label.y+label.height<=s.height)
    }
    @Test fun exteriorRoutesAvoidUnrelatedSiblingFrameAndHeaders() {
        val s=scene("LR",middle="subgraph Spare[Unrelated]\nX[Spare node]\nend")
        val rectangles=s.commands.filterIsInstance<DrawRect>()
        val frame=rectangles[1].rect
        assertEquals("Unrelated",(s.commands[s.commands.indexOf(rectangles[1])+1] as DrawText).text)
        val routes=s.edgeSamples();assertEquals(5,routes.size)
        for(route in routes)assertFalse(route.entersBox(frame,0.0))
        val titles=s.commands.filterIsInstance<DrawText>().filter { it.text in listOf("Group one","Group two","Unrelated") }.map(::bounds)
        assertEquals(3,titles.size)
        for(title in titles)for(route in routes)assertFalse(route.entersBox(title,0.0))
    }
}
