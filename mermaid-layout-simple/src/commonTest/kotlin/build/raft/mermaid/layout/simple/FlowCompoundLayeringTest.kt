package build.raft.mermaid.layout.simple

import build.raft.mermaid.core.*
import build.raft.mermaid.layout.*
import kotlin.math.abs
import kotlin.test.*

class FlowCompoundLayeringTest {
    private fun layout(source: String): LayoutScene {
        val diagram = assertIs<MermaidParseResult.Success>(MermaidParser.parse(source)).diagram
        return SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig())
    }
    private fun bounds(t: DrawText): SceneRect {
        val size = FixedWidthTextMeasurer.measure(t.text, t.style)
        return SceneRect(t.origin.x - size.width / 2, t.origin.y - size.height, size.width, size.height)
    }
    private fun intersects(a: SceneRect, b: SceneRect) =
        a.x < b.x + b.width && b.x < a.x + a.width && a.y < b.y + b.height && b.y < a.y + a.height
    private fun contains(outer: SceneRect, inner: SceneRect) =
        inner.x >= outer.x - 1e-6 && inner.y >= outer.y - 1e-6 &&
            inner.x + inner.width <= outer.x + outer.width + 1e-6 && inner.y + inner.height <= outer.y + outer.height + 1e-6
    private fun node(s: LayoutScene, firstLine: String): SceneRect {
        val text = s.commands.filterIsInstance<DrawText>().first { it.text == firstLine }
        return s.commands.filterIsInstance<DrawRect>().filter { it.cornerRadius != 4.0 }.map { it.rect }
            .first { it.x <= text.origin.x && text.origin.x <= it.x + it.width && it.y <= text.origin.y && text.origin.y <= it.y + it.height + 1e-6 }
    }
    private fun frame(s: LayoutScene, title: String): SceneRect {
        val index = s.commands.indexOfFirst { it is DrawText && it.text == title }
        return (s.commands[index - 1] as DrawRect).rect
    }

    private val crossContainer = """
        flowchart TB
          subgraph Core["Core (packages/server)"]
            direction TB
            R1["routes/internalAgentApiArtifacts.ts<br/>agent API entry"]
            R2["routes/artifactsWeb.ts<br/>web API entry"]
            R3["routes/internalArtifactsContent.ts<br/>content worker entry"]
            DB["db executor"]
            VIS["visibilityService<br/>who can see"]
            FF["featureFlagService<br/>flags"]
            OE["objectEvents<br/>events"]
            CORE["other core capabilities<br/>users/agents, conversations, attachments, login"]
          end
          subgraph App["apps/artifacts (schema app_artifacts)"]
            A["Artifacts module"]
          end
          R1 -- "import + inject" --> A
          R2 -- "import + inject" --> A
          R3 -- "import + inject" --> A
          R1 & R2 & R3 -. "built into functions" .-> CORE
          A --> DB
          A --> VIS
          A --> FF
          A --> OE
    """.trimIndent()

    @Test fun crossContainerEdgesRankNodesGloballyAndGrowTheirFrames() {
        val s = layout(crossContainer)
        assertEquals(s, layout(crossContainer))
        val routes = listOf("routes/internalAgentApiArtifacts.ts", "routes/artifactsWeb.ts", "routes/internalArtifactsContent.ts").map { node(s, it) }
        val module = node(s, "Artifacts module")
        val core = node(s, "other core capabilities")
        val services = listOf("db executor", "visibilityService", "featureFlagService", "objectEvents").map { node(s, it) }
        for (r in routes) {
            assertTrue(module.y >= r.y + r.height, "Artifacts module $module must rank below $r")
            assertTrue(core.y >= r.y + r.height, "Core capabilities $core must rank below $r")
        }
        for (service in services) assertTrue(service.y >= module.y + module.height, "$service must rank below $module")
        val coreFrame = frame(s, "Core (packages/server)")
        val appFrame = frame(s, "apps/artifacts (schema app_artifacts)")
        for (r in routes + core + services) assertTrue(contains(coreFrame, r), "$coreFrame must contain $r")
        assertTrue(contains(appFrame, module))
        assertFalse(intersects(coreFrame, appFrame))
        val labels = s.commands.filterIsInstance<DrawText>().filter { it.text in listOf("import + inject", "built into functions") }.map(::bounds)
        assertEquals(6, labels.size)
        for (i in labels.indices) for (j in i + 1 until labels.size) assertFalse(intersects(labels[i], labels[j]), "${labels[i]} / ${labels[j]}")
        val nodes = routes + module + core + services
        for (label in labels) for (n in nodes) assertFalse(intersects(label, n), "$label overlaps $n")
        for (label in labels) assertTrue(label.x >= 0 && label.y >= 0 && label.x + label.width <= s.width && label.y + label.height <= s.height)
    }

    @Test fun crossContainerLayeringFollowsEveryDirection() {
        for (direction in listOf("TB", "BT", "LR", "RL")) {
            val source = crossContainer.replace("flowchart TB", "flowchart $direction").replace("direction TB", "direction $direction")
            val s = layout(source)
            val module = node(s, "Artifacts module")
            val db = node(s, "db executor")
            val entry = node(s, "routes/artifactsWeb.ts")
            fun main(r: SceneRect) = when (direction) {
                "TB" -> r.y + r.height / 2; "BT" -> -(r.y + r.height / 2)
                "LR" -> r.x + r.width / 2; else -> -(r.x + r.width / 2)
            }
            assertTrue(main(entry) < main(module), direction)
            assertTrue(main(module) < main(db), direction)
            val coreFrame = frame(s, "Core (packages/server)")
            assertTrue(contains(coreFrame, db) && contains(coreFrame, entry), direction)
            assertTrue(contains(frame(s, "apps/artifacts (schema app_artifacts)"), module), direction)
            val labels = s.commands.filterIsInstance<DrawText>().filter { it.text in listOf("import + inject", "built into functions") }.map(::bounds)
            for (i in labels.indices) for (j in i + 1 until labels.size) assertFalse(intersects(labels[i], labels[j]), direction)
        }
    }

    @Test fun acyclicContainersKeepTheRecursivePlacement() {
        // Containers connected one way only still stack as whole units.
        val s = layout("flowchart TD\nsubgraph G[One]\nA-->B\nend\nsubgraph H[Two]\nC-->D\nend\nB-->C")
        val one = frame(s, "One")
        val two = frame(s, "Two")
        assertTrue(two.y >= one.y + one.height)
    }

    @Test fun backEdgesUseSidePortsDistinctFromForwardEdges() {
        val source = """
            flowchart TD
              H([Human asks]) --> A[Agent claims the task]
              A -->|plan| P[Write the change]
              P --> S[Open PR]
              S --> C{CI green?}
              C -->|no| P
              C -->|yes| R[Human reviews]
              R --> M([Merged + released])
        """.trimIndent()
        val s = layout(source)
        assertEquals(s, layout(source))
        // Edges paint in declaration order before nodes; every edge here is visible.
        val curves = s.edgeCurves()
        assertEquals(7, curves.size)
        fun same(a: ScenePoint, b: ScenePoint) = abs(a.x - b.x) < 1e-6 && abs(a.y - b.y) < 1e-6
        val plan = curves[1]; val write = curves[2]; val no = curves[4]; val yes = curves[5]
        assertFalse(same(no.start, yes.start), "back edge must not leave CI green? at the forward port ${yes.start}")
        assertFalse(same(no.end, plan.end), "back edge must not enter Write the change at the plan port ${plan.end}")
        assertFalse(same(no.end, write.start), "back edge must not enter at the forward exit port")
        val decision = s.commands.filterIsInstance<DrawPolygon>().map { p ->
            val xs = p.points.map { it.x }; val ys = p.points.map { it.y }
            SceneRect(xs.min(), ys.min(), xs.max() - xs.min(), ys.max() - ys.min())
        }.maxBy { it.width * it.height }
        // Like dagre, the back edge leaves the decision from its upper right side...
        assertTrue(no.start.y < decision.y + decision.height / 2)
        assertTrue(no.start.x > decision.x + decision.width / 2)
        // ...and enters Write the change from below, right of its center.
        val change = node(s, "Write the change")
        assertEquals(change.y + change.height, no.end.y, 1e-6)
        assertTrue(no.end.x > change.x + change.width / 2, "back edge enters ${no.end} under $change")
        // The loop has its own lane: Open PR shifts to the other side of the chain.
        val pr = node(s, "Open PR")
        assertTrue(pr.x + pr.width / 2 < change.x + change.width / 2)
        val label = bounds(s.commands.filterIsInstance<DrawText>().single { it.text == "no" })
        assertTrue(label.x > pr.x + pr.width)
        for (r in s.boxes()) assertFalse(intersects(label, r))
    }
}
