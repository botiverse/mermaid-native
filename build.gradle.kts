import java.security.MessageDigest

plugins {
    kotlin("multiplatform") version "2.1.21" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.cyclonedx.bom") version "3.4.1"
    `maven-publish`
}

group = providers.gradleProperty("group").orElse("build.raft.mermaid").get()
version = providers.gradleProperty("version").orElse("0.1.0-SNAPSHOT").get()

subprojects {
    group = rootProject.group
    version = rootProject.version

    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
            androidTarget { publishLibraryVariants("release") }
            iosArm64()
            iosSimulatorArm64()
            // OHOS is enabled by the Kuikly host build once its signed toolchain
            // is available; keeping the core graph free of that plugin is
            // intentional and preserves ordinary KMP consumers.
        }
    }

    pluginManager.withPlugin("com.android.library") {
        extensions.configure<com.android.build.api.dsl.LibraryExtension> {
            compileSdk = 35
            namespace = "build.raft.mermaid.${project.name.replace('-', '.') }"
            defaultConfig { minSdk = 21 }
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        apply(plugin = "maven-publish")
        apply(plugin = "signing")
        extensions.configure<org.gradle.api.publish.PublishingExtension> {
            repositories {
                mavenLocal()
                val publishUrl = providers.gradleProperty("mermaidPublishUrl").orNull
                    ?: System.getenv("MERMAID_MAVEN_URL")
                    ?: "https://maven.pkg.github.com/botiverse/mermaid-native"
                maven {
                    name = "mermaidRegistry"
                    url = uri(publishUrl)
                    credentials {
                        username = providers.gradleProperty("mermaidPublishUser").orNull
                            ?: System.getenv("MERMAID_MAVEN_USER")
                            ?: System.getenv("GITHUB_ACTOR")
                        password = providers.gradleProperty("mermaidPublishToken").orNull
                            ?: System.getenv("MERMAID_MAVEN_TOKEN")
                            ?: System.getenv("GITHUB_TOKEN")
                    }
                }
            }

            publications.withType<org.gradle.api.publish.maven.MavenPublication>().configureEach {
                pom {
                    name.set("Mermaid Native ${project.name}")
                    description.set("Mermaid-compatible native diagram components for Kotlin Multiplatform")
                    url.set("https://github.com/botiverse/mermaid-native")
                    licenses {
                        license {
                            name.set("Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }
                    scm {
                        connection.set("scm:git:https://github.com/botiverse/mermaid-native.git")
                        developerConnection.set("scm:git:ssh://git@github.com/botiverse/mermaid-native.git")
                        url.set("https://github.com/botiverse/mermaid-native")
                    }
                }
            }
        }

        extensions.configure<org.gradle.plugins.signing.SigningExtension> {
            isRequired = false
            val key = providers.gradleProperty("signingKey").orNull ?: System.getenv("MERMAID_SIGNING_KEY")
            val password = providers.gradleProperty("signingPassword").orNull ?: System.getenv("MERMAID_SIGNING_PASSWORD")
            if (!key.isNullOrBlank()) {
                useInMemoryPgpKeys(key, password)
                sign(extensions.getByType<org.gradle.api.publish.PublishingExtension>().publications)
            }
        }
    }
}

tasks.register("verifyThirdPartyNotices") {
    val notices = layout.projectDirectory.file("NOTICE")
    val upstreams = layout.projectDirectory.file("compatibility/upstreams.lock")
    inputs.files(notices, upstreams)
    doLast {
        check(notices.asFile.readText().contains("EPL-2.0")) {
            "Third-party notice must record the EPL-2.0 boundary"
        }
        check(upstreams.asFile.readLines().none { it.substringAfter('=').contains("TODO") }) {
            "Compatibility upstream revisions must be pinned"
        }
    }
}

tasks.register("verifyWebAcceptance") {
    val shell = layout.projectDirectory.dir("acceptance")
    val matrix = layout.projectDirectory.file("compatibility/diagram-families.csv")
    inputs.dir(shell)
    inputs.file(matrix)
    doLast {
        val html = shell.file("index.html").asFile.readText()
        val js = shell.file("consumer.js").asFile.readText()
        val css = shell.file("consumer.css").asFile.readText()
        check(html.contains("aria-live=\"polite\"")) { "Web acceptance requires a live status region" }
        check(html.contains("type=\"module\"")) { "Web acceptance must use a module script" }
        check(js.contains("renderMermaidResultJson")) { "Consumer must use the typed Wasm result API" }
        check(!js.contains("eval(") && !js.contains("fetch(") && !js.contains("innerHTML = source")) {
            "Consumer must not evaluate or transmit Mermaid source"
        }
        check(css.contains("min-height: 2.75rem")) { "Keyboard target sizing regressed" }
        val rows = matrix.asFile.readLines().drop(1).filter { it.isNotBlank() }
        check(rows.size == 32) { "Expected the full 32-family acceptance matrix" }
        check(rows.map { it.substringBefore(',') }.toSet().size == 32) { "Family matrix IDs must be unique" }
    }
}

tasks.register("verifyDiagramFamilyRegistry") {
    val registry = layout.projectDirectory.file("compatibility/diagram-families.csv")
    inputs.file(registry)
    doLast {
        val rows = registry.asFile.readLines().filter { it.isNotBlank() }
        check(rows.firstOrNull() == "family,syntax_doc,status,parser,typed_ast,layout,svg_render,notes") {
            "Diagram-family registry header changed unexpectedly"
        }
        val entries = rows.drop(1).map { it.split(',', limit = 8) }
        check(entries.size == 32) { "Expected 32 official Mermaid family entries, found ${entries.size}" }
        check(entries.map { it[0] }.toSet().size == entries.size) { "Family IDs must be unique" }
        val allowedStatus = setOf("implemented", "in_progress", "not_started", "blocked")
        check(entries.all { it.size == 8 && it[2] in allowedStatus }) {
            "Every registry row must have eight columns and an allowed status"
        }
        check(entries.filter { it[2] == "implemented" }.all {
            it[3] == "yes" && it[4] == "yes" && it[5] == "yes" && it[6] == "yes"
        }) { "A family may be implemented only when parser, AST, layout, and SVG are complete" }
    }
}

val conformanceManifest = layout.projectDirectory.file("compatibility/conformance/manifest.tsv")

tasks.register("verifyConformanceCorpus") {
    val upstreams = layout.projectDirectory.file("compatibility/upstreams.lock")
    val notices = layout.projectDirectory.file("NOTICE")
    inputs.files(conformanceManifest, upstreams, notices)
    inputs.dir(layout.projectDirectory.dir("compatibility/conformance"))
    doLast {
        val pinnedMermaid = upstreams.asFile.readLines()
            .single { it.startsWith("mermaid=") }
            .substringAfter('=')
        val lines = conformanceManifest.asFile.readLines().filter { it.isNotBlank() }
        val header = "family\tcase\tclassification\tfixture\tupstream_commit\tupstream_path\tupstream_case\tsha256\texpected_semantics\tdiagnostic_line\tdiagnostic_column"
        check(lines.firstOrNull() == header) { "Conformance manifest header changed unexpectedly" }
        val rows = lines.drop(1).map { it.split('\t') }
        check(rows.isNotEmpty()) { "Conformance manifest must not be empty" }
        check(rows.all { it.size == 11 }) { "Every conformance row must have 11 tab-separated columns" }
        check(rows.map { it[0] to it[1] } == rows.map { it[0] to it[1] }.sortedWith(compareBy({ it.first }, { it.second }))) {
            "Conformance manifest must be sorted by family and case"
        }
        check(rows.map { it[0] to it[1] }.distinct().size == rows.size) { "Conformance family/case keys must be unique" }
        check(rows.map { it[0] }.toSet() == setOf("flowchart", "sequence", "eventmodeling")) {
            "Pilot must cover flowchart, sequence, and eventmodeling"
        }
        check(rows.all { it[2] in setOf("supported", "unsupported", "deferred") }) {
            "Unknown conformance classification"
        }
        check(rows.all { it[4] == pinnedMermaid }) { "Every fixture must bind the pinned Mermaid revision" }
        check(rows.all { it[5].isNotBlank() && it[6].isNotBlank() }) { "Every fixture needs upstream path and case attribution" }
        check(notices.asFile.readText().contains("Mermaid conformance fixtures")) {
            "Third-party notices must describe copied Mermaid conformance fixtures"
        }
        rows.forEach { row ->
            val fixture = layout.projectDirectory.file(row[3]).asFile
            check(fixture.isFile) { "Missing conformance fixture: ${row[3]}" }
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(fixture.readBytes())
                .joinToString("") { "%02x".format(it) }
            check(digest == row[7]) { "Fixture hash drift for ${row[0]}/${row[1]}" }
            if (row[2] == "supported") {
                check(row[8] != "-" && row[9] == "-" && row[10] == "-") { "Supported cases need semantics only" }
            } else {
                check(row[8] == "-" && row[9].toIntOrNull() != null && row[10].toIntOrNull() != null) {
                    "Fail-closed cases need typed diagnostic line and column"
                }
            }
        }
    }
}

tasks.register("reportConformanceCorpusDrift") {
    inputs.file(conformanceManifest)
    val previousPath = providers.gradleProperty("previousConformanceManifest")
    val report = layout.buildDirectory.file("reports/conformance-corpus-drift.txt")
    outputs.file(report)
    doLast {
        fun rows(file: File): Map<String, String> = file.readLines().drop(1).filter { it.isNotBlank() }
            .associateBy { line -> line.split('\t', limit = 3).take(2).joinToString("/") }
        val current = rows(conformanceManifest.asFile)
        val previousFile = previousPath.orNull?.let(::file)?.takeIf(File::isFile)
        val previous = previousFile?.let(::rows).orEmpty()
        val added = current.keys - previous.keys
        val removed = previous.keys - current.keys
        val changed = current.keys.intersect(previous.keys).filter { current[it] != previous[it] }.toSet()
        val unchanged = current.keys.intersect(previous.keys) - changed
        report.get().asFile.apply {
            parentFile.mkdirs()
            writeText(buildString {
                appendLine("baseline=${previousFile?.path ?: "none"}")
                appendLine("added=${added.sorted().joinToString(",")}")
                appendLine("removed=${removed.sorted().joinToString(",")}")
                appendLine("changed=${changed.sorted().joinToString(",")}")
                appendLine("unchanged=${unchanged.sorted().joinToString(",")}")
            })
        }
    }
}

tasks.matching { it.name == "check" }.configureEach {
    dependsOn("verifyWebAcceptance")
    dependsOn("verifyThirdPartyNotices")
    dependsOn("verifyDiagramFamilyRegistry")
    dependsOn("verifyConformanceCorpus")
    dependsOn("reportConformanceCorpusDrift")
}
