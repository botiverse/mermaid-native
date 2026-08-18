package build.raft.mermaid.testkit

import build.raft.mermaid.core.FlowDirection
import build.raft.mermaid.core.FlowEdge
import build.raft.mermaid.core.FlowNode
import build.raft.mermaid.core.FlowchartDiagram
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

    public val all: List<MermaidExample> = listOf(
        piePets,
        flowchartLinear,
        flowchartLeftToRight,
        sequenceRequestResponse,
        stateLifecycle,
    )
}
