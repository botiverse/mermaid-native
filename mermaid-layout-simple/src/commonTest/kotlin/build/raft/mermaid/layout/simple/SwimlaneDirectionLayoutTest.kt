package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class SwimlaneDirectionLayoutTest {
    private fun scene(source:String):LayoutScene = SimpleMermaidLayout.layout(
        assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram,FixedWidthTextMeasurer,LayoutConfig())
    private fun source(direction:String)="swimlane-beta $direction\nsubgraph One\nA[First]-->B[Second]\nend\nsubgraph Two\nC[Third]\nend\nB-->C"
    @Test fun fillOnlyPreservesLaneGeometryInEveryDirection() {
        for(direction in listOf("TB","BT","LR","RL")) {
            val plain=scene(source(direction));val colored=scene(source(direction)+"\nstyle A fill:#abcdef")
            assertEquals(plain.width,colored.width);assertEquals(plain.height,colored.height)
            assertEquals(plain.commands.filterIsInstance<DrawText>(),colored.commands.filterIsInstance<DrawText>())
            assertEquals(plain.commands.filterIsInstance<DrawRect>().map { it.rect },colored.commands.filterIsInstance<DrawRect>().map { it.rect })
            assertTrue(colored.commands.filterIsInstance<DrawRect>().any { it.fill.value=="#abcdef" })
            val lanes=plain.commands.filterIsInstance<DrawRect>().filter { it.stroke.value==DiagramPalette.MUTED && it.fill.value==DiagramPalette.CANVAS }
            assertEquals(2,lanes.size)
            if(direction in listOf("LR","RL")) {
                assertEquals(lanes[0].rect.x,lanes[1].rect.x);assertEquals(lanes[0].rect.width,lanes[1].rect.width)
                assertTrue(lanes[0].rect.y+lanes[0].rect.height<=lanes[1].rect.y+1e-6)
            }else {
                assertEquals(lanes[0].rect.y,lanes[1].rect.y);assertEquals(lanes[0].rect.height,lanes[1].rect.height)
                assertTrue(lanes[0].rect.x+lanes[0].rect.width<=lanes[1].rect.x+1e-6)
            }
        }
    }
    @Test fun nestedShapesRemainInsideTheirLaneAndKeepStyles() {
        for(direction in listOf("TB","BT","LR","RL")) {
            val s=scene("swimlane-beta $direction\nsubgraph One\nsubgraph Nested\nA@{shape: hexagon,label: Work}-->B\nend\nend\nsubgraph Two\nC\nend\nB-->C\nstyle A fill:#abcdef")
            assertTrue(s.commands.filterIsInstance<DrawPolygon>().any { it.points.size==6 && it.fill.value=="#abcdef" })
            val lanes=s.commands.filterIsInstance<DrawRect>().filter { it.stroke.value==DiagramPalette.MUTED && it.fill.value==DiagramPalette.CANVAS }
            assertEquals(2,lanes.size)
            val child=s.commands.filterIsInstance<DrawText>().first { it.text=="Work" }.origin
            assertTrue(lanes.any { child.x in it.rect.x..(it.rect.x+it.rect.width) && child.y in it.rect.y..(it.rect.y+it.rect.height) })
            for(text in s.commands.filterIsInstance<DrawText>()) assertTrue(text.origin.x in 0.0..s.width && text.origin.y in 0.0..s.height)
        }
    }
    @Test fun emptyHorizontalLanesHaveAlignedNonoverlappingBodies() {
        for(direction in listOf("LR","RL")) {
            val s=scene("swimlane-beta $direction\nsubgraph Empty\nend\nsubgraph Two\nC\nend")
            val lanes=s.commands.filterIsInstance<DrawRect>().filter { it.fill.value==DiagramPalette.CANVAS && it.stroke.value==DiagramPalette.MUTED }.sortedBy { it.rect.y }
            assertEquals(2,lanes.size);assertEquals(lanes[0].rect.x,lanes[1].rect.x)
            assertEquals(lanes[0].rect.width,lanes[1].rect.width)
            assertTrue(lanes[0].rect.y+lanes[0].rect.height<=lanes[1].rect.y+1e-6)
        }
    }
    @Test fun bottomToTopNestedTitleDoesNotOverlapChildText() {
        val s=scene("swimlane-beta BT\nsubgraph One\nsubgraph Nested\nA[Alpha]-->B[Beta]\nend\nend")
        val text=s.commands.filterIsInstance<DrawText>();val title=text.single { it.text=="Nested" }
        for(child in text.filter { it.text in listOf("Alpha","Beta") })assertTrue(kotlin.math.abs(child.origin.y-title.origin.y)>20.0)
    }
    @Test fun directionTransformsKeepNodesUprightAndMirrorPathsAndTitles() {
        val n=listOf(SwimlaneDirectionGeometry.Node("L",100.0,50.0,240.0,200.0,true,padding=20.0,title=SceneRect(-20.0,-50.0,240.0,21.0)),
            SwimlaneDirectionGeometry.Node("A",0.0,0.0,80.0,40.0,parentId="L"),SwimlaneDirectionGeometry.Node("B",0.0,100.0,80.0,40.0,parentId="L"))
        val path=listOf(listOf(ScenePoint(0.0,20.0),ScenePoint(0.0,80.0)))
        val lr=SwimlaneDirectionGeometry.transform(n,path,FlowDirection.LR)
        val rl=SwimlaneDirectionGeometry.transform(n,path,FlowDirection.RL)
        assertEquals(80.0,lr.nodes[1].width);assertEquals(40.0,lr.nodes[1].height)
        assertTrue(lr.nodes[2].x!!>lr.nodes[1].x!!);assertTrue(rl.nodes[2].x!!<rl.nodes[1].x!!)
        assertEquals(lr.nodes[1].y,lr.nodes[2].y)
        assertEquals(lr.paths[0].reversed().map { it.x },rl.paths[0].map { it.x })
        val bt=SwimlaneDirectionGeometry.transform(n,path,FlowDirection.BT)
        assertEquals(100.0,bt.nodes[1].y);assertEquals(0.0,bt.nodes[2].y)
        assertEquals(129.0,bt.nodes[0].title!!.y)
        assertEquals(n,SwimlaneDirectionGeometry.transform(n,path,FlowDirection.TB).nodes)
    }
    @Test fun measuredNodesAndTitlesRemainDisjointInsideNestedGroups() {
        for (direction in FlowDirection.entries) for (wide in listOf(80.0, 500.0)) {
            val parsed = assertIs<SwimlaneDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(
                "swimlane-beta $direction\nsubgraph One\nsubgraph Nested [A substantially longer nested group title]\nA-->B\nend\nend\nsubgraph Two\nC-->D\nend")).diagram)
            val flow = swimlaneFlow(parsed)
            val sizes = mapOf("A" to SceneSize(wide, 40.0), "B" to SceneSize(80.0, 160.0), "C" to SceneSize(80.0, 40.0), "D" to SceneSize(80.0, 40.0))
            val result = placeSwimlanes(flow, sizes, LayoutConfig(), FixedWidthTextMeasurer)
            fun contains(outer: SceneRect, inner: SceneRect) = inner.x >= outer.x - 1e-6 && inner.y >= outer.y - 1e-6 && inner.x + inner.width <= outer.x + outer.width + 1e-6 && inner.y + inner.height <= outer.y + outer.height + 1e-6
            fun overlap(a: SceneRect, b: SceneRect) = minOf(a.x+a.width,b.x+b.width)>maxOf(a.x,b.x)+1e-6 && minOf(a.y+a.height,b.y+b.height)>maxOf(a.y,b.y)+1e-6
            for (g in flow.subgraphs) {
                val frame = result.placement.groups.getValue(g.id)
                val title = result.titles.getValue(g.id)
                assertTrue(contains(frame,title), "$direction ${g.id} title outside frame")
                val textWidth = FixedWidthTextMeasurer.measure(g.label, flowGroupStyle(g,flow).text).width
                assertTrue(title.width >= textWidth, "$direction ${g.id} title clipped")
                val children = g.nodeIds.mapNotNull { result.placement.nodes[it] } + flow.subgraphs.filter { it.parentId == g.id }.map { result.placement.groups.getValue(it.id) }
                for (child in children) {
                    assertTrue(contains(frame,child), "$direction ${g.id} child outside frame")
                    assertFalse(overlap(title,child), "$direction ${g.id} title overlaps child")
                }
                children.forEachIndexed { i,a -> children.drop(i+1).forEach { b -> assertFalse(overlap(a,b), "$direction ${g.id} overlapping siblings") } }
            }
        }
    }
    @Test fun explicitNestedDirectionIsRespectedAfterOuterTransform() {
        for (outer in FlowDirection.entries) for (inner in FlowDirection.entries) {
            val s = scene("swimlane-beta $outer\nsubgraph Lane\nsubgraph Nested\ndirection $inner\nA[Alpha]-->B[Beta]\nend\nend\nsubgraph Other\nC\nend")
            val texts = s.commands.filterIsInstance<DrawText>()
            val a = texts.single { it.text == "Alpha" }.origin
            val b = texts.single { it.text == "Beta" }.origin
            when (inner) {
                FlowDirection.TB, FlowDirection.TD -> assertTrue(b.y > a.y, "$outer / $inner")
                FlowDirection.BT -> assertTrue(b.y < a.y, "$outer / $inner")
                FlowDirection.LR -> assertTrue(b.x > a.x, "$outer / $inner")
                FlowDirection.RL -> assertTrue(b.x < a.x, "$outer / $inner")
            }
        }
    }
    @Test fun crossLaneHandoffDoesNotLoopBackThroughNestedTitle() {
        for (direction in listOf("TB","BT","LR","RL")) {
            val s = scene("swimlane-beta $direction\nsubgraph One\nsubgraph Nested\nA-->B\nend\nend\nsubgraph Two\nC\nend\nB-->C")
            // Neither edge is a self-loop or a same-lane feedback edge.
            assertEquals(1, s.commands.filterIsInstance<DrawLine>().size)
            val route = s.commands.filterIsInstance<DrawPolyline>().single().points
            assertTrue(route.size >= 3)
            for ((a,b) in route.zipWithNext()) {
                assertTrue(kotlin.math.abs(a.x-b.x)<1e-6 || kotlin.math.abs(a.y-b.y)<1e-6)
                for (text in s.commands.filterIsInstance<DrawText>().filter { it.text in listOf("One","Nested","Two") }) {
                    val size=FixedWidthTextMeasurer.measure(text.text,text.style)
                    assertFalse(segmentEntersRect(a,b,SceneRect(text.origin.x-size.width/2,text.origin.y-size.height,size.width,size.height)))
                }
            }
        }
    }
    @Test fun invalidGeometryCannotHangHierarchyTraversal() {
        assertFailsWith<IllegalArgumentException> { SwimlaneDirectionGeometry.transform(listOf(SwimlaneDirectionGeometry.Node("A",isGroup=true,parentId="B"),SwimlaneDirectionGeometry.Node("B",isGroup=true,parentId="A")),emptyList(),FlowDirection.LR) }
        assertFailsWith<IllegalArgumentException> { SwimlaneDirectionGeometry.transform(listOf(SwimlaneDirectionGeometry.Node("A",Double.NaN)),emptyList(),FlowDirection.TB) }
    }
    @Test fun emptyAndCollapsedLanesDoNotRestoreHiddenMembers() {
        val s=scene("swimlane-beta LR\nsubgraph One\nsubgraph Fold\nA[Hidden]-->B\nend\nFold@{view: collapsed}\nend\nsubgraph Empty\nend\nsubgraph Two\nC\nend\nB-->C")
        assertFalse(s.commands.filterIsInstance<DrawText>().any { it.text=="Hidden" })
        assertTrue(s.commands.filterIsInstance<DrawText>().any { it.text=="Fold" })
        assertTrue(s.width.isFinite() && s.height.isFinite())
    }
}
