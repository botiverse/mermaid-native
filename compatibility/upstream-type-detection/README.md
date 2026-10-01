# Upstream basic type detection

Runs four unchanged utils.spec.ts assertions from pinned Mermaid: graph,
leading whitespace, leading whitespace/newline, and gitGraph. The other 52
utils tests are unselected (some are covered by separate harnesses).

Build `:mermaid-core:testDebugUnitTest` and run:

```sh
python3 compatibility/upstream-type-detection/run.py --upstream /path/to/mermaid --stdlib /path/to/kotlin-stdlib.jar
```

The Java/Python/JS bridge only transports source and the production enum ID.
`MermaidParser.detectType` and `parse` share the same production classifier.
Neither body parsing nor expected answers are implemented by the adapter.
Original test/source hashes are checked before the original and Native runs.

Wrap/init configuration and YAML frontmatter assertions are not selected. This
is a bounded Native family classifier, not parity with Mermaid's configurable
renderer registry. The original tests remain under Mermaid's MIT License;
see the repository NOTICE and compatibility/upstreams.lock.
