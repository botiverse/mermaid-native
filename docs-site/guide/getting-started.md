# Client SDK integration

Mermaid Native parses Mermaid text into a typed model, lays it out into a shared
scene, and renders through Kuikly Canvas or SVG. The native path needs no WebView
or JavaScript runtime. Android and iOS consume the normal Kotlin Multiplatform
artifacts; HarmonyOS consumes a separate OHOS build of the same source.

## Version and prerequisites

The current SDK release is **0.1.10** for Android/iOS and **0.1.10-ohos** for HarmonyOS.
Publication and artifact verification are in progress. The dependency examples below
target the upcoming release; use the verified 0.1.9 pair until the release receipt
confirms availability. See the
[release notes](./releases) for the immutable source, validation and upgrade steps.

| Consumer | Compiler / platform | Kuikly core used by the adapter |
| --- | --- | --- |
| Android | Kotlin 2.1.21, minimum API 21; SDK built with compile SDK 35 | `2.24.0-raft.1-2.1.21` |
| iOS | Kotlin 2.1.21; `iosArm64`, `iosSimulatorArm64`, `iosX64` KLIBs | `2.24.0-raft.1-2.1.21` |
| HarmonyOS | Kotlin `2.0.21-KBA-010`, `ohosArm64` | `2.24.0-raft.1-2.0.21-ohos` |

JDK 17 is used for builds. The SDK supplies KMP libraries, not a standalone Swift
XCFramework, CocoaPod, or HarmonyOS HAR. Link it into your existing shared module
and continue using that host's framework/HAR packaging. A Kuikly host must already
have its matching native renderer installed; adding a Maven dependency alone does
not set up an Android/iOS/OHOS application.

## Add the Maven repository

Public artifact reads do not require credentials. Add this repository in
`settings.gradle.kts` alongside your existing Google/Maven Central repositories:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.artifacts.botiverse.dev")
            content {
                includeGroup("build.raft.mermaid")
                includeGroup("com.tencent.kuikly-open")
            }
        }
        google()
        mavenCentral()
    }
}
```

If your project already filters this repository to Kuikly only, add
`build.raft.mermaid` to its allowed groups. Keep the existing repositories and
filters required by the rest of your host SDK.

## Android and iOS dependencies

In your existing shared module's `build.gradle.kts`, use a single version for all
Mermaid modules. Gradle selects the Android or iOS variant automatically:

```kotlin
val mermaidVersion = "0.1.10"
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("build.raft.mermaid:mermaid-core:$mermaidVersion")
            implementation("build.raft.mermaid:mermaid-layout-simple:$mermaidVersion")
            implementation("build.raft.mermaid:mermaid-kuikly:$mermaidVersion")
            // Optional: export SVG without a Kuikly host.
            implementation("build.raft.mermaid:mermaid-render-svg:$mermaidVersion")
        }
    }
}
```

For an Android-only module, place the same dependencies inside `dependencies {}`.
Omit `mermaid-kuikly` when you only need parsing/layout/SVG. `mermaid-layout-api`
is provided transitively; add it explicitly if you depend directly on its API.

## HarmonyOS dependencies

Use the host's existing OHOS settings/build entry point and KBA compiler. In that
build only, select the OHOS version for **every** Mermaid dependency:

```kotlin
val mermaidVersion = "0.1.10-ohos"
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("build.raft.mermaid:mermaid-core:$mermaidVersion")
            implementation("build.raft.mermaid:mermaid-layout-simple:$mermaidVersion")
            implementation("build.raft.mermaid:mermaid-kuikly:$mermaidVersion")
        }
    }
}
```

The OHOS publication contains `mermaid-core`, `mermaid-layout-api`,
`mermaid-layout-simple`, and `mermaid-kuikly`. SVG, Web and testkit artifacts are
not published on this plane. Preserve your host's Tencent plugin repository and
KBA stdlib/coroutines/atomicfu repositories. Never put normal and OHOS versions
in the same dependency graph, or substitute a normal KLIB for an OHOS KLIB.

## Parse and lay out a diagram

This function can live in `commonMain`. Handle diagnostics in your UI rather than
assuming every string parses successfully:

```kotlin
import build.raft.mermaid.core.MermaidParser
import build.raft.mermaid.core.MermaidParseResult
import build.raft.mermaid.layout.LayoutConfig
import build.raft.mermaid.layout.LayoutScene
import build.raft.mermaid.layout.simple.FixedWidthTextMeasurer
import build.raft.mermaid.layout.simple.SimpleMermaidLayout

fun makeMermaidScene(source: String, onError: (String) -> Unit): LayoutScene? {
    return when (val result = MermaidParser.parse(source)) {
        is MermaidParseResult.Success -> SimpleMermaidLayout.layout(
            result.diagram,
            FixedWidthTextMeasurer,
            LayoutConfig(),
        )
        is MermaidParseResult.Failure -> {
            onError(result.diagnostics.joinToString("\n"))
            null
        }
    }
}
```

`FixedWidthTextMeasurer` is deterministic and approximate. For accurate host fonts,
provide a `TextMeasurer` that returns `SceneSize` for the supplied text and
`TextStyle`, in the same units/fonts used for drawing. Cache parsing/layout when
the source and layout settings are unchanged; do not rebuild the scene on every
paint. Text measurement that calls a platform UI API must follow that API's
thread requirements.

## Render in Kuikly

Inside an existing Kuikly DSL container, pass the prepared scene to `MermaidView`:

```kotlin
import build.raft.mermaid.kuikly.MermaidView
import build.raft.mermaid.layout.LayoutScene
import com.tencent.kuikly.core.base.ViewContainer

fun ViewContainer<*, *>.addMermaid(sceneToRender: LayoutScene, availableWidth: Float) {
    val fitScale = if (sceneToRender.width > 0.0) {
        (availableWidth / sceneToRender.width.toFloat()).coerceIn(0.01f, 1f)
    } else 1f
    MermaidView {
        attr {
            scene = sceneToRender
            scale = fitScale
            batchDraw = true
            width(sceneToRender.width.toFloat() * fitScale)
            height(sceneToRender.height.toFloat() * fitScale)
        }
    }
}
```

Reserve the scaled scene height in the parent layout. `MermaidView` draws a canvas;
it does not add scrolling, pan/zoom, selectable text, link handling, or an accessible
semantic tree. Supply those through your host UI. On narrow screens you can use a
scrollable viewport instead of reducing text to an unreadable size. The view is a
Kuikly DSL component, not a Jetpack Compose `@Composable` function.

For an existing Kuikly canvas, call
`MermaidKuiklyRenderer.render(scene, context, scale = 1f)` in its draw callback.
The renderer saves/restores the context and supports translucent Sankey gradients.

## Export SVG

```kotlin
import build.raft.mermaid.layout.LayoutScene
import build.raft.mermaid.render.svg.SvgRenderer

fun exportMermaidSvg(scene: LayoutScene): String =
    SvgRenderer.render(scene, baseId = "message-42", diagramType = "flowchart")
```

Use a distinct `baseId` per inline chart and the correct family for `diagramType`.
If your host sanitizes SVG, its allowlist must support inert local `defs`,
`linearGradient`, `stop`, and `fill="url(#...)"` references for Sankey. Do not
allow arbitrary external references or script/style content. The repository's
`acceptance/svg-sanitizer.js` shows the supported bounded format.
Kanban ticket links also require direct SVG `<a>` elements with absolute HTTP(S)
destinations, `target="_blank"`, `rel="noopener noreferrer"`, and a transparent
hit rectangle. The shared sanitizer validates this navigation format separately
from resource references; it still rejects executable schemes and external images.

## Confirm the version in the client

```kotlin
import build.raft.mermaid.core.MERMAID_NATIVE_VERSION

println("Mermaid Native: $MERMAID_NATIVE_VERSION")
```

Expect `0.1.10` on Android/iOS and `0.1.10-ohos` on HarmonyOS. Inspect the resolved
Gradle dependency graph as well, since the core version alone cannot prove every
renderer uses the same release. Rebuild the host's shared framework/HAR after a
KLIB upgrade. Follow [the upgrade checklist and release notes](./releases).

## Build from source

```bash
git clone https://github.com/botiverse/mermaid-native.git
cd mermaid-native
./gradlew check
```

The normal build includes seven modules: core, layout-api, layout-simple,
render-svg, kuikly, web and testkit. The separate OHOS build uses
`./gradlew -c settings.ohos.gradle.kts` with its four supported modules.

## Accessible SVG exports

`SvgRenderer.render(scene)` emits linked SVG title and description metadata when the layout scene provides accessible text. Empty metadata is omitted. The SVG declares the graphics-document role with a document fallback.

For several inline SVG instances on one page, pass a different `baseId` for each instance. The optional diagram type supplies a role description:

```kotlin
val svg = SvgRenderer.render(scene, baseId = "message-42", diagramType = "flowchart")
```

Use letters, digits, underscores, periods, colons, or hyphens in IDs consumed by the provided web sanitizer. Title and description references must resolve within the same SVG. The one-argument renderer derives deterministic IDs from the accessible text for standalone exports; it does not allocate globally unique instance IDs.

## Railroad grammar detection

`RailroadSyntax.detect(source)` identifies the Native, ABNF, EBNF or PEG Railroad
header without parsing its body. It returns null for an unknown header. Leading
blank lines and case variants are accepted; the whole header line must match.
`MermaidParser.parse` uses the same detector after preprocessing comments, then
runs the selected grammar parser. Detection alone does not guarantee valid
syntax, and the low-level detector does not remove comments or metadata.

## TreeView icons

TreeView supports native vector glyphs for `icon(file)` and `icon(folder)` (also
`icon(mermaid-treeview:file)` and `icon(mermaid-treeview:folder)`). `icon(none)`
hides the icon while retaining the tree's connector dot. Custom pack references
remain visible as fallback text; external Iconify packs are not downloaded.

```text
treeView-beta
/ icon(folder)
  src/ icon(folder) ## Source files
    main.kt icon(file)
```

Kotlin callers can configure automatic icons on the parsed model before layout:

```kotlin
val tree = (MermaidParser.parse(source) as MermaidParseResult.Success).diagram as TreeViewDiagram
val configured = tree.copy(iconConfig = TreeViewIconConfig(
    showIcons = true,
    filenameIcons = mapOf("Dockerfile" to "file"),
    extensionIcons = mapOf(".kt" to "file", ".tmp" to "none"),
))
val scene = SimpleMermaidLayout.layout(configured, FixedWidthTextMeasurer, LayoutConfig())
```

Explicit annotations win over automatic detection. Exact filename mappings win
over extension mappings; extensions are matched in lowercase, with or without
the leading dot. Built-in names always use the built-in pack. A `defaultIconPack`
qualifies other unprefixed references. These options can be set programmatically or in the supported YAML `config.treeView` mapping.

## Detect a diagram family before parsing

```kotlin
val type = MermaidParser.detectType("graph TD\nunfinished body")
check(type == MermaidDiagramType.FLOWCHART)
println(type.id) // flowchart
```

The detector identifies a supported Native family from the first header, using
exactly the same comment and whitespace handling as `MermaidParser.parse`.
A detected family does not validate the body or header options; call `parse`
to obtain the model or typed diagnostics. Empty or unknown headers return null.
It recognizes the leading YAML document header; parsing retains its typed metadata. Mermaid init directives are not applied.
Family IDs describe Native parser families, independent of upstream renderer
versions and layout engines; all Railroad dialects belong to `RAILROAD`.


## Document titles and supported settings

`MermaidParser.parse` retains leading YAML metadata in `Success.frontmatter` and applies explicitly supported settings to the diagram model. Use `SimpleMermaidLayout.layout(parsed, measurer, config)` with the complete `Success` value to render document titles for families that have no model title field. Existing model titles take precedence. Both Web SVG and Canvas use this overload.

Supported YAML settings are bounded: Sankey `showValues`, XY `showDataLabel`, TreeView icon selection, Kanban `ticketBaseUrl`, Treemap `valueFormat` (`","` or `"$0,0"`), and `themeVariables.xyChart.plotColorPalette` (comma-separated 3-, 6- or 8-digit hexadecimal colors). Unsupported settings and formats produce diagnostics. This is not general YAML, theme or D3 format compatibility.

Kanban replaces the first `#TICKET#` in an absolute HTTP(S) URL with the ticket text. Its rendered scene exposes `links`, containing the measured ticket rectangle, URL and label. SVG emits links; Canvas JSON includes the same link regions, and the Canvas demo supports pointer activation and accessible keyboard links. Native hosts must implement navigation using these regions and their own zoom/pan transform; the Kuikly drawing renderer does not navigate automatically. Document titles shift the regions together with the commands.

Flowchart metadata accepts `manual-input` / `sl-rect`, `docs` / `documents`, and `procs` / `st-rect` as distinct drawn shapes. Their labels and connector endpoints use the visible polygons. Their appearance remains Native and is not an exact copy of the upstream renderer.
