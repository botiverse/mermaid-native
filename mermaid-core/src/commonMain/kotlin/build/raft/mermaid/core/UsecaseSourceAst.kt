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

    private var committedDraft: UsecaseDraft? = null
    public var title: String = ""
    public fun createModel(): UsecaseDraft = UsecaseDraft()
    /** Use-case label geometry hints shared with the Native layout consumer. */
    public fun usecaseLabelData(): List<Map<String, Any>> = diagram?.useCases.orEmpty().map { node ->
        mapOf("id" to node.id, "label" to node.label, "padding" to node.labelPadding(diagram!!.labelInsets))
    }
    public fun configuration(): UsecaseDocumentConfig = committedDraft?.config?.copy() ?: UsecaseDocumentConfig()

    /** Validate and detach every collection before atomically publishing the replacement. */
    public fun commit(draft: UsecaseDraft) {
        val next = draft.snapshot()
        val nextDiagram = next.diagram()
        val nextAst = next.ast?.let(::UsecaseSourceAst)
        committedDraft = next
        diagram = nextDiagram
        ast = nextAst
    }

    public fun setAccessibilityTitle(value: String) {
        diagram = (diagram ?: UsecaseDraft().diagram()).copy(accTitle = value)
    }
    public fun setAccessibilityDescription(value: String) {
        diagram = (diagram ?: UsecaseDraft().diagram()).copy(accDescription = value)
    }

    public fun clear() { diagram = null; ast = null; committedDraft = null; title = "" }

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
