package build.raft.mermaid.testkit

import build.raft.mermaid.core.FlowDirection
import build.raft.mermaid.core.FlowEdge
import build.raft.mermaid.core.FlowNode
import build.raft.mermaid.core.FlowchartDiagram
import build.raft.mermaid.core.ClassDiagram
import build.raft.mermaid.core.ClassDefinition
import build.raft.mermaid.core.ClassMember
import build.raft.mermaid.core.ClassRelationship
import build.raft.mermaid.core.ClassRelationshipKind
import build.raft.mermaid.core.EntityAttribute
import build.raft.mermaid.core.EntityCardinality
import build.raft.mermaid.core.EntityDefinition
import build.raft.mermaid.core.EntityKey
import build.raft.mermaid.core.EntityRelationship
import build.raft.mermaid.core.EntityRelationshipDiagram
import build.raft.mermaid.core.MermaidDiagram
import build.raft.mermaid.core.SequenceActor
import build.raft.mermaid.core.SequenceArrowHead
import build.raft.mermaid.core.SequenceDiagram
import build.raft.mermaid.core.SequenceLineStyle
import build.raft.mermaid.core.SequenceMessage
import build.raft.mermaid.core.PieDiagram
import build.raft.mermaid.core.PieSection
import build.raft.mermaid.core.StateDiagram
import build.raft.mermaid.core.StateNode
import build.raft.mermaid.core.StateNodeKind
import build.raft.mermaid.core.StateTransition
import build.raft.mermaid.core.NumericAxis
import build.raft.mermaid.core.XyAxis
import build.raft.mermaid.core.XyChartDiagram
import build.raft.mermaid.core.XySeries
import build.raft.mermaid.core.XySeriesKind
import build.raft.mermaid.core.TimelineDiagram
import build.raft.mermaid.core.TimelineEvent
import build.raft.mermaid.core.MindmapDiagram
import build.raft.mermaid.core.MindmapNode
import build.raft.mermaid.core.MindmapNodeShape
import build.raft.mermaid.core.GanttDiagram
import build.raft.mermaid.core.GanttSection
import build.raft.mermaid.core.GanttTask
import build.raft.mermaid.core.GanttTaskStatus
import build.raft.mermaid.core.QuadrantAxis
import build.raft.mermaid.core.QuadrantChartDiagram
import build.raft.mermaid.core.QuadrantPoint
import build.raft.mermaid.core.UserJourneyDiagram
import build.raft.mermaid.core.UserJourneySection
import build.raft.mermaid.core.UserJourneyTask
import build.raft.mermaid.core.GitGraphBranch
import build.raft.mermaid.core.GitGraphCommit
import build.raft.mermaid.core.GitGraphCommitType
import build.raft.mermaid.core.GitGraphDiagram
import build.raft.mermaid.core.RequirementDefinition
import build.raft.mermaid.core.RequirementDiagram
import build.raft.mermaid.core.RequirementElement
import build.raft.mermaid.core.RequirementRelationship
import build.raft.mermaid.core.RequirementRelationshipKind
import build.raft.mermaid.core.RequirementRisk
import build.raft.mermaid.core.RequirementVerifyMethod
import build.raft.mermaid.core.KanbanCard
import build.raft.mermaid.core.KanbanColumn
import build.raft.mermaid.core.KanbanDiagram

/**
 * Public examples mirrored by the files under [samples].
 *
 * The inputs are intentionally limited to syntax accepted by the current
 * parser. The upstream provenance and adaptation notes live in samples/README.md.
 */
public data class MermaidExample(
    val path: String,
    val source: String,
    val expected: MermaidDiagram,
)

public object MermaidExamples {
    public val kanbanReleaseBoard: MermaidExample = MermaidExample(
        "samples/kanban-release-board.mmd",
        "kanban\ntodo[Todo]\n  spec[Write & review spec]\n  tests[Add tests]\ndone[Done]\n  release[Ship release]",
        KanbanDiagram(listOf(
            KanbanColumn("todo", "Todo", listOf(KanbanCard("spec", "Write & review spec"), KanbanCard("tests", "Add tests"))),
            KanbanColumn("done", "Done", listOf(KanbanCard("release", "Ship release"))),
        )),
    )
    public val gitGraphReleaseFlow: MermaidExample = MermaidExample(
        path = "samples/gitgraph-release-flow.mmd",
        source = """
            gitGraph
              commit id: "base" tag: "v1.0"
              branch develop
              commit id: "feature" type: HIGHLIGHT
              checkout main
              commit id: "release" type: REVERSE
              merge develop id: "merge" tag: "v2 & stable"
        """.trimIndent(),
        expected = GitGraphDiagram(
            listOf(GitGraphBranch("main", null), GitGraphBranch("develop", "base")),
            listOf(
                GitGraphCommit("base", "main", emptyList(), tag = "v1.0"),
                GitGraphCommit("feature", "develop", listOf("base"), GitGraphCommitType.HIGHLIGHT),
                GitGraphCommit("release", "main", listOf("base"), GitGraphCommitType.REVERSE),
                GitGraphCommit("merge", "main", listOf("release", "feature"), tag = "v2 & stable", isMerge = true),
            ),
        ),
    )

    public val requirementLogin: MermaidExample = MermaidExample(
        path = "samples/requirement-login.mmd",
        source = """
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
              mobile_client - satisfies -> secure_login
        """.trimIndent(),
        expected = RequirementDiagram(
            requirements = listOf(
                RequirementDefinition(
                    name = "secure_login",
                    id = "AUTH-1",
                    text = "Users authenticate securely",
                    risk = RequirementRisk.HIGH,
                    verifyMethod = RequirementVerifyMethod.TEST,
                ),
            ),
            elements = listOf(RequirementElement("mobile_client", "application", "docs/auth.md")),
            relationships = listOf(
                RequirementRelationship("mobile_client", "secure_login", RequirementRelationshipKind.SATISFIES),
            ),
        ),
    )
    public val mindmapProjectPlan: MermaidExample = MermaidExample(
        path = "samples/mindmap-project-plan.mmd",
        source = """
            mindmap
              root((Project plan))
                Discovery
                  [Requirements]
                  Research
                Delivery
                  ((Native SVG))
        """.trimIndent(),
        expected = MindmapDiagram(
            nodes = listOf(
                MindmapNode("root", "Project plan", null, 0, MindmapNodeShape.DOUBLE_CIRCLE),
                MindmapNode("__mindmap_1", "Discovery", "root", 1),
                MindmapNode("__mindmap_2", "Requirements", "__mindmap_1", 2, MindmapNodeShape.RECTANGLE),
                MindmapNode("__mindmap_3", "Research", "__mindmap_1", 2),
                MindmapNode("__mindmap_4", "Delivery", "root", 1),
                MindmapNode("__mindmap_5", "Native SVG", "__mindmap_4", 2, MindmapNodeShape.DOUBLE_CIRCLE),
            ),
        ),
    )
    public val xyQuarterlySales: MermaidExample = MermaidExample(
        path = "samples/xy-quarterly-sales.mmd",
        source = """
            xychart-beta
              title "Quarterly sales"
              x-axis "Quarter" [Q1, Q2, Q3, Q4]
              y-axis "Revenue" 0 --> 100
              bar [20, 45, 70, 85]
              line [25, 40, 75, 90]
        """.trimIndent(),
        expected = XyChartDiagram(
            title = "Quarterly sales",
            xAxis = XyAxis("Quarter", listOf("Q1", "Q2", "Q3", "Q4")),
            yAxis = NumericAxis("Revenue", 0.0, 100.0),
            series = listOf(
                XySeries(XySeriesKind.BAR, listOf(20.0, 45.0, 70.0, 85.0)),
                XySeries(XySeriesKind.LINE, listOf(25.0, 40.0, 75.0, 90.0)),
            ),
        ),
    )
    public val entityRelationshipCustomerOrder: MermaidExample = MermaidExample(
        path = "samples/entity-customer-order.mmd",
        source = """
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
        expected = EntityRelationshipDiagram(
            entities = listOf(
                EntityDefinition("CUSTOMER", listOf(EntityAttribute("int", "id", EntityKey.PK), EntityAttribute("string", "name"))),
                EntityDefinition("ORDER", listOf(EntityAttribute("int", "id", EntityKey.PK), EntityAttribute("int", "customerId", EntityKey.FK))),
            ),
            relationships = listOf(EntityRelationship("CUSTOMER", "ORDER", EntityCardinality.ONLY_ONE, EntityCardinality.ZERO_OR_MORE, "places")),
        ),
    )
    public val classAnimal: MermaidExample = MermaidExample(
        path = "samples/class-animal.mmd",
        source = """
            classDiagram
            class Animal
            Animal : +String name
            Animal : +eat()
            Animal <|-- Duck
            class Duck
            Duck : +swim()
        """.trimIndent(),
        expected = ClassDiagram(
            classes = listOf(
                ClassDefinition("Animal", members = listOf(ClassMember("String name"), ClassMember("eat()"))),
                ClassDefinition("Duck", members = listOf(ClassMember("swim()"))),
            ),
            relationships = listOf(ClassRelationship("Animal", "Duck", ClassRelationshipKind.INHERITANCE)),
        ),
    )
    public val piePets: MermaidExample = MermaidExample(
        path = "samples/pie-pets.mmd",
        source = """
            pie showData title Pets adopted
              "Dogs" : 386
              "Cats" : 85
              "Rats" : 15
        """.trimIndent(),
        expected = PieDiagram(
            title = "Pets adopted",
            showData = true,
            sections = listOf(PieSection("Dogs", 386.0), PieSection("Cats", 85.0), PieSection("Rats", 15.0)),
        ),
    )

    public val flowchartLinear: MermaidExample = MermaidExample(
        path = "samples/flowchart-linear.mmd",
        source = """
            graph TD
              A[Start] --> B[Process]
              B --> C[End]
        """.trimIndent(),
        expected = FlowchartDiagram(
            direction = FlowDirection.TD,
            nodes = listOf(
                FlowNode("A", "Start"),
                FlowNode("B", "Process"),
                FlowNode("C", "End"),
            ),
            edges = listOf(FlowEdge("A", "B"), FlowEdge("B", "C")),
        ),
    )

    public val flowchartLeftToRight: MermaidExample = MermaidExample(
        path = "samples/flowchart-left-to-right.mmd",
        source = """
            graph LR
              A[Input] --> B[Transform]
              B --> C[Output]
        """.trimIndent(),
        expected = FlowchartDiagram(
            direction = FlowDirection.LR,
            nodes = listOf(
                FlowNode("A", "Input"),
                FlowNode("B", "Transform"),
                FlowNode("C", "Output"),
            ),
            edges = listOf(FlowEdge("A", "B"), FlowEdge("B", "C")),
        ),
    )

    public val sequenceRequestResponse: MermaidExample = MermaidExample(
        path = "samples/sequence-request-response.mmd",
        source = """
            sequenceDiagram
              Alice->>Bob: Hello Bob!
              Bob-->>Alice: Hi Alice!
        """.trimIndent(),
        expected = SequenceDiagram(
            actors = listOf(
                SequenceActor("Alice", "Alice"),
                SequenceActor("Bob", "Bob"),
            ),
            messages = listOf(
                SequenceMessage(
                    from = "Alice",
                    to = "Bob",
                    label = "Hello Bob!",
                    lineStyle = SequenceLineStyle.SOLID,
                    arrowHead = SequenceArrowHead.FILLED,
                ),
                SequenceMessage(
                    from = "Bob",
                    to = "Alice",
                    label = "Hi Alice!",
                    lineStyle = SequenceLineStyle.DASHED,
                    arrowHead = SequenceArrowHead.FILLED,
                ),
            ),
        ),
    )

    public val stateLifecycle: MermaidExample = MermaidExample(
        path = "samples/state-lifecycle.mmd",
        source = """
            stateDiagram-v2
              direction LR
              [*] --> Idle
              state "Processing request" as Working
              Idle --> Working: start
              Working --> [*]: finish
        """.trimIndent(),
        expected = StateDiagram(
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
    )

    public val ganttReleasePlan: MermaidExample = MermaidExample(
        path = "samples/gantt-release-plan.mmd",
        source = """
            gantt
              title Release plan
              dateFormat YYYY-MM-DD
              section Build
              Parser :done, parse, 2026-08-19, 2d
              Renderer :active, render, 2026-08-21, 3d
        """.trimIndent(),
        expected = GanttDiagram("Release plan", "YYYY-MM-DD", listOf(GanttSection("Build", listOf(
            GanttTask("Parser", "parse", 740212, 2, GanttTaskStatus.DONE),
            GanttTask("Renderer", "render", 740214, 3, GanttTaskStatus.ACTIVE),
        )))),
    )

    public val timelineProductHistory: MermaidExample = MermaidExample(
        path = "samples/timeline-product-history.mmd",
        source = """
            timeline
              title Product history
              2024 : Launch : First users
              2025 : Scale
        """.trimIndent(),
        expected = TimelineDiagram("Product history", listOf(
            TimelineEvent("2024", listOf("Launch", "First users")),
            TimelineEvent("2025", listOf("Scale")),
        )),
    )

    public val quadrantProductPortfolio: MermaidExample = MermaidExample(
        path = "samples/quadrant-product-portfolio.mmd",
        source = """
            quadrantChart
              title Product portfolio
              x-axis Low reach --> High reach
              y-axis Low engagement --> High engagement
              quadrant-1 Expand
              quadrant-2 Promote
              quadrant-3 Re-evaluate
              quadrant-4 Improve
              Campaign A: [0.3, 0.6]
              Campaign B: [0.75, 0.25]
        """.trimIndent(),
        expected = QuadrantChartDiagram(
            "Product portfolio",
            QuadrantAxis("Low reach", "High reach"),
            QuadrantAxis("Low engagement", "High engagement"),
            listOf("Expand", "Promote", "Re-evaluate", "Improve"),
            listOf(QuadrantPoint("Campaign A", 0.3, 0.6), QuadrantPoint("Campaign B", 0.75, 0.25)),
        ),
    )

    public val userJourneyCheckout: MermaidExample = MermaidExample(
        path = "samples/user-journey-checkout.mmd",
        source = """
            journey
              title Checkout journey
              section Discover
              Find product: 4: Shopper
              Review & compare: 3: Shopper, Advisor
              section Purchase
              Add to cart: 5: Shopper
              Pay securely: 4: Shopper, Payment service
        """.trimIndent(),
        expected = UserJourneyDiagram(
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
                    listOf(
                        UserJourneyTask("Add to cart", 5, listOf("Shopper")),
                        UserJourneyTask("Pay securely", 4, listOf("Shopper", "Payment service")),
                    ),
                ),
            ),
        ),
    )

    public val all: List<MermaidExample> = listOf(
        kanbanReleaseBoard,
        gitGraphReleaseFlow,
        requirementLogin,
        mindmapProjectPlan,
        xyQuarterlySales,
        entityRelationshipCustomerOrder,
        classAnimal,
        piePets,
        flowchartLinear,
        flowchartLeftToRight,
        sequenceRequestResponse,
        stateLifecycle,
        ganttReleasePlan,
        timelineProductHistory,
        quadrantProductPortfolio,
        userJourneyCheckout,
    )
}
