plugins {
    id("com.android.library")
    kotlin("multiplatform")
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
        val nativeTarget = this
        val sdk = if (nativeTarget.name == "iosArm64") "iphoneos" else "iphonesimulator"
        val arch = if (nativeTarget.name == "iosX64") "x86_64" else "arm64"
        val stubObj = layout.buildDirectory.file("intermediates/c/${nativeTarget.name}/kuikly_stubs.o")
        val compileStubs = tasks.register<Exec>("compileKuikly${nativeTarget.name.replaceFirstChar { it.uppercase() }}TestStubs") {
            val srcFile = file("src/iosTest/c/kuikly_stubs.c")
            inputs.file(srcFile)
            inputs.property("sdk", sdk)
            inputs.property("arch", arch)
            outputs.file(stubObj)
            doFirst { stubObj.get().asFile.parentFile.mkdirs() }
            commandLine("xcrun", "--sdk", sdk, "clang", "-arch", arch,
                "-c", srcFile.absolutePath, "-o", stubObj.get().asFile.absolutePath)
        }
        binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable> {
            linkerOpts(stubObj.get().asFile.absolutePath)
            linkTaskProvider.configure { dependsOn(compileStubs) }
        }
    }
}



