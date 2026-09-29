package build.raft.mermaid.layout.simple
import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*
class RequirementGrammarLayoutTest {
    private fun scene(s:String)=SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(s)).diagram,FixedWidthTextMeasurer,LayoutConfig())
    @Test fun actualDirectionAndReverseArrowsUseDeclaredAxis(){for(dir in listOf("LR","RL","TB","BT")){
        val s=scene("requirementDiagram\ndirection $dir\nA - copies -> B")
        val cards=s.commands.filterIsInstance<DrawRect>();assertEquals(2,cards.size);val a=cards[0].rect;val b=cards[1].rect
        when(dir){"LR"->assertTrue(a.x<b.x);"RL"->assertTrue(a.x>b.x);"TB"->assertTrue(a.y<b.y);else->assertTrue(a.y>b.y)}
        assertTrue(s.commands.filterIsInstance<DrawText>().any { it.text=="copies" });assertEquals(1,s.commands.filterIsInstance<DrawPolygon>().size)
    }}
    @Test fun explicitDirectionFollowsRelationsRatherThanDeclarationOrder(){
        val s=scene("requirementDiagram\ndirection LR\nrequirement Target {\n}\nelement Source {\n}\nSource - satisfies -> Target")
        val labels=s.commands.filterIsInstance<DrawText>();val source=labels.first { it.text=="element Source" };val target=labels.first { it.text=="requirement Target" };assertTrue(source.origin.x<target.origin.x)
    }
    @Test fun actualStyleMeasuresLargeHeadingsAndPaintsOriginalElementDefault(){
        val s=scene("requirementDiagram\nrequirement LongRequirementTitle:::big {\ntext: Summary\n}\nelement client {\n}\nclassDef big fill:#dbeafe,stroke:#2563eb,font-size:30px\nstyle LongRequirementTitle stroke-width:3px")
        val rects=s.commands.filterIsInstance<DrawRect>();val heading=s.commands.filterIsInstance<DrawText>().first { it.text.startsWith("requirement ") };assertEquals(32.0,heading.style.fontSize);assertTrue(rects[0].rect.width>=FixedWidthTextMeasurer.measure(heading.text,heading.style).width+24);assertEquals("#dbeafe",rects[0].fill.value);assertEquals(3.0,rects[0].strokeWidth);assertEquals(DiagramPalette.BLUE_SURFACE,rects[1].fill.value)
    }
    @Test fun emptyDiagramAndSelfLoopHaveFiniteSceneBounds(){assertTrue(scene("requirementDiagram").width>0);val s=scene("requirementDiagram\nA - traces -> A");val loop=s.commands.filterIsInstance<DrawPolyline>().single();assertEquals(4,loop.points.size);assertTrue(loop.points[0].y!=loop.points[3].y);assertTrue(loop.points.all { it.x>=0&&it.x<=s.width&&it.y>=0&&it.y<=s.height })}
}
