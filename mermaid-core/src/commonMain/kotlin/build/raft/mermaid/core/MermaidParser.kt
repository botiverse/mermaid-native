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
            header.text.equals("sequenceDiagram", ignoreCase = true) -> SequenceParser(source).parse()
            STATE_HEADER.matches(header.text) -> StateParser(source).parse()
            header.text.startsWith("pie", ignoreCase = true) -> PieParser(source).parse()
            (header.text.equals("classDiagram", ignoreCase = true) || header.text.equals("classDiagram-v2", ignoreCase = true)) -> ClassParser(source).parse()
            header.text.takeWhile { !it.isWhitespace() }.equals("erDiagram", ignoreCase = true) -> EntityRelationshipParser(source).parse()
            XY_HEADER.matches(header.text) -> XyParser(source).parse()
            header.text.equals("mindmap", ignoreCase = true) -> parseMindmap(source)
            header.text.equals("gantt", ignoreCase = true) -> GanttParser(source).parse()
            header.text.takeWhile { !it.isWhitespace() }.equals("timeline", ignoreCase = true) -> TimelineParser(source).parse()
            header.text.takeWhile { !it.isWhitespace() }.equals("quadrantChart", ignoreCase = true) -> QuadrantParser(source).parse()
            header.text.equals("journey", ignoreCase = true) -> JourneyParser(source).parse()
            header.text.startsWith("gitGraph") -> GitGraphParser(source).parse()
            header.text.equals("requirementDiagram", ignoreCase = true) -> RequirementParser(source).parse()
            header.text.equals("kanban", ignoreCase = true) -> parseKanban(source)
            header.text.equals("packet", ignoreCase = true) || header.text.equals("packet-beta", ignoreCase = true) -> PacketParser(source).parse()
            header.text.equals("block", ignoreCase = true) || header.text.equals("block-beta", ignoreCase = true) -> parseBlock(statements)
            header.text.equals("sankey", ignoreCase = true) || header.text.equals("sankey-beta", ignoreCase = true) -> parseSankey(source)
            header.text.equals("treemap-beta", ignoreCase = true) -> parseTreemap(source)
            header.text.equals("venn-beta", ignoreCase = true) -> parseVenn(source)
            header.text.equals("usecase-beta", ignoreCase = true) || header.text.equals("usecaseDiagram", ignoreCase = true) -> parseUsecase(source)
            header.text.equals("architecture-beta", ignoreCase = true) -> parseArchitecture(source)
            header.text in setOf("C4Context", "C4Container", "C4Component", "C4Dynamic", "C4Deployment") -> parseC4Context(source)
            header.text.equals("cynefin-beta", ignoreCase = true) || header.text.equals("cynefin", ignoreCase = true) -> parseCynefin(source)
            header.text.equals("ishikawa", ignoreCase = true) || header.text.equals("ishikawa-beta", ignoreCase = true) || header.text.equals("fishbone", ignoreCase = true) -> parseIshikawa(source)
            SWIMLANE_HEADER.matches(header.text) -> parseSwimlaneFlow(source)
            header.text.equals("treeView-beta", ignoreCase = true) -> parseTreeView(source)
            header.text.equals("railroad-beta", ignoreCase = true) -> parseRailroad(source)
            header.text.equals("zenuml", ignoreCase = true) -> parseZenuml(statements)
            header.text.equals("wardley-beta", ignoreCase = true) -> parseWardley(statements)
            header.text.startsWith("radar-beta") -> RadarParser(source).parse()
            header.text == "eventmodeling" -> parseEventModeling(source)
            header.text.startsWith("swimlane-beta", ignoreCase = true) -> failure(
                MermaidDiagnosticCode.INVALID_HEADER,
                "Expected swimlane-beta optionally followed by TD, TB, LR, BT, or RL",
                header.location,
            )
            header.text.takeWhile { !it.isWhitespace() }.lowercase() in setOf("flowchart", "graph", "flowchart-elk") -> FlowParser(source).parse()
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
                val rawLabel = alias.groupValues[2].trim()
                val label = if (rawLabel.startsWith('"') && rawLabel.endsWith('"') && rawLabel.length > 2) {
                    rawLabel.substring(1, rawLabel.lastIndex).takeIf { '"' !in it && '\\' !in it }
                } else {
                    rawLabel.takeIf { value -> value.none { it.isWhitespace() || it == '"' || it == '\\' } }
                }
                if (label == null) {
                    diagnostics += unsupported(statement, "zenuml multi-word aliases require double quotes; escaped aliases are not supported")
                } else {
                    register(alias.groupValues[1], label, statement.location)
                }
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

    private fun parseMindmap(source: String): MermaidParseResult = MindmapParser(source).parse()

    private fun parseKanban(source: String): MermaidParseResult = KanbanParser(source).parse()

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
            nodes.getOrPut(sourceLabel) { SankeyNode(sourceLabel, sourceLabel) }
            nodes.getOrPut(targetLabel) { SankeyNode(targetLabel, targetLabel) }
            links += SankeyLink(sourceLabel, targetLabel, value)
        }
        if (links.isEmpty()) diagnostics += unsupported(lines.first(), "sankey requires at least one link")
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

    private fun parseC4Context(source: String): MermaidParseResult = C4Parser(source).parse()

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
        val frames = linkedMapOf<String, EventModelingFrame>()
        val relations = mutableListOf<EventModelingRelation>()
        val diagnostics = mutableListOf<MermaidDiagnostic>()
        var inferenceSource: String? = null
        statements.drop(1).forEach { statement ->
            EVENT_MODELING_TITLE.matchEntire(statement.text)?.let {
                diagnostics += unsupported(statement, "Event Modeling title is not official mermaid syntax")
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
        return if (diagnostics.isEmpty()) MermaidParseResult.Success(EventModelingDiagram(title = null, frames = frames.values.toList(), relations = relations.toList())) else MermaidParseResult.Failure(diagnostics)
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
     * Bounded official railroad-beta slice: optional line-based `title ...`
     * plus one or more `name = expression;` rules. Expressions are the official
     * lowercase constructors: sequence, choice, optional, oneOrMore, zeroOrMore,
     * terminal, nonterminal, special. JS Diagram()/Stack()/Choice(0, ...) and
     * ABNF/EBNF/PEG dialects fail closed.
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

        fun parseIdentifier(): String? {
            skipWhitespace()
            val first = peek() ?: return null
            if (!first.isLetter() && first != '_') {
                return null
            }
            val value = StringBuilder()
            while (true) {
                val character = peek() ?: break
                if (character.isLetterOrDigit() || character == '_') {
                    value.append(character)
                    advance()
                } else {
                    break
                }
            }
            return value.toString().ifEmpty { null }
        }

        fun parseStringLiteral(): String? {
            skipWhitespace()
            if (peek() != '"') {
                fail("Railroad strings must use double quotes")
                return null
            }
            advance()
            val value = StringBuilder()
            while (true) {
                when (val character = peek()) {
                    null, '\n' -> {
                        fail("Unterminated railroad string literal")
                        return null
                    }
                    '"' -> {
                        advance()
                        break
                    }
                    '\\' -> {
                        advance()
                        when (val escaped = peek()) {
                            null -> {
                                fail("Unterminated railroad string escape")
                                return null
                            }
                            'n' -> value.append('\n')
                            't' -> value.append('\t')
                            '"' -> value.append('"')
                            '\\' -> value.append('\\')
                            else -> {
                                fail("Unsupported railroad string escape")
                                return null
                            }
                        }
                        advance()
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

        fun skipSpaces() {
            while (peek() == ' ' || peek() == '\t') advance()
        }

        fun parseTitleLine(): String? {
            skipSpaces()
            if (peek() == null || peek() == '\n' || peek() == '\r') {
                fail("railroad title must not be empty")
                return null
            }
            val raw = StringBuilder()
            while (true) {
                val character = peek()
                if (character == null || character == '\n' || character == '\r') break
                raw.append(character)
                advance()
            }
            var text = raw.toString().trimEnd()
            if (text.length >= 2 &&
                ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'")))
            ) {
                text = text.substring(1, text.length - 1)
            }
            if (text.isEmpty()) {
                fail("railroad title must not be empty")
                return null
            }
            return text
        }

        fun requireExactlyOneChild(symbol: String, children: List<RailroadNode>): RailroadNode? {
            if (children.size != 1) {
                fail("railroad $symbol takes exactly one argument")
                return null
            }
            return children[0]
        }

        fun parseExpression(): RailroadNode? {
            skipWhitespace()
            val identifier = parseIdentifier()
            if (identifier == null) {
                fail("Expected a railroad expression")
                return null
            }
            if (!expect('(', "railroad $identifier requires parentheses")) {
                return null
            }
            val children = mutableListOf<RailroadNode>()
            var stringArg: String? = null
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
                if ((identifier == "terminal" || identifier == "nonterminal" || identifier == "special") &&
                    children.isEmpty() && stringArg == null
                ) {
                    stringArg = parseStringLiteral() ?: return null
                    continue@loop
                }
                val child = parseExpression() ?: return null
                children += child
            }
            return when (identifier) {
                "sequence" ->
                    if (children.isEmpty()) {
                        fail("railroad sequence requires at least one child")
                        null
                    } else if (children.size == 1) {
                        children[0]
                    } else {
                        RailroadSequence(children.toList())
                    }
                "choice" ->
                    if (children.isEmpty()) {
                        fail("railroad choice requires at least one branch")
                        null
                    } else {
                        RailroadChoice(children.toList())
                    }
                "optional" -> requireExactlyOneChild(identifier, children)?.let { RailroadOptional(it) }
                "oneOrMore" -> requireExactlyOneChild(identifier, children)?.let { RailroadOneOrMore(it) }
                "zeroOrMore" -> requireExactlyOneChild(identifier, children)?.let { RailroadZeroOrMore(it) }
                "terminal" -> {
                    if (stringArg == null || children.isNotEmpty()) {
                        fail("railroad terminal takes exactly one quoted label")
                        null
                    } else {
                        RailroadTerminal(stringArg)
                    }
                }
                "nonterminal" -> {
                    if (stringArg == null || children.isNotEmpty()) {
                        fail("railroad nonterminal takes exactly one quoted label")
                        null
                    } else {
                        RailroadNonTerminal(stringArg)
                    }
                }
                "special" -> {
                    if (stringArg == null || children.isNotEmpty()) {
                        fail("railroad special takes exactly one quoted label")
                        null
                    } else {
                        RailroadSpecial(stringArg)
                    }
                }
                else -> {
                    fail("Unsupported railroad symbol: $identifier")
                    null
                }
            }
        }

        skipWhitespace()
        var title: String? = null
        val rules = mutableListOf<RailroadRule>()
        while (peek() != null) {
            skipWhitespace()
            if (peek() == null) break
            val name = parseIdentifier()
            if (name == null) {
                fail("Expected a railroad rule or title")
                return MermaidParseResult.Failure(diagnostics.toList())
            }
            skipWhitespace()
            if (name == "title") {
                if (peek() == '=') {
                    fail("railroad title must be a line, not a named rule")
                    return MermaidParseResult.Failure(diagnostics.toList())
                }
                if (rules.isNotEmpty()) {
                    fail("railroad title must appear before rules")
                    return MermaidParseResult.Failure(diagnostics.toList())
                }
                if (title != null) {
                    fail("railroad-beta accepts at most one title")
                    return MermaidParseResult.Failure(diagnostics.toList())
                }
                title = parseTitleLine() ?: return MermaidParseResult.Failure(diagnostics.toList())
                continue
            }
            if (!expect('=', "railroad rule '$name' requires '='")) {
                return MermaidParseResult.Failure(diagnostics.toList())
            }
            val definition = parseExpression() ?: return MermaidParseResult.Failure(diagnostics.toList())
            if (!expect(';', "railroad rule '$name' must end with ';'")) {
                return MermaidParseResult.Failure(diagnostics.toList())
            }
            rules += RailroadRule(name, definition)
        }
        if (diagnostics.isNotEmpty()) return MermaidParseResult.Failure(diagnostics.toList())
        if (rules.isEmpty()) {
            return failure(
                MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
                "railroad-beta requires at least one named rule",
                SourceLocation(headerLineIndex + 2, 1),
            )
        }
        return MermaidParseResult.Success(RailroadDiagram(title = title, rules = rules.toList()))
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
    private val STATE_HEADER = Regex("^stateDiagram(?:-v2)?$", RegexOption.IGNORE_CASE)
    private val SWIMLANE_HEADER = Regex("^swimlane-beta(?:\\s+(TD|TB|LR|BT|RL))?$", RegexOption.IGNORE_CASE)
    private val ZENUML_TITLE = Regex("^title\\s+(\\S.*)$")
    private val ZENUML_ALIAS_DECLARATION = Regex("^($IDENTIFIER)\\s+as\\s+(\\S.*)$")
    private val ZENUML_BARE_DECLARATION = Regex("^($IDENTIFIER)$")
    private val ZENUML_SYNC_MESSAGE = Regex("^($IDENTIFIER)\\s*->\\s*($IDENTIFIER)\\.([A-Za-z_][A-Za-z0-9_]*)(?:\\(\\))?$")
    private val ZENUML_ASYNC_MESSAGE = Regex("^($IDENTIFIER)\\s*->\\s*($IDENTIFIER)\\s*:\\s*(\\S.*)$")
    private val WARDLEY_TITLE = Regex("^title\\s+(\\S.*)$")
    private val WARDLEY_COORDINATE = Regex("^[01](?:\\.\\d+)?$")
    private const val WARDLEY_ANCHOR_KEYWORD = "anchor "
    private const val WARDLEY_COMPONENT_KEYWORD = "component "
    private const val WARDLEY_EVOLVE_KEYWORD = "evolve "
    private const val WARDLEY_NOTE_KEYWORD = "note \""
    private const val WARDLEY_LINK_SEPARATOR = " -> "
    private val IDENTIFIER_ONLY = Regex("^$IDENTIFIER$")
    private val QUOTED_TOKEN = Regex("\"([^\"]*)\"")
    private val NUMBER = "-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?"
    private val XY_HEADER = Regex("^xychart(?:-beta)?(?:\\s+.*)?$", RegexOption.IGNORE_CASE)
    private const val MINDMAP_INDENT = 2

    private val KANBAN_ITEM = Regex("^($IDENTIFIER)\\[([^]\\r\\n]+)]$")
    private val BLOCK_COLUMNS = Regex("^columns\\s+([0-9]+)$", RegexOption.IGNORE_CASE)
    private val BLOCK_NODE = Regex("^($IDENTIFIER)(?:\\[\"([^\"\\r\\n]+)\"\\])?(?::([1-9][0-9]*))?$")
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

private data class MutableTreemapNode(
    val label: String,
    val value: Double?,
    val children: MutableList<MutableTreemapNode> = mutableListOf(),
    val location: SourceLocation,
) {
    fun freeze(): TreemapNode = TreemapNode(label, value, children.map(MutableTreemapNode::freeze))
}

internal fun parseIsoDay(value: String): Int? {
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
private val EVENT_MODELING_TITLE = Regex("^title\\s+(.+)$", RegexOption.IGNORE_CASE)
private val EVENT_MODELING_FRAME = Regex("^(tf|timeframe|rf|resetframe)\\s+(\\d{1,3})\\s+(ui|cmd|command|evt|event|pcr|processor|rmo|readmodel)\\s+([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*)(?:\\s+->>\\s+(\\d{1,3}(?:\\s+->>\\s+\\d{1,3})*))?$", RegexOption.IGNORE_CASE)

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
