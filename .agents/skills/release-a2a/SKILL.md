---
name: release-a2a
description: Guide maintainers through the multi-step release process — version bump, CI verification, tagging, Maven Central deployment, SNAPSHOT bump, and versioned documentation.
compatibility: Requires gh CLI, mvn, and git
allowed-tools: Bash(gh:*) Bash(mvn:*) Bash(git:*) Bash(./update-version.sh:*) Read Edit Write Glob Grep
---

# Release Process

Guide the full release lifecycle. Proceed autonomously through mechanical steps (running scripts, polling CI, creating PRs) and pause only for genuine decisions, failures, or destructive actions.

## Phase 0: Determine Release Parameters

1. Read the current version from the root `pom.xml` — the `-SNAPSHOT` suffix indicates the current dev version.
2. Ask the user which version to release if not already specified.
3. Apply the **Final suffix convention**: if the user specifies a plain version like `1.2.0`, the release version is `1.2.0.Final`. Pre-release qualifiers (`Alpha1`, `Beta1`, `CR1`) are used as-is.
4. Suggest a sensible next SNAPSHOT version and confirm with the user:
   - Final: `1.1.0.Final` → `1.1.1.Final-SNAPSHOT`
   - Pre-release: `1.1.0.Alpha1` → `1.1.0.Alpha2-SNAPSHOT`
5. Determine the documentation plan:
   - **Skip** for micro/patch releases (X.Y.Z where Z > 0)
   - **Ask** for pre-releases (Alpha/Beta/CR)
   - **Yes** for major/minor Final releases (X.Y.0.Final)
6. **Detect git remotes.** Run `git remote -v` and identify:
   - **Upstream remote**: the remote whose URL contains `a2aproject/a2a-java` (this is the canonical repo — it may be called `upstream`, `origin`, or anything else).
   - **Fork remote**: a different remote owned by the current user (typically `origin`). Extract the fork owner from its URL (e.g., `kabir` from `github.com:kabir/a2a-java.git`).

   Throughout this skill, `<upstream>` refers to the detected upstream remote name and `<fork>` refers to the fork remote name. All PRs are created from the fork, and tags/main are pushed to/pulled from upstream.

## Phase 1: Pre-Release Verification

1. Verify `gh` CLI is installed and authenticated:
   ```bash
   gh auth status
   ```
   If not installed or not logged in, stop and ask the user to run `gh auth login`.

2. Verify clean working tree:
   ```bash
   git status
   ```
   If there are uncommitted changes, stop and ask.

3. Verify we are on `main` and in sync with upstream. Fetch first to ensure the remote ref is current:
   ```bash
   git fetch <upstream> main
   git log --oneline HEAD..<upstream>/main
   git log --oneline <upstream>/main..HEAD
   ```
   If local main is behind or ahead of `<upstream>/main`, stop and ask the user.

4. Check latest CI status on main:
   ```bash
   gh run list --repo a2aproject/a2a-java --branch main --limit 5
   ```
   If CI is failing, alert the user and stop.

5. Confirm the current SNAPSHOT version in `pom.xml` matches expectations.

## Phase 2: Version Bump & Release PR

1. Preview version changes:
   ```bash
   ./update-version.sh <current-SNAPSHOT> <release-version> --dry-run
   ```

2. Apply version update:
   ```bash
   ./update-version.sh <current-SNAPSHOT> <release-version>
   ```

3. Verify the build compiles (tests will run in CI):
   ```bash
   mvn clean install -DskipTests
   ```
   If the build fails, stop and report.

4. Create the release PR (pushed from fork):
   ```bash
   git checkout -b release/<version>
   git add -A
   git commit -m "chore: release <version>"
   git push <fork> release/<version>
   gh pr create --repo a2aproject/a2a-java --head <fork-owner>:release/<version> --base main --title "chore: release <version>" --body "Release <version>"
   ```

5. Wait for CI:
   ```bash
   gh pr checks <pr-number> --repo a2aproject/a2a-java --watch
   ```
   If there are flaky failures, rerun with `gh run rerun <run-id> --repo a2aproject/a2a-java --failed` and watch again.

6. **Ask the user for confirmation before merging.** Then merge:
   ```bash
   gh pr merge <pr-number> --repo a2aproject/a2a-java --squash
   ```

## Phase 3: Tag & Deploy

1. Update local main:
   ```bash
   git checkout main
   git pull <upstream> main
   ```

2. Create annotated tag:
   ```bash
   git tag -a v<version> -m "Release <version>"
   ```

3. **Ask the user for confirmation before pushing the tag** — this is irreversible and triggers Maven Central deployment.

4. Push the tag:
   ```bash
   git push <upstream> v<version>
   ```
   This triggers `release-to-maven-central.yml` and `create-github-release.yml`.

## Phase 4: Documentation, SNAPSHOT Bump & Post-Release PR

This phase combines documentation (when applicable) and the SNAPSHOT version bump into a single PR to avoid redundant CI waits.

### Step 1: Update local main

```bash
git checkout main
git pull <upstream> main
```

### Step 2: Documentation (conditional)

Documentation is created before the SNAPSHOT bump so that Javadoc generation uses release version strings.

**Decision rules:**
- **Skip** for micro/patch releases (X.Y.Z where Z > 0)
- **Ask the user** for pre-releases (Alpha/Beta/CR)
- **Always do** for major/minor Final releases (X.Y.0.Final)

When applicable:

1. Copy dev docs to the new version (replace dots with underscores in directory names to
   work around a Roq bug where dots break GitHub Pages serving):
   ```bash
   cp -r docs/content/dev docs/content/<version_underscored>
   ```
   For example, `1.2.0.Final` → directory name `1_2_0_Final`.

2. Create the version data file by copying `dev.yml` (it has the most up-to-date menu):
   ```bash
   cp docs/data/versions/dev.yml docs/data/versions/<version>.yml
   ```

3. Edit `docs/data/versions/<version>.yml`:
   - Set `label` to `"<version>"`
   - Set `path` to `"<version_underscored>"` (underscores, matching the content directory name)
   - Set `sortOrder` to the next value — scan existing ymls for max `sortOrder` **excluding** `dev.yml` (which uses 999 as a sentinel), then increment by 1
   - Set `defaultVersion` to `true` only for Final releases
   - Set `devVersion` to `false`

4. For Final releases: set the previous default version's `defaultVersion` to `false`.

5. For pre-releases superseding a prior pre-release in the same X.Y.Z series: remove the old pre-release's content folder (`docs/content/<old-version_underscored>`), version yml (`docs/data/versions/<old-version>.yml`), and apidocs folder (`docs/public/<old-version_underscored>/apidocs/`).

6. Generate Javadoc. The `site-javadoc` profile outputs to `docs/public/dev/apidocs/`:
   ```bash
   mvn javadoc:aggregate -Psite-javadoc
   cp -r docs/public/dev/apidocs docs/public/<version_underscored>/apidocs
   ```
   If the `site-javadoc` profile doesn't exist, note it and skip.

7. Add Javadoc menu entry to the version yml if not already present.

### Step 3: Bump to next SNAPSHOT

```bash
./update-version.sh <release-version> <next-SNAPSHOT>
```

### Step 4: Create and merge the post-release PR

Use the branch name and PR title/body based on what the PR contains:
- **With docs**: branch `chore/post-release-<version>`, title `"chore: <version> docs and bump to <next-SNAPSHOT>"`, body `"Versioned documentation for <version> and bump to <next-SNAPSHOT>"`
- **Without docs**: branch `chore/bump-to-<next-SNAPSHOT>`, title `"chore: bump version to <next-SNAPSHOT>"`, body `"Bump version to <next-SNAPSHOT>"`

```bash
git checkout -b <branch-name>
git add -A
git commit -m "<commit-message>"
git push <fork> <branch-name>
gh pr create --repo a2aproject/a2a-java --head <fork-owner>:<branch-name> --base main --title "<title>" --body "<body>"
gh pr checks <pr-number> --repo a2aproject/a2a-java --watch
```
If there are flaky failures, rerun with `gh run rerun <run-id> --repo a2aproject/a2a-java --failed` and watch again.

### Step 5: Verify Maven Central deployment

Check that the Maven Central deployment workflow completed successfully:
```bash
gh run list --repo a2aproject/a2a-java --workflow=release-to-maven-central.yml --limit 5
```
If the release workflow failed, stop and guide troubleshooting (check logs — common causes: expired tokens, javadoc issues). May need to delete the tag and retag.

### Step 6: Merge

Once CI and Maven Central deployment are both green:
```bash
gh pr merge <pr-number> --repo a2aproject/a2a-java --squash
```

## Phase 5: Verify Deployment

Print the following URLs for the maintainer to check:

- **Maven Central**: `https://central.sonatype.com/artifact/org.a2aproject.sdk/a2a-java-sdk-parent/<version>`
- **GitHub Release**: `https://github.com/a2aproject/a2a-java/releases/tag/v<version>`

Note that Maven Central propagation can take up to 2 hours.
