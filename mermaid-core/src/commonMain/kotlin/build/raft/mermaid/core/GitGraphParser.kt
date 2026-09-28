package build.raft.mermaid.core

/** Git history is resolved in the shared model, before any renderer or adapter consumes it. */
internal class GitGraphParser(private val source: String) {
    private data class Token(val value: String, val quoted: Boolean)
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
    fun parse(): MermaidParseResult {
        var lineNumber = 1
        return try {
            val lines = source.replace("\r\n", "\n").lines()
            val headerIndex = lines.indexOfFirst { it.isNotBlank() && !it.trimStart().startsWith("%%") }
            require(headerIndex >= 0)
            val header = Regex("gitGraph(?:\\s+(LR|TB|BT)\\s*:|\\s*:)?").matchEntire(lines[headerIndex].trim()) ?: error("Invalid gitGraph header")
            val direction = header.groupValues[1].ifEmpty { "LR" }.let(FlowDirection::valueOf)
            val branches = linkedMapOf("main" to GitGraphBranch("main", null))
            val heads = linkedMapOf<String, String?>("main" to null)
            val commits = linkedMapOf<String, GitGraphCommit>()
            val warnings = mutableListOf<String>()
            var current = "main"; var next = 1; var sequence = 0
            var title: String? = null; var accTitle: String? = null; var accDescription: String? = null
            fun autoId(): String { while ("commit-$next" in commits) next++; return "commit-${next++}" }
            fun add(commit: GitGraphCommit) { commits[commit.id] = commit.copy(sequence = sequence++); heads[current] = commit.id }
            fun branchName(token: Token): String {
                require(token.quoted || Regex("\\w([-./\\w]*[-\\w])?").matches(token.value)) { "Invalid branch name" }
                return token.value
            }
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
                val ts = tokens(line); if (ts.isEmpty()) continue
                val command = ts[0].value
                var position = 1
                val branch = if (command in setOf("branch", "checkout", "switch", "merge")) branchName(ts.getOrNull(position++) ?: error("Expected branch")) else null
                var id: String? = null; var message = ""; var kind: GitGraphCommitType? = null; var parent: String? = null; var order = 0
                val tags = mutableListOf<String>()
                while (position < ts.size) {
                    val key = ts[position++]
                    if (key.quoted && command == "commit") { message = key.value; continue }
                    val value = ts.getOrNull(position++) ?: error("Expected value after ${key.value}")
                    when (key.value) {
                        "id:" -> { require(value.quoted && command in setOf("commit", "merge", "cherry-pick")); id = value.value }
                        "msg:" -> { require(value.quoted && command == "commit"); message = value.value }
                        "tag:" -> { require(value.quoted && command in setOf("commit", "merge", "cherry-pick")); tags += value.value }
                        "type:" -> { require(!value.quoted && command in setOf("commit", "merge")); kind = GitGraphCommitType.valueOf(value.value) }
                        "parent:" -> { require(value.quoted && command == "cherry-pick"); parent = value.value }
                        "order:" -> { require(!value.quoted && command == "branch"); order = value.value.toInt().also { require(it >= 0) } }
                        else -> error("Unsupported gitGraph attribute ${key.value}")
                    }
                }
                val head = heads[current]
                when (command) {
                    "commit" -> {
                        val resolved = id?.takeIf { it.isNotEmpty() } ?: autoId()
                        if (resolved in commits) warnings += "Commit ID $resolved already exists"
                        add(GitGraphCommit(resolved, current, listOfNotNull(head), kind ?: GitGraphCommitType.NORMAL, tags.firstOrNull(),
                            message = message, tags = tags))
                    }
                    "branch" -> {
                        val name = requireNotNull(branch)
                        require(name !in branches) { "Trying to create an existing branch. (Help: Either use a new name if you want create a new branch or try using \"checkout $name\")" }
                        branches[name] = GitGraphBranch(name, head, order); heads[name] = head; current = name
                    }
                    "checkout", "switch" -> {
                        val name = requireNotNull(branch)
                        require(name in branches) { "Trying to checkout branch which is not yet created. (Help try using \"branch $name\")" }; current = name
                    }
                    "merge" -> {
                        val other = requireNotNull(branch); val otherHead = heads[other]
                        require(!(head != null && otherHead != null && commits[head]?.branch == other)) { "Cannot merge branch '$other' into itself." }
                        require(other != current) { "Incorrect usage of \"merge\". Cannot merge a branch to itself" }
                        require(head != null && head in commits) { "Incorrect usage of \"merge\". Current branch ($current)has no commits" }
                        require(other in branches) { "Incorrect usage of \"merge\". Branch to be merged ($other) does not exist" }
                        require(otherHead != null && otherHead in commits) { "Incorrect usage of \"merge\". Branch to be merged ($other) has no commits" }
                        require(head != otherHead) { "Incorrect usage of \"merge\". Both branches have same head" }
                        require(id.isNullOrEmpty() || id !in commits) { "Incorrect usage of \"merge\". Commit with id:$id already exists, use different custom id" }
                        add(GitGraphCommit(id?.takeIf { it.isNotEmpty() } ?: autoId(), current, listOf(head, otherHead), kind ?: GitGraphCommitType.NORMAL, tags.firstOrNull(), true,
                            "merged branch $other into $current", tags, !id.isNullOrEmpty(), kind))
                    }
                    "cherry-pick" -> {
                        val picked = commits[id] ?: error("Incorrect usage of \"cherryPick\". Source commit id should exist and provided")
                        require(parent.isNullOrEmpty() || parent in picked.parentIds) { "Invalid operation: The specified parent commit is not an immediate parent of the cherry-picked commit." }
                        require(!picked.isMerge || !parent.isNullOrEmpty()) { "Incorrect usage of cherry-pick: If the source commit is a merge commit, an immediate parent commit must be specified." }
                        require(picked.branch != current) { "Incorrect usage of \"cherryPick\". Source commit is already on current branch" }
                        require(head != null && head in commits) { "Incorrect usage of \"cherry-pick\". Current branch ($current)has no commits" }
                        val labels = if (tags.isEmpty()) listOf("cherry-pick:${picked.id}" + if (picked.isMerge) "|parent:$parent" else "") else tags.filter { it.isNotEmpty() }
                        add(GitGraphCommit(autoId(), current, listOf(head, picked.id), tag = labels.firstOrNull(),
                            message = "cherry-picked ${picked.message} into $current", tags = labels, isCherryPick = true))
                    }
                    else -> error("Unsupported gitGraph statement")
                }
            }
            MermaidParseResult.Success(GitGraphDiagram(branches.values.toList(), commits.values.toList(), direction, current, heads, title, accTitle, accDescription, warnings))
        } catch (error: IllegalArgumentException) {
            MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.INVALID_VALUE, error.message ?: "Invalid gitGraph", SourceLocation(lineNumber, 1))))
        } catch (error: IllegalStateException) {
            MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, error.message ?: "Invalid gitGraph", SourceLocation(lineNumber, 1))))
        }
    }
}
