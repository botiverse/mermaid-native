package build.raft.mermaid.core

public data class SourceLocation(
    /** One-based physical source line. */
    val line: Int,
    /** One-based physical source column. */
    val column: Int,
)

public enum class MermaidDiagnosticCode {
    EMPTY_SOURCE,
    UNSUPPORTED_DIAGRAM,
    INVALID_HEADER,
    UNSUPPORTED_SYNTAX,
    INVALID_VALUE,
}

public data class MermaidDiagnostic(
    val code: MermaidDiagnosticCode,
    val message: String,
    val location: SourceLocation,
)

public sealed interface MermaidParseResult {
    public data class Success(val diagram: MermaidDiagram) : MermaidParseResult

    public data class Failure(val diagnostics: List<MermaidDiagnostic>) : MermaidParseResult {
        init {
            require(diagnostics.isNotEmpty()) { "A failed parse must contain at least one diagnostic" }
        }
    }
}
