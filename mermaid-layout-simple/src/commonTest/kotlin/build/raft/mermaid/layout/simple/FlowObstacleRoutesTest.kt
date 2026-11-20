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
            // The skip edge takes its own lane beside the wide node, as in Mermaid.
            assertEquals(3,s.edgeCurves().size,d)
            assertCurvesClear(s,d)
            assertEquals(s,scene("graph $d\nA[Start]-->B[A much wider intermediate node]\nB-->C[End]\nA-->C"))
        }
    }
    @Test fun parallelDetoursKeepDistinctTracksLabelsAndPaint() {
        val s=scene("graph TD\nA[Start]-->B[Middle]\nB-->C[End]\nA-->|first long bypass label|C\nA-.->|second|C\nlinkStyle 2 stroke:#ff0000,stroke-width:4px")
        val lines=s.edgeCurves();assertEquals(4,lines.size);assertNotEquals(lines[2].segments,lines[3].segments)
        assertEquals(SceneColor("#ff0000"),lines[2].stroke);assertEquals(4.0,lines[2].strokeWidth)
        assertEquals(StrokePattern.DASHED,lines[3].pattern)
        assertCurvesClear(s,"TD")
        val labels=listOf("first long bypass label","second").map { s.labelBlock(it) }
        for(r in labels)assertTrue(r.x>=0 && r.x+r.width<=s.width && r.y>=0 && r.y+r.height<=s.height)
        assertFalse(labels[0].intersects(labels[1]))
    }
    // With a feedback edge the flat flowchart uses the layered loop layout and spline edges.
    private fun assertCurvesClear(s: LayoutScene, message: String) {
        val curves=s.edgeCurves().map { it.samples() }
        for(curve in curves)for(r in s.boxes())assertFalse(curve.entersBox(r),"$message: curve enters $r")
        for(curve in curves)for(p in curve)assertTrue(p.x>=0 && p.x<=s.width && p.y>=0 && p.y<=s.height,message)
    }
    @Test fun existingFeedbackRoutesAndForwardDetoursCoexist() {
        val s=scene("graph LR\nA[Start]-->B[Middle]\nB-->C[End]\nA-->|skip|C\nC-->|retry|A")
        assertEquals(4,s.edgeCurves().size);assertCurvesClear(s,"LR")
        assertTrue(s.commands.filterIsInstance<DrawText>().any { it.text=="retry" })
    }
    @Test fun skipAndFeedbackPreferOppositeSidesToAvoidInteriorCrossings() {
        for(direction in listOf("TD","TB","LR","RL","BT")) {
            val source="graph $direction\nA[Start]-->B[Middle]\nB-->C[End]\nA-->|skip|C\nC-->|retry|A"
            val s=scene(source)
            assertCurvesClear(s,direction)
            val curves=s.edgeCurves().map { it.samples() }
            for(i in curves.indices)for(j in i+1 until curves.size)assertEquals(0,crossings(curves[i],curves[j]),"$direction curves $i/$j cross")
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
