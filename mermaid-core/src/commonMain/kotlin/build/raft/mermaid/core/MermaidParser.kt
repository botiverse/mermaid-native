package build.raft.mermaid.core

/**
 * Parser for the explicitly declared Phase 0 Mermaid-compatible subset.
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

            diagnostics += unsupported(statement, "Unsupported Phase 0 flowchart syntax")
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
                diagnostics += unsupported(statement, "Unsupported Phase 0 sequence syntax")
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
