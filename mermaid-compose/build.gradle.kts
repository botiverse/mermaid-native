plugins {
    id("com.android.library")
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    jvm()
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            api(project(":mermaid-layout-api"))
            api(compose.runtime)
            api(compose.foundation)
            api(compose.ui)
        }
        jvmTest.dependencies {
            implementation(project(":mermaid-core"))
            implementation(kotlin("test"))
            implementation(project(":mermaid-layout-simple"))
            implementation(compose.desktop.currentOs)
        }
    }
}

tasks.named<Test>("jvmTest") {
    providers.gradleProperty("mermaidComposeSnapshotDir").orNull?.let { systemProperty("mermaid.compose.snapshotDir", it) }
}
