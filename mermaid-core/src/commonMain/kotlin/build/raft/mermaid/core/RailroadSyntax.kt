package build.raft.mermaid.core

/** Railroad grammar selection, independent of whether its body is valid. */
public enum class RailroadSyntax(public val keyword: String) {
    NATIVE("railroad-beta"),
    ABNF("railroad-abnf-beta"),
    EBNF("railroad-ebnf-beta"),
    PEG("railroad-peg-beta");

    /**
     * Checks the first nonblank line, case-insensitively. The keyword must occupy
     * that whole line. This low-level detector does not strip comments or metadata;
     * [MermaidParser] supplies its already-preprocessed header when dispatching.
     */
    public fun matchesSource(source: String): Boolean =
        source.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.equals(keyword, ignoreCase = true) == true

    public companion object {
        public fun detect(source: String): RailroadSyntax? = entries.firstOrNull { it.matchesSource(source) }
    }
}
