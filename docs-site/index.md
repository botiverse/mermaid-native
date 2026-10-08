---
layout: home
hero:
  name: Mermaid Native
  text: Typed diagrams for Kotlin Multiplatform
  tagline: Mermaid-compatible parsing, deterministic layout, and native rendering without a WebView or JavaScript runtime.
  actions:
    - theme: brand
      text: Get started
      link: /guide/getting-started
    - theme: alt
      text: Try the playground
      link: /playground
features:
  - icon: ✓
    title: Fail-closed compatibility
    details: Unsupported syntax returns typed diagnostics instead of silently producing a different diagram.
  - icon: ◇
    title: Shared rendering model
    details: Parser, typed AST, layout scene, and draw commands stay platform-neutral in commonMain.
  - icon: ⌁
    title: Native platform adapters
    details: Optional Kuikly Canvas rendering, shared SVG export, and a Kotlin/Wasm browser demo.
---

## Integrate diagrams into your application

Current verified SDK: **0.1.11** for Android/iOS and **0.1.11-ohos** for HarmonyOS. Parsing, layout and SVG export work independently of Kuikly. The optional Kuikly adapter is tested with the Raft distribution; official upstream Kuikly integration is not yet independently verified.

[Get started](/guide/getting-started) · [Release notes](/guide/releases) · [GitHub](https://github.com/botiverse/mermaid-native)

The website follows main, which can contain unreleased changes. Use the release notes to distinguish published SDK capabilities from newer source code.

## Check the features you need

Mermaid Native is an independent, non-official implementation. Compatibility is declared per diagram family and syntax feature; the support matrix is the source of truth.

- [Compatibility matrix](/guide/compatibility)
- [Architecture](/guide/architecture)
- [Testing contract](/guide/testing)
- [Official family registry](/reference/official-family-registry)

::: warning Scope is explicit
A family marked `implemented` means only the declared syntax slice is covered. It does not claim full Mermaid parity.
:::
