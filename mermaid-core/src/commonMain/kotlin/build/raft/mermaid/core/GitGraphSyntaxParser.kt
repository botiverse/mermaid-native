package build.raft.mermaid.core

/** Source statements before history resolution. Missing optional properties stay missing. */
public data class GitGraphStatement(
    val type: String,
    val line: Int,
    val id: String? = null,
    val message: String? = null,
    val name: String? = null,
    val branch: String? = null,
    val commitType: GitGraphCommitType? = null,
    val tags: List<String> = emptyList(),
    val parent: String? = null,
    val order: Int? = null,
)

public data class GitGraphSyntax(
    val statements: List<GitGraphStatement>,
    val diagnostics: List<MermaidDiagnostic>,
    val direction: FlowDirection = FlowDirection.LR,
    val title: String? = null,
    val accTitle: String? = null,
    val accDescription: String? = null,
)

/** Shared syntax boundary; rendering accepts it only after syntax and history validation. */
public object GitGraphSyntaxParser {
    private data class Token(val value: String, val quoted: Boolean)
    private val commands = mapOf("commit" to "Commit", "branch" to "Branch", "merge" to "Merge",
        "checkout" to "Checkout", "switch" to "Checkout", "cherry-pick" to "CherryPicking")
    private fun tokens(line: String): List<Token> {
        val result = mutableListOf<Token>(); var i = 0
        while (i < line.length) {
            if (line[i].isWhitespace()) { i++; continue }
            if (line.startsWith("%%", i)) break
            val quote = line[i]
            if (quote == '"' || quote == '\'') {
                i++; val value = StringBuilder(); var closed = false
                while (i < line.length) {
                    val c = line[i++]
                    if (c == quote) { closed = true; break }
                    if (c == '\\') { require(i < line.length); value.append(line[i++]) } else value.append(c)
                }
                require(closed) { "Unclosed gitGraph string" }; result += Token(value.toString(), true)
            } else {
                val start = i
                while (i < line.length && !line[i].isWhitespace() && line[i] != ':' && line[i] != '"' && line[i] != '\'') i++
                if (i < line.length && line[i] == ':') i++
                require(i > start); result += Token(line.substring(start, i), false)
            }
        }
        return result
    }

    public fun parse(source: String): GitGraphSyntax {
        val statements = mutableListOf<GitGraphStatement>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        var direction = FlowDirection.LR
        var title: String? = null; var accTitle: String? = null; var accDescription: String? = null
        var lineNumber = 1
        fun report(message: String) { diagnostics += MermaidDiagnostic(MermaidDiagnosticCode.INVALID_VALUE, message, SourceLocation(lineNumber, 1)) }
        try {
            val lines = source.replace("\r\n", "\n").lines()
            val headerIndex = lines.indexOfFirst { it.isNotBlank() && !it.trimStart().startsWith("%%") }
            require(headerIndex >= 0) { "Expected gitGraph header" }
            lineNumber = headerIndex + 1
            val header = Regex("gitGraph(?:\\s+(LR|TB|BT)\\s*:|\\s*:)?").matchEntire(lines[headerIndex].trim()) ?: error("Invalid gitGraph header")
            direction = header.groupValues[1].ifEmpty { "LR" }.let(FlowDirection::valueOf)
            var i = headerIndex + 1
            while (i < lines.size) {
                lineNumber = i + 1; val line = lines[i++].trim()
                if (line.isBlank() || line.startsWith("%%")) continue
                if (Regex("^title(?:[\\t ]|$)").containsMatchIn(line)) { title = line.drop(5).substringBefore("%%").trim(); continue }
                if (Regex("^accTitle[\\t ]*:").containsMatchIn(line)) { accTitle = line.substringAfter(':').substringBefore("%%").trim(); continue }
                if (line.startsWith("accDescr")) {
                    if ('{' in line) {
                        val content = StringBuilder(line.substringAfter('{'))
                        while ('}' !in content && i < lines.size) content.append('\n').append(lines[i++])
                        require('}' in content) { "Unclosed accessibility description" }
                        accDescription = content.toString().substringBefore('}').trim().replace(Regex("\\n\\s+"), "\n")
                    } else { require(':' in line); accDescription = line.substringAfter(':').substringBefore("%%").trim() }
                    continue
                }
                val ts = try { tokens(line) } catch (e: IllegalArgumentException) { report(e.message ?: "Invalid gitGraph token"); continue }
                var position = 0
                while (position < ts.size) {
                    val token = ts[position++]
                    val command = token.value
                    val type = commands[command].takeUnless { token.quoted }
                    if (type == null) { report("Unsupported gitGraph statement $command"); continue }
                    var branch: String? = null
                    if (command in setOf("branch", "checkout", "switch", "merge")) {
                        val name = ts.getOrNull(position++)
                        if (name == null) report("Expected branch")
                        else if (!name.quoted && !Regex("\\w([-./\\w]*[-\\w])?").matches(name.value)) report("Invalid branch name")
                        else branch = name.value
                    }
                    var id: String? = null; var message: String? = null; var kind: GitGraphCommitType? = null
                    var parent: String? = null; var order: Int? = null
                    val tags = mutableListOf<String>()
                    while (position < ts.size) {
                        val key = ts[position]
                        if (!key.quoted && key.value in commands) { report("Expected newline between gitGraph statements"); break }
                        position++
                        if (key.quoted && command == "commit") { message = key.value; continue }
                        val value = ts.getOrNull(position)
                        if (value == null || (!value.quoted && value.value in commands)) { report("Expected value after ${key.value}"); continue }
                        position++
                        try {
                            when (key.value) {
                                "id:" -> { require(value.quoted && command in setOf("commit", "merge", "cherry-pick")); id = value.value }
                                "msg:" -> { require(value.quoted && command == "commit"); message = value.value }
                                "tag:" -> { require(value.quoted && command in setOf("commit", "merge", "cherry-pick")); tags += value.value }
                                "type:" -> { require(!value.quoted && command in setOf("commit", "merge")); kind = GitGraphCommitType.valueOf(value.value) }
                                "parent:" -> { require(value.quoted && command == "cherry-pick"); parent = value.value }
                                "order:" -> { require(!value.quoted && command == "branch"); order = value.value.toInt().also { require(it >= 0) } }
                                else -> error("Unsupported gitGraph attribute ${key.value}")
                            }
                        } catch (e: IllegalArgumentException) { report(e.message ?: "Invalid gitGraph attribute ${key.value}") }
                          catch (e: IllegalStateException) { report(e.message ?: "Invalid gitGraph attribute ${key.value}") }
                    }
                    statements += GitGraphStatement(type, lineNumber, id, message,
                        name = branch.takeIf { command == "branch" }, branch = branch.takeUnless { command == "branch" },
                        commitType = kind, tags = tags, parent = parent, order = order)
                }
            }
        } catch (e: IllegalArgumentException) { report(e.message ?: "Invalid gitGraph") }
          catch (e: IllegalStateException) { report(e.message ?: "Invalid gitGraph") }
        return GitGraphSyntax(statements, diagnostics, direction, title, accTitle, accDescription)
    }
}
