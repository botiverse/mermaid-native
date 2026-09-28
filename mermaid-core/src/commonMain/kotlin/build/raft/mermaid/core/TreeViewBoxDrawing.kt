package build.raft.mermaid.core

/** Kotlin counterpart of pinned Mermaid treeView boxDrawingPreprocessor. */
internal object TreeViewBoxDrawing {
    data class Result(val text: String, val lineMap: Map<Int, Int>)
    private val all = Regex("[─━│┃└┗├┣]")
    private val branch = Regex("[└┗├┣]")
    private val decoration = Regex("^[\\s│┃]+$")
    private val metadata = Regex("^\\s*(title[\\t ]|accTitle[\\t ]*:|accDescr[\\t ]*[:{])")
    private val comment = Regex("^\\s*%%")
    fun isBoxDrawingFormat(lines: List<String>): Boolean = lines.any { all.containsMatchIn(it) }
    fun remapErrorLines(message: String, lineMap: Map<Int, Int>): String = Regex("\\bline\\s+(\\d+)\\b", RegexOption.IGNORE_CASE).replace(message) { match ->
        lineMap[match.groupValues[1].toIntOrNull()]?.takeIf { it != 0 }?.let { "line $it" } ?: match.value
    }
    fun preprocess(input: String): Result {
        val lines = input.split('\n'); val header = lines.indexOfFirst { it.trim() == "treeView-beta" }
        if (header < 0) return Result(input, emptyMap())
        val content = lines.drop(header + 1).filterNot { it.isBlank() || comment.containsMatchIn(it) || metadata.containsMatchIn(it) || decoration.matches(it) }.map { it.replace("\t", "    ") }
        if (!isBoxDrawingFormat(content)) return Result(input, emptyMap())
        val segment = content.firstNotNullOfOrNull { branch.find(it)?.range?.first?.takeIf { column -> column > 0 } } ?: 4
        val output = mutableListOf<String>(); val mapping = linkedMapOf<Int, Int>()
        fun emit(line: String, original: Int) { output += line; mapping[output.size] = original }
        lines.forEachIndexed { index, line ->
            val original = index + 1
            if (index <= header || line.isBlank() || comment.containsMatchIn(line) || metadata.containsMatchIn(line)) { emit(line, original); return@forEachIndexed }
            if (decoration.matches(line)) return@forEachIndexed
            val normalized = line.replace("\t", "    "); val match = branch.find(normalized)
            if (match != null) {
                val column = match.range.first
                val depth = kotlin.math.floor(column.toDouble() / segment + 0.5).toInt() + 1
                var start = column + 1
                while (start < normalized.length && normalized[start] in "─━") start++
                while (start < normalized.length && normalized[start] == ' ') start++
                val name = normalized.substring(start).trimEnd()
                require(name.isNotEmpty()) { "Line $original: Empty node — expected a filename or directory name after the box-drawing prefix" }
                emit("    ".repeat(depth) + name, original)
            } else if (Regex("^[\\s─━│┃└┗├┣]+$").matches(normalized)) {
                return@forEachIndexed
            } else if (all.containsMatchIn(normalized)) {
                emit(line, original)
            } else if (normalized.firstOrNull()?.isWhitespace() == true) {
                throw IllegalArgumentException("Line $original: Unexpected indentation without box-drawing characters. In box-drawing format, use ├── or └── prefixes for indented nodes.")
            } else emit(line, original)
        }
        return Result(output.joinToString("\n"), mapping)
    }
}
