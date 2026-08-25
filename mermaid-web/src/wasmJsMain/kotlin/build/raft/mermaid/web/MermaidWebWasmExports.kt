package build.raft.mermaid.web

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/** Minimal browser-facing entry points; DOM ownership stays with the host app. */
@OptIn(ExperimentalJsExport::class)
@JsExport
public fun renderMermaidSvg(source: String): String = when (val result = MermaidWebAdapter.render(MermaidWebRequest(source))) {
    is MermaidWebResult.Success -> result.svg
    is MermaidWebResult.Failure -> error(result.diagnostics.joinToString("; ") { it.message })
}

/** JSON-safe typed result for browser consumers that need structured failures. */
@OptIn(ExperimentalJsExport::class)
@JsExport
public fun renderMermaidResultJson(source: String): String = when (val result = MermaidWebAdapter.render(MermaidWebRequest(source))) {
    is MermaidWebResult.Success -> "{\"ok\":true,\"svg\":${jsonString(result.svg)},\"diagnostics\":[]}"
    is MermaidWebResult.Failure -> "{\"ok\":false,\"svg\":null,\"diagnostics\":[${result.diagnostics.joinToString(",") { diagnostic ->
        "{\"code\":${jsonString(diagnostic.code.name)},\"message\":${jsonString(diagnostic.message)},\"line\":${diagnostic.location.line},\"column\":${diagnostic.location.column}}"
    }}]}"
}

private fun jsonString(value: String): String = buildString {
    append('"')
    value.forEach { c ->
        when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }
    append('"')
}
