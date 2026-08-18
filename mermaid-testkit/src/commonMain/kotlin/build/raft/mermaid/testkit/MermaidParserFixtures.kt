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
import build.raft.mermaid.core.StateDiagram
import build.raft.mermaid.core.StateNode
import build.raft.mermaid.core.StateNodeKind
import build.raft.mermaid.core.StateTransition

/** A normalized parser vector derived from the pinned beautiful-mermaid reference. */
public data class MermaidParserFixture(
    val name: String,
    val source: String,
    val expected: MermaidDiagram,
)

public object MermaidParserFixtures {
    public val flowchart: MermaidParserFixture = MermaidParserFixture(
        name = "minimal-flowchart",
        source = "flowchart LR; A[Start] --> B[Finish]",
        expected = FlowchartDiagram(
            direction = FlowDirection.LR,
            nodes = listOf(FlowNode("A", "Start"), FlowNode("B", "Finish")),
            edges = listOf(FlowEdge("A", "B")),
        ),
    )

    public val sequence: MermaidParserFixture = MermaidParserFixture(
        name = "minimal-sequence",
        source = "sequenceDiagram; Alice->>Bob: Hello",
        expected = SequenceDiagram(
            actors = listOf(SequenceActor("Alice", "Alice"), SequenceActor("Bob", "Bob")),
            messages = listOf(
                SequenceMessage(
                    from = "Alice",
                    to = "Bob",
                    label = "Hello",
                    lineStyle = SequenceLineStyle.SOLID,
                    arrowHead = SequenceArrowHead.FILLED,
                ),
            ),
        ),
    )

    public val state: MermaidParserFixture = MermaidParserFixture(
        name = "minimal-state",
        source = "stateDiagram-v2; [*] --> Idle; Idle --> [*]: stop",
        expected = StateDiagram(
            states = listOf(
                StateNode("__start_0", "", StateNodeKind.START),
                StateNode("Idle", "Idle"),
                StateNode("__end_1", "", StateNodeKind.END),
            ),
            transitions = listOf(
                StateTransition("__start_0", "Idle"),
                StateTransition("Idle", "__end_1", "stop"),
            ),
        ),
    )

    public val all: List<MermaidParserFixture> = listOf(flowchart, sequence, state)
}
