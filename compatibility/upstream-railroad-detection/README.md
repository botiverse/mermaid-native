# Original Railroad detector assertions

This harness selects 21 unchanged detection assertions from the pinned upstream
Railroad, ABNF, EBNF and PEG detector suites. Each detector input is transported
to production Kotlin `RailroadSyntax.matchesSource`; Java and JavaScript only
serialize inputs and boolean results. The main Mermaid parser uses the same
methods through `RailroadSyntax.detect` to select the grammar parser.

Four tests of JavaScript export IDs and seven dynamic loader tests are unselected.
They do not establish Native parser or renderer coverage.

```sh
python3 compatibility/upstream-railroad-detection/run.py --upstream /path/to/upstream-mermaid --stdlib /path/to/kotlin-stdlib.jar
```

Build the Android core runtime JAR first. `sources.json` pins the unchanged
upstream tests and implementation files. The detector accepts leading blank
lines, whitespace and case variants but requires the entire header line to be
one recognized keyword. Unlike upstream's unbounded prefix regex, it rejects
`railroad-beta-extra`. Existing Native header validation is preserved. Detection
does not validate the grammar body or remove comments; the main parser applies
its normal preprocessing before dispatch.
