# Batch import of remaining original assertions

This directory imports the complete original source files for **1,519 unresolved assertion occurrences in 159 files**, pinned to Mermaid `04ee3364045d6573f84034d3c9368cc50233a92f`. The immutable manifest records each `(file, full test name, occurrence)` and its baseline coverage status at Native `201bb2e1279cad33513b17da9d2af5bf1f96fd72`. Repeated names and upstream project occurrences are preserved, not deduplicated.

The imported sources retain their original assertions, fixtures, skips and license. A single runner verifies SHA-256 of both imported and upstream files, then loads the imported bytes under the original module IDs. Relative dependencies, mocks, snapshots and inline tests resolve against a supplied upstream checkout/archive with its dependencies installed. This is not a standalone copy of upstream dependencies.

| Batch | Unresolved occurrences |
|---|---:|
| Model and parser | 192 |
| Layout and routing | 345 |
| Rendering | 296 |
| API, configuration and utilities | 270 |
| Styles and themes | 241 |
| Documentation and tooling | 170 |
| Examples | 5 |

## Running the imported tests

```sh
python3 compatibility/upstream-batch/run.py \
  --upstream /path/to/pinned-mermaid --output /path/to/results --verify-only

python3 compatibility/upstream-batch/run.py \
  --upstream /path/to/pinned-mermaid --output /path/to/results \
  --group diagram-model-and-parser
```

CI verifies imported hashes without fetching upstream: `python3 compatibility/upstream-batch/run.py --verify-only --output build/upstream-batch`. A deliberate source-tamper check confirms that changed imported bytes are rejected.

Omit `--group` to run all batches; repeat it for several batches. No test-name filter is used: complete files run, and the report reconciles just the selected unresolved occurrences. Every group writes raw Vitest results/logs, assertion-level status and a summary, including collection errors and timeouts. A failed group does not prevent later groups from running. Documentation runs in the original main/documentation project contexts; its script tests have a separate package-root project for their working-directory contract. Projects execute separately so duplicate original test occurrences remain distinguishable in raw results. The original Architecture test's optional assignment uses the same TypeScript ES2018 transform as its existing harness. The runner uses UTC; upstream timezone-dependent skips remain explicit.

## Coverage boundary

**Import is not Native execution.** Reference results are saved under `referenceRun`; all newly imported occurrences remain `nativeRun: not_executed` and `integrationStatus: interface_not_integrated`. The runner neither changes the coverage ledger nor counts reference JavaScript passes as Native passes. These are retained interface gaps, not silently skipped Native failures.

Production adapters can now be added per whole module against this stable imported baseline. Existing parser-boundary adapters do not establish mutable database, full AST, configuration, DOM/SVG or renderer contracts. Such assertions must execute the corresponding production Native surface before their coverage status changes. The import does not remove any assertion from the global denominator.

## Validated baseline

All seven batches collected all 1,519 unresolved occurrences: 1,513 reference passes and six original skips, matching the original ledger exactly. No collection gaps or failures. Native execution remains pending for all 1,519. See validation.json and model-contracts.json.
