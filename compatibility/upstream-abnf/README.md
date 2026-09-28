# Original ABNF consumer assertions

Pinned upstream `04ee3364045d6573f84034d3c9368cc50233a92f`. Build Native core, then run `run.py --upstream PATH --stdlib PATH`.

The 12 unchanged `abnfDiagram.spec.ts` tests consume Native `MermaidParser.parse` and typed Railroad model values; no upstream parser supplies Native results. Baseline is 2/12 (only error-rejection tests pass).

The parser supports double-quoted strings, numeric terminals, rule references, concatenation/alternation, groups, optional groups and prefix repetitions. Bounded/exact repeats retain typed minimum/maximum values and a visible count annotation in the existing Railroad renderer; zero/one-or-more retain existing geometry. Reversed/overflowing bounds fail closed. Full Langium lexer/diagnostic positions, arbitrary YAML configuration and RFC ABNF dialect completeness remain unclaimed.
