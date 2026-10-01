# Original font-size assertions

Replays the six unchanged `when parsing font sizes` assertions from pinned Mermaid `utils.spec.ts` through the production Kotlin `FontSize.parse` API. The same parser and resolver are consumed by Class/Flow and ER styles; the Class and ER syntax validators use it too. Other utilities in that file remain unselected and do not count as Native coverage.

The transport preserves number/string/undefined input categories; the unsupported object fixture becomes an opaque native object, which Kotlin rejects. The JS adapter only maps Native null results to JS undefined tuple entries. It neither parses numbers nor creates CSS unit strings.

Run after building the Android runtime JARs:

```sh
python3 compatibility/upstream-font-size/run.py --upstream /path/to/upstream-mermaid --stdlib /path/to/kotlin-stdlib.jar
```

The production API supports numbers and px/em/unitless strings. Resolution uses the existing 14px design base in Class/Flow and ER styles and preserves their 512px bound. Fractional sizes remain fractional; the upstream utility's integer truncation of untested fractional strings is deliberately not copied. Existing Flow percentage handling remains unchanged. This is coverage of six original assertions, not all CSS lengths or all utility behavior.
