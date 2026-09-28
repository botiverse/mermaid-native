package build.raft.mermaid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GitGraphSyntaxTest {
    @Test
    fun productionHistoryConsumesEveryStatementOnTheSameLine() {
        val source = """
            gitGraph
            commit id:"root" branch feature commit id:"feat" msg:"checkout main" checkout main commit id:"base" merge feature id:"merge"
        """.trimIndent()
        val diagram = assertIs<GitGraphDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
        assertEquals(listOf("root", "feat", "base", "merge"), diagram.commits.map { it.id })
        assertEquals(listOf("base", "feat"), diagram.commits.last().parentIds)
        assertEquals("checkout main", diagram.commits[1].message)
        assertEquals("main", diagram.currentBranch)
    }

    @Test
    fun recoveredSyntaxNeverAdmitsInvalidProductionHistory() {
        val malformed = "gitGraph\ncommit id:\"one\" msg:\"kept\" unknown:\"bad\"\ncommit id:\"two\""
        val syntax = GitGraphSyntaxParser.parse(malformed)
        assertEquals(listOf("one", "two"), syntax.statements.map { it.id })
        assertEquals("kept", syntax.statements.first().message)
        assertTrue(syntax.diagnostics.isNotEmpty())
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse(malformed))
        val unresolved = "gitGraph\ncheckout missing"
        assertTrue(GitGraphSyntaxParser.parse(unresolved).diagnostics.isEmpty())
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse(unresolved))
    }
}
