package build.raft.mermaid.core

/** Grammar values are retained independently of the renderer's axis projection. */
internal class RadarParser(private val source: String) {
    private var offset = 0
    private fun skip() {
        while (offset < source.length) {
            if (source[offset].isWhitespace()) offset++
            else if (source.startsWith("%%", offset)) offset = source.indexOf('\n', offset).takeIf { it >= 0 } ?: source.length
            else break
        }
    }
    private fun take(pattern: String): String? {
        skip()
        val value = Regex("^(?:$pattern)").find(source.substring(offset))?.value ?: return null
        offset += value.length
        return value
    }
    private fun punctuation(value: String): Boolean {
        skip()
        if (!source.startsWith(value, offset)) return false
        offset += value.length
        return true
    }
    private fun optionSeparator() {
        if (punctuation(",")) {
            skip()
            require(Regex("^(?:max|min|ticks|showLegend|graticule)(?=\\s|$)").containsMatchIn(source.substring(offset))) {
                "Expected radar option after comma"
            }
        }
    }
    private fun identifier(): String = take("\\w(?:[-\\w]*\\w)?") ?: error("Expected radar identifier")
    private fun number(): Double = (take("(?:[0-9]+\\.[0-9]+|0|[1-9][0-9]*)") ?: error("Expected radar number"))
        .toDouble().also { require(it.isFinite()) { "Radar numbers must be finite" } }
    private fun label(): String? {
        if (!punctuation("[")) return null
        skip()
        val quote = source.getOrNull(offset++)
        require(quote == '"' || quote == '\'') { "Expected quoted radar label" }
        val text = StringBuilder()
        var closed = false
        while (offset < source.length) {
            val c = source[offset++]
            if (c == quote) { closed = true; break }
            if (c == '\\') { require(offset < source.length); text.append(source[offset++]) } else text.append(c)
        }
        require(closed && punctuation("]")) { "Unclosed radar label" }
        return text.toString()
    }
    fun parse(): MermaidParseResult = try {
        require(take("radar-beta(?=\\s|:|$)") != null) { "Invalid radar header" }
        punctuation(":")
        var title: String? = null; var accTitle: String? = null; var accDescription: String? = null
        val axes = mutableListOf<RadarAxis>()
        val curves = mutableListOf<RadarCurve>()
        val options = mutableListOf<RadarOption>()
        while (true) {
            skip(); if (offset >= source.length) break
            val keyword = identifier()
            when (keyword) {
                "title", "accTitle", "accDescr" -> {
                    if (keyword == "accDescr" && punctuation("{")) {
                        val end = source.indexOf('}', offset)
                        require(end >= 0) { "Unclosed radar accessibility description" }
                        accDescription = source.substring(offset, end).trim().replace(Regex("\\n\\s+"), "\n")
                        offset = end + 1
                    } else {
                        if (keyword != "title") require(punctuation(":"))
                        val end = source.indexOf('\n', offset).takeIf { it >= 0 } ?: source.length
                        val text = source.substring(offset, end).substringBefore("%%").trim()
                        when (keyword) { "title" -> title = text; "accTitle" -> accTitle = text; else -> accDescription = text }
                        offset = end
                    }
                }
                "axis" -> do {
                    val id = identifier(); axes += RadarAxis(id, label() ?: id)
                } while (punctuation(","))
                "curve" -> do {
                    val id = identifier(); val text = label() ?: id
                    require(punctuation("{")) { "Expected radar curve entries" }
                    val entries = mutableListOf<RadarEntry>()
                    var detailed: Boolean? = null
                    do {
                        skip()
                        val named = source.getOrNull(offset)?.let { !it.isDigit() } ?: false
                        if (detailed == null) detailed = named else require(detailed == named) { "Cannot mix named and positional radar entries" }
                        val axis = if (named) identifier().also { punctuation(":") } else null
                        entries += RadarEntry(number(), axis)
                    } while (punctuation(","))
                    require(punctuation("}")) { "Expected end of radar curve" }
                    curves += RadarCurve(id, text, entries.map { it.value }, entries)
                } while (punctuation(","))
                "max", "min", "ticks" -> { options += RadarOption(keyword, number = number()); optionSeparator() }
                "showLegend" -> {
                    val value = identifier(); require(value == "true" || value == "false")
                    options += RadarOption(keyword, flag = value == "true"); optionSeparator()
                }
                "graticule" -> {
                    val value = identifier(); require(value == "circle" || value == "polygon")
                    options += RadarOption(keyword, text = value); optionSeparator()
                }
                else -> error("Unsupported radar statement: $keyword")
            }
        }
        val maximum = options.lastOrNull { it.name == "max" }?.number ?: maxOf(100.0, curves.flatMap { it.values }.maxOrNull() ?: 0.0)
        val minimum = options.lastOrNull { it.name == "min" }?.number ?: 0.0
        MermaidParseResult.Success(RadarChartDiagram(title, axes, curves, maximum, minimum, options, accTitle, accDescription))
    } catch (error: IllegalArgumentException) {
        failure(error.message ?: "Invalid radar value")
    } catch (error: IllegalStateException) {
        failure(error.message ?: "Invalid radar syntax")
    }
    private fun failure(message: String): MermaidParseResult.Failure {
        val line = source.take(offset.coerceAtMost(source.length)).count { it == '\n' } + 1
        val column = offset - source.lastIndexOf('\n', (offset - 1).coerceAtLeast(0))
        return MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, message, SourceLocation(line, column.coerceAtLeast(1)))))
    }
}
