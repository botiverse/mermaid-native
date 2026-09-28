# Original GitGraph syntax assertions

Runs the 27 assertions in pinned `packages/parser/tests/gitGraph.test.ts` unchanged. The official pass captures exact source inputs and assertion names. A production Native JAR calls `GitGraphSyntaxParser.parse`; the Java bridge projects only its typed statement properties, optional values, metadata and diagnostics. The adapter returns those results without parsing or resolving any Git history in JavaScript.

`GitGraphParser` consumes this same syntax result before resolving branches and commits. Syntax diagnostics prevent rendering; a partial statement tree is exposed for diagnostic consumers but does not admit malformed input into production. Likewise a syntactically valid checkout or merge can still fail history validation. Multiple commands on one source line now reach the normal production resolver.

The pre-change baseline has no statement tree: its adapter exposes existing production metadata and diagnostics, with no reconstructed statements. Thus its 4/27 result records missing syntax observability, not 23 broken render cases. The existing 74-case GitGraph consumer suite (including four skips and five JS-only configuration cases) is replayed separately to protect history behavior. These tests do not claim complete Langium CST/reference/recovery semantics or exact diagnostic spans.

Run `python3 compatibility/upstream-git-syntax/run.py --upstream /path/to/pinned/mermaid --stdlib /path/to/kotlin-stdlib-2.1.21.jar` after building the debug core JAR. `--repo` selects a previously built baseline checkout. Results and JAR SHA-256 are written to `.native-git-syntax-audit/` in the upstream checkout.
