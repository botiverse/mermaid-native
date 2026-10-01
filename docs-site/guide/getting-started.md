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

## TreeView icons

TreeView supports native vector glyphs for `icon(file)` and `icon(folder)` (also
`icon(mermaid-treeview:file)` and `icon(mermaid-treeview:folder)`). `icon(none)`
hides the icon while retaining the tree's connector dot. Custom pack references
remain visible as fallback text; external Iconify packs are not downloaded.

```text
treeView-beta
/ icon(folder)
  src/ icon(folder) ## Source files
    main.kt icon(file)
```

Kotlin callers can configure automatic icons on the parsed model before layout:

```kotlin
val tree = (MermaidParser.parse(source) as MermaidParseResult.Success).diagram as TreeViewDiagram
val configured = tree.copy(iconConfig = TreeViewIconConfig(
    showIcons = true,
    filenameIcons = mapOf("Dockerfile" to "file"),
    extensionIcons = mapOf(".kt" to "file", ".tmp" to "none"),
))
val scene = SimpleMermaidLayout.layout(configured, FixedWidthTextMeasurer, LayoutConfig())
```

Explicit annotations win over automatic detection. Exact filename mappings win
over extension mappings; extensions are matched in lowercase, with or without
the leading dot. Built-in names always use the built-in pack. A `defaultIconPack`
qualifies other unprefixed references. These options are programmatic and are
not read from Mermaid YAML frontmatter.

## Detect a diagram family before parsing

```kotlin
val type = MermaidParser.detectType("graph TD\nunfinished body")
check(type == MermaidDiagramType.FLOWCHART)
println(type.id) // flowchart
```

The detector identifies a supported Native family from the first header, using
exactly the same comment and whitespace handling as `MermaidParser.parse`.
A detected family does not validate the body or header options; call `parse`
to obtain the model or typed diagnostics. Empty or unknown headers return null.
It does not extract YAML frontmatter or apply Mermaid init configuration.
Family IDs describe Native parser families, independent of upstream renderer
versions and layout engines; all Railroad dialects belong to `RAILROAD`.
