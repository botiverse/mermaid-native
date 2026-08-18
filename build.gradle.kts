plugins {
    kotlin("multiplatform") version "2.1.21" apply false
    id("com.android.library") version "8.13.2" apply false
    `maven-publish`
}

group = providers.gradleProperty("group").orElse("build.raft.mermaid").get()
version = providers.gradleProperty("version").orElse("0.1.0-SNAPSHOT").get()

subprojects {
    group = rootProject.group
    version = rootProject.version

    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
            androidTarget()
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
        extensions.configure<org.gradle.api.publish.PublishingExtension> {
            repositories {
                mavenLocal()
                val publishUrl = providers.gradleProperty("mermaidPublishUrl").orNull
                    ?: System.getenv("MERMAID_MAVEN_URL")
                if (!publishUrl.isNullOrBlank()) {
                    maven {
                        name = "mermaidRegistry"
                        url = uri(publishUrl)
                        credentials {
                            username = providers.gradleProperty("mermaidPublishUser").orNull
                                ?: System.getenv("MERMAID_MAVEN_USER")
                            password = providers.gradleProperty("mermaidPublishToken").orNull
                                ?: System.getenv("MERMAID_MAVEN_TOKEN")
                        }
                    }
                }
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
