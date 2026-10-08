# Original Block arrow geometry assertions

This harness executes the **unchanged** `blockArrow.spec.ts` at the revision and
SHA-256 in `sources.json`: 16 direction subsets, axis expansion/deduplication,
and natural width (18 tests). It captures calls and reference outputs, runs the
same inputs through the production Kotlin `BlockArrowGeometry`, compares all
coordinates, and replays the Kotlin results through the original expectations.
Java and TypeScript adapters contain transport only. Mutation controls perturb
coordinates, axis expansion and natural-width results and require failing tests.

Run after building the core/layout Android runtime JARs:

```sh
python3 compatibility/upstream-block-arrow/run.py \
  --upstream /path/to/pinned/mermaid \
  --stdlib /path/to/kotlin-stdlib-2.1.21.jar --verify-mutations
```

Native common tests separately verify production parsing, natural and spanning
width selection, nested containment, four-way tip bounds, styles and polygon
edge intersections (including a circle at the other endpoint). The Kuikly test
feeds a real parsed/layout scene into the Canvas adapter. This is shape geometry
compatibility, **not** parity with Mermaid's entire Block grid layout. Native
uses the upstream default node padding of 8; frontmatter `block.padding` remains
unsupported and receives the existing explicit diagnostic.

Upstream three-direction and corner factories include triangles/trapezoids;
these are deliberately retained, not replaced with inferred arrow designs.
Mermaid source is MIT licensed, copyright Knut Sveidqvist and contributors;
see `../upstream-batch/UPSTREAM-LICENSE` and the module's packaged MIT notice.
