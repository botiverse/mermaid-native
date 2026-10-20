# SDK releases and upgrading

## 0.1.13 / 0.1.13-ohos

**Pending publication** from tag `v0.1.13`. Until the release receipt is added, use 0.1.12.

Changes since 0.1.12:

- Compose Multiplatform (`mermaid-compose`): `MermaidCanvas` takes an optional `textStyle` as the base style for scene text. A `fontFamily` in that style replaces the scene's font family. The canvas now sizes itself to the scene size × `scale` in dp. It also clamps that scale with the new public `mermaidSafeScale` so neither side exceeds `MermaidMaxSidePx` (32,000 px), and it uses the scene's accessibility title or description as its content description. Text is measured once per scene, measurer and style, not on every frame. `measureMermaidScene` and `drawMermaidScene(measured, scale)` give custom `DrawScope` hosts the same caching, and `drawMermaidScene(scene, textMeasurer, scale, textStyle)` gains the optional style.
- Compose behavior changes: `MermaidCanvas` treats scene units as dp instead of pixels, so diagrams are drawn larger on high-density screens. Text is measured in scene units, so its size now matches the shapes at every density. Labels have a line height of 1.2 × font size and never wrap. Colors that are malformed or not hex now mean "do not paint", like `none`; before, they drew black or, for invalid hex digits, threw. A polygon whose fill is `none` still draws its gradient.

Upgrade all normal modules together to `0.1.13` and HarmonyOS modules to `0.1.13-ohos`. Hosts that size `MermaidCanvas` themselves keep their size; check text size and canvas size if you relied on pixel units.

## 0.1.12 / 0.1.12-ohos

**Available.** Published by [release workflow 39235279325](https://github.com/botiverse/mermaid-native/actions/runs/39235279325) from tag [`v0.1.12`](https://github.com/botiverse/mermaid-native/tree/v0.1.12) and resolved from the public Maven repository by the Raft mobile app. No readback receipt was recorded for this release.

Changes since 0.1.11:

- Platforms: `mermaid-core`, `mermaid-layout-api`, `mermaid-layout-simple`, `mermaid-render-svg` and `mermaid-testkit` now publish for every official Kotlin Multiplatform target: Android, iOS, macOS, JVM, JS, Wasm, Linux and Windows. The Kuikly modules keep their existing targets.
- Compose Multiplatform: the new `mermaid-compose` module draws a scene with JetBrains Compose Multiplatform 1.8 on Android, iOS, desktop JVM and Wasm (`MermaidCanvas`, `drawMermaidScene`). `mermaid-kuikly-compose` draws with the Kuikly Compose distribution, including OHOS.
- Kotlin/JS correctness: regex patterns with unescaped `]` or `}` no longer fail under the JavaScript `u` flag, and whole-input checks no longer depend on Kotlin/JS `Regex.matches`. Wardley, Venn, quadrant, kanban, mindmap, event modeling, architecture and usecase diagrams parse on JS, and quadrant/XY colors validate there.
- Rendering: typed stroke paths (moves, lines, quadratic curves and circular arcs) across SVG, browser Canvas and Kuikly. Custom renderers must handle the new draw command.
- Flowcharts: materialized flow geometry with atomic rendered-layout guards.
- Swimlanes: configurable arc/gap crossing hops (set `config.swimlane.lineHops: false` to keep straight crossings), unified geometry for styled and nested lanes, and handoffs routed around nodes and lane titles.
- Block diagrams: upstream multi-direction arrow polygons (axis shorthands, natural and spanning widths, four-way tip bounds, painted-boundary edge clipping) and upstream measured-tree grid sizing. `config.block.padding` (default 8) is read per diagram. Very deep grids are rejected with a bounded-work diagnostic. Native keeps its own text measurement, so this is not DOM typography or pixel parity.

Upgrade all normal modules together to `0.1.12` and HarmonyOS modules to `0.1.12-ohos`. Rebuild host code and custom scene renderers, because binary compatibility is not promised in 0.x.

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

Parsing tests are not visual parity. Native still lacks some original Mermaid behavior, small Treemap cells may have cramped/overflowing labels, and downstream Android/iOS/OHOS app/device acceptance remains required. Kotlin/Wasm is not an H5 or WeChat mini-program Kuikly adapter. The Kuikly artifact was built against the Raft distribution. The [standalone example](https://github.com/botiverse/mermaid-native/tree/main/kuikly-examples) independently verifies official Kuikly 2.28.0 with 0.1.11 on Android by excluding that transitive core and supplying the official dependency. iOS shared code compiles; native linking and rendering remain unverified.

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
