# Original EBNF consumer assertions

Pinned upstream `04ee3364045d6573f84034d3c9368cc50233a92f`. Build the Native core debug runtime, then run `run.py --upstream PATH --stdlib PATH`.

The original 14 `ebnfDiagram.spec.ts` tests execute unchanged against Native admission and typed Railroad model values. The adapter only exposes those values through the original DB getter interface; no upstream parser supplies Native results. Baseline is 2/14 (the two rejection assertions), with all 12 valid-input assertions failing.

The new parser lowers EBNF terminal/nonterminal, sequence/choice, optional/repetition, groups, special sequences and exception notation into the existing Railroad model. That existing production renderer remains shared; EBNF introduces no new drawing implementation. This covers the original EBNF consumer file, not PEG/ABNF or all Langium lexer/token semantics. Frontmatter and exact Langium error locations remain unclaimed.
