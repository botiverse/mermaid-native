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
    val subgraphs: List<FlowSubgraph> = emptyList(),
    val accessibilityTitle: String? = null,
    val accessibilityDescription: String? = null,
    val classDefinitions: Map<String,List<String>> = emptyMap(),
    val interactions: List<FlowInteraction> = emptyList(),
    val defaultEdgeStyles: List<String> = emptyList(),
    val defaultInterpolate: String? = null,
) : MermaidDiagram

public data class FlowNode(
    val id: String,
    val label: String,
    val shape: FlowNodeShape = FlowNodeShape.RECTANGLE,
    val labelType: String = "text",
    val borders: String? = null,
    val styles: List<String> = emptyList(),
    val classes: List<String> = emptyList(),
    val createdByStyle: Boolean = false,
    val metadata: Map<String,String> = emptyMap(),
)

public data class FlowSubgraph(
    val id: String,
    val label: String,
    val nodeIds: List<String>,
    val direction: FlowDirection? = null,
    val parentId: String? = null,
    val labelType: String = "text",
    val collapsed: Boolean = false,
    val classes: List<String> = emptyList(),
)

public data class FlowInteraction(val nodeId:String,val value:String,val callback:Boolean=false,val arguments:String?=null,val tooltip:String?=null,val target:String?=null)

public enum class FlowMarker { NONE, POINT, CROSS, CIRCLE }

public enum class FlowEdgeStyle {
    NORMAL,
    THICK,
    DOTTED,
    INVISIBLE,
}

public data class FlowEdge(
    val sourceId: String,
    val targetId: String,
    val style: FlowEdgeStyle = FlowEdgeStyle.NORMAL,
    val label: String? = null,
    val fromMarker: FlowMarker = FlowMarker.NONE,
    val toMarker: FlowMarker = FlowMarker.POINT,
    val length: Int = 1,
    val id: String? = null,
    val labelType: String = "text",
    val styles: List<String> = emptyList(),
    val interpolate: String? = null,
    val animate: Boolean? = null,
    val animation: String? = null,
    val classes: List<String> = emptyList(),
)

public enum class FlowNodeShape {
    RECTANGLE,
    ROUNDED,
    STADIUM,
    CIRCLE,
    DOUBLE_CIRCLE,
    DIAMOND,
    PARALLELOGRAM,
    PARALLELOGRAM_ALT,
    TRAPEZOID,
    TRAPEZOID_ALT,
    SUBROUTINE, CYLINDER, HEXAGON, ASYMMETRIC, ELLIPSE,
    MANUAL_INPUT, DOCUMENTS, PROCESSES,
}

/** Ordered sequence events are the source of truth for vertical placement. */
public sealed interface SequenceEvent

public data class SequenceDiagram(
    val actors: List<SequenceActor>,
    val messages: List<SequenceMessage>,
    val notes: List<SequenceNote> = emptyList(),
    val activations: List<SequenceActivation> = emptyList(),
    val events: List<SequenceEvent> = messages + notes + activations,
    val title: String? = null,
    val accessibilityTitle: String? = null,
    val accessibilityDescription: String? = null,
    val boxes: List<SequenceBox> = emptyList(),
) : MermaidDiagram

public data class SequenceActor(
    val id: String,
    val label: String,
    val kind: SequenceActorKind = SequenceActorKind.PARTICIPANT,
    val wrap: Boolean? = null,
    val links: Map<String, String> = emptyMap(),
    val properties: Map<String, String> = emptyMap(),
)

/** Participant grouping is independent of temporal control fragments. */
public data class SequenceBox(val label: String, val color: String, val actorIds: List<String>)
public data class SequenceLifecycle(val actorId: String, val create: Boolean) : SequenceEvent

public data class SequenceMessage(
    val from: String,
    val to: String,
    val label: String,
    val lineStyle: SequenceLineStyle,
    val arrowHead: SequenceArrowHead,
    val wrap: Boolean? = null,
    val bidirectional: Boolean = false,
    val centralConnection: SequenceCentralConnection = SequenceCentralConnection.NONE,
    val activate: Boolean = false,
    val headAtSource: Boolean = false,
) : SequenceEvent

public data class SequenceNote(
    val position: SequenceNotePosition,
    val actorIds: List<String>,
    val text: String,
    val wrap: Boolean? = null,
) : SequenceEvent

public data class SequenceActivation(
    val actorId: String,
    val activate: Boolean,
) : SequenceEvent

public data class SequenceNumbering(
    val visible: Boolean,
    val start: Double? = null,
    val step: Double? = null,
) : SequenceEvent

public enum class SequenceCentralConnection { NONE, TO, FROM, BOTH }
public enum class SequenceFragmentKind { LOOP, OPT, ALT, PAR, PAR_OVER, CRITICAL, BREAK, RECT }
public enum class SequenceFragmentBoundary { START, BRANCH, END }
public data class SequenceFragment(
    val kind: SequenceFragmentKind,
    val boundary: SequenceFragmentBoundary,
    val label: String = "",
    val wrap: Boolean? = null,
) : SequenceEvent

/** State diagram model for the Mermaid stateDiagram/stateDiagram-v2 family. */
public data class StateDiagram(
    val direction: FlowDirection = FlowDirection.TB,
    val states: List<StateNode>,
    val transitions: List<StateTransition>,
    val notes: List<StateNote> = emptyList(),
    val accessibilityTitle: String? = null,
    val accessibilityDescription: String? = null,
    val classDefinitions: Map<String, List<String>> = emptyMap(),
) : MermaidDiagram

public data class StateNode(
    val id: String,
    val label: String,
    val kind: StateNodeKind = StateNodeKind.STATE,
    val description: String? = null,
    val childIds: List<String> = emptyList(),
    val direction: FlowDirection? = null,
    val explicitLabel: Boolean = kind == StateNodeKind.STATE && label != id,
    val declared: Boolean = explicitLabel,
    val classes: List<String> = emptyList(),
    val styles: List<String> = emptyList(),
)

public enum class StateNodeKind { STATE, START, END, CHOICE, FORK, JOIN, DIVIDER, NOTE }

public data class StateNote(
    val targetId: String,
    val position: StateNotePosition,
    val text: String,
)

public enum class StateNotePosition { LEFT_OF, RIGHT_OF }

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
    val notes: List<ClassNote> = emptyList(),
    val namespaces: List<ClassNamespace> = emptyList(),
    val direction: FlowDirection = FlowDirection.TB,
    val accessibilityTitle: String? = null,
    val accessibilityDescription: String? = null,
    val classDefinitions: Map<String,List<String>> = emptyMap(),
    val interactions: List<ClassInteraction> = emptyList(),
) : MermaidDiagram

public data class ClassDefinition(
    val id: String,
    val label: String = id,
    val members: List<ClassMember> = emptyList(),
    val namespaceName: String? = null,
    val genericType: String? = null,
    val annotations: List<String> = emptyList(),
    val classes: List<String> = emptyList(),
    val styles: List<String> = emptyList(),
)

public data class ClassMember(
    val signature: String,
    val visibility: ClassVisibility = ClassVisibility.PUBLIC,
    val hasVisibility: Boolean = true,
)

/** Host-facing interaction metadata. Callback names are data, never evaluated by the parser. */
public data class ClassInteraction(val classId:String, val value:String, val callback:Boolean = false,
    val arguments:String? = null, val tooltip:String? = null, val target:String? = null)

public data class ClassNote(val text:String, val classId:String? = null, val namespaceName:String? = null)

public data class ClassNamespace(
    val id: String,
    val label: String = id.substringAfterLast('.'),
    val parentId: String? = null,
    val explicit: Boolean = true,
)

public enum class ClassMarker { NONE, INHERITANCE, COMPOSITION, AGGREGATION, ARROW, LOLLIPOP }

public enum class ClassVisibility { PUBLIC, PRIVATE, PROTECTED, PACKAGE }

public data class ClassRelationship(
    val from: String,
    val to: String,
    val kind: ClassRelationshipKind,
    val label: String? = null,
    val fromCardinality: String? = null,
    val toCardinality: String? = null,
    val fromMarker: ClassMarker = when(kind) {
        ClassRelationshipKind.INHERITANCE,ClassRelationshipKind.REALIZATION -> ClassMarker.INHERITANCE
        ClassRelationshipKind.COMPOSITION -> ClassMarker.COMPOSITION
        ClassRelationshipKind.AGGREGATION -> ClassMarker.AGGREGATION
        else -> ClassMarker.NONE
    },
    val toMarker: ClassMarker = if(kind in listOf(ClassRelationshipKind.ASSOCIATION,ClassRelationshipKind.DEPENDENCY)) ClassMarker.ARROW else ClassMarker.NONE,
    val dashed: Boolean = kind in listOf(ClassRelationshipKind.REALIZATION,ClassRelationshipKind.DEPENDENCY,ClassRelationshipKind.DASHED_ASSOCIATION),
)

public enum class ClassRelationshipKind {
    INHERITANCE,        // <|--  or  --|>
    COMPOSITION,        // *--
    AGGREGATION,        // o--
    ASSOCIATION,        // -->
    LINK,               // --
    DEPENDENCY,         // ..>
    REALIZATION,        // ..|>
    DASHED_ASSOCIATION, // .. (plain dashed link)
}

/** Minimal platform-neutral model for the entityRelationshipDiagram family. */
public data class EntityRelationshipDiagram(
    val entities: List<EntityDefinition>,
    val relationships: List<EntityRelationship>,
    val classDefinitions: Map<String, List<String>> = emptyMap(),
    val accessibilityTitle: String? = null,
    val accessibilityDescription: String? = null,
    val direction: FlowDirection = FlowDirection.TB,
    val subgraphs: List<EntitySubgraph> = emptyList(),
    val rootNodeIds: List<String> = entities.map { it.id },
) : MermaidDiagram

public data class EntitySubgraph(
    val id: String,
    val title: String,
    val nodeIds: List<String>,
    val direction: FlowDirection? = null,
    val styles: List<String> = emptyList(),
    val classes: List<String> = emptyList(),
)

public data class EntityDefinition(
    val id: String,
    val attributes: List<EntityAttribute> = emptyList(),
    val alias: String? = null,
    val styles: List<String> = emptyList(),
    val classes: List<String> = emptyList(),
)

public data class EntityAttribute(
    val type: String,
    val name: String,
    val key: EntityKey = EntityKey.NONE,
    val comment: String? = null,
    val additionalKeys: List<EntityKey> = emptyList(),
)

public enum class EntityKey { NONE, PK, FK, UK }

public data class EntityRelationship(
    val from: String,
    val to: String,
    val fromCardinality: EntityCardinality,
    val toCardinality: EntityCardinality,
    val label: String = "",
    val identifying: Boolean = true,
)

public enum class EntityCardinality { ONLY_ONE, ZERO_OR_ONE, ONE_OR_MORE, ZERO_OR_MORE, MD_PARENT }

/** Typed XY chart data, including declared axis ranges and point labels. */
public data class XyChartDiagram(
    val title: String? = null,
    val xAxis: XyAxis,
    val yAxis: NumericAxis,
    val series: List<XySeries>,
    val orientation: XyOrientation? = null,
    val accessibilityTitle: String? = null,
    val accessibilityDescription: String? = null,
    val showDataLabel: Boolean = true,
    val plotColorPalette: List<String> = emptyList(),
) : MermaidDiagram

public enum class XyOrientation { VERTICAL, HORIZONTAL }

public data class XyAxis(
    val title: String? = null,
    val categories: List<String> = emptyList(),
    val range: NumericAxis? = null,
    val titleType: String = "text",
    val categoryTypes: List<String> = emptyList(),
)

public data class NumericAxis(
    val title: String? = null,
    val minimum: Double,
    val maximum: Double,
    val explicitRange: Boolean = true,
    val titleType: String = "text",
)

public data class XySeries(
    val kind: XySeriesKind,
    val values: List<Double>,
    val title: String = "",
    val labels: List<String> = emptyList(),
    val titleType: String = "text",
) {
    /** Visible band slots retain absent values without inventing zero-valued points. */
    public fun valuesFor(axis: XyAxis): List<Double?> =
        if (axis.categories.isEmpty()) values else axis.categories.indices.map { values.getOrNull(it) }

    /** Keep parser text intact while matching the title used by the legend. */
    public val displayTitle: String get() = title.trim()
}

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
    val sourceId: String = if (id.startsWith("__mindmap_")) label else id,
    val icon: String? = null,
    val cssClasses: String? = null,
)

public enum class MindmapNodeShape { DEFAULT, RECTANGLE, DOUBLE_CIRCLE, ROUNDED_RECTANGLE, CLOUD, BANG, HEXAGON }

public data class GanttDiagram(
    val title: String?, val dateFormat: String, val sections: List<GanttSection>,
    val accessibilityTitle: String? = null, val accessibilityDescription: String? = null,
    val excludes: List<String> = emptyList(), val includes: List<String> = emptyList(),
    val inclusiveEndDates: Boolean = false, val todayMarker: String? = null,
    val axisFormat: String? = null, val tickInterval: String? = null,
    val weekday: String = "sunday", val weekend: String = "saturday",
    val displayMode: String? = null, val interactions: List<FlowInteraction> = emptyList(),
) : MermaidDiagram
public data class GanttSection(val name: String, val tasks: List<GanttTask>)
public data class GanttTask(
    val name: String, val id: String, val startDay: Int, val durationDays: Int,
    val status: GanttTaskStatus = GanttTaskStatus.TODO,
    val statuses: Set<GanttTaskStatus> = if(status==GanttTaskStatus.TODO)emptySet()else setOf(status),
    val milestone: Boolean = durationDays==0,
    val renderDurationDays: Int = durationDays,
    /** UTC epoch milliseconds; date-only inputs use midnight for deterministic calendar geometry. */
    val startEpochMillis: Long = (startDay.toLong() - 719528L) * 86_400_000L,
    val durationMillis: Long = durationDays.toLong() * 86_400_000L,
    val renderDurationMillis: Long = renderDurationDays.toLong() * 86_400_000L,
    /** Calendar-adjusted end used for rendering; absent when no calendar adjustment applies. */
    val renderEndEpochMillis: Long? = null,
    val manualEndTime: Boolean = false,
)
public enum class GanttTaskStatus { TODO, DONE, ACTIVE, CRITICAL }

public data class TimelineDiagram(
    val title: String?, val events: List<TimelineEvent>,
    val sections: List<String> = events.mapNotNull { it.section }.distinct(),
    val direction: FlowDirection = FlowDirection.LR,
    val directionExplicit: Boolean = false,
    val accessibilityTitle: String? = null,
    val accessibilityDescription: String? = null,
) : MermaidDiagram
public data class TimelineEvent(val period: String, val labels: List<String>, val section: String? = null, val sectionIndex: Int? = null)

public data class QuadrantChartDiagram(
    val title: String?,
    val xAxis: QuadrantAxis,
    val yAxis: QuadrantAxis,
    val quadrantLabels: List<String?>,
    val points: List<QuadrantPoint>,
    val classes: Map<String, List<String>> = emptyMap(),
    val quadrantLabelTypes: List<String> = List(4) { "text" },
    val accessibilityTitle: String? = null,
    val accessibilityDescription: String? = null,
) : MermaidDiagram
public data class QuadrantAxis(
    val lowLabel: String,
    val highLabel: String,
    val lowDefined: Boolean = true,
    val highDefined: Boolean = true,
    val lowType: String = "text",
    val highType: String = "text",
)
public data class QuadrantPoint(
    val label: String,
    val x: Double,
    val y: Double,
    val labelType: String = "text",
    val className: String = "",
    val styles: List<String> = emptyList(),
    val sourceX: String = x.toString(),
    val sourceY: String = y.toString(),
)

/** Minimal platform-neutral model for the Mermaid user journey family. */
public data class UserJourneyDiagram(
    val title: String?,
    val sections: List<UserJourneySection>,
    val accessibilityTitle: String? = null,
    val accessibilityDescription: String? = null,
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
    val direction: FlowDirection = FlowDirection.LR,
    val currentBranch: String = commits.lastOrNull()?.branch ?: "main",
    val branchHeads: Map<String, String?> = branches.associate { branch -> branch.name to (commits.lastOrNull { it.branch == branch.name }?.id ?: branch.parentCommitId) },
    val title: String? = null,
    val accessibilityTitle: String? = null,
    val accessibilityDescription: String? = null,
    val warnings: List<String> = emptyList(),
) : MermaidDiagram

public data class GitGraphBranch(
    val name: String,
    val parentCommitId: String?,
    val order: Int = 0,
)

public data class GitGraphCommit(
    val id: String,
    val branch: String,
    val parentIds: List<String>,
    val type: GitGraphCommitType = GitGraphCommitType.NORMAL,
    val tag: String? = null,
    val isMerge: Boolean = false,
    val message: String = "",
    val tags: List<String> = listOfNotNull(tag),
    val customId: Boolean = false,
    val customType: GitGraphCommitType? = null,
    val isCherryPick: Boolean = false,
    val sequence: Int = 0,
)

public enum class GitGraphCommitType { NORMAL, REVERSE, HIGHLIGHT }

/** Minimal platform-neutral model for the Mermaid requirementDiagram family. */
public data class RequirementDiagram(
    val requirements: List<RequirementDefinition>,
    val elements: List<RequirementElement>,
    val relationships: List<RequirementRelationship>,
    val accessibilityTitle: String? = null,
    val accessibilityDescription: String? = null,
    val direction: FlowDirection? = null,
    val classDefinitions: Map<String,List<String>> = emptyMap(),
) : MermaidDiagram {
    /** Resolve inherited declarations before direct styles, as consumed by requirement drawing. */
    public fun resolvedStyles(classes: List<String>, styles: List<String>): List<String> =
        classes.flatMap { classDefinitions[it].orEmpty() } + styles
}

public data class RequirementDefinition(
    val name: String,
    val id: String,
    val text: String,
    val risk: RequirementRisk,
    val verifyMethod: RequirementVerifyMethod,
    val type: RequirementType = RequirementType.REQUIREMENT,
    val classes: List<String> = listOf("default"),
    val styles: List<String> = emptyList(),
)

public enum class RequirementType { REQUIREMENT, FUNCTIONAL_REQUIREMENT, INTERFACE_REQUIREMENT, PERFORMANCE_REQUIREMENT, PHYSICAL_REQUIREMENT, DESIGN_CONSTRAINT }
public enum class RequirementRisk { UNSPECIFIED, LOW, MEDIUM, HIGH }
public enum class RequirementVerifyMethod { UNSPECIFIED, ANALYSIS, DEMONSTRATION, INSPECTION, TEST }

public data class RequirementElement(
    val name: String,
    val type: String,
    val docRef: String,
    val classes: List<String> = listOf("default"),
    val styles: List<String> = emptyList(),
)

public data class RequirementRelationship(
    val from: String,
    val to: String,
    val kind: RequirementRelationshipKind,
)

public enum class RequirementRelationshipKind { CONTAINS, COPIES, DERIVES, SATISFIES, VERIFIES, REFINES, TRACES }

public data class KanbanDiagram(val columns: List<KanbanColumn>, val ticketBaseUrl: String? = null) : MermaidDiagram
public data class KanbanColumn(val id: String, val title: String, val cards: List<KanbanCard>, val metadata: KanbanMetadata = KanbanMetadata())
public data class KanbanCard(val id: String, val label: String, val metadata: KanbanMetadata = KanbanMetadata())
public data class KanbanMetadata(
    val icon: String? = null,
    val cssClasses: String? = null,
    val assigned: String? = null,
    val ticket: String? = null,
    val priority: String? = null,
)

/** Minimal platform-neutral model for the Mermaid packet family. */
public data class PacketDiagram(
    val title: String?,
    val fields: List<PacketField>,
    val accessibilityTitle: String? = null,
    val accessibilityDescription: String? = null,
) : MermaidDiagram
public data class PacketField(val startBit: Int, val endBit: Int, val label: String)

/** Minimal platform-neutral model for the Mermaid block diagram family. */
public data class BlockDiagram(
    val columns: Int,
    val nodes: List<BlockNode>,
    val edges: List<BlockEdge>,
    val classes: Map<String, List<String>> = emptyMap(),
    val warnings: List<String> = emptyList(),
) : MermaidDiagram

public data class BlockNode(
    val id: String, val label: String, val columnSpan: Int = 1,
    val type: String = "square", val children: List<BlockNode> = emptyList(),
    val columns: Int = -1, val directions: List<String> = emptyList(),
    val classes: List<String> = emptyList(), val styles: List<String> = emptyList(),
)
public data class BlockEdge(val from: String, val to: String, val label: String? = null)

/** Minimal platform-neutral model for the Mermaid sankey family. */
public data class SankeyDiagram(
    val nodes: List<SankeyNode>,
    val links: List<SankeyLink>,
    val showValues: Boolean = true,
) : MermaidDiagram

public data class SankeyNode(val id: String, val label: String)
public data class SankeyLink(val sourceId: String, val targetId: String, val value: Double)

/** Minimal platform-neutral model for the Mermaid treemap family. */
public data class TreemapDiagram(
    val roots: List<TreemapNode>,
    val title: String? = null,
    val accessibilityTitle: String? = null,
    val accessibilityDescription: String? = null,
    val classes: Map<String, String> = emptyMap(),
    val classAssignments: Map<String, String> = emptyMap(),
    val valueFormat: String? = null,
) : MermaidDiagram
public data class TreemapNode(
    val label: String,
    val value: Double? = null,
    val children: List<TreemapNode> = emptyList(),
    val classSelector: String? = null,
)

/** Minimal platform-neutral model for the Mermaid venn family. */
public data class VennDiagram(
    val title: String? = null,
    val sets: List<VennSet>,
    val unions: List<VennUnion> = emptyList(),
    val texts: List<VennText> = emptyList(),
    val styles: List<VennStyle> = emptyList(),
) : MermaidDiagram
public data class VennText(val setIds: List<String>, val id: String, val label: String? = null)
public data class VennStyle(val targets: List<String>, val properties: Map<String, String>)
public data class VennSet(val id: String, val label: String, val size: Double? = null)
public data class VennUnion(val setIds: List<String>, val label: String? = null, val size: Double? = null)

public data class UsecaseDiagram(
    val direction: FlowDirection = FlowDirection.TB,
    val actors: List<UsecaseActor>,
    val useCases: List<UsecaseNode>,
    val relationships: List<UsecaseRelationship>,
    val boundaries: List<UsecaseBoundary> = emptyList(),
    val notes: List<UsecaseNote> = emptyList(),
    val jsonNodes: List<UsecaseJsonNode> = emptyList(),
    val attributes: Map<String, UsecaseAttributes> = emptyMap(),
    val classDefs: Map<String, Map<String, String>> = emptyMap(),
    val accTitle: String? = null,
    val accDescription: String? = null,
    val labelInsets: UsecaseDocumentConfig = UsecaseDocumentConfig(),
) : MermaidDiagram
public enum class UsecaseActorType { NORMAL, HOLLOW, AWESOME, ICON }
public data class UsecaseActor(
    val id: String, val label: String,
    val type: UsecaseActorType = UsecaseActorType.NORMAL,
    val icon: String? = null, val business: Boolean = false,
    val labelType: String = "text",
)
public enum class UsecaseShape { ELLIPSE, RECTANGLE }
public data class UsecaseNode(
    val id: String, val label: String, val shape: UsecaseShape,
    val labelType: String = "text",
)
/** Shared label inset consumed by Native Usecase layout and document clients. */
public fun UsecaseNode.labelPadding(config: UsecaseDocumentConfig = UsecaseDocumentConfig()): Double =
    if (shape == UsecaseShape.ELLIPSE) config.ellipsePadding else config.rectanglePadding
public enum class UsecaseRelationshipType { ASSOCIATION, INCLUDE, EXTEND, GENERALIZATION }
public data class UsecaseRelationship(
    val sourceId: String, val targetId: String, val label: String? = null,
    val id: String? = null, val startMarker: String = "none", val endMarker: String = "arrow", val dashed: Boolean = false,
    val explicitId: Boolean = false, val minlen: Int = 1,
    val animate: Boolean = false, val animation: String? = null, val labelType: String? = null,
) {
    // Separate endpoint switches retain every marker in production Wasm lowering.
    val arrowType: Int get() = when (startMarker) {
        "arrow" -> 1
        "circle" -> 5
        "cross" -> 6
        else -> when (endMarker) {
            "circle" -> 3
            "cross" -> 4
            "none" -> 2
            else -> 0
        }
    }
    val type: UsecaseRelationshipType get() = when {
        endMarker == "generalization" -> UsecaseRelationshipType.GENERALIZATION
        dashed && label == "include" -> UsecaseRelationshipType.INCLUDE
        dashed && label == "extend" -> UsecaseRelationshipType.EXTEND
        else -> UsecaseRelationshipType.ASSOCIATION
    }
}
public data class UsecaseBoundary(val id: String, val label: String, val labelType: String = "text")
public data class UsecaseNote(val targetId: String, val label: String, val id: String? = null)
public data class UsecaseJsonNode(val id: String, val source: String, val data: UsecaseOrderedJsonObject? = null)
public data class UsecaseAttributes(
    val properties: Map<String, String> = emptyMap(),
    val stereotype: String? = null,
    val classes: List<String> = emptyList(),
    val styles: Map<String, String> = emptyMap(),
    val parentId: String? = null,
)

public data class ArchitectureDiagram(
    val groups: List<ArchitectureGroup>,
    val services: List<ArchitectureService>,
    val edges: List<ArchitectureEdge>,
    val title: String? = null,
    val accTitle: String? = null,
    val accDescription: String? = null,
    val junctions: List<ArchitectureJunction> = emptyList(),
    val layoutHints: List<ArchitectureLayoutHint> = emptyList(),
) : MermaidDiagram
public data class ArchitectureGroup(val id: String, val icon: String, val label: String, val parentId: String? = null)
public data class ArchitectureJunction(val id: String, val groupId: String? = null)
public data class ArchitectureLayoutHint(val direction: String, val members: List<String>)
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
    val boundaries: List<C4Boundary> = emptyList(),
    val diagramType: String = "C4Context",
) : MermaidDiagram
public enum class C4ElementKind { PERSON, SYSTEM, CONTAINER, COMPONENT }
public data class C4Element(
    val id: String, val label: String, val description: String? = null, val kind: C4ElementKind, val external: Boolean = false,
    val technology: String? = null, val variant: String = "", val parentBoundary: String = "global",
    val labelAttribute: String? = null, val attributes: Map<String, String> = emptyMap(),
)
public data class C4Boundary(
    val id: String, val label: String, val type: String = "system", val parentBoundary: String = "global",
    val labelAttribute: String? = null, val attributes: Map<String, String> = emptyMap(),
)
public data class C4Relationship(val sourceId: String, val targetId: String, val label: String, val technology: String? = null, val bidirectional: Boolean = false)

/** Bounded platform-neutral model for the Ishikawa (fishbone) family. */
public data class IshikawaDiagram(val effect: IshikawaNode) : MermaidDiagram

public data class IshikawaNode(val text: String, val children: List<IshikawaNode> = emptyList())

/** Bounded platform-neutral model for the Cynefin framework family. */
public data class CynefinDiagram(
    val title: String? = null,
    val domains: List<CynefinDomainBlock>,
    val transitions: List<CynefinTransition>,
    val accTitle: String? = null,
    val accDescription: String? = null,
) : MermaidDiagram

public enum class CynefinDomain { COMPLEX, COMPLICATED, CLEAR, CHAOTIC, CONFUSION }
public data class CynefinDomainBlock(val domain: CynefinDomain, val items: List<String>)
public data class CynefinTransition(val from: CynefinDomain, val to: CynefinDomain, val label: String? = null)

/** Bounded platform-neutral model for the Mermaid swimlanes family. */
public data class SwimlaneDiagram(
    val direction: FlowDirection = FlowDirection.TB,
    val lanes: List<Swimlane>,
    val edges: List<SwimlaneEdge>,
    val flowchart: FlowchartDiagram? = null,
    val lineHops: SwimlaneLineHops = SwimlaneLineHops.ARC,
) : MermaidDiagram

public enum class SwimlaneLineHops { DISABLED, ARC, GAP }

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
public data class TreeViewDiagram(
    val nodes: List<TreeViewNode>,
    val title: String? = null,
    val accTitle: String? = null,
    val accDescription: String? = null,
    val iconConfig: TreeViewIconConfig = TreeViewIconConfig(),
) : MermaidDiagram
public data class TreeViewNode(
    val label: String,
    val depth: Int,
    val parentIndex: Int?,
    val directory: Boolean,
    val sourceIndent: Int? = null,
    val classAnnotation: String? = null,
    val iconAnnotation: String? = null,
    val description: String? = null,
)

/** Bounded platform-neutral model for Mermaid railroad-beta expression trees. */
public data class RailroadDiagram(
    val title: String? = null,
    val rules: List<RailroadRule>,
    val accTitle: String? = null,
    val accDescription: String? = null,
) : MermaidDiagram

public data class RailroadRule(
    val name: String,
    val definition: RailroadNode,
)

public sealed interface RailroadNode
public data class RailroadTerminal(val label: String) : RailroadNode
public data class RailroadNonTerminal(val label: String) : RailroadNode
public data class RailroadSpecial(val text: String) : RailroadNode
public data class RailroadSequence(val children: List<RailroadNode>) : RailroadNode
public data class RailroadChoice(val children: List<RailroadNode>) : RailroadNode
public data class RailroadOptional(val child: RailroadNode) : RailroadNode
public data class RailroadOneOrMore(val child: RailroadNode) : RailroadNode
public data class RailroadZeroOrMore(val child: RailroadNode) : RailroadNode
/** A finite or lower-bounded repeat; max == null denotes no upper bound. */
public data class RailroadRepetition(val child: RailroadNode, val min: Int, val max: Int? = null) : RailroadNode

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
    val minimum: Double = 0.0,
    val options: List<RadarOption> = emptyList(),
    val accessibilityTitle: String? = null,
    val accessibilityDescription: String? = null,
) : MermaidDiagram {
    /** Resolved options consumed by the renderer; raw options remain available to grammar clients. */
    public val resolvedOptions: RadarResolvedOptions
        get() = RadarResolvedOptions(
            ticks = (options.lastOrNull { it.name == "ticks" }?.number ?: 5.0).coerceIn(0.0, 32.0).toInt(),
            maximum = options.lastOrNull { it.name == "max" }?.number,
            minimum = minimum,
            showLegend = options.lastOrNull { it.name == "showLegend" }?.flag != false,
            graticule = options.lastOrNull { it.name == "graticule" }?.text ?: "circle",
        )

    /** Place named values on their declared axes, preserving the first matching entry. */
    public fun axisValues(curve: RadarCurve): List<Double> = if (curve.entries.any { it.axis != null }) {
        axes.map { axis -> curve.entries.firstOrNull { it.axis == axis.id }?.value ?: minimum }
    } else curve.values
}

public data class RadarResolvedOptions(
    val ticks: Int,
    val maximum: Double?,
    val minimum: Double,
    val showLegend: Boolean,
    val graticule: String,
)

public data class RadarAxis(val id: String, val label: String)

public data class RadarCurve(
    val id: String,
    val label: String,
    val values: List<Double>,
    val entries: List<RadarEntry> = values.map { RadarEntry(it) },
)

public data class RadarEntry(val value: Double, val axis: String? = null)

public data class RadarOption(val name: String, val number: Double? = null, val flag: Boolean? = null, val text: String? = null)

/** Bounded platform-neutral model for Mermaid wardley-beta maps. */
public data class WardleyMapDiagram(
    val title: String? = null,
    val nodes: List<WardleyNode>,
    val links: List<WardleyLink>,
    val evolutions: List<WardleyEvolution>,
    val notes: List<WardleyNote>,
    val stages: List<String> = listOf("Genesis", "Custom Built", "Product", "Commodity"),
    val stageBoundaries: List<Double> = emptyList(),
    val annotations: List<WardleyAnnotation> = emptyList(),
    val annotationsBox: WardleyNote? = null,
    val accelerators: List<WardleyNote> = emptyList(),
    val deaccelerators: List<WardleyNote> = emptyList(),
    val width: Double? = null, val height: Double? = null,
) : MermaidDiagram

public data class WardleyNode(
    val name: String,
    val visibility: Double,
    val evolution: Double,
    val anchor: Boolean,
    val pipeline: String? = null,
    val labelOffsetX: Double? = null, val labelOffsetY: Double? = null,
    val sourceStrategy: String? = null, val inertia: Boolean = false,
)

public data class WardleyLink(val from: String, val to: String, val flow: String = "forward", val label: String? = null)
public data class WardleyAnnotation(val id: String, val text: String, val visibility: Double, val evolution: Double)

public data class WardleyEvolution(val component: String, val evolution: Double)

public data class WardleyNote(val text: String, val visibility: Double, val evolution: Double)

public data class EventModelingDiagram(val title: String?, val frames: List<EventModelingFrame>, val relations: List<EventModelingRelation>, val data: Map<String, EventModelingData> = emptyMap(), val accTitle: String? = null, val accDescription: String? = null, val notes: List<EventModelingNote> = emptyList(), val scenarios: List<EventModelingScenario> = emptyList(), val entities: List<String> = emptyList()) : MermaidDiagram
public enum class EventModelingEntityKind { UI, COMMAND, EVENT, PROCESSOR, READ_MODEL }
public data class EventModelingFrame(val id: String, val entityId: String, val kind: EventModelingEntityKind, val reset: Boolean = false, val inlineData: EventModelingData? = null, val dataReference: String? = null, val sourceType: String = kind.name.lowercase().replace("read_model", "readmodel"))
public data class EventModelingNote(val frameId: String, val data: EventModelingData)
public data class EventModelingStep(val kind: String, val entityId: String)
public data class EventModelingScenario(val frameId: String, val given: List<EventModelingStep>, val whenSteps: List<EventModelingStep>, val thenSteps: List<EventModelingStep>)
public data class EventModelingData(val type: String, val value: String)
public data class EventModelingRelation(val sourceFrameId: String, val targetFrameId: String)

public enum class SequenceLineStyle {
    SOLID,
    DASHED,
}

public enum class SequenceArrowHead {
    HALF_FILLED_TOP, HALF_FILLED_BOTTOM, HALF_OPEN_TOP, HALF_OPEN_BOTTOM,
    NONE,
    FILLED,
    OPEN,
    CROSS,
    CIRCLE,
}

public enum class SequenceActorKind {
    PARTICIPANT, ACTOR, BOUNDARY, CONTROL, ENTITY, DATABASE, COLLECTIONS, QUEUE,
}

public enum class SequenceNotePosition {
    LEFT_OF,
    RIGHT_OF,
    OVER,
}

/** Native build identity for the Mermaid info diagram. */
public data class InfoDiagram(val showInfo: Boolean = false, val version: String = MERMAID_NATIVE_VERSION) : MermaidDiagram
