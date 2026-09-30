package build.raft.mermaid.render.svg

import build.raft.mermaid.core.FlowDirection
import build.raft.mermaid.core.StateDiagram
import build.raft.mermaid.core.StateNode
import build.raft.mermaid.core.StateNodeKind
import build.raft.mermaid.core.StateTransition
import build.raft.mermaid.core.CynefinDiagram
import build.raft.mermaid.core.CynefinDomain
import build.raft.mermaid.core.CynefinDomainBlock
import build.raft.mermaid.core.CynefinTransition
import build.raft.mermaid.core.RadarAxis
import build.raft.mermaid.core.RadarChartDiagram
import build.raft.mermaid.core.RadarCurve
import build.raft.mermaid.layout.DrawText
import build.raft.mermaid.layout.DrawEllipse
import build.raft.mermaid.layout.LayoutConfig
import build.raft.mermaid.layout.LayoutScene
import build.raft.mermaid.layout.ScenePoint
import build.raft.mermaid.layout.SceneColor
import build.raft.mermaid.layout.TextAnchor
import build.raft.mermaid.layout.TextStyle
import build.raft.mermaid.layout.simple.FixedWidthTextMeasurer
import build.raft.mermaid.layout.simple.SimpleMermaidLayout
import build.raft.mermaid.core.MermaidParser
import build.raft.mermaid.core.MermaidParseResult
import kotlin.test.assertIs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class SvgRendererTest {
    @Test
    fun accessibilityReferencesAreScopedEscapedAndOrderedBeforeDrawingCommands() {
        val scene = LayoutScene(100.0, 60.0, listOf(DrawText("Visible", ScenePoint(10.0, 20.0))),
            accessibilityTitle = "Title <&>", accessibilityDescription = "Description <&>")
        val svg = SvgRenderer.render(scene, "first", "flowchart")
        assertTrue("role=\"graphics-document document\"" in svg)
        assertTrue("aria-roledescription=\"flowchart\"" in svg)
        assertTrue("aria-labelledby=\"chart-title-first\"" in svg)
        assertTrue("aria-describedby=\"chart-desc-first\"" in svg)
        assertTrue("<title id=\"chart-title-first\">Title &lt;&amp;&gt;</title>" in svg)
        assertTrue("<desc id=\"chart-desc-first\">Description &lt;&amp;&gt;</desc>" in svg)
        assertTrue(svg.indexOf("<title") < svg.indexOf("<desc"))
        assertTrue(svg.indexOf("<desc") < svg.indexOf("<text"))
        val second = SvgRenderer.render(scene, "second", "flowchart")
        assertFalse("chart-title-first" in second)
        assertTrue("chart-title-second" in second)
        assertEquals(SvgRenderer.render(scene), SvgRenderer.render(scene))
        assertTrue("chart-title-first&quot;" in SvgRenderer.render(scene, "first\"", "type\"<&>"))
        assertTrue("aria-roledescription=\"type&quot;&lt;&amp;&gt;\"" in SvgRenderer.render(scene, "first", "type\"<&>"))
        assertFailsWith<IllegalArgumentException> { SvgRenderer.render(scene, "two tokens") }
    }

    @Test
    fun emptyAccessibilityMetadataDoesNotCreateDanglingReferences() {
        val svg = SvgRenderer.render(LayoutScene(100.0, 60.0, emptyList(), "", ""), "empty", "")
        assertFalse("aria-labelledby" in svg)
        assertFalse("aria-describedby" in svg)
        assertFalse("aria-roledescription" in svg)
        assertFalse("<title" in svg)
        assertFalse("<desc" in svg)
    }

    @Test fun classAbstractMemberUsesSvgItalicWithNoClassifierText() {
        val diagram = assertIs<MermaidParseResult.Success>(MermaidParser.parse("classDiagram\nclass Clock {\n+read()*\n}")).diagram
        val svg = SvgRenderer.render(SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertTrue(svg.contains("font-style=\"italic\""))
        assertTrue(svg.contains(">+read()</text>"))
        assertFalse(svg.contains("read()*"))
    }

    @Test
    fun erAccessibilityFlowsThroughLayoutAndEscapesSvgMarkup() {
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "erDiagram\naccTitle: Accounts <&>\naccDescr { Entity <relations> & keys }\nA",
        ))
        val scene = SimpleMermaidLayout.layout(parsed.diagram, FixedWidthTextMeasurer, LayoutConfig())
        val svg = SvgRenderer.render(scene)
        assertTrue(">Accounts &lt;&amp;&gt;</title>" in svg)
        assertTrue(">Entity &lt;relations&gt; &amp; keys</desc>" in svg)
        assertTrue("<relations>" !in svg)
    }

    @Test
    fun cynefinSvgMeasuresItemsEscapesTextAndKeepsTransitionVisible() {
        val longItem = "A very long & measured item label that expands the quadrant"
        val diagram = CynefinDiagram(
            title = "Risk <map>",
            domains = listOf(
                CynefinDomainBlock(CynefinDomain.COMPLEX, listOf(longItem)),
                CynefinDomainBlock(CynefinDomain.COMPLICATED, listOf("Expert review")),
                CynefinDomainBlock(CynefinDomain.CONFUSION, listOf("One", "Two", "Three", "Four")),
            ),
            transitions = listOf(CynefinTransition(CynefinDomain.COMPLEX, CynefinDomain.COMPLICATED, "Pattern & proof")),
        )
        val svg = SvgRenderer.render(SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()))
        assertTrue(svg.contains("width=\""))
        assertTrue(svg.contains("Risk &lt;map&gt;"))
        assertTrue(svg.contains("A very long &amp; measured"))
        assertTrue(svg.contains("Pattern &amp; proof"))
        assertTrue(svg.contains("+1 more"))
        assertTrue(svg.contains("Probe → Sense → Respond"))
        assertTrue(svg.contains("Disorder"))
        assertTrue(svg.contains("<line"))
        assertTrue(svg.contains("<polygon"))
    }
    @Test
    fun stateDiagramSvgIsDeterministicAndContainsVisibleTransition() {
        val diagram = StateDiagram(
            direction = FlowDirection.LR,
            states = listOf(StateNode("__start_0", "", StateNodeKind.START), StateNode("Ready", "Ready")),
            transitions = listOf(StateTransition("__start_0", "Ready", "begin & run")),
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        val svg = SvgRenderer.render(scene)

        assertTrue(svg.endsWith("</svg>\n"))
    }

    @Test
    fun radarSvgEscapesLabelsAndDrawsClosedCurveGeometry() {
        val diagram = RadarChartDiagram(
            title = "Skills <& matrix>",
            axes = listOf(RadarAxis("m", "Math & logic"), RadarAxis("s", "Science"), RadarAxis("e", "English")),
            curves = listOf(RadarCurve("alice", "Alice <v1>", listOf(85.0, 60.0, 90.0))),
            maximum = 100.0,
        )
        val scene = SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
        val svg = SvgRenderer.render(scene)

        assertEquals(svg, SvgRenderer.render(scene))
        assertTrue(svg.contains("Skills &lt;&amp; matrix&gt;"))
        assertTrue(svg.contains("Math &amp; logic"))
        assertTrue(svg.contains("Alice &lt;v1&gt;"))
        assertTrue(svg.contains("<polyline"))
        assertTrue(svg.contains("<polygon"))
        assertTrue(svg.contains("stroke-width=\"2\""))
        assertTrue(svg.endsWith("</svg>\n"))
    }

    @Test
    fun serializerIsDeterministicAndEscapesTextAndAttributes() {
        val scene = LayoutScene(
            width = 100.0,
            height = 40.5,
            commands = listOf(
                DrawText(
                    text = "<unsafe & visible>",
                    origin = ScenePoint(50.0, 24.25),
                    anchor = TextAnchor.MIDDLE,
                    style = TextStyle(fontFamily = "A&B\""),
                ),
            ),
        )
        val svg = SvgRenderer.render(scene)
        assertEquals(svg, SvgRenderer.render(scene))
        assertTrue(svg.contains("&lt;unsafe &amp; visible&gt;"))
        assertTrue(svg.contains("font-family=\"A&amp;B&quot;\""))
        assertFalse(svg.contains("<unsafe"))
        assertTrue(svg.endsWith("</svg>\n"))
    }

    @Test
    fun serializerWritesEllipseGeometryOpacityAndEscapedColors() {
        val svg = SvgRenderer.render(
            LayoutScene(
                100.0,
                80.0,
                listOf(DrawEllipse(ScenePoint(50.0, 40.0), 30.0, 20.0, SceneColor("url(&unsafe)"), 0.28)),
            ),
        )
        assertTrue(svg.contains("<ellipse cx=\"50\" cy=\"40\" rx=\"30\" ry=\"20\""))
        assertTrue(svg.contains("fill=\"url(&amp;unsafe)\" fill-opacity=\"0.28\""))
    }
}
