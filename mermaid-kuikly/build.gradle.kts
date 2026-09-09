plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

val compileKuiklyIosTestStubs by tasks.registering(Exec::class) {
    val srcFile = file("src/iosTest/c/kuikly_stubs.c")
    val outFile = layout.buildDirectory.file("intermediates/c/kuikly_stubs.o")
    inputs.file(srcFile)
    outputs.file(outFile)
    doFirst {
        outFile.get().asFile.parentFile.mkdirs()
    }
    commandLine(
        "xcrun", "--sdk", "iphonesimulator", "clang",
        "-arch", "arm64",
        "-c", srcFile.absolutePath,
        "-o", outFile.get().asFile.absolutePath
    )
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":mermaid-layout-api"))
            implementation("com.tencent.kuikly-open:core:2.24.0-raft.1-2.1.21")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(project(":mermaid-testkit"))
        }
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable> {
            val stubObj = layout.buildDirectory.file("intermediates/c/kuikly_stubs.o").get().asFile
            linkerOpts(stubObj.absolutePath)
            linkTaskProvider.configure {
                dependsOn(compileKuiklyIosTestStubs)
            }
        }
    }
}



