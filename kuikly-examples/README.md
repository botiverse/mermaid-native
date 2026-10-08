# Mermaid Native with official Kuikly

A standalone integration example. This directory has its own Gradle build and does not include or modify a Kuikly fork or the Mermaid Native source tree.

- Kotlin: **2.1.21**
- Official Kuikly core, KSP and Android renderer: **2.28.0-2.1.21**, from Tencent Maven
- Mermaid Native: published **0.1.11**, from the public Mermaid Maven repository
- `shared`: Kuikly DSL page, parsing/layout and `MermaidView` in commonMain; Android and iOS targets
- `androidApp`: minimal native host; no WebView

## Dependency boundary

Mermaid 0.1.11 was published against `2.24.0-raft.1-2.1.21`. The shared module explicitly excludes that transitive Kuikly dependency and supplies the official version instead. This is a consumer-side compatibility experiment; it does not change the published POM or claim that all Kuikly versions/platforms are interchangeable. The Mermaid repository is restricted to the `build.raft.mermaid` group, so it cannot supply Raft Kuikly artifacts here.

## Build

Use JDK 17 or newer, Android SDK 35, and set `ANDROID_HOME`. Gradle pins JVM bytecode to 17. macOS and Xcode are required for iOS compilation.

```sh
./gradlew :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64 :shared:compileKotlinIosArm64
./gradlew :androidApp:dependencies --configuration debugRuntimeClasspath
```

Install only on a device you own or have permission to use:

```sh
adb -s YOUR_DEVICE install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb -s YOUR_DEVICE shell am start -n dev.mermaid.examples.officialkuikly/dev.mermaid.examples.MainActivity
```

Select a sample with `--es sample flow`, `treemap`, `shapes` or `sequence` on the `am start` command (use `-S` to restart between cases).

The sample shows a diagram inside the official Kuikly Canvas. There is no custom diagram drawing in the host: parsing, model, layout and rendering all come from Mermaid Native. The Android host supplies the view lifecycle; the page supplies application presentation.

## Verification

Android API 34 emulator rendering passed for flowchart, expanded shapes, Treemap with title, and sequence samples. Android APK and iOS arm64/simulator shared code compile. See `VALIDATION.md` for evidence and limits. iOS shared-source compilation does not prove native renderer linking or on-device display. No HarmonyOS/H5/mini-program result is implied.
