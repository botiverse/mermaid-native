package build.raft.mermaid.testkit

import build.raft.mermaid.core.FlowDirection
import build.raft.mermaid.core.ClassDefinition
import build.raft.mermaid.core.ClassDiagram
import build.raft.mermaid.core.ClassMember
import build.raft.mermaid.core.ClassRelationship
import build.raft.mermaid.core.ClassRelationshipKind
import build.raft.mermaid.core.FlowEdge
import build.raft.mermaid.core.FlowNode
import build.raft.mermaid.core.FlowchartDiagram
import build.raft.mermaid.core.MermaidDiagram
import build.raft.mermaid.core.SequenceActor
import build.raft.mermaid.core.SequenceArrowHead
import build.raft.mermaid.core.SequenceDiagram
import build.raft.mermaid.core.SequenceLineStyle
import build.raft.mermaid.core.SequenceMessage

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

    public val all: List<MermaidExample> = listOf(
        classAnimal,
        flowchartLinear,
        flowchartLeftToRight,
        sequenceRequestResponse,
    )
}
