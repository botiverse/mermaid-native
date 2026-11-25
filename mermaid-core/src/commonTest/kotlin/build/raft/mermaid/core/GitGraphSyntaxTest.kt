package build.raft.mermaid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GitGraphSyntaxTest {
    @Test
    fun frontmatterMainBranchNameAndMultilineBranchNames() {
        val renamed = assertIs<GitGraphDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "---\nconfig:\n  gitGraph:\n    mainBranchName: 'trunk'\n---\ngitGraph\n  commit id: \"1\"\n  branch develop\n  commit id: \"2\"\n  checkout trunk\n  commit id: \"3\"",
        )).diagram)
        assertEquals(listOf("trunk", "develop"), renamed.branches.map { it.name })
        assertEquals(listOf("trunk", "develop", "trunk"), renamed.commits.map { it.branch })

        val multiline = assertIs<GitGraphDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "gitGraph LR:\n  commit id: \"1\"\n  branch \"Feature A\n(ongoing)\"\n  commit id: \"2\"",
        )).diagram)
        assertEquals("Feature A\n(ongoing)", multiline.branches.last().name)
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("gitGraph\n  commit msg: \"never closed"))
    }

    @Test
    fun recoveredSameLineStatementsRequireSeparatorsBeforeProductionResolution() {
        val source = """
            gitGraph
            commit id:"root" branch feature commit id:"feat" msg:"checkout main" checkout main commit id:"base" merge feature id:"merge"
        """.trimIndent()
        val recovered = GitGraphSyntaxParser.parse(source)
        assertEquals(listOf("Commit", "Branch", "Commit", "Checkout", "Commit", "Merge"), recovered.statements.map { it.type })
        assertTrue(recovered.diagnostics.isNotEmpty())
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source))
        val valid = """
            gitGraph
            commit id:"root"
            branch feature
            commit id:"feat" msg:"checkout main"
            checkout main
            commit id:"base"
            merge feature id:"merge"
        """.trimIndent()
        val diagram = assertIs<GitGraphDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(valid)).diagram)
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
