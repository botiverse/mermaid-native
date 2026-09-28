# Original Usecase ordered JSON assertions

Run the unchanged pinned `usecaseJson.spec.ts` (11 assertions) with `run.py --upstream PATH --stdlib PATH`.
The source hash is checked before execution. The capture pass records original JSON text and start coordinates; the Native bridge calls `UsecaseJsonParser.parseOrderedJsonObject`. The JS adapter only forwards values and wraps Native errors in the original error class so identity assertions remain meaningful. It does not parse, validate, reorder, or calculate source locations.

The same Native parser is used by `UsecaseParser.parseValidated`, the real Mermaid production entry. The grammar-only entry remains distinct. Production rejects malformed JSON and retains typed values plus escaped JSON Pointer property order. Duplicate properties keep the first key position and the last value, including replacement of nested order records.

The baseline uses the old real production entry and exposes its raw JSON source or diagnostics; it has no ordered JSON boundary. Baseline failures therefore are not eleven product bugs. Production previously admitted balanced but malformed JSON, which is separately protected by real-entry tests.

The existing Usecase JSON card still displays inert source text. This change does not claim the upstream normalized Usecase AST, transactional JS database API, or complete JSON table visual parity.
