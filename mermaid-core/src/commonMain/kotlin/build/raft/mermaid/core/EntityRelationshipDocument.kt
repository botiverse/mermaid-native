package build.raft.mermaid.core

/** Editable ER graph. Input names remain stable renderer IDs; exported node IDs match the
 * document's generated entity identities. Neither representation depends on browser state. */
public class EntityRelationshipDocument {
    private val entities = linkedMapOf<String, EntityDefinition>()
    private val identities = linkedMapOf<String, String>()
    private val groups = linkedMapOf<String, EntitySubgraph>()
    private val relations = mutableListOf<EntityRelationship>()

    public fun clear() { entities.clear(); identities.clear(); groups.clear(); relations.clear() }

    public fun addEntity(name: String): EntityDocumentNode {
        if (name !in entities) {
            identities[name] = "entity-$name-${entities.size}"
            entities[name] = EntityDefinition(name)
        }
        return entity(name)!!
    }

    public fun entity(name: String): EntityDocumentNode? = entities[name]?.let {
        EntityDocumentNode(identities.getValue(name), name, null, false, it.styles.toList(), it.classes.toList())
    }

    public fun addSubgraph(id: String, nodes: List<String>, title: String): String {
        val key = id.trim()
        require(key !in groups) { "Duplicate ER subgraph: $key" }
        val assigned = groups.values.flatMap { it.nodeIds }.toSet()
        val members = nodes.filter { it.isNotBlank() }.distinctBy { it.trim() }.filter { it !in assigned }
        groups[key] = EntitySubgraph(key, title.trim(), members.toList())
        return key
    }

    public fun subgraphs(): List<EntitySubgraph> = groups.values.map {
        it.copy(nodeIds = it.nodeIds.toList(), styles = it.styles.toList(), classes = it.classes.toList())
    }

    public fun addRelationship(from: String, label: String, to: String, fromCardinality: EntityCardinality,
                               toCardinality: EntityCardinality, identifying: Boolean = true) {
        if (from !in groups) addEntity(from)
        if (to !in groups) addEntity(to)
        relations += EntityRelationship(from, to, fromCardinality, toCardinality, label, identifying)
    }

    public fun addClasses(ids: List<String>, classes: List<String>) {
        ids.forEach { id ->
            entities[id]?.let { entities[id] = it.copy(classes = it.classes + classes) }
            groups[id]?.let { groups[id] = it.copy(classes = it.classes + classes) }
        }
    }

    public fun addStyles(ids: List<String>, styles: List<String>) {
        ids.forEach { id ->
            entities[id]?.let { entities[id] = it.copy(styles = it.styles + styles) }
            groups[id]?.let { groups[id] = it.copy(styles = it.styles + styles) }
        }
    }

    public fun diagram(): EntityRelationshipDiagram {
        validateMembership()
        val grouped = groups.values.flatMap { it.nodeIds }.toSet()
        return EntityRelationshipDiagram(
            entities.values.filter { it.id !in groups }.map { it.copy(styles = it.styles.toList(), classes = it.classes.toList()) },
            relations.toList(), subgraphs = subgraphs(),
            rootNodeIds = (entities.keys + groups.keys).filter { it !in grouped },
        )
    }

    /** Browser-independent node/edge projection for clients inspecting the editable graph. */
    public fun data(): EntityDocumentData {
        validateMembership()
        val parents = linkedMapOf<String, String>()
        groups.values.reversed().forEach { g -> g.nodeIds.forEach { parents[it] = g.id } }
        val nodes = groups.values.reversed().map { g ->
            EntityDocumentNode(g.id, g.title, parents[g.id], true, g.styles.toList(), g.classes.toList())
        } + entities.values.filter { it.id !in groups }.map { e ->
            EntityDocumentNode(identities.getValue(e.id), e.id, parents[e.id], false, e.styles.toList(), e.classes.toList())
        }
        val edges = relations.map { r -> EntityDocumentEdge(identity(r.from), identity(r.to), r.label,
            r.fromCardinality, r.toCardinality, r.identifying) }
        return EntityDocumentData(nodes, edges)
    }

    private fun identity(name: String): String = if (name in groups) name else identities.getValue(name)
    private fun validateMembership() {
        val active = mutableSetOf<String>(); val visited = mutableSetOf<String>()
        fun visit(id: String) {
            if (id !in groups || id in visited) return
            require(active.add(id)) { "Cyclic ER subgraph membership" }
            groups.getValue(id).nodeIds.forEach(::visit)
            active.remove(id); visited.add(id)
        }
        groups.keys.forEach(::visit)
    }
}

public data class EntityDocumentNode(val id: String, val label: String, val parentId: String?,
    val isGroup: Boolean, val styles: List<String>, val classes: List<String>)
public data class EntityDocumentEdge(val start: String, val end: String, val label: String,
    val fromCardinality: EntityCardinality, val toCardinality: EntityCardinality, val identifying: Boolean)
public data class EntityDocumentData(val nodes: List<EntityDocumentNode>, val edges: List<EntityDocumentEdge>)
