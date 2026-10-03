# Direct Native Cynefin state and boundary geometry

Runs all 27 unchanged tests in pinned `cynefin.spec.ts` against production `CynefinDocument` and `CynefinBoundaries`. The original run captures direct operations and raw inputs. A JVM bridge executes Kotlin state updates and geometry; the adapter replays those results with exact operation/argument matching and an unused-operation check. It does not generate Mermaid source or compute Native geometry in JavaScript.

```sh
python3 compatibility/upstream-cynefin-boundaries/run.py \
  --upstream /path/to/pinned/mermaid \
  --stdlib /path/to/kotlin-stdlib.jar \
  --verify-mutations
```

Build `:mermaid-core:bundleLibRuntimeToJarDebug` and `:mermaid-layout-api:bundleLibRuntimeToJarDebug` first. The runner verifies the original test and its imported copy, plus the upstream DB and boundary algorithm hashes. It uses upstream pnpm/Vitest dependencies and writes results under `.native-cynefin-boundaries-audit`. Six mutation controls check empty state, self-loop filtering, cubic paths, ellipse radii, explicit seeds and random range. All 32 executed pure geometry results additionally match the original numerically to absolute tolerance `1e-10`, independently of the original shape/determinism assertions.

## Production scope

`CynefinDocument` merges domain updates, replaces transitions, ignores null updates, filters self-loops and resets state. Input/output collections are detached. `diagram()` feeds the existing Native layout. Configuration exposes pinned defaults; it does not implement browser global configuration overrides.

`CynefinBoundaries` ports Mermaid's MIT-licensed seeded random, hash, seed resolution and cubic/ellipse geometry. The actual Native layout consumes the same cubic structures, sampled at 24 steps per segment, and the same ellipse geometry. It draws contiguous quadrant backgrounds, dashed fold/horizontal boundaries, a red cliff and curved transitions. Integration tests check actual draw commands, seeds, labels, bounds and nondegenerate arrows.

Native retains an adaptive canvas and its existing text/theme styling. This is not complete original renderer, CSS, badge or browser configuration parity. Sampling approximates cubic curves; the exported SVG path helper retains exact control points. No new SDK is published by this change.

Seven DB assertions were already covered by `upstream-cynefin-model`; only 20 occurrences are new coverage candidates (four DB assertions and 16 geometry/seed assertions). Do not double count the older seven.
