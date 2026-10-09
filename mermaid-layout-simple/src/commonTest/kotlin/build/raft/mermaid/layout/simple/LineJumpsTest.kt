@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.io.encoding.Base64
import kotlin.math.PI
import kotlin.test.*

class LineJumpsTest {
    private fun p(x: Number, y: Number) = ScenePoint(x.toDouble(), y.toDouble())
    private val h = LineJumps.Edge("h", listOf(p(0, 5), p(20, 5)))
    private val v = LineJumps.Edge("v", listOf(p(10, 0), p(10, 20)))
    private val config = LineJumps.Config(radius = 1.0)
    @Test fun horizontalRuleAndReverseSweep() {
        for (edges in listOf(listOf(h, v), listOf(v, h))) {
            val hit = LineJumps.findEdgeIntersections(edges).single()
            assertEquals("h", hit.jumpEdgeId)
            assertEquals("M0,5 L9,5 A1,1 0 0 1 11,5 L20,5", LineJumps.processEdgesWithJumps(edges, config)["h"])
        }
        val reversed = h.copy(points = h.points.reversed())
        val geometry = LineJumps.processGeometry(listOf(reversed, v), config).getValue("h")
        assertEquals("M20,5 L11,5 A1,1 0 0 0 9,5 L0,5", geometry.d)
        assertEquals(-PI, geometry.segments.filterIsInstance<PathArc>().single().sweepAngle)
    }
    @Test fun gapsClampSortAndRejectJoins() {
        val a = v.copy(id = "a", points = listOf(p(9.5, 0), p(9.5, 20)))
        val b = v.copy(id = "b", points = listOf(p(10.5, 0), p(10.5, 20)))
        val path = LineJumps.processGeometry(listOf(b, h, a), config).getValue("h")
        assertEquals(listOf(0.5, 0.5), path.segments.filterIsInstance<PathArc>().map { it.radius })
        assertEquals(listOf(9.5, 10.5), path.segments.filterIsInstance<PathArc>().map { it.center.x })
        val gap = LineJumps.processGeometry(listOf(h, v), config.copy(style = LineJumps.Style.GAP)).getValue("h")
        assertEquals("M0,5 L9,5 M11,5 L20,5", gap.d)
        assertEquals(2, gap.segments.filterIsInstance<PathMove>().size)
        assertTrue(LineJumps.findEdgeIntersections(listOf(h, v.copy(points = listOf(p(10, 0), p(10, 5))))).isEmpty())
        assertEquals("M0,5 L20,5", LineJumps.processEdgesWithJumps(listOf(h, v), config.copy(enabled = false))["h"])
    }
    @Test fun renderedPointsOverrideLayoutAndMalformedDataFallsBack() {
        val encoded = Base64.Default.encode("[{\"x\":2,\"y\":6},{\"x\":18,\"y\":6}]".encodeToByteArray())
        val paths = listOf(LineJumps.RenderedPath("h", "M2,6 L18,6", encoded), LineJumps.RenderedPath("v", "M10,0 L10,20"))
        assertEquals("M2,6 L9,6 A1,1 0 0 1 11,6 L18,6", LineJumps.patchRenderedPaths(listOf(h, v), paths, config).single().geometry.d)
        for (bad in listOf("not-base64", Base64.Default.encode("{}".encodeToByteArray()), Base64.Default.encode("[{}]".encodeToByteArray()))) {
            assertTrue(LineJumps.patchRenderedPaths(listOf(h, v), listOf(paths[0].copy(dataPoints = bad), paths[1]), config).single().geometry.d.startsWith("M0,5"))
        }
        assertTrue(LineJumps.patchRenderedPaths(listOf(h, v), emptyList(), config).isEmpty())
    }
    @Test fun actualJumpingEdgeFiltersCurvesAndPreservesRoundedBends() {
        val paths = listOf(LineJumps.RenderedPath("h", "M0,5 C1,5 19,5 20,5"), LineJumps.RenderedPath("v", "M10,0 L10,20"))
        assertTrue(LineJumps.patchRenderedPaths(listOf(h, v), paths, config).isEmpty())
        assertTrue(LineJumps.patchRenderedPaths(listOf(h.copy(curve = "basis"), v), paths, config).isEmpty())
        val rounded = h.copy(points = h.points + p(20, 15), curve = "rounded")
        val patch = LineJumps.patchRenderedPaths(listOf(rounded, v), paths, config).single()
        assertContains(patch.geometry.d, "Q20,5 20,10")
        assertEquals(1, patch.geometry.segments.filterIsInstance<PathQuadratic>().size)
        assertTrue(LineJumps.curveSupportsLineHops("stepAfter"))
        assertFalse(LineJumps.curveSupportsLineHops("monotoneX"))
    }
    @Test fun actualJumpingEdgeUpdatesNeoDashAndMarkerEndpoints() {
        val paths = listOf(LineJumps.RenderedPath("h", "M0,5 L20,5", style = "stroke-dasharray: 0 2 15 3; stroke:#000;"), LineJumps.RenderedPath("v", "M10,0 L10,20"))
        var measured = false
        val result = LineJumps.patchRenderedPaths(listOf(h.copy(arrowTypeStart = "arrow_point", arrowTypeEnd = "arrow_point"), v), paths, config) { id, d ->
            measured = true; assertEquals("h", id); assertTrue(d.endsWith("L16,5")); 12.0 - 2.0 + PI
        }.single()
        assertTrue(measured)
        assertTrue(result.geometry.d.startsWith("M4,5"))
        assertContains(result.style, "stroke:#000;")
        val on = Regex("0 2 ([0-9.]+) 3").find(result.style)!!.groupValues[1].toDouble()
        assertEquals(5.0 + PI, on, 1e-12)
        assertEquals(paths[0].style, LineJumps.patchRenderedPaths(listOf(h, v), paths, config).single().style)
    }
    @Test fun negativeRoundingAndCrowdedCrossings() {
        val negative = LineJumps.Edge("n", listOf(p(-1.2345, -0.0005), p(10, 0)))
        assertEquals("M-1.234,0 L10,0", LineJumps.processEdgesWithJumps(listOf(negative), config)["n"])
        val near = v.copy(id = "near", points = listOf(p(10.0001, 0), p(10.0001, 20)))
        assertTrue(LineJumps.processGeometry(listOf(h, v, near), config).getValue("h").segments.none { it is PathArc })
        assertFailsWith<IllegalArgumentException> { LineJumps.Config(radius = Double.NaN) }
    }
    @Test fun registeredEdgesPreserveDecorationsStylesAndMarkers() {
        val polygon = DrawPolyline(listOf(p(10, 0), p(10, 20)))
        val commands = mutableListOf<DrawCommand>(DrawLine(h.points[0], h.points[1], SceneColor("#aabbcc"), 3.0, StrokePattern.DASHED), DrawLine(v.points[0], v.points[1]), polygon)
        applySwimlaneHops(commands, listOf(0, 1), SwimlaneLineHops.ARC)
        val path = assertIs<DrawPath>(commands[0]); assertEquals(StrokePattern.DASHED, path.pattern)
        assertEquals("#aabbcc", path.stroke.value); assertEquals(3.0, path.strokeWidth)
        assertEquals(h.points.first(), (path.segments.first() as PathMove).to)
        assertEquals(h.points.last(), (path.segments.last() as PathLine).to)
        assertSame(polygon, commands[2])
    }
    @Test fun frontmatterAdmissionAndBoundedProductionPaths() {
        val body = "swimlane-beta\nsubgraph L1\nA\nB\nend\nsubgraph L2\nC\nD\nend\nA-->D\nB-->C"
        fun diagram(config: String) = assertIs<MermaidParseResult.Success>(MermaidParser.parse("---\nconfig:\n  swimlane:\n    lineHops: $config\n---\n$body")).diagram
        val off = SimpleMermaidLayout.layout(diagram("false"), FixedWidthTextMeasurer, LayoutConfig())
        val arc = SimpleMermaidLayout.layout(diagram("arc"), FixedWidthTextMeasurer, LayoutConfig())
        val gap = SimpleMermaidLayout.layout(diagram("gap"), FixedWidthTextMeasurer, LayoutConfig())
        assertTrue(off.commands.none { it is DrawPath })
        assertTrue(arc.commands.filterIsInstance<DrawPath>().any { it.segments.any { segment -> segment is PathArc } })
        assertTrue(gap.commands.filterIsInstance<DrawPath>().any { it.segments.filterIsInstance<PathMove>().size > 1 })
        assertEquals(off.commands.filterIsInstance<DrawPolygon>(), arc.commands.filterIsInstance<DrawPolygon>())
        assertEquals(SwimlaneLineHops.ARC, assertIs<SwimlaneDiagram>(diagram("true")).lineHops)
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("---\nconfig:\n  swimlane:\n    lineHops: invalid\n---\n$body"))
    }
    @Test fun styledLanesHonorSwitchAndKeepMarkersAndSceneBounds() {
        val body = "swimlane-beta\nsubgraph L1\nA\nB\nC\nend\nsubgraph L2\nD\nE\nF\nend\nA-->F\nB-->D\nC-->E\nF-->B\nD-->C\nE-->A\nstyle A fill:#abcdef"
        val diagram = assertIs<SwimlaneDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(body)).diagram)
        fun layout(d: MermaidDiagram) = SimpleMermaidLayout.layout(d, FixedWidthTextMeasurer, LayoutConfig())
        val off = layout(diagram.copy(lineHops = SwimlaneLineHops.DISABLED))
        val arc = layout(diagram)
        val gap = layout(diagram.copy(lineHops = SwimlaneLineHops.GAP))
        assertTrue(off.commands.none { it is DrawPath })
        // Cross-lane handoffs use their facing sides instead of exterior Flow return tracks.
        assertEquals(5, arc.commands.filterIsInstance<DrawPath>().size)
        assertEquals(7, arc.commands.filterIsInstance<DrawPath>().sumOf { p -> p.segments.count { it is PathArc } })
        assertEquals(listOf(2, 2, 2, 3, 3), gap.commands.filterIsInstance<DrawPath>().map { p -> p.segments.count { it is PathMove } })
        assertTrue(gap.commands.filterIsInstance<DrawPath>().all { path -> path.segments.filterIsInstance<PathMove>().size > 1 })
        // Arc ink can expand the padded scene; every marker and label must share that translation.
        val beforeText = off.commands.filterIsInstance<DrawText>()
        val afterText = arc.commands.filterIsInstance<DrawText>()
        val dx = afterText.first().origin.x - beforeText.first().origin.x
        val dy = afterText.first().origin.y - beforeText.first().origin.y
        assertEquals(beforeText, afterText.map { it.copy(origin = ScenePoint(it.origin.x - dx, it.origin.y - dy)) })
        assertEquals(off.commands.filterIsInstance<DrawPolygon>(), arc.commands.filterIsInstance<DrawPolygon>().map { polygon ->
            polygon.copy(points = polygon.points.map { ScenePoint(it.x - dx, it.y - dy) })
        })
        for (path in arc.commands.filterIsInstance<DrawPath>()) {
            val bounds = path.conservativeBounds()!!
            assertTrue(bounds.x >= 0 && bounds.y >= 0 && bounds.x + bounds.width <= arc.width && bounds.y + bounds.height <= arc.height)
        }
        assertTrue(layout(diagram.flowchart!!).commands.none { it is DrawPath })
    }
}
