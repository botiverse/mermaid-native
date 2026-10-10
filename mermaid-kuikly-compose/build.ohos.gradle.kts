plugins {
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":mermaid-layout-api"))
            api("com.tencent.kuikly-open:compose:2.28.0-raft.11-2.0.21-ohos")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
