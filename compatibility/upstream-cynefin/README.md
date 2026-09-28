# Original Cynefin integration assertions

Pinned upstream `04ee3364045d6573f84034d3c9368cc50233a92f`. Build the Native core debug runtime, then run `run.py --upstream PATH --stdlib PATH`.

The original 15 `cynefin.integration.spec.ts` tests execute unchanged against `MermaidParser.parse` and typed `CynefinDiagram` getters (16 unique inputs). The adapter reconstructs the expected domain Map and exposes Native title/accessibility/transitions. It never uses the upstream parser to supply Native results. Baseline: 12/15; the accessibility and combined feature cases fail.

Production parsing now preserves title, accessibility title and single/multiline description; layout forwards accessibility to the existing scene/SVG consumer without changing quadrant colors or geometry. Canonical colon header, quoted/escaped/empty labels, repeated title/domain replacement, transition labels and self-loop filtering are supported. The existing `cynefin` alias and case-insensitive domain names remain extensions.

The 27 standalone DB/SVG boundary helper tests are separate and unclaimed. This is not full Langium CST, exact diagnostic positions, arbitrary YAML/directive configuration, HTML sanitization or renderer pixel parity.
