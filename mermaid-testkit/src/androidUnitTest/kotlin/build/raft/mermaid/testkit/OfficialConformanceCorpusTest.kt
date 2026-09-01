package build.raft.mermaid.testkit

import build.raft.mermaid.core.ClassDiagram
import build.raft.mermaid.core.EventModelingDiagram
import build.raft.mermaid.core.GanttDiagram
import build.raft.mermaid.core.FlowchartDiagram
import build.raft.mermaid.core.MermaidDiagnosticCode
import build.raft.mermaid.core.MermaidDiagram
import build.raft.mermaid.core.PieDiagram
import build.raft.mermaid.core.RequirementDiagram
import build.raft.mermaid.core.TimelineDiagram
import build.raft.mermaid.core.MermaidParseResult
import build.raft.mermaid.core.MermaidParser
import build.raft.mermaid.core.SequenceDiagram
import build.raft.mermaid.layout.LayoutConfig
import build.raft.mermaid.layout.simple.FixedWidthTextMeasurer
import build.raft.mermaid.layout.simple.SimpleMermaidLayout
import build.raft.mermaid.render.svg.SvgRenderer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OfficialConformanceCorpusTest {
    @Test
    fun pinnedOfficialFixturesMatchNativeSemanticContract() {
        val root = repositoryRoot()
        val rows = File(root, "compatibility/conformance/manifest.tsv")
            .readLines().drop(1).filter(String::isNotBlank).map(::parseRow)

        rows.forEach { row ->
            val source = File(root, row.fixture).readText()
            when (row.classification) {
                "supported" -> {
                    val parsed = MermaidParser.parse(source)
                    if (parsed is MermaidParseResult.Failure) error("${row.key}: ${parsed.diagnostics}")
                    val success = assertIs<MermaidParseResult.Success>(parsed, row.key)
                    assertEquals(row.expectedSemantics, success.diagram.semanticProjection(), row.key)
                    val first = render(success.diagram)
                    assertEquals(first, render(success.diagram), "Non-deterministic SVG: ${row.key}")
                    assertTrue(first.startsWith("<svg ") && first.endsWith("</svg>\n"), row.key)
                    success.diagram.expectedVisibleLabels().forEach { label ->
                        assertTrue(first.contains(label), "Missing SVG label '$label': ${row.key}")
                    }
                    assertTrue(Regex("<(rect|line|polygon)\\b").containsMatchIn(first), "Missing SVG geometry: ${row.key}")
                }
                "unsupported", "deferred" -> {
                    val failure = assertIs<MermaidParseResult.Failure>(MermaidParser.parse(source), row.key)
                    val diagnostic = failure.diagnostics.first()
                    assertTrue(
                        diagnostic.code in setOf(
                            MermaidDiagnosticCode.UNSUPPORTED_SYNTAX,
                            MermaidDiagnosticCode.INVALID_HEADER,
                            MermaidDiagnosticCode.INVALID_VALUE,
                        ),
                        row.key,
                    )
                    assertEquals(row.diagnosticLine, diagnostic.location.line, row.key)
                    assertEquals(row.diagnosticColumn, diagnostic.location.column, row.key)
                }
                else -> error("Unexpected classification: ${row.classification}")
            }
        }
    }
}

private data class CorpusRow(
    val family: String,
    val case: String,
    val classification: String,
    val fixture: String,
    val expectedSemantics: String,
    val diagnosticLine: Int?,
    val diagnosticColumn: Int?,
) {
    val key: String get() = "$family/$case"
}

private fun parseRow(line: String): CorpusRow {
    val columns = line.split('\t')
    return CorpusRow(
        family = columns[0],
        case = columns[1],
        classification = columns[2],
        fixture = columns[3],
        expectedSemantics = columns[8],
        diagnosticLine = columns[9].toIntOrNull(),
        diagnosticColumn = columns[10].toIntOrNull(),
    )
}

private fun MermaidDiagram.semanticProjection(): String = when (this) {
    is FlowchartDiagram -> "flowchart|${direction.name}|" +
        nodes.joinToString(",") { "${it.id}:${it.label}" } + "|" +
        edges.joinToString(",") { "${it.sourceId}>${it.targetId}" }
    is SequenceDiagram -> "sequence|" +
        actors.joinToString(",") { "${it.id}:${it.label}" } + "|" +
        messages.joinToString(",") { "${it.from}>${it.to}:${it.lineStyle.name}:${it.arrowHead.name}:${it.label}" }
    is EventModelingDiagram -> "eventmodeling|" +
        frames.joinToString(",") { "${it.id}:${it.kind.name}:${it.entityId}" } + "|" +
        relations.joinToString(",") { "${it.sourceFrameId}>${it.targetFrameId}" }
    is PieDiagram -> "pie|${showData}|${title.orEmpty()}|" +
        sections.joinToString(",") { "${it.label}:${it.value}" }
    is ClassDiagram -> "class|" +
        classes.joinToString(",") { "${it.id}:${it.label}:${it.members.joinToString(";") { member -> member.signature }}:${it.namespaceName.orEmpty()}" } + "|" +
        relationships.joinToString(",") { "${it.from}>${it.to}:${it.kind.name}" }
    is GanttDiagram -> "gantt|${title.orEmpty()}|$dateFormat|" +
        sections.joinToString(",") { section ->
            "${section.name}:" + section.tasks.joinToString(";") { task ->
                "${task.name}:${task.id}:${task.startDay}:${task.durationDays}:${task.status.name}"
            }
        }
    is TimelineDiagram -> "timeline|${title.orEmpty()}|" +
        events.joinToString(",") { event -> "${event.section.orEmpty()}:${event.period}:${event.labels.joinToString(";")}" }
    is RequirementDiagram -> "requirement|${accessibilityTitle.orEmpty()}:${accessibilityDescription.orEmpty()}|" +
        requirements.joinToString(",") { "${it.name}:${it.id}:${it.text}:${it.risk.name}:${it.verifyMethod.name}:${it.type.name}" } + "|" +
        elements.joinToString(",") { "${it.name}:${it.type}:${it.docRef}" } + "|" +
        relationships.joinToString(",") { "${it.from}>${it.to}:${it.kind.name}" }
    else -> error("Corpus pilot has no semantic adapter for ${this::class.simpleName}")
}

private fun MermaidDiagram.expectedVisibleLabels(): List<String> = when (this) {
    is FlowchartDiagram -> nodes.map { it.label }
    is SequenceDiagram -> actors.map { it.label } + messages.map { it.label }
    is EventModelingDiagram -> frames.map { it.entityId }
    is TimelineDiagram -> events.flatMap { event ->
        listOfNotNull(event.section, event.period) + event.labels.flatMap { label ->
            listOf(label.substringBefore('<').trim().takeIf { it.isNotEmpty() } ?: label)
        }
    }
    else -> emptyList()
}

private fun render(diagram: MermaidDiagram): String = SvgRenderer.render(
    SimpleMermaidLayout.layout(diagram, FixedWidthTextMeasurer, LayoutConfig()),
)

private fun repositoryRoot(): File {
    val workingDirectory = System.getProperty("user.dir") ?: error("Missing user.dir")
    return generateSequence(File(workingDirectory)) { it.parentFile }
        .firstOrNull { File(it, "compatibility/conformance/manifest.tsv").isFile }
        ?: error("Could not locate conformance manifest")
}
