package build.raft.mermaid.core

internal class TreeViewParser(private val source: String) {
    fun parse(): MermaidParseResult {
        val nodes = mutableListOf<TreeViewNode>(); val stack = mutableListOf<Pair<Int, Int>>()
        var header = false; var title: String? = null; var accTitle: String? = null; var accDescription: String? = null; var description: StringBuilder? = null
        fun fail(message: String, line: Int) = MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, message, SourceLocation(line, 1))))
        source.lines().forEachIndexed { index, raw ->
            var text = raw.trim()
            if (description != null) {
                if ('}' !in text) { description!!.append('\n').append(text); return@forEachIndexed }
                description!!.append('\n').append(text.substringBefore('}')); accDescription = description.toString().trim(); description = null
                text = text.substringAfter('}').trim()
            }
            if (text.isEmpty() || text.startsWith("%%")) return@forEachIndexed
            if (!header) { if (text != "treeView-beta") return fail("Expected treeView-beta", index + 1); header = true; return@forEachIndexed }
            when {
                Regex("^title(?:\\s|$)").containsMatchIn(text) -> { title = text.removePrefix("title").substringBefore("%%").trim(); return@forEachIndexed }
                Regex("^accTitle\\s*:").containsMatchIn(text) -> { accTitle = text.substringAfter(':').substringBefore("%%").trim(); return@forEachIndexed }
                Regex("^accDescr\\s*:").containsMatchIn(text) -> { accDescription = text.substringAfter(':').substringBefore("%%").trim(); return@forEachIndexed }
                Regex("^accDescr\\s*\\{").containsMatchIn(text) -> { val body = text.substringAfter('{'); if ('}' in body) accDescription = body.substringBefore('}').trim() else description = StringBuilder(body); return@forEachIndexed }
            }
            val name: String
            var rest: String
            if (text.first() in "\"'") {
                val end = text.indexOf(text.first(), 1)
                if (end < 0) return fail("Unclosed treeView label", index + 1)
                name = text.substring(1, end); rest = text.substring(end + 1)
                if (rest.isNotEmpty() && !rest.first().isWhitespace()) return fail("TreeView annotations require whitespace", index + 1)
            } else {
                if (text.startsWith(":::") || text.startsWith("icon(") || text.startsWith("##")) return fail("TreeView annotations require a node", index + 1)
                val split = Regex("\\s+(?=:::|icon\\(|##)").find(text)?.range?.first ?: text.length
                name = text.substring(0, split).trimEnd(); rest = text.substring(split)
            }
            var className: String? = null; var icon: String? = null; var desc: String? = null
            while (rest.isNotBlank()) {
                rest = rest.trimStart()
                if (rest.startsWith("%%")) break
                if (rest.startsWith("##")) { desc = rest.substring(2).trim(); break }
                val clazz = Regex("^:::\\s*([A-Za-z_][A-Za-z0-9_-]*)(?=\\s|$)").find(rest)
                if (clazz != null) { className = clazz.groupValues[1]; rest = rest.substring(clazz.value.length); continue }
                val token = Regex("^icon\\(([A-Za-z0-9_-]*(?::[A-Za-z0-9_-]+)?)\\)(?=\\s|$)").find(rest)
                if (token != null) { icon = token.groupValues[1]; rest = rest.substring(token.value.length); continue }
                return fail("Invalid treeView annotation", index + 1)
            }
            val indentation = raw.takeWhile { it == ' ' || it == '\t' }.length
            while (stack.isNotEmpty() && stack.last().first >= indentation) stack.removeAt(stack.lastIndex)
            val parent = stack.lastOrNull()?.second; val depth = parent?.let { nodes[it].depth + 1 } ?: 0
            val label = if (name == "/") "/" else name.removeSuffix("/")
            nodes += TreeViewNode(label, depth, parent, name.endsWith('/'), indentation.takeIf { it > 0 }, className, icon, desc)
            stack += indentation to nodes.lastIndex
        }
        if (!header) return fail("Expected treeView-beta", 1)
        if (description != null) return fail("Unclosed treeView accessibility description", source.lines().size)
        return MermaidParseResult.Success(TreeViewDiagram(nodes, title, accTitle, accDescription))
    }
}
