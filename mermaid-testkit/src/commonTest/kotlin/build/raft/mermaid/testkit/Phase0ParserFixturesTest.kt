package build.raft.mermaid.testkit

import build.raft.mermaid.core.MermaidParseResult
import build.raft.mermaid.core.MermaidParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Phase0ParserFixturesTest {
    @Test
    fun pinnedReferenceFixturesMatchNormalizedAst() {
        Phase0ParserFixtures.all.forEach { fixture ->
            val success = assertIs<MermaidParseResult.Success>(
                MermaidParser.parse(fixture.source),
                fixture.name,
            )
            assertEquals(fixture.expected, success.diagram, fixture.name)
        }
    }
}
