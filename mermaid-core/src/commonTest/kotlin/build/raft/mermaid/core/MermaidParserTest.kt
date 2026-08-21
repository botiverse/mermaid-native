package build.raft.mermaid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MermaidParserTest {
    @Test
    fun parsesCynefinDomainsItemsAndTransitions() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse("cynefin-beta\ntitle Incident response\ncomplex\n\"Investigate & learn\"\ncomplicated\n\"Expert analysis\"\nclear\n\"Known fix\"\nchaotic\n\"Page on-call\"\nconfusion\n\"Unknown mode\"\ncomplex --> complicated : \"Pattern found\"\nclear --> clear : \"ignored\"")
        )
        val diagram = assertIs<CynefinDiagram>(result.diagram)
        assertEquals("Incident response", diagram.title)
        assertEquals(listOf(CynefinDomain.COMPLEX, CynefinDomain.COMPLICATED, CynefinDomain.CLEAR, CynefinDomain.CHAOTIC, CynefinDomain.CONFUSION), diagram.domains.map { it.domain })
        assertEquals(listOf("Investigate & learn"), diagram.domains.first().items)
        assertEquals(listOf(CynefinTransition(CynefinDomain.COMPLEX, CynefinDomain.COMPLICATED, "Pattern found")), diagram.transitions)
    }

    @Test
    fun malformedCynefinFailsClosed() {
        listOf(
            "cynefin-beta; complex",
            "cynefin-beta\ntitle One\ntitle Two\ncomplex",
            "cynefin-beta\ncomplex\ncomplex",
            "cynefin-beta\n\"orphan item\"",
            "cynefin-beta\ncomplex\nitem without quotes",
            "cynefin-beta\ncomplex -> clear",
            "cynefin-beta\naccTitle: unsupported",
            "cynefin-beta\ncomplex\nstyle complex fill:red",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }
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

    @Test
    fun parsesTimelinePeriodsAndMultipleLabels() {
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("""
            timeline
              title Product history
              2024 : Launch : First users
              2025 : Scale
        """.trimIndent()))
        assertEquals(TimelineDiagram("Product history", listOf(TimelineEvent("2024", listOf("Launch", "First users")), TimelineEvent("2025", listOf("Scale")))), result.diagram)
    }

    @Test
    fun malformedTimelineFailsClosed() {
        listOf("timeline", "timeline\n2024", "timeline\n2024 :", "timeline\n2024 : Launch :", "timeline\ntitle One\ntitle Two\n2024 : Launch")
            .forEach { assertIs<MermaidParseResult.Failure>(MermaidParser.parse(it), it) }
        val comma = assertIs<MermaidParseResult.Success>(MermaidParser.parse("timeline\n2024 : Launch, First users"))
        assertEquals(listOf("Launch, First users"), assertIs<TimelineDiagram>(comma.diagram).events.single().labels)
    }

    @Test
    fun parsesQuadrantChartAxesLabelsAndPoints() {
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("""
            quadrantChart
              title Product portfolio
              x-axis Low reach --> High reach
              y-axis Low engagement --> High engagement
              quadrant-1 Expand
              quadrant-2 Promote
              Campaign A: [0.3, 0.6]
              Campaign B: [1, 0]
        """.trimIndent()))
        assertEquals(
            QuadrantChartDiagram(
                "Product portfolio",
                QuadrantAxis("Low reach", "High reach"),
                QuadrantAxis("Low engagement", "High engagement"),
                listOf("Expand", "Promote", null, null),
                listOf(QuadrantPoint("Campaign A", 0.3, 0.6), QuadrantPoint("Campaign B", 1.0, 0.0)),
            ),
            result.diagram,
        )
    }

    @Test
    fun parsesUserJourneySectionsTasksScoresAndActors() {
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("""
            journey
              title Checkout journey
              section Discover
              Find product: 4: Shopper
              Review & compare: 3: Shopper, Advisor
              section Purchase
              Pay securely: 5: Shopper, Payment service
        """.trimIndent()))
        assertEquals(
            UserJourneyDiagram(
                "Checkout journey",
                listOf(
                    UserJourneySection(
                        "Discover",
                        listOf(
                            UserJourneyTask("Find product", 4, listOf("Shopper")),
                            UserJourneyTask("Review & compare", 3, listOf("Shopper", "Advisor")),
                        ),
                    ),
                    UserJourneySection(
                        "Purchase",
                        listOf(UserJourneyTask("Pay securely", 5, listOf("Shopper", "Payment service"))),
                    ),
                ),
            ),
            result.diagram,
        )
    }

    @Test
    fun parsesRequirementAndElementWithTypedRelationship() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                """
                requirementDiagram
                  requirement secure_login {
                    id: AUTH-1
                    text: Users authenticate securely
                    risk: high
                    verifymethod: test
                  }
                  element mobile_client {
                    type: application
                    docref: docs/auth.md
                  }
                  mobile_client - verifies -> secure_login
                """.trimIndent(),
            ),
        )
        assertEquals(
            RequirementDiagram(
                requirements = listOf(
                    RequirementDefinition("secure_login", "AUTH-1", "Users authenticate securely", RequirementRisk.HIGH, RequirementVerifyMethod.TEST),
                ),
                elements = listOf(RequirementElement("mobile_client", "application", "docs/auth.md")),
                relationships = listOf(RequirementRelationship("mobile_client", "secure_login", RequirementRelationshipKind.VERIFIES)),
            ),
            result.diagram,
        )
    }

    @Test
    fun malformedQuadrantChartFailsClosed() {
        listOf(
            "quadrantChart\nx-axis Low --> High\nCampaign: [0.2, 0.3]",
            "quadrantChart\nx-axis Low --> High\ny-axis Low --> High\nCampaign: [1.1, 0.3]",
            "quadrantChart\nx-axis Low --> High\ny-axis Low --> High\nCampaign: [NaN, 0.3]",
            "quadrantChart\nx-axis Low --> High\nx-axis Again --> High\ny-axis Low --> High\nCampaign: [0.2, 0.3]",
            "quadrantChart\nx-axis Low --> High\ny-axis Low --> High\nquadrant-1 One\nquadrant-1 Two\nCampaign: [0.2, 0.3]",
            "quadrantChart\nx-axis Low --> High\ny-axis Low --> High\nclick Campaign\nCampaign: [0.2, 0.3]",
        ).forEach { assertIs<MermaidParseResult.Failure>(MermaidParser.parse(it), it) }
    }

    @Test
    fun malformedUserJourneyFailsClosed() {
        listOf(
            "journey",
            "journey\nTask: 4: Actor",
            "journey\nsection Empty",
            "journey\nsection A\nTask: 0: Actor",
            "journey\nsection A\nTask: 6: Actor",
            "journey\nsection A\nTask: 4:",
            "journey\ntitle One\ntitle Two\nsection A\nTask: 4: Actor",
            "journey\nsection A\nTask: 4: Actor,",
            "journey\nsection A\nunsupported statement",
        ).forEach { source ->
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source)
        }
    }

    @Test
    fun parsesGitGraphBranchesCommitsCheckoutAndMerge() {
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("""
            gitGraph
              commit id: "base" tag: "v1"
              branch develop
              commit id: "feature" type: HIGHLIGHT
              switch main
              commit id: "release" type: REVERSE
              merge develop id: "merge" tag: "v2"
        """.trimIndent()))
        assertEquals(
            GitGraphDiagram(
                branches = listOf(GitGraphBranch("main", null), GitGraphBranch("develop", "base")),
                commits = listOf(
                    GitGraphCommit("base", "main", emptyList(), tag = "v1"),
                    GitGraphCommit("feature", "develop", listOf("base"), GitGraphCommitType.HIGHLIGHT),
                    GitGraphCommit("release", "main", listOf("base"), GitGraphCommitType.REVERSE),
                    GitGraphCommit("merge", "main", listOf("release", "feature"), tag = "v2", isMerge = true),
                ),
            ),
            result.diagram,
        )
    }

    @Test
    fun parsesPacketTitleSingleBitsAndRanges() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                """
                packet
                  title Header
                  0-15: "Source"
                  16: "Flag"
                  17-31: "Payload"
                """.trimIndent(),
            ),
        )
        assertEquals(
            PacketDiagram(
                "Header",
                listOf(PacketField(0, 15, "Source"), PacketField(16, 16, "Flag"), PacketField(17, 31, "Payload")),
            ),
            result.diagram,
        )
    }

    @Test
    fun malformedGitGraphFailsClosed() {
        listOf(
            "gitGraph",
            "gitGraph\ncheckout missing\ncommit",
            "gitGraph\nbranch develop\nbranch develop\ncommit",
            "gitGraph\ncommit id: \"same\"\ncommit id: \"same\"",
            "gitGraph\ncommit\nmerge main",
            "gitGraph\ncommit\nbranch develop\nswitch main\nmerge develop",
            "gitGraph\ncommit type: UNKNOWN",
            "gitGraph\ncherry-pick id: \"one\"",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }

    @Test
    fun malformedRequirementDiagramFailsClosed() {
        listOf(
            "requirementDiagram\n  requirement r {\n    id: R-1\n  }",
            "requirementDiagram\n  requirement r {\n    id: R-1\n    id: R-2\n    text: Text\n    risk: low\n    verifymethod: test\n  }",
            "requirementDiagram\n  requirement r {\n    id: R-1\n    text: Text\n    risk: extreme\n    verifymethod: test\n  }",
            "requirementDiagram\n  requirement r {\n    id: R-1\n    text: Text\n    risk: low\n    verifymethod: test",
            "requirementDiagram\n  element e {\n    type: app\n    docref: doc.md\n  }\n  e - copies -> missing",
            "requirementDiagram\n  requirement r {\n    id: R-1\n    text: Text\n    risk: low\n    verifymethod: test\n  }\n  e - satisfies -> r",
        ).forEach { source ->
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source)
        }
    }

    @Test fun parsesKanbanColumnsAndCards() {
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("kanban\ntodo[Todo]\n  spec[Write spec]\ndone[Done]\n  ship[Ship release]"))
        assertEquals(KanbanDiagram(listOf(KanbanColumn("todo", "Todo", listOf(KanbanCard("spec", "Write spec"))), KanbanColumn("done", "Done", listOf(KanbanCard("ship", "Ship release"))))), result.diagram)
    }

    @Test fun malformedKanbanFailsClosed() {
        listOf("kanban", "kanban\ntodo[Todo]", "kanban\n  task[Orphan]", "kanban\ntodo[Todo]\n task[Bad indent]", "kanban\ntodo[Todo]\n  todo[Duplicate]", "kanban\ntodo[Todo]\n  task[Card]@{ priority: 'High' }").forEach {
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse(it), it)
        }
    }

    @Test
    fun malformedPacketFailsClosed() {
        listOf(
            "packet",
            "packet\n  8-4: \"Reverse\"",
            "packet\n  0-7: \"A\"\n  7-15: \"Overlap\"",
            "packet\n  0-7: Missing quotes",
            "packet\n  +8: \"Relative is deferred\"",
            "packet\n  4096: \"Beyond bounded layout\"",
            "packet\n  999999999999999999999: \"Overflow\"",
            "packet\n  title First\n  title Second\n  0: \"Flag\"",
        ).forEach { source ->
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source)
        }
    }

    @Test fun parsesBlockGridSpansAndEdges() {
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("block\ncolumns 3\napi[Public API]:2\ndb[Database]\napi --> db"))
        assertEquals(
            BlockDiagram(3, listOf(BlockNode("api", "Public API", 2), BlockNode("db", "Database")), listOf(BlockEdge("api", "db"))),
            result.diagram,
        )
    }

    @Test fun malformedBlockFailsClosed() {
        listOf(
            "block",
            "block\ncolumns 0\na",
            "block\ncolumns 999999999999999999999\na",
            "block\ncolumns 2\ncolumns 3\na",
            "block\ncolumns 2\na:3",
            "block\ncolumns 2\na:999999999999999999999",
            "block\ncolumns 2\na\na",
            "block\ncolumns 2\na --> missing\na",
            "block\ncolumns 2\na --> a\na",
            "block\ncolumns 2\na b",
            "block\ncolumns 2\nblock:group\na\nend",
            "block\ncolumns 2\na\nstyle a fill:#fff",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }

    @Test fun parsesSankeyQuotedCsvAndWeights() {
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("sankey\nGrid,Industry,12.5\nIndustry,\"Heat, \"\"homes\"\"\",4"))
        assertEquals(
            SankeyDiagram(
                listOf(SankeyNode("Grid", "Grid"), SankeyNode("Industry", "Industry"), SankeyNode("Heat, \"homes\"", "Heat, \"homes\"")),
                listOf(SankeyLink("Grid", "Industry", 12.5), SankeyLink("Industry", "Heat, \"homes\"", 4.0)),
            ),
            result.diagram,
        )
    }

    @Test fun malformedSankeyFailsClosed() {
        listOf(
            "sankey",
            "sankey\nA,B",
            "sankey\nA,B,1,extra",
            "sankey\nA,,1",
            "sankey\nA,A,1",
            "sankey\nA,B,0",
            "sankey\nA,B,NaN",
            "sankey\nA,B,Infinity",
            "sankey\nA,B,1\nA,B,2",
            "sankey\nA,B,1\nB,A,1",
            "sankey\nA,\"unterminated,1",
            "sankey\nA,\"B\" tail,1",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }

    @Test fun parsesTreemapHierarchyAndValues() {
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("treemap-beta\n\"Products\"\n  \"Phones\": 50\n  \"Computers\": 30"))
        assertEquals(
            TreemapDiagram(listOf(TreemapNode("Products", children = listOf(TreemapNode("Phones", 50.0), TreemapNode("Computers", 30.0))))),
            result.diagram,
        )
    }

    @Test fun malformedTreemapFailsClosed() {
        listOf(
            "treemap-beta",
            "treemap-beta\n\"Root leaf\": 1",
            "treemap-beta\n\"Empty\"",
            "treemap-beta\n  \"Jump\": 1",
            "treemap-beta\n\"Root\"\n \"Bad indent\": 1",
            "treemap-beta\n\"Root\"\n\t\"Tab\": 1",
            "treemap-beta\n\"Root\"\n  \"Leaf\": 0",
            "treemap-beta\n\"Root\"\n  \"Leaf\": NaN",
            "treemap-beta\n\"Root\"\n  \"A\": 1.7976931348623157E308\n  \"B\": 1.7976931348623157E308",
            "treemap-beta\n\"Root\"\n  \"Leaf\": 1\n  \"Leaf\": 2",
            "treemap-beta\n\"Root\"\n  \"Leaf\": 1\n    \"Child\": 1",
            "treemap-beta\n\"Root\":::class1\n  \"Leaf\": 1",
            "treemap-beta\n\"Root\"\n  \"Leaf\": 1\nclassDef class1 fill:red",
            "treemap-beta;\n\"Root\"\n  \"Leaf\": 1",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }

    @Test fun parsesVennSetsAndUnions() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                "venn-beta\ntitle \"Product overlap\"\nset A[\"Mobile\"]:20\nset \"Web Team\":12\nset API\nunion A,\"Web Team\"[\"Shared UI\"]:3\nunion A,\"Web Team\",API[\"Platform\"]",
            ),
        )
        assertEquals(
            VennDiagram(
                title = "Product overlap",
                sets = listOf(VennSet("A", "Mobile", 20.0), VennSet("Web Team", "Web Team", 12.0), VennSet("API", "API")),
                unions = listOf(
                    VennUnion(listOf("A", "Web Team"), "Shared UI", 3.0),
                    VennUnion(listOf("A", "Web Team", "API"), "Platform"),
                ),
            ),
            result.diagram,
        )
    }

    @Test fun parsesVennUnionWithCommaInsideQuotedSetId() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse("venn-beta\nset \"A,B\"\nset C\nunion \"A,B\",C[\"Shared\"]"),
        )
        val diagram = assertIs<VennDiagram>(result.diagram)
        assertEquals(listOf("A,B", "C"), diagram.unions.single().setIds)
    }

    @Test fun malformedVennFailsClosed() {
        listOf(
            "venn-beta",
            "venn-beta\nset A\nset A",
            "venn-beta\nset A\nset B\nset C\nset D",
            "venn-beta\nset A:0\nset B",
            "venn-beta\nset A:NaN\nset B",
            "venn-beta\nset A\nset B\nunion A,C",
            "venn-beta\nset A\nset B\nunion A,A",
            "venn-beta\nset A\nset B\nunion A,B\nunion B,A",
            "venn-beta\nset A\nset B\nunion A",
            "venn-beta\nset A\nset B\nunion A,B:Infinity",
            "venn-beta\nset A\nset B\ntext T[\"Deferred\"]",
            "venn-beta\nset A\nset B\nstyle A fill:red",
            "venn-beta;\nset A\nset B",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }

    @Test fun parsesUsecaseActorsShapesAndRelationships() {
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("usecase-beta\ndirection LR\nactor Customer(\"Customer\")\n1Checkout(\"Place order\")\nReport[Generate report]\nCustomer -- \"starts\" --> 1Checkout\n1Checkout --> Report"))
        assertEquals(
            UsecaseDiagram(
                FlowDirection.LR,
                listOf(UsecaseActor("Customer", "Customer")),
                listOf(UsecaseNode("1Checkout", "Place order", UsecaseShape.ELLIPSE), UsecaseNode("Report", "Generate report", UsecaseShape.RECTANGLE)),
                listOf(UsecaseRelationship("Customer", "1Checkout", "starts"), UsecaseRelationship("1Checkout", "Report")),
            ),
            result.diagram,
        )
    }

    @Test fun malformedUsecaseFailsClosed() {
        listOf(
            "usecase-beta",
            "usecase-beta\nactor User\nactor User\nLogin(\"Login\")",
            "usecase-beta\nactor User\nLogin(\"Login\")\nUnknown --> Login",
            "usecase-beta\nactor User\nLogin(\"Login\")\nUser ..> Login",
            "usecase-beta\nactor User\nLogin(\"Login\")\nsystemBoundary \"App\"",
            "usecase-beta\nactor User\nLogin(\"Login\")\nstyle Login fill:red",
            "usecase-beta\nactor User-name\nLogin(\"Login\")",
            "usecase-beta\ndirection LR\ndirection TD\nactor User\nLogin(\"Login\")",
            "usecase-beta\ndirection BT\nactor User\nLogin(\"Login\")",
            "usecase-beta;\nactor User\nLogin(\"Login\")",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }

    @Test fun parsesArchitectureGroupsServicesAndPorts() {
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("architecture-beta\ngroup api(cloud)[API]\nservice db(database)[Database] in api\nservice app(server)[Server] in api\ndb:R --> L:app\napp:T -- B:db"))
        assertEquals(
            ArchitectureDiagram(
                groups = listOf(ArchitectureGroup("api", "cloud", "API")),
                services = listOf(ArchitectureService("db", "database", "Database", "api"), ArchitectureService("app", "server", "Server", "api")),
                edges = listOf(
                    ArchitectureEdge("db", ArchitecturePort.RIGHT, "app", ArchitecturePort.LEFT, true),
                    ArchitectureEdge("app", ArchitecturePort.TOP, "db", ArchitecturePort.BOTTOM, false),
                ),
            ),
            result.diagram,
        )
    }

    @Test fun malformedArchitectureFailsClosed() {
        listOf(
            "architecture-beta",
            "architecture-beta\ngroup api(cloud)[API]\nservice db(database)[Database] in missing",
            "architecture-beta\nservice db(database)[Database]\nservice db(server)[Duplicate]",
            "architecture-beta\ngroup api(cloud)[API]\nservice api(server)[Duplicate namespace]",
            "architecture-beta\nservice db(database)[Database]\ndb:R --> L:missing",
            "architecture-beta\nservice db(database)[Database]\ndb:R --> L:db",
            "architecture-beta\nservice db(database)[Database]\ndb:R ..> L:db",
            "architecture-beta\ngroup api(cloud)[API]\ngroup child(cloud)[Child] in api\nservice db(database)[Database] in api",
            "architecture-beta\ngroup api(cloud)[API]\nservice db(database)[Database] in api\nstyle db fill:red",
            "architecture-beta;\nservice db(database)[Database]",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }

    @Test fun parsesC4ContextElementsAndRelationships() {
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("C4Context\ntitle Banking context\nPerson(customer, \"Customer\", \"Uses the service\")\nSystem_Ext(bank, \"Bank API\")\nRel(customer, bank, \"Checks balance\", \"HTTPS\")\nBiRel(bank, customer, \"Updates\")"))
        assertEquals(
            C4Diagram(
                "Banking context",
                listOf(C4Element("customer", "Customer", "Uses the service", C4ElementKind.PERSON), C4Element("bank", "Bank API", null, C4ElementKind.SYSTEM, true)),
                listOf(C4Relationship("customer", "bank", "Checks balance", "HTTPS"), C4Relationship("bank", "customer", "Updates", bidirectional = true)),
            ),
            result.diagram,
        )
    }

    @Test fun malformedC4ContextFailsClosed() {
        listOf(
            "C4Context",
            "c4context\nPerson(a, \"A\")",
            "C4Context\nPerson(a, \"A\")\nSystem(a, \"Duplicate\")",
            "C4Context\nPerson(a, \"A\")\nRel(a, missing, \"Uses\")",
            "C4Context\nPerson(a, \"A\")\nRel(a, a, \"Self\")",
            "C4Context\nPerson(a, \"A\")\nBoundary(b, \"Deferred\") {",
            "C4Context\nPerson(a, \"A\")\nUpdateElementStyle(a, ${'$'}fontColor=\"red\")",
            "C4Context;\nPerson(a, \"A\")",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }
}
