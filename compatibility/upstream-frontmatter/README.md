# Native frontmatter document boundary

This batch executes the entire unchanged upstream `diagram-api/frontmatter.spec.ts`
(16 original assertions / 18 calls) at revision
`04ee3364045d6573f84034d3c9368cc50233a92f`. The bridge calls
`MermaidFrontmatter.extract`; JavaScript only captures inputs and replays Native
values/errors into the unchanged assertions. Full value comparisons cover 17 calls;
the malformed YAML call compares the diagnostic message requested by the original
assertion. Five corrupted-result controls verify the assertions observe Native data.

`MermaidParser.parse` extracts the header, maps body diagnostics back to physical
source lines, retains typed frontmatter on `Success`, and applies supported options
to the actual diagram models. Body titles override frontmatter titles. To retain a
title for a family without a title field, use the document-aware layout overload:

```kotlin
import build.raft.mermaid.layout.simple.layout
val result = MermaidParser.parse(source)
if (result is MermaidParseResult.Success) {
    val scene = SimpleMermaidLayout.layout(result, textMeasurer, layoutConfig)
}
```

Both production Web SVG and Canvas adapters use that overload. Passing only
`result.diagram` omits the extra document title for families without a title field.
The added title transforms draw commands, gradient coordinates and optional
validation geometry consistently.

Supported production options in this batch are `sankey.showValues`,
`xyChart.showDataLabel`, and TreeView `showIcons`, `defaultIconPack`, `filenameIcons`,
`extensionIcons`. They modify the existing render/selection paths. Unimplemented
options and `displayMode` return explicit diagnostics; they are never silently
ignored. Extraction alone preserves arbitrary inert config data. External icon pack
fetching is not added.

The dependency-free YAML reader supports block mappings/sequences, flow mappings
and sequences, quoted/plain scalars, decimal numbers, booleans/null, and literal or
folded block strings. It explicitly rejects tags, anchors, aliases, merge keys,
complex keys, mapping entries inline after a block-sequence dash, and unsupported
block-scalar modifiers/escapes. It is a bounded subset, not general YAML or complete
Mermaid configuration compatibility. Limits are 256 KiB and 64 nesting levels.

Validation evidence and cumulative promotion are recorded separately after all
platform, CI and public-browser checks. No SDK release is part of this change.
