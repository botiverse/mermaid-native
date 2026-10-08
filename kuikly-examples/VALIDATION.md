# Official Kuikly integration validation

Validated 2026-10-09 with published Mermaid Native **0.1.11**, official Kuikly **2.28.0-2.1.21**, Kotlin **2.1.21**, AGP **8.13.2**, Gradle **8.13**. No framework or library source changes and no local/composite dependency substitution.

| Check | Result |
| --- | --- |
| Android APK assembly | PASS |
| iOS arm64 shared KLIB compilation | PASS |
| iOS simulator arm64 shared KLIB compilation | PASS |
| Android runtime dependency graph | Official core/core-android/core-render-android/core-annotations; no `-raft` Kuikly version |
| Android API 34 arm64 emulator, flowchart | Rendered boxes, decision, text and arrows |
| Android emulator, extended shapes | Rendered manual-input, cylinder, hexagon and stadium |
| Android emulator, Treemap | Rendered title, group and weighted rectangles |
| Android emulator, sequence | Rendered participants, messages and lifelines |
| iOS renderer/framework linking and runtime | NOT RUN |
| HarmonyOS, H5, WeChat mini-program | NOT RUN |

The screenshots were inspected. This verifies these four scenarios on one Android emulator, not pixel parity or complete platform compatibility. iOS compilation does not establish compatibility of the published Mermaid Kuikly KLIB's native link symbols with the official native renderer. A complete iOS host/link/run check remains necessary before promising iOS support.

The Android emulator experienced system UI/system-process ANRs during first boot, before sample validation. After dismissing them and reducing emulator resolution to 720×1280, the four app scenarios rendered. No emulator-performance claim is made; other agents' simulators were not modified.

APK SHA-256: `8ff4f65ac33d7437b8cc2b3d8b05d6e9e6344f0aa8eeb5777e324dad8be9b9fa`.

The published Mermaid adapter's Raft Kuikly dependency is excluded explicitly in `shared/build.gradle.kts`; official core is supplied directly. Tencent Maven provides Kuikly artifacts. The public Mermaid Maven repository is restricted to Mermaid's group. Consumers should reproduce this exact graph instead of assuming a version-string replacement proves compatibility.

Screenshots: [flowchart](screenshots/flow.png), [Treemap](screenshots/treemap.png), [expanded shapes](screenshots/shapes.png), [sequence](screenshots/sequence.png).
