package build.raft.mermaid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MermaidParserTest {
    @Test
    fun acceptsOfficialGalleryHeaderAliases() {
        val aliases = listOf(
            "block-beta\ncolumns 2\nA[Client]\nB[Server]\nA --> B",
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
        val result = assertIs<MermaidParseResult.Success>(MermaidParser.parse("eventmodeling\ntitle Cart & inventory\ntf 01 ui CartUI\ntimeframe 02 command AddItem\ntf 03 evt ItemAdded\nresetframe 04 event External.InventoryChanged\ntf 05 readmodel InventoryView ->> 03 ->> 04"))
        val diagram = assertIs<EventModelingDiagram>(result.diagram)
        assertEquals("Cart & inventory", diagram.title)
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
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }

    @Test
    fun parsesRailroadExpressionTree() {
        val result = assertIs<MermaidParseResult.Success>(
            MermaidParser.parse(
                "railroad-beta\nDiagram(\n  Sequence('token',\n    Choice(0, Sequence('user', Terminal('password')), NonTerminal('oauth')),\n    Stack(Skip, Optional('mfa'))\n  )\n)",
            ),
        )
        assertEquals(
            RailroadDiagram(
                RailroadSequence(
                    listOf(
                        RailroadTerminal("token"),
                        RailroadChoice(
                            0,
                            listOf(
                                RailroadSequence(listOf(RailroadTerminal("user"), RailroadTerminal("password"))),
                                RailroadNonTerminal("oauth"),
                            ),
                        ),
                        RailroadStack(listOf(RailroadSkip, RailroadOptional(RailroadTerminal("mfa")))),
                    ),
                ),
            ),
            result.diagram,
        )
    }

    @Test
    fun parsesRailroadRepeatAndBoundarySymbols() {
        val parsed = MermaidParser.parse("railroad-beta\nComplexDiagram(OneOrMore(ZeroOrMore(Sequence(Start, 'a', End))))")
        val result = assertIs<MermaidParseResult.Success>(
            parsed,
            (parsed as? MermaidParseResult.Failure)?.diagnostics.toString(),
        )
        assertEquals(
            RailroadDiagram(RailroadOneOrMore(RailroadZeroOrMore(RailroadSequence(listOf(RailroadStart, RailroadTerminal("a"), RailroadEnd))))),
            result.diagram,
        )
    }

    @Test
    fun malformedRailroadFailsClosed() {
        listOf(
            "railroad-beta",
            "railroad\nDiagram('a')",
            "railroad-beta\nDiagram()",
            "railroad-beta\nFoo('a')",
            "railroad-beta\nDiagram(\"double quoted\")",
            "railroad-beta\nDiagram(Terminal(\"x\"))",
            "railroad-beta\nDiagram('a', 'b')",
            "railroad-beta\nDiagram(Choice('a', 'b'))",
            "railroad-beta\nDiagram(Choice(-1, 'a'))",
            "railroad-beta\nDiagram(Optional())",
            "railroad-beta\nDiagram(Sequence())",
            "railroad-beta\nDiagram(Terminal())",
            "railroad-beta\nDiagram(Terminal(5))",
            "railroad-beta\nDiagram('unterminated)",
            "railroad-beta\nDiagram(Skip())",
            "railroad-beta\nDiagram(Stack('a')) extra",
            "railroad-beta\nDiagram(Sequence('a'))\nDiagram(Sequence('b'))",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
    }

    @Test
    fun parsesZenumlInteractions() {
        val parsed = MermaidParser.parse(
            "zenuml\n" +
                "    title Token handshake\n" +
                "    Client\n" +
                "    Store as Token store\n" +
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
                    TreeViewNode("project", 0, null, true),
                    TreeViewNode("src", 1, 0, true),
                    TreeViewNode("index.ts", 2, 1, false),
                    TreeViewNode("README file.md", 1, 0, false),
                ),
            ),
            result.diagram,
        )
    }

    @Test
    fun malformedTreeViewFailsClosed() {
        listOf(
            "treeView-beta",
            "treeView-beta\nroot/",
            "treeView-beta\n  root/",
            "treeView-beta\n    root/\n            skipped.txt",
            "treeView-beta\n    unquoted label",
            "treeView-beta\n    \"unterminated",
            "treeView-beta\n    root/ :::highlight",
            "treeView-beta\n    root/ ## description",
            "treeView-beta\n\troot/",
        ).forEach { source -> assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), source) }
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
            result.diagram,
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
            "swimlane-beta\nsubgraph A\nsubgraph B\nb[Two]\nend",
            "swimlane-beta\nsubgraph A\na[One]\nend\nsubgraph A\nb[Two]\nend",
            "swimlane-beta\nsubgraph A\na[One]\nend\nsubgraph B\na[Duplicate]\nend",
            "swimlane-beta\nsubgraph A\na[One]\nend\na --> missing",
            "swimlane-beta\nsubgraph A\na[One]\nend\na --> a",
            "swimlane-beta\nsubgraph A\na[One]\nend\nstyle a fill:red",
            "swimlane-beta\naccTitle: unsupported\nsubgraph A\na[One]\nend",
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
            ),
            result.diagram,
        )
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
        assertEquals(1, diagnostic.location.column)
    }

    @Test
    fun malformedRadarFailsClosed() {
        listOf(
            // Empty or wrong header.
            "radar-beta",
            "RadarBeta\naxis a, b, c",
            // Too few axes for polar geometry.
            "radar-beta\naxis a, b\ncurve x{1, 2}",
            // Missing curves.
            "radar-beta\naxis a, b, c",
            // Duplicate ids.
            "radar-beta\naxis a, b, a\ncurve x{1, 2, 3}",
            "radar-beta\naxis a, b, c\ncurve x{1, 2, 3}\ncurve x{4, 5, 6}",
            // Curve/axis count mismatch.
            "radar-beta\naxis a, b, c\ncurve x{1, 2}",
            "radar-beta\naxis a, b, c\ncurve x{1, 2, 3, 4}",
            // Invalid values.
            "radar-beta\naxis a, b, c\ncurve x{-1, 2, 3}",
            "radar-beta\naxis a, b, c\ncurve x{1, two, 3}",
            "radar-beta\naxis a, b, c\nmax 50\ncurve x{10, 60, 20}",
            // Invalid max.
            "radar-beta\naxis a, b, c\ncurve x{1, 2, 3}\nmax -5",
            "radar-beta\naxis a, b, c\ncurve x{1, 2, 3}\nmax 0",
            "radar-beta\naxis a, b, c\ncurve x{1, 2, 3}\nmax abc",
            // Strict numeric lexicon: no scientific notation, signs, or bare fractions.
            "radar-beta\naxis a, b, c\ncurve x{1, 2, 3}\nmax 1e2",
            "radar-beta\naxis a, b, c\ncurve x{1, 2, 3}\nmax +50",
            "radar-beta\naxis a, b, c\ncurve x{1, 2, 3}\nmax .5",
            "radar-beta\naxis a, b, c\ncurve x{1, 2, 3}\nmax 5.",
            "radar-beta\naxis a, b, c\ncurve x{-0, 2, 3}",
            "radar-beta\naxis a, b, c\ncurve x{NaN, 2, 3}",
            "radar-beta\naxis a, b, c\ncurve x{Infinity, 2, 3}",
            // Malformed comma structure inside the value list.
            "radar-beta\naxis a, b, c\ncurve x{1,,3}",
            "radar-beta\naxis a, b, c\ncurve x{1, 2, 3,}",
            // Malformed quotes, brackets, and braces.
            "radar-beta\naxis m\"Math\", s, e\ncurve x{1, 2, 3}",
            "radar-beta\naxis m[\"Math, s, e\ncurve x{1, 2, 3}",
            "radar-beta\naxis a, b, c\ncurve alice[\"Alice\"] 1, 2, 3",
            "radar-beta\naxis a, b, c\ncurve alice{1, 2, 3",
            // Empty axis entries and whitespace-only title.
            "radar-beta\naxis a,,b,c\ncurve x{1, 2, 3, 4}",
            "radar-beta\ntitle   \naxis a, b, c\ncurve x{1, 2, 3}",
            // Explicitly unsupported official options fail closed by name.
            "radar-beta\naxis a, b, c\ncurve x{1, 2, 3}\ngraticule circle",
            "radar-beta\naxis a, b, c\ncurve x{1, 2, 3}\nticks 5",
            "radar-beta\naxis a, b, c\ncurve x{1, 2, 3}\nshowLegend false",
            "radar-beta\naxis a, b, c\ncurve x{1, 2, 3}\nmin 10",
            // Duplicate title.
            "radar-beta\ntitle One\ntitle Two\naxis a, b, c\ncurve x{1, 2, 3}",
            // Malformed declarations.
            "radar-beta\naxis a[\"\"], b, c\ncurve x{1, 2, 3}",
            "radar-beta\naxis m[\"A,B\"], s, e\ncurve x{1, 2, 3}",
            "radar-beta\naxis a, b, c\ncurve x 1, 2, 3",
            "radar-beta\naxis a, b, c\ncurve x{1, 2, 3}\nstyle x fill:red",
            "radar-beta\naccTitle: unsupported\naxis a, b, c\ncurve x{1, 2, 3}",
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
