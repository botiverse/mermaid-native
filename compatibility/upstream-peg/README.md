# Original PEG consumer assertions

Pinned upstream `04ee3364045d6573f84034d3c9368cc50233a92f`. Build Native core, then run `run.py --upstream PATH --stdlib PATH`.

The 14 original `pegDiagram.spec.ts` tests remain unchanged and consume Native `MermaidParser.parse` output through typed Railroad model getters. No upstream parser supplies Native results. Baseline: 2/14 (only rejection tests pass).

PEG ordered choice, sequences, literals/references, groups, suffix optional/repetition, any-character and lookahead predicates lower into the existing Railroad renderer. Predicates use the upstream consumer's special-node labels. No duplicate renderer or CSS change is introduced. ABNF, full Langium token/error-location contracts and arbitrary frontmatter/configuration remain unclaimed.
