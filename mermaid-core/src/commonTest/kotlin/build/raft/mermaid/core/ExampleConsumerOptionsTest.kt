package build.raft.mermaid.core

import kotlin.test.*

class ExampleConsumerOptionsTest {
    private fun parse(s:String)=assertIs<MermaidParseResult.Success>(MermaidParser.parse(s)).diagram
    @Test fun expandedShapeAliasesRetainDistinctKinds() {
        for((aliases,shape) in listOf(
            listOf("sl-rect","manual-input","sloped-rectangle") to FlowNodeShape.MANUAL_INPUT,
            listOf("docs","documents","st-doc","stacked-document") to FlowNodeShape.DOCUMENTS,
            listOf("st-rect","procs","processes","stacked-rectangle") to FlowNodeShape.PROCESSES,
        )) for(alias in aliases) assertEquals(shape,assertIs<FlowchartDiagram>(parse("flowchart TD\nA@{shape: $alias, label: Form}")).nodes.single().shape)
    }
    @Test fun configCombinesBothXySectionsInEitherOrderAndDoesNotLeak() {
        val sections=listOf("  xyChart: {showDataLabel: false}","  themeVariables:\n    xyChart: {plotColorPalette: '#2563eb, #dc2626'}")
        for(items in listOf(sections,sections.reversed())) {
            val d=assertIs<XyChartDiagram>(parse("---\nconfig:\n${items.joinToString("\n")}\n---\nxychart-beta\nbar [1,2]"))
            assertFalse(d.showDataLabel);assertEquals(listOf("#2563eb","#dc2626"),d.plotColorPalette)
        }
        assertTrue(assertIs<XyChartDiagram>(parse("xychart-beta\nbar [1,2]")).plotColorPalette.isEmpty())
    }
    @Test fun ticketSubstitutionUsesSafeLinksOnly() {
        val d=assertIs<KanbanDiagram>(parse("---\nconfig:\n  kanban: {ticketBaseUrl: 'https://example.com/#TICKET#?q=#TICKET#'}\n---\nkanban\n Todo\n  A[Task]@{ticket: 123}"))
        assertEquals("https://example.com/123?q=#TICKET#",d.ticketUrl("123"))
        assertNull(d.ticketUrl("bad\nvalue"))
        for(link in listOf("javascript:alert(1)","https://user@host/"))
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("---\nconfig:\n  kanban: {ticketBaseUrl: '$link'}\n---\nkanban\n Todo"))
    }
    @Test fun valuesNativeCannotUseKeepTheDefault() {
        assertNull(assertIs<TreemapDiagram>(parse("---\nconfig:\n  treemap: {valueFormat: '.2f'}\n---\ntreemap-beta\n\"A\": 1")).valueFormat)
        for(palette in listOf("#12345","#123,","red"))
            assertTrue(assertIs<XyChartDiagram>(parse("---\nconfig:\n  themeVariables: {xyChart: {plotColorPalette: '$palette'}}\n---\nxychart-beta\nbar [1,2]")).plotColorPalette.isEmpty())
    }
}
