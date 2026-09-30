# Original Unicode splitting and measured wrapping assertions

Replay 34 unchanged assertions from pinned `splitText.spec.ts` through production `UnicodeGraphemes` and `MermaidTextWrapping`. Both Usecase cards and JSON table cells consume this wrapping implementation. It preserves entire extended grapheme clusters, including surrogate pairs, combining marks and emoji ZWJ sequences, when a line reaches its measured width. If one cluster is wider than the available width, it stays whole and the wrapper advances.

The adapter replaces only `splitTextToChars` and `splitLineToFitWidth`. Original `splitLineToWords` constructs inputs and expected word tokens; Native word segmentation is **not** claimed. The pinned tests supply a monospace character-count fit callback. The adapter probes that callback to obtain the width constraint; production Kotlin performs segmentation, counting and wrapping. When the original fixture disables `Intl.Segmenter`, the bridge calls the production code-point fallback. The helper-only checkFit assertion remains unselected. No JS wrapping algorithm supplies Native results.

Run after building the production core JAR:

```sh
python3 compatibility/upstream-text-wrapping/run.py \
  --upstream <pinned checkout with dependencies> \
  --stdlib <kotlin-stdlib.jar> \
  --runtime-jar <production core jar>
```

The runner never launches Gradle. Preserve `.native-text-wrapping-audit` results, runtime SHA-256, and source revision. The Unicode conformance cases are a separate test suite, not additional Mermaid original assertions.
