package build.raft.mermaid.core

/** Cynefin's canonical domain blocks and transitions, with shared accessibility metadata. */
internal class CynefinParser(private val source: String) {
    private var pos = 0
    private fun fail(message: String): Nothing = throw IllegalArgumentException(message)
    private fun skip(newlines: Boolean = true) {
        while (pos < source.length) {
            if (source[pos] == ' ' || source[pos] == '\t' || newlines && source[pos] in "\r\n") { pos++; continue }
            if (source.startsWith("%%{", pos)) {
                val end = source.indexOf("}%%", pos + 3)
                if (end < 0) fail("Unclosed Cynefin directive")
                pos = end + 3
            } else if (source.startsWith("%%", pos)) {
                while (pos < source.length && source[pos] !in "\r\n") pos++
            } else return
        }
    }
    private fun word(): String {
        skip()
        val start = pos
        while (pos < source.length && (source[pos].isLetter() || source[pos] == '-' && source.getOrNull(pos + 1)?.isLetter() == true)) pos++
        if (pos == start) fail("Expected Cynefin domain or metadata")
        return source.substring(start, pos)
    }
    private fun eol() {
        skip(false)
        if (pos < source.length && source[pos] !in "\r\n") fail("Expected end of Cynefin statement")
    }
    private fun text(): String {
        val start = pos
        while (pos < source.length && source[pos] !in "\r\n" && !source.startsWith("%%", pos)) pos++
        return normalize(source.substring(start, pos))
    }
    private fun normalize(value: String): String = value.trim().replace(Regex("[\\t ]{2,}"), " ")
    private fun string(): String {
        skip(); val quote = source.getOrNull(pos)
        if (quote != '"' && quote != '\'') fail("Expected quoted Cynefin label")
        pos++; val value = StringBuilder()
        while (pos < source.length) {
            val c = source[pos++]
            if (c == quote) return value.toString()
            if (c != '\\') value.append(c) else {
                val escaped = source.getOrNull(pos++) ?: fail("Unclosed Cynefin escape")
                if (escaped in "\r\n") fail("Invalid Cynefin escape")
                value.append(when (escaped) { 'b' -> '\b'; 'f' -> '\u000c'; 'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'; 'v' -> '\u000b'; '0' -> '\u0000'; else -> escaped })
            }
        }
        fail("Unclosed Cynefin label")
    }
    private fun domain(name: String): CynefinDomain = CynefinDomain.entries.firstOrNull { it.name.equals(name, true) }
        ?: fail("Unknown Cynefin domain: $name")

    fun parse(): MermaidParseResult = try {
        val header = word().lowercase()
        if (header != "cynefin-beta" && header != "cynefin") fail("Expected cynefin-beta header")
        if (header == "cynefin-beta" && source.getOrNull(pos) == ':') pos++
        eol()
        var title: String? = null
        var accTitle: String? = null
        var accDescr: String? = null
        val domains = linkedMapOf<CynefinDomain, List<String>>()
        val transitions = mutableListOf<CynefinTransition>()
        while (true) {
            skip(); if (pos >= source.length) break
            val name = word()
            when (name) {
                "title" -> { title = text(); eol() }
                "accTitle", "accDescr" -> {
                    skip(false)
                    val value = when {
                        source.getOrNull(pos) == ':' -> { pos++; text() }
                        name == "accDescr" && source.getOrNull(pos) == '{' -> {
                            val start = ++pos; val end = source.indexOf('}', start)
                            if (end < 0) fail("Unclosed accessibility description")
                            pos = end + 1
                            source.substring(start, end).lines().map(::normalize).filter { it.isNotEmpty() }.joinToString("\n")
                        }
                        else -> fail("Expected accessibility separator")
                    }
                    eol(); if (name == "accTitle") accTitle = value else accDescr = value
                }
                else -> {
                    val from = domain(name)
                    skip(false)
                    if (source.startsWith("-->", pos)) {
                        pos += 3; skip(false)
                        if (pos >= source.length || source[pos] in "\r\n") fail("Expected target domain")
                        val to = domain(word()); skip(false)
                        val label = if (source.getOrNull(pos) == ':') { pos++; skip(false); if (source.getOrNull(pos) !in listOf('"', '\'')) fail("Expected transition label"); string().ifEmpty { null } } else null
                        eol(); if (from != to) transitions += CynefinTransition(from, to, label)
                    } else {
                        val items = mutableListOf<String>()
                        while (true) { skip(); if (source.getOrNull(pos) !in listOf('"', '\'')) break; items += string() }
                        domains[from] = items
                    }
                }
            }
        }
        MermaidParseResult.Success(CynefinDiagram(title?.ifEmpty { null }, domains.map { CynefinDomainBlock(it.key, it.value) }, transitions,
            accTitle?.ifEmpty { null }, accDescr?.ifEmpty { null }))
    } catch (e: IllegalArgumentException) {
        val lines = source.take(pos).split(Regex("\r\n|\r|\n"))
        MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
            e.message ?: "Invalid Cynefin syntax", SourceLocation(lines.size, lines.last().length + 1))))
    }
}
