# Compatibility contract

Mermaid Native implements **bounded, tested vertical slices** across the 32-family registry. It does not claim full Mermaid parity for any family unless the registry explicitly says so.

## What support means

A supported slice includes all of the following:

1. source parsing with typed AST output;
2. source-located diagnostics for rejected input;
3. deterministic layout and SVG rendering;
4. positive and negative fixtures;
5. a right-cause regression that fails when the production path is removed.

Unsupported syntax fails closed. Consumers must not treat a failure as an empty diagram or silently fall back to a different family.

## Current breadth

The current registry contains 32 families with partial vertical slices, including:

- **Structure:** flowchart, class, state, entity relationship, block, architecture, C4, use case, and tree view;
- **Process:** sequence, gantt, timeline, user journey, kanban, event modeling, swimlanes, and ZenUML;
- **Analysis:** requirement, mindmap, Ishikawa, Cynefin, Wardley, quadrant, and Venn;
- **Data:** pie, XY, radar, sankey, treemap, packet, git graph, and railroad.

See the generated [diagram family matrix](/reference/families) for the current machine-readable status of every family.

## Compatibility boundaries

Family names alone are not evidence of full support. Configuration directives, theming, interactivity, accessibility metadata, alternate operators, nesting, and advanced syntax remain family-specific. Read the notes in [`compatibility/diagram-families.csv`](https://github.com/botiverse/mermaid-native/blob/main/compatibility/diagram-families.csv) before depending on a syntax feature.

The compatibility corpus is pinned in [`compatibility/upstreams.lock`](https://github.com/botiverse/mermaid-native/blob/main/compatibility/upstreams.lock). Upstream Mermaid is the syntax reference; external implementations are used only as attributed implementation references and fixtures.
