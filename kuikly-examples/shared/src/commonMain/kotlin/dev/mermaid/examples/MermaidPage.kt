package dev.mermaid.examples

import build.raft.mermaid.core.MermaidParser
import build.raft.mermaid.core.MermaidParseResult
import build.raft.mermaid.layout.LayoutConfig
import build.raft.mermaid.layout.simple.FixedWidthTextMeasurer
import build.raft.mermaid.layout.simple.SimpleMermaidLayout
import build.raft.mermaid.layout.simple.layout
import build.raft.mermaid.kuikly.MermaidView
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.views.Text

@Page("mermaid")
class MermaidPage : Pager() {
    override fun body(): ViewBuilder = {
        attr { backgroundColor(Color.WHITE); padding(24f) }
        Text { attr { text("Mermaid Native · " + this@MermaidPage.pageData.params.optString("sample", "flow")); fontSize(18f); color(Color.BLACK); marginBottom(16f) } }
        val source = when (this@MermaidPage.pageData.params.optString("sample")) {
            "treemap" -> """---
                |title: Official Kuikly Treemap
                |---
                |treemap-beta
                |"Team"
                |  "Core": 40
                |  "Layout": 35
                |  "Render": 25
                |""".trimMargin()
            "shapes" -> """flowchart TD
                |A@{ shape: manual-input, label: "Input" } --> B@{ shape: cyl, label: "Store" }
                |B --> C@{ shape: hex, label: "Transform" }
                |C --> D@{ shape: stadium, label: "Done" }
                |""".trimMargin()
            "sequence" -> """sequenceDiagram
                |Alice->>Bob: Native Canvas
                |Bob-->>Alice: Official Kuikly
                |""".trimMargin()
            else -> "flowchart TD\nA[Official Kuikly] --> B{Native scene}\nB --> C[Mermaid 0.1.11]\nB --> D[No WebView]"
        }
        val parsed = MermaidParser.parse(source)
        when (parsed) {
            is MermaidParseResult.Success -> {
                val diagram = SimpleMermaidLayout.layout(parsed, FixedWidthTextMeasurer, LayoutConfig())
                MermaidView { attr { scene = diagram; scale = minOf(1f, 320f / diagram.width.toFloat()); width(diagram.width.toFloat() * scale); height(diagram.height.toFloat() * scale) } }
            }
            is MermaidParseResult.Failure -> Text { attr { text(parsed.diagnostics.joinToString()); color(Color.RED) } }
        }
    }
}
