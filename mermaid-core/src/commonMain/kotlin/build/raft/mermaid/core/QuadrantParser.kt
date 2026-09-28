package build.raft.mermaid.core

/** Retains original label text and point lexemes independently of their numeric geometry. */
internal class QuadrantParser(private val source: String) {
    private var line = 1
    private data class Label(val text: String, val type: String = "text")
    private fun label(value: String): Label {
        val text = value.trimStart()
        require(text.isNotEmpty()) { "Expected quadrant label" }
        if (text.startsWith("\"`")) {
            require(text.trimEnd().endsWith("`\"")) { "Unclosed markdown label" }
            return Label(text.trimEnd().removeSurrounding("\"`", "`\""), "markdown")
        }
        if (text.startsWith('"')) {
            require(text.trimEnd().endsWith('"') && text.trimEnd().length > 1) { "Unclosed quadrant label" }
            return Label(text.trimEnd().removeSurrounding("\""))
        }
        require(text.none { it in "[](){}:\"" }) { "Quote special characters in quadrant labels" }
        return Label(text)
    }
    private fun styles(value: String): List<String> = if (value.isBlank()) emptyList() else value.split(',').map {
        val part = it.trim()
        val key = part.substringBefore(':').trim()
        val rawValue = part.substringAfter(':', "").trim()
        val (pattern, expected) = when (key) {
            "radius" -> Regex("[0-9]+") to "number"
            "color", "stroke-color" -> Regex("#?([0-9A-Fa-f]{6}|[0-9A-Fa-f]{3})") to "hex code"
            "stroke-width" -> Regex("[0-9]+px") to "number of pixels (eg. 10px)"
            else -> error("style named $key is not supported.")
        }
        require(pattern.matches(rawValue)) { "value for $key $rawValue is invalid, please use a valid $expected" }
        part
    }
    fun parse(): MermaidParseResult = try {
        val header = Regex("^(?:\\s|%%[^\\n]*(?:\\n|$))*quadrantChart\\b[ \\t]*", RegexOption.IGNORE_CASE).find(source)
        require(header != null) { "Expected quadrantChart" }
        var title: String? = null; var accTitle: String? = null; var accDescription: String? = null
        var xAxis = QuadrantAxis("", "", false, false)
        var yAxis = QuadrantAxis("", "", false, false)
        val labels = MutableList<String?>(4) { null }; val labelTypes = MutableList(4) { "text" }
        val points = mutableListOf<QuadrantPoint>(); val classes = linkedMapOf<String, List<String>>()
        val body = source.substring(header.range.last + 1)
        val statements = mutableListOf<Pair<Int, String>>()
        var quote = false; var braces = 0; val part = StringBuilder(); var currentLine = source.take(header.range.last + 1).count { it == '\n' } + 1
        var startLine = currentLine; var i = 0
        while (i < body.length) {
            val c = body[i]
            if (!quote && braces == 0 && body.startsWith("%%", i)) {
                while (i < body.length && body[i] != '\n') i++
                continue
            }
            if (c == '"') quote = !quote
            if (!quote && c == '{') braces++
            if (!quote && c == '}') braces--
            if (!quote && braces == 0 && (c == '\n' || c == ';')) {
                statements += startLine to part.toString(); part.clear(); startLine = currentLine + if (c == '\n') 1 else 0
            } else part.append(c)
            if (c == '\n') currentLine++
            i++
        }
        require(!quote && braces == 0) { "Unclosed quadrant text" }
        statements += startLine to part.toString()
        for ((sourceLine, raw) in statements) {
            line = sourceLine; val text = raw.trimStart().removeSuffix("\r")
            if (text.isBlank()) continue
            val axis = Regex("^([xy])-axis[ \\t]*(.*)$", RegexOption.IGNORE_CASE).matchEntire(text)
            val quadrant = Regex("^quadrant-([1-4])[ \\t]*(.*)$", RegexOption.IGNORE_CASE).matchEntire(text)
            val point = Regex("^(.*?)(?::::(\\w+))?\\s*:\\s*\\[\\s*(1|0(?:\\.\\d+)?)\\s*,\\s*(1|0(?:\\.\\d+)?)\\s*]\\s*(.*)$").matchEntire(text)
            when {
                Regex("^title(?:[ \\t]+|$)", RegexOption.IGNORE_CASE).containsMatchIn(text) && point == null -> title = text.substring(5).trim()
                Regex("^accTitle\\s*:", RegexOption.IGNORE_CASE).containsMatchIn(text) -> accTitle = text.substringAfter(':').trim()
                Regex("^accDescr\\s*:", RegexOption.IGNORE_CASE).containsMatchIn(text) -> accDescription = text.substringAfter(':').trim()
                Regex("^accDescr\\s*\\{", RegexOption.IGNORE_CASE).containsMatchIn(text) -> accDescription = text.substringAfter('{').substringBeforeLast('}').trim()
                text.startsWith("classDef ", true) -> {
                    val definition = text.substringAfter(' ').trimStart(); val id = definition.takeWhile { !it.isWhitespace() }
                    require(id.isNotEmpty()); classes[id] = styles(definition.substring(id.length).trim())
                }
                axis != null -> {
                    val content = axis.groupValues[2]
                    var quoted = false; var split = -1; var end = -1
                    for (at in content.indices) {
                        if (content[at] == '"') quoted = !quoted
                        if (!quoted && content.startsWith("--", at)) {
                            val delimiter = Regex("^-{2,}>").find(content.substring(at))
                            if (delimiter != null) { split = at; end = at + delimiter.value.length; break }
                        }
                    }
                    val low = label(if (split >= 0) content.substring(0, split).trimEnd() else content)
                    val rest = if (end >= 0) content.substring(end).trimStart() else ""
                    val high = rest.takeIf { it.isNotEmpty() }?.let(::label)
                    val previous = if (axis.groupValues[1].equals("x", true)) xAxis else yAxis
                    val resolved = QuadrantAxis(low.text + if (split >= 0 && high == null) " ⟶ " else "", high?.text ?: previous.highLabel, true, high != null || previous.highDefined, low.type, high?.type ?: previous.highType)
                    if (axis.groupValues[1].equals("x", true)) xAxis = resolved else yAxis = resolved
                }
                quadrant != null -> {
                    val at = quadrant.groupValues[1].toInt() - 1; val value = label(quadrant.groupValues[2])
                    labels[at] = value.text; labelTypes[at] = value.type
                }
                point != null -> {
                    val value = label(point.groupValues[1].trimEnd())
                    val rawX = point.groupValues[3]; val rawY = point.groupValues[4]
                    points += QuadrantPoint(value.text, rawX.toDouble(), rawY.toDouble(), value.type, point.groupValues[2], styles(point.groupValues[5]), rawX, rawY)
                }
                else -> error("Unsupported quadrantChart statement: $text")
            }
        }
        MermaidParseResult.Success(QuadrantChartDiagram(title, xAxis, yAxis, labels, points, classes, labelTypes, accTitle, accDescription))
    } catch (error: IllegalArgumentException) {
        failure(error.message ?: "Invalid quadrantChart value")
    } catch (error: IllegalStateException) {
        failure(error.message ?: "Invalid quadrantChart syntax")
    }
    private fun failure(message: String): MermaidParseResult.Failure = MermaidParseResult.Failure(
        listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, message, SourceLocation(line, 1))),
    )
}
