# Parser publication file

The six previously failing assertions are addressed by the semantic/AST batch; the full file passes 77/77 locally. Confirm current results in `validation.json` and rerun the complete harness when changing the parser. No original assertion is rewritten, skipped or replaced.

The separate DB, rendering, layout and other grammar suites remain independent work. This file only describes `usecase.parser.spec.ts`; it does not declare complete Usecase parity.
