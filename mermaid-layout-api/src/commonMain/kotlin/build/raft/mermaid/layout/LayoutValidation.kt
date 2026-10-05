package build.raft.mermaid.layout

/** Measured geometry in scene coordinates, independent of a renderer or source parser. */
public data class LayoutValidationNode(
    val id: String,
    val bounds: SceneRect,
    val parentId: String? = null,
    val isGroup: Boolean = false,
    val isEdgeLabel: Boolean = false,
    val isDummy: Boolean = false,
    val shape: String? = null,
    val groupTitleBounds: SceneRect? = null,
)

public data class LayoutValidationEdge(
    val id: String,
    val points: List<ScenePoint>,
    val start: String? = null,
    val end: String? = null,
    val labelNodeId: String? = null,
    val label: String? = null,
    val labelBounds: SceneRect? = null,
    val arrowTypeStart: String? = null,
    val arrowTypeEnd: String? = null,
    val type: String? = null,
)

public data class LayoutValidationInput(
    val nodes: List<LayoutValidationNode>,
    val edges: List<LayoutValidationEdge>,
)

/** Issue identifiers follow Mermaid's orthogonal layout validation vocabulary. */
public data class LayoutIssue(
    val type: String,
    val message: String,
    val nodeIds: List<String> = emptyList(),
    val edgeId: String? = null,
    val details: Map<String, Any?> = emptyMap(),
)

public data class LayoutEdgePenalty(val id: String, val points: Int, val bendPenalty: Double)

public data class LayoutValidationBreakdown(
    val nodeCount: Int,
    val edgeCount: Int,
    val crossings: Int,
    val totalPoints: Int,
    val totalBendPenalty: Double,
    val crossingPenalty: Double,
    val edges: List<LayoutEdgePenalty>,
    val pointsHistogram: Map<String, Int>,
)

/** A diagnostic, not proof of full visual correctness. Non-orthogonal routes are reported. */
public data class LayoutValidationResult(
    val ok: Boolean,
    val issues: List<LayoutIssue>,
    val score: Double,
    val breakdown: LayoutValidationBreakdown,
)

/** Input uses final scene coordinates, after any padding/route translation. */
public data class LayoutValidationReport(val geometry: LayoutValidationInput, val result: LayoutValidationResult, val quality: LayoutQualityResult? = null)
