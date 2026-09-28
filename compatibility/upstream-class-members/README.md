# Original class member display assertions

Pinned upstream `04ee3364045d6573f84034d3c9368cc50233a92f`. Build the Native core and layout debug runtimes, then run `run.py --upstream PATH --stdlib PATH`.

All 93 original `classTypes.spec.ts` assertions remain unchanged. Capture records constructor inputs; the Native pass replaces that JS class with getters backed by `ClassMemberFormatter`, the same typed formatter used by production class layout. CSS strings in the adapter are a mechanical projection of Native italic/underline flags. No upstream formatter supplies Native results. Baseline: 56/93 through the existing production class-label function with no decoration support.

These are Native member-formatting assertions, not new parser or renderer coverage. Real-entry layout/SVG/Canvas/Kuikly tests separately ensure the format and style reach consumers. Static members use shared measured underline commands; abstract members use italic text in SVG, browser Canvas and Kuikly Canvas. The consumed Kuikly core is `2.24.0-raft.1-2.1.21`; its `FontStyle.ITALIC` was verified in the resolved artifact.

Nested and multiple generic parameters, visibility, parameters/return types and classifiers follow the upstream display contract. Arbitrary HTML sanitization and JS object/CSS implementation identity remain unclaimed. No renderer geometry/color redesign is intended.
