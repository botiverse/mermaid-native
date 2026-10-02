# d3-sankey layout attribution and scope

`SankeyFlowLayout.kt` adapts DAG depth, justify alignment, proportional breadth allocation, collision resolution, link ordering and six relaxation iterations from d3-sankey 0.12.3. The original BSD-3-Clause license is retained in this directory; sources.json pins the source URL and SHA-256.

The shared renderer uses 600×400 defaults, 10px Tableau-colored node bars, 27px padding with displayed values, external 14px labels and 50% opaque two-stop gradients. Curved link outlines approximate a constant-width cubic stroke using 48 sampled segments. Measured long captions may expand the canvas. The existing cyclic fallback is retained; non-default Mermaid configuration and exact upstream pixel parity are not claimed.

Regression verification includes the reported Grid/Industry/Heating example, split/merge/long-caption inputs, exact expected d3 node positions, flow-width conservation, deterministic SVG gradient scoping, actual Canvas endpoint/opacity pixels and calls to the pinned Kuikly gradient API. Kuikly mock tests are not device screenshot evidence.

This port adds no original Mermaid test assertions to the original-assertion coverage ledger.
