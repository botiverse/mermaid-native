plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

kotlin {
    wasmJs {
        browser()
        binaries.executable()
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":mermaid-core"))
            api(project(":mermaid-layout-api"))
            implementation(project(":mermaid-layout-simple"))
            implementation(project(":mermaid-render-svg"))
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
