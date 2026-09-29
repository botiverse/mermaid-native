package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class ArchitectureModelLayoutTest {
    private fun scene(source:String):LayoutScene {
        val d=assertIs<MermaidParseResult.Success>(MermaidParser.parse("architecture-beta\n$source")).diagram
        return SimpleMermaidLayout.layout(d,FixedWidthTextMeasurer,LayoutConfig())
    }
    private fun contains(outer:SceneRect,inner:SceneRect)=inner.x>=outer.x && inner.y>=outer.y && inner.x+inner.width<=outer.x+outer.width && inner.y+inner.height<=outer.y+outer.height
    @Test fun nestedFramesContainTheirCardsAndChildHeaders() {
        val s=scene("group root(cloud)[Root]\ngroup child(cloud)[Child] in root\nservice a(server)[A] in child")
        val frames=s.commands.filterIsInstance<DrawRect>().filter { it.fill.value==DiagramPalette.SURFACE }.map { it.rect }
        val card=s.commands.filterIsInstance<DrawRect>().single { it.fill.value==DiagramPalette.BLUE_SURFACE }.rect
        assertEquals(2,frames.size);assertTrue(contains(frames[0],frames[1]));assertTrue(contains(frames[1],card))
        assertTrue(frames[1].y>=frames[0].y+40)
        assertTrue(frames.all { it.x>=0 && it.y>=0 && it.x+it.width<=s.width && it.y+it.height<=s.height })
    }
    @Test fun rowHintsChangeCardGeometryAndExpandTheContainer() {
        val s=scene("group g(cloud)[Group]\nservice a(server)[A] in g\nservice b(server)[B] in g\nservice c(server)[C] in g\nalign row a b c")
        val cards=s.commands.filterIsInstance<DrawRect>().filter { it.fill.value==DiagramPalette.BLUE_SURFACE }.map { it.rect }
        val group=s.commands.filterIsInstance<DrawRect>().single { it.fill.value==DiagramPalette.SURFACE }.rect
        assertEquals(1,cards.map { it.y }.distinct().size)
        assertTrue(cards.zipWithNext().all { (a,b)->a.x+a.width<b.x });assertTrue(cards.all { contains(group,it) })
    }
    @Test fun columnHintsKeepCardsOnTheSameVerticalAxis() {
        val s=scene("service a(server)[A]\nservice b(server)[B]\nalign column a b")
        val cards=s.commands.filterIsInstance<DrawRect>().filter { it.fill.value==DiagramPalette.BLUE_SURFACE }.map { it.rect }
        assertEquals(cards[0].x,cards[1].x);assertTrue(cards[0].y+cards[0].height<cards[1].y)
    }
    @Test fun junctionPortsBranchWithoutPassingThroughSiblingCards() {
        val s=scene("group g(cloud)[Group]\nservice app(server)[App] in g\nservice db(database)[DB] in g\nservice api(server)[API] in g\njunction mid in g\napp:B -- T:mid\nmid:R -- L:db\nmid:B -- T:api")
        val dot=s.commands.filterIsInstance<DrawEllipse>().single { it.fill.value==DiagramPalette.SECONDARY }
        val lines=s.commands.filterIsInstance<DrawPolyline>()
        assertEquals(dot.center.y-4,lines[0].points.last().y)
        assertEquals(dot.center.x+4,lines[1].points.first().x)
        val cards=s.commands.filterIsInstance<DrawRect>().filter { it.fill.value==DiagramPalette.BLUE_SURFACE }.map { it.rect }
        assertTrue(cards[0].y+cards[0].height<dot.center.y)
        assertTrue(cards[1].x>dot.center.x && cards[2].y>dot.center.y)
    }

    @Test fun crossContainerColumnHintsKeepIndependentFramesSeparated() {
        val s=scene("group first(cloud)[First]\ngroup second(cloud)[Second]\nservice a(server)[A] in first\nservice b(server)[B] in second\nalign column a b")
        val frames=s.commands.filterIsInstance<DrawRect>().filter { it.fill.value==DiagramPalette.SURFACE }.map { it.rect }
        val cards=s.commands.filterIsInstance<DrawRect>().filter { it.fill.value==DiagramPalette.BLUE_SURFACE }.map { it.rect }
        assertEquals(2,frames.size)
        assertEquals(cards[0].x,cards[1].x)
        assertTrue(contains(frames[0],cards[0]) && contains(frames[1],cards[1]))
        assertTrue(frames[0].y+frames[0].height<frames[1].y || frames[1].y+frames[1].height<frames[0].y)
    }

    @Test fun crossContainerRowHintsMoveWholeNestedSubtrees() {
        val s=scene("group first(cloud)[First]\ngroup nested(cloud)[Nested] in first\nservice a(server)[A] in nested\nservice companion(server)[Companion] in nested\ngroup second(cloud)[Second]\nservice b(server)[B] in second\nalign row a b")
        val frames=s.commands.filterIsInstance<DrawRect>().filter { it.fill.value==DiagramPalette.SURFACE }.map { it.rect }
        val cards=s.commands.filterIsInstance<DrawRect>().filter { it.fill.value==DiagramPalette.BLUE_SURFACE }.map { it.rect }
        assertEquals(cards[0].y,cards[2].y)
        val labels=s.commands.filterIsInstance<DrawText>().associateBy { it.text }
        fun frame(label:String)=frames.single { r-> val p=labels.getValue(label).origin; kotlin.math.abs(p.x-r.x-14)<1e-6 && kotlin.math.abs(p.y-r.y-24)<1e-6 }
        assertTrue(contains(frame("First"),frame("Nested")))
        assertTrue(contains(frame("Nested"),cards[0]) && contains(frame("Nested"),cards[1]))
        assertTrue(contains(frame("Second"),cards[2]))
        assertTrue(frame("First").x+frame("First").width<frame("Second").x || frame("Second").x+frame("Second").width<frame("First").x)
    }
    @Test fun intersectingRowAndColumnHintsPreserveBothAxes() {
        val s=scene("service a(server)[A]\nservice b(server)[B]\nservice c(server)[C]\nservice d(server)[D]\nalign row a b\nalign row c d\nalign column a c\nalign column b d")
        val cards=s.commands.filterIsInstance<DrawRect>().filter { it.fill.value==DiagramPalette.BLUE_SURFACE }.map { it.rect }
        assertEquals(cards[0].y,cards[1].y)
        assertEquals(cards[2].y,cards[3].y)
        assertEquals(cards[0].x,cards[2].x)
        assertEquals(cards[1].x,cards[3].x)
        assertTrue(cards[0].x+cards[0].width<cards[1].x)
        assertTrue(cards[0].y+cards[0].height<cards[2].y)
    }

}
