package build.raft.mermaid.layout.simple

/**
 * Whole-input match that behaves the same on every target.
 *
 * On Kotlin/JS, `Regex.matches` does not backtrack into an earlier alternative when a
 * later one would match the whole input, so `Regex("a|ab").fullyMatches("ab")` is false there.
 * `matchEntire` anchors the pattern instead and agrees with the JVM and Native.
 */
internal fun Regex.fullyMatches(input: CharSequence): Boolean = matchEntire(input) != null
