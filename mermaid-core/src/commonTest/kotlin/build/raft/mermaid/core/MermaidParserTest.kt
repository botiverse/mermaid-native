package build.raft.mermaid.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MermaidParserTest {
    @Test fun infoSupportsOriginalShowInfoAndUsesNativeBuildVersion() {
        val plain = assertIs<InfoDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("info")).diagram)
        assertEquals(MERMAID_NATIVE_VERSION, plain.version)
        assertEquals(false, plain.showInfo)
        val shown = assertIs<InfoDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("info\nshowInfo")).diagram)
        assertEquals(true, shown.showInfo)
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("info unsupported"))
    }

    @Test
    fun bareFlowchartReferencesPreserveLabelsAndShapes() {
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "flowchart TD\nA[Start] --> B(Process)\nB --> C[End]\nB\nC[Renamed]",
        ))
        val diagram = assertIs<FlowchartDiagram>(parsed.diagram)
        assertEquals(FlowNode("B", "Process", FlowNodeShape.ROUNDED), diagram.nodes.first { it.id == "B" })
        assertEquals("Renamed", diagram.nodes.first { it.id == "C" }.label)
        val redeclared = assertIs<MermaidParseResult.Success>(MermaidParser.parse("flowchart TD\nA --> B\nB{Decision}"))
        assertEquals(FlowNodeShape.DIAMOND, assertIs<FlowchartDiagram>(redeclared.diagram).nodes.first { it.id == "B" }.shape)
    }

    @Test
    fun sequenceMessagesRequireAColonEvenForAnEmptyLabel() {
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("sequenceDiagram; A-->B"))
        assertIs<MermaidParseResult.Success>(MermaidParser.parse("sequenceDiagram; A-->B:"))
    }

    @Test
    fun acceptsOfficialGalleryHeaderAliases() {
        val aliases = listOf(
            "block-beta\ncolumns 2\nA[\"Client\"]\nB[\"Server\"]\nA --> B",
            "fishbone\nEffect\n  Cause",
            "packet-beta\n0-7: \"Header\"",
            "sankey-beta\nSolar,Grid,40",
            "usecaseDiagram\nactor User\nRender(\"Render\")\nUser --> Render",
            "cynefin\nclear\n\"Known fix\"",
        )
        aliases.forEach { source ->
            assertIs<MermaidParseResult.Success>(MermaidParser.parse(source), source)
        }
    }

    @Test
    fun parsesIshikawaIndentationHierarchyLikeTheOfficialGrammar() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                "ishikawa-beta\n" +
                    "    Blurry Photo\n" +
                    "    Process\n" +
                    "        Out of focus\n" +
                    "        Shutter speed too slow\n" +
                    "    Equipment\n" +
                    "        LENS\n" +
                    "            Dirty lens\n",
            ),
        )
        val diagram = assertIs<IshikawaDiagram>(result.diagram)
        assertEquals("Blurry Photo", diagram.effect.text)
        assertEquals(listOf("Process", "Equipment"), diagram.effect.children.map { it.text })
        assertEquals(listOf("Out of focus", "Shutter speed too slow"), diagram.effect.children[0].children.map { it.text })
        assertEquals("Dirty lens", diagram.effect.children[1].children[0].children.single().text)
    }

    @Test
    fun ishikawaAcceptsBareHeaderEffectDeeperThanCausesAndComments() {
        // Official sparse grammar: `ishikawa` without -beta, an effect indented
        // more than its causes, and %% comment/blank lines are ignored.
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                "ishikawa\n" +
                    "    Problem\n" +
                    "%% a comment line\n" +
                    "Cause A\n" +
                    "\n" +
                    "  Subcause A1\n",
            ),
        )
        val diagram = assertIs<IshikawaDiagram>(result.diagram)
        assertEquals("Problem", diagram.effect.text)
        assertEquals("Cause A", diagram.effect.children.single().text)
        assertEquals("Subcause A1", diagram.effect.children.single().children.single().text)
    }

    @Test
    fun ishikawaStackPopPathRunsOnRepeatedSiblingClosure() {
        // Regression tooth for the JDK17 portability blocker: closing sibling
        // subtrees must repeatedly unwind the cause stack (the former Java-21
        // List.removeLast() call site). Each "Cause N" closes the previous
        // nested chain and reattaches at cause level.
        val source = buildString {
            appendLine("ishikawa-beta")
            appendLine("Problem")
            repeat(4) { cause ->
                appendLine("Cause $cause")
                appendLine("    Sub $cause-1")
                appendLine("        Leaf $cause")
            }
        }
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse(source))
        val diagram = assertIs<IshikawaDiagram>(result.diagram)
        assertEquals(listOf("Cause 0", "Cause 1", "Cause 2", "Cause 3"), diagram.effect.children.map { it.text })
        assertEquals("Leaf 0", diagram.effect.children[0].children[0].children.single().text)
        assertEquals(1, diagram.effect.children[1].children.size)
    }

    @Test
    fun malformedIshikawaFailsClosed() {
        listOf(
            "ishikawa\n",
            "ishikawa-beta\n%% only comments and blanks\n\n",
            "IshikawaBeta\nProblem\nCause A",
            "ishikawa-v2\nProblem\nCause A",
            "ishikawa-beta extra\nProblem\nCause A",
            "ishikawa-beta\nProblem\n\tTabbed cause",
            "ishikawa-beta\nProblem\nCause A\n\tSub with tab",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }


    @Test
    fun parsesEventModelingCompactRelaxedResetAndExplicitRelations() {
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("eventmodeling\ntf 01 ui CartUI\ntimeframe 02 command AddItem\ntf 03 evt ItemAdded\nresetframe 04 event External.InventoryChanged\ntf 05 readmodel InventoryView ->> 03 ->> 04"))
        val diagram = assertIs<EventModelingDiagram>(result.diagram)
        assertEquals(null, diagram.title)
        assertEquals(listOf("01", "02", "03", "04", "05"), diagram.frames.map { it.id })
        assertEquals(EventModelingEntityKind.READ_MODEL, diagram.frames.last().kind)
        assertTrue(diagram.frames[3].reset)
        assertEquals(
            listOf(EventModelingRelation("01", "02"), EventModelingRelation("02", "03"), EventModelingRelation("03", "05"), EventModelingRelation("04", "05")),
            diagram.relations,
        )
    }

    @Test
    fun malformedEventModelingFailsClosed() {
        listOf(
            "eventmodeling",
            "EventModeling\ntf 01 ui Cart",
            "eventmodeling; tf 01 ui Cart",
            "eventmodeling\ntf 01 ui Cart\ntf 01 evt Duplicate",
            "eventmodeling\ntf 01 rmo View ->> 99",
            "eventmodeling\ntf 1000 ui Cart",
            "eventmodeling\ntf 01 unknown Cart",
            "eventmodeling\ntf 01 ui Cart { value: string }",
            "eventmodeling\ndata Cart {",
            "eventmodeling\naccTitle: deferred",
            "eventmodeling\ntitle Cart inventory\ntf 01 ui Cart",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }

    @Test
    fun parsesRailroadExpressionTree() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                """
                railroad-beta
                title "Auth"
                auth = sequence(
                  terminal("token"),
                  choice(
                    sequence(terminal("user"), terminal("password")),
                    nonterminal("oauth")
                  ),
                  optional(terminal("mfa"))
                );
                """.trimIndent(),
            ),
        )
        assertEquals(
            RailroadDiagram(
                title = "Auth",
                rules = listOf(
                    RailroadRule(
                        name = "auth",
                        definition = RailroadSequence(
                            listOf(
                                RailroadTerminal("token"),
                                RailroadChoice(
                                    listOf(
                                        RailroadSequence(listOf(RailroadTerminal("user"), RailroadTerminal("password"))),
                                        RailroadNonTerminal("oauth"),
                                    ),
                                ),
                                RailroadOptional(RailroadTerminal("mfa")),
                            ),
                        ),
                    ),
                ),
            ),
            result.diagram,
        )
    }

    @Test
    fun parsesRailroadRepeatSymbols() {
        val parsed = MermaidParser.parse(
            """
            railroad-beta
            loop = oneOrMore(zeroOrMore(terminal("a")));
            mark = special("EOF");
            """.trimIndent(),
        )
        val result = assertIs<MermaidParseResult.Success>(
            parsed,
            (parsed as? MermaidParseResult.Failure)?.diagnostics.toString(),
        )
        assertEquals(
            RailroadDiagram(
                rules = listOf(
                    RailroadRule(
                        name = "loop",
                        definition = RailroadOneOrMore(RailroadZeroOrMore(RailroadTerminal("a"))),
                    ),
                    RailroadRule(
                        name = "mark",
                        definition = RailroadSpecial("EOF"),
                    ),
                ),
            ),
            result.diagram,
        )
    }

    @Test
    fun malformedRailroadFailsClosed() {
        listOf(
            "railroad\nauth = terminal(\"a\");",
            "railroad-beta\nDiagram(sequence(terminal(\"a\")));",
            "railroad-beta\nauth = Sequence(terminal(\"a\"));",
            "railroad-beta\nauth = terminal(\"a\")",
            "railroad-beta\nauth = choice();",
            "railroad-beta\nauth = sequence();",
            "railroad-beta\nauth = terminal();",
            "railroad-beta\nauth = terminal(5);",
            "railroad-beta\nauth = terminal(\"unterminated);",
            "railroad-beta\nauth = Stack(terminal(\"a\"));",
            "railroad-beta\ntitle = terminal(\"a\");",
            "railroad-beta\nauth = special();",
            "railroad-beta\nauth = sequence(terminal(\"a\")); extra",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }

    @Test
    fun parsesZenumlInteractions() {
        val parsed = MermaidParser.parse(
            "zenuml\n" +
                "    title Token handshake\n" +
                "    Client\n" +
                "    Store as \"Token store\"\n" +
                "    Client->Gateway.submit()\n" +
                "    Gateway->Store.lookup\n" +
                "    Client->Gateway: cancel",
        )
        val result = assertIs<MermaidParseResult.Success>(
            parsed,
            (parsed as? MermaidParseResult.Failure)?.diagnostics.toString(),
        )
        assertEquals(
            ZenumlDiagram(
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
            result.diagram,
        )
    }

    @Test
    fun zenumlAliasesRequireQuotesForMultipleWords() {
        listOf("Token store", "\"Token store", "Token store\"").forEach { alias ->
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("zenuml\nStore as $alias\nClient->Store.lookup()"))
        }
        val parsed = assertIs<MermaidParseResult.Success>(MermaidParser.parse("zenuml\nStore as TokenStore\nClient->Store.lookup()"))
        assertEquals("TokenStore", assertIs<ZenumlDiagram>(parsed.diagram).participants.first().label)
    }

    @Test
    fun malformedZenumlFailsClosed() {
        listOf(
            // No statements at all beyond the header.
            "zenuml",
            // Declarations alone are not an interaction.
            "zenuml\nA\nB",
            // Sync messages need a method name.
            "zenuml\nA->B",
            // Sync message parentheses must be empty or absent.
            "zenuml\nA->B.submit(token)",
            "zenuml\nA->B.submit(",
            // Bare receiver-only sync calls are outside the slice.
            "zenuml\nA.submit()",
            // Nested bodies are not supported.
            "zenuml\nA.submit() {\n  B.handle()\n}",
            // Async labels must be non-empty.
            "zenuml\nA->B:",
            "zenuml\nA->B:   ",
            // Dashed arrows are sequence-diagram syntax, not this slice.
            "zenuml\nA-->B: hi",
            // Creation, assignment, and return forms are rejected.
            "zenuml\nnew Client\nA->B.go()",
            "zenuml\nx = A.submit()",
            "zenuml\nToken x = A.submit()",
            "zenuml\nreturn ok",
            // Annotators are rejected.
            "zenuml\n@Actor Alice\nA->B.go()",
            // Comments are rejected.
            "zenuml\n// hello\nA->B.go()",
            // Control flow is rejected.
            "zenuml\nif (ok) { }\nA->B.go()",
            // Duplicate conflicting alias declarations are rejected.
            "zenuml\nStore as One\nStore as Two\nA->B.go()",
            // At most one title.
            "zenuml\ntitle One\ntitle Two\nA->B.go()",
            // Unknown statement forms fail closed.
            "zenuml\nA -> B -> C",
            "zenuml\nA->B.go() extra",
            "zenuml\n\"quoted participant\"\nA->B.go()",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }

    @Test
    fun parsesWardleyMap() {
        val parsed = MermaidParser.parse(
            "wardley-beta\n" +
                "title Tea Shop\n" +
                "anchor Business [0.95, 0.63]\n" +
                "component Cup of Tea [0.79, 0.61]\n" +
                "component real-time processing [0.40, 0.30]\n" +
                "Business -> Cup of Tea\n" +
                "Cup of Tea -> real-time processing\n" +
                "evolve real-time processing 0.75\n" +
                "note \"keep it simple\" [0.5, 0.5]",
        )
        val result = assertIs<MermaidParseResult.Success>(
            parsed,
            (parsed as? MermaidParseResult.Failure)?.diagnostics.toString(),
        )
        assertEquals(
            WardleyMapDiagram(
                title = "Tea Shop",
                nodes = listOf(
                    WardleyNode("Business", 0.95, 0.63, anchor = true),
                    WardleyNode("Cup of Tea", 0.79, 0.61, anchor = false),
                    WardleyNode("real-time processing", 0.40, 0.30, anchor = false),
                ),
                links = listOf(
                    WardleyLink("Business", "Cup of Tea"),
                    WardleyLink("Cup of Tea", "real-time processing"),
                ),
                evolutions = listOf(WardleyEvolution("real-time processing", 0.75)),
                notes = listOf(WardleyNote("keep it simple", 0.5, 0.5)),
            ),
            result.diagram,
        )
    }

    @Test
    fun malformedWardleyFailsClosed() {
        listOf(
            // No nodes at all.
            "wardley-beta",
            "wardley-beta\ntitle Only",
            // Quoted names are outside the slice.
            "wardley-beta\ncomponent \"Custom Service\" [0.5, 0.5]",
            // Decorators, label offsets, and size are rejected.
            "wardley-beta\ncomponent API [0.5, 0.5] (build)",
            "wardley-beta\ncomponent API [0.5, 0.5] label [-50, 10]",
            "wardley-beta\nsize [800, 1000]",
            // Non-basic link styles are rejected.
            "wardley-beta\ncomponent A [0.1, 0.1]\ncomponent B [0.2, 0.2]\nA --> B",
            "wardley-beta\ncomponent A [0.1, 0.1]\ncomponent B [0.2, 0.2]\nA +> B",
            "wardley-beta\ncomponent A [0.1, 0.1]\ncomponent B [0.2, 0.2]\nA -.-> B",
            // Unknown endpoints and self links are rejected.
            "wardley-beta\ncomponent A [0.1, 0.1]\nA -> Ghost",
            "wardley-beta\ncomponent A [0.1, 0.1]\nA -> A",
            // Coordinates must be decimal literals in [0, 1].
            "wardley-beta\ncomponent A [1.5, 0.5]",
            "wardley-beta\ncomponent A [0.5, NaN]",
            "wardley-beta\ncomponent A [1e-1, 0.5]",
            "wardley-beta\ncomponent A [.5, 0.5]",
            "wardley-beta\ncomponent A [0.5]",
            "wardley-beta\ncomponent A [0.5, 0.5, 0.5]",
            // evolve rules.
            "wardley-beta\ncomponent A [0.5, 0.5]\nevolve Ghost 0.7",
            "wardley-beta\ncomponent A [0.5, 0.5]\nevolve A 1.5",
            "wardley-beta\ncomponent A [0.5, 0.5]\nevolve A 0.7\nevolve A 0.8",
            // Pipelines, custom stages, annotations, forces are rejected.
            "wardley-beta\npipeline Database {\n  component SQL [0.5]\n}",
            "wardley-beta\nevolution A -> B",
            "wardley-beta\nannotations [0.1, 0.9]",
            "wardley-beta\naccelerator \"AI\" [0.5, 0.5]",
            // Duplicate names, including anchor/component collisions.
            "wardley-beta\ncomponent A [0.5, 0.5]\ncomponent A [0.4, 0.4]",
            "wardley-beta\nanchor A [0.5, 0.5]\ncomponent A [0.4, 0.4]",
            // Unknown statements fail closed.
            "wardley-beta\ntrend A -.- (0.5, 0.5)",
            "wardley-beta\nA -> B -> C",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }

    @Test
    fun parsesTreeViewIndentationQuotedLabelsAndDirectories() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse("treeView-beta\n    project/\n        src/\n            index.ts\n        \"README file.md\"")
        )
        assertEquals(
            TreeViewDiagram(
                listOf(
                    TreeViewNode("project", 0, null, true, sourceIndent = 4),
                    TreeViewNode("src", 1, 0, true, sourceIndent = 8),
                    TreeViewNode("index.ts", 2, 1, false, sourceIndent = 12),
                    TreeViewNode("README file.md", 1, 0, false, sourceIndent = 8),
                ),
            ),
            result.diagram,
        )
    }

    @Test
    fun malformedTreeViewFailsClosed() {
        listOf("treeView-beta\n    \"unterminated", "treeView-beta\nfile icon(bad value)", "treeView-beta\n:::highlight", "treeView-beta\naccDescr { missing close").forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }

    @Test
    fun parsesSwimlanesLanesShapesAndLabeledEdgeChains() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                "swimlane-beta LR\nsubgraph customer [Customer team]\nstart([Start])\nrequest[Request & review]\nend\nsubgraph Support\ntriage{Known?}\nanswer((Done))\nend\nstart --> request -->|handoff| triage\ntriage --> answer",
            ),
        )
        assertEquals(
            SwimlaneDiagram(
                FlowDirection.LR,
                listOf(
                    Swimlane("customer", "Customer team", listOf(SwimlaneNode("start", "Start", SwimlaneNodeShape.STADIUM), SwimlaneNode("request", "Request & review", SwimlaneNodeShape.RECTANGLE))),
                    Swimlane("Support", "Support", listOf(SwimlaneNode("triage", "Known?", SwimlaneNodeShape.DECISION), SwimlaneNode("answer", "Done", SwimlaneNodeShape.CIRCLE))),
                ),
                listOf(SwimlaneEdge("start", "request"), SwimlaneEdge("request", "triage", "handoff"), SwimlaneEdge("triage", "answer")),
            ),
            assertIs<SwimlaneDiagram>(result.diagram).copy(flowchart = null),
        )
    }

    @Test
    fun malformedSwimlanesFailClosed() {
        listOf(
            "swimlane-beta ZZ\nsubgraph A\na[One]\nend",
            "swimlane-beta",
            "swimlane-beta\nend",
            "swimlane-beta\nsubgraph A\nend",
            "swimlane-beta\nsubgraph A\na[One]",
            "swimlane-beta\nsubgraph A\nsubgraph A\na[One]\nend\nend",
            "swimlane-beta\nA-->B\nclick A call broken(",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }

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
    fun acceptsOfficialEmptyFrameworkAndSparseEmptyDomains() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse("cynefin-beta\ncomplex\ncomplicated\nclear\nchaotic")
        )
        val diagram = assertIs<CynefinDiagram>(result.diagram)
        assertEquals(
            listOf(CynefinDomain.COMPLEX, CynefinDomain.COMPLICATED, CynefinDomain.CLEAR, CynefinDomain.CHAOTIC),
            diagram.domains.map { it.domain },
        )
        assertTrue(diagram.domains.all { it.items.isEmpty() })
    }

    @Test
    fun parsesRadarAxesCurvesTitleAndOptionalMax() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                "radar-beta\ntitle Team skill matrix\naxis m[\"Math\"], s[\"Science\"], e[\"English\"]\n" +
                    "curve alice[\"Alice\"]{85, 78, 92}\ncurve bob{62, 84, 55}\nmax 100",
            ),
        )
        assertEquals(
            RadarChartDiagram(
                title = "Team skill matrix",
                axes = listOf(RadarAxis("m", "Math"), RadarAxis("s", "Science"), RadarAxis("e", "English")),
                curves = listOf(
                    RadarCurve("alice", "Alice", listOf(85.0, 78.0, 92.0)),
                    RadarCurve("bob", "bob", listOf(62.0, 84.0, 55.0)),
                ),
                maximum = 100.0,
                options = listOf(RadarOption("max", number = 100.0)),
            ),
            result.diagram,
        )
    }

    @Test
    fun parsesRadarGallerySeriesSyntax() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse("radar-beta\ntitle Team skills\naxis Docs,Code,UX\ncurve Team{8,7,6}"),
        )
        val diagram = assertIs<RadarChartDiagram>(result.diagram)
        assertEquals(3, diagram.axes.size)
        assertEquals(listOf(8.0, 7.0, 6.0), diagram.curves.single().values)
    }

    @Test
    fun radarDefaultsToMax100AndAcceptsSplitAxisStatements() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse("radar-beta\naxis a, b, c\naxis d[\"Depth\"], a2[\"Depth\"], f\ncurve one{1, 2, 3, 4, 5, 6}")
        )
        val diagram = assertIs<RadarChartDiagram>(result.diagram)
        assertEquals(listOf("a", "b", "c", "d", "a2", "f"), diagram.axes.map { it.id })
        assertEquals("Depth", diagram.axes[3].label)
        // The same label under different ids is allowed.
        assertEquals("Depth", diagram.axes[4].label)
        assertEquals("a2", diagram.axes[4].id)
        assertEquals(100.0, diagram.maximum)
    }

    @Test
    fun malformedRadarDiagnosticCarriesLineAndColumn() {
        val result = assertIs<MermaidParseResult.Failure>(
            MermaidParser.parse("radar-beta\naxis a, b, c\ncurve x{1, two, 3}")
        )
        val diagnostic = result.diagnostics.single()
        assertEquals(3, diagnostic.location.line)
        assertEquals(12, diagnostic.location.column)
    }

    @Test
    fun radarRetainsNamedEntriesOptionsAndInlineMetadata() {
        val source = """
            radar-beta: title Measurements
            accTitle: Radar access
            accDescr { Detailed
              description }
            axis a["A, quoted"], b
            curve one { b: 20, a 50 }, two { 1, 2 }
            min 10, max 50, ticks 4, showLegend false, graticule polygon
        """.trimIndent()
        val chart = assertIs<RadarChartDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
        assertEquals("Measurements", chart.title)
        assertEquals("Radar access", chart.accessibilityTitle)
        assertEquals("Detailed\ndescription", chart.accessibilityDescription)
        assertEquals("A, quoted", chart.axes.first().label)
        assertEquals(listOf(RadarEntry(20.0, "b"), RadarEntry(50.0, "a")), chart.curves.first().entries)
        assertEquals(10.0, chart.minimum)
        assertEquals(50.0, chart.maximum)
        assertEquals(false, chart.options.first { it.name == "showLegend" }.flag)
    }

    @Test
    fun radarAllowsEmptyDocumentsAndGrammarWithoutRenderingRestrictions() {
        listOf(
            "radar-beta", "radar-beta :", "radar-beta\naxis a, a",
            "radar-beta\naxis a, b\ncurve x{1}, x{2, 3, 4}",
            "radar-beta\naxis a\ncurve x{60}\nmax 50",
            "radar-beta\naxis a[\"\"]\ncurve x{0}\nmax 0",
        ).forEach { assertIs<MermaidParseResult.Success>(MermaidParser.parse(it), it) }
    }

    @Test
    fun malformedRadarFailsClosed() {
        listOf(
            "RadarBeta\naxis a, b, c",
            "radar-beta\naxis a, b, c\ncurve x{-1, 2, 3}",
            "radar-beta\naxis a, b, c\ncurve x{1, two, 3}",
            "radar-beta\naxis a, b, c\nmax -5",
            "radar-beta\nmax abc", "radar-beta\nmax 1e2",
            "radar-beta\nmax +50", "radar-beta\nmax .5", "radar-beta\nmax 5.",
            "radar-beta\ncurve x{-0, 2, 3}", "radar-beta\ncurve x{NaN, 2, 3}",
            "radar-beta\ncurve x{Infinity, 2, 3}",
            "radar-beta\ncurve x{1,,3}", "radar-beta\ncurve x{1, 2, 3,}",
            "radar-beta\naxis m\"Math\", s, e", "radar-beta\naxis m[\"Math, s, e",
            "radar-beta\ncurve alice[\"Alice\"] 1, 2, 3", "radar-beta\ncurve alice{1, 2, 3",
            "radar-beta\naxis a,,b,c", "radar-beta\ncurve x 1, 2, 3",
            "radar-beta\nstyle x fill:red", "radar-beta\nshowLegend maybe",
            "radar-beta\ngraticule square", "radar-beta\naccDescr {unclosed",
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
    fun xyChartPreservesOriginalMetadataRangesAndPointLabels() {
        val chart = assertIs<XyChartDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "xychart horizontal\ntitle \" Revenue \"\nx-axis \"Time\" 45.5 --> .34\ny-axis \"  Value  \"\nline \"Trend\" [10 \"First\", 20, 30 \"Last\"]\naccTitle: Sales\naccDescr {First line\n  second line}",
        )).diagram)
        assertEquals(XyOrientation.HORIZONTAL, chart.orientation)
        assertEquals("Revenue", chart.title)
        assertEquals(45.5, chart.xAxis.range?.minimum)
        assertEquals(0.34, chart.xAxis.range?.maximum)
        assertEquals("  Value  ", chart.yAxis.title)
        assertEquals(false, chart.yAxis.explicitRange)
        assertEquals(10.0, chart.yAxis.minimum)
        assertEquals("Trend", chart.series.single().title)
        assertEquals(listOf("First", "", "Last"), chart.series.single().labels)
        assertEquals("Sales", chart.accessibilityTitle)
        assertEquals("First line\n  second line", chart.accessibilityDescription)
    }

    @Test
    fun xyChartAcceptsPartialChartsAndPreservesQuotedCategorySpaces() {
        assertIs<MermaidParseResult.Success>(MermaidParser.parse("xychart"))
        val chart = assertIs<XyChartDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "xychart\nx-axis [first category, \" second category \"]\nbar [1]",
        )).diagram)
        assertEquals(listOf("firstcategory", " second category "), chart.xAxis.categories)
        assertEquals(listOf(1.0), chart.series.single().values)
    }

    @Test
    fun xyChartKeepsMarkdownTypesAndQuotedStatementSeparators() {
        val chart = assertIs<XyChartDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "xychart-beta; x-axis \"`**Axis**`\" [\"a;b\", \"`**b**`\"]; bar \"`**Series**`\" [1,2]",
        )).diagram)
        assertEquals("markdown", chart.xAxis.titleType)
        assertEquals(listOf("text", "markdown"), chart.xAxis.categoryTypes)
        assertEquals("a;b", chart.xAxis.categories.first())
        assertEquals("markdown", chart.series.single().titleType)
    }

    @Test
    fun malformedXyChartFailsClosed() {
        listOf(
            "xychart-beta\nx-axis [A, B]\ny-axis 0 --> 10\nline []",
            "xychart-beta\nx-axis [A]\ny-axis bad --> 0\nline [1]",
            "xychart-beta\nx-axis [A]\ny-axis 0 --> 10\nline [nope]",
            "xychart-beta\nx-axis [A]\ny-axis 0 --> 10\nline [NaN]",
            "xychart-beta\nx-axis [A]\ny-axis 0 --> 10\nline [Infinity]",
            "xychart-beta\nx-axis [A]\ny-axis 0 --> 10\nline [-Infinity]",
            "xychart-beta\nx-axis [A]\ny-axis 0 --> 10\nline [+Infinity]",
            "xychart-beta\nx-axis [A]\ny-axis 0 --> 10\nline [11 extra]",
            "xychart-beta\nx-axis [A]\ny-axis [0, 10]",
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
            MermaidParser.parse("stateDiagram-v2\nA --> B\nstate \"unclosed"),
        )

        assertEquals(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, failure.diagnostics.single().code)
        assertEquals(SourceLocation(line = 3, column = 1), failure.diagnostics.single().location)
    }

    @Test
    fun stateSupportsNotesDescriptionsCompositesAndPseudoStates() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                """
                stateDiagram-v2
                  direction LR
                  state Working : handles requests
                  state Block {
                    A --> B
                    B --> C
                  }
                  state c <<choice>>
                  note left of Working : pinned note
                  Idle --> Working
                  Working --> c
                """.trimIndent(),
            ),
        )
        val diagram = assertIs<StateDiagram>(result.diagram)

        assertTrue(diagram.states.isNotEmpty())
        assertEquals("handles requests", diagram.states.first { it.id == "Working" }.description)
        assertTrue(diagram.states.any { it.childIds.isNotEmpty() })
        assertEquals(StateNodeKind.CHOICE, diagram.states.first { it.id == "c" }.kind)
        assertEquals(1, diagram.notes.size)
        assertEquals(StateNotePosition.LEFT_OF, diagram.notes[0].position)
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
    fun parsesFlowchartThickArrow() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse("graph TD\nA ==> B"),
        )

        assertEquals(
            FlowchartDiagram(
                direction = FlowDirection.TD,
                nodes = listOf(FlowNode("A", "A"), FlowNode("B", "B")),
                edges = listOf(FlowEdge("A", "B", FlowEdgeStyle.THICK)),
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
    fun flowchartChainedEdgesPipeLabelsSubgraphsAndShapes() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                """
                flowchart LR
                  subgraph cluster[Pipeline]
                    A[Start] -->|first| B(Process)
                    B --> C{Diamond}
                  end
                  C -->|done| D((Circle))
                  D -.-> E[End]
                """.trimIndent(),
            ),
        )
        val diagram = assertIs<FlowchartDiagram>(result.diagram)

        assertTrue(diagram.edges.size >= 4)
        assertEquals("first", diagram.edges[0].label)
        assertEquals("done", diagram.edges[2].label)
        assertEquals(FlowEdgeStyle.DOTTED, diagram.edges[3].style)
        assertEquals(FlowNodeShape.RECTANGLE, diagram.nodes.first { it.id == "A" }.shape)
        assertEquals(FlowNodeShape.ROUNDED, diagram.nodes.first { it.id == "B" }.shape)
        assertEquals(FlowNodeShape.DIAMOND, diagram.nodes.first { it.id == "C" }.shape)
        assertEquals(FlowNodeShape.CIRCLE, diagram.nodes.first { it.id == "D" }.shape)
        assertEquals(1, diagram.subgraphs.size)
        assertEquals(setOf("A", "B", "C"), diagram.subgraphs[0].nodeIds.toSet())
    }

    @Test
    fun classRelationshipKindsLabelsAndCardinality() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                """
                classDiagram
                  Animal <|-- Dog
                  Car *-- Wheel
                  Bag o-- Item
                  User --> Order : places
                  Repo "1" --> "many" Commit
                  Service ..> Db
                """.trimIndent(),
            ),
        )
        val diagram = assertIs<ClassDiagram>(result.diagram)

        assertEquals(6, diagram.relationships.size)
        assertEquals(ClassRelationshipKind.INHERITANCE, diagram.relationships[0].kind)
        assertEquals(ClassRelationshipKind.COMPOSITION, diagram.relationships[1].kind)
        assertEquals(ClassRelationshipKind.AGGREGATION, diagram.relationships[2].kind)
        assertEquals(ClassRelationshipKind.ASSOCIATION, diagram.relationships[3].kind)
        assertEquals("places", diagram.relationships[3].label)
        assertEquals("1", diagram.relationships[4].fromCardinality)
        assertEquals("many", diagram.relationships[4].toCardinality)
        assertEquals(ClassRelationshipKind.DEPENDENCY, diagram.relationships[5].kind)
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

    @Test fun mindmapEmptyIconDecorationDoesNotReserveAnIcon() {
        val empty = assertIs<MindmapDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("mindmap\nroot(Root)\n::icon()")).diagram)
        assertEquals(null, empty.nodes.single().icon)
        val retained = assertIs<MindmapDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("mindmap\nroot(Root)\n::icon(star)\n::icon()")).diagram)
        assertEquals("star", retained.nodes.single().icon)
    }

    @Test fun mindmapArbitraryIndentationPreservesSourceIdsDecorationsAndShapes() {
        val diagram = assertIs<MindmapDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("""
            mindmap
            root(Root)
                  child["Quoted []"]
                  ::icon(star)
                  :::hot
                   cloud)Cloud(
                  boom))Burst((
                  hex{{Hexagon}}
        """.trimIndent())).diagram)
        assertEquals(listOf(0, 1, 2, 1, 1), diagram.nodes.map { it.depth })
        assertEquals("star", diagram.nodes[1].icon)
        assertEquals("hot", diagram.nodes[1].cssClasses)
        assertEquals("child", diagram.nodes[2].parentId)
        assertEquals(MindmapNodeShape.CLOUD, diagram.nodes[2].shape)
        assertEquals(MindmapNodeShape.BANG, diagram.nodes[3].shape)
        assertEquals(MindmapNodeShape.HEXAGON, diagram.nodes[4].shape)
        val duplicate = assertIs<MindmapDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("mindmap\nroot\n a[One]\n a[Two]")).diagram)
        assertEquals(listOf("a", "a"), duplicate.nodes.drop(1).map { it.sourceId })
        assertEquals(3, duplicate.nodes.map { it.id }.distinct().size)
    }

    @Test
    fun malformedMindmapIndentationAndMultipleRootsFailClosed() {
        listOf(
            "mindmap\n  root((Root))\n  Other",
            "mindmap\n  root((Root))\n\tChild",
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
    fun acceptsEmptySequenceMessageWithRequiredColon() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse("sequenceDiagram; A->>B:"),
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
    fun sequenceSupportsDeclarationsNotesActivationsAndArrowVariants() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                """
                sequenceDiagram
                  participant A as Alice
                  actor B as Bob
                  autonumber
                  A->B: no-arrow solid
                  B-->A: no-arrow dashed
                  A-xB: solid cross
                  B--xA: dashed cross
                  A-)B: solid open
                  B--)A: dashed open
                  Note left of A: pinned note
                  Note over A,B: shared note
                  activate A
                  A->>B: request
                  deactivate A
                """.trimIndent(),
            ),
        )
        val diagram = assertIs<SequenceDiagram>(result.diagram)

        assertEquals(setOf("A", "B"), diagram.actors.map { it.id }.toSet())
        assertEquals("Alice", diagram.actors.first { it.id == "A" }.label)
        assertEquals("Bob", diagram.actors.first { it.id == "B" }.label)
        assertEquals(7, diagram.messages.size)
        assertEquals(SequenceArrowHead.NONE, diagram.messages[0].arrowHead)
        assertEquals(SequenceLineStyle.SOLID, diagram.messages[0].lineStyle)
        assertEquals(SequenceArrowHead.NONE, diagram.messages[1].arrowHead)
        assertEquals(SequenceLineStyle.DASHED, diagram.messages[1].lineStyle)
        assertEquals(SequenceArrowHead.CROSS, diagram.messages[2].arrowHead)
        assertEquals(SequenceArrowHead.CROSS, diagram.messages[3].arrowHead)
        assertEquals(SequenceArrowHead.OPEN, diagram.messages[4].arrowHead)
        assertEquals(SequenceArrowHead.OPEN, diagram.messages[5].arrowHead)
        assertEquals(2, diagram.notes.size)
        assertEquals(SequenceNotePosition.LEFT_OF, diagram.notes[0].position)
        assertEquals(listOf("A", "B"), diagram.notes[1].actorIds)
        assertEquals(2, diagram.activations.size)
        assertTrue(diagram.activations[0].activate)
        assertFalse(diagram.activations[1].activate)
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
    fun classMemberBlockCollectsVisibilityMarkedMembers() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                """
                classDiagram
                class Animal {
                  +String name
                  +speak()
                  -int age
                }
                Animal <|-- Duck
                """.trimIndent(),
            ),
        )
        val diagram = assertIs<ClassDiagram>(result.diagram)

        val animal = diagram.classes.first { it.id == "Animal" }
        assertEquals(3, animal.members.size)
        assertEquals("String name", animal.members[0].signature)
        assertEquals(ClassVisibility.PUBLIC, animal.members[0].visibility)
        assertEquals("speak()", animal.members[1].signature)
        assertEquals(ClassVisibility.PRIVATE, animal.members[2].visibility)
        assertEquals(ClassRelationshipKind.INHERITANCE, diagram.relationships.single().kind)
    }

    @Test
    fun classMemberBlockFailsClosedOnNestedBody() {
        val failure = assertIs<MermaidParseResult.Failure>(
            MermaidParser.parse("classDiagram\nclass A {\nclass B {\n}\n}"),
        )

        assertEquals(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, failure.diagnostics.single().code)
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
    fun entityAttributesSupportQuotedComments() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                """
                erDiagram
                  CUSTOMER {
                    int id PK "primary key"
                    string name "display name"
                  }
                """.trimIndent(),
            ),
        )
        val diagram = assertIs<EntityRelationshipDiagram>(result.diagram)

        assertEquals("primary key", diagram.entities[0].attributes[0].comment)
        assertEquals("display name", diagram.entities[0].attributes[1].comment)
    }

    @Test
    fun malformedFlowchartHeaderHasTypedDiagnostic() {
        val failure = assertIs<MermaidParseResult.Failure>(MermaidParser.parse("flowchart SIDEWAYS"))

        assertEquals(MermaidDiagnosticCode.INVALID_HEADER, failure.diagnostics.single().code)
    }

    @Test
    fun unsupportedBodySyntaxFailsWithoutPartialSuccess() {
        val failure = assertIs<MermaidParseResult.Failure>(
            MermaidParser.parse("flowchart TD\nA-->B\nclick A call broken("),
        )

        assertEquals(MermaidDiagnosticCode.UNSUPPORTED_SYNTAX, failure.diagnostics.single().code)
        assertEquals(SourceLocation(line = 3, column = 1), failure.diagnostics.single().location)
    }

    @Test
    fun semicolonDiagnosticReportsPhysicalColumn() {
        val failure = assertIs<MermaidParseResult.Failure>(
            MermaidParser.parse("flowchart TD; A-->B; click A callback invalid"),
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
            "gantt\ndateFormat YYYY-MM-DD\nTask :id, 2026-08-19, invalid",
        ).forEach { assertIs<MermaidParseResult.Failure>(MermaidParser.parse(it), it) }
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("gantt\ndateFormat YYYY-MM-DD\nsection Build\nTask :blocked, id, 2026-08-19, 2d"))
    }

    @Test
    fun ganttSupportsAfterDependsMilestoneAndAxisDirectives() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                """
                gantt
                  title Sprint
                  dateFormat YYYY-MM-DD
                  axisFormat %m-%d
                  excludes weekends
                  section Phase 1
                  Design :done, design, 2026-08-19, 3d
                  Build :after design, 2d
                  Ship :milestone, ship, 2026-08-24, 0d
                """.trimIndent(),
            ),
        )
        val diagram = assertIs<GanttDiagram>(result.diagram)

        assertEquals(3, diagram.sections.single().tasks.size)
        // A Saturday computed end advances through the excluded weekend to Monday.
        assertEquals(5, diagram.sections.single().tasks[0].durationDays)
        assertEquals(2, diagram.sections.single().tasks[1].durationDays)
        assertEquals(0, diagram.sections.single().tasks[2].durationDays)
        assertEquals(GanttTaskStatus.TODO, diagram.sections.single().tasks[2].status)
        assertTrue(diagram.sections.single().tasks[2].milestone)
    }

    @Test
    fun parsesTimelinePeriodsAndMultipleLabels() {
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("""
            timeline
              title Product history
              2024 : Launch : First users
              2025 : Scale
        """.trimIndent()))
        assertEquals(TimelineDiagram("Product history", listOf(TimelineEvent("2024 ", listOf("Launch ", "First users")), TimelineEvent("2025 ", listOf("Scale")))), result.diagram)
    }

    @Test
    fun timelineKeepsEmptySectionsUrlsContinuationAndOriginalWhitespace() {
        val chart = assertIs<TimelineDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "timeline TD\nsection empty\nsection Phase\n2024: [Link](http://example.com): Extra \n  : Continued\n2025\nsection empty",
        )).diagram)
        assertEquals(FlowDirection.TB, chart.direction)
        assertEquals(true, chart.directionExplicit)
        assertEquals(listOf("empty", "Phase", "empty"), chart.sections)
        assertEquals(listOf("[Link](http://example.com)", "Extra ", "Continued"), chart.events.first().labels)
        assertEquals(1, chart.events.first().sectionIndex)
        assertEquals(emptyList(), chart.events.last().labels)
    }

    @Test
    fun timelinePreservesMetadataAndAcceptsEmptyDocument() {
        assertIs<MermaidParseResult.Success>(MermaidParser.parse("timeline"))
        val chart = assertIs<TimelineDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "timeline LR\ntitle ;title;\naccTitle: History\naccDescr {Line one\n  line two}\nsection #phase#\n2024: #event# ; ",
        )).diagram)
        assertEquals(";title;", chart.title)
        assertEquals("History", chart.accessibilityTitle)
        assertEquals("Line one\n  line two", chart.accessibilityDescription)
        assertEquals(listOf("#event# ; "), chart.events.single().labels)
    }

    @Test
    fun timelineUsesTokenSpecificCommentAndSectionBoundaries() {
        val chart = assertIs<TimelineDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "timeline\ntitle URL %% encoded\n2023: Event %% retained\nsection Q1: plan\n20#24: ignored\n2025: #event#",
        )).diagram)
        assertEquals("URL %% encoded", chart.title)
        assertEquals(listOf("Q1"), chart.sections)
        assertEquals(listOf("Event %% retained", "plan"), chart.events.first().labels)
        assertEquals(listOf("2023", "20", "2025"), chart.events.map { it.period })
        assertEquals(emptyList(), chart.events[1].labels)
        assertEquals(listOf("#event#"), chart.events.last().labels)
    }

    @Test
    fun malformedTimelineFailsClosed() {
        listOf("timeline RL", "timeline\n: Event without period", "timeline\n2024 :", "timeline\n2024 : Launch : ", "timeline\naccDescr {unclosed")
            .forEach { assertIs<MermaidParseResult.Failure>(MermaidParser.parse(it), it) }
        val comma = assertIs<MermaidParseResult.Success>(MermaidParser.parse("timeline\n2024 : Launch, First users"))
        assertEquals(listOf("Launch, First users"), assertIs<TimelineDiagram>(comma.diagram).events.single().labels)
    }

    @Test
    fun quadrantOriginalOptionalAxesAndQuotedStylesAreRetained() {
        val empty = assertIs<QuadrantChartDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("quadrantChart")).diagram)
        assertFalse(empty.xAxis.lowDefined)
        val source = "quadrantChart\nx-axis Low -->\nquadrant-1 First\nquadrant-1 Last  \nclassDef constructor radius:10,color:#ff0000\n\"产品 [A]\":::constructor: [0.20, 1] stroke-width:2px"
        val parsed = assertIs<QuadrantChartDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
        assertEquals("Low ⟶ ", parsed.xAxis.lowLabel)
        assertFalse(parsed.xAxis.highDefined)
        assertEquals("Last  ", parsed.quadrantLabels[0])
        assertEquals("constructor", parsed.points.single().className)
        assertEquals("0.20", parsed.points.single().sourceX)
        assertEquals(listOf("radius:10", "color:#ff0000"), parsed.classes["constructor"])
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
                listOf(QuadrantPoint("Campaign A", 0.3, 0.6), QuadrantPoint("Campaign B", 1.0, 0.0, sourceX = "1", sourceY = "0")),
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
                    id: "AUTH-1"
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
            "quadrantChart\nx-axis Low --> High\ny-axis Low --> High\nCampaign: [1.1, 0.3]",
            "quadrantChart\nx-axis Low --> High\ny-axis Low --> High\nCampaign: [NaN, 0.3]",
            "quadrantChart\nx-axis Low --> High\ny-axis Low --> High\nclick Campaign\nCampaign: [0.2, 0.3]",
        ).forEach { assertIs<MermaidParseResult.Failure>(MermaidParser.parse(it), it) }
    }

    @Test
    fun journeySupportsEmptySectionsOptionalActorsAndAccessibility() {
        val chart = assertIs<UserJourneyDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "journey\naccDescr { First line\n  second line }title Sample\naccTitle: Access\nsection Empty\nsection Work\nA: 5\nB: 3:\nC: 4: Alice, Bob",
        )).diagram)
        assertEquals("Sample", chart.title)
        assertEquals("Access", chart.accessibilityTitle)
        assertEquals("First line\nsecond line", chart.accessibilityDescription)
        assertEquals(emptyList(), chart.sections.first().tasks)
        assertEquals(listOf(emptyList(), listOf(""), listOf("Alice", "Bob")), chart.sections.last().tasks.map { it.actors })
        assertIs<MermaidParseResult.Success>(MermaidParser.parse("journey"))
        assertIs<MermaidParseResult.Success>(MermaidParser.parse("journey\nTask: -2"))
    }

    @Test
    fun malformedUserJourneyFailsClosed() {
        listOf(
            "journey\nsection A\nTask: bad: Actor",
            "journey\nsection A\nTask:",
            "journey\naccDescr {unterminated",
            "journey\nsection A\nunsupported statement",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
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
                    GitGraphCommit("feature", "develop", listOf("base"), GitGraphCommitType.HIGHLIGHT, sequence = 1),
                    GitGraphCommit("release", "main", listOf("base"), GitGraphCommitType.REVERSE, sequence = 2),
                    GitGraphCommit("merge", "main", listOf("release", "feature"), tag = "v2", isMerge = true, message = "merged branch develop into main", customId = true, sequence = 3),
                ),
            ),
            result.diagram,
        )
    }

    @Test
    fun packetAcceptsEmptyAndRelativeContiguousFieldsWithAccessibility() {
        assertTrue(assertIs<PacketDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("packet-beta")).diagram).fields.isEmpty())
        val source = "packet\ntitle First\ntitle Second\naccTitle: Frame\naccDescr {\nHeader and payload\n}\n+8: \"byte\"\n+16: \"word\""
        val d = assertIs<PacketDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
        assertEquals("Second", d.title)
        assertEquals("Frame", d.accessibilityTitle)
        assertEquals("Header and payload", d.accessibilityDescription)
        assertEquals(listOf(PacketField(0, 7, "byte"), PacketField(8, 23, "word")), d.fields)
        val gap = assertIs<MermaidParseResult.Failure>(MermaidParser.parse("packet\n+16: \"test\"\n18: \"error\""))
        assertEquals("Packet block 18 - 18 is not contiguous. It should start from 16.", gap.diagnostics.single().message)
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
    fun gitGraphSupportsOrderedQuotedBranchesMetadataAndAttributeOrder() {
        val source = """
            gitGraph TB:
            accTitle: History
            accDescr { Accessible
              history }
            commit tag: "v1" msg: "Initial work" id: "base" tag: "stable"
            branch "feature branch" order: 2
            commit type: HIGHLIGHT id: "feature"
            checkout main
        """.trimIndent()
        val chart = assertIs<GitGraphDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
        assertEquals(FlowDirection.TB, chart.direction)
        assertEquals("History", chart.accessibilityTitle)
        assertEquals("Accessible\nhistory", chart.accessibilityDescription)
        assertEquals("main", chart.currentBranch)
        assertEquals("base", chart.branchHeads["main"])
        assertEquals(2, chart.branches.last().order)
        assertEquals(listOf("v1", "stable"), chart.commits.first().tags)
        assertEquals("Initial work", chart.commits.first().message)
    }

    @Test
    fun gitGraphResolvesCherryPickAndValidatesMergeParents() {
        val source = """
            gitGraph BT:
            commit id: "base"
            branch develop
            commit id: "work" msg: "Feature"
            checkout main
            commit id: "release"
            merge develop id: "merged" type: REVERSE
            branch cherry
            checkout develop
            cherry-pick id: "merged" parent: "release" tag: "picked"
        """.trimIndent()
        val chart = assertIs<GitGraphDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
        val merge = chart.commits.first { it.id == "merged" }
        assertEquals(GitGraphCommitType.REVERSE, merge.customType)
        assertTrue(merge.customId)
        assertEquals(listOf("release", "work"), merge.parentIds)
        val picked = chart.commits.last()
        assertTrue(picked.isCherryPick)
        assertEquals(listOf("work", "merged"), picked.parentIds)
        assertEquals(listOf("picked"), picked.tags)
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source.replace("parent: \"release\"", "parent: \"base\"")))
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source.replace("parent: \"release\"", "")))
    }

    @Test
    fun gitGraphAcceptsEmptyHistoryAndReportsDuplicateCommitWarning() {
        val empty = assertIs<GitGraphDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("gitGraph")).diagram)
        assertTrue(empty.commits.isEmpty())
        val duplicate = assertIs<GitGraphDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "gitGraph\ncommit id: \"same\"\ncommit id: \"same\" msg: \"replacement\"",
        )).diagram)
        assertEquals("replacement", duplicate.commits.single().message)
        assertEquals(listOf("Commit ID same already exists"), duplicate.warnings)
    }

    @Test
    fun gitGraphRequiresDirectionColonAndPreservesReplacementSequence() {
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("gitGraph TB\ncommit"))
        val chart = assertIs<GitGraphDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(
            "gitGraph TB:\ntitle\tHistory\ncommit id: \"first\"\ncommit id: \"second\"\ncommit id: \"first\"",
        )).diagram)
        assertEquals("History", chart.title)
        assertEquals(listOf("first", "second"), chart.commits.map { it.id })
        assertEquals(listOf(2, 1), chart.commits.map { it.sequence })
    }

    @Test
    fun malformedGitGraphFailsClosed() {
        listOf(
            "gitGraph WRONG",
            "gitGraph\ncheckout missing\ncommit",
            "gitGraph\nbranch develop\nbranch develop\ncommit",
            "gitGraph\ncommit unknown: \"same\"",
            "gitGraph\ncommit\nmerge main",
            "gitGraph\ncommit\nbranch develop\nswitch main\nmerge develop",
            "gitGraph\ncommit type: UNKNOWN",
            "gitGraph\ncherry-pick id: \"one\"",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }

    @Test
    fun malformedRequirementDiagramFailsClosed() {
        listOf(
            "requirementDiagram\n  requirement r {\n    unknown: R-1\n  }",
            "requirementDiagram\n  requirement r {\n    id: R-1\n    field: R-2\n    text: Text\n    risk: low\n    verifymethod: test\n  }",
            "requirementDiagram\n  requirement r {\n    id: R-1\n    text: Text\n    risk: extreme\n    verifymethod: test\n  }",
            "requirementDiagram\n  requirement r {\n    id: R-1\n    text: Text\n    risk: low\n    verifymethod: test",
            "requirementDiagram\n  element e {\n    type: app\n    docref: doc.md\n  }\n  e - unknown -> missing",
            "requirementDiagram\n  requirement r {\n    id: R-1\n    text: Text\n    risk: low\n    verifymethod: test\n  }\n  e - satisfies ->",
        ).forEach { source ->
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source)
        }
    }

    @Test fun quadrantCommentsAndTitleLikePointLabelsRemainVisible() {
        val source = "%% introductory comment\nquadrantChart\ntitle Overview\ntitleA: [0.1, 0.2]\ntitle: [0.3, 0.4]"
        val diagram = assertIs<QuadrantChartDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
        assertEquals("Overview", diagram.title)
        assertEquals(listOf("titleA", "title"), diagram.points.map { it.label })
    }

    @Test fun quadrantInvalidStylesFailAtTheParserBoundary() {
        for (style in listOf("color: red", "color: notacolor", "radius: abc", "stroke-width: -5px", "fill: #abc")) {
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("quadrantChart\nA: [0.1, 0.2] $style"), style)
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("quadrantChart\nclassDef invalid $style"), style)
        }
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("quadrantChart\nquadrant-1 a: b"))
        assertIs<MermaidParseResult.Success>(MermaidParser.parse("quadrantChart\nquadrant-1 \"a: b\"\nA: [0.1, 0.2] color:abc,stroke-color:#123456,radius:10,stroke-width:2px"))
    }

    @Test fun parsesKanbanColumnsAndCards() {
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("kanban\ntodo[Todo]\n  spec[Write spec]\ndone[Done]\n  ship[Ship release]"))
        assertEquals(KanbanDiagram(listOf(KanbanColumn("todo", "Todo", listOf(KanbanCard("spec", "Write spec"))), KanbanColumn("done", "Done", listOf(KanbanCard("ship", "Ship release"))))), result.diagram)
    }

    @Test fun kanbanFlattensNestedCardsAndPreservesMetadataAndDecorations() {
        val diagram = assertIs<KanbanDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("""
            kanban
              todo[Todo]
                task["Keep [] labels"]@{priority: high, assigned: Ada, ticket: K-4}
                :::hot card
                ::icon(star)
                  leaf(Deeper item)
              done@{
                label: 'Ready'
                icon: star
              }
        """.trimIndent())).diagram)
        assertEquals(listOf("Todo", "Ready"), diagram.columns.map { it.title })
        assertEquals(listOf("Keep [] labels", "Deeper item"), diagram.columns[0].cards.map { it.label })
        assertEquals(KanbanMetadata("star", "hot card", "Ada", "K-4", "high"), diagram.columns[0].cards[0].metadata)
        assertTrue(diagram.columns[1].cards.isEmpty())
    }

    @Test fun malformedKanbanFailsClosed() {
        listOf("kanban", "kanban\ntodo[Unclosed", "kanban\n::icon(star)", "kanban\nroot@{ assigned: ").forEach {
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse(it), it)
        }
    }

    @Test
    fun malformedPacketFailsClosed() {
        listOf(
            "packet\n  8-4: \"Reverse\"",
            "packet\n  0-7: \"A\"\n  7-15: \"Overlap\"",
            "packet\n  0-7: Missing quotes",
            "packet\n  4096: \"Beyond bounded layout\"",
            "packet\n  999999999999999999999: \"Overflow\"",
        ).forEach { source ->
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source)
        }
    }

    @Test fun parsesBlockGridSpansAndEdges() {
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("block\ncolumns 3\napi[\"Public API\"]:2\ndb[\"Database\"]\napi --> db"))
        assertEquals(
            BlockDiagram(3, listOf(BlockNode("api", "Public API", 2), BlockNode("db", "Database")), listOf(BlockEdge("api", "db"))),
            result.diagram,
        )
    }

    @Test fun malformedBlockFailsClosed() {
        listOf(
            "block\ncolumns 0\na",
            "block\ncolumns 999999999999999999999\na",
            "block\ncolumns 2\na:999999999999999999999",
            "block\nend",
            "block\nblock\na",
            "block\na[\"unclosed",
            "block\na -- bad --> b",
            "block\nar<[\"Go\"]>(diagonal)",
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

    @Test fun sankeyPreservesParallelLinksAndCyclicIdentifiers() {
        val source = "sankey\n__proto__,A,0.597\nA,__proto__,0.403\nA,__proto__,0.2\nA,A,0.1"
        val diagram = assertIs<SankeyDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
        assertEquals(listOf("__proto__", "A"), diagram.nodes.map { it.id })
        assertEquals(4, diagram.links.size)
        assertEquals(listOf(0.597, 0.403, 0.2, 0.1), diagram.links.map { it.value })
    }

    @Test fun malformedSankeyFailsClosed() {
        listOf(
            "sankey",
            "sankey\nA,B",
            "sankey\nA,B,1,extra",
            "sankey\nA,,1",
            "sankey\nA,B,0",
            "sankey\nA,B,NaN",
            "sankey\nA,B,Infinity",
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

    @Test fun treemapAcceptsOriginalRowsMetadataAndClasses() {
        val source = "treemap\ntitle Portfolio\naccTitle: Access\naccDescr: Allocation\nclassDef hot fill:red;\n\"Root\"\n \"Leaf\", 100:::hot\n\"Empty\""
        val d = assertIs<TreemapDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
        assertEquals("Portfolio", d.title)
        assertEquals("Access", d.accessibilityTitle)
        assertEquals("Allocation", d.accessibilityDescription)
        assertEquals("hot", d.roots[0].children.single().classSelector)
        assertEquals(100.0, d.roots[0].children.single().value)
        assertEquals("fill:red", d.classes["hot"])
        assertEquals("Empty", d.roots[1].label)
        assertIs<MermaidParseResult.Success>(MermaidParser.parse("treemap"))
    }

    @Test fun malformedTreemapFailsClosed() {
        listOf(
            "treemap-beta\n\"Root\"\n  \"Leaf\": 0",
            "treemap-beta\n\"Root\"\n  \"Leaf\": NaN",
            "treemap-beta\n\"Root\"\n  \"A\": 1.7976931348623157E308\n  \"B\": 1.7976931348623157E308",
            "treemap-beta\n\"Root\"\n  \"Leaf\": 1\n  \"Leaf\": 2",
            "treemap-beta\n\"Root\"\n  \"Leaf\": 1\n    \"Child\": 1",
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

    @Test fun architectureMetadataQuotesAndEmptyGrammarReachProduct() {
        val source = "architecture-beta title Sample\naccTitle: Accessible\naccDescr {\nDescription\n}\nservice db(database)[\"John's Database\"] in api\ngroup api(cloud)[API]"
        val diagram = assertIs<ArchitectureDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
        assertEquals("Sample", diagram.title); assertEquals("Accessible", diagram.accTitle); assertEquals("Description", diagram.accDescription)
        assertEquals("John's Database", diagram.services.single().label)
        assertIs<MermaidParseResult.Success>(MermaidParser.parse("architecture-beta"))
        assertIs<MermaidParseResult.Success>(ArchitectureParser("architecture-beta\nservice a in missing").parse())
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("architecture-beta\nservice a in missing"))
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

    @Test fun c4OriginalMacrosRetainVariantsNamedAttributesAndNestedBoundaries() {
        val dollar = '$'
        val source = """C4Container
            Boundary(bank, "Bank") {
              ContainerDb(db, "Ledger", "SQL", "Balances", ${dollar}tags="core")
              Boundary(inner, "Internal") {
                Component(worker, "Worker", "Kotlin")
              }
            }
            Person(user, ${dollar}sprite="users")
            Rel(bank, user, "Serves")
        """.trimIndent()
        val diagram = assertIs<C4Diagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
        assertEquals("C4Container", diagram.diagramType)
        assertEquals(listOf("global", "bank"), diagram.boundaries.map { it.parentBoundary })
        assertEquals("database", diagram.elements[0].variant)
        assertEquals("SQL", diagram.elements[0].technology)
        assertEquals("core", diagram.elements[0].attributes["tags"])
        assertEquals("inner", diagram.elements[1].parentBoundary)
        assertEquals("sprite", diagram.elements[2].labelAttribute)
        assertEquals("bank", diagram.relationships.single().sourceId)
    }

    @Test fun c4NamedDescriptionAndTechnologyReachProductFields() {
        val dollar = '$'
        val source = """C4Container
            System_Ext(bank, "Bank", ${dollar}descr="Card processing")
            Container(api, "API", ${dollar}techn="HTTP", ${dollar}descr="Orders")
        """.trimIndent()
        val diagram = assertIs<C4Diagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram)
        assertEquals("Card processing", diagram.elements[0].description)
        assertEquals("HTTP", diagram.elements[1].technology)
        assertEquals("Orders", diagram.elements[1].description)
    }

    @Test fun malformedC4ContextFailsClosed() {
        listOf(
            "C4Context",
            "c4context\nPerson(a, \"A\")",
            "C4Context\nPerson(a, \"A\")\nRel(a, missing, \"Uses\")",
            "C4Context\nPerson(a, \"A\")\nRel(a, a, \"Self\")",
            "C4Context\nPerson(a, \"A\")\nBoundary(b, \"Deferred\") {",
            "C4Context\nPerson(a, \"A\")\nUpdateElementStyle(a, ${'$'}fontColor=\"red\")",
            "C4Context;\nPerson(a, \"A\")",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }
    @Test fun treeViewRawIndentationAndAnnotationsReachModel() {
        val d = assertIs<TreeViewDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("treeView-beta\ntitle Project\nroot/\n  'my file.ts' icon(logos:react) :::highlight ## entry point\n      nested\n  sibling")).diagram)
        assertEquals(listOf(null, 2, 6, 2), d.nodes.map { it.sourceIndent })
        assertEquals(listOf(null, 0, 1, 0), d.nodes.map { it.parentIndex })
        assertEquals("my file.ts", d.nodes[1].label); assertEquals("logos:react", d.nodes[1].iconAnnotation)
        assertEquals("entry point", d.nodes[1].description); assertEquals("highlight", d.nodes[1].classAnnotation)
        assertIs<MermaidParseResult.Success>(MermaidParser.parse("treeView-beta"))
    }

    @Test
    fun railroadOriginalMetadataCommentsEmptyAndSingleChoice() {
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("""
            railroad-beta
            title Grammar
            accTitle: Accessible grammar
            accDescr { line one
            line two }
            /* comment */
            my-rule = choice(terminal('a')) ;
        """.trimIndent()))
        val diagram = assertIs<RailroadDiagram>(result.diagram)
        assertEquals("Accessible grammar", diagram.accTitle)
        assertEquals("line one\nline two", diagram.accDescription)
        assertEquals(RailroadTerminal("a"), diagram.rules.single().definition)
        assertIs<MermaidParseResult.Success>(MermaidParser.parse("railroad-beta\ntitle Empty Grammar"))
        val bad = assertIs<MermaidParseResult.Failure>(MermaidParser.parse("railroad-beta\n/* unclosed"))
        assertEquals(2, bad.diagnostics.single().location.line)
    }
    @Test fun treeViewBoxDrawingProductMatchesIndentAndPreservesErrorLines() {
        val box = "treeView-beta\n├── src/\n│   ├── index.ts icon(logos:typescript) ## entry\n│   └── app.ts\n│\n└── README.md"
        val indent = "treeView-beta\n    src/\n        index.ts icon(logos:typescript) ## entry\n        app.ts\n    README.md"
        assertEquals(MermaidParser.parse(indent), MermaidParser.parse(box))
        val error = assertIs<MermaidParseResult.Failure>(MermaidParser.parse("treeView-beta\n├── src/\n│\n└── file icon(bad value)"))
        assertEquals(4, error.diagnostics.single().location.line)
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("treeView-beta\n├── src/\n    mixed.ts"))
    }

    @Test fun treeViewRejectsMetadataAfterNodesWithoutSwallowingContent() {
        for (suffix in listOf("title X", "  title X", "accTitle: X", "  accDescr { X }")) {
            val failure = assertIs<MermaidParseResult.Failure>(MermaidParser.parse("treeView-beta\nroot/\n$suffix"))
            assertEquals(3, failure.diagnostics.single().location.line)
            assertEquals("TreeView metadata must appear before nodes", failure.diagnostics.single().message)
        }
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("TREEVIEW-BETA\nroot/"))
        assertIs<MermaidParseResult.Success>(MermaidParser.parse("treeView-beta\ntitle X\nroot/\n  titleFile.ts"))
    }
    @Test fun vennOriginalTextAndStylesReachTheProductModel() {
        val d = assertIs<VennDiagram>(assertIs<MermaidParseResult.Success>(MermaidParser.parse("venn-beta\ntitle Overlap\nset A[Alpha]\n  text a1[Note]\nset B\nunion A,B[Both]\n  text ab1\nstyle A fill:rgb(255, 0, 128), color:#333")).diagram)
        assertEquals("Overlap", d.title)
        assertEquals(listOf("A"), d.texts[0].setIds)
        assertEquals(listOf("A", "B"), d.texts[1].setIds)
        assertEquals("rgb(255, 0, 128)", d.styles.single().properties["fill"])
        assertIs<MermaidParseResult.Success>(MermaidParser.parse("venn-beta"))
    }
    @Test
    fun railroadRejectsMalformedArgumentSeparators() {
        listOf(
            "sequence(terminal(\"a\") terminal(\"b\"))",
            "choice(terminal(\"a\"),)",
            "sequence(,terminal(\"a\"))",
            "choice(terminal(\"a\"),,terminal(\"b\"))",
            "optional(terminal(\"a\"),)",
            "terminal(\"a\",)",
        ).forEach { expression ->
            assertIs<MermaidParseResult.Failure>(MermaidParser.parse("railroad-beta\nr = $expression;"), expression)
        }
        assertIs<MermaidParseResult.Success>(MermaidParser.parse("railroad-beta\nr = choice(terminal(\"a\"), /* gap */ terminal(\"b\"));"))
    }

    @Test
    fun railroadTitlesStayOnTheirLineAndMetadataNamesCanBeRules() {
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("""
            railroad-beta
            title First
            title Last %% hidden comment
            title
            accTitle = terminal('\d');
            accDescr = terminal('b');
        """.trimIndent()))
        val diagram = assertIs<RailroadDiagram>(result.diagram)
        assertEquals("", diagram.title)
        assertEquals(listOf("accTitle", "accDescr"), diagram.rules.map { it.name })
        assertEquals(RailroadTerminal("d"), diagram.rules.first().definition)
        val repeated = assertIs<MermaidParseResult.Success>(MermaidParser.parse("railroad-beta\ntitle First\ntitle Last %% comment"))
        assertEquals("Last", assertIs<RailroadDiagram>(repeated.diagram).title)
    }

    @Test fun blockOriginalCompositesStylesAndWarningUseRealModel() {
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("""
            block
            columns 1
            classDef dark color:#ffffff,fill:#000000;
            block:group["Services"]
              columns auto
              A["API"] -- "calls" --> B["Database"]
              space
              next<["Go"]>(up,down)
            end
            class A dark
            style B fill:#f9F
            wide:2
        """.trimIndent()))
        val diagram = assertIs<BlockDiagram>(result.diagram)
        assertEquals(2, diagram.nodes.size)
        assertEquals(4, diagram.nodes.first().children.size)
        assertEquals("composite", diagram.nodes.first().type)
        assertEquals(-1, diagram.nodes.first().columns)
        assertEquals(listOf("dark"), diagram.nodes.first().children.first().classes)
        assertEquals(listOf("fill:#f9F"), diagram.nodes.first().children[1].styles)
        assertEquals("calls", diagram.edges.single().label)
        assertEquals(listOf("Block wide width 2 exceeds configured column width 1"), diagram.warnings)
        assertIs<MermaidParseResult.Success>(MermaidParser.parse("block\n__proto__; constructor"))
        assertIs<MermaidParseResult.Failure>(MermaidParser.parse("block\nblock\nA"))
    }

}
