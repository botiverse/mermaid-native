# Original Railroad grammar and consumer assertions

Pinned upstream revision: `04ee3364045d6573f84034d3c9368cc50233a92f`.
`run.py --upstream PATH --stdlib PATH` verifies the original spec hashes and runs all 7 grammar plus 26 product-consumer assertions unchanged against an already-built Native runtime JAR.

The bridge calls the real `MermaidParser`, projects typed Railroad nodes and accessibility fields, and replaces the original parser/database entry points. The grammar adapter maps typed nodes to the asserted Langium shape; the product adapter exposes the Native tree directly. Native failures are wrapped in the original `MermaidParseError` class for the async API contract assertion. The adapter does not claim native exception-class identity or run the original parser as a fallback. Baseline was 28/33; missing metadata, block comments, empty grammar and single-choice normalization account for five failures.

This is parser-boundary evidence, not native layout parity. The Native renderer keeps its existing tracks and labels. ABNF, EBNF and PEG dialects, complete Langium CST/reflection state, frontmatter/directive semantics and unasserted lexical edge cases are not covered by these suites.

Native normalizes single-element sequence/choice nodes in its product parser, while the original Langium grammar retains wrappers until its product transform. The grammar adapter projects the normalized tree; unasserted CST structure is not equivalent. Follow-up regressions also exercise strict argument separators, repeated/empty title lines, unknown string escapes, and metadata-named rules. Bridge exceptions always produce an error result, including exceptions without a message.
