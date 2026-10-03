package build.raft.mermaid.core

import kotlin.test.*

class UsecaseLexerTest {
    @Test fun tokenSpansRetainPhysicalNewlinesAndUtf16() {
        val source = "actor Person\r\n  %% comment\r日本語 😀\n"
        val result = UsecaseLexer.tokenize(source)
        assertTrue(result.errors.isEmpty())
        result.tokens.forEach { assertEquals(it.image, source.substring(it.startOffset, it.endOffset + 1)) }
        val unicode = result.tokens.first { it.image == "日本語" }
        assertEquals(3, unicode.startLine); assertEquals(1, unicode.startColumn)
        val emoji = result.tokens.first { it.image == "😀" }
        assertEquals(2, emoji.endOffset - emoji.startOffset + 1)
        assertEquals(5, emoji.startColumn); assertEquals(6, emoji.endColumn)
        assertEquals(listOf("\r\n", "\r", "\n"), result.tokens.filter { it.name == "NEWLINE" }.map { it.image })
    }
    @Test fun jsonAndMarkdownDoNotLeakModeOrTreatStringsAsComments() {
        val source = "json P@{\"x\":\"}\\\"{\",\"nested\":{}}:::data\n\"`%% text\nlast`\""
        val result = UsecaseLexer.tokenize(source)
        assertTrue(result.errors.isEmpty())
        assertEquals(listOf("JSON_DECLARATION_START", "JSON_OBJECT_LITERAL", "CLASS_SEPARATOR", "IDENTIFIER", "NEWLINE", "MARKDOWN_STRING"), result.tokens.map { it.name })
        assertEquals(2, result.tokens.last().startLine); assertEquals(3, result.tokens.last().endLine)
    }
    @Test fun largeUnclosedInputsAreConsumedOnceAndRecoverableErrorsRemainErrors() {
        for ((source, name) in listOf("\"`x".repeat(32000) to "UNCLOSED_MARKDOWN_STRING", "json P@{" + "{".repeat(32000) to "UNCLOSED_JSON_OBJECT_LITERAL")) {
            val result = UsecaseLexer.tokenize(source)
            assertTrue(result.errors.isEmpty()); assertEquals(name, result.tokens.last().name)
            assertEquals(source.lastIndex, result.tokens.last().endOffset)
            assertTrue(result.tokens.size <= 2)
        }
        val stereotypes = UsecaseLexer.tokenize("<<x\n".repeat(8000))
        assertTrue(stereotypes.errors.isEmpty())
        assertEquals(24000, stereotypes.tokens.size)
        val recovery = UsecaseLexer.tokenize("<<" + "\n".repeat(32000) + "body")
        assertEquals(32000, recovery.errors.single().length)
        assertEquals("UNCLOSED_STEREOTYPE_TEXT", recovery.tokens.last().name)
        val result = UsecaseLexer.tokenize("\"broken\nactor Good")
        assertEquals(UsecaseLexError(0, 1, 1, 1), result.errors.single())
        assertEquals("ACTOR", result.tokens.first { it.image == "actor" }.name)
    }
    @Test fun actualParserUsesLongestTokenImageForGrammarDiagnostics() {
        val source = "usecase-beta\nactor User ::: customer"
        val result = UsecaseDocument().parse(source)
        assertIs<MermaidParseResult.Success>(result)
        val invalid = UsecaseDocument().parse("usecase-beta\nsystemBoundary Auth\njson Payload@{}\nend")
        assertIs<MermaidParseResult.Failure>(invalid)
        assertTrue(invalid.diagnostics.single().message.contains("json Payload@"))
        val quoted = UsecaseDocument().parse("usecase-beta\ndirection LR \"extra\"")
        assertIs<MermaidParseResult.Failure>(quoted)
        assertTrue(quoted.diagnostics.single().message.contains("but found: '\"extra\"'"))
    }
}
