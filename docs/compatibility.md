# Compatibility matrix

This table describes the intentionally small Phase 0 contract. It is not a
claim of full Mermaid compatibility.

| Family | Supported now | Explicitly deferred |
| --- | --- | --- |
| `flowchart` / `graph` | `TD`, `TB`, `LR`, `BT`, `RL`; bare and `[label]` nodes; `-->` edges | Other edge operators, shapes, subgraphs, styles, links, click handlers |
| `sequenceDiagram` | auto-registered participants; `->>` and `-->>`; optional message label | `participant`/`actor` declarations, notes, activation, blocks, `else`/`and`, response variants |
| Other Mermaid families | rejected with `UNSUPPORTED_DIAGRAM` | all other families until separately specified and tested |

Unsupported syntax is a normal, typed failure. Consumers must not treat a
failure as an empty diagram.

The compatibility corpus is pinned in `compatibility/upstreams.lock` and uses
the MIT `beautiful-mermaid` and its Swift port as implementation references;
upstream Mermaid remains the syntax reference.
