# Architecture

The repository keeps product-neutral diagram data separate from platform
rendering:

1. `mermaid-core` turns source into a typed AST and source-located diagnostics.
2. `mermaid-layout-api` defines layout input/output and draw-command seams.
3. `mermaid-layout-simple` supplies the permissive deterministic Phase 0
   layout. A future ELK adapter is isolated because ELK/elk-swift are
   EPL-2.0, not MIT.
4. `mermaid-render-svg` serializes common data; an ASCII renderer may be added
   later as a separate module.
5. `mermaid-kuikly` and platform samples provide host text measurement and
   drawing; no DOM, WebView, or JavaScript type belongs in `commonMain`.

The AST intentionally has no coordinates or font assumptions. Layout receives a
text-measurement seam and returns stable geometry. This makes parser tests and
geometry tests deterministic while allowing Android, iOS, and OHOS fonts to
remain platform-specific.

Public modules share one version and are published with Gradle Module Metadata.
Consumers should depend on exact published artifacts rather than source or
composite builds.
