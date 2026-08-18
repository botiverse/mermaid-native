# OHOS adapter gate

The public KMP core and render artifacts remain independent of the signed OHOS
toolchain. `mermaid-kuikly` is the host adapter boundary; the Raft Mobile
Kuikly build enables its `ohosArm64` target and consumes the exact Maven version.

Before an artifact version is marked OHOS-compatible, CI must compile the
adapter with the pinned Kuikly/OHOS toolchain and render the Phase 0 fixtures on
the approved OHOS host. No source-inclusion fallback is allowed.
