package build.raft.mermaid.core
import kotlin.test.*
class ClassStyleTest {
    private fun parse(source:String)=assertIs<ClassDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("classDiagram\n$source")).diagram)
    @Test fun classStylesAndInlineOverridesRetainDeclarationOrder() {
        val d=parse("class A~T~:::accent\nclass B\ncssClass \"A~T~,B\" bold\nclassDef accent fill:rgb(1,2,3),stroke:red\nclassDef bold font-weight:bold\nstyle A color:blue,font-size:20px")
        assertEquals(listOf("accent","bold"),d.classes.first().classes)
        assertEquals(listOf("bold"),d.classes.last().classes)
        assertEquals(listOf("fill:rgb(1,2,3)","stroke:red"),d.classDefinitions["accent"])
        assertEquals(listOf("color:blue","font-size:20px"),d.classes.first().styles)
    }
    @Test fun interactionMetadataPreservesLiteralCallbackArguments() {
        val d=parse("class A~T~\nclick A~T~ call send(\"a,b\", 3) \"Tip\"\nlink A \"https://example.test/a;b#c\" \"Docs\" _self")
        assertEquals(ClassInteraction("A","send",true,"\"a,b\", 3","Tip"),d.interactions.first())
        assertEquals(ClassInteraction("A","https://example.test/a;b#c",tooltip="Docs",target="_self"),d.interactions.last())
    }
    @Test fun unsupportedStyleAndMalformedInteractionFailClosed() {
        listOf("class A\nstyle A filter:blur(2px)","class A\nclick A call broken(","class A\nstyle A font-size:1em").forEach {
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("classDiagram\n$it"),it)
        }
    }
}
