## Summary

<!-- What is this change and why is it needed? Keep one compatibility or
     infrastructure seam per pull request. -->

## Compatibility impact

- [ ] No syntax/API compatibility change
- [ ] Compatibility matrix and pinned fixture updated
- [ ] Unsupported input still fails closed with typed diagnostics

## Verification

<!-- Include exact commands and relevant target results. -->

- [ ] Focused tests pass
- [ ] Android and/or iOS targets pass when affected
- [ ] `git diff --check` passes
- [ ] `verifyThirdPartyNotices` passes when dependencies or notices change

## API, security, and license review

- [ ] Public API and module boundaries are documented
- [ ] No DOM/WebView/JavaScript dependency enters `commonMain`
- [ ] Untrusted source and renderer output boundaries are covered
- [ ] Third-party license/NOTICE impact is recorded

## Checklist

- [ ] PR title uses Conventional Commits (`feat(core): ...`, `fix(parser): ...`, `docs: ...`)
- [ ] Commit author/committer and `Signed-off-by` identify the actual contributor
- [ ] This PR is small enough for an independent review
