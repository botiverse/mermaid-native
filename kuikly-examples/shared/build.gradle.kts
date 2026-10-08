plugins { kotlin("multiplatform"); id("com.android.library"); id("com.google.devtools.ksp") }
val kuiklyVersion: String by project
val mermaidVersion: String by project
kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    sourceSets.commonMain.dependencies {
        api("com.tencent.kuikly-open:core:$kuiklyVersion")
        implementation("com.tencent.kuikly-open:core-annotations:$kuiklyVersion")
        implementation("build.raft.mermaid:mermaid-layout-simple:$mermaidVersion")
        implementation("build.raft.mermaid:mermaid-kuikly:$mermaidVersion") {
            // Replace Mermaid's published Raft dependency with official Kuikly.
            exclude(group = "com.tencent.kuikly-open")
        }
    }
}
android { namespace = "dev.mermaid.examples.shared"; compileSdk = 35; defaultConfig { minSdk = 24 } }
dependencies {
    add("kspAndroid", "com.tencent.kuikly-open:core-ksp:$kuiklyVersion")
    add("kspIosArm64", "com.tencent.kuikly-open:core-ksp:$kuiklyVersion")
    add("kspIosSimulatorArm64", "com.tencent.kuikly-open:core-ksp:$kuiklyVersion")
}

android { compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 } }
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
