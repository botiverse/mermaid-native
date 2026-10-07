package build.raft.mermaid.web

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import build.raft.mermaid.layout.simple.*
import kotlin.test.*

class MermaidFrontmatterRenderingTest {
    @Test fun metadataTitleIsPaintedByBothConsumerPaths() {
        val source = "---\ntitle: 'Header & title'\n---\ngraph TD\nA-->B"
        val svg = assertIs<MermaidWebResult.Success>(MermaidWebAdapter.render(MermaidWebRequest(source))).svg
        val canvas = assertIs<MermaidWebCanvasResult.Success>(MermaidWebAdapter.renderCanvas(MermaidWebRequest(source))).script
        assertContains(svg, "Header &amp; title"); assertContains(canvas, "Header & title")
        assertTrue(Regex("<title[^>]*>Header &amp; title</title>").containsMatchIn(svg))
    }
    @Test fun valuesDisappearOnlyWhenConfiguredAndPlainRenderingRemainsStable() {
        fun render(prefix: String, body: String) = assertIs<MermaidWebResult.Success>(MermaidWebAdapter.render(MermaidWebRequest(prefix + body))).svg
        val sankey = "sankey\nA,B,17"
        val baseline = render("", sankey)
        assertContains(baseline, "A 17")
        val hidden = render("---\nconfig:\n  sankey:\n    showValues: false\n---\n", sankey)
        assertFalse(hidden.contains("A 17")); assertContains(hidden, ">A</text>")
        assertEquals(baseline, render("", sankey))
        val xy = "xychart-beta\nx-axis [A,B]\ny-axis 0 --> 100\nbar [37,83]"
        assertContains(render("", xy), ">37</text>")
        assertFalse(render("---\nconfig:\n  xyChart:\n    showDataLabel: false\n---\n", xy).contains(">37</text>"))
    }
    @Test fun declaredTitleWinsInActualPaint() {
        val source = "---\ntitle: Header\n---\nxychart-beta\ntitle \"Body\"\nx-axis [A,B]\nbar [1,2]"
        val svg = assertIs<MermaidWebResult.Success>(MermaidWebAdapter.render(MermaidWebRequest(source))).svg
        assertContains(svg, ">Body</text>"); assertFalse(svg.contains(">Header</text>"))
    }
    @Test fun titleTranslationKeepsGeometryAndGradientsInFinalSceneCoordinates() {
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse("---\ntitle: |\n  first\n  second\n---\ngraph LR\nA-->B"))
        val config = LayoutConfig(validateOrthogonalLayout = true)
        val plain = SimpleMermaidLayout.layout(parsed.diagram, FixedWidthTextMeasurer, config)
        val document = SimpleMermaidLayout.layout(parsed, FixedWidthTextMeasurer, config)
        val shift = document.height - plain.height
        assertTrue(shift > 0)
        val before = assertNotNull(plain.layoutValidation).geometry
        val after = assertNotNull(document.layoutValidation).geometry
        assertEquals(before.nodes.map { it.bounds.y + shift }, after.nodes.map { it.bounds.y })
        assertEquals(before.edges.map { e -> e.points.map { it.y + shift } }, after.edges.map { e -> e.points.map { it.y } })
        assertEquals(listOf("first", "second"), document.commands.take(2).map { assertIs<DrawText>(it).text })
    }
}
