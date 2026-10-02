package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class FlowObstacleRoutesTest {
    private fun scene(source: String)=SimpleMermaidLayout.layout(
        assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram,FixedWidthTextMeasurer,LayoutConfig())
    private fun routes(s: LayoutScene)=s.commands.filterIsInstance<DrawPolyline>()
    private fun assertClear(s: LayoutScene) {
        val rects=s.commands.filterIsInstance<DrawRect>().map { it.rect }
        for(route in routes(s))for((a,b)in route.points.zipWithNext()) {
            assertTrue(a.x==b.x || a.y==b.y)
            for(r in rects) {
                val crosses=if(a.x==b.x)a.x>r.x+.01 && a.x<r.x+r.width-.01 &&
                    maxOf(a.y,b.y)>r.y+.01 && minOf(a.y,b.y)<r.y+r.height-.01
                else a.y>r.y+.01 && a.y<r.y+r.height-.01 &&
                    maxOf(a.x,b.x)>r.x+.01 && minOf(a.x,b.x)<r.x+r.width-.01
                assertFalse(crosses,"route crosses node: $a $b $r")
            }
            for(p in listOf(a,b))assertTrue(p.x>=0 && p.x<=s.width && p.y>=0 && p.y<=s.height)
        }
    }
    @Test fun alignedSkipEdgesAvoidWideIntermediateNodesInEveryDirection() {
        for(d in listOf("TD","TB","LR","RL","BT")) {
            val s=scene("graph $d\nA[Start]-->B[A much wider intermediate node]\nB-->C[End]\nA-->C")
            assertEquals(1,routes(s).size,d);assertEquals(6,routes(s).single().points.size,d)
            assertClear(s)
            assertEquals(s,scene("graph $d\nA[Start]-->B[A much wider intermediate node]\nB-->C[End]\nA-->C"))
        }
    }
    @Test fun parallelDetoursKeepDistinctTracksLabelsAndPaint() {
        val s=scene("graph TD\nA[Start]-->B[Middle]\nB-->C[End]\nA-->|first long bypass label|C\nA-.->|second|C\nlinkStyle 2 stroke:#ff0000,stroke-width:4px")
        val lines=routes(s);assertEquals(2,lines.size);assertNotEquals(lines[0].points,lines[1].points)
        assertEquals(SceneColor("#ff0000"),lines[0].stroke);assertEquals(4.0,lines[0].strokeWidth)
        assertEquals(StrokePattern.DASHED,lines[1].pattern)
        assertClear(s)
        val texts=s.commands.filterIsInstance<DrawText>().filter { it.text in listOf("first long bypass label","second") }
        assertEquals(2,texts.size)
        for(t in texts){val size=FixedWidthTextMeasurer.measure(t.text,t.style)
            assertTrue(t.origin.x-size.width/2>=0 && t.origin.x+size.width/2<=s.width)
            assertTrue(t.origin.y-size.height>=0 && t.origin.y<=s.height)
        }
    }
    @Test fun existingFeedbackRoutesAndForwardDetoursCoexist() {
        val s=scene("graph LR\nA[Start]-->B[Middle]\nB-->C[End]\nA-->|skip|C\nC-->|retry|A")
        assertEquals(2,routes(s).size);assertClear(s)
        assertTrue(s.commands.filterIsInstance<DrawText>().any { it.text=="retry" })
    }
    @Test fun skipAndFeedbackPreferOppositeSidesToAvoidInteriorCrossings() {
        for(direction in listOf("TD","TB","LR","RL","BT")) {
            val source="graph $direction\nA[Start]-->B[Middle]\nB-->C[End]\nA-->|skip|C\nC-->|retry|A"
            val s=scene(source)
            val lines=routes(s);assertEquals(2,lines.size,direction);assertClear(s)
            // Independent axis-aligned crossing check of the final rendered routes.
            for((a,b) in lines[0].points.zipWithNext())for((c,d) in lines[1].points.zipWithNext()) {
                fun interior(x: Double,u: Double,v: Double)=x>minOf(u,v)+.001 && x<maxOf(u,v)-.001
                val crosses=if(a.y==b.y && c.x==d.x)interior(c.x,a.x,b.x)&&interior(a.y,c.y,d.y)
                    else if(a.x==b.x && c.y==d.y)interior(a.x,c.x,d.x)&&interior(c.y,a.y,b.y)else false
                assertFalse(crosses,"$direction routes cross: $a $b / $c $d")
            }
            assertEquals(s,scene(source),direction)
        }
    }

    @Test fun clearDiagonalInvisibleAndNestedEdgesKeepTheirExistingPath() {
        for(source in listOf("graph TD\nA-->B", "graph TD\nA-->B\nA-->C",
            "graph TD\nA-->B\nB-->C\nA~~~C", "graph TD\nsubgraph G\nA-->B\nB-->C\nA-->C\nend")) {
            assertTrue(routes(scene(source)).isEmpty(),source)
        }
    }
}
