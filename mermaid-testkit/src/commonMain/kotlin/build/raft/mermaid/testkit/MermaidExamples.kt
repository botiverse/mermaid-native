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
import build.raft.mermaid.core.PacketDiagram
import build.raft.mermaid.core.PacketField
import build.raft.mermaid.core.BlockDiagram
import build.raft.mermaid.core.BlockNode
import build.raft.mermaid.core.BlockEdge
import build.raft.mermaid.core.SankeyDiagram
import build.raft.mermaid.core.SankeyNode
import build.raft.mermaid.core.SankeyLink
import build.raft.mermaid.core.TreemapDiagram
import build.raft.mermaid.core.TreemapNode
import build.raft.mermaid.core.VennDiagram
import build.raft.mermaid.core.VennSet
import build.raft.mermaid.core.VennUnion
import build.raft.mermaid.core.UsecaseDiagram
import build.raft.mermaid.core.UsecaseActor
import build.raft.mermaid.core.UsecaseNode
import build.raft.mermaid.core.UsecaseShape
import build.raft.mermaid.core.UsecaseRelationship
import build.raft.mermaid.core.ArchitectureDiagram
import build.raft.mermaid.core.ArchitectureGroup
import build.raft.mermaid.core.ArchitectureService
import build.raft.mermaid.core.ArchitectureEdge
import build.raft.mermaid.core.ArchitecturePort
import build.raft.mermaid.core.C4Diagram
import build.raft.mermaid.core.C4Element
import build.raft.mermaid.core.C4ElementKind
import build.raft.mermaid.core.C4Relationship
import build.raft.mermaid.core.CynefinDiagram
import build.raft.mermaid.core.CynefinDomain
import build.raft.mermaid.core.CynefinDomainBlock
import build.raft.mermaid.core.CynefinTransition
import build.raft.mermaid.core.EventModelingDiagram
import build.raft.mermaid.core.EventModelingEntityKind
import build.raft.mermaid.core.EventModelingFrame
import build.raft.mermaid.core.EventModelingRelation
import build.raft.mermaid.core.SwimlaneDiagram
import build.raft.mermaid.core.Swimlane
import build.raft.mermaid.core.SwimlaneNode
import build.raft.mermaid.core.SwimlaneNodeShape
import build.raft.mermaid.core.SwimlaneEdge
import build.raft.mermaid.core.TreeViewDiagram
import build.raft.mermaid.core.TreeViewNode
import build.raft.mermaid.core.RailroadChoice
import build.raft.mermaid.core.RailroadDiagram
import build.raft.mermaid.core.RailroadNonTerminal
import build.raft.mermaid.core.RailroadOptional
import build.raft.mermaid.core.RailroadSequence
import build.raft.mermaid.core.RailroadStack
import build.raft.mermaid.core.RailroadTerminal
import build.raft.mermaid.core.ZenumlAsyncMessage
import build.raft.mermaid.core.ZenumlDiagram
import build.raft.mermaid.core.ZenumlParticipant
import build.raft.mermaid.core.ZenumlSyncMessage
import build.raft.mermaid.core.WardleyEvolution
import build.raft.mermaid.core.WardleyLink
import build.raft.mermaid.core.WardleyMapDiagram
import build.raft.mermaid.core.WardleyNode
import build.raft.mermaid.core.WardleyNote

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
    public val eventModelingCartFlow: MermaidExample = MermaidExample(
        path = "samples/eventmodeling-cart-flow.mmd",
        source = "eventmodeling\ntitle Cart & inventory\ntf 01 ui CartUI\ntf 02 cmd AddItem\ntf 03 evt ItemAdded\nrf 04 evt External.InventoryChanged\ntf 05 pcr InventoryProcessor\ntf 06 rmo InventoryView ->> 03 ->> 04",
        expected = EventModelingDiagram(
            title = "Cart & inventory",
            frames = listOf(
                EventModelingFrame("01", "CartUI", EventModelingEntityKind.UI),
                EventModelingFrame("02", "AddItem", EventModelingEntityKind.COMMAND),
                EventModelingFrame("03", "ItemAdded", EventModelingEntityKind.EVENT),
                EventModelingFrame("04", "External.InventoryChanged", EventModelingEntityKind.EVENT, reset = true),
                EventModelingFrame("05", "InventoryProcessor", EventModelingEntityKind.PROCESSOR),
                EventModelingFrame("06", "InventoryView", EventModelingEntityKind.READ_MODEL),
            ),
            relations = listOf(
                EventModelingRelation("01", "02"),
                EventModelingRelation("02", "03"),
                EventModelingRelation("04", "05"),
                EventModelingRelation("03", "06"),
                EventModelingRelation("04", "06"),
            ),
        ),
    )
    public val c4BankingContext: MermaidExample = MermaidExample(
        "samples/c4-banking-context.mmd",
        "C4Context\ntitle Banking context\nPerson(customer, \"Customer & partner\", \"Uses the app\")\nSystem(bank, \"Banking system\", \"Shows balances\")\nRel(customer, bank, \"Uses\", \"HTTPS\")",
        C4Diagram(
            title = "Banking context",
            elements = listOf(C4Element("customer", "Customer & partner", "Uses the app", C4ElementKind.PERSON), C4Element("bank", "Banking system", "Shows balances", C4ElementKind.SYSTEM)),
            relationships = listOf(C4Relationship("customer", "bank", "Uses", "HTTPS")),
        ),
    )
    public val architectureApiStack: MermaidExample = MermaidExample(
        "samples/architecture-api-stack.mmd",
        "architecture-beta\ngroup api(cloud)[API & gateway]\nservice db(database)[Database] in api\nservice server(server)[Application server] in api\ndb:B --> T:server",
        ArchitectureDiagram(
            groups = listOf(ArchitectureGroup("api", "cloud", "API & gateway")),
            services = listOf(ArchitectureService("db", "database", "Database", "api"), ArchitectureService("server", "server", "Application server", "api")),
            edges = listOf(ArchitectureEdge("db", ArchitecturePort.BOTTOM, "server", ArchitecturePort.TOP, true)),
        ),
    )
    public val usecaseOrderFlow: MermaidExample = MermaidExample(
        "samples/usecase-order-flow.mmd",
        "usecase-beta\ndirection LR\nactor Customer(\"Customer\")\nCheckout(\"Place order\")\nReceipt[Create receipt]\nCustomer -- \"starts\" --> Checkout\nCheckout --> Receipt",
        UsecaseDiagram(
            direction = build.raft.mermaid.core.FlowDirection.LR,
            actors = listOf(UsecaseActor("Customer", "Customer")),
            useCases = listOf(UsecaseNode("Checkout", "Place order", UsecaseShape.ELLIPSE), UsecaseNode("Receipt", "Create receipt", UsecaseShape.RECTANGLE)),
            relationships = listOf(UsecaseRelationship("Customer", "Checkout", "starts"), UsecaseRelationship("Checkout", "Receipt")),
        ),
    )
    public val vennTeamOverlap: MermaidExample = MermaidExample(
        "samples/venn-team-overlap.mmd",
        "venn-beta\ntitle \"Team overlap\"\nset Frontend[\"Frontend & design\"]:20\nset Backend:16\nset Platform:12\nunion Frontend,Backend[\"APIs\"]:5\nunion Frontend,Backend,Platform[\"Shared tooling\"]:2",
        VennDiagram(
            title = "Team overlap",
            sets = listOf(
                VennSet("Frontend", "Frontend & design", 20.0),
                VennSet("Backend", "Backend", 16.0),
                VennSet("Platform", "Platform", 12.0),
            ),
            unions = listOf(
                VennUnion(listOf("Frontend", "Backend"), "APIs", 5.0),
                VennUnion(listOf("Frontend", "Backend", "Platform"), "Shared tooling", 2.0),
            ),
        ),
    )
    public val treemapProductMix: MermaidExample = MermaidExample(
        "samples/treemap-product-mix.mmd",
        "treemap-beta\n\"Products & services\"\n  \"Mobile\": 45\n  \"Web\": 35\n  \"API\": 20",
        TreemapDiagram(
            listOf(
                TreemapNode(
                    "Products & services",
                    children = listOf(TreemapNode("Mobile", 45.0), TreemapNode("Web", 35.0), TreemapNode("API", 20.0)),
                ),
            ),
        ),
    )
    public val sankeyEnergyFlow: MermaidExample = MermaidExample(
        "samples/sankey-energy-flow.mmd",
        "sankey\nGrid,Industry,12.5\nGrid,\"Heating, homes\",7.25\nIndustry,Losses & exports,2.5",
        SankeyDiagram(
            listOf(
                SankeyNode("Grid", "Grid"),
                SankeyNode("Industry", "Industry"),
                SankeyNode("Heating, homes", "Heating, homes"),
                SankeyNode("Losses & exports", "Losses & exports"),
            ),
            listOf(
                SankeyLink("Grid", "Industry", 12.5),
                SankeyLink("Grid", "Heating, homes", 7.25),
                SankeyLink("Industry", "Losses & exports", 2.5),
            ),
        ),
    )
    public val blockServiceMap: MermaidExample = MermaidExample(
        "samples/block-service-map.mmd",
        "block\ncolumns 3\napi[Public & partner API]:2\ndb[Database]\nworker[Worker]:2\napi --> worker\ndb --> worker",
        BlockDiagram(
            3,
            listOf(BlockNode("api", "Public & partner API", 2), BlockNode("db", "Database"), BlockNode("worker", "Worker", 2)),
            listOf(BlockEdge("api", "worker"), BlockEdge("db", "worker")),
        ),
    )
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

    public val packetUdp: MermaidExample = MermaidExample(
        path = "samples/packet-udp.mmd",
        source = """
            packet
              title UDP Packet
              0-15: "Source Port"
              16-31: "Destination Port"
              32-47: "Length"
              48-63: "Checksum"
              64-95: "Data"
        """.trimIndent(),
        expected = PacketDiagram(
            title = "UDP Packet",
            fields = listOf(
                PacketField(0, 15, "Source Port"),
                PacketField(16, 31, "Destination Port"),
                PacketField(32, 47, "Length"),
                PacketField(48, 63, "Checksum"),
                PacketField(64, 95, "Data"),
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

    public val cynefinIncidentResponse: MermaidExample = MermaidExample(
        path = "samples/cynefin-incident-response.mmd",
        source = """
            cynefin-beta
              title Incident response
              complex
                "Investigate & learn"
                "Run chaos experiment"
              complicated
                "Expert analysis"
              clear
                "Apply known fix"
              chaotic
                "Page on-call"
              confusion
                "Unknown failure"
              complex --> complicated : "Pattern found"
              chaotic --> complex : "Stabilized"
        """.trimIndent(),
        expected = CynefinDiagram(
            title = "Incident response",
            domains = listOf(
                CynefinDomainBlock(CynefinDomain.COMPLEX, listOf("Investigate & learn", "Run chaos experiment")),
                CynefinDomainBlock(CynefinDomain.COMPLICATED, listOf("Expert analysis")),
                CynefinDomainBlock(CynefinDomain.CLEAR, listOf("Apply known fix")),
                CynefinDomainBlock(CynefinDomain.CHAOTIC, listOf("Page on-call")),
                CynefinDomainBlock(CynefinDomain.CONFUSION, listOf("Unknown failure")),
            ),
            transitions = listOf(
                CynefinTransition(CynefinDomain.COMPLEX, CynefinDomain.COMPLICATED, "Pattern found"),
                CynefinTransition(CynefinDomain.CHAOTIC, CynefinDomain.COMPLEX, "Stabilized"),
            ),
        ),
    )

    public val swimlaneSupportEscalation: MermaidExample = MermaidExample(
        path = "samples/swimlane-support-escalation.mmd",
        source = """
            swimlane-beta LR
              subgraph customer [Customer & partner]
                request[Request service]
                receive((Receive update))
              end
              subgraph support [Support team]
                triage{Known issue?}
                answer[Send answer]
              end
              request -->|handoff & review| triage
              triage --> answer --> receive
        """.trimIndent(),
        expected = SwimlaneDiagram(
            FlowDirection.LR,
            listOf(
                Swimlane("customer", "Customer & partner", listOf(SwimlaneNode("request", "Request service", SwimlaneNodeShape.RECTANGLE), SwimlaneNode("receive", "Receive update", SwimlaneNodeShape.CIRCLE))),
                Swimlane("support", "Support team", listOf(SwimlaneNode("triage", "Known issue?", SwimlaneNodeShape.DECISION), SwimlaneNode("answer", "Send answer", SwimlaneNodeShape.RECTANGLE))),
            ),
            listOf(SwimlaneEdge("request", "triage", "handoff & review"), SwimlaneEdge("triage", "answer"), SwimlaneEdge("answer", "receive")),
        ),
    )

    public val treeViewProject: MermaidExample = MermaidExample(
        path = "samples/treeview-project.mmd",
        source = """
            treeView-beta
                project/
                    src/
                        index.ts
                    "README file.md"
                package.json
        """.trimIndent(),
        expected = TreeViewDiagram(
            listOf(
                TreeViewNode("project", 0, null, true),
                TreeViewNode("src", 1, 0, true),
                TreeViewNode("index.ts", 2, 1, false),
                TreeViewNode("README file.md", 1, 0, false),
                TreeViewNode("package.json", 0, null, false),
            ),
        ),
    )

    public val railroadAuthFlow: MermaidExample = MermaidExample(
        path = "samples/railroad-auth-flow.mmd",
        source = """
            railroad-beta
            Diagram(
              Sequence(
                'token',
                Choice(0,
                  NonTerminal('session'),
                  Optional('refresh')
                ),
                Stack('validate', 'store')
              )
            )
        """.trimIndent(),
        expected = RailroadDiagram(
            RailroadSequence(
                listOf(
                    RailroadTerminal("token"),
                    RailroadChoice(
                        0,
                        listOf(RailroadNonTerminal("session"), RailroadOptional(RailroadTerminal("refresh"))),
                    ),
                    RailroadStack(listOf(RailroadTerminal("validate"), RailroadTerminal("store"))),
                ),
            ),
        ),
    )

    public val zenumlTokenHandshake: MermaidExample = MermaidExample(
        path = "samples/zenuml-token-handshake.mmd",
        source = """
            zenuml
            title Token handshake
            Client
            Store as Token store
            Client->Gateway.submit()
            Gateway->Store.lookup
            Client->Gateway: cancel
        """.trimIndent(),
        expected = ZenumlDiagram(
            title = "Token handshake",
            participants = listOf(
                ZenumlParticipant("Client", "Client"),
                ZenumlParticipant("Store", "Token store"),
                ZenumlParticipant("Gateway", "Gateway"),
            ),
            messages = listOf(
                ZenumlSyncMessage("Client", "Gateway", "submit"),
                ZenumlSyncMessage("Gateway", "Store", "lookup"),
                ZenumlAsyncMessage("Client", "Gateway", "cancel"),
            ),
        ),
    )

    public val wardleyTeaShop: MermaidExample = MermaidExample(
        path = "samples/wardley-tea-shop.mmd",
        source = """
            wardley-beta
            title Tea Shop Value Chain
            anchor Business [0.95, 0.63]
            component Cup of Tea [0.79, 0.61]
            component Tea [0.63, 0.81]
            component Hot Water [0.52, 0.80]
            component Kettle [0.43, 0.35]
            component Power [0.10, 0.70]
            Business -> Cup of Tea
            Cup of Tea -> Tea
            Cup of Tea -> Hot Water
            Hot Water -> Kettle
            Kettle -> Power
            evolve Kettle 0.62
            evolve Power 0.89
            note "Standardising power allows Kettles to evolve faster" [0.30, 0.49]
        """.trimIndent(),
        expected = WardleyMapDiagram(
            title = "Tea Shop Value Chain",
            nodes = listOf(
                WardleyNode("Business", 0.95, 0.63, anchor = true),
                WardleyNode("Cup of Tea", 0.79, 0.61, anchor = false),
                WardleyNode("Tea", 0.63, 0.81, anchor = false),
                WardleyNode("Hot Water", 0.52, 0.80, anchor = false),
                WardleyNode("Kettle", 0.43, 0.35, anchor = false),
                WardleyNode("Power", 0.10, 0.70, anchor = false),
            ),
            links = listOf(
                WardleyLink("Business", "Cup of Tea"),
                WardleyLink("Cup of Tea", "Tea"),
                WardleyLink("Cup of Tea", "Hot Water"),
                WardleyLink("Hot Water", "Kettle"),
                WardleyLink("Kettle", "Power"),
            ),
            evolutions = listOf(
                WardleyEvolution("Kettle", 0.62),
                WardleyEvolution("Power", 0.89),
            ),
            notes = listOf(WardleyNote("Standardising power allows Kettles to evolve faster", 0.30, 0.49)),
        ),
    )

    public val all: List<MermaidExample> = listOf(
        c4BankingContext,
        architectureApiStack,
        usecaseOrderFlow,
        vennTeamOverlap,
        treemapProductMix,
        sankeyEnergyFlow,
        blockServiceMap,
        kanbanReleaseBoard,
        gitGraphReleaseFlow,
        requirementLogin,
        packetUdp,
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
        cynefinIncidentResponse,
        eventModelingCartFlow,
        swimlaneSupportEscalation,
        treeViewProject,
        railroadAuthFlow,
        zenumlTokenHandshake,
        wardleyTeaShop,
    )
}
