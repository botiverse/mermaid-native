plugins {
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":mermaid-layout-api"))
            implementation("com.tencent.kuikly-open:core:2.24.0-raft.1-2.0.21-ohos")
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
