package build.raft.mermaid.core

/** Canonical line grammar, shared by the Native product entry and compatibility boundary. */
public class UsecaseParser(private val source: String) {
    private var pos = 0
    private val actors = linkedMapOf<String, UsecaseActor>()
    private val nodes = linkedMapOf<String, UsecaseNode>()
    private val edges = mutableListOf<UsecaseRelationship>()
    private var anonymousEdgeCount = 0
    private val boundaries = mutableListOf<UsecaseBoundary>()
    private val notes = mutableListOf<UsecaseNote>()
    private val jsonNodes = mutableListOf<UsecaseJsonNode>()
    private val jsonStarts = mutableListOf<Int>()
    private val attrs = linkedMapOf<String, UsecaseAttributes>()
    private val classes = linkedMapOf<String, Map<String, String>>()
    private var direction = FlowDirection.LR
    private var parent: String? = null
    private var accTitle: String? = null
    private var accDescr: String? = null
    private val idPattern = Regex("[A-Za-z0-9_]+")
    private val forbidden = setOf("allowmixing", "newpage", "package", "rectangle", "skinparam")
    private data class Entity(val id: String, val label: String, val shape: UsecaseShape, val explicit: Boolean, val attributes: UsecaseAttributes, val labelType: String)
    private fun fail(message: String): Nothing = throw IllegalArgumentException(message)
    private fun hws() { while (source.getOrNull(pos) == ' ' || source.getOrNull(pos) == '\t') pos++ }
    private fun at(text: String): Boolean { hws(); return source.startsWith(text, pos) }
    private fun take(text: String): Boolean { if (!at(text)) return false; pos += text.length; return true }
    private fun requireText(text: String) { if (!take(text)) fail("Expected $text") }
    private fun newline(): Boolean { if (source.getOrNull(pos) == '\r') { pos++; if (source.getOrNull(pos) == '\n') pos++; return true }; if (source.getOrNull(pos) == '\n') { pos++; return true }; return false }
    private fun endLine() { hws(); if (pos < source.length && !newline()) fail("Expected end of usecase statement") }
    private fun lineText(): String { hws(); val start = pos; while (pos < source.length && source[pos] != '\n' && source[pos] != '\r') pos++; return source.substring(start, pos).trim() }
    private fun identifier(): String { hws(); val m = idPattern.find(source, pos)?.takeIf { it.range.first == pos } ?: fail("Expected usecase identifier"); pos = m.range.last + 1; return m.value }
    private fun quoted(): String {
        hws(); requireText("\""); val markdown = source.getOrNull(pos) == '`'; if (markdown) pos++
        val start = pos
        while (pos < source.length) {
            if (markdown && source.startsWith("`\"", pos)) { val value = source.substring(start, pos); pos += 2; return value }
            if (!markdown && source[pos] == '"') { val value = source.substring(start, pos); pos++; return value }
            if (!markdown && source[pos] in "\r\n") fail("Physical newlines require a Markdown label")
            pos++
        }
        fail("Unclosed usecase label")
    }
    private fun name(): Pair<String, String> {
        hws(); if (at("\"")) { val label = quoted(); return label.replace(Regex("[^A-Za-z0-9_]"), "_") to label }
        val id = identifier(); return id to id
    }
    private fun label(close: Char): String {
        hws(); if (at("\"")) return quoted()
        val start = pos
        while (pos < source.length && source[pos] != close) {
            if (source[pos] in "\r\n;" || source.startsWith("@{", pos) || source.startsWith("<<", pos)) fail("Invalid unquoted usecase label")
            pos++
        }
        return source.substring(start, pos).trim().takeIf { it.isNotEmpty() } ?: fail("Empty usecase label")
    }
    private fun metadata(): Map<String, String> {
        requireText("@{"); val result = linkedMapOf<String, String>()
        fun lines() { hws(); while (newline()) hws() }
        lines(); if (take("}")) return result
        while (true) {
            val key = if (at("\"")) quoted() else identifier(); requireText(":")
            val value = if (at("\"")) quoted() else identifier(); result[key] = value; hws()
            if (take("}")) return result
            val comma = take(","); val hadLine = newline(); if (!comma && !hadLine) fail("Expected metadata separator")
            lines(); if (hadLine) { take(","); lines() }; if (take("}")) return result
        }
    }
    private fun classList(): List<String> { val result = mutableListOf(identifier()); while (take(",")) result += identifier(); return result }
    private fun entity(actor: Boolean = false, boundary: Boolean = false): Entity {
        var labelType = if (at("\"`")) "markdown" else "text"
        val (id, initialLabel) = name(); var text = initialLabel; var shape = UsecaseShape.ELLIPSE; var explicit = initialLabel != id
        if (take("(")) { labelType = if (at("\"`")) "markdown" else "text"; text = label(')'); requireText(")"); explicit = true }
        else if (!actor && take("[")) { labelType = if (at("\"`")) "markdown" else "text"; text = label(']'); requireText("]"); shape = UsecaseShape.RECTANGLE; explicit = true }
        val properties = if (at("@{")) metadata() else emptyMap()
        var stereotype: String? = null
        if (!boundary && take("<<")) {
            val start = pos; while (pos < source.length && !source.startsWith(">>", pos)) { if (source[pos] in "\r\n") fail("Multiline stereotype"); pos++ }
            stereotype = source.substring(start, pos).trim().takeIf { it.isNotEmpty() } ?: fail("Empty stereotype"); requireText(">>")
        }
        val assigned = if (take(":::")) classList() else emptyList()
        return Entity(id, text, shape, explicit, UsecaseAttributes(properties, stereotype, assigned, parentId = parent), labelType)
    }
    private fun publish(e: Entity, actor: Boolean) {
        if (!actor && jsonNodes.any { it.id == e.id }) return
        if (actor) { nodes.remove(e.id); val previous = actors[e.id]; if (previous != null && previous.label != e.label) fail("Conflicting actor declaration ${e.id}"); actors[e.id] = UsecaseActor(e.id, e.label, labelType = e.labelType) }
        else if (e.id !in actors) { val previous = nodes[e.id]; if (previous == null || e.explicit) nodes[e.id] = UsecaseNode(e.id, e.label, e.shape, e.labelType) }
        if (e.attributes != UsecaseAttributes()) attrs[e.id] = e.attributes
    }
    private fun relation(from: Entity) {
        var edgeId: String? = null; hws(); val candidate = idPattern.find(source, pos)?.takeIf { it.range.first == pos }
        if (candidate != null && source.getOrNull(candidate.range.last + 1) == '@') { edgeId = candidate.value; pos = candidate.range.last + 2 }
        hws(); val match = Regex("--\\|>|\\.\\.>|<--+|o--|x--|--o|--x|--+>|--+").find(source, pos)?.takeIf { it.range.first == pos } ?: fail("Expected usecase relation")
        var operator = match.value; pos = match.range.last + 1; var edgeLabel: String? = null
        if (operator == "..>") { requireText(":"); edgeLabel = identifier().lowercase(); if (edgeLabel !in listOf("include", "extend")) fail("Expected include or extend") }
        else if (operator in listOf("--", "<--", "o--", "x--")) {
            val restStart = pos
            hws()
            if (source.getOrNull(pos) == '"') {
                val text = quoted(); hws()
                val right = Regex("--+(?:>|[ox])?").find(source, pos)?.takeIf { it.range.first == pos }
                if (right == null) pos = restStart // A quoted destination, not a relation label.
                else {
                    if (operator != "--" && right.value != "--") fail("Invalid reverse labelled relation")
                    edgeLabel = text
                    if (operator == "--") operator = right.value
                    pos = right.range.last + 1
                }
            } else {
                pos = restStart
                val rest = source.substring(pos).takeWhile { it !in "\r\n" }
                val right = Regex("\\s*(--+(?:>|[ox])?)\\s*").find(rest)
                if (right != null) {
                    val raw = rest.substring(0, right.range.first).trim()
                    if (raw.isNotEmpty()) {
                        edgeLabel = raw
                        val rightOperator = right.groupValues[1]
                        if (operator != "--" && rightOperator != "--") fail("Invalid reverse labelled relation")
                        if (operator == "--") operator = rightOperator
                        pos = restStart + right.range.last + 1
                    }
                }
            }
        }
        val to = entity(); publish(to, false)
        val startMarker = when { operator.startsWith('<') -> "arrow"; operator.startsWith('o') -> "circle"; operator.startsWith('x') -> "cross"; else -> "none" }
        val endMarker = when { operator == "--|>" -> "generalization"; operator.endsWith('>') -> "arrow"; operator.endsWith('o') -> "circle"; operator.endsWith('x') -> "cross"; else -> "none" }
        edges += UsecaseRelationship(from.id, to.id, edgeLabel, edgeId ?: "edge-${anonymousEdgeCount++}", startMarker, endMarker, operator == "..>")
    }
    private fun styleValues(): Map<String, String> {
        val text = lineText(); if (';' in text) fail("Semicolons are not CSS separators")
        val fields = Regex("(?<!\\\\),").split(text); val result = linkedMapOf<String, String>()
        for (field in fields) { val separator = field.indexOf(':'); if (separator <= 0 || field.substring(separator + 1).isBlank()) fail("Invalid usecase style"); result[field.substring(0, separator).trim()] = field.substring(separator + 1).trim().replace("\\,", ",") }
        return result
    }
    private fun jsonObject(): String {
        hws(); val start = pos; requireText("{"); var depth = 1; var quoted = false; var escaped = false
        while (pos < source.length) { val c = source[pos++]; if (quoted) { if (escaped) escaped = false else if (c == '\\') escaped = true else if (c == '"') quoted = false } else if (c == '"') quoted = true else if (c == '{') depth++ else if (c == '}' && --depth == 0) return source.substring(start, pos) }
        fail("Unclosed JSON object")
    }
    public fun parse(): MermaidParseResult = try {
        hws(); if (!take("usecase-beta") && !take("usecaseDiagram")) fail("Expected usecase-beta"); endLine()
        while (true) {
            hws(); if (pos >= source.length) break
            if (newline()) continue
            if (take("%%")) { lineText(); endLine(); continue }
            val start = pos; val keyword = idPattern.find(source, pos)?.takeIf { it.range.first == pos }?.value
            if (parent != null && keyword in setOf("systemBoundary", "note", "json", "direction", "classDef", "class", "style", "accTitle", "accDescr")) fail("Only declarations are allowed inside a system boundary")
            when (keyword) {
                "end" -> { pos += 3; if (parent == null) fail("Unexpected boundary end"); parent = null }
                "direction" -> { pos += 9; val value = identifier(); direction = when (value) { "LR" -> FlowDirection.LR; "RL" -> FlowDirection.RL; "BT" -> FlowDirection.BT; "TB", "TD" -> FlowDirection.TB; else -> fail("Invalid usecase direction") } }
                "accTitle" -> { pos += 8; requireText(":"); accTitle = lineText() }
                "accDescr" -> { pos += 8; if (take(":")) accDescr = lineText() else { requireText("{"); val end = source.indexOf('}', pos); if (end < 0) fail("Unclosed accessibility description"); accDescr = source.substring(pos, end).trim(); pos = end + 1 } }
                "systemBoundary" -> { pos += 14; val e = entity(boundary = true); boundaries += UsecaseBoundary(e.id, e.label, e.labelType); if (e.attributes != UsecaseAttributes()) attrs[e.id] = e.attributes; parent = e.id }
                "actor" -> { pos += 5; val e = entity(actor = true); publish(e, true); if (take(",")) { do { publish(entity(actor = true), true) } while (take(",")) } else { hws(); if (pos < source.length && source[pos] !in "\r\n") { if (parent != null) fail("Relations are not allowed inside a boundary"); relation(e) } } }
                "note" -> { pos += 4; if (identifier() != "for") fail("Expected note for"); val target = identifier(); if (at(",")) fail("Notes require a single target"); val text = if (at("\"")) quoted() else lineText(); if (text.isBlank() || text.startsWith("as ")) fail("Invalid note label"); notes += UsecaseNote(target, text, "note-${notes.size}") }
                "json" -> { pos += 4; val id = identifier(); requireText("@"); hws(); jsonStarts += pos; val json = jsonObject(); val assigned = if (take(":::")) classList() else emptyList(); jsonNodes += UsecaseJsonNode(id, json); if (assigned.isNotEmpty()) attrs[id] = UsecaseAttributes(classes = assigned) }
                "classDef" -> { pos += 8; val ids = classList(); val values = styleValues(); ids.forEach { classes[it] = values } }
                "class" -> { pos += 5; val ids = classList(); val assigned = classList(); ids.forEach { attrs[it] = (attrs[it] ?: UsecaseAttributes()).copy(classes = (attrs[it]?.classes.orEmpty() + assigned)) } }
                "style" -> { pos += 5; val id = identifier(); val values = styleValues(); attrs[id] = (attrs[id] ?: UsecaseAttributes()).copy(styles = attrs[id]?.styles.orEmpty() + values) }
                else -> {
                    if (keyword?.lowercase() in forbidden) fail("Unsupported PlantUML statement")
                    val e = entity(); hws()
                    // A bare target followed by metadata is an assignment, not a declaration.
                    if (!e.explicit && e.attributes.properties.isNotEmpty() && e.attributes.stereotype == null && e.attributes.classes.isEmpty() && parent == null && (pos == source.length || source[pos] in "\r\n")) attrs[e.id] = (attrs[e.id] ?: UsecaseAttributes()).copy(properties = attrs[e.id]?.properties.orEmpty() + e.attributes.properties)
                    else { publish(e, false); if (pos < source.length && source[pos] !in "\r\n") { if (parent != null) fail("Relations are not allowed inside a boundary"); relation(e) } }
                }
            }
            if (pos == start) fail("Invalid usecase statement"); endLine()
        }
        if (parent != null) fail("Unclosed system boundary")
        for (boundary in boundaries) {
            val attributes = attrs[boundary.id] ?: UsecaseAttributes()
            attrs[boundary.id] = attributes.copy(properties = mapOf("type" to "rect") + attributes.properties)
        }
        MermaidParseResult.Success(UsecaseDiagram(direction, actors.values.toList(), nodes.values.toList(), edges, boundaries, notes, jsonNodes, attrs, classes, accTitle, accDescr))
    } catch (e: IllegalArgumentException) {
        val prefix = source.take(pos); val lines = prefix.split(Regex("\\r\\n|\\r|\\n"))
        MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, e.message ?: "Invalid usecase syntax", SourceLocation(lines.size, lines.last().length + 1))))
    }

    public fun parseValidated(): MermaidParseResult {
        val result = parse(); if (result !is MermaidParseResult.Success) return result
        val d = result.diagram as UsecaseDiagram
        val validatedJson = try {
            d.jsonNodes.mapIndexed { index, node ->
                val lines = source.take(jsonStarts[index]).split(Regex("\\r\\n|\\r|\\n"))
                node.copy(data = UsecaseJsonParser.parseOrderedJsonObject(node.source, lines.size, lines.last().length + 1))
            }
        } catch (e: UsecaseJsonError) {
            return MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, e.message ?: "Invalid JSON", SourceLocation(e.line, e.column))))
        }
        val nodeIds = (d.actors.map { it.id } + d.useCases.map { it.id }).toSet()
        val allIds = nodeIds + d.boundaries.map { it.id } + d.jsonNodes.map { it.id } + d.relationships.mapNotNull { it.id }
        val unknown = d.notes.firstOrNull { it.targetId !in nodeIds }?.targetId ?: d.attributes.keys.firstOrNull { it !in allIds }
        if (unknown != null) return MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, "Unknown usecase target $unknown", SourceLocation(1, 1))))
        val validatedActors = mutableListOf<UsecaseActor>()
        for (actor in d.actors) {
            val properties = d.attributes[actor.id]?.properties.orEmpty()
            val declared = properties["type"] ?: "normal"
            val icon = properties["icon"]?.takeIf { it.isNotEmpty() }
            val business = properties["business"] == "true"
            val error = when {
                declared !in listOf("normal", "hollow", "awesome") -> "Invalid actor type '$declared' for '${actor.id}'"
                properties["business"] != null && properties["business"] !in listOf("true", "false") -> "Invalid business value for '${actor.id}'"
                icon != null && declared != "normal" -> "Actor '${actor.id}' cannot combine icon with type '$declared'"
                business && (icon != null || declared == "awesome") -> "Business actor '${actor.id}' must use normal or hollow geometry"
                else -> null
            }
            if (error != null) return MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, error, SourceLocation(1, 1))))
            val type = if (icon != null) UsecaseActorType.ICON else when (declared) { "hollow" -> UsecaseActorType.HOLLOW; "awesome" -> UsecaseActorType.AWESOME; else -> UsecaseActorType.NORMAL }
            validatedActors += actor.copy(type = type, icon = icon, business = business)
        }
        return MermaidParseResult.Success(d.copy(actors = validatedActors, jsonNodes = validatedJson))
    }
}
