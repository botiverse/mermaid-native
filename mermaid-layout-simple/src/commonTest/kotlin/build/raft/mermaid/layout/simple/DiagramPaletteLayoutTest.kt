package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.test.*

class DiagramPaletteLayoutTest {
    private fun scene(source: String) = SimpleMermaidLayout.layout(
        assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram, FixedWidthTextMeasurer, LayoutConfig())

    @Test fun neutralNodeFamiliesShareTheSameSurfaceAndReadableText() {
        for (source in listOf("flowchart LR\nA[Account] --> B[Invoice]", "classDiagram\nclass Account", "erDiagram\nACCOUNT ||--o{ INVOICE : has", "sequenceDiagram\nAccount->>Invoice: create")) {
            val result = scene(source)
            assertTrue(result.commands.filterIsInstance<DrawRect>().any { it.fill.value == DiagramPalette.SURFACE }, source)
            assertTrue(result.commands.filterIsInstance<DrawText>().any { it.style.color.value == DiagramPalette.INK }, source)
        }
    }

    @Test fun explicitStylesKeepTheirLiteralColorsEvenWhenTheyMatchOldDefaults() {
        for (source in listOf(
            "flowchart LR\nA[Account]\nstyle A fill:#2563eb,stroke:#16a34a,color:#111827",
            "classDiagram\nclass Account\nstyle Account fill:#2563eb,stroke:#16a34a,color:#111827",
            "erDiagram\nACCOUNT\nstyle ACCOUNT fill:#2563eb,stroke:#16a34a,color:#111827",
        )) {
            val result = scene(source)
            assertTrue(result.commands.filterIsInstance<DrawRect>().any { it.fill.value == "#2563eb" && it.stroke.value == "#16a34a" }, source)
            assertTrue(result.commands.filterIsInstance<DrawText>().any { it.style.color.value == "#111827" }, source)
        }
        val region = scene("sequenceDiagram\nrect red\nA->>B: message\nend")
        assertTrue(region.commands.filterIsInstance<DrawRect>().any { it.fill.value == "#ff0000" })
    }

    @Test fun journeyScoresRemainSixDistinctLevelsAfterPaletteUnification() {
        val result = scene("journey\nsection Scores\n"+(0..5).joinToString("\n") { "Task$it: $it: User" })
        val colors = result.commands.filterIsInstance<DrawRect>().map { it.fill.value }.toSet()
        val scores = listOf(DiagramPalette.RED_SOFT, DiagramPalette.RED_SURFACE, DiagramPalette.PEACH_SURFACE,
            DiagramPalette.AMBER_SURFACE, DiagramPalette.GREEN_SURFACE, DiagramPalette.GREEN_STRONG)
        assertEquals(6, scores.distinct().size)
        assertTrue(colors.containsAll(scores))
    }
}
