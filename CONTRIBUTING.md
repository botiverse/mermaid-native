# Contributing

Mermaid Native is an independent, non-official open-source project. Keep pull
requests small and scoped to one compatibility or infrastructure seam.

Before opening a PR:

1. Add or update the compatibility matrix and pinned fixture when syntax is
   added.
2. Add positive and negative tests; unsupported input must fail closed.
3. Run the relevant Gradle common, Android, and iOS targets locally.
4. Run `git diff --check` and preserve `LICENSE`/`NOTICE` and third-party notices.
5. Use a conventional PR title (`feat(core): ...`, `fix(parser): ...`,
   `docs: ...`) and a body containing scope, compatibility impact, tests, and
   license/NOTICE impact.

Commits should use the repository identity configured for the contributor and
include a `Signed-off-by` line. Do not claim official Mermaid parity or add
DOM/WebView dependencies to `commonMain`.
