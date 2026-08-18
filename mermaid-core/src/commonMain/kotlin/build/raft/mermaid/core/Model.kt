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

public data class PieDiagram(
    val title: String?,
    val showData: Boolean,
    val sections: List<PieSection>,
    val accessibilityTitle: String? = null,
    val accessibilityDescription: String? = null,
) : MermaidDiagram

public data class PieSection(val label: String, val value: Double)
public data class ClassDiagram(
    val classes: List<ClassDefinition>,
    val relationships: List<ClassRelationship>,
) : MermaidDiagram

public data class ClassDefinition(
    val id: String,
    val label: String = id,
    val members: List<ClassMember> = emptyList(),
)

public data class ClassMember(
    val signature: String,
    val visibility: ClassVisibility = ClassVisibility.PUBLIC,
)

public enum class ClassVisibility { PUBLIC, PRIVATE, PROTECTED, PACKAGE }

public data class ClassRelationship(
    val from: String,
    val to: String,
    val kind: ClassRelationshipKind,
)

public enum class ClassRelationshipKind { INHERITANCE, ASSOCIATION }

/** Minimal platform-neutral model for the entityRelationshipDiagram family. */
public data class EntityRelationshipDiagram(
    val entities: List<EntityDefinition>,
    val relationships: List<EntityRelationship>,
) : MermaidDiagram

public data class EntityDefinition(
    val id: String,
    val attributes: List<EntityAttribute> = emptyList(),
)

public data class EntityAttribute(
    val type: String,
    val name: String,
    val key: EntityKey = EntityKey.NONE,
)

public enum class EntityKey { NONE, PK, FK, UK }

public data class EntityRelationship(
    val from: String,
    val to: String,
    val fromCardinality: EntityCardinality,
    val toCardinality: EntityCardinality,
    val label: String = "",
)

public enum class EntityCardinality { ONLY_ONE, ZERO_OR_ONE, ONE_OR_MORE, ZERO_OR_MORE }

public enum class SequenceLineStyle {
    SOLID,
    DASHED,
}

public enum class SequenceArrowHead {
    FILLED,
}
