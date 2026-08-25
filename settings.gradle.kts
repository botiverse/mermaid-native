pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
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
