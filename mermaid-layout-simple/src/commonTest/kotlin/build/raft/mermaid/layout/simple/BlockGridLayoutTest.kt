package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class BlockGridLayoutTest {
    private fun leaf(id: String, width: Double = 100.0, height: Double = 50.0, span: Int = 1) =
        BlockGridLayout.Node(id, span = span, size = BlockGridLayout.Size(width, height))
    private fun scene(source: String) = SimpleMermaidLayout.layout(
        assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram,
        TextMeasurer { _, _ -> SceneSize(100.0, 20.0) }, LayoutConfig(),
    )
    @Test fun runtimePaddingUsesFreshMeasuredTreeAndChangesPaintedSpacing() {
        fun source(padding: Int) = "---\nconfig:\n  block:\n    padding: $padding\n---\nblock-beta\ncolumns 2\nA[\"Alpha\"] B[\"Beta\"]"
        val a = scene(source(4)); val b = scene(source(20)); val c = scene(source(4))
        assertEquals(a, c)
        fun gap(s: LayoutScene): Double { val r = s.commands.filterIsInstance<DrawRect>(); return r[1].rect.x-r[0].rect.x-r[0].rect.width }
        assertEquals(4.0, gap(a), 1e-9); assertEquals(20.0, gap(b), 1e-9)
        assertTrue(b.width > a.width)
    }
    @Test fun invalidPaddingAndWrongFamilyAreRejected() {
        for (value in listOf("-1", "true", "bad")) assertIs<MermaidParseResult.Failure>(MermaidParser.parse("---\nconfig:\n  block:\n    padding: $value\n---\nblock-beta\nA"))
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("---\nconfig:\n  block:\n    padding: 8\n---\nflowchart LR\nA --> B"))
    }
    @Test fun siblingsShareMeasuredCellSizeAndOverflowOccupancyStaysOnStartingRow() {
        val a = leaf("a", 200.0, 80.0); val b = leaf("b", 120.0, 20.0, 2); val c = leaf("c")
        val root = BlockGridLayout.Node("root", columns = 2, children = listOf(a,b,c))
        BlockGridLayout.layout(root)
        assertEquals(200.0,a.size!!.width); assertEquals(408.0,b.size!!.width)
        assertEquals(a.size!!.y,b.size!!.y)
        assertEquals(a.size!!.y+88.0,c.size!!.y)
        assertEquals(a.size!!.x,c.size!!.x)
    }
    @Test fun spacesConsumeColumnsWithoutInflatingSiblingMeasurement() {
        val a = leaf("a"); val spacer = leaf("space", 999.0, 999.0, 2).copy(type="space"); val b=leaf("b")
        BlockGridLayout.layout(BlockGridLayout.Node("root", columns=3, children=listOf(a,spacer,b)))
        assertEquals(100.0,a.size!!.width); assertEquals(208.0,spacer.size!!.width)
        assertEquals(50.0,spacer.size!!.height); assertTrue(b.size!!.y>a.size!!.y)
    }
    @Test fun rawNestedGridAndNativeHeadingBandKeepChildrenInside() {
        for (heading in listOf(0.0,32.0)) {
            val child = leaf("a"); val group = BlockGridLayout.Node("g", columns=1, children=listOf(child), headingHeight=heading)
            BlockGridLayout.layout(BlockGridLayout.Node("root",columns=1,children=listOf(group)))
            val g=group.size!!;val a=child.size!!
            assertEquals(g.y-g.height/2+heading+8,a.y-a.height/2,1e-9)
            assertTrue(a.y+a.height/2 <= g.y+g.height/2)
        }
    }
    @Test fun emptyAndSpaceOnlyScenesRemainFiniteAndRepeatable() {
        for (body in listOf("", "space:3", "columns 2\nspace space")) {
            val s=scene("block-beta\n$body");assertTrue(s.width.isFinite()&&s.height.isFinite());assertTrue(s.commands.isEmpty())
        }
    }
    @Test fun repeatedSizingIsBoundedForDeepCompositeInput() {
        var root = leaf("leaf")
        repeat(30) { root = BlockGridLayout.Node("g$it", children = listOf(root)) }
        val error = assertFailsWith<IllegalArgumentException> { BlockGridLayout.layout(root) }
        assertTrue(error.message.orEmpty().contains("work limit"))
    }
    @Test fun invalidPublicGridInputsFailBeforePlacing() {
        assertFailsWith<IllegalArgumentException> { BlockGridLayout.position(0,0) }
        assertFailsWith<IllegalArgumentException> { BlockGridLayout.position(2,-1) }
        assertFailsWith<IllegalArgumentException> { BlockGridLayout.layout(leaf("a"),Double.NaN) }
        assertFailsWith<IllegalArgumentException> { BlockGridLayout.layout(leaf("a",span=0)) }
    }
}
