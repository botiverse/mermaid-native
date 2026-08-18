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

/** A normalized parser vector derived from the pinned beautiful-mermaid reference. */
public data class MermaidParserFixture(
    val name: String,
    val source: String,
    val expected: MermaidDiagram,
)

public object Phase0ParserFixtures {
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

    public val all: List<MermaidParserFixture> = listOf(flowchart, sequence)
}
