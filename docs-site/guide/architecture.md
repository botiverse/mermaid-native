# Architecture and host responsibilities

Mermaid Native keeps diagram behavior in a shared library and application behavior in the host.

```text
Mermaid source
    ↓ mermaid-core
Typed diagram + document metadata + diagnostics
    ↓ mermaid-layout-simple + host TextMeasurer
LayoutScene: geometry, text, styles, link regions
    ↓ renderer
SVG | Kuikly Canvas | browser Canvas | your adapter
```

## Modules

| Module | Responsibility | Depends on Kuikly? |
| --- | --- | --- |
| `mermaid-core` | Parsing, typed diagram models, document metadata, diagnostics | No |
| `mermaid-layout-api` | Scene commands, geometry, text measurement and layout interfaces | No |
| `mermaid-layout-simple` | Deterministic layout, edge routing, style resolution | No |
| `mermaid-render-svg` | SVG serialization, document accessibility metadata, supported link regions | No |
| `mermaid-kuikly` | `MermaidView` and Canvas drawing through Kuikly `ContextApi` | Yes |
| `mermaid-web` | Kotlin/Wasm exports for the playground and Canvas draw script | No |
| `mermaid-testkit` | Shared fixtures and normalized expectations for tests | No |

Parsing, layout and SVG export can be used independently of Kuikly. The normal build declares Android and iOS targets; applicable modules also declare Kotlin/Wasm. The separate HarmonyOS build uses its own compiler and four-module publication. Pure Kotlin source does not imply a published variant for every possible KMP target; consult [the integration guide](./getting-started).

## Kuikly compatibility

The Canvas renderer is implemented. Its current publication is built against the Raft Kuikly distribution, using the versions listed in the integration guide. Independent compilation and rendering against the official upstream Kuikly distribution have **not** been verified. A consumer using a different distribution needs to resolve dependency and compiler compatibility and test actual drawing; replacing a version string is not sufficient evidence.

Reusable diagram behavior belongs in mermaid-native. A Kuikly framework fix belongs in the framework only when it addresses a framework capability or defect, rather than a Mermaid-specific layout rule.

## Host responsibilities

The library prepares and draws scenes. Applications provide:

- a canvas/SVG container and appropriate viewport size;
- accurate font measurement when the default approximation is insufficient;
- scroll, pan, zoom, selection, copy/save and fullscreen UI;
- link activation using scene link regions and the same viewport transform;
- accessible semantics and keyboard navigation for native Canvas views;
- caching, lifecycle management and limits for untrusted input.

`MermaidView` is a Kuikly DSL component, not a Jetpack Compose composable. It does not create those application features automatically. Cache parsing/layout by source and relevant font/configuration inputs rather than recomputing on every paint.

## Rendering and measurement

`TextMeasurer` returns dimensions for text and `TextStyle`. Measurements and drawing must use consistent fonts and scene units. `FixedWidthTextMeasurer` provides deterministic estimates, including Unicode width handling; it is not a font-shaping engine. Platform font APIs retain their platform threading requirements.

Renderers share the same scene. New draw-command variants may require updates to custom exhaustive renderers. Main-branch APIs can be newer than the latest published SDK; review [release notes](./releases) before adopting them.

There is no DOM or WebView dependency in common diagram code. Browser integration uses JavaScript to load Kotlin/Wasm and connect browser APIs. The native path uses neither a browser runtime nor the upstream JavaScript Mermaid engine.
