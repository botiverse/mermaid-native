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
            XY_HEADER.matches(header.text) -> parseXyChart(statements)
            header.text.equals("mindmap", ignoreCase = true) -> parseMindmap(source)
            header.text.equals("gantt", ignoreCase = true) -> parseGantt(statements)
            header.text.equals("timeline", ignoreCase = true) -> parseTimeline(statements)
            header.text.equals("quadrantChart", ignoreCase = true) -> parseQuadrantChart(statements)
            header.text.equals("journey", ignoreCase = true) -> parseUserJourney(statements)
            header.text.equals("gitGraph", ignoreCase = true) -> parseGitGraph(statements)
            header.text.equals("requirementDiagram", ignoreCase = true) -> parseRequirement(statements)
            header.text.equals("kanban", ignoreCase = true) -> parseKanban(source)
            header.text.equals("packet", ignoreCase = true) || header.text.equals("packet-beta", ignoreCase = true) -> parsePacket(statements)
            header.text.equals("block", ignoreCase = true) || header.text.equals("block-beta", ignoreCase = true) -> parseBlock(statements)
            header.text.equals("sankey", ignoreCase = true) || header.text.equals("sankey-beta", ignoreCase = true) -> parseSankey(source)
            header.text.equals("treemap-beta", ignoreCase = true) -> parseTreemap(source)
            header.text.equals("venn-beta", ignoreCase = true) -> parseVenn(source)
            header.text.equals("usecase-beta", ignoreCase = true) || header.text.equals("usecaseDiagram", ignoreCase = true) -> parseUsecase(source)
            header.text.equals("architecture-beta", ignoreCase = true) -> parseArchitecture(source)
            header.text.equals("C4Context", ignoreCase = true) -> parseC4Context(source)
            header.text.equals("cynefin-beta", ignoreCase = true) || header.text.equals("cynefin", ignoreCase = true) -> parseCynefin(source)
            header.text.equals("ishikawa", ignoreCase = true) || header.text.equals("ishikawa-beta", ignoreCase = true) || header.text.equals("fishbone", ignoreCase = true) -> parseIshikawa(source)
            SWIMLANE_HEADER.matches(header.text) -> parseSwimlane(source)
            header.text.equals("treeView-beta", ignoreCase = true) -> parseTreeView(source)
            header.text.equals("railroad-beta", ignoreCase = true) -> parseRailroad(source)
            header.text.equals("zenuml", ignoreCase = true) -> parseZenuml(statements)
            header.text.equals("wardley-beta", ignoreCase = true) -> parseWardley(statements)
            header.text.equals("radar-beta", ignoreCase = true) -> parseRadar(statements)
            header.text == "eventmodeling" -> parseEventModeling(source)
            header.text.startsWith("swimlane-beta", ignoreCase = true) -> failure(
                MermaidDiagnosticCode.INVALID_HEADER,
                "Expected swimlane-beta optionally followed by TD, TB, LR, BT, or RL",
                header.location,
            )
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
                val operator = edge.groupValues[3]
                val targetId = edge.groupValues[4]
                val targetLabel = edge.groupValues[5].ifEmpty { null }
                register(sourceId, sourceLabel)
                register(targetId, targetLabel)
                edges += FlowEdge(
                    sourceId = sourceId,
                    targetId = targetId,
                    style = if (operator == "==>") FlowEdgeStyle.THICK else FlowEdgeStyle.NORMAL,
                )
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

    /**
     * Bounded zenuml slice. Supported statements: an optional single `title`,
     * participant declarations (bare identifier or `id as Label`), sync
     * messages `A->B.method` / `A->B.method()` with empty parentheses, and
     * async messages `A->B: label`. Everything else fails closed with a
     * typed diagnostic.
     */
    private fun parseZenuml(statements: List<SourceStatement>): MermaidParseResult {
        val participants = linkedMapOf<String, ZenumlParticipant>()
        val messages = mutableListOf<ZenumlMessage>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        var title: String? = null

        fun register(id: String, declaredLabel: String?, location: SourceLocation) {
            val existing = participants[id]
            when {
                existing == null -> participants[id] = ZenumlParticipant(id = id, label = declaredLabel ?: id)
                declaredLabel != null && declaredLabel != existing.label -> diagnostics += MermaidDiagnostic(
                    code = MermaidDiagnosticCode.INVALID_VALUE,
                    message = "zenuml participant '$id' is redeclared with a different alias",
                    location = location,
                )
            }
        }

        loop@ for (statement in statements.drop(1)) {
            val text = statement.text

            fun hasZenumlBoundaryDash(vararg ids: String): Boolean =
                ids.any { it.startsWith('-') || it.endsWith('-') }

            val titleMatch = ZENUML_TITLE.matchEntire(text)
            if (titleMatch != null) {
                if (title != null) {
                    diagnostics += MermaidDiagnostic(
                        code = MermaidDiagnosticCode.INVALID_VALUE,
                        message = "zenuml accepts at most one title",
                        location = statement.location,
                    )
                } else {
                    title = titleMatch.groupValues[1]
                }
                continue@loop
            }
            val alias = ZENUML_ALIAS_DECLARATION.matchEntire(text)
            if (alias != null) {
                register(alias.groupValues[1], alias.groupValues[2].trim(), statement.location)
                continue@loop
            }
            val bareDeclaration = ZENUML_BARE_DECLARATION.matchEntire(text)
            if (bareDeclaration != null) {
                register(bareDeclaration.groupValues[1], null, statement.location)
                continue@loop
            }
            val sync = ZENUML_SYNC_MESSAGE.matchEntire(text)
            if (sync != null) {
                val from = sync.groupValues[1]
                val to = sync.groupValues[2]
                if (hasZenumlBoundaryDash(from, to)) {
                    diagnostics += unsupported(statement, "Unsupported zenuml syntax")
                    continue@loop
                }
                register(from, null, statement.location)
                register(to, null, statement.location)
                messages += ZenumlSyncMessage(from = from, to = to, method = sync.groupValues[3])
                continue@loop
            }
            val async = ZENUML_ASYNC_MESSAGE.matchEntire(text)
            if (async != null) {
                val from = async.groupValues[1]
                val to = async.groupValues[2]
                if (hasZenumlBoundaryDash(from, to)) {
                    diagnostics += unsupported(statement, "Unsupported zenuml syntax")
                    continue@loop
                }
                register(from, null, statement.location)
                register(to, null, statement.location)
                messages += ZenumlAsyncMessage(from = from, to = to, label = async.groupValues[3].trim())
                continue@loop
            }
            diagnostics += unsupported(statement, "Unsupported zenuml syntax")
        }

        if (diagnostics.isEmpty() && messages.isEmpty()) {
            diagnostics += MermaidDiagnostic(
                code = MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
                message = "zenuml diagram requires at least one message",
                location = statements.first().location,
            )
        }
        return if (diagnostics.isEmpty()) {
            MermaidParseResult.Success(
                ZenumlDiagram(
                    title = title,
                    participants = participants.values.toList(),
                    messages = messages.toList(),
                ),
            )
        } else {
            MermaidParseResult.Failure(diagnostics)
        }
    }

    /**
     * Bounded wardley-beta slice. Supported statements: an optional single
     * `title`, `anchor Name [v, e]`, `component Name [v, e]` (unquoted
     * names), basic `A -> B` links between declared nodes, `evolve Name e`
     * (one per component), and `note "text" [v, e]`. Coordinates are OWM
     * ordered: first visibility, then evolution, both within [0, 1].
     * Everything else fails closed with a typed diagnostic.
     */
    private fun parseWardley(statements: List<SourceStatement>): MermaidParseResult {
        val nodes = linkedMapOf<String, WardleyNode>()
        val links = mutableListOf<WardleyLink>()
        val evolutions = mutableListOf<WardleyEvolution>()
        val notes = mutableListOf<WardleyNote>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        var title: String? = null

        fun fail(code: MermaidDiagnosticCode, message: String, location: SourceLocation) {
            diagnostics += MermaidDiagnostic(code = code, message = message, location = location)
        }

        fun parseCoordinate(raw: String): Double? {
            // Strict decimal literals only: rejects NaN, Infinity, exponents,
            // signs, and values outside [0, 1] so coordinates stay unambiguous.
            val trimmed = raw.trim()
            if (WARDLEY_COORDINATE.matchEntire(trimmed) == null) return null
            return trimmed.toDoubleOrNull()?.takeIf { it <= 1.0 }
        }

        fun parseCoordinatePair(raw: String, location: SourceLocation): Pair<Double, Double>? {
            val inner = raw.trim()
            val parts = inner.split(',')
            if (parts.size != 2) {
                fail(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, "wardley coordinates must be [visibility, evolution]", location)
                return null
            }
            val visibility = parseCoordinate(parts[0])
            val evolution = parseCoordinate(parts[1])
            if (visibility == null || evolution == null) {
                fail(MermaidDiagnosticCode.INVALID_VALUE, "wardley coordinates must be decimal numbers in [0, 1]", location)
                return null
            }
            return visibility to evolution
        }

        fun validWardleyName(name: String): Boolean {
            val trimmed = name.trim()
            if (trimmed.isEmpty() || trimmed != name) return false
            if (trimmed.startsWith('-') || trimmed.endsWith('-')) return false
            if (!trimmed.any { it.isLetterOrDigit() || it == '_' }) return false
            return trimmed.all { it.isLetterOrDigit() || it == '_' || it == '-' || it == ' ' }
        }

        loop@ for (statement in statements.drop(1)) {
            val text = statement.text
            val location = statement.location
            val titleMatch = WARDLEY_TITLE.matchEntire(text)
            if (titleMatch != null) {
                if (title != null) {
                    fail(MermaidDiagnosticCode.INVALID_VALUE, "wardley accepts at most one title", location)
                } else {
                    title = titleMatch.groupValues[1]
                }
                continue@loop
            }
            val isAnchor = text.startsWith(WARDLEY_ANCHOR_KEYWORD)
            val isComponent = !isAnchor && text.startsWith(WARDLEY_COMPONENT_KEYWORD)
            if (isAnchor || isComponent) {
                val keyword = if (isAnchor) WARDLEY_ANCHOR_KEYWORD else WARDLEY_COMPONENT_KEYWORD
                val remainder = text.removePrefix(keyword)
                val openIndex = remainder.indexOf('[')
                val closeIndex = remainder.lastIndexOf(']')
                if (openIndex <= 0 || closeIndex != remainder.length - 1 || closeIndex <= openIndex + 1) {
                    fail(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, "Unsupported wardley $keyword declaration", location)
                    continue@loop
                }
                val name = remainder.substring(0, openIndex).trim()
                if (!validWardleyName(name)) {
                    fail(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, "Invalid wardley node name: $name", location)
                    continue@loop
                }
                if (name in nodes) {
                    fail(MermaidDiagnosticCode.INVALID_VALUE, "Duplicate wardley node name: $name", location)
                    continue@loop
                }
                val coordinates = parseCoordinatePair(remainder.substring(openIndex + 1, closeIndex), location) ?: continue@loop
                nodes[name] = WardleyNode(
                    name = name,
                    visibility = coordinates.first,
                    evolution = coordinates.second,
                    anchor = isAnchor,
                )
                continue@loop
            }
            val evolveRemainder = text.removePrefix(WARDLEY_EVOLVE_KEYWORD)
            if (evolveRemainder != text) {
                val lastSpace = evolveRemainder.lastIndexOf(' ')
                if (lastSpace <= 0) {
                    fail(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, "Unsupported wardley evolve statement", location)
                    continue@loop
                }
                val name = evolveRemainder.substring(0, lastSpace).trim()
                val target = parseCoordinate(evolveRemainder.substring(lastSpace + 1))
                when {
                    name !in nodes -> fail(MermaidDiagnosticCode.INVALID_VALUE, "wardley evolve references unknown component: $name", location)
                    target == null -> fail(MermaidDiagnosticCode.INVALID_VALUE, "wardley evolve target must be a decimal number in [0, 1]", location)
                    evolutions.any { it.component == name } -> fail(MermaidDiagnosticCode.INVALID_VALUE, "wardley evolve declared more than once for: $name", location)
                    else -> evolutions += WardleyEvolution(component = name, evolution = target)
                }
                continue@loop
            }
            if (text.startsWith(WARDLEY_NOTE_KEYWORD)) {
                val quoteStart = text.indexOf('"')
                val quoteEnd = text.indexOf('"', quoteStart + 1)
                if (quoteStart != WARDLEY_NOTE_KEYWORD.length - 1 || quoteEnd < 0) {
                    fail(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, "wardley note text must be double quoted", location)
                    continue@loop
                }
                if (text.substring(quoteStart + 1, quoteEnd).any { it == '\\' }) {
                    fail(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, "wardley note escapes are not supported", location)
                    continue@loop
                }
                val noteText = text.substring(quoteStart + 1, quoteEnd)
                val tail = text.substring(quoteEnd + 1)
                val openIndex = tail.indexOf('[')
                val closeIndex = tail.lastIndexOf(']')
                if (openIndex < 0 || closeIndex != tail.length - 1 || closeIndex <= openIndex + 1 || tail.substring(0, openIndex).isNotBlank()) {
                    fail(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, "Unsupported wardley note placement", location)
                    continue@loop
                }
                val coordinates = parseCoordinatePair(tail.substring(openIndex + 1, closeIndex), location) ?: continue@loop
                notes += WardleyNote(text = noteText, visibility = coordinates.first, evolution = coordinates.second)
                continue@loop
            }
            if (WARDLEY_LINK_SEPARATOR in text) {
                val parts = text.split(WARDLEY_LINK_SEPARATOR)
                if (parts.size != 2) {
                    fail(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, "Unsupported wardley link chain", location)
                    continue@loop
                }
                val from = parts[0].trim()
                val to = parts[1].trim()
                when {
                    from !in nodes -> fail(MermaidDiagnosticCode.INVALID_VALUE, "wardley link references unknown source: $from", location)
                    to !in nodes -> fail(MermaidDiagnosticCode.INVALID_VALUE, "wardley link references unknown target: $to", location)
                    from == to -> fail(MermaidDiagnosticCode.INVALID_VALUE, "wardley self links are not supported: $from", location)
                    else -> links += WardleyLink(from = from, to = to)
                }
                continue@loop
            }
            diagnostics += unsupported(statement, "Unsupported wardley syntax")
        }

        if (diagnostics.isEmpty() && nodes.isEmpty()) {
            diagnostics += MermaidDiagnostic(
                code = MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
                message = "wardley diagram requires at least one anchor or component",
                location = statements.first().location,
            )
        }
        return if (diagnostics.isEmpty()) {
            MermaidParseResult.Success(
                WardleyMapDiagram(
                    title = title,
                    nodes = nodes.values.toList(),
                    links = links.toList(),
                    evolutions = evolutions.toList(),
                    notes = notes.toList(),
                ),
            )
        } else {
            MermaidParseResult.Failure(diagnostics)
        }
    }

    /**
     * Bounded radar-beta slice. Supported statements: an optional single `title`,
     * one or more `axis` declarations whose comma-separated entries are `id` or
     * `id["Label"]` (at least three entries in total), one or more `curve
     * id["Label"]{v1, v2, ...}` declarations with exactly one value per axis,
     * and an optional single `max <number>` (default 100). Values must be finite
     * decimals within [0, max]. Everything else fails closed with a typed
     * diagnostic.
     */
    private fun parseRadar(statements: List<SourceStatement>): MermaidParseResult {
        val axes = linkedMapOf<String, String>()
        val curveIds = linkedMapOf<String, Pair<String, List<Double>?>>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        var title: String? = null
        var maximum: Double? = null

        fun fail(code: MermaidDiagnosticCode, message: String, location: SourceLocation) {
            diagnostics += MermaidDiagnostic(code = code, message = message, location = location)
        }

        fun parseLabel(raw: String?): String? {
            if (raw == null) return null
            // The capturing regexes already exclude quotes, backslashes, CR and LF;
            // an empty label is still rejected here.
            return raw.takeIf { it.isNotEmpty() }
        }

        loop@ for (statement in statements.drop(1)) {
            val text = statement.text
            val location = statement.location
            val titleMatch = RADAR_TITLE.matchEntire(text)
            if (titleMatch != null) {
                if (title != null) {
                    fail(MermaidDiagnosticCode.INVALID_VALUE, "radar accepts at most one title", location)
                } else {
                    title = titleMatch.groupValues[1]
                }
                continue@loop
            }
            val maxMatch = RADAR_MAX.matchEntire(text)
            if (maxMatch != null) {
                when {
                    maximum != null -> fail(MermaidDiagnosticCode.INVALID_VALUE, "radar accepts at most one max", location)
                    else -> {
                        val parsed = maxMatch.groupValues[1].toDoubleOrNull()
                        if (parsed == null || !parsed.isFinite() || parsed <= 0.0) {
                            fail(MermaidDiagnosticCode.INVALID_VALUE, "radar max must be a positive finite number", location)
                        } else {
                            maximum = parsed
                        }
                    }
                }
                continue@loop
            }
            val isAxis = text.startsWith(RADAR_AXIS_KEYWORD, ignoreCase = true)
            val isCurve = !isAxis && text.startsWith(RADAR_CURVE_KEYWORD, ignoreCase = true)
            if (isAxis || isCurve) {
                val keyword = if (isAxis) RADAR_AXIS_KEYWORD else RADAR_CURVE_KEYWORD
                val remainder = text.substring(keyword.length).trim()
                if (isAxis) {
                    var valid = true
                    remainder.split(',').forEach { rawEntry ->
                        val entry = rawEntry.trim()
                        val match = RADAR_AXIS_ENTRY.matchEntire(entry)
                        when {
                            match == null -> {
                                fail(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, "Unsupported radar axis entry", location)
                                valid = false
                            }
                            match.groupValues[1] in axes -> {
                                fail(MermaidDiagnosticCode.INVALID_VALUE, "Duplicate radar axis id: ${match.groupValues[1]}", location)
                                valid = false
                            }
                            else -> axes[match.groupValues[1]] = parseLabel(match.groupValues[2].ifEmpty { null }) ?: match.groupValues[1]
                        }
                    }
                    continue@loop
                }
                // Curve branch: id, optional quoted label, brace-enclosed value list.
                val match = RADAR_CURVE.matchEntire(remainder)
                if (match == null) {
                    fail(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, "Unsupported radar curve declaration", location)
                    continue@loop
                }
                val id = match.groupValues[1]
                if (id in curveIds) {
                    fail(MermaidDiagnosticCode.INVALID_VALUE, "Duplicate radar curve id: $id", location)
                    continue@loop
                }
                val declaredLabel = parseLabel(match.groupValues[2].ifEmpty { null })
                if (match.groupValues[2].isNotEmpty() && declaredLabel == null) {
                    fail(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, "radar curve labels must not be empty", location)
                    continue@loop
                }
                val rawValues = match.groupValues[3].split(',')
                val values = mutableListOf<Double>()
                var valuesValid = true
                rawValues.forEach { raw ->
                    val token = raw.trim()
                    val parsed = token.toDoubleOrNull()
                    when {
                        RADAR_VALUE.matches(token) && parsed != null && parsed.isFinite() -> values += parsed
                        else -> {
                            fail(MermaidDiagnosticCode.INVALID_VALUE, "radar curve values must be finite non-negative decimal numbers", location)
                            valuesValid = false
                        }
                    }
                }
                curveIds[id] = (declaredLabel ?: id) to (if (valuesValid) values.toList() else null)
                continue@loop
            }
            diagnostics += unsupported(statement, "Unsupported radar syntax")
        }

        if (diagnostics.isNotEmpty()) return MermaidParseResult.Failure(diagnostics.toList())

        val resolvedMaximum = maximum ?: 100.0
        if (axes.size < 3) {
            fail(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, "radar requires at least three axes", statements.first().location)
        }
        if (curveIds.isEmpty()) {
            fail(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, "radar requires at least one curve", statements.first().location)
        }
        curveIds.forEach { (id, pair) ->
            val (_, values) = pair
            if (values != null && axes.isNotEmpty()) {
                when {
                    values.size != axes.size -> fail(
                        MermaidDiagnosticCode.INVALID_VALUE,
                        "radar curve '$id' has ${values.size} values for ${axes.size} axes",
                        statements.first().location,
                    )
                    values.any { it > resolvedMaximum } -> fail(
                        MermaidDiagnosticCode.INVALID_VALUE,
                        "radar curve '$id' contains a value above max $resolvedMaximum",
                        statements.first().location,
                    )
                }
            }
        }
        if (diagnostics.isNotEmpty()) return MermaidParseResult.Failure(diagnostics.toList())
        return MermaidParseResult.Success(
            RadarChartDiagram(
                title = title,
                axes = axes.map { RadarAxis(it.key, it.value) },
                curves = curveIds.map { RadarCurve(it.key, it.value.first, it.value.second.orEmpty()) },
                maximum = resolvedMaximum,
            ),
        )
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
        var currentNamespace: String? = null

        fun ensure(id: String) {
            if (id !in classes) classes[id] = ClassDefinition(id, namespaceName = currentNamespace)
        }
        statements.drop(1).forEach { statement ->
            CLASS_NAMESPACE.matchEntire(statement.text)?.let {
                if (currentNamespace != null) diagnostics += unsupported(statement, "Nested class namespaces are not supported")
                else currentNamespace = it.groupValues[1]
                return@forEach
            }
            if (statement.text == "}") {
                if (currentNamespace == null) diagnostics += unsupported(statement, "Unexpected class namespace terminator")
                else currentNamespace = null
                return@forEach
            }
            CLASS_DECLARATION.matchEntire(statement.text)?.let {
                val id = it.groupValues[1]
                val label = it.groupValues[2].ifEmpty { id }
                classes[id] = classes[id]?.copy(label = label, namespaceName = currentNamespace)
                    ?: ClassDefinition(id, label, namespaceName = currentNamespace)
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
        if (currentNamespace != null) diagnostics += unsupported(statements.last(), "Unclosed class namespace")
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

    private fun parseXyChart(statements: List<SourceStatement>): MermaidParseResult {
        var title: String? = null
        var titleSeen = false
        var xAxis: XyAxis? = null
        var yAxis: NumericAxis? = null
        val series = mutableListOf<XySeries>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()

        statements.drop(1).forEach { statement ->
            XY_TITLE.matchEntire(statement.text)?.let { match ->
                val parsedTitle = match.groupValues[1].trim().unquote()
                if (titleSeen || parsedTitle.isEmpty()) {
                    diagnostics += unsupported(statement, "Duplicate xychart title")
                } else {
                    titleSeen = true
                    title = parsedTitle
                }
                return@forEach
            }
            XY_X_AXIS.matchEntire(statement.text)?.let { match ->
                if (xAxis != null) {
                    diagnostics += unsupported(statement, "Duplicate xychart x-axis")
                } else {
                    val categories = match.groupValues[2].csvTokens()
                    if (categories.isEmpty() || categories.any { it.isEmpty() }) {
                        diagnostics += unsupported(statement, "x-axis requires non-empty categories")
                    } else {
                        xAxis = XyAxis(match.groupValues[1].ifEmpty { null }, categories)
                    }
                }
                return@forEach
            }
            XY_Y_AXIS.matchEntire(statement.text)?.let { match ->
                if (yAxis != null) {
                    diagnostics += unsupported(statement, "Duplicate xychart y-axis")
                } else {
                    val minimum = match.groupValues[2].toDouble()
                    val maximum = match.groupValues[3].toDouble()
                    if (minimum >= maximum) {
                        diagnostics += MermaidDiagnostic(
                            MermaidDiagnosticCode.INVALID_VALUE,
                            "y-axis minimum must be lower than maximum",
                            statement.location,
                        )
                    } else {
                        yAxis = NumericAxis(match.groupValues[1].ifEmpty { null }, minimum, maximum)
                    }
                }
                return@forEach
            }
            XY_SERIES.matchEntire(statement.text)?.let { match ->
                val values = match.groupValues[2].csvTokens().mapNotNull { it.toDoubleOrNull() }
                val rawCount = match.groupValues[2].split(',').size
                if (values.isEmpty() || values.size != rawCount || values.any { !it.isFinite() }) {
                    diagnostics += unsupported(statement, "xychart series requires numeric values")
                } else {
                    series += XySeries(XySeriesKind.valueOf(match.groupValues[1].uppercase()), values)
                }
                return@forEach
            }
            diagnostics += unsupported(statement, "Unsupported xychart syntax")
        }

        val x = xAxis
        val y = yAxis
        if (x == null) diagnostics += unsupported(statements.first(), "xychart requires one x-axis")
        if (y == null) diagnostics += unsupported(statements.first(), "xychart requires one y-axis")
        if (series.isEmpty()) diagnostics += unsupported(statements.first(), "xychart requires at least one series")
        if (x != null) {
            series.forEachIndexed { index, item ->
                if (item.values.size != x.categories.size) {
                    diagnostics += MermaidDiagnostic(
                        MermaidDiagnosticCode.INVALID_VALUE,
                        "Series ${index + 1} has ${item.values.size} values for ${x.categories.size} categories",
                        statements.first().location,
                    )
                }
                if (y != null && item.values.any { it < y.minimum || it > y.maximum }) {
                    diagnostics += MermaidDiagnostic(
                        MermaidDiagnosticCode.INVALID_VALUE,
                        "Series ${index + 1} contains a value outside the y-axis range",
                        statements.first().location,
                    )
                }
            }
        }
        return if (diagnostics.isEmpty() && x != null && y != null) {
            MermaidParseResult.Success(XyChartDiagram(title, x, y, series.toList()))
        } else MermaidParseResult.Failure(diagnostics)
    }

    private fun parseMindmap(source: String): MermaidParseResult {
        val lines = source.toMindmapLines()
        val header = lines.firstOrNull()
            ?: return failure(
                MermaidDiagnosticCode.EMPTY_SOURCE,
                "The Mermaid source is empty",
                SourceLocation(1, 1),
            )
        if (!header.text.equals("mindmap", ignoreCase = true) || header.indent != 0) {
            return failure(
                MermaidDiagnosticCode.INVALID_HEADER,
                "mindmap header must be unindented",
                header.location,
            )
        }

        val nodes = mutableListOf<MindmapNode>()
        val ancestors = mutableListOf<MindmapNode>()
        val explicitIds = mutableSetOf<String>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()

        lines.drop(1).forEach { line ->
            if (line.hasTab || line.indent < MINDMAP_INDENT || line.indent % MINDMAP_INDENT != 0) {
                diagnostics += MermaidDiagnostic(
                    MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
                    "Mindmap nodes require spaces in two-space indentation steps: ${line.text}",
                    line.location,
                )
                return@forEach
            }
            val depth = line.indent / MINDMAP_INDENT - 1
            if (depth > ancestors.size) {
                diagnostics += MermaidDiagnostic(
                    MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
                    "Mindmap indentation skipped a parent level: ${line.text}",
                    line.location,
                )
                return@forEach
            }
            if (depth == 0 && nodes.isNotEmpty()) {
                diagnostics += MermaidDiagnostic(
                    MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
                    "Mindmap requires exactly one root node: ${line.text}",
                    line.location,
                )
                return@forEach
            }

            val parsed = line.text.toMindmapNodeSyntax(nodes.size)
            if (parsed == null) {
                diagnostics += MermaidDiagnostic(
                    MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
                    "Unsupported mindmap node syntax: ${line.text}",
                    line.location,
                )
                return@forEach
            }
            if (parsed.explicitId && !explicitIds.add(parsed.id)) {
                diagnostics += MermaidDiagnostic(
                    MermaidDiagnosticCode.INVALID_VALUE,
                    "Duplicate mindmap node id: ${parsed.id}",
                    line.location,
                )
                return@forEach
            }
            if (parsed.explicitId && parsed.id.startsWith("__mindmap_")) {
                diagnostics += MermaidDiagnostic(
                    MermaidDiagnosticCode.INVALID_VALUE,
                    "Mindmap explicit ids cannot use the reserved generated prefix: ${parsed.id}",
                    line.location,
                )
                return@forEach
            }

            val parent = if (depth == 0) null else ancestors.getOrNull(depth - 1)
            if (depth > 0 && parent == null) {
                diagnostics += MermaidDiagnostic(
                    MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
                    "Mindmap node has no parent: ${line.text}",
                    line.location,
                )
                return@forEach
            }
            while (ancestors.size > depth) ancestors.removeAt(ancestors.lastIndex)
            val node = MindmapNode(
                id = parsed.id,
                label = parsed.label,
                parentId = parent?.id,
                depth = depth,
                shape = parsed.shape,
            )
            nodes += node
            ancestors += node
        }

        if (nodes.isEmpty()) {
            diagnostics += MermaidDiagnostic(
                MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
                "mindmap requires one root node",
                header.location,
            )
        }
        return if (diagnostics.isEmpty()) {
            MermaidParseResult.Success(MindmapDiagram(nodes.toList()))
        } else MermaidParseResult.Failure(diagnostics)
    }

    private fun parseGantt(statements: List<SourceStatement>): MermaidParseResult {
        var title: String? = null
        var format: String? = null
        var current: GanttSection? = null
        val sections = mutableListOf<GanttSection>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        statements.drop(1).forEach { statement ->
            when {
                statement.text.startsWith("title ", true) -> title = statement.text.substringAfter(' ').trim()
                statement.text.startsWith("dateFormat ", true) -> format = statement.text.substringAfter(' ').trim()
                statement.text.startsWith("section ", true) -> { current?.let { sections += it }; current = GanttSection(statement.text.substringAfter(' ').trim(), emptyList()) }
                else -> {
                    val match = GANTT_TASK.matchEntire(statement.text)
                    val noStatusMatch = GANTT_TASK_NO_STATUS.matchEntire(statement.text)
                    val section = current
                    if ((match == null && noStatusMatch == null) || section == null) diagnostics += unsupported(statement, "Unsupported gantt task")
                    else {
                        val taskName = match?.groupValues?.get(1) ?: noStatusMatch!!.groupValues[1]
                        val rawStatus = match?.groupValues?.get(2).orEmpty()
                            .split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                        val taskId = match?.groupValues?.get(3) ?: noStatusMatch!!.groupValues[2]
                        val startText = match?.groupValues?.get(4) ?: noStatusMatch!!.groupValues[3]
                        val endOrDuration = match?.groupValues?.get(5) ?: noStatusMatch!!.groupValues[4]
                        val start = parseIsoDay(startText)
                        val duration = if (endOrDuration.endsWith("d", ignoreCase = true)) {
                            endOrDuration.dropLast(1).toIntOrNull()
                        } else {
                            parseIsoDay(endOrDuration)?.let { end -> start?.let { end - it + 1 } }
                        }
                        if (start == null || duration == null || duration <= 0) diagnostics += MermaidDiagnostic(MermaidDiagnosticCode.INVALID_VALUE, "Invalid gantt date or duration", statement.location)
                        else {
                            val status = rawStatus.firstNotNullOfOrNull { GANTT_STATUS[it] }
                            if (rawStatus.size > 1 || (rawStatus.isNotEmpty() && status == null)) {
                                diagnostics += unsupported(statement, "Unsupported gantt task status")
                            } else {
                                current = section.copy(tasks = section.tasks + GanttTask(taskName.trim(), taskId, start, duration, status ?: GanttTaskStatus.TODO))
                            }
                        }
                    }
                }
            }
        }
        current?.let { sections += it }
        if (format != "YYYY-MM-DD") diagnostics += unsupported(statements.first(), "Only dateFormat YYYY-MM-DD is supported")
        if (sections.isEmpty()) diagnostics += unsupported(statements.first(), "gantt requires a section")
        return if (diagnostics.isEmpty()) MermaidParseResult.Success(GanttDiagram(title, "YYYY-MM-DD", sections)) else MermaidParseResult.Failure(diagnostics)
    }

    private fun parseTimeline(statements: List<SourceStatement>): MermaidParseResult {
        var title: String? = null
        var section: String? = null
        val events = mutableListOf<TimelineEvent>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        statements.drop(1).forEach { statement ->
            if (statement.text.startsWith("title ", ignoreCase = true)) {
                val value = statement.text.substringAfter(' ').trim()
                if (value.isEmpty() || title != null) diagnostics += unsupported(statement, "Timeline requires at most one non-empty title") else title = value
            } else if (statement.text.startsWith("section ", ignoreCase = true)) {
                val value = statement.text.substringAfter(' ').trim()
                if (value.isEmpty()) diagnostics += unsupported(statement, "Timeline section requires a non-empty name") else section = value
            } else {
                val parts = statement.text.split(':').map { it.trim() }
                if (parts.size < 2 || parts.any { it.isEmpty() }) diagnostics += unsupported(statement, "Timeline event requires period : event [: event]")
                else events += TimelineEvent(parts.first(), parts.drop(1), section)
            }
        }
        if (events.isEmpty()) diagnostics += unsupported(statements.first(), "timeline requires at least one event")
        return if (diagnostics.isEmpty()) MermaidParseResult.Success(TimelineDiagram(title, events)) else MermaidParseResult.Failure(diagnostics)
    }

    private fun parsePacket(statements: List<SourceStatement>): MermaidParseResult {
        var title: String? = null
        val fields = mutableListOf<PacketField>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        var previousEnd = -1
        statements.drop(1).forEach { statement ->
            PACKET_TITLE.matchEntire(statement.text)?.let {
                val value = it.groupValues[1].trim()
                if (value.isEmpty() || title != null) diagnostics += unsupported(statement, "Packet requires at most one non-empty title")
                else title = value
                return@forEach
            }
            val match = PACKET_FIELD.matchEntire(statement.text)
            if (match == null) {
                diagnostics += unsupported(statement, "Unsupported packet field syntax")
                return@forEach
            }
            val start = match.groupValues[1].toIntOrNull()
            val end = match.groupValues[2].ifEmpty { match.groupValues[1] }.toIntOrNull()
            val label = match.groupValues[3]
            if (start == null || end == null || start > end || end > PACKET_MAX_BIT || start <= previousEnd || label.isBlank()) {
                diagnostics += MermaidDiagnostic(
                    MermaidDiagnosticCode.INVALID_VALUE,
                    "Packet bit ranges must be finite, ascending, non-overlapping, and labelled",
                    statement.location,
                )
            } else {
                fields += PacketField(start, end, label)
                previousEnd = end
            }
        }
        if (fields.isEmpty() && diagnostics.isEmpty()) diagnostics += unsupported(statements.first(), "packet requires at least one field")
        return if (diagnostics.isEmpty()) MermaidParseResult.Success(PacketDiagram(title, fields))
        else MermaidParseResult.Failure(diagnostics)
    }

    private fun parseQuadrantChart(statements: List<SourceStatement>): MermaidParseResult {
        var title: String? = null
        var xAxis: QuadrantAxis? = null
        var yAxis: QuadrantAxis? = null
        val quadrantLabels = MutableList<String?>(4) { null }
        val points = mutableListOf<QuadrantPoint>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        statements.drop(1).forEach { statement ->
            when {
                QUADRANT_TITLE.matches(statement.text) -> {
                    val value = QUADRANT_TITLE.matchEntire(statement.text)!!.groupValues[1].trim()
                    if (title != null) diagnostics += unsupported(statement, "quadrantChart allows one title") else title = value
                }
                QUADRANT_AXIS.matches(statement.text) -> {
                    val match = QUADRANT_AXIS.matchEntire(statement.text)!!
                    val axis = QuadrantAxis(match.groupValues[2].trim(), match.groupValues[3].trim())
                    if (match.groupValues[1].equals("x", true)) {
                        if (xAxis != null) diagnostics += unsupported(statement, "Duplicate x-axis") else xAxis = axis
                    } else if (yAxis != null) diagnostics += unsupported(statement, "Duplicate y-axis") else yAxis = axis
                }
                QUADRANT_LABEL.matches(statement.text) -> {
                    val match = QUADRANT_LABEL.matchEntire(statement.text)!!
                    val index = match.groupValues[1].toInt() - 1
                    if (quadrantLabels[index] != null) diagnostics += unsupported(statement, "Duplicate quadrant label")
                    else quadrantLabels[index] = match.groupValues[2].trim()
                }
                QUADRANT_POINT.matches(statement.text) -> {
                    val match = QUADRANT_POINT.matchEntire(statement.text)!!
                    val x = match.groupValues[2].toDoubleOrNull()
                    val y = match.groupValues[3].toDoubleOrNull()
                    if (x == null || y == null || !x.isFinite() || !y.isFinite() || x !in 0.0..1.0 || y !in 0.0..1.0) {
                        diagnostics += MermaidDiagnostic(MermaidDiagnosticCode.INVALID_VALUE, "Quadrant point coordinates must be finite values from 0 to 1", statement.location)
                    } else points += QuadrantPoint(match.groupValues[1].trim(), x, y)
                }
                else -> diagnostics += unsupported(statement, "Unsupported quadrantChart statement")
            }
        }
        val x = xAxis
        val y = yAxis
        if (x == null) diagnostics += unsupported(statements.first(), "quadrantChart requires one x-axis")
        if (y == null) diagnostics += unsupported(statements.first(), "quadrantChart requires one y-axis")
        if (points.isEmpty()) diagnostics += unsupported(statements.first(), "quadrantChart requires at least one point")
        return if (diagnostics.isEmpty() && x != null && y != null) MermaidParseResult.Success(
            QuadrantChartDiagram(title, x, y, quadrantLabels.toList(), points.toList()),
        ) else MermaidParseResult.Failure(diagnostics)
    }

    private fun parseUserJourney(statements: List<SourceStatement>): MermaidParseResult {
        var title: String? = null
        var current: UserJourneySection? = null
        val sections = mutableListOf<UserJourneySection>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()

        fun finishSection(statement: SourceStatement) {
            current?.let { section ->
                if (section.tasks.isEmpty()) {
                    diagnostics += unsupported(statement, "User journey sections require at least one task")
                }
                sections += section
            }
        }

        statements.drop(1).forEach { statement ->
            when {
                statement.text.startsWith("title ", ignoreCase = true) -> {
                    val value = statement.text.substringAfter(' ').trim()
                    if (value.isEmpty() || title != null) {
                        diagnostics += unsupported(statement, "User journey requires at most one non-empty title")
                    } else {
                        title = value
                    }
                }
                statement.text.startsWith("section ", ignoreCase = true) -> {
                    finishSection(statement)
                    val name = statement.text.substringAfter(' ').trim()
                    if (name.isEmpty()) {
                        diagnostics += unsupported(statement, "User journey section names must be non-empty")
                        current = null
                    } else {
                        current = UserJourneySection(name, emptyList())
                    }
                }
                else -> {
                    val match = USER_JOURNEY_TASK.matchEntire(statement.text)
                    val section = current
                    if (match == null || section == null) {
                        diagnostics += unsupported(statement, "Unsupported user journey task")
                    } else {
                        val actors = match.groupValues[3].split(',').map { it.trim() }
                        if (actors.any { it.isEmpty() }) {
                            diagnostics += unsupported(statement, "User journey actors must be non-empty")
                        } else {
                            current = section.copy(
                                tasks = section.tasks + UserJourneyTask(
                                    label = match.groupValues[1].trim(),
                                    score = match.groupValues[2].toInt(),
                                    actors = actors,
                                ),
                            )
                        }
                    }
                }
            }
        }
        finishSection(statements.last())
        if (sections.isEmpty()) {
            diagnostics += unsupported(statements.first(), "journey requires at least one section")
        }
        return if (diagnostics.isEmpty()) {
            MermaidParseResult.Success(UserJourneyDiagram(title, sections))
        } else MermaidParseResult.Failure(diagnostics)
    }

    private fun parseGitGraph(statements: List<SourceStatement>): MermaidParseResult {
        val branches = linkedMapOf("main" to GitGraphBranch("main", null))
        val heads = mutableMapOf<String, String?>("main" to null)
        val commits = mutableListOf<GitGraphCommit>()
        val commitIds = mutableSetOf<String>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        var currentBranch = "main"
        var autoId = 1

        fun nextId(): String {
            while ("commit-$autoId" in commitIds) autoId += 1
            return "commit-${autoId++}"
        }

        fun appendCommit(statement: SourceStatement, match: MatchResult, isMerge: Boolean, sourceBranch: String? = null) {
            val id = match.groupValues[if (isMerge) 2 else 1].ifEmpty { nextId() }
            val typeValue = match.groupValues[if (isMerge) 3 else 2]
            val tag = match.groupValues[if (isMerge) 4 else 3].ifEmpty { null }
            if (!commitIds.add(id)) {
                diagnostics += unsupported(statement, "gitGraph commit IDs must be unique")
                return
            }
            val currentHead = heads[currentBranch]
            val parents = if (isMerge) {
                val sourceHead = heads[sourceBranch]
                if (currentHead == null || sourceHead == null) {
                    commitIds.remove(id)
                    diagnostics += unsupported(statement, "gitGraph merge requires commits on both branches")
                    return
                }
                listOf(currentHead, sourceHead)
            } else listOfNotNull(currentHead)
            val type = typeValue.takeIf { it.isNotEmpty() }
                ?.uppercase()
                ?.let(GitGraphCommitType::valueOf)
                ?: GitGraphCommitType.NORMAL
            commits += GitGraphCommit(id, currentBranch, parents, type, tag, isMerge)
            heads[currentBranch] = id
        }

        statements.drop(1).forEach { statement ->
            when {
                GIT_COMMIT.matches(statement.text) -> appendCommit(statement, GIT_COMMIT.matchEntire(statement.text)!!, false)
                GIT_BRANCH.matches(statement.text) -> {
                    val name = GIT_BRANCH.matchEntire(statement.text)!!.groupValues[1]
                    if (name in branches) diagnostics += unsupported(statement, "gitGraph branch names must be unique")
                    else {
                        branches[name] = GitGraphBranch(name, heads[currentBranch])
                        heads[name] = heads[currentBranch]
                        currentBranch = name
                    }
                }
                GIT_CHECKOUT.matches(statement.text) -> {
                    val name = GIT_CHECKOUT.matchEntire(statement.text)!!.groupValues[2]
                    if (name !in branches) diagnostics += unsupported(statement, "gitGraph checkout requires an existing branch")
                    else currentBranch = name
                }
                GIT_MERGE.matches(statement.text) -> {
                    val match = GIT_MERGE.matchEntire(statement.text)!!
                    val source = match.groupValues[1]
                    when {
                        source !in branches -> diagnostics += unsupported(statement, "gitGraph merge requires an existing branch")
                        source == currentBranch -> diagnostics += unsupported(statement, "gitGraph cannot merge a branch into itself")
                        heads[source] == heads[currentBranch] -> diagnostics += unsupported(statement, "gitGraph merge requires divergent branch heads")
                        else -> appendCommit(statement, match, true, source)
                    }
                }
                else -> diagnostics += unsupported(statement, "Unsupported gitGraph statement")
            }
        }
        if (commits.isEmpty()) diagnostics += unsupported(statements.first(), "gitGraph requires at least one commit")
        return if (diagnostics.isEmpty()) {
            MermaidParseResult.Success(GitGraphDiagram(branches.values.toList(), commits))
        } else MermaidParseResult.Failure(diagnostics)
    }

    private fun parseRequirement(statements: List<SourceStatement>): MermaidParseResult {
        val requirements = linkedMapOf<String, RequirementDefinition>()
        val elements = linkedMapOf<String, RequirementElement>()
        val relationships = mutableListOf<RequirementRelationship>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        var accessibilityTitle: String? = null
        var accessibilityDescription: String? = null
        var block: RequirementBlock? = null

        fun duplicateName(name: String): Boolean = name in requirements || name in elements
        fun closeBlock(statement: SourceStatement) {
            when (val current = block) {
                is RequirementBlock.Requirement -> {
                    val id = current.fields["id"]
                    val text = current.fields["text"]
                    val risk = current.fields["risk"]?.uppercase()?.let { runCatching { RequirementRisk.valueOf(it) }.getOrNull() }
                    val method = current.fields["verifymethod"]?.uppercase()?.let {
                        runCatching { RequirementVerifyMethod.valueOf(it) }.getOrNull()
                    }
                    if (id == null || text == null || risk == null || method == null) {
                        diagnostics += unsupported(statement, "Requirement requires id, text, risk low|medium|high, and verifymethod")
                    } else {
                        requirements[current.name] = RequirementDefinition(current.name, id, text, risk, method)
                    }
                }
                is RequirementBlock.Element -> {
                    val type = current.fields["type"]
                    val docRef = current.fields["docref"]
                    if (type == null || docRef == null) {
                        diagnostics += unsupported(statement, "Element requires type and docref")
                    } else {
                        elements[current.name] = RequirementElement(current.name, type, docRef)
                    }
                }
                null -> diagnostics += unsupported(statement, "Unexpected requirement block terminator")
            }
            block = null
        }

        statements.drop(1).forEach { statement ->
            val current = block
            if (current != null) {
                if (statement.text == "}") {
                    closeBlock(statement)
                    return@forEach
                }
                val field = REQUIREMENT_FIELD.matchEntire(statement.text)
                if (field == null) {
                    diagnostics += unsupported(statement, "Unsupported requirement block field")
                    return@forEach
                }
                val key = field.groupValues[1].lowercase()
                val allowed = when (current) {
                    is RequirementBlock.Requirement -> REQUIREMENT_FIELDS
                    is RequirementBlock.Element -> ELEMENT_FIELDS
                }
                if (key !in allowed || key in current.fields) {
                    diagnostics += unsupported(statement, "Unknown or duplicate requirement block field")
                } else {
                    current.fields[key] = field.groupValues[2].trim()
                }
                return@forEach
            }

            if (statement.text.startsWith("accTitle:", ignoreCase = true)) {
                val value = statement.text.substringAfter(':').trim()
                if (value.isEmpty() || accessibilityTitle != null) diagnostics += unsupported(statement, "Requirement accessibility title must be non-empty and unique")
                else accessibilityTitle = value
                return@forEach
            }
            if (statement.text.startsWith("accDescr:", ignoreCase = true)) {
                val value = statement.text.substringAfter(':').trim()
                if (value.isEmpty() || accessibilityDescription != null) diagnostics += unsupported(statement, "Requirement accessibility description must be non-empty and unique")
                else accessibilityDescription = value
                return@forEach
            }
            REQUIREMENT_START.matchEntire(statement.text)?.let {
                val name = it.groupValues[1]
                if (duplicateName(name)) diagnostics += unsupported(statement, "Duplicate requirement artifact name")
                else block = RequirementBlock.Requirement(name)
                return@forEach
            }
            ELEMENT_START.matchEntire(statement.text)?.let {
                val name = it.groupValues[1]
                if (duplicateName(name)) diagnostics += unsupported(statement, "Duplicate requirement artifact name")
                else block = RequirementBlock.Element(name)
                return@forEach
            }
            REQUIREMENT_RELATION.matchEntire(statement.text)?.let {
                relationships += RequirementRelationship(
                    from = it.groupValues[1],
                    to = it.groupValues[3],
                    kind = when (it.groupValues[2].lowercase()) {
                        "contains" -> RequirementRelationshipKind.CONTAINS
                        "satisfies" -> RequirementRelationshipKind.SATISFIES
                        else -> RequirementRelationshipKind.VERIFIES
                    },
                )
                return@forEach
            }
            diagnostics += unsupported(statement, "Unsupported requirementDiagram syntax")
        }
        if (block != null) {
            diagnostics += MermaidDiagnostic(
                MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
                "Unclosed requirementDiagram block",
                statements.last().location,
            )
        }
        val names = requirements.keys + elements.keys
        relationships.forEach { relationship ->
            if (names.isNotEmpty() && (relationship.from !in names || relationship.to !in names)) {
                diagnostics += MermaidDiagnostic(
                    MermaidDiagnosticCode.INVALID_VALUE,
                    "Requirement relationship references an unknown artifact: ${relationship.from} -> ${relationship.to}",
                    statements.first().location,
                )
            }
        }
        return if (diagnostics.isEmpty() && (requirements.isNotEmpty() || elements.isNotEmpty() || relationships.isNotEmpty())) {
            MermaidParseResult.Success(
                RequirementDiagram(
                    requirements = requirements.values.toList(),
                    elements = elements.values.toList(),
                    relationships = relationships,
                    accessibilityTitle = accessibilityTitle,
                    accessibilityDescription = accessibilityDescription,
                )
            )
        } else {
            if (requirements.isEmpty() && diagnostics.isEmpty()) {
                diagnostics += unsupported(statements.first(), "requirementDiagram requires at least one requirement or element")
            }
            MermaidParseResult.Failure(diagnostics)
        }
    }

    private fun parseKanban(source: String): MermaidParseResult {
        val lines = source.toMindmapLines()
        val columns = mutableListOf<KanbanColumn>()
        val ids = mutableSetOf<String>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        var current: KanbanColumn? = null
        fun finish(line: MindmapSourceLine) {
            current?.let {
                if (it.cards.isEmpty()) diagnostics += unsupported(SourceStatement(line.text, line.location), "Kanban columns require at least one card")
                columns += it
            }
        }
        lines.drop(1).forEach { line ->
            val match = KANBAN_ITEM.matchEntire(line.text)
            if (line.hasTab || match == null || line.indent !in setOf(0, 2)) {
                diagnostics += unsupported(SourceStatement(line.text, line.location), "Unsupported kanban syntax or indentation")
                return@forEach
            }
            val id = match.groupValues[1]
            val label = match.groupValues[2].trim()
            if (label.isEmpty() || !ids.add(id)) {
                diagnostics += unsupported(SourceStatement(line.text, line.location), "Kanban IDs and labels must be unique and non-empty")
            } else if (line.indent == 0) {
                finish(line)
                current = KanbanColumn(id, label, emptyList())
            } else {
                val column = current
                if (column == null) diagnostics += unsupported(SourceStatement(line.text, line.location), "Kanban cards require a parent column")
                else current = column.copy(cards = column.cards + KanbanCard(id, label))
            }
        }
        lines.lastOrNull()?.let(::finish)
        if (columns.isEmpty()) diagnostics += unsupported(SourceStatement("kanban", SourceLocation(1, 1)), "Kanban requires at least one column")
        return if (diagnostics.isEmpty()) MermaidParseResult.Success(KanbanDiagram(columns)) else MermaidParseResult.Failure(diagnostics)
    }

    private fun parseBlock(statements: List<SourceStatement>): MermaidParseResult {
        var columns: Int? = null
        val nodes = mutableListOf<BlockNode>()
        val nodeIds = mutableSetOf<String>()
        val edges = mutableListOf<BlockEdge>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()

        statements.drop(1).forEach { statement ->
            BLOCK_COLUMNS.matchEntire(statement.text)?.let { match ->
                val value = match.groupValues[1].toIntOrNull()
                if (columns != null || value == null || value !in 1..16) {
                    diagnostics += unsupported(statement, "block requires one columns value from 1 to 16")
                } else {
                    columns = value
                }
                return@forEach
            }
            BLOCK_EDGE.matchEntire(statement.text)?.let { match ->
                val from = match.groupValues[1]
                val to = match.groupValues[2]
                if (from == to) diagnostics += unsupported(statement, "block self edges are not supported")
                else edges += BlockEdge(from, to)
                return@forEach
            }
            BLOCK_NODE.matchEntire(statement.text)?.let { match ->
                val id = match.groupValues[1]
                val label = match.groupValues[2].ifEmpty { id }.trim()
                val span = match.groupValues[3].ifEmpty { "1" }.toIntOrNull()
                if (span == null) {
                    diagnostics += unsupported(statement, "block column span is too large")
                } else if (!nodeIds.add(id) || label.isEmpty()) {
                    diagnostics += unsupported(statement, "block IDs must be unique and labels non-empty")
                } else {
                    nodes += BlockNode(id, label, span)
                }
                return@forEach
            }
            diagnostics += unsupported(statement, "Unsupported block diagram syntax")
        }

        val columnCount = columns
        if (columnCount == null) {
            diagnostics += unsupported(statements.first(), "block requires a columns declaration")
        } else {
            nodes.filter { it.columnSpan > columnCount }.forEach {
                diagnostics += MermaidDiagnostic(
                    MermaidDiagnosticCode.INVALID_VALUE,
                    "Block ${it.id} spans more than $columnCount columns",
                    statements.first().location,
                )
            }
        }
        if (nodes.isEmpty()) diagnostics += unsupported(statements.first(), "block requires at least one node")
        edges.forEach { edge ->
            if (edge.from !in nodeIds || edge.to !in nodeIds) {
                diagnostics += MermaidDiagnostic(
                    MermaidDiagnosticCode.INVALID_VALUE,
                    "Block edge references an unknown node: ${edge.from} -> ${edge.to}",
                    statements.first().location,
                )
            }
        }
        return if (diagnostics.isEmpty()) {
            MermaidParseResult.Success(BlockDiagram(columnCount!!, nodes, edges))
        } else MermaidParseResult.Failure(diagnostics)
    }

    private fun parseSankey(source: String): MermaidParseResult {
        val nodes = linkedMapOf<String, SankeyNode>()
        val links = mutableListOf<SankeyLink>()
        val linkIds = mutableSetOf<Pair<String, String>>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        val lines = source.lineSequence().mapIndexedNotNull { index, raw ->
            val trimmed = raw.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("%%")) null else SourceStatement(raw.trimEnd(), SourceLocation(index + 1, 1))
        }.toList()

        lines.drop(1).forEach { statement ->
            val fields = statement.text.parseSankeyCsvLine()
            if (fields == null || fields.size != 3) {
                diagnostics += unsupported(statement, "sankey rows require exactly three valid CSV fields")
                return@forEach
            }
            val sourceLabel = fields[0].trim()
            val targetLabel = fields[1].trim()
            val value = fields[2].trim().toDoubleOrNull()
            if (sourceLabel.isEmpty() || targetLabel.isEmpty()) {
                diagnostics += unsupported(statement, "sankey source and target labels must be non-empty")
                return@forEach
            }
            if (value == null || !value.isFinite() || value <= 0.0) {
                diagnostics += unsupported(statement, "sankey values must be finite and positive")
                return@forEach
            }
            if (sourceLabel == targetLabel || !linkIds.add(sourceLabel to targetLabel)) {
                diagnostics += unsupported(statement, "sankey self-links and duplicate links are not supported")
                return@forEach
            }
            nodes.getOrPut(sourceLabel) { SankeyNode(sourceLabel, sourceLabel) }
            nodes.getOrPut(targetLabel) { SankeyNode(targetLabel, targetLabel) }
            links += SankeyLink(sourceLabel, targetLabel, value)
        }
        if (links.isEmpty()) diagnostics += unsupported(lines.first(), "sankey requires at least one link")
        if (diagnostics.isEmpty() && sankeyHasCycle(nodes.keys, links)) {
            diagnostics += unsupported(lines.first(), "Cyclic sankey links are not supported")
        }
        return if (diagnostics.isEmpty()) {
            MermaidParseResult.Success(SankeyDiagram(nodes.values.toList(), links))
        } else MermaidParseResult.Failure(diagnostics)
    }

    private fun parseTreemap(source: String): MermaidParseResult {
        val lines = source.toMindmapLines()
        val roots = mutableListOf<MutableTreemapNode>()
        val stack = mutableListOf<MutableTreemapNode>()
        val labels = mutableSetOf<String>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        if (lines.firstOrNull()?.text != "treemap-beta") {
            return failure(
                MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
                "Treemap requires the exact treemap-beta header",
                lines.firstOrNull()?.location ?: SourceLocation(1, 1),
            )
        }
        lines.drop(1).forEach { line ->
            val statement = SourceStatement(line.text, line.location)
            val match = TREEMAP_NODE.matchEntire(line.text)
            if (line.hasTab || line.indent % 2 != 0 || match == null) {
                diagnostics += unsupported(statement, "Unsupported treemap syntax or indentation")
                return@forEach
            }
            val depth = line.indent / 2
            if (depth > stack.size) {
                diagnostics += unsupported(statement, "Treemap indentation cannot jump levels")
                return@forEach
            }
            val label = match.groupValues[1].trim()
            val rawValue = match.groupValues[2]
            val value = rawValue.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
            if (label.isEmpty() || !labels.add(label)) {
                diagnostics += unsupported(statement, "Treemap labels must be unique and non-empty")
                return@forEach
            }
            if (rawValue.isNotEmpty() && (value == null || !value.isFinite() || value <= 0.0)) {
                diagnostics += unsupported(statement, "Treemap leaf values must be finite and positive")
                return@forEach
            }
            val node = MutableTreemapNode(label, value, location = line.location)
            if (depth == 0) {
                roots += node
            } else {
                val parent = stack[depth - 1]
                if (parent.value != null) {
                    diagnostics += unsupported(statement, "Treemap leaves cannot have children")
                    return@forEach
                }
                parent.children += node
            }
            while (stack.size > depth) stack.removeAt(stack.lastIndex)
            stack += node
        }
        fun validate(node: MutableTreemapNode): Double? {
            if (node.value == null && node.children.isEmpty()) {
                diagnostics += unsupported(SourceStatement(node.label, node.location), "Treemap sections require children")
            }
            val weight = node.value ?: node.children.mapNotNull(::validate).sum()
            if (!weight.isFinite()) {
                diagnostics += unsupported(SourceStatement(node.label, node.location), "Treemap section weights must have a finite sum")
                return null
            }
            return weight
        }
        roots.forEach(::validate)
        roots.filter { it.value != null }.forEach {
            diagnostics += unsupported(SourceStatement(it.label, it.location), "Treemap roots must be sections")
        }
        if (roots.isEmpty()) diagnostics += unsupported(SourceStatement("treemap-beta", SourceLocation(1, 1)), "Treemap requires at least one root section")
        return if (diagnostics.isEmpty()) {
            MermaidParseResult.Success(TreemapDiagram(roots.map(MutableTreemapNode::freeze)))
        } else MermaidParseResult.Failure(diagnostics)
    }

    private fun parseVenn(source: String): MermaidParseResult {
        val physicalLines = source.toMindmapLines()
        if (physicalLines.firstOrNull()?.text != "venn-beta") {
            return failure(
                MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
                "Venn requires the exact venn-beta header",
                physicalLines.firstOrNull()?.location ?: SourceLocation(1, 1),
            )
        }
        val statements = source.toStatements()
        val sets = linkedMapOf<String, VennSet>()
        val unions = mutableListOf<VennUnion>()
        val unionKeys = mutableSetOf<String>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        var title: String? = null

        statements.drop(1).forEach { statement ->
            VENN_TITLE.matchEntire(statement.text)?.let { match ->
                if (title != null || sets.isNotEmpty() || unions.isNotEmpty()) {
                    diagnostics += unsupported(statement, "Venn title must appear once before sets")
                } else {
                    title = match.groupValues[1]
                }
                return@forEach
            }
            VENN_SET.matchEntire(statement.text)?.let { match ->
                val id = match.groupValues[1].unquoteVennId()
                val label = match.groupValues[2].ifEmpty { id }
                val size = match.groupValues[3].parseVennSize(statement, diagnostics)
                if (id in sets) diagnostics += unsupported(statement, "Duplicate venn set")
                else if (size != INVALID_VENN_SIZE) sets[id] = VennSet(id, label, size)
                return@forEach
            }
            VENN_UNION.matchEntire(statement.text)?.let { match ->
                val rawMembers = match.groupValues[1].parseVennMembers()
                val members = rawMembers.mapNotNull { token -> VENN_IDENTIFIER.matchEntire(token)?.value?.unquoteVennId() }
                val key = members.sorted().joinToString("\u0000")
                val size = match.groupValues[3].parseVennSize(statement, diagnostics)
                when {
                    members.size != rawMembers.size || members.size !in 2..3 -> diagnostics += unsupported(statement, "Venn unions require two or three valid set identifiers")
                    members.toSet().size != members.size -> diagnostics += unsupported(statement, "Venn union members must be unique")
                    members.any { it !in sets } -> diagnostics += unsupported(statement, "Venn union members must reference earlier sets")
                    !unionKeys.add(key) -> diagnostics += unsupported(statement, "Duplicate venn union")
                    size != INVALID_VENN_SIZE -> unions += VennUnion(members, match.groupValues[2].ifEmpty { null }, size)
                }
                return@forEach
            }
            diagnostics += unsupported(statement, "Unsupported venn syntax")
        }
        if (sets.size !in 2..3) {
            diagnostics += unsupported(statements.first(), "Venn partial support requires two or three sets")
        }
        return if (diagnostics.isEmpty()) MermaidParseResult.Success(VennDiagram(title, sets.values.toList(), unions))
        else MermaidParseResult.Failure(diagnostics)
    }

    private fun parseUsecase(source: String): MermaidParseResult {
        val physicalLines = source.toMindmapLines()
        if (physicalLines.firstOrNull()?.text !in setOf("usecase-beta", "usecaseDiagram")) {
            return failure(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, "Usecase requires the usecase-beta or usecaseDiagram header", physicalLines.firstOrNull()?.location ?: SourceLocation(1, 1))
        }
        val statements = source.toStatements()
        val actors = linkedMapOf<String, UsecaseActor>()
        val nodes = linkedMapOf<String, UsecaseNode>()
        val relationships = mutableListOf<UsecaseRelationship>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        var direction = FlowDirection.TB
        var hasDirection = false
        statements.drop(1).forEach { statement ->
            USECASE_DIRECTION.matchEntire(statement.text)?.let {
                if (hasDirection) diagnostics += unsupported(statement, "Duplicate usecase direction")
                else {
                    direction = FlowDirection.valueOf(it.groupValues[1].uppercase())
                    hasDirection = true
                }
                return@forEach
            }
            USECASE_ACTOR.matchEntire(statement.text)?.let { match ->
                val id = match.groupValues[1]
                val label = match.groupValues[2].ifEmpty { id }
                if (id in actors || id in nodes) diagnostics += unsupported(statement, "Duplicate usecase identifier")
                else actors[id] = UsecaseActor(id, label)
                return@forEach
            }
            USECASE_ELLIPSE.matchEntire(statement.text)?.let { match ->
                val id = match.groupValues[1]
                val label = match.groupValues[2].ifEmpty { id }
                if (id in actors || id in nodes) diagnostics += unsupported(statement, "Duplicate usecase identifier")
                else nodes[id] = UsecaseNode(id, label, UsecaseShape.ELLIPSE)
                return@forEach
            }
            USECASE_RECTANGLE.matchEntire(statement.text)?.let { match ->
                val id = match.groupValues[1]
                val label = match.groupValues[2]
                if (id in actors || id in nodes) diagnostics += unsupported(statement, "Duplicate usecase identifier")
                else nodes[id] = UsecaseNode(id, label, UsecaseShape.RECTANGLE)
                return@forEach
            }
            USECASE_EDGE.matchEntire(statement.text)?.let { match ->
                val source = match.groupValues[1]
                val target = match.groupValues[3]
                if (source in actors || source in nodes) {
                    if (target !in actors && target !in nodes) nodes[target] = UsecaseNode(target, target, UsecaseShape.ELLIPSE)
                    relationships += UsecaseRelationship(source, target, match.groupValues[2].ifEmpty { null })
                } else diagnostics += unsupported(statement, "Usecase relationship source must be declared")
                return@forEach
            }
            diagnostics += unsupported(statement, "Unsupported usecase syntax")
        }
        if (actors.isEmpty() || nodes.isEmpty()) diagnostics += unsupported(statements.first(), "Usecase requires actors and use cases")
        return if (diagnostics.isEmpty()) MermaidParseResult.Success(UsecaseDiagram(direction, actors.values.toList(), nodes.values.toList(), relationships))
        else MermaidParseResult.Failure(diagnostics)
    }

    private fun parseArchitecture(source: String): MermaidParseResult {
        val physicalLines = source.toMindmapLines()
        if (physicalLines.firstOrNull()?.text != "architecture-beta") {
            return failure(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, "Architecture requires the exact architecture-beta header", physicalLines.firstOrNull()?.location ?: SourceLocation(1, 1))
        }
        val statements = source.toStatements()
        val groups = linkedMapOf<String, ArchitectureGroup>()
        val services = linkedMapOf<String, ArchitectureService>()
        val edges = mutableListOf<ArchitectureEdge>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        statements.drop(1).forEach { statement ->
            ARCHITECTURE_GROUP.matchEntire(statement.text)?.let { match ->
                val id = match.groupValues[1]
                if (id in groups || id in services) diagnostics += unsupported(statement, "Duplicate architecture identifier")
                else groups[id] = ArchitectureGroup(id, match.groupValues[2], match.groupValues[3])
                return@forEach
            }
            ARCHITECTURE_SERVICE.matchEntire(statement.text)?.let { match ->
                val id = match.groupValues[1]
                val groupId = match.groupValues[4].ifEmpty { null }
                when {
                    id in groups || id in services -> diagnostics += unsupported(statement, "Duplicate architecture identifier")
                    groupId != null && groupId !in groups -> diagnostics += unsupported(statement, "Architecture service group must be declared first")
                    else -> services[id] = ArchitectureService(id, match.groupValues[2], match.groupValues[3], groupId)
                }
                return@forEach
            }
            ARCHITECTURE_EDGE.matchEntire(statement.text)?.let { match ->
                val sourceId = match.groupValues[1]
                val targetId = match.groupValues[5]
                val edge = ArchitectureEdge(
                    sourceId = sourceId,
                    sourcePort = match.groupValues[2].toArchitecturePort(),
                    targetId = targetId,
                    targetPort = match.groupValues[4].toArchitecturePort(),
                    directed = match.groupValues[3] == "-->",
                )
                when {
                    sourceId !in services || targetId !in services -> diagnostics += unsupported(statement, "Architecture edge services must be declared first")
                    sourceId == targetId -> diagnostics += unsupported(statement, "Architecture self edges are not supported")
                    edge in edges -> diagnostics += unsupported(statement, "Duplicate architecture edge")
                    else -> edges += edge
                }
                return@forEach
            }
            diagnostics += unsupported(statement, "Unsupported architecture syntax")
        }
        if (services.isEmpty()) diagnostics += unsupported(statements.first(), "Architecture requires at least one service")
        return if (diagnostics.isEmpty()) MermaidParseResult.Success(ArchitectureDiagram(groups.values.toList(), services.values.toList(), edges))
        else MermaidParseResult.Failure(diagnostics)
    }

    private fun parseC4Context(source: String): MermaidParseResult {
        val physicalLines = source.toMindmapLines()
        if (physicalLines.firstOrNull()?.text != "C4Context") {
            return failure(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, "C4 requires the exact C4Context header", physicalLines.firstOrNull()?.location ?: SourceLocation(1, 1))
        }
        val statements = source.toStatements()
        val elements = linkedMapOf<String, C4Element>()
        val relationships = mutableListOf<C4Relationship>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        var title: String? = null
        statements.drop(1).forEach { statement ->
            C4_TITLE.matchEntire(statement.text)?.let { match ->
                if (title != null) diagnostics += unsupported(statement, "Duplicate C4 title") else title = match.groupValues[1]
                return@forEach
            }
            C4_ELEMENT.matchEntire(statement.text)?.let { match ->
                val id = match.groupValues[2]
                if (id in elements) diagnostics += unsupported(statement, "Duplicate C4 element identifier")
                else elements[id] = C4Element(
                    id = id,
                    label = match.groupValues[3],
                    description = match.groupValues[4].ifEmpty { null },
                    kind = if (match.groupValues[1].startsWith("Person")) C4ElementKind.PERSON else C4ElementKind.SYSTEM,
                    external = match.groupValues[1].endsWith("_Ext"),
                )
                return@forEach
            }
            C4_RELATIONSHIP.matchEntire(statement.text)?.let { match ->
                val relationship = C4Relationship(match.groupValues[2], match.groupValues[3], match.groupValues[4], match.groupValues[5].ifEmpty { null }, match.groupValues[1] == "BiRel")
                when {
                    relationship.sourceId !in elements || relationship.targetId !in elements -> diagnostics += unsupported(statement, "C4 relationship endpoints must be declared first")
                    relationship.sourceId == relationship.targetId -> diagnostics += unsupported(statement, "C4 self relationships are not supported")
                    relationship in relationships -> diagnostics += unsupported(statement, "Duplicate C4 relationship")
                    else -> relationships += relationship
                }
                return@forEach
            }
            diagnostics += unsupported(statement, "Unsupported C4 syntax")
        }
        if (elements.isEmpty()) diagnostics += unsupported(statements.first(), "C4Context requires at least one element")
        return if (diagnostics.isEmpty()) MermaidParseResult.Success(C4Diagram(title, elements.values.toList(), relationships)) else MermaidParseResult.Failure(diagnostics)
    }

    /**
     * Bounded ishikawa/ishikawa-beta slice following the official indentation
     * grammar: the first content line is the effect and every later content
     * line is a cause label whose depth is its relative indentation (clamped
     * to at least one level below the first cause). Blank and %% comment lines
     * are ignored. Tabs in indentation fail closed because their width is
     * ambiguous; directives, configuration, styling, and every other
     * decoration syntax are outside the slice and fail closed.
     */
    private fun parseIshikawa(source: String): MermaidParseResult {
        val lines = source.lineSequence().toList()
        val headerIndex = lines.indexOfFirst {
            val trimmed = it.trim()
            trimmed.equals("ishikawa", ignoreCase = true) || trimmed.equals("ishikawa-beta", ignoreCase = true) || trimmed.equals("fishbone", ignoreCase = true)
        }
        if (headerIndex < 0) {
            return failure(MermaidDiagnosticCode.INVALID_HEADER, "Expected ishikawa, ishikawa-beta, or fishbone header", SourceLocation(1, 1))
        }

        class CauseBuilder(val text: String) {
            val children = mutableListOf<CauseBuilder>()

            fun toNode(): IshikawaNode = IshikawaNode(text, children.map { it.toNode() })
        }

        val diagnostics = mutableListOf<MermaidDiagnostic>()
        var effectBuilder: CauseBuilder? = null
        val stack = mutableListOf<Pair<Int, CauseBuilder>>()
        var baseIndent = -1
        lines.drop(headerIndex + 1).forEachIndexed { offset, raw ->
            val text = raw.trim()
            if (text.isEmpty() || text.startsWith("%%")) return@forEachIndexed
            val location = SourceLocation(headerIndex + offset + 2, raw.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0) + 1)
            if (raw.contains('\t')) {
                diagnostics += unsupported(SourceStatement(text, location), "Tabs are not supported in ishikawa indentation")
                return@forEachIndexed
            }
            val effect = effectBuilder
            if (effect == null) {
                val created = CauseBuilder(text)
                effectBuilder = created
                stack += 0 to created
                return@forEachIndexed
            }
            val indent = raw.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
            if (baseIndent < 0) baseIndent = indent
            // Relative level like the official parser: measured from the first
            // cause's indentation and clamped so no node sits above the causes.
            val level = maxOf(1, indent - baseIndent + 1)
            while (stack.size > 1 && stack.last().first >= level) {
                // MutableList.removeLast() compiles to the Java 21 List method on
                // this toolchain and breaks JDK 17 runtimes; removeAt stays portable.
                stack.removeAt(stack.lastIndex)
            }
            val node = CauseBuilder(text)
            stack.last().second.children += node
            stack += level to node
        }
        if (effectBuilder == null) {
            diagnostics += unsupported(SourceStatement("ishikawa", SourceLocation(headerIndex + 1, 1)), "ishikawa requires an effect line")
        }
        if (diagnostics.isNotEmpty()) return MermaidParseResult.Failure(diagnostics)
        return MermaidParseResult.Success(IshikawaDiagram(effectBuilder!!.toNode()))
    }

    private fun parseCynefin(source: String): MermaidParseResult {
        val lines = source.lineSequence().toList()
        val headerIndex = lines.indexOfFirst { it.trim().equals("cynefin-beta", ignoreCase = true) || it.trim().equals("cynefin", ignoreCase = true) }
        if (headerIndex < 0) return failure(MermaidDiagnosticCode.INVALID_HEADER, "Expected cynefin-beta or cynefin header", SourceLocation(1, 1))
        var title: String? = null
        val blocks = linkedMapOf<CynefinDomain, MutableList<String>>()
        val transitions = mutableListOf<CynefinTransition>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        var current: CynefinDomain? = null
        val domainRegex = Regex("^(complex|complicated|clear|chaotic|confusion)$", RegexOption.IGNORE_CASE)
        val itemRegex = Regex("^\\\"([^\\\"\\r\\n]+)\\\"$")
        val transitionRegex = Regex("^(complex|complicated|clear|chaotic|confusion)\\s+-->\\s+(complex|complicated|clear|chaotic|confusion)(?:\\s*:\\s*\\\"([^\\\"\\r\\n]+)\\\")?$", RegexOption.IGNORE_CASE)
        lines.drop(headerIndex + 1).forEachIndexed { offset, raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("%%")) return@forEachIndexed
            val location = SourceLocation(headerIndex + offset + 2, raw.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0) + 1)
            when {
                line.startsWith("title ", ignoreCase = true) && title == null -> title = line.substringAfter(' ').trim().takeIf { it.isNotEmpty() }
                line.startsWith("title ", ignoreCase = true) -> diagnostics += unsupported(SourceStatement(line, location), "Duplicate cynefin title")
                transitionRegex.matches(line) -> {
                    val m = transitionRegex.matchEntire(line)!!
                    val from = CynefinDomain.valueOf(m.groupValues[1].uppercase())
                    val to = CynefinDomain.valueOf(m.groupValues[2].uppercase())
                    if (from != to) transitions += CynefinTransition(from, to, m.groupValues[3].ifEmpty { null })
                }
                domainRegex.matches(line) -> {
                    val domain = CynefinDomain.valueOf(line.uppercase())
                    if (domain in blocks) diagnostics += unsupported(SourceStatement(line, location), "Duplicate cynefin domain")
                    else { blocks[domain] = mutableListOf(); current = domain }
                }
                itemRegex.matches(line) && current != null -> blocks.getValue(current!!).add(itemRegex.matchEntire(line)!!.groupValues[1])
                else -> diagnostics += unsupported(SourceStatement(line, location), "Unsupported cynefin syntax")
            }
        }
        return if (diagnostics.isEmpty()) MermaidParseResult.Success(
            CynefinDiagram(title, blocks.map { CynefinDomainBlock(it.key, it.value.toList()) }, transitions.toList())
        ) else MermaidParseResult.Failure(diagnostics)
    }

    private fun parseEventModeling(source: String): MermaidParseResult {
        val statements = source.toStatements()
        val headerLine = source.lineSequence().firstOrNull()?.trim()
        if (headerLine != "eventmodeling") {
            return failure(MermaidDiagnosticCode.INVALID_HEADER, "Expected exact eventmodeling header", SourceLocation(1, 1))
        }
        var title: String? = null
        val frames = linkedMapOf<String, EventModelingFrame>()
        val relations = mutableListOf<EventModelingRelation>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        var inferenceSource: String? = null
        statements.drop(1).forEach { statement ->
            EVENT_MODELING_TITLE.matchEntire(statement.text)?.let { m ->
                val value = m.groupValues[1].trim()
                if (title != null || value.isEmpty()) diagnostics += unsupported(statement, "Duplicate or empty Event Modeling title") else title = value
                return@forEach
            }
            EVENT_MODELING_FRAME.matchEntire(statement.text)?.let { m ->
                val reset = m.groupValues[1].equals("rf", true) || m.groupValues[1].equals("resetframe", true)
                val id = m.groupValues[2]
                val kind = when (m.groupValues[3].lowercase()) { "ui" -> EventModelingEntityKind.UI; "cmd", "command" -> EventModelingEntityKind.COMMAND; "evt", "event" -> EventModelingEntityKind.EVENT; "pcr", "processor" -> EventModelingEntityKind.PROCESSOR; else -> EventModelingEntityKind.READ_MODEL }
                val sources = m.groupValues[5].takeIf { it.isNotEmpty() }?.split(Regex("\\s*->>\\s*"))?.filter { it.isNotEmpty() }.orEmpty()
                when { id in frames -> diagnostics += unsupported(statement, "Duplicate Event Modeling frame identifier"); sources.any { it !in frames } -> diagnostics += unsupported(statement, "Event Modeling relation source must be declared first"); sources.any { it == id } -> diagnostics += unsupported(statement, "Event Modeling frame cannot reference itself"); else -> { frames[id] = EventModelingFrame(id, m.groupValues[4], kind, reset); (if (sources.isNotEmpty()) sources else if (reset) emptyList() else inferenceSource?.let { listOf(it) }.orEmpty()).forEach { relations += EventModelingRelation(it, id) }; inferenceSource = id } }
                return@forEach
            }
            diagnostics += unsupported(statement, "Unsupported Event Modeling syntax")
        }
        if (frames.isEmpty()) diagnostics += unsupported(statements.first(), "Event Modeling requires at least one frame")
        return if (diagnostics.isEmpty()) MermaidParseResult.Success(EventModelingDiagram(title, frames.values.toList(), relations.toList())) else MermaidParseResult.Failure(diagnostics)
    }

    private fun parseSwimlane(source: String): MermaidParseResult {
        val lines = source.lineSequence().toList()
        val headerIndex = lines.indexOfFirst { SWIMLANE_HEADER.matches(it.trim()) }
        if (headerIndex < 0) return failure(MermaidDiagnosticCode.INVALID_HEADER, "Invalid swimlane-beta header", SourceLocation(1, 1))
        val header = SWIMLANE_HEADER.matchEntire(lines[headerIndex].trim())!!
        val direction = header.groupValues[1].takeIf { it.isNotEmpty() }
            ?.uppercase()
            ?.let(FlowDirection::valueOf)
            ?: FlowDirection.TB
        val lanes = mutableListOf<Swimlane>()
        val laneIds = mutableSetOf<String>()
        val nodeIds = mutableSetOf<String>()
        val edges = mutableListOf<SwimlaneEdge>()
        val edgeKeys = mutableSetOf<Triple<String, String, String?>>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        var currentLaneId: String? = null
        var currentLaneLabel: String? = null
        var currentNodes = mutableListOf<SwimlaneNode>()

        fun statement(text: String, line: Int, raw: String): SourceStatement = SourceStatement(
            text,
            SourceLocation(line, raw.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0) + 1),
        )

        fun closeLane(line: Int, raw: String) {
            val id = currentLaneId
            if (id == null) {
                diagnostics += unsupported(statement("end", line, raw), "Orphan swimlane end")
            } else {
                if (currentNodes.isEmpty()) diagnostics += unsupported(statement("end", line, raw), "Swimlane must contain at least one node")
                lanes += Swimlane(id, currentLaneLabel ?: id, currentNodes.toList())
                currentLaneId = null
                currentLaneLabel = null
                currentNodes = mutableListOf()
            }
        }

        lines.drop(headerIndex + 1).forEachIndexed { offset, raw ->
            val lineNumber = headerIndex + offset + 2
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("%%")) return@forEachIndexed
            val sourceStatement = statement(line, lineNumber, raw)
            if (line.equals("end", ignoreCase = true)) {
                closeLane(lineNumber, raw)
                return@forEachIndexed
            }
            SWIMLANE_LANE.matchEntire(line)?.let { match ->
                if (currentLaneId != null) {
                    diagnostics += unsupported(sourceStatement, "Nested swimlanes are not supported")
                    return@forEachIndexed
                }
                val id = match.groupValues[1]
                val label = match.groupValues[2].ifEmpty { id }
                if (!laneIds.add(id)) diagnostics += unsupported(sourceStatement, "Duplicate swimlane id")
                currentLaneId = id
                currentLaneLabel = label
                currentNodes = mutableListOf()
                return@forEachIndexed
            }
            if (currentLaneId != null) {
                parseSwimlaneNode(line)?.let { node ->
                    if (!nodeIds.add(node.id)) diagnostics += unsupported(sourceStatement, "Duplicate swimlane node id")
                    else currentNodes += node
                    return@forEachIndexed
                }
                diagnostics += unsupported(sourceStatement, "Unsupported swimlane node syntax")
                return@forEachIndexed
            }
            val segments = splitSwimlaneEdgeChain(line)
            if (segments == null) {
                diagnostics += unsupported(sourceStatement, "Unsupported swimlane syntax")
                return@forEachIndexed
            }
            segments.forEach { edge ->
                val key = Triple(edge.sourceId, edge.targetId, edge.label)
                when {
                    edge.sourceId !in nodeIds || edge.targetId !in nodeIds -> diagnostics += unsupported(sourceStatement, "Swimlane edge endpoints must be declared first")
                    edge.sourceId == edge.targetId -> diagnostics += unsupported(sourceStatement, "Swimlane self edges are not supported")
                    !edgeKeys.add(key) -> diagnostics += unsupported(sourceStatement, "Duplicate swimlane edge")
                    else -> edges += edge
                }
            }
        }
        if (currentLaneId != null) diagnostics += unsupported(
            SourceStatement(currentLaneId!!, SourceLocation(lines.size, 1)),
            "Unclosed swimlane",
        )
        if (lanes.isEmpty()) diagnostics += unsupported(SourceStatement(lines[headerIndex].trim(), SourceLocation(headerIndex + 1, 1)), "Swimlane diagram requires at least one lane")
        return if (diagnostics.isEmpty()) MermaidParseResult.Success(SwimlaneDiagram(direction, lanes, edges)) else MermaidParseResult.Failure(diagnostics)
    }

    private fun parseTreeView(source: String): MermaidParseResult {
        val lines = source.lineSequence().toList()
        val header = lines.indexOfFirst { it.trim().equals("treeView-beta", ignoreCase = true) }
        if (header < 0) return failure(MermaidDiagnosticCode.INVALID_HEADER, "Invalid treeView-beta header", SourceLocation(1, 1))
        val nodes = mutableListOf<TreeViewNode>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        lines.drop(header + 1).forEachIndexed { offset, raw ->
            if (raw.trim().isEmpty() || raw.trim().startsWith("%%")) return@forEachIndexed
            if (raw.contains('\t')) {
                diagnostics += unsupported(SourceStatement(raw.trim(), SourceLocation(header + offset + 2, 1)), "Tabs are not supported in treeView indentation")
                return@forEachIndexed
            }
            val leading = raw.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
            if (leading == 0 || leading % 4 != 0) {
                diagnostics += unsupported(SourceStatement(raw.trim(), SourceLocation(header + offset + 2, leading + 1)), "treeView nodes require positive four-space indentation")
                return@forEachIndexed
            }
            val text = raw.trim()
            val label = when {
                text.length >= 2 && text.first() == '"' && text.last() == '"' -> text.substring(1, text.length - 1).takeIf { it.isNotEmpty() }
                text.matches(Regex("[A-Za-z0-9_./@+\\-]+/?")) -> text
                else -> null
            }
            if (label == null) {
                diagnostics += unsupported(SourceStatement(text, SourceLocation(header + offset + 2, leading + 1)), "Unsupported treeView node syntax")
                return@forEachIndexed
            }
            val depth = leading / 4 - 1
            if (depth > (nodes.maxOfOrNull { it.depth }?.plus(1) ?: 0)) {
                diagnostics += unsupported(SourceStatement(text, SourceLocation(header + offset + 2, leading + 1)), "treeView indentation skips a parent")
                return@forEachIndexed
            }
            val parent = nodes.indexOfLast { it.depth == depth - 1 }
            nodes += TreeViewNode(label.removeSuffix("/"), depth, parent.takeIf { it >= 0 }, label.endsWith('/'))
        }
        if (nodes.isEmpty()) diagnostics += unsupported(SourceStatement("treeView-beta", SourceLocation(header + 1, 1)), "treeView requires at least one node")
        if (diagnostics.isNotEmpty()) return MermaidParseResult.Failure(diagnostics)
        return MermaidParseResult.Success(TreeViewDiagram(nodes))
    }

    /**
     * Bounded railroad-beta slice: one Diagram/ComplexDiagram root over Terminal,
     * NonTerminal, Skip, Start, End, Sequence, Stack, Choice, Optional, OneOrMore,
     * and ZeroOrMore with single-quoted literal labels. Anything else fails closed.
     */
    private fun parseRailroad(source: String): MermaidParseResult {
        var headerEnd = -1
        var headerLineIndex = -1
        var offset = 0
        for ((index, line) in source.lineSequence().withIndex()) {
            if (line.trim().equals("railroad-beta", ignoreCase = true)) {
                headerEnd = offset + line.length
                if (source.getOrNull(headerEnd) == '\r') headerEnd++
                if (source.getOrNull(headerEnd) == '\n') headerEnd++
                headerLineIndex = index
                break
            }
            offset += line.length + 1
        }
        if (headerEnd < 0) {
            return failure(MermaidDiagnosticCode.INVALID_HEADER, "Invalid railroad-beta header", SourceLocation(1, 1))
        }

        val diagnostics = mutableListOf<MermaidDiagnostic>()
        var index = headerEnd
        var line = headerLineIndex + 2
        var column = 1

        fun location() = SourceLocation(line = line, column = column)

        fun fail(message: String) {
            diagnostics += MermaidDiagnostic(
                code = MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
                message = message,
                location = location(),
            )
        }

        fun peek(): Char? = source.getOrNull(index)

        fun advance() {
            when (source[index]) {
                '\n' -> {
                    line++
                    column = 1
                }
                else -> column++
            }
            index++
        }

        fun skipWhitespace() {
            while (peek()?.isWhitespace() == true) advance()
        }

        fun expect(character: Char, description: String): Boolean {
            skipWhitespace()
            if (peek() != character) {
                fail(description)
                return false
            }
            advance()
            return true
        }

        fun parseStringLiteral(): String? {
            advance() // opening quote
            val value = StringBuilder()
            while (true) {
                when (val character = peek()) {
                    null, '\n' -> {
                        fail("Unterminated railroad string literal")
                        return null
                    }
                    '\'' -> {
                        advance()
                        break
                    }
                    else -> {
                        value.append(character)
                        advance()
                    }
                }
            }
            if (value.isEmpty()) {
                fail("Railroad labels must not be empty")
                return null
            }
            return value.toString()
        }

        fun parseInteger(): Int? {
            val startLocation = location()
            val digits = StringBuilder()
            if (peek() == '-') {
                digits.append('-')
                advance()
            }
            while (peek()?.isDigit() == true) {
                digits.append(peek())
                advance()
            }
            val raw = digits.toString()
            if (raw.isEmpty() || raw == "-") {
                fail("Expected a railroad Choice priority number")
                return null
            }
            val parsed = raw.toIntOrNull()
            if (parsed == null || parsed < 0) {
                diagnostics += MermaidDiagnostic(
                    code = MermaidDiagnosticCode.INVALID_VALUE,
                    message = "Railroad Choice priority must be a non-negative integer",
                    location = startLocation,
                )
                return null
            }
            return parsed
        }

        fun parseIdentifier(): String {
            val name = StringBuilder()
            while (peek()?.let { it.isLetterOrDigit() || it == '_' } == true) {
                name.append(peek())
                advance()
            }
            return name.toString()
        }

        fun requireExactlyOneChild(symbol: String, children: List<RailroadNode>): RailroadNode? {
            val child = children.singleOrNull()
            if (child == null) fail("railroad $symbol takes exactly one child")
            return child
        }

        fun parseExpression(): RailroadNode? {
            skipWhitespace()
            when (peek()) {
                null -> {
                    fail("Unexpected end of railroad expression")
                    return null
                }
                '\'' -> return parseStringLiteral()?.let { RailroadTerminal(it) }
                else -> if (!(peek() as Char).isLetter() && peek() != '_') {
                    fail("Unsupported railroad syntax")
                    return null
                }
            }
            val identifier = parseIdentifier()
            skipWhitespace()
            when (identifier) {
                "Skip" -> {
                    if (peek() == '(') fail("railroad Skip takes no arguments")
                    return RailroadSkip
                }
                "Start" -> {
                    if (peek() == '(') fail("railroad Start takes no arguments")
                    return RailroadStart
                }
                "End" -> {
                    if (peek() == '(') fail("railroad End takes no arguments")
                    return RailroadEnd
                }
                else -> if (peek() != '(') {
                    fail("Unsupported railroad symbol: $identifier")
                    return null
                }
            }
            advance() // opening parenthesis
            val children = mutableListOf<RailroadNode>()
            var priority: Int? = null
            loop@ while (true) {
                skipWhitespace()
                when (peek()) {
                    ')' -> {
                        advance()
                        break@loop
                    }
                    null -> {
                        fail("Unterminated railroad $identifier call")
                        return null
                    }
                    ',' -> {
                        advance()
                        continue@loop
                    }
                }
                if (identifier == "Choice" && priority == null && children.isEmpty() &&
                    (peek()?.isDigit() == true || peek() == '-')
                ) {
                    val parsedPriority = parseInteger() ?: return null
                    priority = parsedPriority
                    continue@loop
                }
                if ((identifier == "Terminal" || identifier == "NonTerminal") && children.isEmpty()) {
                    skipWhitespace()
                    if (peek() != '\'') {
                        fail("railroad $identifier takes exactly one quoted label")
                        return null
                    }
                    val label = parseStringLiteral() ?: return null
                    children += if (identifier == "Terminal") RailroadTerminal(label) else RailroadNonTerminal(label)
                    continue@loop
                }
                val child = parseExpression() ?: return null
                children += child
            }
            return when (identifier) {
                "Sequence" ->
                    if (children.isEmpty()) {
                        fail("railroad Sequence requires at least one child")
                        null
                    } else RailroadSequence(children.toList())
                "Stack" ->
                    if (children.isEmpty()) {
                        fail("railroad Stack requires at least one child")
                        null
                    } else RailroadStack(children.toList())
                "Choice" ->
                    when {
                        priority == null -> {
                            fail("railroad Choice requires a non-negative priority first argument")
                            null
                        }
                        children.isEmpty() -> {
                            fail("railroad Choice requires at least one branch")
                            null
                        }
                        else -> RailroadChoice(priority ?: 0, children.toList())
                    }
                "Optional" -> requireExactlyOneChild(identifier, children)?.let { RailroadOptional(it) }
                "OneOrMore" -> requireExactlyOneChild(identifier, children)?.let { RailroadOneOrMore(it) }
                "ZeroOrMore" -> requireExactlyOneChild(identifier, children)?.let { RailroadZeroOrMore(it) }
                "Terminal", "NonTerminal" ->
                    requireExactlyOneChild(identifier, children)
                else -> {
                    fail("Unsupported railroad symbol: $identifier")
                    null
                }
            }
        }

        skipWhitespace()
        val rootName = parseIdentifier()
        if (rootName != "Diagram" && rootName != "ComplexDiagram") {
            fail("railroad-beta requires a single top-level Diagram or ComplexDiagram expression")
            return MermaidParseResult.Failure(diagnostics.toList())
        }
        if (!expect('(', "railroad $rootName requires parentheses")) {
            return MermaidParseResult.Failure(diagnostics.toList())
        }
        val root = parseExpression() ?: return MermaidParseResult.Failure(diagnostics.toList())
        if (!expect(')', "railroad $rootName must be closed")) {
            return MermaidParseResult.Failure(diagnostics.toList())
        }
        skipWhitespace()
        if (peek() != null) {
            fail("Unexpected content after the railroad diagram")
            return MermaidParseResult.Failure(diagnostics.toList())
        }
        if (diagnostics.isNotEmpty()) return MermaidParseResult.Failure(diagnostics.toList())
        return MermaidParseResult.Success(RailroadDiagram(root))
    }

    private fun parseSwimlaneNode(line: String): SwimlaneNode? {
        val patterns = listOf(
            SWIMLANE_STADIUM_NODE to SwimlaneNodeShape.STADIUM,
            SWIMLANE_CIRCLE_NODE to SwimlaneNodeShape.CIRCLE,
            SWIMLANE_DECISION_NODE to SwimlaneNodeShape.DECISION,
            SWIMLANE_RECT_NODE to SwimlaneNodeShape.RECTANGLE,
            SWIMLANE_ROUNDED_NODE to SwimlaneNodeShape.ROUNDED,
        )
        patterns.forEach { (pattern, shape) ->
            pattern.matchEntire(line)?.let { return SwimlaneNode(it.groupValues[1], it.groupValues[2], shape) }
        }
        return null
    }

    private fun splitSwimlaneEdgeChain(line: String): List<SwimlaneEdge>? {
        val first = Regex("^($IDENTIFIER)").find(line) ?: return null
        var sourceId = first.groupValues[1]
        var cursor = first.range.last + 1
        val edges = mutableListOf<SwimlaneEdge>()
        while (cursor < line.length) {
            val match = SWIMLANE_EDGE_TAIL.find(line, cursor) ?: return null
            if (match.range.first != cursor) return null
            val targetId = match.groupValues[2]
            edges += SwimlaneEdge(sourceId, targetId, match.groupValues[1].ifEmpty { null })
            sourceId = targetId
            cursor = match.range.last + 1
        }
        return edges.takeIf { it.isNotEmpty() }
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
    private val SWIMLANE_HEADER = Regex("^swimlane-beta(?:\\s+(TD|TB|LR|BT|RL))?$", RegexOption.IGNORE_CASE)
    private val SWIMLANE_LANE = Regex("^subgraph\\s+($IDENTIFIER)(?:\\s+\\[([^]\\r\\n]+)])?$", RegexOption.IGNORE_CASE)
    private val SWIMLANE_RECT_NODE = Regex("^($IDENTIFIER)\\[([^]\\r\\n]+)]$")
    private val SWIMLANE_ROUNDED_NODE = Regex("^($IDENTIFIER)\\(([^()\\r\\n]+)\\)$")
    private val SWIMLANE_STADIUM_NODE = Regex("^($IDENTIFIER)\\(\\[([^]\\r\\n]+)]\\)$")
    private val SWIMLANE_DECISION_NODE = Regex("^($IDENTIFIER)\\{([^}\\r\\n]+)}$")
    private val SWIMLANE_CIRCLE_NODE = Regex("^($IDENTIFIER)\\(\\(([^()\\r\\n]+)\\)\\)$")
    private val SWIMLANE_EDGE_TAIL = Regex("\\s+-->\\s*(?:\\|([^|\\r\\n]+)\\|\\s*)?($IDENTIFIER)")
    private val STATE_DIRECTION = Regex("^direction\\s+(TB|TD|LR|BT|RL)$", RegexOption.IGNORE_CASE)
    private val STATE_ALIAS = Regex("^state\\s+\"([^\"]+)\"\\s+as\\s+($IDENTIFIER)$")
    private val STATE_TRANSITION = Regex(
        "^(\\[\\*\\]|$IDENTIFIER)\\s*-->\\s*(\\[\\*\\]|$IDENTIFIER)(?:\\s*:\\s*(.*))?$",
    )
    private val FLOW_NODE = Regex("^($IDENTIFIER)(?:\\[([^]\\r\\n]+)])?$")
    private val FLOW_EDGE = Regex(
        "^($IDENTIFIER)(?:\\[([^]\\r\\n]+)])?\\s*(-->|==>)\\s*" +
            "($IDENTIFIER)(?:\\[([^]\\r\\n]+)])?$",
    )
    private val ZENUML_TITLE = Regex("^title\\s+(\\S.*)$")
    private val ZENUML_ALIAS_DECLARATION = Regex("^($IDENTIFIER)\\s+as\\s+(\\S.*)$")
    private val ZENUML_BARE_DECLARATION = Regex("^($IDENTIFIER)$")
    private val ZENUML_SYNC_MESSAGE = Regex("^($IDENTIFIER)\\s*->\\s*($IDENTIFIER)\\.([A-Za-z_][A-Za-z0-9_]*)(?:\\(\\))?$")
    private val ZENUML_ASYNC_MESSAGE = Regex("^($IDENTIFIER)\\s*->\\s*($IDENTIFIER)\\s*:\\s*(\\S.*)$")
    private val WARDLEY_TITLE = Regex("^title\\s+(\\S.*)$")
    private val WARDLEY_COORDINATE = Regex("^[01](?:\\.\\d+)?$")
    private val RADAR_TITLE = Regex("^title\\s+(\\S.*)$", RegexOption.IGNORE_CASE)
    private val RADAR_MAX = Regex("^max\\s+(-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?)$", RegexOption.IGNORE_CASE)
    private const val RADAR_AXIS_KEYWORD = "axis"
    private const val RADAR_CURVE_KEYWORD = "curve"
    private val RADAR_AXIS_ENTRY = Regex("^([A-Za-z_][A-Za-z0-9_]*)(?:\\[\"([^\"\\\\\\r\\n]+)\"])?$")
    private val RADAR_CURVE = Regex("^([A-Za-z_][A-Za-z0-9_]*)(?:\\[\"([^\"\\\\\\r\\n]+)\"])?\\s*\\{([^{}]*)}$")
    private val RADAR_VALUE = Regex("^(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?$")
    private const val WARDLEY_ANCHOR_KEYWORD = "anchor "
    private const val WARDLEY_COMPONENT_KEYWORD = "component "
    private const val WARDLEY_EVOLVE_KEYWORD = "evolve "
    private const val WARDLEY_NOTE_KEYWORD = "note \""
    private const val WARDLEY_LINK_SEPARATOR = " -> "
    private val SEQUENCE_MESSAGE = Regex(
        // The lazy IDs are intentional: an ID may contain '-' while '-->>'
        // starts with the same character. The arrow must win at the boundary.
        "^($IDENTIFIER?)\\s*(->>|-->>)\\s*($IDENTIFIER?)(?:\\s*:\\s*(.*))?$",
    )
    private val PIE_SECTION = Regex("^([\\\"'](?:[^\\\"']|\\\\.)*[\\\"'])\\s*:\\s*(-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?)$")
    private val CLASS_NAMESPACE = Regex("^namespace\\s+($IDENTIFIER)\\s*\\{$", RegexOption.IGNORE_CASE)
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
    private val NUMBER = "-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?"
    private val XY_HEADER = Regex("^xychart-beta$", RegexOption.IGNORE_CASE)
    private val XY_TITLE = Regex("^title\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val XY_X_AXIS = Regex("^x-axis(?:\\s+\"([^\"]+)\")?\\s+\\[([^]]+)]$", RegexOption.IGNORE_CASE)
    private val XY_Y_AXIS = Regex("^y-axis(?:\\s+\"([^\"]+)\")?\\s+($NUMBER)\\s*-->\\s*($NUMBER)$", RegexOption.IGNORE_CASE)
    private val XY_SERIES = Regex("^(line|bar)\\s+\\[([^]]+)]$", RegexOption.IGNORE_CASE)
    private const val MINDMAP_INDENT = 2
    private val GANTT_TASK = Regex("^(.+?)\\s*:\\s*([^,]*),\\s*($IDENTIFIER),\\s*(\\d{4}-\\d{2}-\\d{2}),\\s*((?:\\d{4}-\\d{2}-\\d{2})|(?:\\d+)d)$", RegexOption.IGNORE_CASE)
    private val GANTT_TASK_NO_STATUS = Regex("^(.+?)\\s*:\\s*($IDENTIFIER),\\s*(\\d{4}-\\d{2}-\\d{2}),\\s*((?:\\d{4}-\\d{2}-\\d{2})|(?:\\d+)d)$", RegexOption.IGNORE_CASE)
    private val GANTT_STATUS = mapOf("done" to GanttTaskStatus.DONE, "active" to GanttTaskStatus.ACTIVE, "crit" to GanttTaskStatus.CRITICAL)
    private val QUADRANT_TITLE = Regex("^title\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val QUADRANT_AXIS = Regex("^(x|y)-axis\\s+(.+?)\\s*-->\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val QUADRANT_LABEL = Regex("^quadrant-([1-4])\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val QUADRANT_POINT = Regex("^(.+?)\\s*:\\s*\\[($NUMBER)\\s*,\\s*($NUMBER)]$")
    private val USER_JOURNEY_TASK = Regex("^(.+?)\\s*:\\s*([1-5])\\s*:\\s*(.+)$")
    private val GIT_COMMIT = Regex(
        "^commit(?:\\s+id\\s*:\\s*\"([^\"]+)\")?(?:\\s+type\\s*:\\s*(NORMAL|REVERSE|HIGHLIGHT))?(?:\\s+tag\\s*:\\s*\"([^\"]+)\")?$",
        RegexOption.IGNORE_CASE,
    )
    private val GIT_BRANCH = Regex("^branch\\s+($IDENTIFIER)$", RegexOption.IGNORE_CASE)
    private val GIT_CHECKOUT = Regex("^(checkout|switch)\\s+($IDENTIFIER)$", RegexOption.IGNORE_CASE)
    private val GIT_MERGE = Regex(
        "^merge\\s+($IDENTIFIER)(?:\\s+id\\s*:\\s*\"([^\"]+)\")?(?:\\s+type\\s*:\\s*(NORMAL|REVERSE|HIGHLIGHT))?(?:\\s+tag\\s*:\\s*\"([^\"]+)\")?$",
        RegexOption.IGNORE_CASE,
    )
    private val KANBAN_ITEM = Regex("^($IDENTIFIER)\\[([^]\\r\\n]+)]$")
    private val BLOCK_COLUMNS = Regex("^columns\\s+([0-9]+)$", RegexOption.IGNORE_CASE)
    private val BLOCK_NODE = Regex("^($IDENTIFIER)(?:\\[([^]\\r\\n]+)])?(?::([1-9][0-9]*))?$")
    private val BLOCK_EDGE = Regex("^($IDENTIFIER)\\s*-->\\s*($IDENTIFIER)$")
    private val TREEMAP_NODE = Regex("^\"([^\"\\r\\n]+)\"(?:\\s*:\\s*(\\S+))?$")
    private val VENN_IDENTIFIER = Regex("(?:[A-Za-z_][A-Za-z0-9_-]*|\"[^\"\\r\\n]+\")")
    private val VENN_TITLE = Regex("^title\\s+\"([^\"\\r\\n]+)\"$")
    private val VENN_SET = Regex("^set\\s+($VENN_IDENTIFIER)(?:\\[\"([^\"\\r\\n]+)\"])?(?:\\s*:\\s*(\\S+))?$")
    private val VENN_UNION = Regex(
        "^union\\s+($VENN_IDENTIFIER(?:\\s*,\\s*$VENN_IDENTIFIER){1,2})(?:\\[\"([^\"\\r\\n]+)\"])?(?:\\s*:\\s*(\\S+))?$",
    )
    private val USECASE_DIRECTION = Regex("^direction\\s+(TD|TB|LR|RL)$", RegexOption.IGNORE_CASE)
    private const val USECASE_IDENTIFIER = "[A-Za-z0-9_]+"
    private val USECASE_ACTOR = Regex("^actor\\s+($USECASE_IDENTIFIER)(?:\\(\"([^\"\\r\\n]+)\"\\))?$")
    private val USECASE_ELLIPSE = Regex("^($USECASE_IDENTIFIER)\\(\"([^\"\\r\\n]+)\"\\)$")
    private val USECASE_RECTANGLE = Regex("^($USECASE_IDENTIFIER)\\[([^]\\r\\n]+)]$")
    private val USECASE_EDGE = Regex("^($USECASE_IDENTIFIER)(?:\\s+--\\s+\"([^\"\\r\\n]+)\"\\s+-->|\\s+-->)\\s+($USECASE_IDENTIFIER)$")
    private const val ARCHITECTURE_IDENTIFIER = "[A-Za-z0-9_]+"
    private const val ARCHITECTURE_ICON = "[A-Za-z0-9_-]+"
    private val ARCHITECTURE_GROUP = Regex("^group\\s+($ARCHITECTURE_IDENTIFIER)\\(($ARCHITECTURE_ICON)\\)\\[([^]\\r\\n]+)]$")
    private val ARCHITECTURE_SERVICE = Regex("^service\\s+($ARCHITECTURE_IDENTIFIER)\\(($ARCHITECTURE_ICON)\\)\\[([^]\\r\\n]+)](?:\\s+in\\s+($ARCHITECTURE_IDENTIFIER))?$")
    private val ARCHITECTURE_EDGE = Regex("^($ARCHITECTURE_IDENTIFIER):(T|B|L|R)\\s+(-->|--)\\s+(T|B|L|R):($ARCHITECTURE_IDENTIFIER)$")
    private const val C4_IDENTIFIER = "[A-Za-z0-9_]+"
    private val C4_TITLE = Regex("^title\\s+(.+)$")
    private val C4_ELEMENT = Regex("^(Person|Person_Ext|System|System_Ext)\\(($C4_IDENTIFIER),\\s*\"([^\"\\r\\n]+)\"(?:,\\s*\"([^\"\\r\\n]+)\")?\\)$")
    private val C4_RELATIONSHIP = Regex("^(Rel|BiRel)\\(($C4_IDENTIFIER),\\s*($C4_IDENTIFIER),\\s*\"([^\"\\r\\n]+)\"(?:,\\s*\"([^\"\\r\\n]+)\")?\\)$")
}

private fun String.toArchitecturePort(): ArchitecturePort = when (this) {
    "T" -> ArchitecturePort.TOP
    "B" -> ArchitecturePort.BOTTOM
    "L" -> ArchitecturePort.LEFT
    else -> ArchitecturePort.RIGHT
}

private val INVALID_VENN_SIZE: Double = Double.NEGATIVE_INFINITY

private fun String.unquoteVennId(): String = if (startsWith('"') && endsWith('"')) substring(1, lastIndex) else this

private fun String.parseVennMembers(): List<String> {
    val members = mutableListOf<String>()
    var quoted = false
    var start = 0
    forEachIndexed { index, character ->
        when (character) {
            '"' -> quoted = !quoted
            ',' -> if (!quoted) {
                members += substring(start, index).trim()
                start = index + 1
            }
        }
    }
    members += substring(start).trim()
    return members
}

private fun String.parseVennSize(
    statement: SourceStatement,
    diagnostics: MutableList<MermaidDiagnostic>,
): Double? {
    if (isEmpty()) return null
    val parsed = toDoubleOrNull()
    if (parsed == null || !parsed.isFinite() || parsed <= 0.0) {
        diagnostics += MermaidDiagnostic(
            MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
            "Venn sizes must be finite and positive: ${statement.text}",
            statement.location,
        )
        return INVALID_VENN_SIZE
    }
    return parsed
}

private fun String.parseSankeyCsvLine(): List<String>? {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var quoted = false
    var closedQuote = false
    var index = 0
    while (index < length) {
        val char = this[index]
        if (quoted) {
            if (char == '"') {
                if (index + 1 < length && this[index + 1] == '"') {
                    current.append('"')
                    index += 1
                } else {
                    quoted = false
                    closedQuote = true
                }
            } else current.append(char)
        } else if (closedQuote) {
            if (char != ',') return null
            fields += current.toString()
            current.clear()
            closedQuote = false
        } else {
            when (char) {
                ',' -> {
                    fields += current.toString()
                    current.clear()
                }
                '"' -> if (current.isEmpty()) quoted = true else return null
                else -> current.append(char)
            }
        }
        index += 1
    }
    if (quoted) return null
    fields += current.toString()
    return fields
}

private fun sankeyHasCycle(nodeIds: Set<String>, links: List<SankeyLink>): Boolean {
    val indegree = nodeIds.associateWith { 0 }.toMutableMap()
    val outgoing = nodeIds.associateWith { mutableListOf<String>() }
    links.forEach { link ->
        indegree[link.targetId] = indegree.getValue(link.targetId) + 1
        outgoing.getValue(link.sourceId) += link.targetId
    }
    val queue = ArrayDeque(indegree.filterValues { it == 0 }.keys)
    var visited = 0
    while (queue.isNotEmpty()) {
        val node = queue.removeFirst()
        visited += 1
        outgoing.getValue(node).forEach { target ->
            val next = indegree.getValue(target) - 1
            indegree[target] = next
            if (next == 0) queue.addLast(target)
        }
    }
    return visited != nodeIds.size
}

private data class MutableTreemapNode(
    val label: String,
    val value: Double?,
    val children: MutableList<MutableTreemapNode> = mutableListOf(),
    val location: SourceLocation,
) {
    fun freeze(): TreemapNode = TreemapNode(label, value, children.map(MutableTreemapNode::freeze))
}

private fun parseIsoDay(value: String): Int? {
    val m = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").matchEntire(value) ?: return null
    val y = m.groupValues[1].toInt(); val mo = m.groupValues[2].toInt(); val d = m.groupValues[3].toInt()
    fun leap(year: Int) = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    if (mo !in 1..12) return null
    val md = intArrayOf(31, if (leap(y)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    if (d !in 1..md[mo - 1]) return null
    return (0 until y).fold(0) { total, year -> total + if (leap(year)) 366 else 365 } + md.take(mo - 1).sum() + d - 1
}

private fun String.csvTokens(): List<String> = split(',').map { it.trim().unquote() }

private fun String.unquote(): String =
    if (length >= 2 && ((first() == '"' && last() == '"') || (first() == '\'' && last() == '\''))) {
        substring(1, lastIndex)
    } else this

private data class SourceStatement(
    val text: String,
    val location: SourceLocation,
)

private data class MindmapSourceLine(
    val text: String,
    val indent: Int,
    val hasTab: Boolean,
    val location: SourceLocation,
)

private data class ParsedMindmapNode(
    val id: String,
    val label: String,
    val shape: MindmapNodeShape,
    val explicitId: Boolean,
)

private sealed interface RequirementBlock {
    val name: String
    val fields: MutableMap<String, String>

    data class Requirement(
        override val name: String,
        override val fields: MutableMap<String, String> = linkedMapOf(),
    ) : RequirementBlock

    data class Element(
        override val name: String,
        override val fields: MutableMap<String, String> = linkedMapOf(),
    ) : RequirementBlock
}

private val REQUIREMENT_START = Regex("^requirement\\s+([A-Za-z_][A-Za-z0-9_-]*)\\s*\\{$", RegexOption.IGNORE_CASE)
private val ELEMENT_START = Regex("^element\\s+([A-Za-z_][A-Za-z0-9_-]*)\\s*\\{$", RegexOption.IGNORE_CASE)
private val REQUIREMENT_FIELD = Regex("^([A-Za-z]+)\\s*:\\s*(\\S(?:.*\\S)?)$")
private val REQUIREMENT_RELATION = Regex(
    "^([A-Za-z_][A-Za-z0-9_-]*)\\s+-\\s+(contains|satisfies|verifies)\\s+->\\s+([A-Za-z_][A-Za-z0-9_-]*)$",
    RegexOption.IGNORE_CASE,
)
private val REQUIREMENT_FIELDS = setOf("id", "text", "risk", "verifymethod")
private val ELEMENT_FIELDS = setOf("type", "docref")

private fun String.toMindmapNodeSyntax(index: Int): ParsedMindmapNode? {
    MINDMAP_DOUBLE_CIRCLE.matchEntire(this@toMindmapNodeSyntax)?.let { match ->
        return ParsedMindmapNode(
            id = match.groupValues[1],
            label = match.groupValues[2].trim(),
            shape = MindmapNodeShape.DOUBLE_CIRCLE,
            explicitId = true,
        ).takeIf { it.label.isNotEmpty() }
    }
    MINDMAP_RECTANGLE.matchEntire(this@toMindmapNodeSyntax)?.let { match ->
        return ParsedMindmapNode(
            id = match.groupValues[1],
            label = match.groupValues[2].trim(),
            shape = MindmapNodeShape.RECTANGLE,
            explicitId = true,
        ).takeIf { it.label.isNotEmpty() }
    }
    MINDMAP_ANONYMOUS_DOUBLE_CIRCLE.matchEntire(this@toMindmapNodeSyntax)?.let { match ->
        return ParsedMindmapNode(
            id = "__mindmap_$index",
            label = match.groupValues[1].trim(),
            shape = MindmapNodeShape.DOUBLE_CIRCLE,
            explicitId = false,
        ).takeIf { it.label.isNotEmpty() }
    }
    MINDMAP_ANONYMOUS_RECTANGLE.matchEntire(this@toMindmapNodeSyntax)?.let { match ->
        return ParsedMindmapNode(
            id = "__mindmap_$index",
            label = match.groupValues[1].trim(),
            shape = MindmapNodeShape.RECTANGLE,
            explicitId = false,
        ).takeIf { it.label.isNotEmpty() }
    }
    val label = trim()
    if (label.isEmpty() || label.startsWith("::") || label.any { it in "[](){}" }) return null
    return ParsedMindmapNode(
        id = "__mindmap_$index",
        label = label,
        shape = MindmapNodeShape.DEFAULT,
        explicitId = false,
    )
}

private val MINDMAP_DOUBLE_CIRCLE = Regex("^([A-Za-z_][A-Za-z0-9_-]*)\\(\\(([^()\\r\\n]+)\\)\\)$")
private val MINDMAP_RECTANGLE = Regex("^([A-Za-z_][A-Za-z0-9_-]*)\\[([^]\\r\\n]+)]$")
private val MINDMAP_ANONYMOUS_DOUBLE_CIRCLE = Regex("^\\(\\(([^()\\r\\n]+)\\)\\)$")
private val MINDMAP_ANONYMOUS_RECTANGLE = Regex("^\\[([^]\\r\\n]+)]$")
private val PACKET_TITLE = Regex("^title\\s+(.+)$", RegexOption.IGNORE_CASE)
private val PACKET_FIELD = Regex("^(\\d+)(?:-(\\d+))?\\s*:\\s*\"([^\"\\r\\n]+)\"$")
private val EVENT_MODELING_TITLE = Regex("^title\\s+(.+)$", RegexOption.IGNORE_CASE)
private val EVENT_MODELING_FRAME = Regex("^(tf|timeframe|rf|resetframe)\\s+(\\d{1,3})\\s+(ui|cmd|command|evt|event|pcr|processor|rmo|readmodel)\\s+([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*)(?:\\s+->>\\s+(\\d{1,3}(?:\\s+->>\\s+\\d{1,3})*))?$", RegexOption.IGNORE_CASE)
private const val PACKET_MAX_BIT: Int = 4095

private fun String.toMindmapLines(): List<MindmapSourceLine> = buildList {
    lineSequence().forEachIndexed { index, rawLine ->
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("%%")) return@forEachIndexed
        val leading = rawLine.takeWhile { it == ' ' || it == '\t' }
        add(
            MindmapSourceLine(
                text = rawLine.drop(leading.length).trimEnd(),
                indent = leading.count { it == ' ' },
                hasTab = '\t' in leading,
                location = SourceLocation(index + 1, leading.length + 1),
            ),
        )
    }
}

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
