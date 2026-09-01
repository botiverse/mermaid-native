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

public enum class FlowEdgeStyle {
    NORMAL,
    THICK,
}

public data class FlowEdge(
    val sourceId: String,
    val targetId: String,
    val style: FlowEdgeStyle = FlowEdgeStyle.NORMAL,
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
    val namespaceName: String? = null,
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
public data class TimelineEvent(val period: String, val labels: List<String>, val section: String? = null)

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

/** Minimal platform-neutral model for the Mermaid requirementDiagram family. */
public data class RequirementDiagram(
    val requirements: List<RequirementDefinition>,
    val elements: List<RequirementElement>,
    val relationships: List<RequirementRelationship>,
    val accessibilityTitle: String? = null,
    val accessibilityDescription: String? = null,
) : MermaidDiagram

public data class RequirementDefinition(
    val name: String,
    val id: String,
    val text: String,
    val risk: RequirementRisk,
    val verifyMethod: RequirementVerifyMethod,
    val type: RequirementType = RequirementType.REQUIREMENT,
)

public enum class RequirementType { REQUIREMENT, FUNCTIONAL_REQUIREMENT }
public enum class RequirementRisk { LOW, MEDIUM, HIGH }
public enum class RequirementVerifyMethod { ANALYSIS, DEMONSTRATION, INSPECTION, TEST }

public data class RequirementElement(
    val name: String,
    val type: String,
    val docRef: String,
)

public data class RequirementRelationship(
    val from: String,
    val to: String,
    val kind: RequirementRelationshipKind,
)

public enum class RequirementRelationshipKind { CONTAINS, SATISFIES, VERIFIES }

public data class KanbanDiagram(val columns: List<KanbanColumn>) : MermaidDiagram
public data class KanbanColumn(val id: String, val title: String, val cards: List<KanbanCard>)
public data class KanbanCard(val id: String, val label: String)

/** Minimal platform-neutral model for the Mermaid packet family. */
public data class PacketDiagram(val title: String?, val fields: List<PacketField>) : MermaidDiagram
public data class PacketField(val startBit: Int, val endBit: Int, val label: String)

/** Minimal platform-neutral model for the Mermaid block diagram family. */
public data class BlockDiagram(
    val columns: Int,
    val nodes: List<BlockNode>,
    val edges: List<BlockEdge>,
) : MermaidDiagram

public data class BlockNode(val id: String, val label: String, val columnSpan: Int = 1)
public data class BlockEdge(val from: String, val to: String)

/** Minimal platform-neutral model for the Mermaid sankey family. */
public data class SankeyDiagram(
    val nodes: List<SankeyNode>,
    val links: List<SankeyLink>,
) : MermaidDiagram

public data class SankeyNode(val id: String, val label: String)
public data class SankeyLink(val sourceId: String, val targetId: String, val value: Double)

/** Minimal platform-neutral model for the Mermaid treemap family. */
public data class TreemapDiagram(val roots: List<TreemapNode>) : MermaidDiagram
public data class TreemapNode(
    val label: String,
    val value: Double? = null,
    val children: List<TreemapNode> = emptyList(),
)

/** Minimal platform-neutral model for the Mermaid venn family. */
public data class VennDiagram(
    val title: String? = null,
    val sets: List<VennSet>,
    val unions: List<VennUnion> = emptyList(),
) : MermaidDiagram
public data class VennSet(val id: String, val label: String, val size: Double? = null)
public data class VennUnion(val setIds: List<String>, val label: String? = null, val size: Double? = null)

public data class UsecaseDiagram(
    val direction: FlowDirection = FlowDirection.TB,
    val actors: List<UsecaseActor>,
    val useCases: List<UsecaseNode>,
    val relationships: List<UsecaseRelationship>,
) : MermaidDiagram
public data class UsecaseActor(val id: String, val label: String)
public enum class UsecaseShape { ELLIPSE, RECTANGLE }
public data class UsecaseNode(val id: String, val label: String, val shape: UsecaseShape)
public data class UsecaseRelationship(val sourceId: String, val targetId: String, val label: String? = null)

public data class ArchitectureDiagram(
    val groups: List<ArchitectureGroup>,
    val services: List<ArchitectureService>,
    val edges: List<ArchitectureEdge>,
) : MermaidDiagram
public data class ArchitectureGroup(val id: String, val icon: String, val label: String)
public data class ArchitectureService(val id: String, val icon: String, val label: String, val groupId: String? = null)
public enum class ArchitecturePort { TOP, BOTTOM, LEFT, RIGHT }
public data class ArchitectureEdge(
    val sourceId: String,
    val sourcePort: ArchitecturePort,
    val targetId: String,
    val targetPort: ArchitecturePort,
    val directed: Boolean,
)

public data class C4Diagram(
    val title: String? = null,
    val elements: List<C4Element>,
    val relationships: List<C4Relationship>,
) : MermaidDiagram
public enum class C4ElementKind { PERSON, SYSTEM }
public data class C4Element(val id: String, val label: String, val description: String? = null, val kind: C4ElementKind, val external: Boolean = false)
public data class C4Relationship(val sourceId: String, val targetId: String, val label: String, val technology: String? = null, val bidirectional: Boolean = false)

/** Bounded platform-neutral model for the Ishikawa (fishbone) family. */
public data class IshikawaDiagram(val effect: IshikawaNode) : MermaidDiagram

public data class IshikawaNode(val text: String, val children: List<IshikawaNode> = emptyList())

/** Bounded platform-neutral model for the Cynefin framework family. */
public data class CynefinDiagram(
    val title: String? = null,
    val domains: List<CynefinDomainBlock>,
    val transitions: List<CynefinTransition>,
) : MermaidDiagram

public enum class CynefinDomain { COMPLEX, COMPLICATED, CLEAR, CHAOTIC, CONFUSION }
public data class CynefinDomainBlock(val domain: CynefinDomain, val items: List<String>)
public data class CynefinTransition(val from: CynefinDomain, val to: CynefinDomain, val label: String? = null)

/** Bounded platform-neutral model for the Mermaid swimlanes family. */
public data class SwimlaneDiagram(
    val direction: FlowDirection = FlowDirection.TB,
    val lanes: List<Swimlane>,
    val edges: List<SwimlaneEdge>,
) : MermaidDiagram

public data class Swimlane(
    val id: String,
    val label: String,
    val nodes: List<SwimlaneNode>,
)

public data class SwimlaneNode(
    val id: String,
    val label: String,
    val shape: SwimlaneNodeShape,
)

public enum class SwimlaneNodeShape { RECTANGLE, ROUNDED, STADIUM, DECISION, CIRCLE }

public data class SwimlaneEdge(
    val sourceId: String,
    val targetId: String,
    val label: String? = null,
)

/** Bounded platform-neutral model for Mermaid treeView-beta indentation trees. */
public data class TreeViewDiagram(val nodes: List<TreeViewNode>) : MermaidDiagram
public data class TreeViewNode(
    val label: String,
    val depth: Int,
    val parentIndex: Int?,
    val directory: Boolean,
)

/** Bounded platform-neutral model for Mermaid railroad-beta expression trees. */
public data class RailroadDiagram(val root: RailroadNode) : MermaidDiagram
public sealed interface RailroadNode
public data class RailroadTerminal(val label: String) : RailroadNode
public data class RailroadNonTerminal(val label: String) : RailroadNode
public data object RailroadSkip : RailroadNode
public data object RailroadStart : RailroadNode
public data object RailroadEnd : RailroadNode
public data class RailroadSequence(val children: List<RailroadNode>) : RailroadNode
public data class RailroadStack(val children: List<RailroadNode>) : RailroadNode
public data class RailroadChoice(val priority: Int, val children: List<RailroadNode>) : RailroadNode
public data class RailroadOptional(val child: RailroadNode) : RailroadNode
public data class RailroadOneOrMore(val child: RailroadNode) : RailroadNode
public data class RailroadZeroOrMore(val child: RailroadNode) : RailroadNode

/** Bounded platform-neutral model for the Mermaid zenuml family. */
public data class ZenumlDiagram(
    val title: String? = null,
    val participants: List<ZenumlParticipant>,
    val messages: List<ZenumlMessage>,
) : MermaidDiagram

public data class ZenumlParticipant(
    val id: String,
    val label: String,
)

public sealed interface ZenumlMessage {
    public val from: String
    public val to: String
}

public data class ZenumlSyncMessage(
    override val from: String,
    override val to: String,
    val method: String,
) : ZenumlMessage

public data class ZenumlAsyncMessage(
    override val from: String,
    override val to: String,
    val label: String,
) : ZenumlMessage

/** Bounded platform-neutral model for the Mermaid radar-beta family. */
public data class RadarChartDiagram(
    val title: String? = null,
    val axes: List<RadarAxis>,
    val curves: List<RadarCurve>,
    val maximum: Double,
) : MermaidDiagram

public data class RadarAxis(val id: String, val label: String)

public data class RadarCurve(val id: String, val label: String, val values: List<Double>)

/** Bounded platform-neutral model for Mermaid wardley-beta maps. */
public data class WardleyMapDiagram(
    val title: String? = null,
    val nodes: List<WardleyNode>,
    val links: List<WardleyLink>,
    val evolutions: List<WardleyEvolution>,
    val notes: List<WardleyNote>,
) : MermaidDiagram

public data class WardleyNode(
    val name: String,
    val visibility: Double,
    val evolution: Double,
    val anchor: Boolean,
)

public data class WardleyLink(val from: String, val to: String)

public data class WardleyEvolution(val component: String, val evolution: Double)

public data class WardleyNote(val text: String, val visibility: Double, val evolution: Double)

public data class EventModelingDiagram(val title: String?, val frames: List<EventModelingFrame>, val relations: List<EventModelingRelation>) : MermaidDiagram
public enum class EventModelingEntityKind { UI, COMMAND, EVENT, PROCESSOR, READ_MODEL }
public data class EventModelingFrame(val id: String, val entityId: String, val kind: EventModelingEntityKind, val reset: Boolean = false)
public data class EventModelingRelation(val sourceFrameId: String, val targetFrameId: String)

public enum class SequenceLineStyle {
    SOLID,
    DASHED,
}

public enum class SequenceArrowHead {
    FILLED,
}
