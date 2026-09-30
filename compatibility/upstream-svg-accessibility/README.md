# Original SVG accessibility assertions

Nineteen unchanged assertions from pinned `packages/mermaid/src/accessibility.spec.ts` inspect actual SVG produced by `SvgRenderer`. They cover the graphics/document role, optional diagram role description, title/description text, scoped IDs and ARIA references, and absent metadata. The twentieth assertion only tests a D3 mock without an `insert` method; it remains unselected and does not count as Native coverage.

The runner captures original calls, passes the supplied metadata to a real `LayoutScene` and the production Kotlin SVG renderer, then imports the returned XML into the original test's DOM. The JS adapter does not synthesize SVG attributes, IDs, or child elements. For before/after evidence the Java bridge can fall back to the old one-argument renderer when the scoped overload does not exist; it never fills in missing behavior itself.

Run after building the Android runtime JARs (the runner never launches Gradle):

```sh
python3 compatibility/upstream-svg-accessibility/run.py \
  --upstream /path/to/pinned/upstream-mermaid \
  --stdlib /path/to/kotlin-stdlib.jar
```

`--runtime-repo` may point to a separately built baseline checkout. Results and the SHA-256 of all three production runtime JARs are written to `.native-svg-accessibility-audit` inside the upstream checkout. Keep those hashes and the source revision with archived results.

The production renderer keeps `render(scene)` for standalone exports and offers `render(scene, baseId, diagramType)` for hosts composing inline SVGs. Such hosts should supply a distinct `baseId` per rendered instance. The default IDs are deterministic and derive from accessible text; they are not globally unique instance IDs. The web sanitizer only accepts metadata IDs of the form `chart-title-<baseId>` / `chart-desc-<baseId>` with ASCII letters, digits, `_`, `.`, `:`, or `-` in the base ID, and requires references to resolve inside the same SVG. Its existing active-content restrictions remain in force.
