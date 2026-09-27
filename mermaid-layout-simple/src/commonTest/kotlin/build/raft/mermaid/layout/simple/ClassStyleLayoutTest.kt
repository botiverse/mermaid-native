package build.raft.mermaid.layout.simple
import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*
class ClassStyleLayoutTest {
    private fun layout(source:String)=SimpleMermaidLayout.layout(assertIs<MermaidParseResult.Success>(MermaidParser.parse("classDiagram\n$source")).diagram,FixedWidthTextMeasurer,LayoutConfig())
    @Test fun stylesReachShapesTextAndMeasuredBounds() {
        val s=layout("class LongClassName:::accent {\n+call()\n}\nclassDef accent fill:rgb(1,2,3),stroke:red,color:blue,font-size:28px\nstyle LongClassName stroke-width:3,font-weight:bold")
        val box=s.commands.filterIsInstance<DrawRect>().single()
        assertEquals("#010203",box.fill.value);assertEquals("#ff0000",box.stroke.value);assertEquals(3.0,box.strokeWidth)
        val text=s.commands.filterIsInstance<DrawText>()
        assertTrue(text.all { it.style.fontSize==28.0 && it.style.fontWeight==700 && it.style.color.value=="#0000ff" })
        text.forEach { assertTrue(it.origin.x+FixedWidthTextMeasurer.measure(it.text,it.style).width<=box.rect.x+box.rect.width) }
        val normal=layout("class LongClassName {\n+call()\n}").commands.filterIsInstance<DrawRect>().single()
        assertTrue(box.rect.width>normal.rect.width && box.rect.height>normal.rect.height)
    }
    @Test fun inlineNoneOverridesDefaultFillWithoutChangingOtherClasses() {
        val s=layout("class A\nclass B\nclassDef default fill:red\nstyle A fill:none")
        assertEquals(listOf("none","#ff0000"),s.commands.filterIsInstance<DrawRect>().map { it.fill.value })
    }
}
