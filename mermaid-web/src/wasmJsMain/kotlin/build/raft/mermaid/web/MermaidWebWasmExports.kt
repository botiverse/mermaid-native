package build.raft.mermaid.web

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/** Minimal browser-facing entry point; DOM ownership stays with the host app. */
@OptIn(ExperimentalJsExport::class)
@JsExport
public fun renderMermaidSvg(source: String): String = when (val result = MermaidWebAdapter.render(MermaidWebRequest(source))) {
    is MermaidWebResult.Success -> result.svg
    is MermaidWebResult.Failure -> error(result.diagnostics.joinToString("; ") { it.message })
}
