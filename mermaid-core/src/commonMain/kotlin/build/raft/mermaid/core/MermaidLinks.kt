package build.raft.mermaid.core

/** No executable schemes, credentials or whitespace; hosts retain navigation ownership. */
public fun isSafeMermaidLink(url: String): Boolean {
    if (url.any { it.isWhitespace() || it.code < 32 || it == '\\' }) return false
    val scheme = url.substringBefore("://").lowercase()
    if (scheme !in listOf("http", "https") || !url.contains("://")) return false
    val authority = url.substringAfter("://").takeWhile { it !in "/?#" }
    return authority.isNotEmpty() && '@' !in authority
}

/** Mermaid replaces the first placeholder and retains the ticket's literal spelling. */
public fun KanbanDiagram.ticketUrl(ticket: String): String? = ticketBaseUrl
    ?.replaceFirst("#TICKET#", ticket)
    ?.takeIf(::isSafeMermaidLink)
