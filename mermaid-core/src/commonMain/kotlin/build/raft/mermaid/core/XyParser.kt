package build.raft.mermaid.core

/** Parse the original XY statement grammar before applying rendering defaults. */
internal class XyParser(private val source: String) {
    private data class Text(val value: String, val type: String = "text")
    private val number = "[+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)"
    private fun text(raw: String): Text {
        val s = raw.trim()
        require(s.isNotEmpty()) { "Expected text" }
        if (s.startsWith('"')) {
            require(s.length >= 2 && s.endsWith('"') && '"' !in s.substring(1, s.lastIndex)) { "Unclosed or extra quote" }
            val body = s.substring(1, s.lastIndex)
            return if (body.startsWith('`') && body.endsWith('`')) Text(body.drop(1).dropLast(1), "markdown") else Text(body)
        }
        require(s.all { it.isLetterOrDigit() && it.code < 128 || it.isWhitespace() || it in "&+=*.#-_" }) { "Invalid text" }
        return Text(s.filterNot { it.isWhitespace() })
    }
    private fun split(raw: String, delimiters: Set<Char>): List<String> {
        val result = mutableListOf<String>(); val part = StringBuilder(); var quoted = false; var braces = 0; var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '"') quoted = !quoted
            if (!quoted && c == '%' && raw.getOrNull(i + 1) == '%') {
                while (i < raw.length && raw[i] != '\n') i++
                continue
            }
            if (!quoted) { if (c == '{') braces++; if (c == '}') braces-- }
            if (!quoted && braces == 0 && c in delimiters) { result += part.toString(); part.clear() } else part.append(c)
            i++
        }
        require(!quoted && braces == 0) { "Unclosed text or accessibility block" }
        result += part.toString(); return result
    }
    fun parse(): MermaidParseResult {
        var line = 1
        return try {
            val statements = split(source, setOf('\n', ';')).map { it.trim() }.filter { it.isNotEmpty() }
            val header = Regex("xychart(?:-beta)?(?:\\s+(horizontal|vertical))?", RegexOption.IGNORE_CASE).matchEntire(statements.first())
                ?: error("Invalid XY chart header")
            val orientation = header.groupValues[1].takeIf { it.isNotEmpty() }?.let { XyOrientation.valueOf(it.uppercase()) }
            var title: String? = null; var accTitle: String? = null; var accDescription: String? = null
            var x = XyAxis(); var y = NumericAxis(minimum = 0.0, maximum = 1.0, explicitRange = false)
            val series = mutableListOf<XySeries>()
            statements.drop(1).forEachIndexed { index, statement ->
                line = index + 2
                val key = statement.takeWhile { !it.isWhitespace() && it != ':' && it != '{' }.lowercase()
                val body = statement.drop(key.length).trim()
                when (key) {
                    "title" -> title = text(body).value.trim()
                    "acctitle" -> { require(body.startsWith(':')); accTitle = body.drop(1).trim() }
                    "accdescr" -> accDescription = when {
                        body.startsWith(':') -> body.drop(1).trim()
                        body.startsWith('{') && body.endsWith('}') -> body.drop(1).dropLast(1).trim()
                        else -> error("Invalid accessibility description")
                    }
                    "x-axis", "y-axis" -> {
                        var quote = false
                        val bracket = body.indices.firstOrNull { i -> if (body[i] == '"') quote = !quote; !quote && body[i] == '[' }
                        if (bracket != null) {
                            require(key == "x-axis" && body.endsWith(']')) { "Only x-axis accepts categories" }
                            val name = body.substring(0, bracket).takeIf { it.isNotBlank() }?.let(::text) ?: Text("")
                            val cats = split(body.substring(bracket + 1, body.lastIndex), setOf(',')).map(::text)
                            x = XyAxis(name.value, cats.map { it.value }, titleType = name.type, categoryTypes = if(cats.any { it.type != "text" }) cats.map { it.type } else emptyList())
                        } else {
                            val range = Regex("^(.*?)($number)\\s*-->\\s*($number)$").matchEntire(body)
                            val name = if(range != null) range.groupValues[1].takeIf { it.isNotBlank() }?.let(::text) ?: Text("") else text(body)
                            val numeric = range?.let {
                                val low = it.groupValues[2].toDouble(); val high = it.groupValues[3].toDouble()
                                require(low.isFinite() && high.isFinite() && (high - low).isFinite()) { "Axis range must be finite" }
                                NumericAxis(name.value, low, high, titleType = name.type)
                            }
                            if(key == "x-axis") x = XyAxis(name.value, range = numeric, titleType = name.type)
                            else y = numeric ?: y.copy(title = name.value, titleType = name.type)
                        }
                    }
                    "line", "bar" -> {
                        var quoted = false
                        val opening = body.indices.firstOrNull { i -> if(body[i] == '"') quoted = !quoted; !quoted && body[i] == '[' } ?: error("Expected data list")
                        require(body.endsWith(']')) { "Unclosed data list" }
                        val name = body.substring(0, opening).takeIf { it.isNotBlank() }?.let(::text) ?: Text("")
                        val points = split(body.substring(opening + 1, body.lastIndex), setOf(',')).map { raw ->
                            val match = Regex("^\\s*($number)(?:\\s+\"([^\"]*)\")?\\s*$").matchEntire(raw) ?: error("Invalid data point")
                            match.groupValues[1].toDouble().also { require(it.isFinite()) } to match.groupValues[2]
                        }
                        series += XySeries(XySeriesKind.valueOf(key.uppercase()), points.map { it.first }, name.value,
                            if(points.any { it.second.isNotEmpty() }) points.map { it.second } else emptyList(), name.type)
                    }
                    else -> error("Unsupported XY statement")
                }
            }
            if(!y.explicitRange) {
                val values = series.flatMap { it.valuesFor(x).filterNotNull() }
                val low = values.minOrNull() ?: 0.0; val high = values.maxOrNull() ?: 1.0
                y = y.copy(minimum = low, maximum = if(low == high) high + 1.0 else high)
            }
            MermaidParseResult.Success(XyChartDiagram(title, x, y, series, orientation, accTitle, accDescription))
        } catch (error: IllegalArgumentException) {
            MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.INVALID_VALUE, error.message ?: "Invalid XY chart", SourceLocation(line, 1))))
        } catch (error: IllegalStateException) {
            MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, error.message ?: "Invalid XY chart", SourceLocation(line, 1))))
        }
    }
}
