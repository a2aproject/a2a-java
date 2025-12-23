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
- Maven Central account: `kkhan+a2asdk@redhat.com` (namespace: `io.github.a2asdk`)

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
- ✅ All `pom.xml` files (48 files)
- ✅ All JBang script `//DEPS` declarations in `examples/`
- ✅ Validates the update with built-in GMavenPlus validation

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

The `build-with-release-profile.yml` workflow automatically verifies:
- ✅ Build succeeds with `-Prelease` profile
- ✅ All JavaDoc generation succeeds
- ✅ GPG signing works correctly
- ✅ JBang version validation passes
- ✅ No compilation or test failures

**Important**: This workflow tests the actual PR branch (not main) to catch issues before merge.

Wait for all CI checks to pass before proceeding.

### 5. Merge Release PR

Once all checks pass and the PR is approved:
- Merge the PR to `main` branch
- **Do NOT squash** - keep the release commit message intact for changelog

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
https://central.sonatype.com/artifact/io.github.a2asdk/a2a-java-sdk-parent/0.4.0.Alpha1
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

### 9. Increment to Next SNAPSHOT

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
grep -r "//DEPS io.github.a2asdk:" examples/
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
1. Log in to Maven Central with `kkhan+a2asdk@redhat.com`
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

- **Major.Minor.Patch** - Standard releases (e.g., `1.0.0`)
- **Major.Minor.Patch.AlphaN** - Alpha releases (e.g., `0.4.0.Alpha1`)
- **Major.Minor.Patch.BetaN** - Beta releases (e.g., `0.3.0.Beta1`)
- **Major.Minor.Patch.RCN** - Release candidates (e.g., `1.0.0.RC1`)
- **-SNAPSHOT** - Development versions (e.g., `0.4.0.Alpha2-SNAPSHOT`)

## Workflows Reference

### build-with-release-profile.yml
- **Triggers**: All PRs, all pushes
- **Purpose**: Verify builds with `-Prelease` profile
- **Special**: Tests actual PR branch (not main) using `pull_request_target` with explicit checkout
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
