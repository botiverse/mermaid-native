package build.raft.mermaid.layout.simple

/** Shared by actual radar drawing and compatibility checks. */
public object RadarGeometry {
    public fun relativeRadius(value: Double, minimum: Double, maximum: Double, radius: Double): Double {
        val range = maximum - minimum
        // Empty, constant-zero and reversed ranges must not introduce NaN into the scene.
        if (!range.isFinite() || range <= 0.0) return 0.0
        return radius * ((value - minimum) / range).coerceIn(0.0, 1.0)
    }
}
