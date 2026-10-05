package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class OrthogonalLayoutValidatorTest {
    private fun point(x: Int,y: Int)=ScenePoint(x.toDouble(),y.toDouble())
    @Test fun normalizationCrossingsAndInvalidScoreAreIndependent() {
        val clean=LayoutValidationInput(emptyList(),listOf(LayoutValidationEdge("a",listOf(point(0,0),point(0,0),point(20,0),point(40,0)))))
        val straight=OrthogonalLayoutValidator.validate(clean)
        assertTrue(straight.ok);assertEquals(2,straight.breakdown.totalPoints);assertEquals(1000.0,straight.score)
        val crossing=OrthogonalLayoutValidator.validate(clean.copy(edges=clean.edges+LayoutValidationEdge("b",listOf(point(20,-20),point(20,20)))))
        assertEquals(1,crossing.breakdown.crossings);assertEquals(997.0,crossing.score)
        val invalid=OrthogonalLayoutValidator.validate(clean.copy(nodes=listOf(LayoutValidationNode("block",SceneRect(10.0,-10.0,20.0,20.0)))))
        assertFalse(invalid.ok);assertEquals(0.0,invalid.score);assertTrue(invalid.issues.any { it.type=="edge-intersects-obstacle" })
    }
    @Test fun missingPointsGroupCyclesAndOverlayBorderAreHandled() {
        val input=LayoutValidationInput(listOf(LayoutValidationNode("a",SceneRect(0.0,0.0,100.0,100.0),"b",isGroup=true),LayoutValidationNode("b",SceneRect(200.0,0.0,100.0,100.0),"a",isGroup=true)),listOf(
            LayoutValidationEdge("missing",emptyList()),
            LayoutValidationEdge("label",listOf(point(-50,50),point(50,50)),label="cross",labelBounds=SceneRect(-10.0,40.0,20.0,20.0)),
        ))
        val result=OrthogonalLayoutValidator.validate(input)
        assertTrue(result.issues.any { it.type=="edge-missing-points" });assertTrue(result.issues.any { it.type=="edge-label-overlaps-group-border" })
        assertEquals(1,result.breakdown.edgeCount)
    }
    @Test fun actualFlowCommandsSupplyFinalGeometryWithoutChangingDrawing() {
        val diagram=assertIs<MermaidParseResult.Success>(MermaidParser.parse("flowchart LR\nsubgraph group[Container]\nA[Start] -->|forward| B[End]\nB -->|return| A\nend\nA ~~~ C[Hidden link]")).diagram
        val measure=TextMeasurer { text,style->SceneSize(text.length*7.0,style.fontSize) }
        val plain=SimpleMermaidLayout.layout(diagram,measure,LayoutConfig())
        val checked=SimpleMermaidLayout.layout(diagram,measure,LayoutConfig(validateOrthogonalLayout=true))
        assertNull(plain.layoutValidation);assertEquals(plain.commands,checked.commands);assertEquals(plain.width,checked.width);assertEquals(plain.height,checked.height)
        val report=assertNotNull(checked.layoutValidation)
        assertEquals(2,report.geometry.edges.size)
        val lines=checked.commands.filterIsInstance<DrawLine>().map { listOf(it.from,it.to) }+checked.commands.filterIsInstance<DrawPolyline>().map { it.points }
        report.geometry.edges.forEach { assertTrue(it.points in lines);assertNotNull(it.labelBounds) }
        val boxes=checked.commands.filterIsInstance<DrawRect>().map { it.rect }
        report.geometry.nodes.forEach { assertTrue(it.bounds in boxes) }
        assertEquals("group",report.geometry.nodes.first { it.id=="A" }.parentId)
        assertNotNull(report.geometry.nodes.first { it.id=="group" }.groupTitleBounds)
        assertEquals(OrthogonalLayoutValidator.validate(report.geometry),report.result)
    }
}
