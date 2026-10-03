package build.raft.mermaid.core

/** Canonical line grammar, shared by the Native product entry and compatibility boundary. */
public class UsecaseParser(private val source: String) {
    public var sourceAst: UsecaseSourceAst? = null
        private set
    private val sourceTokens by lazy { UsecaseLexer.tokenize(source).tokens.associateBy { it.startOffset } }
    private var pos = 0
    private var headerSpan = listOf(0, 0)
    private val statements = mutableListOf<MutableMap<String, Any?>>()
    private var groupStatement: MutableMap<String, Any?>? = null
    private var groupChildren = mutableListOf<MutableMap<String, Any?>>()
    private var statementNodes = mutableListOf<Map<String, Any?>>()
    private var statementEdges = mutableListOf<Map<String, Any?>>()
    private var metadataOccurrences = mutableListOf<Map<String, Any?>>()
    private data class MetadataProperty(val key: String, val value: String, val boolean: Boolean, val range: List<Int>)
    private var metadataProperties = mutableListOf<MetadataProperty>()
    private val metadataAssignments = mutableListOf<Pair<String, List<MetadataProperty>>>()
    private fun span(start: Int, end: Int = pos): List<Int> = listOf(start, end)
    private fun labelSpan(start: Int, end: Int): List<Int> {
        val trim = if (source.startsWith("\"`", start)) 2 else if (source.getOrNull(start) in listOf('"', '\'')) 1 else 0
        return span(start + trim, end - trim)
    }
    private fun appendStatement(statement: MutableMap<String, Any?>) {
        if (groupStatement == null) statements += statement else groupChildren += statement
    }
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
    private val jsonDeclarationStart = Regex("json[\\t ]+\\w+[\\t ]*@[\\t ]*(?=\\{)")
    private val labelOperator = Regex("--\\|>|\\.\\.>|:::|@\\{|<<|<--+|--+>|--o|o--|--x|x--|--+")
    private val forbidden = setOf("allowmixing", "newpage", "package", "rectangle", "skinparam")
    private data class Entity(val id: String, val label: String, val shape: UsecaseShape, val explicit: Boolean, val attributes: UsecaseAttributes, val labelType: String, val occurrence: Map<String, Any?>, val generated: Boolean, val originLabel: String, val endpointDeclaration: Boolean)
    private fun fail(message: String): Nothing = throw IllegalArgumentException(message)
    private fun hws() { while (source.getOrNull(pos) == ' ' || source.getOrNull(pos) == '\t') pos++ }
    private fun at(text: String): Boolean { hws(); return source.startsWith(text, pos) }
    private fun take(text: String): Boolean { if (!at(text)) return false; pos += text.length; return true }
    private fun requireText(text: String) { if (!take(text)) fail("Expected $text") }
    private fun newline(): Boolean { if (source.getOrNull(pos) == '\r') { pos++; if (source.getOrNull(pos) == '\n') pos++; return true }; if (source.getOrNull(pos) == '\n') { pos++; return true }; return false }
    private fun endLine() { hws(); if (pos < source.length && !newline()) failGrammarToken("end of usecase statement") }
    private fun lineText(): String { hws(); val start = pos; while (pos < source.length && source[pos] != '\n' && source[pos] != '\r') pos++; return source.substring(start, pos).trim() }
    private fun identifier(): String { hws(); val m = idPattern.find(source, pos)?.takeIf { it.range.first == pos } ?: fail("Expected usecase identifier"); pos = m.range.last + 1; return m.value }
    private class LocatedSyntaxError(message: String, val location: SourceLocation) : IllegalArgumentException(message)
    private fun failQuotedLabel(start: Int, reason: String): Nothing {
        val lines = source.take(start).split(Regex("\\r\\n|\\r|\\n"))
        val location = SourceLocation(lines.size, lines.last().length + 1)
        throw LocatedSyntaxError(
            "Error lexing usecase diagram: $reason at line ${location.line}, column ${location.column} [$start,$pos)",
            location,
        )
    }
    private fun quoted(): String {
        hws(); val quoteStart = pos; val quote = source.getOrNull(pos) ?: fail("Expected quoted label"); requireText(quote.toString()); val markdown = quote == '"' && source.getOrNull(pos) == '`'; if (markdown) pos++
        val start = pos
        while (pos < source.length) {
            if (markdown && source.startsWith("`\"", pos)) { val value = source.substring(start, pos); pos += 2; return value }
            if (!markdown && source[pos] == quote) { val value = source.substring(start, pos); pos++; return value }
            if (!markdown && source[pos] in "\r\n") failQuotedLabel(quoteStart, "Physical newlines require a Markdown label")
            pos++
        }
        failQuotedLabel(quoteStart, "Unclosed usecase label")
    }
    private fun name(): Pair<String, String> {
        hws(); if (at("\"")) { val label = quoted(); return label.replace(Regex("[^A-Za-z0-9_]"), "_") to label }
        val id = identifier(); return id to id
    }
    private fun failGrammarToken(expected: String): Nothing {
        hws()
        val image = sourceTokens[pos]?.image
            ?: jsonDeclarationStart.matchAt(source, pos)?.value
            ?: labelOperator.matchAt(source, pos)?.value
            ?: idPattern.matchAt(source, pos)?.value
            ?: source.getOrNull(pos)?.toString().orEmpty()
        failToken(image, expected)
    }
    private fun failToken(image: String, expected: String): Nothing {
        val location = sourceLocation(pos)
        throw LocatedSyntaxError(
            "Error parsing usecase diagram: expected $expected but found: '$image' at line ${location.line}, column ${location.column} [$pos,${pos + image.length})",
            location,
        )
    }
    private fun label(close: Char): String {
        hws(); if (at("\"") || at("'")) return quoted()
        val start = pos
        while (pos < source.length && source[pos] != close) {
            val char = source[pos]
            if (char == '"' || char == '\'') {
                val end = source.indexOf(char, pos + 1)
                if (end < 0 || source.substring(pos + 1, end).any { it in "\r\n" }) {
                    val quoteStart = pos; pos++
                    failQuotedLabel(quoteStart, "Unclosed string inside an unquoted label")
                }
                failToken(source.substring(pos, end + 1), "label text")
            }
            val operator = labelOperator.matchAt(source, pos)?.value
            if (operator != null) failToken(operator, "label text")
            if (char in "()[]{}\r\n") failToken(if (source.startsWith("\r\n", pos)) "\r\n" else char.toString(), "label text")
            // Consume identifier text as one token: the final 'o' in 'foo--bar'
            // belongs to the word, not the start of a backward-circle operator.
            val word = idPattern.matchAt(source, pos)
            pos = if (word != null) word.range.last + 1 else pos + 1
        }
        return source.substring(start, pos).trim().takeIf { it.isNotEmpty() } ?: fail("Empty usecase label")
    }
    private fun metadata(): Map<String, String> {
        requireText("@{"); val result = linkedMapOf<String, String>()
        fun lines() { hws(); while (newline()) hws() }
        lines(); if (take("}")) return result
        while (true) {
            hws(); val keyStart = pos
            val key = if (at("\"")) quoted() else identifier(); val keyEnd = pos; requireText(":"); hws()
            val valueStart = pos; val quotedValue = at("\""); val value = if (quotedValue) quoted() else identifier(); val valueEnd = pos
            metadataProperties += MetadataProperty(key, value, !quotedValue && value in listOf("true", "false"), span(keyStart, keyEnd))
            metadataOccurrences += mapOf("key" to key, "span" to span(keyStart, valueEnd), "keySpan" to labelSpan(keyStart, keyEnd), "valueSpan" to labelSpan(valueStart, valueEnd))
            result[key] = value; hws()
            if (take("}")) return result
            val comma = take(","); val hadLine = newline(); if (!comma && !hadLine) fail("Expected metadata separator")
            lines(); if (hadLine) { take(","); lines() }; if (take("}")) return result
        }
    }
    private fun classList(): List<String> { val result = mutableListOf(identifier()); while (take(",")) result += identifier(); return result }
    private fun entity(actor: Boolean = false, boundary: Boolean = false): Entity {
        hws(); val start = pos; metadataOccurrences = mutableListOf(); metadataProperties = mutableListOf()
        val generated = at("\"")
        var labelType = if (at("\"`")) "markdown" else "text"
        val (id, initialLabel) = name(); val idEnd = pos
        var text = initialLabel; var shape = UsecaseShape.ELLIPSE; var explicit = generated
        var endpointDeclaration = false
        var textSpan = labelSpan(start, idEnd)
        if (take("(")) { hws(); val textStart = pos; labelType = if (at("\"`")) "markdown" else "text"; text = label(')'); textSpan = labelSpan(textStart, pos); requireText(")"); explicit = true; endpointDeclaration = true }
        else if (!actor && take("[")) { hws(); val textStart = pos; labelType = if (at("\"`")) "markdown" else "text"; text = label(']'); textSpan = labelSpan(textStart, pos); requireText("]"); shape = UsecaseShape.RECTANGLE; explicit = true; endpointDeclaration = true }
        val hasMetadata = at("@{")
        val properties = if (hasMetadata) metadata() else emptyMap()
        if (hasMetadata) metadataAssignments += id to metadataProperties.toList()
        var stereotype: String? = null; var stereotypeSpan: List<Int>? = null
        if (!actor && !boundary && !explicit && hasMetadata && edges.any { it.id == id } && at("<<")) failGrammarToken("end of edge metadata")
        if (!boundary && take("<<")) {
            val stereotypeStart = pos; while (pos < source.length && !source.startsWith(">>", pos)) { if (source[pos] in "\r\n") fail("Multiline stereotype"); pos++ }
            stereotype = source.substring(stereotypeStart, pos).trim().takeIf { it.isNotEmpty() } ?: fail("Empty stereotype")
            stereotypeSpan = span(stereotypeStart, pos); requireText(">>")
        }
        val assigned = if (take(":::")) classList() else emptyList()
        val occurrence = linkedMapOf<String, Any?>("id" to id, "span" to span(start), "idSpan" to labelSpan(start, idEnd), "labelSpan" to textSpan, "defines" to (actor || explicit))
        if (metadataOccurrences.isNotEmpty()) occurrence["metadata"] = metadataOccurrences.toList()
        if (stereotypeSpan != null) occurrence["stereotypeSpan"] = stereotypeSpan
        statementNodes += occurrence
        return Entity(id, text, shape, explicit, UsecaseAttributes(properties, stereotype, assigned, parentId = parent), labelType, occurrence, generated, initialLabel, endpointDeclaration || hasMetadata || stereotype != null)
    }
    private data class Declaration(val kind: String, val range: List<Int>, val entity: Entity? = null)
    private val declarations = linkedMapOf<String, Declaration>()
    private class DeclarationError(message: String, val location: SourceLocation) : IllegalArgumentException(message)
    private fun sourceLocation(offset: Int): SourceLocation {
        val lines = source.take(offset).split(Regex("\\r\\n|\\r|\\n"))
        return SourceLocation(lines.size, lines.last().length + 1)
    }
    private fun describe(declaration: Declaration, withLabel: Boolean): String {
        val (start, end) = declaration.range
        val location = sourceLocation(start)
        val suffix = if (withLabel && declaration.entity?.generated == true) " (label \"${declaration.entity.originLabel}\")" else ""
        return "line ${location.line}, column ${location.column} [$start,$end)$suffix"
    }
    private fun conflict(message: String, current: Declaration, previous: Declaration, withLabel: Boolean = false): Nothing {
        throw DeclarationError("$message at ${describe(current, withLabel)}; previous declaration at ${describe(previous, withLabel)}", sourceLocation(current.range[0]))
    }
    private fun declare(id: String, current: Declaration) {
        val previous = declarations[id]
        if (previous == null) { declarations[id] = current; return }
        val entity = current.entity
        val old = previous.entity
        if (entity == null) conflict("ID '$id' is declared more than once (${previous.kind} and ${current.kind})", current, previous, true)
        if (previous.kind != current.kind) conflict("ID '$id' is declared as both ${previous.kind} and ${current.kind}", current, previous, true)
        if (entity.generated || old?.generated == true) conflict("Generated ID '$id' collides with another declaration", current, previous, true)
        if (old == null) conflict("ID '$id' is declared more than once (${previous.kind} and ${current.kind})", current, previous, true)
        if (old.label != entity.label || old.labelType != entity.labelType) conflict("ID '$id' has conflicting labels", current, previous)
        if (current.kind == "usecase" && old.shape != entity.shape) conflict("Use case '$id' has conflicting shapes", current, previous)
        if (old.attributes.parentId != null && entity.attributes.parentId != null && old.attributes.parentId != entity.attributes.parentId) conflict("Element '$id' belongs to more than one system boundary", current, previous)
        if (old.attributes.stereotype != null && entity.attributes.stereotype != null && old.attributes.stereotype != entity.attributes.stereotype) conflict("Element '$id' has conflicting stereotypes", current, previous)
        // Retain the first origin, but remember optional semantics introduced by later declarations.
        declarations[id] = previous.copy(entity = old.copy(attributes = old.attributes.copy(
            parentId = old.attributes.parentId ?: entity.attributes.parentId,
            stereotype = old.attributes.stereotype ?: entity.attributes.stereotype,
        )))
    }
    private fun declareEntity(e: Entity, kind: String) {
        val range = (e.occurrence.getValue("idSpan") as List<*>).map { it as Int }
        declare(e.id, Declaration(kind, range, e))
    }
    private fun publish(e: Entity, actor: Boolean, declaration: Boolean = true) {
        if (declaration) declareEntity(e, if (actor) "actor" else "usecase")

        if (!actor && jsonNodes.any { it.id == e.id }) return
        if (actor) { nodes.remove(e.id); val previous = actors[e.id]; if (previous != null && previous.label != e.label) fail("Conflicting actor declaration ${e.id}"); actors[e.id] = UsecaseActor(e.id, e.label, labelType = e.labelType) }
        else if (e.id !in actors) { val previous = nodes[e.id]; if (previous == null || e.explicit) nodes[e.id] = UsecaseNode(e.id, e.label, e.shape, e.labelType) }
        if (e.attributes != UsecaseAttributes()) attrs[e.id] = e.attributes
    }
    private fun relation(from: Entity) {
        var edgeId: String? = null; hws(); val relationStart = pos; var idSpan: List<Int>? = null; var edgeLabelSpan: List<Int>? = null; val candidate = idPattern.find(source, pos)?.takeIf { it.range.first == pos }
        if (candidate != null && source.getOrNull(candidate.range.last + 1) == '@') { edgeId = candidate.value; idSpan = span(candidate.range.first, candidate.range.last + 1); pos = candidate.range.last + 2 }
        hws(); val match = Regex("--\\|>|\\.\\.>|<--+|o--|x--|--o|--x|--+>|--+").find(source, pos)?.takeIf { it.range.first == pos } ?: fail("Expected usecase relation")
        var operator = match.value; pos = match.range.last + 1; var edgeLabel: String? = null
        if (operator == "..>") { requireText(":"); hws(); val labelStart = pos; edgeLabel = identifier().lowercase(); edgeLabelSpan = span(labelStart); if (edgeLabel !in listOf("include", "extend")) fail("Expected include or extend") }
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
        val to = entity(); publish(to, false, to.endpointDeclaration)
        val startMarker = when { operator.startsWith('<') -> "arrow"; operator.startsWith('o') -> "circle"; operator.startsWith('x') -> "cross"; else -> "none" }
        val endMarker = when { operator == "--|>" -> "generalization"; operator.endsWith('>') -> "arrow"; operator.endsWith('o') -> "circle"; operator.endsWith('x') -> "cross"; else -> "none" }
        if (edgeId != null) declare(edgeId, Declaration("edge", idSpan!!))
        val resolvedId = edgeId ?: "edge-${anonymousEdgeCount++}"
        edges += UsecaseRelationship(from.id, to.id, edgeLabel, resolvedId, startMarker, endMarker, operator == "..>")
        val occurrence = linkedMapOf<String, Any?>("id" to resolvedId, "span" to span(relationStart))
        if (idSpan != null) occurrence["idSpan"] = idSpan
        if (edgeLabelSpan != null) occurrence["labelSpan"] = edgeLabelSpan
        statementEdges += occurrence
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
        sourceAst = null
        hws(); val headerStart = pos; if (!take("usecase-beta") && !take("usecaseDiagram")) fail("Expected usecase-beta"); headerSpan = span(headerStart); endLine()
        while (true) {
            val whitespaceStart = pos; hws(); if (pos >= source.length) break
            if (newline()) { appendStatement(linkedMapOf("kind" to "blank", "span" to span(whitespaceStart))); continue }
            val commentStart = pos
            if (take("%%")) { lineText(); appendStatement(linkedMapOf("kind" to "comment", "span" to span(commentStart))); endLine(); continue }
            statementNodes = mutableListOf(); statementEdges = mutableListOf(); metadataOccurrences = mutableListOf()
            val start = pos; val extra = linkedMapOf<String, Any?>(); var kind = "node"
            val keyword = idPattern.find(source, pos)?.takeIf { it.range.first == pos }?.value
            if (parent != null && keyword in setOf("systemBoundary", "note", "json", "direction", "classDef", "class", "style", "accTitle", "accDescr")) failGrammarToken("declaration inside a system boundary")
            when (keyword) {
                "end" -> { pos += 3; if (parent == null) fail("Unexpected boundary end"); parent = null; kind = "end" }
                "direction" -> { kind = "direction"; pos += 9; val value = identifier(); direction = when (value) { "LR" -> FlowDirection.LR; "RL" -> FlowDirection.RL; "BT" -> FlowDirection.BT; "TB", "TD" -> FlowDirection.TB; else -> fail("Invalid usecase direction") } }
                "accTitle" -> { kind = "accTitle"; pos += 8; requireText(":"); accTitle = lineText() }
                "accDescr" -> { kind = "accDescr"; pos += 8; if (take(":")) accDescr = lineText() else { requireText("{"); val end = source.indexOf('}', pos); if (end < 0) fail("Unclosed accessibility description"); accDescr = source.substring(pos, end).trim(); pos = end + 1 } }
                "systemBoundary" -> { kind = "group"; pos += 14; val e = entity(boundary = true); declareEntity(e, "boundary"); extra["group"] = e.id; extra["idSpan"] = e.occurrence["idSpan"]; extra["titleSpan"] = e.occurrence["labelSpan"]; boundaries += UsecaseBoundary(e.id, e.label, e.labelType); if (e.attributes != UsecaseAttributes()) attrs[e.id] = e.attributes; parent = e.id }
                "actor" -> { pos += 5; val e = entity(actor = true); publish(e, true); if (take(",")) { do { publish(entity(actor = true), true) } while (take(",")) } else { hws(); if (pos < source.length && source[pos] !in "\r\n") { if (parent != null) failGrammarToken("end of boundary declaration"); relation(e) } } }
                "note" -> { kind = "note"; pos += 4; if (identifier() != "for") fail("Expected note for"); hws(); val targetStart = pos; val target = identifier(); val targetEnd = pos; if (at(",")) fail("Notes require a single target"); hws(); val textStart = pos; val text = if (at("\"")) quoted() else lineText(); extra["ref"] = "note-${notes.size}"; extra["refSpan"] = labelSpan(textStart, pos); statementNodes += mapOf("id" to target, "span" to span(targetStart, targetEnd), "idSpan" to span(targetStart, targetEnd)); if (text.isBlank() || text.startsWith("as ")) fail("Invalid note label"); notes += UsecaseNote(target, text, "note-${notes.size}") }
                "json" -> { kind = "json"; pos += 4; hws(); val idStart = pos; val id = identifier(); declare(id, Declaration("json", span(idStart))); statementNodes += mapOf("id" to id, "span" to span(idStart), "idSpan" to span(idStart)); requireText("@"); hws(); jsonStarts += pos; val json = jsonObject(); val assigned = if (take(":::")) classList() else emptyList(); jsonNodes += UsecaseJsonNode(id, json); if (assigned.isNotEmpty()) attrs[id] = UsecaseAttributes(classes = assigned) }
                "classDef" -> { kind = "classDef"; pos += 8; val ids = classList(); val values = styleValues(); ids.forEach { classes[it] = values } }
                "class" -> { kind = "classAssign"; pos += 5; val ids = classList(); val assigned = classList(); ids.forEach { attrs[it] = (attrs[it] ?: UsecaseAttributes()).copy(classes = (attrs[it]?.classes.orEmpty() + assigned)) } }
                "style" -> { kind = "style"; pos += 5; val id = identifier(); val values = styleValues(); attrs[id] = (attrs[id] ?: UsecaseAttributes()).copy(styles = attrs[id]?.styles.orEmpty() + values) }
                else -> {
                    if (keyword?.lowercase() in forbidden) fail("Unsupported PlantUML statement")
                    val e = entity(); hws()
                    // A bare target followed by metadata is an assignment, not a declaration.
                    if (!e.explicit && e.attributes.properties.isNotEmpty() && e.attributes.stereotype == null && e.attributes.classes.isEmpty() && parent == null && (pos == source.length || source[pos] in "\r\n")) { kind = if (edges.any { it.id == e.id }) "edgeMetadata" else "metadata"; attrs[e.id] = (attrs[e.id] ?: UsecaseAttributes()).copy(properties = attrs[e.id]?.properties.orEmpty() + e.attributes.properties) }
                    else { val hasRelation = pos < source.length && source[pos] !in "\r\n"; publish(e, false, !hasRelation || e.endpointDeclaration); if (hasRelation) { if (parent != null) failGrammarToken("end of boundary declaration"); relation(e) } }
                }
            }
            if (pos == start) fail("Invalid usecase statement")
            if (kind == "end") {
                val group = groupStatement ?: fail("Unexpected boundary end")
                group["span"] = span((group.getValue("span") as List<*>)[0] as Int)
                group["endSpan"] = span(start); group["children"] = groupChildren.toList()
                groupStatement = null; groupChildren = mutableListOf()
            } else {
                if (statementEdges.isNotEmpty()) kind = "edge"
                val statement = linkedMapOf<String, Any?>("kind" to kind, "span" to span(start))
                statement.putAll(extra)
                if (statementNodes.isNotEmpty() && kind != "group") statement["nodes"] = statementNodes.toList()
                if (statementEdges.isNotEmpty()) statement["edges"] = statementEdges.toList()
                if (metadataOccurrences.isNotEmpty() && kind in setOf("group", "metadata", "edgeMetadata")) statement["metadata"] = metadataOccurrences.toList()
                appendStatement(statement)
                if (kind == "group") { groupStatement = statement; groupChildren = mutableListOf() }
            }
            endLine()
        }
        if (parent != null) fail("Unclosed system boundary")
        for (boundary in boundaries) {
            val attributes = attrs[boundary.id] ?: UsecaseAttributes()
            attrs[boundary.id] = attributes.copy(properties = mapOf("type" to "rect") + attributes.properties)
        }
        MermaidParseResult.Success(UsecaseDiagram(direction, actors.values.toList(), nodes.values.toList(), edges, boundaries, notes, jsonNodes, attrs, classes, accTitle, accDescr))
    } catch (e: IllegalArgumentException) {
        val prefix = source.take(pos); val lines = prefix.split(Regex("\\r\\n|\\r|\\n"))
        MermaidParseResult.Failure(listOf(MermaidDiagnostic(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, e.message ?: "Invalid usecase syntax", (e as? LocatedSyntaxError)?.location ?: (e as? DeclarationError)?.location ?: SourceLocation(lines.size, lines.last().length + 1))))
    }

    private fun semanticFailure(message: String, range: List<Int>): MermaidParseResult.Failure {
        val location = sourceLocation(range[0])
        return MermaidParseResult.Failure(listOf(MermaidDiagnostic(
            MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
            "$message at line ${location.line}, column ${location.column} [${range[0]},${range[1]})",
            location,
        )))
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
        val metadataKinds = d.actors.associate { it.id to "actor" } +
            d.useCases.associate { it.id to "usecase" } + d.boundaries.associate { it.id to "boundary" }
        for ((id, properties) in metadataAssignments) {
            val kind = metadataKinds[id] ?: continue
            for (property in properties) {
                val valid = when (kind) {
                    "actor" -> when (property.key) {
                        "type" -> !property.boolean && property.value in listOf("normal", "hollow", "awesome")
                        "icon" -> !property.boolean
                        "business" -> property.boolean
                        else -> false
                    }
                    "usecase" -> property.key == "business" && property.boolean
                    else -> property.key == "type" && property.value in listOf("rect", "package")
                }
                if (!valid) return semanticFailure("Metadata property '${property.key}' is invalid for $kind '$id'", property.range)
            }
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
                icon != null && declared != "normal" -> "Actor '${actor.id}' cannot combine icon with type '$declared'"
                business && (icon != null || declared == "awesome") -> "Business actor '${actor.id}' must use normal or hollow geometry"
                else -> null
            }
            if (error != null) return semanticFailure(error, declarations.getValue(actor.id).range)
            val type = if (icon != null) UsecaseActorType.ICON else when (declared) { "hollow" -> UsecaseActorType.HOLLOW; "awesome" -> UsecaseActorType.AWESOME; else -> UsecaseActorType.NORMAL }
            validatedActors += actor.copy(type = type, icon = icon, business = business)
        }
        for (node in d.useCases) {
            if (node.shape == UsecaseShape.RECTANGLE && d.attributes[node.id]?.properties?.get("business") == "true") {
                return semanticFailure("Rectangular use case '${node.id}' cannot be a business use case", declarations.getValue(node.id).range)
            }
        }
        val complete = d.copy(actors = validatedActors, jsonNodes = validatedJson)
        sourceAst = buildAst(complete)
        return MermaidParseResult.Success(complete)
    }
    private fun buildAst(d: UsecaseDiagram): UsecaseSourceAst {
        val graphNodes = linkedMapOf<String, Any?>()
        fun node(id: String, label: String, shape: String, kind: String, properties: Map<String, Any?>) {
            val attributes = d.attributes[id]
            val attrs = linkedMapOf<String, Any?>("kind" to kind)
            attrs.putAll(properties)
            attributes?.stereotype?.let { attrs["stereotype"] = it }
            attributes?.parentId?.let { attrs["parentId"] = it }
            val row = linkedMapOf<String, Any?>("label" to label, "shape" to shape, "attrs" to attrs)
            if (!attributes?.classes.isNullOrEmpty()) row["classes"] = attributes!!.classes
            if (!attributes?.styles.isNullOrEmpty()) row["styles"] = attributes!!.styles.map { "${it.key}:${it.value}" }
            graphNodes[id] = row
        }
        for (a in d.actors) node(a.id, a.label, if (a.type == UsecaseActorType.NORMAL) "actor" else "actor-${a.type.name.lowercase()}", "actor", mapOf("actorType" to a.type.name.lowercase(), "business" to a.business, "labelType" to a.labelType))
        for (n in d.useCases) {
            val shape = if (n.shape == UsecaseShape.RECTANGLE) "rect" else "ellipse"
            node(n.id, n.label, shape, "usecase", mapOf("useCaseShape" to shape, "business" to (d.attributes[n.id]?.properties?.get("business") == "true"), "labelType" to n.labelType))
        }
        for (n in d.jsonNodes) node(n.id, n.id, "json-table", "json", emptyMap())
        for (n in d.notes) node(n.id.orEmpty(), n.label, "note", "note", mapOf("target" to n.targetId, "labelType" to "text"))
        val groups = d.boundaries.associate { b -> b.id to mapOf("title" to b.label, "nodes" to d.attributes.filterValues { it.parentId == b.id }.keys.toList(), "attrs" to mapOf("kind" to "systemBoundary", "boundaryType" to d.attributes[b.id]?.properties?.get("type"), "labelType" to b.labelType)) }
        val graphEdges = d.relationships.map { e ->
            linkedMapOf<String, Any?>("id" to e.id, "source" to e.sourceId, "target" to e.targetId).also { row ->
                e.label?.let { row["label"] = it }
                row["attrs"] = mapOf("relationshipType" to e.type.name.lowercase())
            }
        }
        val data = linkedMapOf<String, Any?>("version" to 1, "diagramType" to "usecase", "source" to source,
            "header" to mapOf("keyword" to "usecase", "direction" to d.direction.name, "span" to headerSpan),
            "nodes" to graphNodes, "edges" to graphEdges, "groups" to groups,
            "classDefs" to d.classDefs.mapValues { (_, values) -> mapOf("styles" to values.map { "${it.key}:${it.value}" }) },
            "statements" to statements.toList())
        accTitle?.let { data["accTitle"] = it }; accDescr?.let { data["accDescr"] = it }
        return UsecaseSourceAst(data)
    }

}
