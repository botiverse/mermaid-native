package build.raft.mermaid.testkit

import build.raft.mermaid.core.MermaidParseResult
import build.raft.mermaid.core.MermaidParser
import build.raft.mermaid.layout.simple.FixedWidthTextMeasurer
import build.raft.mermaid.layout.simple.SimpleMermaidLayout
import build.raft.mermaid.render.svg.SvgRenderer
import build.raft.mermaid.layout.LayoutConfig
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CheckedInSamplesTest {
    @Test
    fun sampleFilesParseToTheirNormalizedAst() {
        val workingDirectory = System.getProperty("user.dir")
            ?: error("The test process did not expose user.dir")
        val repositoryRoot = generateSequence(File(workingDirectory)) { it.parentFile }
            .firstOrNull { File(it, "samples").isDirectory }
            ?: error("Could not locate repository samples directory")

        MermaidExamples.all.forEach { example ->
            val file = File(repositoryRoot, example.path)
            assertTrue(file.isFile, "Missing checked-in example: ${example.path}")
            val success = assertIs<MermaidParseResult.Success>(
                MermaidParser.parse(file.readText()),
                example.path,
            )
            assertEquals(example.expected, success.diagram, example.path)

            val svg = SvgRenderer.render(
                SimpleMermaidLayout.layout(success.diagram, FixedWidthTextMeasurer, LayoutConfig()),
            )
            val golden = File(repositoryRoot, example.path.removeSuffix(".mmd") + ".svg")
            if (System.getenv("UPDATE_MERMAID_GOLDENS") == "true") golden.writeText(svg)
            assertTrue(golden.isFile, "Missing generated SVG golden: ${golden.path}")
            assertEquals(golden.readText(), svg, "SVG drift: ${example.path}")
        }
    }
}
