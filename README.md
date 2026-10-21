# Mermaid Native

[![Built with Raft](https://raw.githubusercontent.com/raft-builders/awesome-raft-build/main/badge/built-with-raft.svg)](https://raft.build)

Render Mermaid-compatible diagrams in Kotlin Multiplatform applications without a WebView or a JavaScript diagram engine. Mermaid Native provides parsing, typed diagram models, deterministic layout, SVG export, and a Kuikly Canvas renderer.

[Documentation](https://botiverse.github.io/mermaid-native/) · [Try the playground](https://botiverse.github.io/mermaid-native/playground) · [Integration guide](docs-site/guide/getting-started.md) · [Release notes](docs-site/guide/releases.md)

This is an independent implementation, not an official Mermaid project. It covers **32 diagram families with feature-specific limits**, rather than the entire Mermaid API or identical upstream layout. Start with the [support matrix](docs-site/reference/families.md) when adopting advanced syntax.

## Choose an integration

| What you need | Modules | Current availability |
| --- | --- | --- |
| Parse diagrams and inspect typed models | `mermaid-core` | Every official KMP target: Android, iOS, macOS, JVM, JS, Wasm, Linux and Windows; separate HarmonyOS publication |
| Compute layout or build your own renderer | `mermaid-layout-simple`, `mermaid-layout-api` | Same platforms; no Kuikly dependency |
| Export SVG | `mermaid-render-svg` | Same platforms as `mermaid-core` |
| Draw in a Compose Multiplatform app | `mermaid-compose` | Android, iOS, desktop JVM and Wasm, on JetBrains Compose Multiplatform 1.8 |
| Draw in a Kuikly app | `mermaid-kuikly` | Raft integration plus an independent official Kuikly 2.28.0 Android example; see the verified scope below |
| Use a browser demo | `mermaid-web` | Kotlin/Wasm playground and Canvas demo; JavaScript loads Wasm and connects browser APIs |

Current release: **0.1.14** (**0.1.14-ohos** for HarmonyOS). The engine modules (core, layout and SVG) are published for every official Kotlin Multiplatform target, and `mermaid-compose` for Android, iOS, desktop JVM and Wasm. Nothing needs a JavaScript runtime; the browser demos use JavaScript only to host Wasm. There is currently no Swift XCFramework, CocoaPod, or HarmonyOS HAR distribution of this SDK.

**Release vs. main:** published artifacts come from the release tag. The website and repository describe the current main branch, which may contain unreleased APIs. See [releases and upgrading](docs-site/guide/releases.md); do not assume a feature on main is present in 0.1.14. The incomplete 0.1.10 release pair should not be used.

## Quick start: KMP to SVG

For an existing Android/iOS KMP project using Kotlin 2.1.21, add the public Maven repository to `settings.gradle.kts`. Artifact reads do not require credentials:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.artifacts.botiverse.dev") {
            content { includeGroup("build.raft.mermaid") }
        }
    }
}
```

Add these dependencies to your shared module:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("build.raft.mermaid:mermaid-layout-simple:0.1.14")
            implementation("build.raft.mermaid:mermaid-render-svg:0.1.14")
        }
    }
}
```

Parsing and the layout API are transitive dependencies. This example retains document titles and supported frontmatter settings by passing the complete parse result to layout:

```kotlin
import build.raft.mermaid.core.MermaidParseResult
import build.raft.mermaid.core.MermaidParser
import build.raft.mermaid.layout.LayoutConfig
import build.raft.mermaid.layout.simple.FixedWidthTextMeasurer
import build.raft.mermaid.layout.simple.SimpleMermaidLayout
import build.raft.mermaid.layout.simple.layout
import build.raft.mermaid.render.svg.SvgRenderer

fun diagramSvg(source: String): String = when (val parsed = MermaidParser.parse(source)) {
    is MermaidParseResult.Success -> SvgRenderer.render(
        SimpleMermaidLayout.layout(parsed, FixedWidthTextMeasurer, LayoutConfig()),
        baseId = "example-diagram",
    )
    is MermaidParseResult.Failure -> error(parsed.diagnostics.joinToString("\n"))
}

// diagramSvg("flowchart LR\nA[Start] --> B[Finish]")
```

In an application, show diagnostics in the UI instead of throwing. Give each inline SVG a distinct `baseId`. The fixed-width measasurer is an approximation; provide a host `TextMeasurer` for accurate font metrics.

For Compose Multiplatform, add `build.raft.mermaid:mermaid-compose` and draw a scene with `MermaidCanvas(scene, modifier, scale, textStyle)`. One scene unit is one dp times `scale`. The canvas sizes itself to `scene.width × scene.height × scale` dp, and size modifiers you pass still take precedence. Very large diagrams are scaled down so neither side exceeds `MermaidMaxSidePx` (32,000 px); `mermaidSafeScale(width, height, scale, density)` computes the same limit for your own layout. Text uses `textStyle` (default `TextStyle.Default`) as its base style, with the scene's size, weight, italic and color. The scene's font family applies only when `textStyle` has no `fontFamily`, so pass your UI font to use it for every label. Text is measured once per scene and style. When the scene has an accessibility title or description, the canvas uses it as its content description.

To draw into your own `DrawScope`, call `drawMermaidScene(scene, textMeasurer, scale, textStyle)` with a `TextMeasurer` from `rememberTextMeasurer()`. This overload draws one scene unit as `scale` pixels and measures text on every call. To avoid that, keep `measureMermaidScene(scene, textMeasurer, textStyle)` in `remember` and call `drawMermaidScene(measured, scale)`.

For Kuikly, add `mermaid-kuikly` and its repository group, then draw a prepared scene with `MermaidView` or `MermaidKuiklyRenderer`. An existing Kuikly host and matching native renderer are required. See the [complete Kuikly and HarmonyOS instructions](docs-site/guide/getting-started.md), including compiler versions and dependency boundaries.

## What lives in the library?

```text
Mermaid text → typed model → layout scene → SVG / Kuikly Canvas / custom renderer
```

Diagram syntax, layout, styles, geometry, and renderer adapters live in **mermaid-native**. The core, layout, and SVG modules do not depend on Kuikly. `mermaid-kuikly` is the optional UI adapter; its published build currently depends on the Raft Kuikly artifacts. Standard-looking Canvas APIs alone do not establish binary or runtime compatibility with another Kuikly distribution.

Your app supplies fonts, a viewport, scrolling/zoom gestures, navigation and accessibility interactions. `MermaidView` draws the diagram; it does not provide a complete editor or message card. See [architecture and host responsibilities](docs-site/guide/architecture.md).

## Compatibility and limitations

- Flowcharts, sequence, class, state, ER, Gantt, pie, XY, Sankey, Treemap and other families are covered within the [declared feature matrix](docs-site/reference/families.md).
- Unsupported syntax and configuration produce typed diagnostics. Handle failures explicitly; do not treat them as an empty successful diagram.
- Complex graphs can still overlap. Layout and text appearance are not pixel-identical to upstream Mermaid.
- Configuration, Markdown, links and accessibility support are bounded. Kuikly hosts implement link activation and semantic accessibility themselves.
- The standalone [kuikly-examples](kuikly-examples/) build verifies official Kuikly **2.28.0-2.1.21** with Mermaid **0.1.11**: Android emulator rendering of four samples and iOS shared-code compilation. iOS native linking/runtime, HarmonyOS with official Kuikly, H5 and WeChat mini-program integration remain unverified.
- APIs may change during the 0.x series. Recompile consuming code and review custom renderers when upgrading.

Original upstream assertions, Native layout tests, and rendered-image comparisons measure different things. A passing parser test is not proof of visual or device parity. See [testing](docs-site/guide/testing.md) for reproducible checks and coverage boundaries.

## Build and contribute

Contributions to syntax coverage, layout quality, platform adapters and documentation are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for setup, focused commands and what to include in a pull request. Report reproducible bugs through [GitHub Issues](https://github.com/botiverse/mermaid-native/issues), with the diagram source, SDK version, platform and expected result. See [SECURITY.md](SECURITY.md) for security reporting and host precautions.

## License

[Apache-2.0](LICENSE). Third-party code and fixtures retain their original licenses and attribution in [NOTICE](NOTICE). This project is not affiliated with or endorsed by the Mermaid project.
