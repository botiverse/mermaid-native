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
    @Test fun ticketSubstitutionAndUnsupportedOptionsStayExplicit() {
        val d=assertIs<KanbanDiagram>(parse("---\nconfig:\n  kanban: {ticketBaseUrl: 'https://example.com/#TICKET#?q=#TICKET#'}\n---\nkanban\n Todo\n  A[Task]@{ticket: 123}"))
        assertEquals("https://example.com/123?q=#TICKET#",d.ticketUrl("123"))
        assertNull(d.ticketUrl("bad\nvalue"))
        val bad=listOf("kanban: {ticketBaseUrl: 'javascript:alert(1)'}","kanban: {ticketBaseUrl: 'https://user@host/'}","treemap: {valueFormat: '.2f'}","themeVariables: {xyChart: {plotColorPalette: '#12345'}}","themeVariables: {xyChart: {plotColorPalette: '#123,'}}")
        for(config in bad) {
            val body=when { config.startsWith("themeVariables") -> "xychart-beta\nbar [1,2]"; config.startsWith("treemap") -> "treemap-beta\n\"A\": 1"; else -> "kanban\n Todo" }
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("---\nconfig:\n  $config\n---\n$body"))
        }
    }
}
