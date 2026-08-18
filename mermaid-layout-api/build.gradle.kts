plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies { api(project(":mermaid-core")) }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
