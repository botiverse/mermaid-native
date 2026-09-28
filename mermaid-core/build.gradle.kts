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


val versionSources = layout.buildDirectory.dir("generated/nativeVersion/commonMain")
val generateNativeVersion by tasks.registering {
    val nativeVersion = project.version.toString()
    inputs.property("nativeVersion", nativeVersion)
    outputs.dir(versionSources)
    doLast {
        val target = versionSources.get().file("build/raft/mermaid/core/NativeVersion.kt").asFile
        target.parentFile.mkdirs()
        val escaped = nativeVersion.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$").replace("\n", "\\n").replace("\r", "\\r")
        target.writeText("package build.raft.mermaid.core\n\npublic const val MERMAID_NATIVE_VERSION: String = \"$escaped\"\n")
    }
}
kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(generateNativeVersion)
tasks.matching { it.name.startsWith("compile") }.configureEach { dependsOn(generateNativeVersion) }
