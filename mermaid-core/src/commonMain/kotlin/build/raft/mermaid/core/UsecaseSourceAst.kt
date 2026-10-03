package build.raft.mermaid.core

/** Source-ordered Usecase syntax snapshot. Ranges use half-open UTF-16 offsets. */
public class UsecaseSourceAst internal constructor(private val content: Map<String, Any?>) {
    /** A detached, JSON-compatible snapshot; callers cannot mutate parser-owned collections. */
    public fun asMap(): Map<String, Any?> = copyMap(content)

    private fun copyValue(value: Any?): Any? = when (value) {
        is Map<*, *> -> value.entries.associate { it.key.toString() to copyValue(it.value) }
        is List<*> -> value.map(::copyValue)
        else -> value
    }
    private fun copyMap(value: Map<String, Any?>): Map<String, Any?> = value.mapValues { copyValue(it.value) }
}

/** Stateful editor document: a failed parse or explicit clear removes all published state. */
public class UsecaseDocument {
    public var diagram: UsecaseDiagram? = null
        private set
    public var ast: UsecaseSourceAst? = null
        private set
    public val direction: FlowDirection get() = diagram?.direction ?: FlowDirection.LR
    public val accessibilityTitle: String get() = diagram?.accTitle.orEmpty()
    public val accessibilityDescription: String get() = diagram?.accDescription.orEmpty().lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")

    public fun clear() { diagram = null; ast = null }

    public fun parse(source: String): MermaidParseResult {
        clear()
        val parser = UsecaseParser(source)
        val result = parser.parseValidated()
        if (result is MermaidParseResult.Success) {
            val usecase = result.diagram as? UsecaseDiagram
                ?: return MermaidParseResult.Failure(listOf(MermaidDiagnostic(
                    MermaidDiagnosticCode.UNSUPPORTED_DIAGRAM, "Expected a Usecase document", SourceLocation(1, 1),
                )))
            diagram = usecase
            ast = parser.sourceAst
        }
        return result
    }
}
