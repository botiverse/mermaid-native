package build.raft.mermaid.testkit

import build.raft.mermaid.core.MermaidParseResult
import build.raft.mermaid.core.MermaidParser
import build.raft.mermaid.layout.LayoutConfig
import build.raft.mermaid.layout.simple.FixedWidthTextMeasurer
import build.raft.mermaid.layout.simple.SimpleMermaidLayout
import build.raft.mermaid.layout.simple.layout
import build.raft.mermaid.render.svg.SvgRenderer
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.test.Test
import kotlin.test.fail

/**
 * Every Mermaid source imported from upstream mermaid-js tests (compatibility/upstream-corpus,
 * see its README) runs through Native parse + layout + SVG render and is compared with the
 * committed mermaid.js outcome (reference.tsv):
 *
 * - pass: both render it, or both reject it (Native with a parse diagnostic).
 * - rejects-valid: mermaid.js renders it, Native reports a parse diagnostic.
 * - accepts-invalid: mermaid.js rejects it, Native renders it.
 * - crash: Native layout or rendering throws, or exceeds the per-case time limit.
 * - unscored (unless Native crashes): ref-render-error (mermaid.js parses it but its renderer
 *   throws), ref-timeout, ref-unsupported (the mermaid.js release has no such diagram registered).
 *
 * Known failures are listed in expected-failures.tsv. A case failing that is not listed
 * (regression) or a listed case that now passes (stale entry) fails the test. The report and a
 * regenerated list are written to build/upstream-corpus/ in the module directory.
 */
class UpstreamCorpusTest {
    @Test
    fun upstreamCorpusMatchesExpectedFailures() {
        val root = repositoryRoot()
        val corpus = File(root, "compatibility/upstream-corpus")
        val manifest = readTsv(File(corpus, "manifest.tsv"))
        val reference = readTsv(File(corpus, "reference.tsv")).associateBy { it.getValue("case") }
        val expected = readTsv(File(corpus, "expected-failures.tsv")).associate { it.getValue("case") to it.getValue("outcome") }

        val problems = mutableListOf<String>()
        val results = manifest.map { row ->
            val case = row.getValue("case")
            val source = File(root, row.getValue("fixture")).readText()
            val sha = sha256(source)
            if (sha != row.getValue("sha256")) problems += "$case: fixture hash differs from manifest"
            val ref = reference[case]
            if (ref == null || ref.getValue("sha256") != sha) {
                problems += "$case: reference.tsv has no outcome for this fixture; rerun scripts/upstream-corpus/reference.mjs"
            }
            val native = runNative(source)
            CaseResult(
                case = case,
                diagramType = row.getValue("diagram_type"),
                kind = row.getValue("kind"),
                reference = ref?.getValue("ref_status") ?: "missing",
                native = native,
            )
        }
        worker.shutdownNow()

        val outDir = File(System.getProperty("user.dir"), "build/upstream-corpus").apply { mkdirs() }
        File(outDir, "results.tsv").writeText(
            "case\tdiagram_type\tref_status\tnative_status\toutcome\tdetail\n" + results.joinToString("") {
                "${it.case}\t${it.diagramType}\t${it.reference}\t${it.native.status}\t${it.outcome}\t${it.native.detail.oneLine()}\n"
            },
        )
        val failing = results.filter { it.outcome != PASS && !it.unscored }
        File(outDir, "expected-failures.candidate.tsv").writeText(expectedFailuresTsv(failing))
        val summary = summaryMarkdown(results)
        File(outDir, "summary.md").writeText(summary)
        println(summary)

        failing.filter { it.case !in expected }.forEach {
            problems += "regression ${it.case}: ${it.outcome} (mermaid.js ${it.reference}, native ${it.native.status}: ${it.native.detail.oneLine().take(160)})"
        }
        results.filter { (it.outcome == PASS || it.unscored) && it.case in expected }.forEach {
            problems += "fixed ${it.case}: now passes; remove it from expected-failures.tsv"
        }
        failing.filter { expected[it.case] != null && expected[it.case] != it.outcome }.forEach {
            problems += "changed ${it.case}: expected ${expected[it.case]}, now ${it.outcome}"
        }
        val known = manifest.map { it.getValue("case") }.toSet()
        expected.keys.filter { it !in known }.forEach { problems += "unknown case in expected-failures.tsv: $it" }
        if (problems.isNotEmpty()) {
            fail(
                "${problems.size} upstream-corpus problem(s). If intended, copy " +
                    "mermaid-testkit/build/upstream-corpus/expected-failures.candidate.tsv over " +
                    "compatibility/upstream-corpus/expected-failures.tsv.\n" + problems.take(200).joinToString("\n"),
            )
        }
    }

    private var worker = Executors.newSingleThreadExecutor(::daemon)

    private fun daemon(r: Runnable) = Thread(r, "upstream-corpus").apply { isDaemon = true }

    private fun runNative(source: String): NativeResult {
        val future = worker.submit<NativeResult> {
            when (val parsed = MermaidParser.parse(source)) {
                is MermaidParseResult.Failure -> NativeResult(
                    "parse-error",
                    parsed.diagnostics.first().let { "${it.code} ${it.location.line}:${it.location.column} ${it.message}" },
                )
                is MermaidParseResult.Success -> {
                    val scene = try {
                        SimpleMermaidLayout.layout(parsed, FixedWidthTextMeasurer, LayoutConfig())
                    } catch (e: Throwable) {
                        return@submit NativeResult("layout-error", e.describe())
                    }
                    val svg = try {
                        SvgRenderer.render(scene)
                    } catch (e: Throwable) {
                        return@submit NativeResult("render-error", e.describe())
                    }
                    if (svg.startsWith("<svg")) NativeResult("ok", "") else NativeResult("render-error", "output is not an <svg>")
                }
            }
        }
        return try {
            future.get(CASE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            // The stuck thread cannot be stopped safely; abandon it (daemon) and continue on a new one.
            worker.shutdownNow()
            worker = Executors.newSingleThreadExecutor(::daemon)
            NativeResult("timeout", "no result within $CASE_TIMEOUT_SECONDS s")
        } catch (e: ExecutionException) {
            NativeResult("parse-crash", (e.cause ?: e).describe())
        }
    }

    private companion object {
        const val CASE_TIMEOUT_SECONDS = 20L
        const val PASS = "pass"
    }
}

private data class NativeResult(val status: String, val detail: String)

private data class CaseResult(
    val case: String,
    val diagramType: String,
    val kind: String,
    val reference: String,
    val native: NativeResult,
) {
    val unscored: Boolean get() = outcome.startsWith("ref-")

    val outcome: String
        get() = when {
            native.status != "ok" && native.status != "parse-error" -> "crash"
            reference != "ok" && reference != "parse-error" -> "ref-$reference"
            reference == "ok" && native.status == "ok" -> "pass"
            reference == "ok" -> "rejects-valid"
            native.status == "parse-error" -> "pass"
            else -> "accepts-invalid"
        }
}

private fun expectedFailuresTsv(failing: List<CaseResult>): String =
    "case\toutcome\n" + failing.sortedBy { it.case }.joinToString("") { "${it.case}\t${it.outcome}\n" }

private fun summaryMarkdown(results: List<CaseResult>): String = buildString {
    fun pct(n: Int, d: Int) = if (d == 0) "–" else "%.1f%%".format(100.0 * n / d)
    fun row(name: String, all: List<CaseResult>) {
        val rs = all.filter { !it.unscored }
        val valid = rs.filter { it.reference == "ok" }
        val pass = rs.count { it.outcome == "pass" }
        val validPass = valid.count { it.outcome == "pass" }
        append("| $name | ${rs.size} | $pass | ${pct(pass, rs.size)} | ${valid.size} | $validPass | ${pct(validPass, valid.size)} | ")
        append("${rs.count { it.outcome == "rejects-valid" }} | ${rs.count { it.outcome == "accepts-invalid" }} | ${rs.count { it.outcome == "crash" }} | ${all.size - rs.size} |\n")
    }
    append("# Upstream corpus: Native vs mermaid.js\n\n")
    append("Unscored: mermaid.js parses the source but its renderer throws or times out, or the mermaid.js release has no such diagram.\n\n")
    append("| diagram | scored | pass | pass % | mermaid.js renders | Native renders those | % | rejects valid | accepts invalid | crash | unscored |\n")
    append("| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |\n")
    results.groupBy { it.diagramType }.toSortedMap().forEach { (type, rs) -> row(type, rs) }
    row("**all**", results)
    append("\n")
    results.groupBy { it.kind }.toSortedMap().forEach { (kind, rs) -> row("**$kind**", rs) }
}

private fun readTsv(file: File): List<Map<String, String>> {
    val lines = file.readLines().filter { it.isNotBlank() && !it.startsWith("#") }
    val header = lines.first().split('\t')
    return lines.drop(1).map { line ->
        val cells = line.split('\t')
        header.indices.associate { header[it] to cells.getOrElse(it) { "" } }
    }
}

private fun sha256(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

private fun Throwable.describe(): String =
    "${this::class.simpleName}: ${message.orEmpty()} @ ${stackTrace.firstOrNull { it.className.startsWith("build.raft") } ?: stackTrace.firstOrNull()}"

private fun String.oneLine(): String = replace(Regex("\\s+"), " ").trim()

private fun repositoryRoot(): File {
    val workingDirectory = System.getProperty("user.dir") ?: error("Missing user.dir")
    return generateSequence(File(workingDirectory)) { it.parentFile }
        .firstOrNull { File(it, "compatibility/upstream-corpus/manifest.tsv").isFile }
        ?: error("Could not locate compatibility/upstream-corpus")
}
