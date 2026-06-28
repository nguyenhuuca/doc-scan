# Security Standards — Doc Scanner Android

## Android-Specific Checklist

- [ ] Document IDs validated as UUID format before use in file paths (prevents path traversal)
- [ ] Image paths never constructed from raw user input
- [ ] No `..` traversal possible in `filesDir` paths
- [ ] PDF export written to `cacheDir` (cleared by OS) not exposed to other apps
- [ ] No sensitive data written to external storage without explicit user action
- [ ] `debuggable = false` in release build (enforced by Proguard config)
- [ ] No hardcoded secrets or API keys in source code
- [ ] Proguard enabled for release (`isMinifyEnabled = true`)
- [ ] APK signed before distribution (keystore managed outside repo)

## General Security Checklist

- [ ] No hardcoded credentials
- [ ] All user input validated and sanitized
- [ ] SQL via Room parameterized queries only — no raw string concatenation
- [ ] Error messages don't expose internal file paths or stack traces to UI
- [ ] Dependencies audited before adding

## Path Traversal Prevention (CWE-22)

`ImageStorage.requireValidId()` validates UUIDs before constructing paths:
```kotlin
require(documentId.matches(Regex("[0-9a-fA-F]{8}-...-[0-9a-fA-F]{12}")))
```
Any new method accepting a `documentId` parameter **must** call `requireValidId()` first.
Never construct `File(filesDir, userInput)` without this check.

## OWASP Mobile Top 10

| Risk | Check For in This App |
|------|-----------------------|
| M1 Improper Credential Usage | No credentials exist — N/A |
| M2 Inadequate Supply Chain | Audit gradle dependencies before adding |
| M3 Insecure Authentication | No auth — N/A |
| M4 Insufficient Input/Output Validation | Document ID UUID check, image path validation |
| M5 Insecure Communication | No network calls — N/A |
| M6 Inadequate Privacy Controls | All data in private `filesDir`, not external storage |
| M7 Insufficient Binary Protections | Proguard enabled in release build |
| M8 Security Misconfiguration | `debuggable=false`, no exported components unnecessarily |
| M9 Insecure Data Storage | Private internal storage only — no SharedPreferences with sensitive data |
| M10 Insufficient Cryptography | No crypto needed — data is private to app |

## Severity Classification

| Severity | Definition | Action |
|----------|------------|--------|
| Critical | Data loss, path traversal, exploitable crash | MUST fix before merge |
| High | Privacy leak, insecure file exposure, breaking bug | MUST fix before merge |
| Medium | Proguard gap, minor info leak, code smell | SHOULD fix |
| Low | Best practice, style | COULD fix |

## Dependency Safety

- Audit any new library before adding to `build.gradle.kts`
- Prefer zero new dependencies — existing stack covers all current needs
- Check for known CVEs on `libs.versions.toml` versions periodically

## Output Guidelines

- Never log file absolute paths at INFO level (they contain UUIDs but still internal)
- Never expose `exception.message` directly in UI error strings
- Provide specific file:line in review findings
- Reference CWE IDs when reporting vulnerabilities
