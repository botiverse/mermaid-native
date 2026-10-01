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

## Accessible SVG exports

`SvgRenderer.render(scene)` emits linked SVG title and description metadata when the layout scene provides accessible text. Empty metadata is omitted. The SVG declares the graphics-document role with a document fallback.

For several inline SVG instances on one page, pass a different `baseId` for each instance. The optional diagram type supplies a role description:

```kotlin
val svg = SvgRenderer.render(scene, baseId = "message-42", diagramType = "flowchart")
```

Use letters, digits, underscores, periods, colons, or hyphens in IDs consumed by the provided web sanitizer. Title and description references must resolve within the same SVG. The one-argument renderer derives deterministic IDs from the accessible text for standalone exports; it does not allocate globally unique instance IDs.

## Railroad grammar detection

`RailroadSyntax.detect(source)` identifies the Native, ABNF, EBNF or PEG Railroad
header without parsing its body. It returns null for an unknown header. Leading
blank lines and case variants are accepted; the whole header line must match.
`MermaidParser.parse` uses the same detector after preprocessing comments, then
runs the selected grammar parser. Detection alone does not guarantee valid
syntax, and the low-level detector does not remove comments or metadata.
