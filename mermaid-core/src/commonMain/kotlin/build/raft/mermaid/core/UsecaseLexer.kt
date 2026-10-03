package build.raft.mermaid.core

/** A source token with inclusive UTF-16 offsets and one-based line/column locations. */
public data class UsecaseToken(
    val name: String, val image: String, val startOffset: Int, val endOffset: Int,
    val startLine: Int, val startColumn: Int, val endLine: Int, val endColumn: Int,
)
public data class UsecaseLexError(val offset: Int, val length: Int, val line: Int, val column: Int)
public data class UsecaseLexResult(val tokens: List<UsecaseToken>, val errors: List<UsecaseLexError>)

/** Mode-aware tokenizer used by Native Usecase diagnostics and available to editors. */
public object UsecaseLexer {
    private data class Rule(val name: String, val pattern: Regex, val keyword: Boolean = false)
    private fun rule(name: String, pattern: String) = Rule(name, Regex(pattern))
    private val identifier = Regex("[A-Za-z0-9_]+")
    private val number = Regex("(?:\\d+\\.\\d+|\\d+|\\.\\d+)(?:[A-Za-z]+)?")
    private val jsonStart = Regex("json[\\t ]+\\w+[\\t ]*@[\\t ]*(?=\\{)")
    private val accBlockStart = Regex("accDescr[\\t ]*\\{")
    private val accLines = listOf(rule("ACC_TITLE_LINE", "accTitle[\\t ]*:[^\\n\\r]*"), rule("ACC_DESCR_LINE", "accDescr[\\t ]*:[^\\n\\r]*"))
    private val keywords = listOf(
        "USECASE" to "usecase-beta", "ACTOR" to "actor", "SYSTEM_BOUNDARY" to "systemBoundary",
        "END" to "end", "DIRECTION" to "direction", "TD" to "TD", "TB" to "TB", "BT" to "BT", "LR" to "LR", "RL" to "RL",
        "NOTE" to "note", "FOR" to "for", "JSON" to "json", "CLASS_DEF" to "classDef", "CLASS" to "class", "STYLE" to "style",
        "INCLUDE" to "include", "EXTEND" to "extend", "TRUE" to "true", "FALSE" to "false",
    ).map { (name, word) -> Rule(name, if (name == "INCLUDE" || name == "EXTEND") Regex(word, RegexOption.IGNORE_CASE) else Regex(word), true) }
    private val operators = listOf(
        rule("GENERALIZATION", "--\\|>"), rule("DEPENDENCY_ARROW", "\\.\\.>"), rule("STEREOTYPE_START", "<<"), rule("CLASS_SEPARATOR", ":::"),
        rule("FORWARD_SOLID", "--+>"), rule("BACKWARD_SOLID", "<--+"), rule("FORWARD_CIRCLE", "--o"), rule("BACKWARD_CIRCLE", "o--"),
        rule("FORWARD_CROSS", "--x"), rule("BACKWARD_CROSS", "x--"), rule("MARKERLESS_SOLID", "--+"), rule("METADATA_START", "@\\{"),
        rule("AT", "@"), rule("LBRACE", "\\{"), rule("RBRACE", "}"), rule("LBRACKET", "\\["), rule("RBRACKET", "]"),
        rule("LPAREN", "\\("), rule("RPAREN", "\\)"), rule("COMMA", ","), rule("COLON", ":"), rule("HASH_COLOR", "#[0-9A-Fa-f]+"),
        rule("PLAIN_STRING", "\"[^\\n\\r\"]*\"|'[^\\n\\r']*'"), rule("CSS_IDENTIFIER", "--[A-Z_a-z][\\w-]*|[A-Z_a-z]\\w*(?:-\\w+)+"),
    )
    private val tail = listOf(
        rule("CSS_ESCAPED_COMMA", "\\\\,"), rule("DASH", "-"), rule("DOT", "\\."), rule("PERCENT", "%"),
        rule("CSS_PUNCTUATION", "[!#$&*+/=?^_|~]"), rule("LABEL_PUNCTUATION", "[;<>\\\\`]"), rule("LABEL_SYMBOL", "[^\\t\\n\\r !-~]+"),
    )
    private data class Match(val name: String, val length: Int, val mode: Int = 0)

    public fun tokenize(source: String): UsecaseLexResult {
        val tokens = mutableListOf<UsecaseToken>()
        val errors = mutableListOf<UsecaseLexError>()
        var offset = 0; var line = 1; var column = 1; var mode = 0
        var onlyIndent = true
        fun regex(rule: Rule): Match? {
            val match = rule.pattern.matchAt(source, offset) ?: return null
            val longer = if (rule.keyword) identifier.matchAt(source, offset)?.value else null
            return if (longer != null && longer.length > match.value.length) Match("IDENTIFIER", longer.length)
                else Match(rule.name, match.value.length)
        }
        fun match(): Match? {
            if (mode == 1) {
                if (source[offset] != '{') return null
                var depth = 0; var quoted = false; var escaped = false
                for (i in offset until source.length) {
                    val c = source[i]
                    if (quoted) {
                        if (escaped) escaped = false else if (c == '\\') escaped = true else if (c == '"') quoted = false
                    } else if (c == '"') quoted = true
                    else if (c == '{') depth++
                    else if (c == '}' && --depth == 0) return Match("JSON_OBJECT_LITERAL", i + 1 - offset)
                }
                return Match("UNCLOSED_JSON_OBJECT_LITERAL", source.length - offset)
            }
            if (mode == 2) {
                if (source.startsWith(">>", offset)) return Match("STEREOTYPE_END", 2)
                // Stop at the physical line before looking for a closer. Rescanning the
                // remaining source for every malformed stereotype would be quadratic.
                var end = offset
                while (end < source.length && source[end] !in "\r\n" && !source.startsWith(">>", end)) end++
                if (source.startsWith(">>", end) && source.substring(offset, end).isNotBlank())
                    return Match("STEREOTYPE_TEXT", end - offset, 2)
                while (end < source.length && source[end] !in "\r\n") end++
                return if (end > offset) Match("UNCLOSED_STEREOTYPE_TEXT", end - offset) else null
            }
            if (source[offset] == ' ' || source[offset] == '\t') {
                var i = offset + 1; while (i < source.length && source[i] in " \t") i++
                return Match("HWS", i - offset)
            }
            if (source.startsWith("\"`", offset)) {
                val end = source.indexOf("`\"", offset + 2)
                return if (end < 0) Match("UNCLOSED_MARKDOWN_STRING", source.length - offset) else Match("MARKDOWN_STRING", end + 2 - offset)
            }
            if (onlyIndent && source.startsWith("%%", offset)) {
                var i = offset + 2; while (i < source.length && source[i] !in "\r\n") i++
                return Match("COMMENT", i - offset)
            }
            if (source[offset] in "\r\n") return Match("NEWLINE", if (source.startsWith("\r\n", offset)) 2 else 1)
            if (onlyIndent) {
                val opening = accBlockStart.matchAt(source, offset)
                if (opening != null) {
                    val end = source.indexOf('}', opening.range.last + 1)
                    if (end >= 0) return Match("ACC_DESCR_BLOCK", end + 1 - offset)
                }
                accLines.forEach { regex(it)?.let { found -> return found } }
            }
            jsonStart.matchAt(source, offset)?.let { return Match("JSON_DECLARATION_START", it.value.length, 1) }
            keywords.forEach { regex(it)?.let { found -> return found } }
            operators.forEach { regex(it)?.let { found -> return if (found.name == "STEREOTYPE_START") found.copy(mode = 2) else found } }
            val id = identifier.matchAt(source, offset)?.value
            val num = number.matchAt(source, offset)?.value
            if (id != null) return if (num != null && num.length > id.length) Match("NUMBER", num.length) else Match("IDENTIFIER", id.length)
            if (num != null) return Match("NUMBER", num.length)
            tail.forEach { regex(it)?.let { found -> return found } }
            return null
        }
        while (offset < source.length) {
            val start = offset; val startLine = line; val startColumn = column
            val found = match()
            if (found == null) {
                // Recover a contiguous unmatched run without interpreting it as label text.
                do { offset++; column++; onlyIndent = false } while (offset < source.length && match() == null)
                errors += UsecaseLexError(start, offset - start, startLine, startColumn)
                continue
            }
            val image = source.substring(start, start + found.length)
            var endLine = line; var endColumn = column + image.length - 1
            var i = 0; var lineBreaks = 0
            while (i < image.length) {
                val c = image[i]
                if (c == '\r' || c == '\n') {
                    if (c == '\r' && image.getOrNull(i + 1) == '\n') i++
                    line++; lineBreaks++; column = 1; onlyIndent = true
                } else { column++; if (c != ' ' && c != '\t') onlyIndent = false }
                i++
            }
            if (line > startLine && image.last() !in "\r\n") { endLine = line; endColumn = column - 1 }
            else if (lineBreaks > 1) { endLine = line - 1; endColumn = 1 }
            if (found.name != "HWS") tokens += UsecaseToken(found.name, image, start, start + image.length - 1, startLine, startColumn, endLine, endColumn)
            offset += image.length; mode = found.mode
        }
        return UsecaseLexResult(tokens.toList(), errors.toList())
    }
}
