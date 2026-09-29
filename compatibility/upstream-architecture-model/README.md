# Original Architecture model assertions

Replays 25 unmodified original architecture.spec.ts assertions through MermaidParser and its typed model. Source hashes are verified. Parser calls and programmatic DB mutations become source text; the Java bridge resolves them with production code. The JS adapter only serializes calls and returns Native fields/diagnostics.

Seven upstream-only assertions involving Cytoscape style warnings, fcose/randomize configuration and JS prototype data structures remain unselected and are not counted as Native coverage. This suite does not claim fcose algorithm or renderer parity. Nested group frames contain their child groups and services; junction ports connect through a visible branch point; row/column hints affect service centers and move whole subtrees across group boundaries. Layout regression tests verify containment and non-overlap. This deterministic layout does not implement the upstream fcose solver.

Both sides compile the original test with TypeScript ES2018 emission (the pinned upstream tsconfig target). This preserves upstream lowering of the test's optional-chain assignment, which plain esbuild rejects; no assertion source is rewritten.
