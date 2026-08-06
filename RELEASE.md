# Release Process

This document describes the process for releasing a new version of the A2A Java SDK to Maven Central.

## Overview

The release process involves:
1. Updating version numbers across the project (automated)
2. Opening and merging a release PR
3. Tagging the release
4. Automatic deployment to Maven Central
5. Automatic GitHub release creation
6. Incrementing to next SNAPSHOT version

## Prerequisites

### Required Accounts & Access
- GitHub repository write access to `a2aproject/a2a-java`
- Maven Central account: namespace: `org.a2aproject.sdk`

### Required Secrets (Repository Maintainers)
The following secrets must be configured in GitHub repository settings:
- `GPG_SIGNING_KEY`: Private GPG key for artifact signing
- `GPG_SIGNING_PASSPHRASE`: Passphrase for the GPG key
- `CENTRAL_TOKEN_USERNAME`: Maven Central username token
- `CENTRAL_TOKEN_PASSWORD`: Maven Central password token

## Release Steps

The examples below use versions like `0.4.0.Alpha1-SNAPSHOT` and `0.4.0.Alpha1` for demonstration. Be sure to substitute these with the actual versions for your release.

### 1. Prepare Release Version

Use the provided script to update all version numbers:

```bash
# Preview changes (dry run)
./update-version.sh 0.4.0.Alpha1-SNAPSHOT 0.4.0.Alpha1 --dry-run

# Apply version update
./update-version.sh 0.4.0.Alpha1-SNAPSHOT 0.4.0.Alpha1
```

The script automatically updates:
- ✅ All `pom.xml` files
- ✅ All JBang script `//DEPS` declarations in `examples/`
- ✅ Validates the JBang update with built-in GMavenPlus validation

**What gets updated**:
```
pom.xml:              0.4.0.Alpha1-SNAPSHOT → 0.4.0.Alpha1
//DEPS io.github...:  0.4.0.Alpha1-SNAPSHOT → 0.4.0.Alpha1
```

### 2. Verify Changes

Review the changes before committing:

```bash
# Review all changes
git diff

# Verify build works
mvn clean install
```

### 3. Create Release PR

Create a pull request with the version update:

```bash
git checkout -b release/0.4.0.Alpha1
git add -A
git commit -m "chore: release 0.4.0.Alpha1"
git push origin release/0.4.0.Alpha1
```

Open PR on GitHub with title: `chore: release 0.4.0.Alpha1`

### 4. CI Verification

The release profile CI runs in two tiers:

**Tier 1** (all PRs, no secrets):
- ✅ Build succeeds with `-Prelease` profile (`-DskipTests -Dgpg.skip=true -Drelease.auto.publish=false`)
- ✅ All JavaDoc generation succeeds
- ✅ JBang version validation passes
- ✅ No compilation failures

**Tier 2** (allow-listed maintainers and pushes to `main`/`0.3.x`, with secrets):
- ✅ GPG signing works correctly
- ✅ Maven Central credentials are valid
- ✅ Full release profile build succeeds

**Important**: Tier 2 only runs for allow-listed actors (see `build-with-release-profile-run.yml`). Fork PRs from other contributors get Tier 1 only.

Wait for all CI checks to pass before proceeding.

### 5. Merge Release PR

Once all checks pass and the PR is approved:
- Merge the PR to `main` branch (squash merge — enforced by repo settings)

### 6. Tag and Push

After the PR is merged to main:

```bash
# Switch to main and pull the merged changes
git checkout main
git pull origin main

# Create annotated tag
git tag -a v0.4.0.Alpha1 -m "Release 0.4.0.Alpha1"

# Push the tag (triggers deployment + GitHub release)
git push origin v0.4.0.Alpha1
```

### 7. Automated Workflows Triggered

Pushing the tag triggers **two workflows**:

#### A. Maven Central Deployment (`release-to-maven-central.yml`)
1. Detects tag (pattern: `v?[0-9]+.[0-9]+.[0-9]+*`)
2. Checks out the tagged commit
3. Builds with `-Prelease -DskipTests`
4. Signs all artifacts with GPG
5. Deploys to Maven Central with auto-publish

**⏱️ Deployment typically takes 30 minutes**, but can vary.

#### B. GitHub Release Creation (`create-github-release.yml`)
1. Detects the same tag
2. Extracts version from tag name
3. Generates release notes from commits since last release
4. Creates GitHub release with:
   - Auto-generated changelog
   - Link to Maven Central artifacts
   - Installation instructions

### 8. Verify Deployment

Check that artifacts are available:

**Maven Central**:
```
https://central.sonatype.com/artifact/org.a2aproject.sdk/a2a-java-sdk-parent/0.4.0.Alpha1
```

**GitHub Release**:
```
https://github.com/a2aproject/a2a-java/releases/tag/v0.4.0.Alpha1
```

Artifacts should include:
- `.jar` files (main artifacts)
- `-sources.jar` (source code)
- `-javadoc.jar` (JavaDoc)
- `.pom` files
- `.asc` GPG signatures for all artifacts

### 9. Update Versioned Documentation

Create a new documentation version for the release:

```bash
# Copy dev docs to the new version folder (use underscores instead of dots to
# work around a Roq bug where dots in directory names break GitHub Pages serving)
cp -r docs/content/dev docs/content/X_Y_Z_Final

# Create a new version data file
cp docs/data/versions/dev.yml docs/data/versions/X.Y.Z.Final.yml
```

Edit `docs/data/versions/X.Y.Z.Final.yml`:
- Set `label` to `"X.Y.Z.Final"`
- Set `path` to `"X_Y_Z_Final"` (underscores, matching the content directory name)
- Set `sortOrder` to the next number (higher than the previous release)
- Set `defaultVersion` to `true`
- Set `devVersion` to `false`
- Verify the `menu` list matches the pages in the new version's content folder (add/remove entries if pages were added or removed since the previous release)

Update the previous default version's data file (e.g., `docs/data/versions/<previous-version>.yml`):
- Set `defaultVersion` to `false`

Review the new version's content for accuracy — ensure all pages reflect features available in this release.

#### Generate Javadoc (major/minor releases only)

For major and minor releases (X.Y.0.Final), generate aggregated Javadoc. Skip this step for micro/patch releases (X.Y.Z.Final where Z > 0) — the API surface doesn't change meaningfully.

```bash
# Generate aggregated Javadoc (from the tagged commit)
mvn javadoc:aggregate -Psite-javadoc

# Copy to the release version directory (overwrites the placeholder)
cp -r docs/public/dev/apidocs docs/public/X_Y_Z_Final/apidocs

# Commit the generated Javadoc
git add docs/public/X_Y_Z_Final/apidocs
git commit -m "docs: add Javadoc for X.Y.Z.Final"
```

The Javadoc menu entry structure matches `dev.yml` — copy it into the new version's data file when creating version files.

**Validation**: The docs site enforces that exactly one version has `defaultVersion: true` and all `sortOrder` values are unique (see `docs/src/main/java/org/a2aproject/docs/Versions.java`). Run the docs site locally (`cd docs && mvn quarkus:dev`) to verify the new version renders correctly.

### 10. Increment to Next SNAPSHOT

Prepare repository for next development cycle:

```bash
# Update to next SNAPSHOT version
./update-version.sh 0.4.0.Alpha1 0.4.0.Alpha2-SNAPSHOT

# Create and push PR
git checkout -b chore/bump-to-0.4.0.Alpha2-SNAPSHOT
git add -A
git commit -m "chore: bump version to 0.4.0.Alpha2-SNAPSHOT"
git push origin chore/bump-to-0.4.0.Alpha2-SNAPSHOT
```

Open PR, wait for CI, and merge.

## Troubleshooting

### Build fails with "JBang version mismatch"

**Cause**: JBang script dependencies don't match POM version

**Fix**:
```bash
# Re-run the update script to fix mismatches
./update-version.sh OLD_VERSION NEW_VERSION

# Or manually check:
grep -r "//DEPS org.a2aproject.sdk:" examples/
```

### GPG signing fails in workflow

**Cause**: GPG secrets are missing or incorrect

**Fix**: Repository maintainers - verify secrets in:
```
Settings → Secrets and variables → Actions
```
Check: `GPG_SIGNING_KEY`, `GPG_SIGNING_PASSPHRASE`

### Maven Central deployment times out

**Cause**: Normal Maven Central processing delays

**Fix**: Wait (up to 2 hours). Check status:
```
https://central.sonatype.com/publishing
```

### Deployment fails with authentication error

**Cause**: Maven Central tokens expired or incorrect

**Fix**: Repository maintainers:
1. Log in to Maven Central with the GitHub account for the a2asdk user.
2. Generate new tokens: `User → Generate User Token`
3. Update secrets: `CENTRAL_TOKEN_USERNAME` and `CENTRAL_TOKEN_PASSWORD`

### GitHub release not created

**Cause**: Workflow failed or tag pattern didn't match

**Fix**:
```bash
# Check workflow runs
https://github.com/a2aproject/a2a-java/actions

# Manually create release if needed
https://github.com/a2aproject/a2a-java/releases/new
```

### Need to rollback a release

**Not possible** - Maven Central does not allow artifact deletion.

**Mitigation**:
1. Release a patch version with fixes (e.g., `0.4.0.Alpha1` → `0.4.0.Alpha2`)
2. Document issues in GitHub release notes
3. Update documentation to recommend correct version

## Version Numbering

Follow semantic versioning with qualifiers:

- **Major.Minor.Patch.Final** - Standard releases (e.g., `1.0.0.Final`)
- **Major.Minor.Patch.AlphaN** - Alpha releases (e.g., `0.4.0.Alpha1`)
- **Major.Minor.Patch.BetaN** - Beta releases (e.g., `0.3.0.Beta1`)
- **Major.Minor.Patch.CRN** - Candidate releases (e.g., `1.0.0.CR1`)
- **-SNAPSHOT** - Development versions (e.g., `0.4.0.Alpha2-SNAPSHOT`)

## Workflows Reference

### build-with-release-profile.yml (Tier 1 — Trigger)
- **Triggers**: All PRs, all pushes, manual dispatch
- **Purpose**: Build with `-Prelease` profile without secrets; upload PR info for Tier 2
- **Catches**: Compilation, javadoc, plugin configuration issues

### build-with-release-profile-run.yml (Tier 2 — Secrets)
- **Triggers**: `workflow_run` on Tier 1 completion
- **Purpose**: Full release profile build with GPG signing and Maven Central credential validation
- **Access**: Allow-listed maintainers, pushes to `main`/`0.3.x`, manual dispatch
- **Requires**: GPG and Maven Central secrets

### release-to-maven-central.yml
- **Triggers**: Tags matching `v?[0-9]+.[0-9]+.[0-9]+*`
- **Purpose**: Deploy to Maven Central
- **Duration**: ~30 minutes
- **Requires**: GPG and Maven Central secrets

### create-github-release.yml
- **Triggers**: Tags matching `v?[0-9]+.[0-9]+.[0-9]+*`
- **Purpose**: Create GitHub release with changelog
- **Features**: Auto-generated release notes, Maven Central links
- **Requires**: Default `GITHUB_TOKEN` (automatic)

## Support

For questions or issues with the release process:
- Open an issue: https://github.com/a2aproject/a2a-java/issues
- Reference: [Issue #532](https://github.com/a2aproject/a2a-java/issues/532) - Release process improvements
