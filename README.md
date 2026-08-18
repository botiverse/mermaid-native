# Mermaid Native

Mermaid-compatible diagram parsing and native rendering implemented with Kotlin
Multiplatform. The project does not use a WebView or JavaScript at runtime.

This is an independent, non-official implementation. Compatibility is declared
per diagram family and syntax feature; unsupported syntax fails with typed
diagnostics rather than silently rendering a different diagram.

## Modules

- `mermaid-core`: parser, typed AST, and diagnostics.
- `mermaid-layout-api`: toolkit-neutral scene graph, draw commands, and layout SPI.
- `mermaid-layout-simple`: deterministic MIT-licensed starter layout.
- `mermaid-render-svg`: common SVG serializer.
- `mermaid-kuikly`: Kuikly Canvas/Text adapter for Raft Mobile.
- `mermaid-testkit`: compatibility fixtures and geometry goldens.

All artifacts share one version and are published under `build.raft.mermaid`.
ELK support is deliberately outside the MIT core; any future `layout-elk`
artifact must carry its own EPL-2.0 obligations.

## Current parser support

The first supported syntax slices are:

- `sequenceDiagram` with one `A->>B` message;
- `flowchart` with one `A-->B` edge.

Parser support is covered on Android and iOS. Layout, SVG, and native host
support remain separate compatibility entries and are not implied by parser
support.

## Quick start

```kotlin
val result = MermaidParser.parse("flowchart LR; A[Start] --> B[Finish]")
when (result) {
    is MermaidParseResult.Success -> {
        // Pass the typed diagram to a layout/render module.
    }
    is MermaidParseResult.Failure -> result.diagnostics.forEach(::println)
}
```

The parser is deliberately fail-closed: syntax outside the declared support
matrix returns a typed diagnostic instead of silently changing diagram type or
dropping statements. See [compatibility](docs/compatibility.md),
[architecture](docs/architecture.md), and [testing](docs/testing.md).

## Examples

The [`samples`](samples/) directory contains flowchart and sequence sources
adapted from the pinned beautiful-mermaid corpus. The same inputs have
normalized AST expectations in `mermaid-testkit`, so the examples are executed
by the multiplatform test suite instead of drifting as documentation-only text.

## Contributing

This is an independent, non-official Mermaid-compatible implementation. New
syntax needs a support-matrix entry, parser tests, a negative/unsupported case,
and a fixture or differential vector before it is considered complete. See
[CONTRIBUTING.md](CONTRIBUTING.md).
