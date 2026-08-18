package build.raft.mermaid.core

/** A parsed Mermaid-compatible diagram with no rendering or platform state. */
public sealed interface MermaidDiagram

public enum class FlowDirection {
    TD,
    TB,
    LR,
    BT,
    RL,
}

public data class FlowchartDiagram(
    val direction: FlowDirection,
    val nodes: List<FlowNode>,
    val edges: List<FlowEdge>,
) : MermaidDiagram

public data class FlowNode(
    val id: String,
    val label: String,
)

public data class FlowEdge(
    val sourceId: String,
    val targetId: String,
)

public data class SequenceDiagram(
    val actors: List<SequenceActor>,
    val messages: List<SequenceMessage>,
) : MermaidDiagram

public data class SequenceActor(
    val id: String,
    val label: String,
)

public data class SequenceMessage(
    val from: String,
    val to: String,
    val label: String,
    val lineStyle: SequenceLineStyle,
    val arrowHead: SequenceArrowHead,
)

/** State diagram model for the Mermaid stateDiagram/stateDiagram-v2 family. */
public data class StateDiagram(
    val direction: FlowDirection = FlowDirection.TB,
    val states: List<StateNode>,
    val transitions: List<StateTransition>,
) : MermaidDiagram

public data class StateNode(
    val id: String,
    val label: String,
    val kind: StateNodeKind = StateNodeKind.STATE,
)

public enum class StateNodeKind { STATE, START, END }

public data class StateTransition(
    val from: String,
    val to: String,
    val label: String = "",
)

public enum class SequenceLineStyle {
    SOLID,
    DASHED,
}

public enum class SequenceArrowHead {
    FILLED,
}
