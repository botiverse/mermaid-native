package build.raft.mermaid.testkit

import build.raft.mermaid.core.MermaidParseResult
import build.raft.mermaid.core.MermaidParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MermaidExamplesTest {
    @Test
    fun checkedInExamplesParseToTheirNormalizedAst() {
        MermaidExamples.all.forEach { example ->
            val success = assertIs<MermaidParseResult.Success>(
                MermaidParser.parse(example.source),
                example.path,
            )
            assertEquals(example.expected, success.diagram, example.path)
        }
    }
}
