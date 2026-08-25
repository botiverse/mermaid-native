plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }
    sourceSets {
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
