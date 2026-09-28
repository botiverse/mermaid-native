package build.raft.mermaid.core

/** PEG syntax lowers into the existing typed Railroad model and renderer. */
internal class PegParser(private val source: String) {
    private var pos = 0
    private var title: String? = null
    private var accTitle: String? = null
    private var accDescr: String? = null
    private fun fail(message: String): Nothing = throw IllegalArgumentException(message)
    private fun skip() {
        while (pos < source.length) {
            if (source[pos].isWhitespace()) { pos++; continue }
            val close = when {
                source.startsWith("%%{", pos) -> "}%%"
                else -> null
            }
            if (close != null) {
                val end = source.indexOf(close, pos + 2)
                if (end < 0) fail("Unclosed PEG comment")
                pos = end + close.length
            } else if ((source.startsWith("%%", pos) || source.startsWith("#", pos))) {
                while (pos < source.length && source[pos] !in "\r\n") pos++
            } else return
        }
    }
    private fun take(token: String): Boolean {
        skip(); if (!source.startsWith(token, pos)) return false
        pos += token.length; return true
    }
    private fun requireToken(token: String) { if (!take(token)) fail("Expected $token") }
    private fun identifier(): String {
        skip(); val match = Regex("[A-Za-z_][A-Za-z0-9_-]*").find(source, pos)?.takeIf { it.range.first == pos }
            ?: fail("Expected PEG identifier")
        pos = match.range.last + 1; return match.value
    }
    private fun string(): String {
        skip(); val quote = source.getOrNull(pos)
        if (quote != '\'' && quote != '"') fail("Expected PEG string")
        pos++; val out = StringBuilder()
        while (pos < source.length) {
            val c = source[pos++]
            if (c == quote) return out.toString()
            if (c != '\\') out.append(c) else {
                val escaped = source.getOrNull(pos++) ?: fail("Unclosed PEG escape")
                out.append(when (escaped) { 'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'; else -> escaped })
            }
        }
        fail("Unclosed PEG string")
    }
    private fun lineText(): String {
        val start = pos
        while (pos < source.length && source[pos] !in "\r\n" && !(source.startsWith("%%", pos) || source.startsWith("#", pos))) pos++
        return source.substring(start, pos).trim()
    }
    private fun metadataAhead(name: String): Boolean {
        var at = pos
        while (source.getOrNull(at) == ' ' || source.getOrNull(at) == '\t') at++
        return source.getOrNull(at) == ':' || name == "accDescr" && source.getOrNull(at) == '{'
    }
    private fun primary(): RailroadNode {
        skip()
        return when (source.getOrNull(pos)) {
            '"', '\'' -> RailroadTerminal(string())
            '(' -> { pos++; choice().also { requireToken(")") } }
            '.' -> { pos++; RailroadSpecial(".") }
            else -> RailroadNonTerminal(identifier())
        }
    }
    private fun term(): RailroadNode {
        skip()
        val prefix = source.getOrNull(pos)?.takeIf { it == '&' || it == '!' }
        if (prefix != null) pos++
        var node = primary()
        skip()
        node = when (source.getOrNull(pos)) {
            '?' -> { pos++; RailroadOptional(node) }
            '*' -> { pos++; RailroadZeroOrMore(node) }
            '+' -> { pos++; RailroadOneOrMore(node) }
            else -> node
        }
        if (prefix == null) return node
        val label = when (node) {
            is RailroadTerminal -> "\"${node.label}\""
            is RailroadNonTerminal -> node.label
            is RailroadSpecial -> node.text
            else -> "(...)"
        }
        return RailroadSpecial("$prefix$label")
    }
    private fun sequence(): RailroadNode {
        val nodes = mutableListOf(term())
        while (true) {
            skip()
            if (pos >= source.length || source[pos] in "/;)") break
            nodes += term()
        }
        return if (nodes.size == 1) nodes.single() else RailroadSequence(nodes)
    }
    private fun choice(): RailroadNode {
        val nodes = mutableListOf(sequence())
        while (take("/")) nodes += sequence()
        return if (nodes.size == 1) nodes.single() else RailroadChoice(nodes)
    }
    fun parse(): MermaidParseResult = try {
        requireToken("railroad-peg-beta")
        if (source.getOrNull(pos)?.let { !it.isWhitespace() && it != '%' } == true) fail("Invalid PEG header")
        val rules = mutableListOf<RailroadRule>()
        while (true) {
            skip(); if (pos >= source.length) break
            val name = identifier()
            if (rules.isEmpty() && name == "title" && (pos == source.length || source.getOrNull(pos)?.let { it.isWhitespace() } == true)) {
                while (source.getOrNull(pos) == ' ' || source.getOrNull(pos) == '\t') pos++
                title = if (source.getOrNull(pos) in listOf('\'', '"')) string() else lineText()
            } else if (rules.isEmpty() && name in listOf("accTitle", "accDescr") && metadataAhead(name)) {
                if (take(":")) { if (name == "accTitle") accTitle = lineText() else accDescr = lineText() }
                else if (name == "accDescr" && take("{")) {
                    val end = source.indexOf('}', pos); if (end < 0) fail("Unclosed accessibility description")
                    accDescr = source.substring(pos, end).trim(); pos = end + 1
                } else fail("Invalid accessibility metadata")
            } else {
                requireToken("<-")
                val definition = choice(); requireToken(";")
                rules += RailroadRule(name, definition)
            }
        }
        MermaidParseResult.Success(RailroadDiagram(title, rules, accTitle, accDescr))
    } catch (e: IllegalArgumentException) {
        val lines = source.take(pos).split(Regex("\r\n|\r|\n"))
        MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
            e.message ?: "Invalid PEG syntax", SourceLocation(lines.size, lines.last().length + 1))))
    }
}
