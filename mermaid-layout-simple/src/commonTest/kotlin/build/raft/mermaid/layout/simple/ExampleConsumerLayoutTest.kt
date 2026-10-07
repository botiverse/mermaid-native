package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class ExampleConsumerLayoutTest {
    private val measure=TextMeasurer { text,style->SceneSize(text.length*style.fontSize*0.6,style.fontSize) }
    private fun scene(source:String)=SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)),measure,LayoutConfig())
    @Test fun seriesPaletteCyclesThroughActualPaintInBothOrientations() {
        for(orientation in listOf(""," horizontal")) {
            val s=scene("---\nconfig:\n  themeVariables:\n    xyChart: {plotColorPalette: '#2563eb, #dc2626'}\n---\nxychart-beta$orientation\nx-axis [A,B]\ny-axis 0 --> 100\nline [10,20]\nline [20,30]\nline [30,40]")
            assertEquals(listOf("#2563eb","#dc2626","#2563eb"),s.commands.filterIsInstance<DrawPolyline>().map { it.stroke.value })
        }
    }
    @Test fun treemapCurrencyAppliesToLeavesAndGroupSums() {
        val s=scene("---\nconfig:\n  treemap: {valueFormat: '$0,0'}\n---\ntreemap-beta\n\"Budget\"\n  \"Rent\": 1400\n  \"Bills\": 234.5")
        val values=s.commands.filterIsInstance<DrawText>().map { it.text }
        assertTrue("$1,400" in values);assertTrue("$234.5" in values);assertTrue("$1,634.5" in values)
        for((number,expected) in listOf(0.0 to "0",-12.5 to "−12.5",1.23e-7 to "1.23e-7",1e12 to "1e+12",1234567890123.0 to "1.23456789012e+12",999999999999.9 to "1e+12")) assertEquals(expected,formatTreemapValue(number,","))
    }
    @Test fun ticketHitRegionFollowsTextMetricsAndDocumentTitleShift() {
        val body="kanban\n Todo\n  task[Example]@{ticket: 123, priority: High, assigned: Owner, icon: star}"
        val config="config:\n  kanban: {ticketBaseUrl: 'https://example.com/#TICKET#'}"
        val plain=scene("---\n$config\n---\n$body");val titled=scene("---\ntitle: Board\n$config\n---\n$body")
        val a=plain.links.single();val b=titled.links.single()
        assertEquals("123",a.label);assertEquals("https://example.com/123",a.url)
        assertEquals(measure.measure("123",TextStyle(fontSize=10.0)).width,a.rect.width)
        assertEquals(titled.height-plain.height,b.rect.y-a.rect.y)
        assertTrue(plain.commands.filterIsInstance<DrawLine>().any { it.from.x==a.rect.x && it.to.x==a.rect.x+a.rect.width })
        val ticket = plain.commands.filterIsInstance<DrawText>().single { it.text == "123" }
        assertEquals(TextAnchor.START,ticket.anchor)
        assertEquals(a.rect.x,ticket.origin.x)
        assertTrue(ticket.origin.y in a.rect.y..(a.rect.y+a.rect.height))
        assertTrue(plain.commands.filterIsInstance<DrawText>().any { it.text == " · star" && it.origin.x >= a.rect.x+a.rect.width })
        assertTrue(scene(body).links.isEmpty())
    }
    @Test fun expandedShapesHaveDifferentRealOutlinesAndClipToThoseOutlines() {
        val r=SceneRect(0.0,0.0,120.0,72.0)
        for(shape in listOf(FlowNodeShape.MANUAL_INPUT,FlowNodeShape.DOCUMENTS,FlowNodeShape.PROCESSES)) {
            val polygons=expandedFlowPolygons(shape,r)
            assertEquals(if(shape==FlowNodeShape.MANUAL_INPUT)1 else 3,polygons.size)
            assertTrue(polygons.flatten().all { it.x in 0.0..120.0 && it.y in 0.0..72.0 })
            val diagram=FlowchartDiagram(FlowDirection.TD,listOf(FlowNode("A","A",shape),FlowNode("B","B")),listOf(FlowEdge("B","A")))
            val clipped=clipExpandedFlowShapes(diagram,mapOf("A" to r),mapOf(0 to listOf(ScenePoint(60.0,-50.0),ScenePoint(60.0,0.0)))).getValue(0).last()
            val hit=polygons.any { p->(p+p.first()).zipWithNext().any { (a,b)->
                val cross=(clipped.x-a.x)*(b.y-a.y)-(clipped.y-a.y)*(b.x-a.x)
                kotlin.math.abs(cross)<1e-6 && clipped.x>=minOf(a.x,b.x)-1e-6 && clipped.x<=maxOf(a.x,b.x)+1e-6 && clipped.y>=minOf(a.y,b.y)-1e-6 && clipped.y<=maxOf(a.y,b.y)+1e-6
            } }
            assertTrue(hit,"$shape endpoint $clipped")
        }
        val actual=scene("flowchart TD\nA@{shape: manual-input}\nB@{shape: docs}\nC@{shape: procs}\nA --> B\nB --> C")
        assertTrue(actual.commands.filterIsInstance<DrawPolygon>().size>=7)
    }
    @Test fun paintedConnectorEndpointsTouchTheVisibleExpandedOutlines() {
        val actual=scene("flowchart TD\nA@{shape: manual-input}\nB@{shape: docs}\nA --> B")
        val line=actual.commands.filterIsInstance<DrawLine>().single()
        val outlines=actual.commands.filterIsInstance<DrawPolyline>()
        fun touches(p:ScenePoint)=outlines.any { outline -> outline.points.zipWithNext().any { (a,b)->
            val cross=(p.x-a.x)*(b.y-a.y)-(p.y-a.y)*(b.x-a.x)
            kotlin.math.abs(cross)<1e-6 && p.x>=minOf(a.x,b.x)-1e-6 && p.x<=maxOf(a.x,b.x)+1e-6 && p.y>=minOf(a.y,b.y)-1e-6 && p.y<=maxOf(a.y,b.y)+1e-6
        } }
        assertTrue(touches(line.from));assertTrue(touches(line.to))
    }

}
