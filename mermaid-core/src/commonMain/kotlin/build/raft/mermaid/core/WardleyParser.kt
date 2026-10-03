package build.raft.mermaid.core

internal class WardleyParser(private val source: String) {
    private var line = 1
    private fun fail(message: String): Nothing = throw IllegalArgumentException(message)
    private fun name(raw: String): String {
        val text = raw.trim(); val value = if (text.startsWith('"') && text.endsWith('"')) text.substring(1, text.length - 1) else text
        if (value.isEmpty() || value.none { it.isLetterOrDigit() || it == '_' } || value.any { !it.isLetterOrDigit() && it !in "_ -()&" }) fail("Invalid Wardley name")
        return value
    }
    private fun coordinate(raw: String): Double {
        if (!Regex("[0-9]+(?:\\.[0-9]+)?").matches(raw.trim())) fail("Wardley coordinates require decimal numbers")
        return raw.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..1.0 } ?: fail("Wardley coordinates must be in [0, 1]")
    }
    private fun pair(raw: String): Pair<Double, Double> { val values = raw.split(','); if (values.size != 2) fail("Wardley requires [visibility, evolution]"); return coordinate(values[0]) to coordinate(values[1]) }
    fun parse(): MermaidParseResult = try {
        val nodes = linkedMapOf<String, WardleyNode>(); val links = mutableListOf<WardleyLink>(); val evolutions = mutableListOf<WardleyEvolution>(); val notes = mutableListOf<WardleyNote>()
        val annotations = mutableListOf<WardleyAnnotation>(); val accelerators = mutableListOf<WardleyNote>(); val deaccelerators = mutableListOf<WardleyNote>()
        var title: String? = null; var pipeline: String? = null; var annotationBox: WardleyNote? = null; var width: Double? = null; var height: Double? = null
        var stages = listOf("Genesis", "Custom Built", "Product", "Commodity"); var boundaries = emptyList<Double>(); var header = false
        val positioned = Regex("^(.+?)\\s*\\[([^]]+)](.*)$")
        source.lineSequence().forEachIndexed { index, raw ->
            line = index + 1; val text = raw.substringBefore("%%").trim()
            if (text.isEmpty()) return@forEachIndexed
            if (!header) { if (text != "wardley-beta") fail("Expected wardley-beta"); header = true; return@forEachIndexed }
            when {
                text.startsWith("title ") -> { if (title != null) fail("Duplicate Wardley title"); title = text.substringAfter(' ') }
                text.startsWith("evolution ") -> {
                    val values = text.substringAfter(' ').split("->").map { it.trim() }; if (values.size < 2 || values.any { it.isEmpty() }) fail("Invalid Wardley stages")
                    stages = values.map { it.substringBeforeLast('@').trim() }
                    boundaries = if (values.any { '@' in it }) values.map { coordinate(it.substringAfterLast('@', "")) } else emptyList()
                    if (boundaries.isNotEmpty() && (boundaries.zipWithNext().any { it.first >= it.second } || boundaries.last() != 1.0)) fail("Wardley stage boundaries must increase to 1")
                }
                text.startsWith("size ") -> { val match = Regex("^size\\s+\\[([^,]+),([^]]+)]$").matchEntire(text) ?: fail("Invalid Wardley size"); width = match.groupValues[1].trim().toDoubleOrNull()?.takeIf { it.isFinite() && it in 200.0..10000.0 } ?: fail("Invalid Wardley width"); height = match.groupValues[2].trim().toDoubleOrNull()?.takeIf { it.isFinite() && it in 200.0..10000.0 } ?: fail("Invalid Wardley height") }
                text.startsWith("pipeline ") -> { if (pipeline != null || !text.endsWith('{')) fail("Invalid Wardley pipeline"); pipeline = name(text.removePrefix("pipeline ").dropLast(1)); if (pipeline !in nodes) fail("Unknown Wardley pipeline component") }
                text == "}" -> { if (pipeline == null) fail("Unexpected Wardley pipeline end"); pipeline = null }
                text.startsWith("component ") || text.startsWith("anchor ") -> {
                    val anchor = text.startsWith("anchor "); val match = positioned.matchEntire(text.substringAfter(' ')) ?: fail("Invalid Wardley component")
                    val label = name(match.groupValues[1]); if (label in nodes) fail("Duplicate Wardley component")
                    val values = match.groupValues[2].split(','); val parent = pipeline
                    val coordinates = if (values.size == 1 && parent != null) nodes.getValue(parent).visibility to coordinate(values[0]) else pair(match.groupValues[2])
                    var tail = match.groupValues[3].trim(); var strategy: String? = null; var inertia = false; var dx: Double? = null; var dy: Double? = null
                    val offset = Regex("label\\s*\\[([^,]+),([^]]+)]").find(tail)
                    if (offset != null) { dx = offset.groupValues[1].trim().toDoubleOrNull()?.takeIf { it.isFinite() } ?: fail("Invalid Wardley label offset"); dy = offset.groupValues[2].trim().toDoubleOrNull()?.takeIf { it.isFinite() } ?: fail("Invalid Wardley label offset"); tail = tail.removeRange(offset.range).trim() }
                    if (parent != null && tail.isNotEmpty()) fail("Wardley pipeline components support label offsets, not decorators")
                    while (tail.isNotEmpty()) { val token = Regex("^\\((build|buy|outsource|inertia)\\)").find(tail) ?: fail("Unsupported Wardley component decorator"); if (token.groupValues[1] == "inertia") inertia = true else strategy = token.groupValues[1]; tail = tail.substring(token.value.length).trim() }
                    nodes[label] = WardleyNode(label, coordinates.first, coordinates.second, anchor, parent, dx, dy, strategy, inertia)
                }
                text.startsWith("evolve ") -> { val body = text.substringAfter(' '); val label = name(body.substringBeforeLast(' ')); val target = coordinate(body.substringAfterLast(' ')); if (label !in nodes || evolutions.any { it.component == label }) fail("Unknown or duplicate Wardley evolution"); evolutions += WardleyEvolution(label, target) }
                text.startsWith("annotations ") -> { val match = Regex("^annotations\\s*\\[([^]]+)]$").matchEntire(text) ?: fail("Invalid Wardley annotations placement"); val p = pair(match.groupValues[1]); annotationBox = WardleyNote("", p.first, p.second) }
                text.startsWith("annotation ") -> { val match = Regex("^annotation\\s+([0-9]+),\\s*\\[([^]]+)]\\s*\"([^\"]*)\"$").matchEntire(text) ?: fail("Invalid Wardley annotation"); val p = pair(match.groupValues[2]); annotations += WardleyAnnotation(match.groupValues[1], match.groupValues[3], p.first, p.second) }
                text.startsWith("accelerator ") || text.startsWith("deaccelerator ") -> {
                    val match = positioned.matchEntire(text.substringAfter(' ')) ?: fail("Invalid Wardley force")
                    if (match.groupValues[3].isNotBlank()) fail("Unexpected Wardley force suffix")
                    val label = name(match.groupValues[1]); val p = pair(match.groupValues[2])
                    val force = WardleyNote(label, p.first, p.second)
                    if (text.startsWith("accelerator ")) accelerators += force else deaccelerators += force
                }
                text.startsWith("note ") -> {
                    val match = Regex("^(note|accelerator|deaccelerator)\\s+\"([^\"]*)\"\\s*\\[([^]]+)]$").matchEntire(text) ?: fail("Invalid Wardley note")
                    val p = pair(match.groupValues[3]); val note = WardleyNote(match.groupValues[2], p.first, p.second)
                    when (match.groupValues[1]) { "note" -> notes += note; "accelerator" -> accelerators += note; else -> deaccelerators += note }
                }
                else -> {
                    val match = Regex("^(.+?)(\\+<>|->)([^;]+?)(?:;\\s*(.*))?$").matchEntire(text) ?: fail("Unsupported Wardley syntax")
                    val from = name(match.groupValues[1]); val to = name(match.groupValues[3]); if (from !in nodes || to !in nodes || from == to) fail("Unknown or self Wardley link")
                    links += WardleyLink(from, to, if (match.groupValues[2] == "+<>") "bidirectional" else "forward", match.groupValues[4].ifEmpty { null })
                }
            }
        }
        if (!header || nodes.isEmpty() || pipeline != null) fail("Wardley requires components and closed pipelines")
        MermaidParseResult.Success(WardleyMapDiagram(title, nodes.values.toList(), links, evolutions, notes, stages, boundaries, annotations, annotationBox, accelerators, deaccelerators, width, height))
    } catch (error: IllegalArgumentException) { MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, error.message ?: "Invalid Wardley source", SourceLocation(line, 1)))) }
}
