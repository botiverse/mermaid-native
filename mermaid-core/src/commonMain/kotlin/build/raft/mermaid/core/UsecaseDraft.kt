package build.raft.mermaid.core

/** Editable model, independent of the currently published document. Null collections represent
 * incomplete imported drafts and are rejected before replacing any published state. */
public class UsecaseDraft {
    public var actors: MutableMap<String, UsecaseActor>? = linkedMapOf()
    public var useCases: MutableMap<String, UsecaseNode>? = linkedMapOf()
    public var systemBoundaries: MutableMap<String, UsecaseBoundary>? = linkedMapOf()
    public var relationships: MutableList<UsecaseRelationship>? = mutableListOf()
    public var notes: MutableMap<String, UsecaseNote>? = linkedMapOf()
    public var jsonNodes: MutableMap<String, UsecaseJsonNode>? = linkedMapOf()
    public var attributes: MutableMap<String, UsecaseAttributes>? = linkedMapOf()
    public var classDefs: MutableMap<String, Map<String, String>>? = linkedMapOf()
    public var symbols: MutableMap<String, String>? = linkedMapOf()
    public var direction: FlowDirection = FlowDirection.LR
    public var relationshipCounter: Int = 0
    public var noteCounter: Int = 0
    public var accTitle: String = ""
    public var accDescription: String = ""
    public var ast: Map<String, Any?>? = null
    public var config: UsecaseDocumentConfig? = UsecaseDocumentConfig()

    internal fun snapshot(): UsecaseDraft {
        require(actors != null && useCases != null && systemBoundaries != null && relationships != null &&
            notes != null && jsonNodes != null && attributes != null && classDefs != null && symbols != null &&
            config != null && relationshipCounter >= 0 && noteCounter >= 0) {
            "Cannot commit an incomplete usecase model"
        }
        return UsecaseDraft().also { copy ->
            copy.actors = actors!!.toMutableMap()
            copy.useCases = useCases!!.toMutableMap()
            copy.systemBoundaries = systemBoundaries!!.toMutableMap()
            copy.relationships = relationships!!.toMutableList()
            copy.notes = notes!!.toMutableMap()
            copy.jsonNodes = jsonNodes!!.mapValues { (_, node) -> node.copy(data = node.data?.let { data ->
                UsecaseOrderedJsonObject(data.value.mapValues { detachedJson(it.value) }, data.propertyOrder.mapValues { it.value.toList() })
            }) }.toMutableMap()
            copy.attributes = attributes!!.mapValues { (_, a) -> a.copy(properties = a.properties.toMap(), classes = a.classes.toList(), styles = a.styles.toMap()) }.toMutableMap()
            copy.classDefs = classDefs!!.mapValues { it.value.toMap() }.toMutableMap()
            copy.symbols = symbols!!.toMutableMap()
            copy.direction = direction
            copy.relationshipCounter = relationshipCounter
            copy.noteCounter = noteCounter
            copy.accTitle = accTitle
            copy.accDescription = accDescription
            copy.ast = ast?.let { UsecaseSourceAst(it).asMap() }
            copy.config = config!!.copy()
        }
    }

    internal fun diagram(): UsecaseDiagram = UsecaseDiagram(
        direction, actors!!.values.toList(), useCases!!.values.toList(), relationships!!.toList(),
        systemBoundaries!!.values.toList(), notes!!.values.toList(), jsonNodes!!.values.toList(),
        attributes!!.toMap(), classDefs!!.toMap(), accTitle, accDescription,
    )
}

/** Native document configuration. Rendering uses platform text measurement and LayoutConfig;
 * these values describe model defaults, not browser-global configuration. */
public data class UsecaseDocumentConfig(val ellipsePadding: Double = 20.0, val rectanglePadding: Double = 10.0)

private fun detachedJson(value: UsecaseJsonValue): UsecaseJsonValue = when (value) {
    is UsecaseJsonValue.ObjectValue -> UsecaseJsonValue.ObjectValue(value.value.mapValues { detachedJson(it.value) })
    is UsecaseJsonValue.ArrayValue -> UsecaseJsonValue.ArrayValue(value.value.map(::detachedJson))
    else -> value
}
