package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.test.*

class FlowNestedCycleTest {
    private fun parse(source:String) = (MermaidParser.parse(source) as MermaidParseResult.Success).diagram as FlowchartDiagram
    private fun layout(source:String) = SimpleMermaidLayout.layout(parse(source),FixedWidthTextMeasurer,LayoutConfig())
    private fun contains(a:SceneRect,b:SceneRect) = b.x>=a.x-0.001 && b.y>=a.y-0.001 && b.x+b.width<=a.x+a.width+0.001 && b.y+b.height<=a.y+a.height+0.001
    private fun intersects(a:SceneRect,b:SceneRect) = a.x<b.x+b.width && b.x<a.x+a.width && a.y<b.y+b.height && b.y<a.y+a.height
    private fun textBounds(t:DrawText):SceneRect {
        val size=FixedWidthTextMeasurer.measure(t.text,t.style)
        return SceneRect(t.origin.x-size.width/2,t.origin.y-size.height,size.width,size.height)
    }
    private fun enters(a:ScenePoint,b:ScenePoint,r:SceneRect):Boolean =
        if(abs(a.x-b.x)<1e-6) a.x>r.x+1e-6 && a.x<r.x+r.width-1e-6 && max(a.y,b.y)>r.y+1e-6 && min(a.y,b.y)<r.y+r.height-1e-6
        else if(abs(a.y-b.y)<1e-6) a.y>r.y+1e-6 && a.y<r.y+r.height-1e-6 && max(a.x,b.x)>r.x+1e-6 && min(a.x,b.x)<r.x+r.width-1e-6
        else error("Return segments must be orthogonal")
    private val branch="A[Choose] -->|yes| B[Short]\nA -->|no| C[Longer destination]\nB -->|retry| A"

    @Test fun leafCyclesKeepSiblingRanksAndRoutesInsideTheFrameInEveryDirection() {
        FlowDirection.entries.forEach { direction ->
            val source="graph TD\nsubgraph G[Group]\ndirection ${direction.name}\n$branch\nend"
            val s=layout(source);assertEquals(s,layout(source))
            val group=s.commands.filterIsInstance<DrawRect>().single { it.cornerRadius==4.0 }.rect
            val nodes=s.commands.filterIsInstance<DrawRect>().filter { it.cornerRadius==5.0 }.map { it.rect }
            assertEquals(3,nodes.size)
            val horizontal=direction in listOf(FlowDirection.LR,FlowDirection.RL)
            fun center(r:SceneRect)=if(horizontal)r.x+r.width/2 else r.y+r.height/2
            assertEquals(center(nodes[1]),center(nodes[2]),0.00001)
            val route=s.commands.filterIsInstance<DrawPolyline>().single()
            for(p in route.points)assertTrue(contains(group,SceneRect(p.x,p.y,0.0,0.0)),direction.name)
            for((a,b) in route.points.zipWithNext())for(node in nodes)assertFalse(enters(a,b,node),direction.name)
            val labels=s.commands.filterIsInstance<DrawText>().filter { it.text in listOf("yes","no","retry","Group") }.map(::textBounds)
            for(label in labels){assertTrue(contains(group,label));for(node in nodes)assertFalse(intersects(label,node))}
            for(i in labels.indices)for(j in i+1 until labels.size)assertFalse(intersects(labels[i],labels[j]))
        }
    }

    @Test fun ancestorFramesGrowAroundLongReturnLabels() {
        val source="graph LR\nsubgraph O[Outer]\nsubgraph G[Inner]\ndirection TD\nA-->B\nB-->|A long retry label with words|A\nend\nend"
        val s=layout(source)
        val groups=s.commands.filterIsInstance<DrawRect>().filter { it.cornerRadius==4.0 }.map { it.rect }
        assertEquals(2,groups.size);assertTrue(contains(groups[0],groups[1]))
        val label=textBounds(s.commands.filterIsInstance<DrawText>().single { it.text.startsWith("A long retry") })
        assertTrue(contains(groups[1],label))
        for(p in s.commands.filterIsInstance<DrawPolyline>().single().points)assertTrue(contains(groups[1],SceneRect(p.x,p.y,0.0,0.0)))
        assertTrue(contains(SceneRect(0.0,0.0,s.width,s.height),groups[0]))
    }

    @Test fun siblingSelfLoopsReserveIndependentContainerSpace() {
        val s=layout("graph LR\nsubgraph G[First]\nA[Work]-->|retry one|A\nend\nsubgraph H[Second]\nB[Wait]-->|retry two|B\nend")
        val groups=s.commands.filterIsInstance<DrawRect>().filter { it.cornerRadius==4.0 }.map { it.rect }
        assertEquals(2,groups.size);assertFalse(intersects(groups[0],groups[1]))
        val routes=s.commands.filterIsInstance<DrawPolyline>();assertEquals(2,routes.size)
        for(route in routes)assertEquals(1,groups.count { g->route.points.all { contains(g,SceneRect(it.x,it.y,0.0,0.0)) } })
        for(t in s.commands.filterIsInstance<DrawText>().filter { it.text.startsWith("retry") })assertEquals(1,groups.count { contains(it,textBounds(t)) })
    }

    @Test fun nestedRoutesRetainOriginalGlobalEdgeIndexAndPaint() {
        val d=parse("graph TD\nX-->Y\nsubgraph G[Group]\n$branch\nend")
        val painted=d.copy(edges=d.edges.map { if(it.label=="retry")it.copy(style=FlowEdgeStyle.DOTTED,styles=listOf("stroke:#123456","stroke-width:3px"))else it })
        val s=SimpleMermaidLayout.layout(painted,FixedWidthTextMeasurer,LayoutConfig())
        val route=s.commands.filterIsInstance<DrawPolyline>().single()
        assertEquals(StrokePattern.DASHED,route.pattern);assertEquals("#123456",route.stroke.value);assertEquals(3.0,route.strokeWidth)
        assertEquals(3,s.commands.filterIsInstance<DrawLine>().size)
        assertEquals(1,s.commands.filterIsInstance<DrawText>().count { it.text=="retry" })
    }

    private fun compound(direction:String,extra:String="") = """
        flowchart $direction
        subgraph Outer[Outer group]
        A[Entry]-->B[First]
        subgraph Inner[Inner group]
        B-->C[Second]
        C-->|retry inner|B
        end
        C-->D[Exit]
        D-->|retry outer|A
        $extra
        end
    """.trimIndent()

    @Test fun compoundCyclesReserveAncestorGuttersInEveryDirection() {
        for(direction in listOf("TD","LR","RL","BT")) {
            val source=compound(direction)
            val s=layout(source);assertEquals(s,layout(source))
            val frames=s.commands.filterIsInstance<DrawRect>().filter { it.cornerRadius==4.0 }.map { it.rect }
            val nodes=s.commands.filterIsInstance<DrawRect>().filter { it.cornerRadius==5.0 }.map { it.rect }
            assertEquals(2,frames.size);assertEquals(4,nodes.size)
            assertTrue(contains(frames[0],frames[1]))
            val routes=s.commands.filterIsInstance<DrawPolyline>();assertEquals(2,routes.size,direction)
            for(route in routes) {
                for(p in route.points)assertTrue(contains(frames[0],SceneRect(p.x,p.y,0.0,0.0)),direction)
                for((a,b) in route.points.zipWithNext())for(node in nodes)assertFalse(enters(a,b,node),direction)
                for((a,b) in route.points.zipWithNext())for(frame in frames)assertFalse(enters(a,b,frame.copy(height=32.0)),direction)
            }
            val text=s.commands.filterIsInstance<DrawText>().map(::textBounds)
            for(i in text.indices)for(j in i+1 until text.size)assertFalse(intersects(text[i],text[j]),direction)
            for(t in text)assertTrue(contains(frames[0],t),direction)
            val horizontal=direction in listOf("LR","RL")
            val sign=if(direction in listOf("RL","BT"))-1 else 1
            val centers=nodes.map { sign*(if(horizontal)it.x+it.width/2 else it.y+it.height/2) }
            assertTrue(centers.zipWithNext().all { (a,b)->a<b },direction)
        }
    }

    @Test fun compoundLongLabelsGrowTheirOwnFrameWithoutOverlappingRootSiblings() {
        val s=layout(compound("LR","D-->|retry after every validation result is checked|A")+"\nsubgraph Spare[Unrelated]\nX[Untouched]\nend")
        val frames=s.commands.filterIsInstance<DrawRect>().filter { it.cornerRadius==4.0 }.map { it.rect }
        fun frame(label:String):SceneRect {
            val index=s.commands.indexOfFirst { it is DrawText && it.text==label }
            return (s.commands[index-1] as DrawRect).rect
        }
        val outer=frame("Outer group");val spare=frame("Unrelated")
        assertFalse(intersects(outer,spare))
        val labels=s.commands.filterIsInstance<DrawText>().filter { it.text.startsWith("retry") }.map(::textBounds)
        assertEquals(3,labels.size)
        for(t in labels)assertTrue(contains(outer,t),"$outer must contain $t")
        for(i in labels.indices)for(j in i+1 until labels.size)assertFalse(intersects(labels[i],labels[j]))
        for(route in s.commands.filterIsInstance<DrawPolyline>())for((a,b) in route.points.zipWithNext())assertFalse(enters(a,b,spare))
        assertTrue(contains(SceneRect(0.0,0.0,s.width,s.height),outer))
    }

    @Test fun compoundRoutesPreserveGlobalEdgeIndexAndPaint() {
        val d=parse("flowchart LR\nX-->Y\n"+compound("LR").substringAfter("\n"))
        val painted=d.copy(edges=d.edges.map { if(it.label=="retry outer")it.copy(style=FlowEdgeStyle.DOTTED,styles=listOf("stroke:#123456","stroke-width:3px"))else it })
        val s=SimpleMermaidLayout.layout(painted,FixedWidthTextMeasurer,LayoutConfig())
        val route=s.commands.filterIsInstance<DrawPolyline>().single { it.stroke.value=="#123456" }
        assertEquals(StrokePattern.DASHED,route.pattern);assertEquals(3.0,route.strokeWidth)
        assertEquals(1,s.commands.filterIsInstance<DrawText>().count { it.text=="retry outer" })
    }
}
