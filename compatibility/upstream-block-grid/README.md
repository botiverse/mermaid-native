# Measured Block grid compatibility

Replays both unchanged original tests in `diagrams/block/layout.spec.ts` against production `BlockGridLayout.position/layout`. A separate supplemental test covers 96 measured trees across columns -1/1/2/3 and padding 0/4/8/20, with spans, spaces, unequal leaf sizes, two-level composites, empty trees and missing roots. It compares every resulting node center/size and bounds with the pinned reference before replay. The supplemental case is not an original test and must not inflate the coverage ledger.

The Java bridge transports measured tree data only. It does not calculate geometry. The Vitest adapter consumes recorded Native results, restores the actual mutated tree, and rejects extra/missing/different calls. Mutation controls corrupt positions, padding-sensitive bounds and nested sizes; the suite must detect each corruption. This is measured-grid parity, not upstream DOM measurement or full rendering parity. Native's production renderer reserves an optional composite heading band (zero in the reference harness) and retains its own typography. The layout enforces a finite, nonnegative padding and bounded recursion/work contract.

Build the Android runtime jars, then run:

```sh
python3 compatibility/upstream-block-grid/run.py --upstream /path/to/upstream-mermaid --stdlib /path/to/kotlin-stdlib.jar --verify-mutations
```

Evidence is saved under upstream `.native-block-grid-audit`, including checked source hashes, reference calls, Native outputs, reports and runtime/source hashes. Production integration is separately exercised by common `BlockGridLayoutTest`, existing Block shape/arrow tests and Kuikly recording tests.
