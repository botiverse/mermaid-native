package build.raft.mermaid.core

/**
 * Editable railroad rules with append order and last-definition lookup.
 * [diagram] exports detached values to the existing Native renderer.
 * Labels are plain text; script/style blocks are discarded for display. This is
 * not an HTML sanitizer and does not reproduce browser DOMPurify configuration.
 */
public class RailroadDocument {
    private val entries = mutableListOf<RailroadRule>()
    private val byName = linkedMapOf<String, RailroadRule>()

    public var title: String = ""
        set(value) { field = displayText(value) }
    public var accessibilityTitle: String = ""
        set(value) { field = displayText(value).trimStart() }
    public var accessibilityDescription: String = ""
        set(value) { field = displayText(value).replace(Regex("\n\\s+"), "\n") }

    public fun clear() {
        entries.clear()
        byName.clear()
        title = ""
        accessibilityTitle = ""
        accessibilityDescription = ""
    }

    public fun addRule(rule: RailroadRule) {
        val stored = RailroadRule(displayText(rule.name), mapText(rule.definition, ::displayText))
        entries += stored
        byName[stored.name] = stored
    }

    public fun rules(): List<RailroadRule> = entries.map(::snapshot)
    public fun rule(name: String): RailroadRule? = byName[name]?.let(::snapshot)
    public fun diagram(): RailroadDiagram = RailroadDiagram(
        title.takeIf { it.isNotEmpty() }, rules(),
        accessibilityTitle.takeIf { it.isNotEmpty() }, accessibilityDescription.takeIf { it.isNotEmpty() },
    )

    private fun snapshot(rule: RailroadRule): RailroadRule =
        RailroadRule(rule.name, mapText(rule.definition) { it })

    private fun mapText(node: RailroadNode, transform: (String) -> String): RailroadNode = when (node) {
        is RailroadTerminal -> node.copy(label = transform(node.label))
        is RailroadNonTerminal -> node.copy(label = transform(node.label))
        is RailroadSpecial -> node.copy(text = transform(node.text))
        is RailroadSequence -> node.copy(children = node.children.map { mapText(it, transform) })
        is RailroadChoice -> node.copy(children = node.children.map { mapText(it, transform) })
        is RailroadOptional -> node.copy(child = mapText(node.child, transform))
        is RailroadOneOrMore -> node.copy(child = mapText(node.child, transform))
        is RailroadZeroOrMore -> node.copy(child = mapText(node.child, transform))
        is RailroadRepetition -> node.copy(child = mapText(node.child, transform))
    }

    private fun displayText(text: String): String = activeTextBlocks.replace(text, "")

    private companion object {
        // Raw text blocks have no useful label content. Other markup remains literal text.
        val activeTextBlocks = Regex(
            "<(script|style)\\b[^>]*>[\\s\\S]*?(?:</\\1\\s*>|$)",
            RegexOption.IGNORE_CASE,
        )
    }
}
