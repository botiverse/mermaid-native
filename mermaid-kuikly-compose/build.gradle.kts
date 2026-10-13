plugins {
    id("com.android.library")
    kotlin("multiplatform")
    kotlin("plugin.compose")
}

kotlin {
    androidTarget { publishLibraryVariants("release") }
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            api(project(":mermaid-layout-api"))
            api("com.tencent.kuikly-open:compose:2.28.0-raft.11-2.1.21")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "build.raft.mermaid.kuikly.compose"
    compileSdk = 35
    defaultConfig { minSdk = 21 }
}
