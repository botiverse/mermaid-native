# Original Event Modeling grammar and validator assertions

All 31 assertions in pinned `packages/parser/tests/eventmodeling.test.ts` run unchanged. Five parser calls project the real `EventModelingDiagram`; 25 validator calls use the production `EventModelingValidator.errors` which is also consumed by actual explicit frame relations. The single Langium registry wiring assertion remains original JS and is NOT Native coverage. Baseline is 15/31 including that one JS-only assertion.

The adapter preserves each original argument. It projects model lists and lexical source types; it neither invokes the original parser/validator for Native cases nor manufactures their expected values. Direct validation uses the same Native rule implementation as the production parser. When measuring the old baseline without this rule API, the bridge constructs equivalent explicit frame relations and calls the existing Native parser, whose lack of source-type checking is exposed by failing assertions.

Notes and scenarios retain their source frame identifiers and inert text and reach actual layout cards. Model-entity names are retained; full linked Langium references, CST/source locations, arbitrary embedded-data rendering and YAML configuration are not claimed. Original lexer/parser checks are admission and typed-model boundary coverage, not full Langium API equivalence.
