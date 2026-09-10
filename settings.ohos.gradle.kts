pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // OHOS Kotlin/Native distribution (kotlin-native-prebuilt) and the KBA
        // compiler live on the Tencent mirror. Gradle resolves the plugin marker
        // (org.jetbrains.kotlin.multiplatform:2.0.21-KBA-010) from here too.
        maven {
            name = "TencentOHOS"
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
    }
}

dependencyResolutionManagement {
    // Kuikly OHOS and the KBA-patched stdlib/coroutines/atomicfu are immutable
    // in Raft Artifacts; keep the normal KMP target repos out of the OHOS leg's
    // dependency graph so the OHOS plane never falls through to a non-OHOS artifact.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        maven {
            name = "RaftArtifactsKuiklyRelease"
            url = uri("https://maven.artifacts.botiverse.dev")
            content {
                includeGroup("com.tencent.kuikly-open")
                includeGroupByRegex("com\\.tencent\\.kuikly-open\\.compose\\..+")
                includeVersion("org.jetbrains.kotlin", "kotlin-stdlib", "2.0.21-KBA-003")
                includeVersion("org.jetbrains.kotlin", "kotlin-stdlib-common", "2.0.21-KBA-003")
                includeVersion("org.jetbrains.kotlin", "kotlin-stdlib", "2.0.21-KBA-010")
                includeVersion("org.jetbrains.kotlin", "kotlin-stdlib-common", "2.0.21-KBA-010")
                includeVersion("org.jetbrains.kotlinx", "atomicfu", "0.23.2-KBA-001")
                includeVersion("org.jetbrains.kotlinx", "atomicfu-ohosarm64", "0.23.2-KBA-001")
                includeVersion("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.8.0-KBA-002")
                includeVersion("org.jetbrains.kotlinx", "kotlinx-coroutines-core-ohosarm64", "1.8.0-KBA-002")
            }
        }
        maven {
            name = "TencentOHOS"
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "mermaid-native-ohos"

val buildFileName = "build.ohos.gradle.kts"
rootProject.buildFileName = buildFileName

include(
    ":mermaid-core",
    ":mermaid-layout-api",
    ":mermaid-layout-simple",
    ":mermaid-kuikly",
)
project(":mermaid-core").buildFileName = buildFileName
project(":mermaid-layout-api").buildFileName = buildFileName
project(":mermaid-layout-simple").buildFileName = buildFileName
project(":mermaid-kuikly").buildFileName = buildFileName
