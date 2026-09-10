plugins {
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies { api(project(":mermaid-core")) }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
