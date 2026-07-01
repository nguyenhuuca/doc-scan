---
description: Release a new version of Doc Scanner — bump version, update changelog, push, trigger CI
argument-hint: <version> (e.g. 1.2.0)
allowed-tools: Read, Edit, Bash
---

# Release Orchestrator — Doc Scanner Android

Automate the full release flow: validate → bump → changelog → commit → push → trigger CI.

## Steps

### 1. Validate input

```bash
VERSION="$ARGUMENTS"
# Must match semver x.y.z
if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "ERROR: version must be x.y.z (e.g. 1.2.0)"
  exit 1
fi
```

### 2. Check working tree is clean

```bash
git diff --exit-code && git diff --cached --exit-code \
  || { echo "ERROR: uncommitted changes — commit or stash first"; exit 1; }
```

### 3. Read current version from build.gradle.kts

```bash
grep versionName app/build.gradle.kts
grep versionCode app/build.gradle.kts
```

### 4. Bump versionName and versionCode in build.gradle.kts

- `versionName` → `"$VERSION"`
- `versionCode` → previous value + 1

Use Edit tool, not sed.

### 5. Prepend new entry to assets/release_notes.yaml

Format:
```yaml
- version: "$VERSION"
  date: "DD/MM/YYYY"   ← today's date
  changes:
    - "<change 1 from git log since last tag>"
    - "<change 2>"
    - ...
```

Get changes from:
```bash
git log $(git describe --tags --abbrev=0 2>/dev/null || echo "")..HEAD \
  --no-merges --pretty='%s' \
  | grep -v '^chore:' | grep -v '^docs:' | grep -v '^ci:' \
  | grep -v '^test:' | grep -iv 'unit.test\|coverage\|jacoco'
```

Translate commit messages to user-facing Vietnamese bullet points.
Skip pure CI/chore/docs/test commits.
**Never include testing, unit test, or coverage content in the changelog.**

### 6. Run quality gate

```bash
./gradlew testDebugUnitTest --no-daemon
```

Must be GREEN before continuing.

### 7. Commit

```bash
git add app/build.gradle.kts app/src/main/assets/release_notes.yaml
git commit -m "chore: bump version to $VERSION, update release notes"
```

### 8. Push

```bash
git push
```

### 9. Trigger GitHub Actions release workflow

```bash
gh workflow run release.yml --field version=$VERSION
```

### 10. Confirm

```bash
sleep 5
gh run list --workflow=release.yml --limit=1
```

Report the run URL so the user can monitor CI.

## Constraints

- NEVER skip the quality gate (step 6)
- NEVER bump version if working tree is dirty
- NEVER hardcode version — always use `$ARGUMENTS`
- ALWAYS translate commits to user-facing Vietnamese for the YAML changelog
- NEVER include testing, unit test, or coverage changes in the changelog — users don't care about internal quality work
- ALWAYS use Edit tool (not sed/awk) to modify files

## Example invocation

```
/release 1.2.0
```

$ARGUMENTS
