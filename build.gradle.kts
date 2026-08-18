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
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
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
    val notices = layout.projectDirectory.file("THIRD_PARTY_NOTICES.md")
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
