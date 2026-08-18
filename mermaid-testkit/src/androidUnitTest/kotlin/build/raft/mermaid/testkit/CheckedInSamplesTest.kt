package build.raft.mermaid.testkit

import build.raft.mermaid.core.MermaidParseResult
import build.raft.mermaid.core.MermaidParser
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
        }
    }
}
