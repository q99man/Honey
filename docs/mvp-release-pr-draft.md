# MVP Release Candidate PR Draft

Use this draft when opening the pull request from `codex/prepare-mvp-release-packaging` to `main`.

PR URL:

https://github.com/q99man/Honey/pull/new/codex/prepare-mvp-release-packaging

## Title

```text
docs: prepare MVP release runbook
```

## Body

```markdown
## Summary

- Add the MVP release runbook covering pre-release command order, staging smoke, browser smoke, stop cleanup, rollback, and rerun notes.
- Update deployment task tracking and progress docs for release branch packaging.
- Keep secret handling explicit so `.env`, provider credentials, phone numbers, verification codes, and tokens are not copied into docs or logs.

## Verification

- `.\scripts\run-backend-gradle.ps1 test`
- `.\scripts\run-backend-gradle.ps1 bootJar`
- `.\scripts\run-frontend-npm.ps1 run build`
- `.\scripts\run-frontend-npm.ps1 run lint`
- `git diff --check`
- `scripts/verify-ui-language.sh`
- `scripts/check-korean-encoding.sh`
- `scripts/verify-doc-sync.sh`
- Secret/smoke credential pattern scan returned no tracked-file matches.
- `.env`, local logs, build output, Gradle caches, frontend `dist`, and `node_modules` remained ignored.

## Release Notes

- MVP local auth, phone verification, region verification, place participation, ranking, reporting, and admin operation flows are implemented.
- SOLAPI live SMS delivery has been smoke tested.
- Ranking reads use aggregate season score rows.
- Core business limits remain server-side and policy-driven.
- Admin policy, report, place, user, activity, ranking, and audit-log screens are available.
- Production profile uses Flyway and `JPA_DDL_AUTO=validate` by default.

## Follow-Up

- Confirm CI status after opening the PR.
- Review the runbook with the operator who will perform the first release rehearsal.
- Keep production seed/bootstrap disabled unless an approved one-time setup run is being performed.
```

