package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class SankeyPacketAppearanceTest {
    private fun scene(source: String)=SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram,FixedWidthTextMeasurer,LayoutConfig())
    @Test fun userSankeyHasConservedFlowWidthsJustifiedSinksAndUpstreamColors() {
        val source="sankey\nGrid,Industry,12.5\nGrid,\"Heating, homes\",7.25\nIndustry,Losses & exports,2.5"
        val s=scene(source);assertEquals(s,scene(source));assertEquals(600.0,s.width);assertEquals(400.0,s.height)
        val bars=s.commands.filterIsInstance<DrawRect>();assertEquals(4,bars.size)
        assertTrue(bars.all { it.rect.width==10.0 && it.strokeWidth==0.0 })
        assertEquals(listOf("#4e79a7","#f28e2c","#e15759","#76b7b2"),bars.map { it.fill.value })
        assertEquals(listOf(0.0,295.0,590.0,590.0),bars.map { it.rect.x })
        // Golden positions from Mermaid's d3-sankey 0.12.3 with extent600x400, padding27 and justify.
        val ys=listOf(0.0,146.8354430379747,0.0,173.83544303797476)
        bars.forEachIndexed { i,b->assertEquals(ys[i],b.rect.y,1e-7) }
        val ribbons=s.commands.filterIsInstance<DrawPolygon>();assertEquals(3,ribbons.size)
        val widths=ribbons.map { it.points.last().y-it.points.first().y }
        assertEquals(400.0,widths[0]+widths[1],1e-7)
        assertEquals(5.0,widths[0]/widths[2],1e-7)
        assertTrue(ribbons.all { it.gradient?.opacity==.5 && it.points.size==98 })
        assertEquals(listOf(TextAnchor.START,TextAnchor.START,TextAnchor.END,TextAnchor.END),s.commands.filterIsInstance<DrawText>().map { it.anchor })
    }
    @Test fun sankeyMergeStacksFlowWithoutOverlapAndZeroValuesStayFinite() {
        // The public parser rejects zero values; exercise the layout model directly for that boundary.
        val zero = SankeyDiagram(listOf(SankeyNode("A", "A"), SankeyNode("B", "B")), listOf(SankeyLink("A", "B", 0.0)))
        for(s in listOf(scene("sankey\nA,Hub,4\nB,Hub,6\nHub,Out,10"), SimpleMermaidLayout.layout(zero, FixedWidthTextMeasurer, LayoutConfig()))) {
            assertTrue(s.width.isFinite()&&s.height.isFinite())
            val bars=s.commands.filterIsInstance<DrawRect>()
            assertTrue(bars.all { it.rect.y.isFinite()&&it.rect.height.isFinite()&&it.rect.y>=-.001&&it.rect.y+it.rect.height<=s.height+.001 })
            bars.groupBy { it.rect.x }.values.forEach { group->
                group.sortedBy { it.rect.y }.zipWithNext().forEach { (a,b)->assertTrue(a.rect.y+a.rect.height<=b.rect.y+.001) }
            }
        }
    }
    @Test fun userPacketMatchesUpstreamGridAndPlacesTitleBelowRows() {
        val s=scene("packet\ntitle UDP Packet\n0-15: \"Source Port\"\n16-31: \"Destination Port\"\n32-47: \"Length\"\n48-63: \"Checksum\"\n64-95: \"Data\"")
        assertEquals(1026.0,s.width);assertEquals(188.0,s.height)
        val rects=s.commands.filterIsInstance<DrawRect>();assertEquals(5,rects.size)
        assertEquals(SceneRect(1.0,15.0,507.0,32.0),rects[0].rect)
        assertEquals(SceneRect(513.0,15.0,507.0,32.0),rects[1].rect)
        assertEquals(SceneRect(1.0,109.0,1019.0,32.0),rects.last().rect)
        assertTrue(rects.all { it.fill.value=="#efefef"&&it.stroke.value=="#000000"&&it.strokeWidth==1.0&&it.cornerRadius==0.0 })
        val title=s.commands.filterIsInstance<DrawText>().single { it.text=="UDP Packet" }
        assertEquals(TextAnchor.MIDDLE,title.anchor);assertEquals(513.0,title.origin.x);assertTrue(title.origin.y>141.0&&title.origin.y<s.height)
    }
}
