pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories {
    google(); mavenCentral()
    maven("https://mirrors.tencent.com/repository/maven-tencent/") { content { includeGroup("com.tencent.kuikly-open") } }
    maven("https://maven.artifacts.botiverse.dev") { content { includeGroup("build.raft.mermaid") } }
} }
rootProject.name = "kuikly-examples"
include(":shared", ":androidApp")
