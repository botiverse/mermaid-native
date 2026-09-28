package build.raft.mermaid.core
import kotlin.test.*
class FlowStyleTest {
    private fun parse(s:String)=assertIs<FlowchartDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("graph LR\n$s")).diagram)
    @Test fun styleBeforeDefinitionPreservesLabelAndInlineClasses() {
        val d=parse("style A fill:rgb(1,2,3)\nA[Named]:::accent --> B\nclassDef accent color: blue,font-size:150%\nclass A,B second")
        assertEquals("Named",d.nodes.first().label)
        assertEquals(listOf("fill:rgb(1,2,3)"),d.nodes.first().styles)
        assertEquals(listOf("accent","second"),d.nodes.first().classes)
        assertEquals(listOf("box"),parse("A:::box-->B").nodes.first().classes)
        assertEquals(listOf("color: blue","font-size:150%"),d.classDefinitions["accent"])
    }
    @Test fun callbacksAndLinksStayLiteralData() {
        val d=parse("A-->B\nclick A call notify(\"a,b\", 4) \"Tip\"\nclick B href \"https://example.test\" _blank")
        assertEquals(FlowInteraction("A","notify",true,"\"a,b\", 4","Tip"),d.interactions.first())
        assertEquals("_blank",d.interactions.last().target)
    }
    @Test fun edgeStylesAndInvalidIndicesAreExplicit() {
        val d=parse("A-->B\nlinkStyle default stroke:blue\nlinkStyle 0 stroke-width:3px")
        assertEquals(listOf("stroke:blue"),d.defaultEdgeStyles)
        assertEquals(listOf("stroke-width:3px"),d.edges.single().styles)
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("graph LR;A-->B;linkStyle 1 stroke:red"))
    }
}
