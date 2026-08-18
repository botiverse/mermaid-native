# Mermaid Native

Mermaid-compatible diagram parsing and native rendering implemented with Kotlin
Multiplatform. The project does not use a WebView or JavaScript at runtime.

This is an independent, non-official implementation. Compatibility is declared
per diagram family and syntax feature; unsupported syntax fails with typed
diagnostics rather than silently rendering a different diagram.

## Modules

- `mermaid-core`: parser, typed AST, and diagnostics.
- `mermaid-layout-api`: toolkit-neutral scene graph, draw commands, and layout SPI.
- `mermaid-layout-simple`: deterministic MIT-licensed Phase 0 layout.
- `mermaid-render-svg`: common SVG serializer.
- `mermaid-kuikly`: Kuikly Canvas/Text adapter for Raft Mobile.
- `mermaid-testkit`: compatibility fixtures and geometry goldens.

All artifacts share one version and are published under `build.raft.mermaid`.
ELK support is deliberately outside the MIT core; any future `layout-elk`
artifact must carry its own EPL-2.0 obligations.

## Phase 0

The first vertical slices are:

- `sequenceDiagram` with one `A->>B` message;
- `flowchart` with one `A-->B` edge.

Both must pass parser diagnostics, deterministic geometry, SVG output, and
Android/iOS/OHOS native rendering evidence before their compatibility entries
are marked supported.
