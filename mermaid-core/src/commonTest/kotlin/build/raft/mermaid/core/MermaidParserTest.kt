package build.raft.mermaid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MermaidParserTest {
    @Test
    fun parsesXyChartAxesAndSeries() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                """
                xychart-beta
                  title "Quarterly sales"
                  x-axis "Quarter" [Q1, Q2, Q3]
                  y-axis "Revenue" 0 --> 100
                  bar [20, 50, 80]
                  line [25, 45, 90]
                """.trimIndent(),
            ),
        )
        assertEquals(
            XyChartDiagram(
                title = "Quarterly sales",
                xAxis = XyAxis("Quarter", listOf("Q1", "Q2", "Q3")),
                yAxis = NumericAxis("Revenue", 0.0, 100.0),
                series = listOf(
                    XySeries(XySeriesKind.BAR, listOf(20.0, 50.0, 80.0)),
                    XySeries(XySeriesKind.LINE, listOf(25.0, 45.0, 90.0)),
                ),
            ),
            result.diagram,
        )
    }

    @Test
    fun malformedXyChartFailsClosed() {
        listOf(
            "xychart-beta\nx-axis [A, B]\ny-axis 0 --> 10\nline [1]",
            "xychart-beta\nx-axis [A]\ny-axis 10 --> 0\nline [1]",
            "xychart-beta\nx-axis [A]\ny-axis 0 --> 10\nline [nope]",
            "xychart-beta\nx-axis [A]\ny-axis 0 --> 10\nline [NaN]",
            "xychart-beta\nx-axis [A]\ny-axis 0 --> 10\nline [Infinity]",
            "xychart-beta\nx-axis [A]\ny-axis 0 --> 10\nline [-Infinity]",
            "xychart-beta\nx-axis [A]\ny-axis 0 --> 10\nline [+Infinity]",
            "xychart-beta\nx-axis [A]\ny-axis 0 --> 10\nline [11]",
            "xychart-beta\nx-axis [A]\ny-axis 0 --> 10",
        ).forEach { source ->
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source)
        }
    }

    @Test
    fun parsesStateDiagramAliasesDirectionAndTerminalTransitions() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                """
                stateDiagram-v2
                  direction LR
                  [*] --> Idle
                  state "Processing request" as Working
                  Idle --> Working: start
                  Working --> [*]: finish
                """.trimIndent(),
            ),
        )

        assertEquals(
            StateDiagram(
                direction = FlowDirection.LR,
                states = listOf(
                    StateNode("__start_0", "", StateNodeKind.START),
                    StateNode("Idle", "Idle"),
                    StateNode("Working", "Processing request"),
                    StateNode("__end_1", "", StateNodeKind.END),
                ),
                transitions = listOf(
                    StateTransition("__start_0", "Idle"),
                    StateTransition("Idle", "Working", "start"),
                    StateTransition("Working", "__end_1", "finish"),
                ),
            ),
            result.diagram,
        )
    }

    @Test
    fun unsupportedStateSyntaxFailsWithoutPartialSuccess() {
        val failure = assertIs<MermaidParseResult.Failure>(
            MermaidParser.parse("stateDiagram-v2\nA --> B\nstate Composite {"),
        )

        assertEquals(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, failure.diagnostics.single().code)
        assertEquals(SourceLocation(line = 3, column = 1), failure.diagnostics.single().location)
    }

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
    fun parsesMindmapHierarchyAndTypedShapes() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                """
                mindmap
                  root((Mindmap))
                    Origins
                      [History]
                    Research
                      ((Native))
                """.trimIndent(),
            ),
        )
        assertEquals(
            MindmapDiagram(
                listOf(
                    MindmapNode("root", "Mindmap", null, 0, MindmapNodeShape.DOUBLE_CIRCLE),
                    MindmapNode("__mindmap_1", "Origins", "root", 1),
                    MindmapNode("__mindmap_2", "History", "__mindmap_1", 2, MindmapNodeShape.RECTANGLE),
                    MindmapNode("__mindmap_3", "Research", "root", 1),
                    MindmapNode("__mindmap_4", "Native", "__mindmap_3", 2, MindmapNodeShape.DOUBLE_CIRCLE),
                ),
            ),
            result.diagram,
        )
    }

    @Test
    fun malformedMindmapIndentationAndMultipleRootsFailClosed() {
        listOf(
            "mindmap\n    root((Root))\n      Child",
            "mindmap\n  root((Root))\n  Other",
            "mindmap\n  root((Root))\n\tChild",
            "mindmap\n  root((Root))\n    Child\n        Grandchild",
            "mindmap\n  root((Root))\n    unsupported { shape",
            "mindmap\n  root((Root))\n    __mindmap_1[Reserved]",
        ).forEach { source ->
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source)
        }
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
    fun parsesClassDiagramMembersAndRelationships() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                """
                classDiagram
                class Animal
                Animal : +String name
                Animal <|-- Duck
                """.trimIndent(),
            ),
        )
        val diagram = assertIs<ClassDiagram>(result.diagram)
        assertEquals(listOf("Animal", "Duck"), diagram.classes.map { it.id })
        assertEquals("String name", diagram.classes.first().members.single().signature)
        assertEquals(ClassRelationshipKind.INHERITANCE, diagram.relationships.single().kind)
    }

    @Test
    fun classMemberVisibilityWithoutSignatureFailsClosed() {
        listOf("+", "-", "#", "~").forEach { marker ->
            val failure = assertIs<MermaidParseResult.Failure>(
                MermaidParser.parse("classDiagram\nA : $marker"),
                marker,
            )
            assertEquals(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, failure.diagnostics.single().code, marker)
        }
    }

    @Test
    fun parsesEntityAttributesKeysAndRelationshipCardinality() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                """
                erDiagram
                  CUSTOMER {
                    int id PK
                    string name
                  }
                  ORDER {
                    int id PK
                    int customerId FK
                  }
                  CUSTOMER ||--o{ ORDER : places
                """.trimIndent(),
            ),
        )
        assertEquals(
            EntityRelationshipDiagram(
                entities = listOf(
                    EntityDefinition(
                        "CUSTOMER",
                        listOf(EntityAttribute("int", "id", EntityKey.PK), EntityAttribute("string", "name")),
                    ),
                    EntityDefinition(
                        "ORDER",
                        listOf(EntityAttribute("int", "id", EntityKey.PK), EntityAttribute("int", "customerId", EntityKey.FK)),
                    ),
                ),
                relationships = listOf(
                    EntityRelationship(
                        "CUSTOMER",
                        "ORDER",
                        EntityCardinality.ONLY_ONE,
                        EntityCardinality.ZERO_OR_MORE,
                        "places",
                    ),
                ),
            ),
            result.diagram,
        )
    }

    @Test
    fun malformedEntityBodyAndRelationshipFailClosed() {
        listOf(
            "erDiagram\nCUSTOMER {\nstring name",
            "erDiagram\nCUSTOMER {\nunknown\n}",
            "erDiagram\nCUSTOMER XX--o{ ORDER : places",
        ).forEach { source ->
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source)
        }
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

    @Test
    fun parsesOfficialPieMetadataSectionsAndDuplicateFirstWins() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                """
                pie showData title Pets adopted
                  accTitle: Adoption chart
                  accDescr: Counts by animal
                  "Dogs" : 386
                  "Cats" : 85.5
                  "Dogs" : 1
                """.trimIndent(),
            ),
        )
        assertEquals(
            PieDiagram(
                title = "Pets adopted",
                showData = true,
                sections = listOf(PieSection("Dogs", 386.0), PieSection("Cats", 85.5)),
                accessibilityTitle = "Adoption chart",
                accessibilityDescription = "Counts by animal",
            ),
            result.diagram,
        )
    }

    @Test
    fun negativePieValueFailsClosedAtTheSection() {
        val failure = assertIs<MermaidParseResult.Failure>(MermaidParser.parse("pie\n  \"Dogs\" : -1"))
        assertEquals(MermaidDiagnosticCode.INVALID_VALUE, failure.diagnostics.single().code)
        assertEquals(SourceLocation(2, 3), failure.diagnostics.single().location)
    }

    @Test
    fun malformedPieTitleTokenFailsClosed() {
        val failure = assertIs<MermaidParseResult.Failure>(
            MermaidParser.parse("pie titlefoo\n\"Dogs\" : 1"),
        )
        assertEquals(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, failure.diagnostics.single().code)
    }

    @Test
    fun parsesBoundedGanttTasksAndStatuses() {
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("""
            gantt
              title Release plan
              dateFormat YYYY-MM-DD
              section Build
              Parser :done, parse, 2026-08-19, 2d
              Renderer :active, render, 2026-08-21, 3d
        """.trimIndent()))
        val diagram = assertIs<GanttDiagram>(result.diagram)
        assertEquals("Release plan", diagram.title)
        assertEquals(listOf(GanttTaskStatus.DONE, GanttTaskStatus.ACTIVE), diagram.sections.single().tasks.map { it.status })
        assertEquals(2, diagram.sections.single().tasks.first().durationDays)
    }

    @Test
    fun malformedGanttFailsClosed() {
        listOf(
            "gantt\nsection Build\nTask :id, 2026-02-30, 2d",
            "gantt\ndateFormat DD-MM-YYYY\nsection Build\nTask :id, 2026-08-19, 2d",
            "gantt\ndateFormat YYYY-MM-DD\nTask :id, 2026-08-19, 2d",
        ).forEach { assertIs<MermaidParseResult.Failure>(MermaidParser.parse(it), it) }
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("gantt\ndateFormat YYYY-MM-DD\nsection Build\nTask :blocked, id, 2026-08-19, 2d"))
    }
}
