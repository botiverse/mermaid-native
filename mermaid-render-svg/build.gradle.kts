plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":mermaid-layout-api"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(project(":mermaid-layout-simple"))
        }
    }
}
