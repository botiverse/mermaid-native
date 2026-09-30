package build.raft.mermaid.testkit

// TEMPORARY (PR #201): renders note previews for review; removed before merge.
import build.raft.mermaid.core.MermaidParseResult
import build.raft.mermaid.core.MermaidParser
import build.raft.mermaid.layout.LayoutConfig
import build.raft.mermaid.layout.simple.FixedWidthTextMeasurer
import build.raft.mermaid.layout.simple.SimpleMermaidLayout
import build.raft.mermaid.render.svg.SvgRenderer
import java.io.File
import kotlin.test.Test

class TmpNotePreviewTest {
    @Test
    fun renderNotePreviews() {
        val out = System.getenv("NOTE_PREVIEW_DIR") ?: return
        File(out).mkdirs()
        TmpNotePreviewSources.all.forEach { (name, source) ->
            val parsed = MermaidParser.parse(source)
            if (parsed is MermaidParseResult.Success) {
                File(out, "$name.svg").writeText(
                    SvgRenderer.render(SimpleMermaidLayout.layout(parsed.diagram, FixedWidthTextMeasurer, LayoutConfig()))
                )
            } else {
                File(out, "$name.error.txt").writeText(parsed.toString())
            }
        }
    }
}

object TmpNotePreviewSources {
    val all = listOf(
        "flowchart" to "flowchart TD\n    A[Start] --> B{Ready?}\n    B -->|yes| C[Ship]\n    B -->|no| D[Fix]\n",
        "sequence" to "sequenceDiagram\n    Alice->>Bob: Hello Bob\n    Note right of Bob: Bob thinks\n    Bob-->>Alice: Hi Alice\n",
        "state" to "stateDiagram-v2\n    [*] --> Idle\n    Idle --> Running\n    note right of Running : doing work\n    Running --> [*]\n",
        "class" to "classDiagram\n    class Animal {\n      +String name\n      +eat()\n    }\n    note for Animal \"base type\"\n",
    )
}
