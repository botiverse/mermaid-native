# Official Mermaid family registry

The machine-readable registry in
[`compatibility/diagram-families.csv`](../compatibility/diagram-families.csv)
is pinned to Mermaid revision
`04ee3364045d6573f84034d3c9368cc50233a92f` (see
[`compatibility/upstreams.lock`](../compatibility/upstreams.lock)). It is the
progress source of truth for the full official compatibility goal.

This census contains 32 documented Mermaid families. `railroad` is one family
entry because Mermaid documents four railroad dialects under one syntax
family. `zenuml` is included because it is an official Mermaid syntax entry,
although Mermaid loads it as an external diagram package.

The status contract is deliberately strict:

- `implemented` requires parser, typed AST, layout, SVG renderer, positive and
  negative fixtures, and platform/test coverage for the declared family scope.
- `in_progress` means work exists or the family is actively tracked, but the
  complete contract is not closed.
- `not_started` means no implementation work has been accepted.
- `blocked` is reserved for a concrete external or technical blocker.

The current flowchart, sequence, and class vertical slices therefore remain
`in_progress`: they are executable and tested, but they are not full family
parity.
The registry must be updated in the same PR that closes a family's parser,
layout, render, and tests; registering a name alone never means support.
