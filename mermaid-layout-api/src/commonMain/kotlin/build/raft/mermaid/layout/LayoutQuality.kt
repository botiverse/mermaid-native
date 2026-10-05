package build.raft.mermaid.layout

/** Soft quality metrics; NaN means undefined, never a passing threshold. */
public data class LayoutQualityScores(
    val edgeLengthRatio: Double,
    val aspectRatio: Double,
    val avgBendsPerEdge: Double,
    val totalBends: Int,
    val crossings: Int,
    val rankFaithfulness: Double,
    val neighborhoodPreservation: Double,
    /** Upstream symmetry scoring is unimplemented. */
    val symmetryScore: Double,
    val boundingBoxArea: Double,
    val straightEdgeRatio: Double,
    /** Simulates the reference rectangle endpoint snap, not Native painting. */
    val renderedDiagonalEndpoints: Int,
) {
    public fun values(): Map<String,Double> = linkedMapOf(
        "edgeLengthRatio" to edgeLengthRatio,"aspectRatio" to aspectRatio,
        "avgBendsPerEdge" to avgBendsPerEdge,"totalBends" to totalBends.toDouble(),
        "crossings" to crossings.toDouble(),"rankFaithfulness" to rankFaithfulness,
        "neighborhoodPreservation" to neighborhoodPreservation,"symmetryScore" to symmetryScore,
        "boundingBoxArea" to boundingBoxArea,"straightEdgeRatio" to straightEdgeRatio,
        "renderedDiagonalEndpoints" to renderedDiagonalEndpoints.toDouble(),
    )
}
public data class LayoutQualityThreshold(val min:Double?=null,val max:Double?=null)
public data class LayoutThresholdResult(val value:Double,val threshold:String,val pass:Boolean)
public data class LayoutQualityResult(val scores:LayoutQualityScores,val thresholdResults:Map<String,LayoutThresholdResult>?)
