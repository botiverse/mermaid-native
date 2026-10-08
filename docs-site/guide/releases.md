# SDK releases and upgrading

## Unreleased on main

The main branch includes additional edge-route refinement and typed stroke paths (moves, lines, quadratic curves and circular arcs) across SVG, browser Canvas and Kuikly. These changes are **not included in 0.1.11**. The typed-path addition introduces a new draw command for custom renderers to handle; it does not itself enable crossing hops in existing diagrams.

Use a release tag and the matching published artifacts for a reproducible integration. The latest verified release remains the pair below.

## 0.1.11 / 0.1.11-ohos

**Available.** Both planes were published by [release workflow 37754435207](https://github.com/botiverse/mermaid-native/actions/runs/37754435207) from immutable tag [`v0.1.11`](https://github.com/botiverse/mermaid-native/tree/v0.1.11), source [`feb4d563`](https://github.com/botiverse/mermaid-native/commit/feb4d563f25877a5301484d792f43f02f165e6b6). Later main-branch commits are not included in these artifacts.

[The release receipt](../releases/sdk-0.1.11-readback.json) records all 49 module coordinates (41 normal and 8 OHOS) and 217 files checked from the public Maven repository: dependency versions, variant references, sizes and published hashes. Actual published Android AARs compiled the guide functions and Kuikly DSL, then passed SDK-version, flowchart, Sankey-gradient, expanded-shape, Kanban-link, XY-palette and Treemap-formatting checks. This is published-package verification; installed-app and device acceptance remain separate.

Changes since 0.1.9:

- Flowcharts: routes across and inside compound containers, separated return tracks, fewer avoidable crossing/label collisions, measured endpoint clipping and guarded orthogonal cleanup. Complex graphs can still overlap; this is not a global optimal-layout guarantee.
- Document frontmatter: typed metadata, titles/accessibility, and explicitly bounded configuration. Unknown settings produce diagnostics. Expanded manual-input, stacked-document and stacked-process shapes now produce distinct geometry.
- Usecase: tokenization, semantic/metadata checks, original source locations, editable document/AST contracts and wrapped Markdown label handling.
- Additional typed model/document contracts for trees, Railroad, Journey and ER; Cynefin boundaries now use shared Native geometry. Block circle/cylinder paint and Wardley syntax corrections are included.
- XY palettes, Treemap comma/currency formatting, and Kanban HTTP(S) ticket regions reach their actual rendering consumers. The web demos support bounded SVG links and Canvas pointer/keyboard navigation. Native hosts implement their own navigation.

Upgrade all normal modules together to `0.1.11` and the separate HarmonyOS modules to `0.1.11-ohos`. Keep the host’s existing Kuikly pins and verify the resolved dependency graph; the adapter itself was built against `2.24.0-raft.1`, so other host distributions need their own compile/runtime check. Rebuild host code and custom scene renderers because binary compatibility is not promised. New public model members and shape variants require review of exhaustive host switches. `LayoutScene.links` describes link rectangles/URLs/labels; apply the same zoom/pan transform as drawing, reject unsupported schemes, and let the host decide how to open them. SVG sanitizers must allow only the bounded anchor/rectangle structure described in the integration guide.

Parsing tests are not visual parity. Native still lacks some original Mermaid behavior, small Treemap cells may have cramped/overflowing labels, and downstream Android/iOS/OHOS app/device acceptance remains required. Kotlin/Wasm is not an H5 or WeChat mini-program Kuikly adapter. The Kuikly module remains built against the Raft Kuikly distribution, not independently verified against the public upstream distribution.

## 0.1.10 / 0.1.10-ohos — incomplete release pair

Do not adopt this version pair. [Release workflow 37743538290](https://github.com/botiverse/mermaid-native/actions/runs/37743538290) published the OHOS plane, but normal publication failed during common metadata compilation after some Android files had already been uploaded. The existing tag and files are retained unchanged. 0.1.11 fixes the non-portable frontmatter location API and adds common metadata compilation to CI and before normal uploads.

## 0.1.9 / 0.1.9-ohos

**Available.** Both release planes were published by
[the 0.1.9 release workflow](https://github.com/botiverse/mermaid-native/actions/runs/36990371765)
from immutable tag [`v0.1.9`](https://github.com/botiverse/mermaid-native/tree/v0.1.9),
source [`63c2c7ed`](https://github.com/botiverse/mermaid-native/commit/63c2c7edbeba8926acb22de041b6377409a237e5).
This release includes engine changes through
[PR #219](https://github.com/botiverse/mermaid-native/pull/219); later main-branch
changes are not included in these artifacts.

- Sankey: flow-proportional bars and curved bands, upstream DAG positioning,
  Tableau colors, value labels and translucent gradients in SVG and Kuikly Canvas.
- Packet: gray square fields, black borders, bit-number placement, row spacing
  and bottom-centered title aligned with the upstream default style.
- Flowcharts: improved cycle/skip-edge obstacle routing and fewer avoidable
  detour crossings, including TD, LR, RL and BT cases; nested boundaries preserved.
- Node labels: basic Markdown bold/italic/line breaks and better width estimates
  for combined emoji sequences.
- Additional original Mermaid parser/model/helper assertions run against Native
  production code; this is not a claim of complete upstream rendering parity.

### Upgrade from 0.1.8

1. Update every normal Mermaid module together to `0.1.9`, and every module in
   the separate HarmonyOS build to `0.1.9-ohos`. Keep the existing Kuikly
   `2.24.0-raft.1` release pair; this update does not require a Kuikly SDK bump.
2. Recompile all host code against the new libraries. `DrawPolygon` now carries
   optional gradient information; source callers retain defaults, but binary
   compatibility with precompiled custom renderers is not promised.
3. If you implement your own scene renderer, handle `DrawPolygon.gradient`.
   If you sanitize SVG exports, support the bounded local gradient format
   described in the [integration guide](./getting-started).
4. Resolve Android/iOS and OHOS dependency graphs separately, rebuild the shared
   framework/HAR, and check `MERMAID_NATIVE_VERSION` in the installed client.
5. Exercise Sankey and Packet examples plus existing diagrams in your host at
   small screen sizes and with long labels. Confirm scrolling, fit/zoom and
   accessibility behavior provided by the app.

### Validation and limits

The underlying changes passed Android/JVM and multiplatform CI, browser SVG
comparisons and actual Canvas pixel checks. These checks do not substitute for
Android/iOS/HarmonyOS device visual acceptance in a consuming app.

Non-default Sankey/Packet configuration remains outside this change. Sankey
cyclic fallback is retained; sampled ribbon outlines are not pixel-identical to
upstream cubic strokes. Packet may expand columns for measured long labels.
The flow router chooses between existing detour candidates, not a globally
optimal route. Font measurement is approximate unless the host supplies a
`TextMeasurer`; full Markdown and JavaScript interaction/configuration support
are not included.

## 0.1.8 / 0.1.8-ohos

Previously published from
[`a23d753`](https://github.com/botiverse/mermaid-native/commit/a23d753944776887abb05f8bf34bedee294aea06).
This pair introduced the shared web-aligned palette and earlier branching and
decision-diamond corrections. It remains available for pinned consumers.

SDK publication updates library artifacts. It does not update an installed app:
the app must select the new dependencies, rebuild, test and release its own package.
