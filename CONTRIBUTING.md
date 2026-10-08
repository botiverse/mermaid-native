# Contributing to Mermaid Native

Contributions to parsers, layout, renderer adapters, examples and documentation are welcome. This is an independent Mermaid-compatible implementation. Please follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## Report a bug or propose a feature

Open a [GitHub issue](https://github.com/botiverse/mermaid-native/issues) with a minimal diagram, expected and actual output, SDK version or commit, platform, and renderer. For layout problems, include the viewport size, font/measurer and a screenshot when possible. Remove credentials and private information from diagrams and logs. Report security vulnerabilities privately as described in [SECURITY.md](SECURITY.md).

Check the [support matrix](docs-site/reference/families.md) first. A family may be supported while a particular syntax, option or interaction is not. Discuss large architectural changes in an issue before implementing them.

## Development setup

- JDK 17 and Android SDK 35 for the normal Gradle build; configure `ANDROID_HOME` or your local SDK path.
- macOS and Xcode for iOS compilation and simulator tests.
- A supported browser for Kotlin/Wasm browser tests. Gradle manages its JavaScript dependencies.
- Node.js and npm for the documentation site (`npm ci` in `docs-site`). CI currently builds docs with Node 20 and runs Wasm verification with Node 24.
- HarmonyOS uses the separate `settings.ohos.gradle.kts` build and the KBA toolchain described in the [integration guide](docs-site/guide/getting-started.md). It is not required for a documentation-only contribution.

```bash
git clone https://github.com/botiverse/mermaid-native.git
cd mermaid-native
./gradlew :mermaid-core:testDebugUnitTest :mermaid-layout-simple:testDebugUnitTest
```

Useful focused checks:

```bash
# SVG serialization
./gradlew :mermaid-render-svg:testDebugUnitTest

# macOS / Xcode required
./gradlew :mermaid-core:iosSimulatorArm64Test :mermaid-layout-simple:iosSimulatorArm64Test

# Browser required
./gradlew :mermaid-web:wasmJsBrowserTest

# Documentation
cd docs-site
npm ci
npm run docs:build
```

Use the repository's pinned dependencies. The Kuikly adapter currently builds against the Raft distribution; changes must not silently claim compatibility with an untested upstream distribution. The full CI workflow additionally checks common metadata, other targets, optimized Wasm behavior and third-party notices. Run relevant checks locally and state any checks you could not run.

## Prepare a pull request

1. Keep the change focused. Put reusable diagram behavior in mermaid-native and application UI behavior in the consuming application.
2. For new syntax or configuration, update the feature matrix and add accepted and rejected examples. Unsupported input should return diagnostics.
3. Test observable behavior. Parser/model assertions, layout geometry, visual rendering and host interactions are separate forms of evidence.
4. For upstream compatibility work, preserve pinned sources and original assertions. Document which Native boundary is tested and which upstream behavior remains outside it. Do not count skipped or reference-only tests as Native coverage.
5. Update public documentation when behavior or integration changes. Distinguish released capabilities from main-only changes.
6. Preserve third-party attribution in `LICENSE`, `NOTICE` and module resources; identify any new dependency's license.
7. Run `git diff --check`, use a descriptive title, and include the problem, resulting behavior, compatibility impact and validation in the PR description.

Sign commits with `git commit -s` using your contributor identity. The sign-off records your [Developer Certificate of Origin](https://developercertificate.org/) certification.

Maintainers review changes before merging. Do not add DOM/WebView dependencies to common diagram code or describe partial feature support as complete Mermaid parity. This project does not currently promise binary compatibility across 0.x releases.
