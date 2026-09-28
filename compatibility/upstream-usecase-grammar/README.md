# Original Usecase canonical grammar assertions

Pinned upstream revision: `04ee3364045d6573f84034d3c9368cc50233a92f`.
Build `:mermaid-core:testDebugUnitTest`, then run `run.py --upstream PATH --stdlib PATH`.
The source hash is checked and the original 76 tests execute unchanged. Admission for all captured inputs comes from the real Native `UsecaseParser.parse`, also used by the product `MermaidParser` entry through `parseValidated`. No upstream lexer or parser implementation supplies Native admission results.

75 tests concern grammar admission; the remaining `getSerializedGastProductions` assertion introspects Chevrotain's JS rule names and stays explicitly JS-only. Its result must not count as Native coverage. Baseline is 40/75 Native grammar assertions (41/76 including that JS case). The adapter's `start` name is a grammar-root projection, not a claim to publish Chevrotain's full CST.

This increment covers line-oriented declarations, actor lists, boundaries, accessibility, notes, balanced JSON literals, metadata, stereotypes, class/style statements and relation markers. The separate original publication/AST suite and direct DB suite are not yet integrated: exact span/error text, full symbol collision semantics, JSON value validation, property-order publication and all semantic metadata constraints remain unclaimed. `parseValidated` currently checks missing note/style targets; grammar-only parsing deliberately allows unresolved references.

Native retains existing plain actor/ellipse/rectangle layout. Extended scenes render separated boundary bands, notes, JSON source cards, relation markers and bounded color styles. Arbitrary CSS, external actor icons, markdown typography, animation and official JSON-table geometry are not asserted. Existing `usecaseDiagram` is retained as an explicit Native alias. This is parser-boundary coverage, not full browser/rendering parity.
