pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Kotlin's Wasm/Node toolchain contributes the Node distribution repo at
    // project configuration time; prefer that repo so wasm runtime tests can
    // resolve the pinned Node distribution locally and in Hosted CI.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "mermaid-native"

include(
    ":mermaid-core",
    ":mermaid-layout-api",
    ":mermaid-layout-simple",
    ":mermaid-render-svg",
    ":mermaid-web",
    ":mermaid-kuikly",
    ":mermaid-testkit",
)
