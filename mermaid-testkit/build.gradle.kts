plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":mermaid-core"))
            api(project(":mermaid-layout-api"))
            api(project(":mermaid-layout-simple"))
            api(project(":mermaid-render-svg"))
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
