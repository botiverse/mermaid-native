import java.security.MessageDigest

plugins {
    kotlin("multiplatform") version "2.0.21-KBA-010" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.cyclonedx.bom") version "3.4.1"
    `maven-publish`
}

group = providers.gradleProperty("group").orElse("build.raft.mermaid").get()
// Local dev fallback only. Production release MUST be driven by -Pversion
// (release.yml sets it from the git tag); never publish with this SNAPSHOT default.
version = providers.gradleProperty("version").orElse("0.1.0-SNAPSHOT").get()

// OHOS-plane Kuikly version. The normal plane uses 2.24.0-raft.1-2.1.21; the
// OHOS leg consumes the matching 2.24.0-raft.1 KBA plane (2.0.21-ohos) so the
// mermaid-kuikly KLIB links against the same release set the Mobile OHOS leg does.
val kuiklyOhosVersion = providers.gradleProperty("mermaidKuiklyOhosVersion")
    .orElse("2.24.0-raft.1-2.0.21-ohos").get()

subprojects {
    group = rootProject.group
    version = rootProject.version

    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
            // OHOS leg: only the signed OHOS toolchain target. Android/iOS targets
            // stay in the ordinary build.gradle.kts (normal plane); they are never
            // compiled here, which keeps the OHOS KLIB closure free of non-OHOS ABI.
            ohosArm64()
        }
    }

    pluginManager.withPlugin("com.android.library") {
        extensions.configure<com.android.build.api.dsl.LibraryExtension> {
            compileSdk = 35
            namespace = "build.raft.mermaid.${project.name.replace('-', '.')}"
            defaultConfig { minSdk = 21 }
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        apply(plugin = "maven-publish")
        apply(plugin = "signing")
        extensions.configure<org.gradle.api.publish.PublishingExtension> {
            repositories {
                maven {
                    name = "mermaidRegistry"
                    url = uri("https://maven.artifacts.botiverse.dev")
                    credentials {
                        username = providers.gradleProperty("mermaidPublishUser").orNull
                            ?: System.getenv("MERMAID_MAVEN_USER")
                        password = providers.gradleProperty("mermaidPublishToken").orNull
                            ?: System.getenv("MERMAID_MAVEN_TOKEN")
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
