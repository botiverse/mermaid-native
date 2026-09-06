---
layout: home
hero:
  name: Mermaid Native
  text: Typed diagrams for Kotlin Multiplatform
  tagline: Mermaid-compatible parsing, deterministic layout, and native rendering without a WebView or JavaScript runtime.
  actions:
    - theme: brand
      text: Read the compatibility contract
      link: /guide/compatibility
    - theme: alt
      text: Explore examples
      link: /examples
features:
  - icon: ✓
    title: Fail-closed compatibility
    details: Unsupported syntax returns typed diagnostics instead of silently producing a different diagram.
  - icon: ◇
    title: Shared rendering model
    details: Parser, typed AST, layout scene, and draw commands stay platform-neutral in commonMain.
  - icon: ⌁
    title: Native platform adapters
    details: Android, iOS, OHOS, and Kuikly remain explicit consumers with their own support gates.
---

## Start with the contract

Mermaid Native is an independent, non-official implementation. Compatibility is declared per diagram family and syntax feature; the support matrix is the source of truth.

- [Compatibility matrix](/guide/compatibility)
- [Architecture](/guide/architecture)
- [Testing contract](/guide/testing)
- [Official family registry](/reference/official-family-registry)

::: warning Scope is explicit
A family marked `implemented` means only the declared syntax slice is covered. It does not claim full Mermaid parity.
:::
