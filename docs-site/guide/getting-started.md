# Getting started

Mermaid Native is currently consumed from source while artifact publication remains gated. The repository builds with JDK 17 and the included Gradle wrapper.

## Build and test

```bash
git clone https://github.com/botiverse/mermaid-native.git
cd mermaid-native
./gradlew check
```

Run the focused multiplatform compatibility suites:

```bash
./gradlew \
  :mermaid-core:allTests \
  :mermaid-layout-simple:allTests \
  :mermaid-testkit:allTests \
  :mermaid-web:allTests
```

## Parse source

```kotlin
val result = MermaidParser.parse("flowchart LR; A[Start] --> B[Finish]")
when (result) {
    is MermaidParseResult.Success -> {
        // Pass result.diagram to a supported layout and renderer.
    }
    is MermaidParseResult.Failure -> {
        result.diagnostics.forEach(::println)
    }
}
```

## Choose a module

| Module | Responsibility |
| --- | --- |
| `mermaid-core` | Parser, typed AST, and diagnostics |
| `mermaid-layout-api` | Platform-neutral scene graph and layout SPI |
| `mermaid-layout-simple` | Deterministic built-in layout |
| `mermaid-render-svg` | Common SVG serialization |
| `mermaid-web` | Kotlin/Wasm browser-facing adapter |
| `mermaid-testkit` | Fixtures, semantic vectors, and geometry goldens |
| `mermaid-kuikly` | Reserved Kuikly adapter module; the real renderer is not implemented yet |

::: warning Publication status
The Gradle publication model and Maven coordinates exist, but no stable Maven release is currently documented. Do not copy an unpublished version into production dependencies.
:::
