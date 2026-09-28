# Project Guidelines

This file documents the branching strategy, commit conventions, and PR policies for the a2a-java project.

- Fix branch naming pattern: `issue-<ISSUE_NUMBER>`
- Feature branch naming pattern: `issue-<ISSUE_NUMBER>-<short-slug>`
- Bugfix branch naming pattern: `issue-<ISSUE_NUMBER>`
- Quick-fix branch naming pattern: `quick-fix/<short-slug>`
- SonarCloud branch naming pattern: _(n/a — no SonarCloud)_
- CI-fix branch naming pattern: `ci-issue/<short-slug>`
- Commit format (fix): Conventional Commits — `<type>(<scope>): <description>\n\nFixes #<ISSUE_NUMBER>`
- Commit format (quick-fix): `chore: <brief description>`
- Commit format (ci-fix): `ci: <brief description>`
- PR creation policy: always
- Find-task source: GitHub labels
- Find-task beginner label: `good first issue`
- Find-task intermediate label: _(none)_
- Find-task experienced label: `help wanted`
- Scope-too-large redirect: break into smaller issues and PRs; squash commits into one logical commit per PR unless multiple meaningful commits are warranted

## Notes

- PRs must be reviewed by at least one a2a-java committer before merging.
- Link the issue in both the PR description and the commit message (e.g. `Fixes #<NUMBER>`).
- PRs should include tests and documentation for new functionality.
- Prefer squashing all commits into a single commit for small changes.

## Version

788c6a703f406b4196cdcb5b30173bd971e1b6e0
