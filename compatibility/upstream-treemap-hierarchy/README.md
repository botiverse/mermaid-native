# Upstream Treemap hierarchy assertions

Runs all four unchanged `utils.test.ts` tests pinned in `sources.json` against
production `TreemapHierarchy`. `TreemapParser` uses the same incremental builder.
Build the Android runtime JAR first, then run:

```sh
python3 compatibility/upstream-treemap-hierarchy/run.py \
  --upstream /path/to/upstream-mermaid --stdlib /path/to/kotlin-stdlib.jar
```

The official run captures flat input rows. Python transports their fields to
Java, which calls the production Kotlin builder and serializes the resulting
nested nodes. The Native run replaces only `buildHierarchy`; original test
inputs and expected results remain unchanged. No Java/JS hierarchy algorithm is
substituted. Input hashes, calls, runtime digest and both Vitest reports are
written to the upstream checkout's `.native-treemap-hierarchy-audit` directory.

This tests valid hierarchy construction, not arbitrary upstream database or CSS
behavior. Native's parser still rejects children of leaves, negative indentation
in programmatic rows, and invalid parsed labels/weights. The transport rejects
unsupported fields, unknown row types, and leaves without a numeric value.
