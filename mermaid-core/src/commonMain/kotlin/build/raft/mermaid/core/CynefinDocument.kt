package build.raft.mermaid.core

/** Editable Cynefin state; null updates are ignored and exported diagrams are detached. */
public class CynefinDocument {
    private val domainItems = linkedMapOf<CynefinDomain, List<String>>()
    private var links = emptyList<CynefinTransition>()
    public var title: String = ""
    public var accessibilityTitle: String = ""
    public var accessibilityDescription: String = ""

    public fun setDomains(blocks: List<CynefinDomainBlock>?) {
        blocks?.forEach { domainItems[it.domain] = it.items.toList() }
    }

    public fun setTransitions(transitions: List<CynefinTransition>?) {
        if (transitions != null) links = transitions.filter { it.from != it.to }
            .map { it.copy(label = it.label?.takeIf(String::isNotEmpty)) }
    }

    public fun domains(): List<CynefinDomainBlock> = domainItems.map { CynefinDomainBlock(it.key, it.value.toList()) }
    public fun transitions(): List<CynefinTransition> = links.toList()
    public fun clear() {
        domainItems.clear(); links = emptyList()
        title = ""; accessibilityTitle = ""; accessibilityDescription = ""
    }

    /** Pinned upstream defaults; browser configuration overrides are not owned by this document. */
    public fun configuration(): Map<String, Any> = mapOf(
        "width" to 800, "height" to 600, "padding" to 40, "showDomainDescriptions" to true,
        "boundaryAmplitude" to 8, "seed" to 0, "useMaxWidth" to true,
    )

    public fun diagram(): CynefinDiagram = CynefinDiagram(
        title.takeIf(String::isNotEmpty), domains(), transitions(),
        accessibilityTitle.takeIf(String::isNotEmpty), accessibilityDescription.takeIf(String::isNotEmpty),
    )
}
