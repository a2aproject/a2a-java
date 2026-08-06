---
name: release
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

## Phase 1: Pre-Release Verification

1. Verify clean working tree:
   ```bash
   git status
   ```
   If there are uncommitted changes or we're not on `main`, stop and ask.

2. Check latest CI status on main:
   ```bash
   gh run list --branch main --limit 5
   ```
   If CI is failing, alert the user and stop.

3. Confirm the current SNAPSHOT version in `pom.xml` matches expectations.

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

4. Create the release PR:
   ```bash
   git checkout -b release/<version>
   git add -A
   git commit -m "chore: release <version>"
   git push origin release/<version>
   gh pr create --title "chore: release <version>" --body "Release <version>"
   ```

5. Wait for CI:
   ```bash
   gh pr checks --watch
   ```
   If there are flaky failures, rerun with `gh run rerun <run-id> --failed` and watch again.

6. **Ask the user for confirmation before merging.** Then merge:
   ```bash
   gh pr merge --squash
   ```

## Phase 3: Tag & Deploy

1. Update local main:
   ```bash
   git checkout main
   git pull origin main
   ```

2. Create annotated tag:
   ```bash
   git tag -a v<version> -m "Release <version>"
   ```

3. **Ask the user for confirmation before pushing the tag** — this is irreversible and triggers Maven Central deployment.

4. Push the tag:
   ```bash
   git push origin v<version>
   ```
   This triggers `release-to-maven-central.yml` and `create-github-release.yml`.

## Phase 4: Documentation (conditional)

Documentation is created before the SNAPSHOT bump so that Javadoc generation uses release version strings.

**Decision rules:**
- **Skip entirely** for micro/patch releases
- **Ask the user** for pre-releases (Alpha/Beta/CR)
- **Always do** for major/minor Final releases

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

6. Generate Javadoc:
   ```bash
   mvn javadoc:aggregate -Psite-javadoc
   mkdir -p docs/public/<version_underscored>/apidocs
   cp -r target/reports/apidocs/* docs/public/<version_underscored>/apidocs/
   ```
   If the `site-javadoc` profile doesn't exist, note it and skip.

7. Add Javadoc menu entry to the version yml if not already present.

8. Create and merge a docs PR:
   ```bash
   git checkout -b docs/release-<version>
   git add -A
   git commit -m "docs: <version> release"
   git push origin docs/release-<version>
   gh pr create --title "docs: <version> release" --body "Versioned documentation for <version>"
   gh pr checks --watch
   ```
   If there are flaky failures, rerun with `gh run rerun <run-id> --failed` and watch again.
   Once CI passes, merge:
   ```bash
   gh pr merge --squash
   ```

## Phase 5: Bump to Next SNAPSHOT

1. Update local main:
   ```bash
   git checkout main
   git pull origin main
   ```

2. Bump to next SNAPSHOT:
   ```bash
   ./update-version.sh <release-version> <next-SNAPSHOT>
   ```

3. Create the SNAPSHOT PR:
   ```bash
   git checkout -b chore/bump-to-<next-SNAPSHOT>
   git add -A
   git commit -m "chore: bump version to <next-SNAPSHOT>"
   git push origin chore/bump-to-<next-SNAPSHOT>
   gh pr create --title "chore: bump version to <next-SNAPSHOT>" --body "Bump version to <next-SNAPSHOT>"
   gh pr checks --watch
   ```
   If there are flaky failures, rerun with `gh run rerun <run-id> --failed` and watch again.

4. Check that the Maven Central deployment workflow completed successfully:
   ```bash
   gh run list --workflow=release-to-maven-central.yml --limit 5
   ```
   If the release workflow failed, stop and guide troubleshooting (check logs — common causes: expired tokens, javadoc issues). May need to delete the tag and retag.

5. Merge the SNAPSHOT PR once everything is green:
   ```bash
   gh pr merge --squash
   ```

## Phase 6: Verify Deployment

Print the following URLs for the maintainer to check:

- **Maven Central**: `https://central.sonatype.com/artifact/org.a2aproject.sdk/a2a-java-sdk-parent/<version>`
- **GitHub Release**: `https://github.com/a2aproject/a2a-java/releases/tag/v<version>`

Note that Maven Central propagation can take up to 2 hours.
