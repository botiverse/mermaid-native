# Examples

The repository ships executable `.mmd` sources and deterministic SVG goldens for the accepted compatibility slices. Every example is exercised by the multiplatform test suite; examples are not documentation-only screenshots.

## Browser gallery

The maintained Kotlin/Wasm acceptance gallery renders the current example corpus through the public `mermaid-web` adapter.

[Open the live gallery](https://botiverse.github.io/mermaid-native/playground/){ .VPButton .brand }

## Source corpus

Browse the paired sources and SVG outputs in [`samples/`](https://github.com/botiverse/mermaid-native/tree/main/samples).

Representative families include:

- flowchart and sequence diagrams
- state, class, entity relationship, and requirement diagrams
- gantt, timeline, user journey, and kanban
- architecture, C4, packet, sankey, treemap, radar, and XY charts

The exact accepted syntax remains governed by the [compatibility contract](/guide/compatibility), not by the presence of a family name in this list.
