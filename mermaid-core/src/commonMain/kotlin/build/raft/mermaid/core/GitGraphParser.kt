package build.raft.mermaid.core

/** Git history is resolved in the shared model, before any renderer or adapter consumes it. */
internal class GitGraphParser(private val source: String) {
    fun parse(): MermaidParseResult {
        var lineNumber = 1
        return try {
            val syntax = GitGraphSyntaxParser.parse(source)
            if (syntax.diagnostics.isNotEmpty()) return MermaidParseResult.Failure(syntax.diagnostics)
            val direction = syntax.direction
            val branches = linkedMapOf("main" to GitGraphBranch("main", null))
            val heads = linkedMapOf<String, String?>("main" to null)
            val commits = linkedMapOf<String, GitGraphCommit>()
            val warnings = mutableListOf<String>()
            var current = "main"; var next = 1; var sequence = 0
            val title = syntax.title; val accTitle = syntax.accTitle; val accDescription = syntax.accDescription
            fun autoId(): String { while ("commit-$next" in commits) next++; return "commit-${next++}" }
            fun add(commit: GitGraphCommit) { commits[commit.id] = commit.copy(sequence = sequence++); heads[current] = commit.id }
            for (statement in syntax.statements) {
                lineNumber = statement.line
                val command = when (statement.type) {
                    "Commit" -> "commit"
                    "Branch" -> "branch"
                    "Checkout" -> "checkout"
                    "Merge" -> "merge"
                    "CherryPicking" -> "cherry-pick"
                    else -> error("Unsupported gitGraph statement")
                }
                val branch = statement.name ?: statement.branch
                val id = statement.id; val message = statement.message.orEmpty(); val kind = statement.commitType
                val parent = statement.parent; val order = statement.order ?: 0; val tags = statement.tags
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
