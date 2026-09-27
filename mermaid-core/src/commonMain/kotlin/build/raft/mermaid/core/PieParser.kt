package build.raft.mermaid.core

/** Pie's line grammar keeps quoted labels and multiline metadata intact. */
internal class PieParser(private val source: String) {
    private var offset = 0
    private var title: String? = null
    private var accessibilityTitle: String? = null
    private var accessibilityDescription: String? = null
    private val sections = linkedMapOf<String, PieSection>()

    fun parse(): MermaidParseResult = try {
        whitespace()
        requireSyntax(word("pie"), "Expected pie")
        horizontalWhitespace()
        val showData = word("showData")
        while (true) {
            whitespace()
            if (offset == source.length) break
            when {
                word("title") -> {
                    requireSyntax(peek() == null || peek()?.isWhitespace() == true || source.startsWith("%%", offset), "Expected whitespace after title")
                    title = lineText()
                }
                word("accTitle") -> {
                    horizontalWhitespace()
                    requireSyntax(take(':'), "Expected : after accTitle")
                    accessibilityTitle = lineText()
                }
                word("accDescr") -> {
                    horizontalWhitespace()
                    accessibilityDescription = if (take('{')) {
                        val start = offset
                        while (peek() != null && peek() != '}') offset++
                        requireSyntax(peek() == '}', "Unclosed accessibility description")
                        source.substring(start, offset++).lineSequence().map { normalize(it) }.filter { it.isNotEmpty() }.joinToString("\n")
                    } else {
                        requireSyntax(take(':'), "Expected : or { after accDescr")
                        lineText()
                    }
                }
                else -> section()
            }
            horizontalWhitespace()
            if (source.startsWith("%%", offset)) while (peek() != null && peek() != '\r' && peek() != '\n') offset++
            requireSyntax(peek() == null || peek() == '\r' || peek() == '\n', "Expected a line ending after pie statement")
        }
        MermaidParseResult.Success(PieDiagram(title, showData, sections.values.toList(), accessibilityTitle, accessibilityDescription))
    } catch (error: SyntaxError) {
        val prefix = source.take(error.offset)
        MermaidParseResult.Failure(listOf(MermaidDiagnostic(error.code, error.message ?: "Unsupported pie syntax",
            SourceLocation(prefix.count { it == '\n' } + 1, error.offset - prefix.lastIndexOf('\n')))))
    }

    private fun section() {
        val sectionStart = offset
        val delimiter = peek()
        requireSyntax(delimiter == '"' || delimiter == '\'', "Expected a quoted pie label")
        offset++
        val label = buildString {
            while (peek() != delimiter) {
                val char = peek() ?: fail("Unclosed pie label")
                offset++
                if (char != '\\') append(char) else {
                    val escaped = peek() ?: fail("Unclosed pie label escape")
                    offset++
                    append(when (escaped) {
                        'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'; 'b' -> '\b'; 'f' -> '\u000c'; 'v' -> '\u000b'; '0' -> '\u0000'
                        else -> escaped
                    })
                }
            }
        }
        offset++
        horizontalWhitespace()
        requireSyntax(take(':'), "Expected : after pie label")
        horizontalWhitespace()
        val number = NUMBER.find(source, offset)?.takeIf { it.range.first == offset } ?: fail("Expected numeric pie slice value")
        offset += number.value.length
        val value = number.value.toDoubleOrNull() ?: fail("Expected numeric pie slice value")
        requireSyntax(value.isFinite(), "Pie slice values must be finite")
        if (value < 0.0) fail("\"$label\" has invalid value: ${value.toString().removeSuffix(".0")}. Negative values are not allowed in pie charts. All slice values must be >= 0.", MermaidDiagnosticCode.INVALID_VALUE, sectionStart)
        if (label !in sections) sections[label] = PieSection(label, value)
    }

    private fun lineText(): String {
        val start = offset
        while (peek() != null && peek() != '\r' && peek() != '\n' && !source.startsWith("%%", offset)) offset++
        return normalize(source.substring(start, offset))
    }
    private fun normalize(value: String) = value.trim().replace(Regex("[\\t ]{2,}"), " ")
    private fun horizontalWhitespace() { while (peek() == ' ' || peek() == '\t') offset++ }
    private fun whitespace() {
        while (offset < source.length) {
            if (source[offset].isWhitespace()) offset++
            else if (source.startsWith("%%", offset)) while (peek() != null && peek() != '\r' && peek() != '\n') offset++
            else break
        }
    }
    private fun peek(): Char? = source.getOrNull(offset)
    private fun take(char: Char): Boolean = (peek() == char).also { if (it) offset++ }
    private fun word(value: String): Boolean {
        if (!source.startsWith(value, offset)) return false
        val next = source.getOrNull(offset + value.length)
        if (next != null && (next.isLetterOrDigit() || next == '_')) return false
        offset += value.length
        return true
    }
    private fun requireSyntax(condition: Boolean, message: String) { if (!condition) fail(message) }
    private fun fail(message: String, code: MermaidDiagnosticCode = MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, errorOffset: Int = offset): Nothing = throw SyntaxError(message, errorOffset, code)
    private class SyntaxError(message: String, val offset: Int, val code: MermaidDiagnosticCode) : Exception(message)
    private companion object { val NUMBER = Regex("-?(?:[0-9]+\\.[0-9]+|0|[1-9][0-9]*)") }
}
