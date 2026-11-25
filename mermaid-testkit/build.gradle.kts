plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

kotlin {
    wasmJs {
        browser()
    }
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

// UpstreamCorpusTest reads the imported corpus from the repository; rerun when it changes.
tasks.withType<Test>().configureEach {
    inputs.dir(rootProject.layout.projectDirectory.dir("compatibility/upstream-corpus"))
        .withPropertyName("upstreamCorpus")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
