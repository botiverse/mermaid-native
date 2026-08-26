plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }
    sourceSets {
        commonMain.dependencies { api(project(":mermaid-core")) }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
