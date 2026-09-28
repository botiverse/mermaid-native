# Original Requirement parser assertions

Runs all43 unchanged upstream Requirement parser assertions at pinned revision04ee3364045d6573f84034d3c9368cc50233a92f against the original parser and then the real Kotlin parser (39 unique inputs). Source hashes are verified. The Java bridge reads Native definitions, optional fields, relationship kinds and directions, styles and classes. The JavaScript adapter only projects this typed model into the original RequirementDB; original assertions remain unchanged.

These are parser-boundary checks. Upstream class expansion and DB behavior are not Native renderer coverage. Actual direction, measured styles, cards and self-loop bounds have separate Native layout tests and production browser checks.

Run `python3 compatibility/upstream-requirement/run.py --upstream /path/to/pinned/mermaid --stdlib /path/to/kotlin-stdlib-2.1.21.jar`. Pass `--skip-build` only after building the desired source. The runner snapshots and hashes its JAR, and writes reports under the upstream checkout's `.native-requirement-audit` directory.
