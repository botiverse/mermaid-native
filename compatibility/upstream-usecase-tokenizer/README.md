# Direct Native Usecase tokenizer

Runs the complete unchanged `usecase.lexer.spec.ts` against production Kotlin `UsecaseLexer`. Raw strings go to a JVM bridge; Native returns token names, original images, inclusive UTF-16 offsets, line/column locations and recoverable error spans. The JavaScript adapter only reconstructs `tokenType.name` for the original assertions. It neither tokenizes nor falls back to the reference lexer.

```sh
python3 compatibility/upstream-usecase-tokenizer/run.py \
  --upstream /path/to/pinned/mermaid \
  --stdlib /path/to/kotlin-stdlib.jar \
  --verify-mutations
```

Build `:mermaid-core:bundleLibRuntimeToJarDebug` first. The runner verifies upstream/imported original test hashes and lexer/token vocabulary dependency hashes. All executed original token streams are compared exactly to the reference, including source positions beyond what the assertions inspect. A separate edge corpus checks accessibility blocks, Unicode, CR/LF/CRLF, lexical recovery, CSS words, malformed stereotypes and long unclosed input; those extra cases are not original assertion coverage.

Six mutation controls must each fail the corresponding original test: operator precedence, keyword separation, physical newline count, JSON source column, unclosed Markdown classification and decimal classification. Reports and raw traces are written under `.native-usecase-tokenizer-audit` in the upstream checkout.

## Production scope

The tokenizer implements the full pinned vocabulary and its default, JSON-body and stereotype modes. Long unterminated Markdown/JSON is consumed once. Stereotype scans stop at physical newlines to avoid rescanning the remaining document for each malformed declaration. Tokens and errors are returned as detached snapshots for editor clients.

The actual `UsecaseParser` uses this tokenizer to identify unexpected tokens in grammar diagnostics. For example, an unexpected quoted string is reported as the complete string rather than its opening quote. Existing valid diagram parsing remains in the established parser; this does not replace its entire grammar with a token-based parser. Chevrotain token categories, internal token indices, object identity and its formatted error strings are not part of the Native API.

This batch contributes 13 new original lexer assertions. It does not resolve the six remaining parser/AST failures or imply complete Usecase rendering parity. No SDK is published.
