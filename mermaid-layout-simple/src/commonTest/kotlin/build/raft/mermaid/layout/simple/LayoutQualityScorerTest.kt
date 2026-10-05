package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class LayoutQualityScorerTest {
    @Test fun undefinedMetricsNeverPassThresholds() {
        val r=LayoutQualityScorer.score(LayoutValidationInput(emptyList(),emptyList()),mapOf("symmetryScore" to LayoutQualityThreshold(max=1.0),"edgeLengthRatio" to LayoutQualityThreshold(min=0.0)))
        assertTrue(r.scores.symmetryScore.isNaN());assertTrue(r.scores.edgeLengthRatio.isNaN())
        assertTrue(r.thresholdResults!!.values.none { it.pass });assertEquals(0,r.scores.totalBends)
    }
    @Test fun topologyRanksUseRootsAndRetainTiedRanks() {
        val nodes=listOf("A","B","C").mapIndexed { i,id->LayoutValidationNode(id,SceneRect(0.0,i*100.0,40.0,40.0)) }
        val edges=listOf(LayoutValidationEdge("ab",listOf(ScenePoint(20.0,40.0),ScenePoint(20.0,100.0)),"A","B"),LayoutValidationEdge("bc",listOf(ScenePoint(20.0,140.0),ScenePoint(20.0,200.0)),"B","C"))
        val input=LayoutValidationInput(nodes,edges)
        assertEquals(1.0,LayoutQualityScorer.score(input).scores.rankFaithfulness)
        val cycle=input.copy(edges=edges+LayoutValidationEdge("ca",listOf(ScenePoint(20.0,240.0),ScenePoint(20.0,0.0)),"C","A"))
        assertTrue(LayoutQualityScorer.score(cycle).scores.rankFaithfulness.isNaN())
    }
    @Test fun actualFlowDiagnosticCarriesQualityOfItsMeasuredGeometry() {
        val d=assertIs<MermaidParseResult.Success>(MermaidParser.parse("flowchart TD\nA --> B\nB --> C")).diagram
        val s=SimpleMermaidLayout.layout(d,TextMeasurer { t,st->SceneSize(t.length*7.0,st.fontSize) },LayoutConfig(validateOrthogonalLayout=true))
        val report=assertNotNull(s.layoutValidation);val quality=assertNotNull(report.quality)
        val expected=LayoutQualityScorer.score(report.geometry)
        assertEquals(expected.scores.edgeLengthRatio,quality.scores.edgeLengthRatio)
        assertEquals(expected.scores.totalBends,quality.scores.totalBends)
        assertEquals(1.0,quality.scores.rankFaithfulness)
    }
}
