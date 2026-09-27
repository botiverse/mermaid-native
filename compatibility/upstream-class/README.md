# Original upstream Class suite

Runs unchanged `classDiagram.spec.ts` from pinned Mermaid revision
`04ee3364045d6573f84034d3c9368cc50233a92f`. Source hashes cover the suite,
Jison grammar, ClassDB and member types. A source mismatch or failed original
assertion exits nonzero; snapshots and expectations are never rewritten.

```sh
python3 compatibility/upstream-class/run.py \
  --upstream /path/to/installed-and-built/upstream-mermaid \
  --stdlib /path/to/kotlin-stdlib-2.1.21.jar
```

Use Java, Python, pnpm and the Android Gradle environment. Upstream dependencies
must be installed from its lockfile and built with `pnpm exec tsx .esbuild/build.ts`.
`--skip-build` requires a fresh core runtime JAR. Reports, source calls, typed
Native models and exact JAR SHA-256 are written to `.native-class-audit/`.

The original parser reference passes 383 cases with one skip. The previous
Native implementation passed 23, failed 360 and skipped one. This branch's final
result is pending its frozen build. These numbers describe the adapter boundary,
not Native rendering equivalence. One case tests the upstream database directly
without invoking the parser and must not be counted as Native coverage.

The bridge serializes only Native typed data. The adapter rebuilds ClassDB from
that data and never calls the original parser for Native results. ClassDB still
formats raw member signatures and implements database/configuration behavior;
passing those assertions does not prove Native member typography or interaction.
Native layout tests separately exercise group containment, direction, measured
notes, visible generic/annotation headers and relationship marker endpoints.

Current implementation covers declarations, generics, quoted labels/identifiers,
unmarked members, annotations, namespace hierarchy, notes, direction, accessibility
and both relationship endpoints. CSS classes/styles now reach the Native layout. Callback/link declarations are
retained as typed host-facing interaction metadata; Native interactive host
bindings are not implemented in this batch. Static/abstract member typography, lollipop interface semantics,
advanced graph routing and rendering equivalence remain open work.
