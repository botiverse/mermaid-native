# Official Mermaid conformance pilot

This offline corpus copies small source snippets from Mermaid at the exact
revision in `compatibility/upstreams.lock`. Mermaid remains MIT licensed; each row
in `manifest.tsv` records the original path and test case. The JavaScript test
harness and Mermaid runtime are not copied or shipped.

`supported` cases must parse and match a normalized semantic projection.
`unsupported` and `deferred` cases must fail closed with a typed diagnostic at
the recorded physical line and column. SVG checks assert deterministic native
output and structural text/shape invariants; upstream SVG snapshots are not a
layout or byte-level contract for this project.

The manifest is tab-separated and sorted by family/case. Its source hash is
SHA-256 over the fixture bytes. Updating the pinned Mermaid revision requires
an explicit manifest diff showing added, removed, changed, and unchanged rows.
