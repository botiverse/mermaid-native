package build.raft.mermaid.core

/**
 * Parser for the explicitly declared Mermaid-compatible subset.
 *
 * Unsupported input fails with typed diagnostics. It is never reinterpreted as
 * another diagram family and never returns a partially parsed diagram.
 */
public object MermaidParser {
    public fun parse(source: String): MermaidParseResult {
        val statements = source.toStatements()
        val header = statements.firstOrNull()
            ?: return failure(
                MermaidDiagnosticCode.EMPTY_SOURCE,
                "The Mermaid source is empty",
                SourceLocation(line = 1, column = 1),
            )

        return when {
            header.text.equals("sequenceDiagram", ignoreCase = true) -> parseSequence(statements)
            STATE_HEADER.matches(header.text) -> parseState(statements)
            header.text.startsWith("pie", ignoreCase = true) -> parsePie(statements)
            header.text.equals("classDiagram", ignoreCase = true) -> parseClass(statements)
            header.text.equals("erDiagram", ignoreCase = true) -> parseEntityRelationship(statements)
            FLOW_HEADER.matches(header.text) -> parseFlowchart(statements)
            header.text.startsWith("flowchart", ignoreCase = true) ||
                header.text.startsWith("graph", ignoreCase = true) -> failure(
                MermaidDiagnosticCode.INVALID_HEADER,
                "Expected graph/flowchart followed by TD, TB, LR, BT, or RL",
                header.location,
            )
            else -> failure(
                MermaidDiagnosticCode.UNSUPPORTED_DIAGRAM,
                "Unsupported Mermaid diagram header: ${header.text}",
                header.location,
            )
        }
    }

    private fun parseFlowchart(statements: List<SourceStatement>): MermaidParseResult {
        val header = FLOW_HEADER.matchEntire(statements.first().text)
            ?: return failure(
                MermaidDiagnosticCode.INVALID_HEADER,
                "Invalid flowchart header",
                statements.first().location,
            )
        val direction = FlowDirection.valueOf(header.groupValues[1].uppercase())
        val nodes = linkedMapOf<String, FlowNode>()
        val edges = mutableListOf<FlowEdge>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()

        fun register(id: String, label: String?) {
            val existing = nodes[id]
            val resolvedLabel = label?.takeIf { it.isNotEmpty() } ?: existing?.label ?: id
            nodes[id] = FlowNode(id = id, label = resolvedLabel)
        }

        statements.drop(1).forEach { statement ->
            val edge = FLOW_EDGE.matchEntire(statement.text)
            if (edge != null) {
                val sourceId = edge.groupValues[1]
                val sourceLabel = edge.groupValues[2].ifEmpty { null }
                val targetId = edge.groupValues[3]
                val targetLabel = edge.groupValues[4].ifEmpty { null }
                register(sourceId, sourceLabel)
                register(targetId, targetLabel)
                edges += FlowEdge(sourceId = sourceId, targetId = targetId)
                return@forEach
            }

            val node = FLOW_NODE.matchEntire(statement.text)
            if (node != null) {
                register(node.groupValues[1], node.groupValues[2].ifEmpty { null })
                return@forEach
            }

            diagnostics += unsupported(statement, "Unsupported flowchart syntax")
        }

        return if (diagnostics.isEmpty()) {
            MermaidParseResult.Success(
                FlowchartDiagram(
                    direction = direction,
                    nodes = nodes.values.toList(),
                    edges = edges.toList(),
                ),
            )
        } else {
            MermaidParseResult.Failure(diagnostics)
        }
    }

    private fun parseSequence(statements: List<SourceStatement>): MermaidParseResult {
        val actors = linkedMapOf<String, SequenceActor>()
        val messages = mutableListOf<SequenceMessage>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()

        fun register(id: String) {
            if (id !in actors) {
                actors[id] = SequenceActor(id = id, label = id)
            }
        }

        statements.drop(1).forEach { statement ->
            val message = SEQUENCE_MESSAGE.matchEntire(statement.text)
            if (message == null) {
                diagnostics += unsupported(statement, "Unsupported sequence syntax")
                return@forEach
            }

            val from = message.groupValues[1]
            val arrow = message.groupValues[2]
            val to = message.groupValues[3]
            val label = message.groupValues[4]
            register(from)
            register(to)
            messages += SequenceMessage(
                from = from,
                to = to,
                label = label,
                lineStyle = if (arrow.startsWith("--")) {
                    SequenceLineStyle.DASHED
                } else {
                    SequenceLineStyle.SOLID
                },
                arrowHead = SequenceArrowHead.FILLED,
            )
        }

        return if (diagnostics.isEmpty()) {
            MermaidParseResult.Success(
                SequenceDiagram(
                    actors = actors.values.toList(),
                    messages = messages.toList(),
                ),
            )
        } else {
            MermaidParseResult.Failure(diagnostics)
        }
    }

    private fun parseState(statements: List<SourceStatement>): MermaidParseResult {
        val states = linkedMapOf<String, StateNode>()
        val transitions = mutableListOf<StateTransition>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        var direction = FlowDirection.TB
        var pseudoStateIndex = 0

        fun register(id: String, label: String? = null, kind: StateNodeKind = StateNodeKind.STATE) {
            val resolved = when {
                kind != StateNodeKind.STATE -> label.orEmpty()
                !label.isNullOrEmpty() -> label
                else -> states[id]?.label ?: id
            }
            states[id] = StateNode(id = id, label = resolved, kind = kind)
        }

        fun endpoint(raw: String, isSource: Boolean): String {
            if (raw != "[*]") {
                register(raw)
                return raw
            }
            val kind = if (isSource) StateNodeKind.START else StateNodeKind.END
            val id = "__${kind.name.lowercase()}_${pseudoStateIndex++}"
            register(id = id, label = "", kind = kind)
            return id
        }

        statements.drop(1).forEach { statement ->
            val directionMatch = STATE_DIRECTION.matchEntire(statement.text)
            if (directionMatch != null) {
                direction = FlowDirection.valueOf(directionMatch.groupValues[1].uppercase())
                return@forEach
            }
            val alias = STATE_ALIAS.matchEntire(statement.text)
            if (alias != null) {
                register(alias.groupValues[2], alias.groupValues[1])
                return@forEach
            }
            val transition = STATE_TRANSITION.matchEntire(statement.text)
            if (transition != null) {
                val from = endpoint(transition.groupValues[1], isSource = true)
                val to = endpoint(transition.groupValues[2], isSource = false)
                transitions += StateTransition(from = from, to = to, label = transition.groupValues[3])
                return@forEach
            }
            diagnostics += unsupported(statement, "Unsupported state diagram syntax")
        }

        return if (diagnostics.isEmpty()) {
            MermaidParseResult.Success(
                StateDiagram(direction = direction, states = states.values.toList(), transitions = transitions.toList()),
            )
        } else {
            MermaidParseResult.Failure(diagnostics)
        }
    }

    private fun parsePie(statements: List<SourceStatement>): MermaidParseResult {
        val header = statements.first()
        var remainingHeader = header.text.removePrefix("pie").trim()
        var showData = false
        var title: String? = null
        var accTitle: String? = null
        var accDescription: String? = null
        val sections = linkedMapOf<String, PieSection>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        fun consumeMetadata(text: String): Boolean {
            val trimmed = text.trim()
            when {
                trimmed.equals("showData", ignoreCase = true) -> showData = true
                trimmed.startsWith("title ", ignoreCase = true) -> title = trimmed.drop(6).trim().ifEmpty { null }
                trimmed.startsWith("accTitle:", ignoreCase = true) -> accTitle = trimmed.substringAfter(':').trim().ifEmpty { null }
                trimmed.startsWith("accDescr:", ignoreCase = true) -> accDescription = trimmed.substringAfter(':').trim().ifEmpty { null }
                else -> return false
            }
            return true
        }
        if (remainingHeader.startsWith("showData", ignoreCase = true)) {
            showData = true
            remainingHeader = remainingHeader.drop("showData".length).trim()
        }
        if (remainingHeader.isNotEmpty() && !consumeMetadata(remainingHeader)) diagnostics += unsupported(header, "Unsupported pie header syntax")
        statements.drop(1).forEach { statement ->
            if (consumeMetadata(statement.text)) return@forEach
            val section = PIE_SECTION.matchEntire(statement.text)
            if (section == null) {
                diagnostics += unsupported(statement, "Unsupported pie syntax")
                return@forEach
            }
            val value = section.groupValues[2].toDouble()
            if (value < 0.0) {
                diagnostics += MermaidDiagnostic(MermaidDiagnosticCode.INVALID_VALUE, "Pie slice values must be non-negative", statement.location)
            } else {
                val label = section.groupValues[1].substring(1, section.groupValues[1].length - 1)
                if (label !in sections) sections[label] = PieSection(label, value)
            }
        }
        return if (diagnostics.isEmpty()) MermaidParseResult.Success(
            PieDiagram(title, showData, sections.values.toList(), accTitle, accDescription),
        ) else MermaidParseResult.Failure(diagnostics)
    }

    private fun parseClass(statements: List<SourceStatement>): MermaidParseResult {
        val classes = linkedMapOf<String, ClassDefinition>()
        val relationships = mutableListOf<ClassRelationship>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()

        fun ensure(id: String) { if (id !in classes) classes[id] = ClassDefinition(id) }
        statements.drop(1).forEach { statement ->
            CLASS_DECLARATION.matchEntire(statement.text)?.let {
                val id = it.groupValues[1]
                val label = it.groupValues[2].ifEmpty { id }
                classes[id] = classes[id]?.copy(label = label) ?: ClassDefinition(id, label)
                return@forEach
            }
            CLASS_MEMBER.matchEntire(statement.text)?.let {
                val id = it.groupValues[1]
                val marker = it.groupValues[2]
                val signature = it.groupValues[3].trim()
                if (marker.isEmpty() && signature in CLASS_VISIBILITY_MARKERS) {
                    diagnostics += unsupported(statement, "Class member visibility requires a signature")
                    return@forEach
                }
                ensure(id)
                val visibility = when (marker) {
                    "-" -> ClassVisibility.PRIVATE
                    "#" -> ClassVisibility.PROTECTED
                    "~" -> ClassVisibility.PACKAGE
                    else -> ClassVisibility.PUBLIC
                }
                val member = ClassMember(signature, visibility)
                classes[id] = classes.getValue(id).copy(members = classes.getValue(id).members + member)
                return@forEach
            }
            CLASS_RELATION.matchEntire(statement.text)?.let {
                val kind = when (it.groupValues[2]) {
                    "<|--" -> ClassRelationshipKind.INHERITANCE
                    else -> ClassRelationshipKind.ASSOCIATION
                }
                ensure(it.groupValues[1]); ensure(it.groupValues[3])
                relationships += ClassRelationship(it.groupValues[1], it.groupValues[3], kind)
                return@forEach
            }
            diagnostics += unsupported(statement, "Unsupported classDiagram syntax")
        }
        return if (diagnostics.isEmpty()) {
            MermaidParseResult.Success(ClassDiagram(classes.values.toList(), relationships.toList()))
        } else MermaidParseResult.Failure(diagnostics)
    }

    private fun parseEntityRelationship(statements: List<SourceStatement>): MermaidParseResult {
        val entities = linkedMapOf<String, EntityDefinition>()
        val relationships = mutableListOf<EntityRelationship>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        var currentEntity: String? = null

        fun ensure(id: String) {
            if (id !in entities) entities[id] = EntityDefinition(id)
        }

        fun cardinality(token: String): EntityCardinality = when (token) {
            "||" -> EntityCardinality.ONLY_ONE
            "o|", "|o" -> EntityCardinality.ZERO_OR_ONE
            "|{", "}|" -> EntityCardinality.ONE_OR_MORE
            "o{", "}o" -> EntityCardinality.ZERO_OR_MORE
            else -> error("Cardinality token must be admitted by the relationship regex")
        }

        statements.drop(1).forEach { statement ->
            val owner = currentEntity
            if (owner != null) {
                if (statement.text == "}") {
                    currentEntity = null
                    return@forEach
                }
                val attribute = ER_ATTRIBUTE.matchEntire(statement.text)
                if (attribute == null) {
                    diagnostics += unsupported(statement, "Unsupported entity attribute syntax")
                    return@forEach
                }
                val key = attribute.groupValues[3].takeIf { it.isNotEmpty() }
                    ?.let(EntityKey::valueOf) ?: EntityKey.NONE
                val entity = entities.getValue(owner)
                entities[owner] = entity.copy(
                    attributes = entity.attributes + EntityAttribute(
                        type = attribute.groupValues[1],
                        name = attribute.groupValues[2],
                        key = key,
                    ),
                )
                return@forEach
            }

            ER_ENTITY_START.matchEntire(statement.text)?.let {
                val id = it.groupValues[1]
                ensure(id)
                currentEntity = id
                return@forEach
            }
            ER_RELATIONSHIP.matchEntire(statement.text)?.let {
                val from = it.groupValues[1]
                val to = it.groupValues[4]
                ensure(from)
                ensure(to)
                relationships += EntityRelationship(
                    from = from,
                    to = to,
                    fromCardinality = cardinality(it.groupValues[2]),
                    toCardinality = cardinality(it.groupValues[3]),
                    label = it.groupValues[5],
                )
                return@forEach
            }
            diagnostics += unsupported(statement, "Unsupported entity relationship syntax")
        }

        if (currentEntity != null) {
            diagnostics += MermaidDiagnostic(
                MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
                "Unclosed entity declaration: $currentEntity",
                statements.last().location,
            )
        }
        return if (diagnostics.isEmpty()) {
            MermaidParseResult.Success(EntityRelationshipDiagram(entities.values.toList(), relationships.toList()))
        } else MermaidParseResult.Failure(diagnostics)
    }

    private fun unsupported(statement: SourceStatement, message: String): MermaidDiagnostic =
        MermaidDiagnostic(
            code = MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
            message = "$message: ${statement.text}",
            location = statement.location,
        )

    private fun failure(
        code: MermaidDiagnosticCode,
        message: String,
        location: SourceLocation,
    ): MermaidParseResult.Failure = MermaidParseResult.Failure(
        listOf(MermaidDiagnostic(code = code, message = message, location = location)),
    )

    private val IDENTIFIER = "[A-Za-z_][A-Za-z0-9_-]*"
    private val FLOW_HEADER = Regex(
        pattern = "^(?:graph|flowchart)\\s+(TD|TB|LR|BT|RL)$",
        option = RegexOption.IGNORE_CASE,
    )
    private val STATE_HEADER = Regex("^stateDiagram(?:-v2)?$", RegexOption.IGNORE_CASE)
    private val STATE_DIRECTION = Regex("^direction\\s+(TB|TD|LR|BT|RL)$", RegexOption.IGNORE_CASE)
    private val STATE_ALIAS = Regex("^state\\s+\"([^\"]+)\"\\s+as\\s+($IDENTIFIER)$")
    private val STATE_TRANSITION = Regex(
        "^(\\[\\*\\]|$IDENTIFIER)\\s*-->\\s*(\\[\\*\\]|$IDENTIFIER)(?:\\s*:\\s*(.*))?$",
    )
    private val FLOW_NODE = Regex("^($IDENTIFIER)(?:\\[([^]\\r\\n]+)])?$")
    private val FLOW_EDGE = Regex(
        "^($IDENTIFIER)(?:\\[([^]\\r\\n]+)])?\\s*-->\\s*" +
            "($IDENTIFIER)(?:\\[([^]\\r\\n]+)])?$",
    )
    private val SEQUENCE_MESSAGE = Regex(
        // The lazy IDs are intentional: an ID may contain '-' while '-->>'
        // starts with the same character. The arrow must win at the boundary.
        "^($IDENTIFIER?)\\s*(->>|-->>)\\s*($IDENTIFIER?)(?:\\s*:\\s*(.*))?$",
    )
    private val PIE_SECTION = Regex("^([\\\"'](?:[^\\\"']|\\\\.)*[\\\"'])\\s*:\\s*(-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?)$")
    private val CLASS_DECLARATION = Regex("^class\\s+($IDENTIFIER)(?:\\s+as\\s+(.+))?$", RegexOption.IGNORE_CASE)
    private val CLASS_MEMBER = Regex("^($IDENTIFIER)\\s*:\\s*([+\\-#~]?)(.+)$")
    private val CLASS_RELATION = Regex("^($IDENTIFIER)\\s+(<\\|--|-->)\\s+($IDENTIFIER)(?:\\s*:\\s*.*)?$")
    private val CLASS_VISIBILITY_MARKERS = setOf("+", "-", "#", "~")
    private val ER_ENTITY_START = Regex("^($IDENTIFIER)\\s*\\{$")
    private val ER_ATTRIBUTE = Regex("^([A-Za-z_][A-Za-z0-9_<>\\[\\]-]*)\\s+($IDENTIFIER)(?:\\s+(PK|FK|UK))?$")
    private val ER_RELATIONSHIP = Regex(
        "^($IDENTIFIER)\\s+(\\|\\||o\\||\\|o|\\|\\{|o\\{|}\\||}o)--" +
            "(\\|\\||o\\||\\|o|\\|\\{|o\\{|}\\||}o)\\s+($IDENTIFIER)(?:\\s*:\\s*(.*))?$",
    )
}

private data class SourceStatement(
    val text: String,
    val location: SourceLocation,
)

private fun String.toStatements(): List<SourceStatement> = buildList {
    lineSequence().forEachIndexed { lineIndex, physicalLine ->
        var segmentStart = 0
        physicalLine.split(';').forEach { segment ->
            val leadingWhitespace = segment.indexOfFirst { !it.isWhitespace() }
            if (leadingWhitespace >= 0) {
                val text = segment.substring(leadingWhitespace).trimEnd()
                if (text.isNotEmpty() && !text.startsWith("%%")) {
                    add(
                        SourceStatement(
                            text = text,
                            location = SourceLocation(
                                line = lineIndex + 1,
                                column = segmentStart + leadingWhitespace + 1,
                            ),
                        ),
                    )
                }
            }
            segmentStart += segment.length + 1
        }
    }
}
