---
aside: false
outline: false
---

# Diagram Examples & Live Gallery

The Mermaid Native project maintains deterministic, verified Kotlin Multiplatform implementations across 32 diagram families.
Every example below is exercised in our continuous multiplatform test suite and rendered with the native SVG engine.
Each card also live-renders the same source with official Mermaid.js so you can compare missing details. Official Mermaid is comparison-only and is never used as a Native fallback.

<MermaidGallery :gallery-only="true" />

## Verification & Architecture

The live gallery above reflects the current bounded support matrix:

- **Strict zero fallback**: Syntax outside the bounded slice fails closed with typed diagnostics detailing exact line and column numbers. No third-party or fallback renderer is ever invoked.
- **Deterministic output**: Every diagram matches its golden SVG across Kotlin/JVM, Kotlin/Native, and Kotlin/Wasm targets.
- **Local & safe execution**: The Kotlin/Wasm adapter executes client-side in your browser. All SVG output is sanitized and stripped of script and event handlers.
- **Source corpus**: You can inspect the paired `.mmd` sources and deterministic `.svg` outputs in [`samples/`](https://github.com/botiverse/mermaid-native/tree/main/samples).
- **Standalone acceptance**: For automated CI testing and headless verification, the bare-metal Wasm acceptance runner remains available at [`/acceptance/`](/acceptance/).
