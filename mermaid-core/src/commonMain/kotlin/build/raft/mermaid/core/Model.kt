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

/** Minimal platform-neutral model for the Mermaid xychart family. */
public data class XyChartDiagram(
    val title: String? = null,
    val xAxis: XyAxis,
    val yAxis: NumericAxis,
    val series: List<XySeries>,
) : MermaidDiagram

public data class XyAxis(
    val title: String? = null,
    val categories: List<String>,
)

public data class NumericAxis(
    val title: String? = null,
    val minimum: Double,
    val maximum: Double,
)

public data class XySeries(
    val kind: XySeriesKind,
    val values: List<Double>,
)

public enum class XySeriesKind { LINE, BAR }

/** Minimal platform-neutral model for the Mermaid mindmap family. */
public data class MindmapDiagram(
    val nodes: List<MindmapNode>,
) : MermaidDiagram

public data class MindmapNode(
    val id: String,
    val label: String,
    val parentId: String?,
    val depth: Int,
    val shape: MindmapNodeShape = MindmapNodeShape.DEFAULT,
)

public enum class MindmapNodeShape { DEFAULT, RECTANGLE, DOUBLE_CIRCLE }

public data class GanttDiagram(val title: String?, val dateFormat: String, val sections: List<GanttSection>) : MermaidDiagram
public data class GanttSection(val name: String, val tasks: List<GanttTask>)
public data class GanttTask(val name: String, val id: String, val startDay: Int, val durationDays: Int, val status: GanttTaskStatus = GanttTaskStatus.TODO)
public enum class GanttTaskStatus { TODO, DONE, ACTIVE, CRITICAL }

public data class TimelineDiagram(val title: String?, val events: List<TimelineEvent>) : MermaidDiagram
public data class TimelineEvent(val period: String, val labels: List<String>)

public data class QuadrantChartDiagram(
    val title: String?,
    val xAxis: QuadrantAxis,
    val yAxis: QuadrantAxis,
    val quadrantLabels: List<String?>,
    val points: List<QuadrantPoint>,
) : MermaidDiagram
public data class QuadrantAxis(val lowLabel: String, val highLabel: String)
public data class QuadrantPoint(val label: String, val x: Double, val y: Double)

/** Minimal platform-neutral model for the Mermaid user journey family. */
public data class UserJourneyDiagram(
    val title: String?,
    val sections: List<UserJourneySection>,
) : MermaidDiagram

public data class UserJourneySection(
    val name: String,
    val tasks: List<UserJourneyTask>,
)

public data class UserJourneyTask(
    val label: String,
    val score: Int,
    val actors: List<String>,
)

/** Minimal platform-neutral model for the Mermaid gitGraph family. */
public data class GitGraphDiagram(
    val branches: List<GitGraphBranch>,
    val commits: List<GitGraphCommit>,
) : MermaidDiagram

public data class GitGraphBranch(
    val name: String,
    val parentCommitId: String?,
)

public data class GitGraphCommit(
    val id: String,
    val branch: String,
    val parentIds: List<String>,
    val type: GitGraphCommitType = GitGraphCommitType.NORMAL,
    val tag: String? = null,
    val isMerge: Boolean = false,
)

public enum class GitGraphCommitType { NORMAL, REVERSE, HIGHLIGHT }

public enum class SequenceLineStyle {
    SOLID,
    DASHED,
}

public enum class SequenceArrowHead {
    FILLED,
}
