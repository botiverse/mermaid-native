# SDK releases and upgrading

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
