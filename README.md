# Mermaid Native

Render Mermaid-compatible diagrams in Kotlin Multiplatform applications without a WebView or a JavaScript diagram engine. Mermaid Native provides parsing, typed diagram models, deterministic layout, SVG export, and a Kuikly Canvas renderer.

[Documentation](https://botiverse.github.io/mermaid-native/) · [Try the playground](https://botiverse.github.io/mermaid-native/playground) · [Integration guide](docs-site/guide/getting-started.md) · [Release notes](docs-site/guide/releases.md)

This is an independent implementation, not an official Mermaid project. It covers **32 diagram families with feature-specific limits**, rather than the entire Mermaid API or identical upstream layout. Start with the [support matrix](docs-site/reference/families.md) when adopting advanced syntax.

## Choose an integration

| What you need | Modules | Current availability |
| --- | --- | --- |
| Parse diagrams and inspect typed models | `mermaid-core` | Android/iOS KMP; separate HarmonyOS publication |
| Compute layout or build your own renderer | `mermaid-layout-simple`, `mermaid-layout-api` | Same platforms; no Kuikly dependency |
| Export SVG | `mermaid-render-svg` | Android/iOS KMP and repository Kotlin/Wasm build |
| Draw in a Kuikly app | `mermaid-kuikly` | Raft integration plus an independent official Kuikly 2.28.0 Android example; see the verified scope below |
| Use a browser demo | `mermaid-web` | Kotlin/Wasm playground and Canvas demo; JavaScript loads Wasm and connects browser APIs |

The latest verified SDK pair is **0.1.11** for Android/iOS and **0.1.11-ohos** for HarmonyOS. The native modules do not require a JavaScript runtime. The browser demos do use JavaScript host code. There is currently no standalone JVM/Desktop target, Swift XCFramework, CocoaPod, or HarmonyOS HAR distribution of this SDK.

**Release vs. main:** published artifacts come from the release tag. The website and repository describe the current main branch, which may contain unreleased APIs. See [releases and upgrading](docs-site/guide/releases.md); do not assume a feature on main is present in 0.1.11. The incomplete 0.1.10 release pair should not be used.

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
            implementation("build.raft.mermaid:mermaid-layout-simple:0.1.11")
            implementation("build.raft.mermaid:mermaid-render-svg:0.1.11")
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
