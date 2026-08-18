package build.raft.mermaid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MermaidParserTest {
    @Test
    fun parsesMinimalFlowchartAndPreservesNodeOrder() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                """
                flowchart LR
                  A[Start] --> B[Finish]
                """.trimIndent(),
            ),
        )

        assertEquals(
            FlowchartDiagram(
                direction = FlowDirection.LR,
                nodes = listOf(FlowNode("A", "Start"), FlowNode("B", "Finish")),
                edges = listOf(FlowEdge("A", "B")),
            ),
            result.diagram,
        )
    }

    @Test
    fun parsesGraphAliasAndSemicolonSeparatedStatements() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse("graph TD; A; A-->B"),
        )
        val diagram = assertIs<FlowchartDiagram>(result.diagram)

        assertEquals(FlowDirection.TD, diagram.direction)
        assertEquals(listOf(FlowNode("A", "A"), FlowNode("B", "B")), diagram.nodes)
        assertEquals(listOf(FlowEdge("A", "B")), diagram.edges)
    }

    @Test
    fun parsesMinimalSequenceAndAutoRegistersActors() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                """
                sequenceDiagram
                  Alice->>Bob: Hello
                  Bob-->>Alice: Ack
                """.trimIndent(),
            ),
        )

        assertEquals(
            SequenceDiagram(
                actors = listOf(
                    SequenceActor("Alice", "Alice"),
                    SequenceActor("Bob", "Bob"),
                ),
                messages = listOf(
                    SequenceMessage(
                        from = "Alice",
                        to = "Bob",
                        label = "Hello",
                        lineStyle = SequenceLineStyle.SOLID,
                        arrowHead = SequenceArrowHead.FILLED,
                    ),
                    SequenceMessage(
                        from = "Bob",
                        to = "Alice",
                        label = "Ack",
                        lineStyle = SequenceLineStyle.DASHED,
                        arrowHead = SequenceArrowHead.FILLED,
                    ),
                ),
            ),
            result.diagram,
        )
    }

    @Test
    fun acceptsLabelLessPhaseZeroSequenceMessage() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse("sequenceDiagram; A->>B"),
        )
        val diagram = assertIs<SequenceDiagram>(result.diagram)

        assertEquals("", diagram.messages.single().label)
    }

    @Test
    fun sequenceArrowBoundaryDoesNotConsumeHyphenatedActorId() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse("sequenceDiagram; api-v1->>worker_2: call"),
        )
        val diagram = assertIs<SequenceDiagram>(result.diagram)

        assertEquals(listOf("api-v1", "worker_2"), diagram.actors.map { it.id })
        assertEquals("api-v1", diagram.messages.single().from)
        assertEquals("worker_2", diagram.messages.single().to)
    }

    @Test
    fun ignoresBlankLinesAndFullLineComments() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse("sequenceDiagram\n\n %% comment\nA->>B: hi"),
        )

        assertEquals(1, assertIs<SequenceDiagram>(result.diagram).messages.size)
    }

    @Test
    fun emptySourceFailsClosed() {
        val failure = assertIs<MermaidParseResult.Failure>(MermaidParser.parse(" \n %% only"))

        assertEquals(MermaidDiagnosticCode.EMPTY_SOURCE, failure.diagnostics.single().code)
        assertEquals(SourceLocation(1, 1), failure.diagnostics.single().location)
    }

    @Test
    fun unsupportedDiagramDoesNotFallback() {
        val failure = assertIs<MermaidParseResult.Failure>(
            MermaidParser.parse("classDiagram\nA <|-- B"),
        )

        assertEquals(MermaidDiagnosticCode.UNSUPPORTED_DIAGRAM, failure.diagnostics.single().code)
    }

    @Test
    fun malformedFlowchartHeaderHasTypedDiagnostic() {
        val failure = assertIs<MermaidParseResult.Failure>(MermaidParser.parse("flowchart SIDEWAYS"))

        assertEquals(MermaidDiagnosticCode.INVALID_HEADER, failure.diagnostics.single().code)
    }

    @Test
    fun unsupportedBodySyntaxFailsWithoutPartialSuccess() {
        val failure = assertIs<MermaidParseResult.Failure>(
            MermaidParser.parse("flowchart TD\nA-->B\nsubgraph unsupported"),
        )

        assertEquals(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, failure.diagnostics.single().code)
        assertEquals(SourceLocation(line = 3, column = 1), failure.diagnostics.single().location)
    }

    @Test
    fun semicolonDiagnosticReportsPhysicalColumn() {
        val failure = assertIs<MermaidParseResult.Failure>(
            MermaidParser.parse("flowchart TD; A-->B; click A callback"),
        )

        assertEquals(SourceLocation(line = 1, column = 22), failure.diagnostics.single().location)
    }
}
