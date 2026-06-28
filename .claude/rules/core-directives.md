# Core Directives — Doc Scanner Android

## Read CLAUDE.md First

Before starting any task, CLAUDE.md at the repo root is the primary reference:
- Project structure and package layout
- Core principles (AppConfig, threading, N+1, debounce)
- Storage layout (filesDir vs cacheDir)
- Migration checklist
- Prohibited patterns

## Single Source of Truth

| What | Where |
|------|-------|
| All tunable constants | `AppConfig.kt` |
| Tech choices | `.claude/rules/tech-strategy.md` |
| DB schema history | `app/schemas/**/*.json` (committed to git) |
| Migration logic | `Migrations.kt` + `AppDatabase.ALL` |

Never duplicate a value that already exists in `AppConfig`. Never hardcode what belongs there.

## Before Writing Code

1. Check `AppConfig` — does the constant already exist?
2. Check `DocumentRepository` interface — does the method already exist?
3. Check existing UseCases — is there a use case that already does this?
4. Use established patterns (suspend + IO dispatcher, bulk DAO, StateFlow → Composable)

## Coroutine Dispatcher Rules

| Work type | Dispatcher |
|-----------|-----------|
| File read / write / exists | `Dispatchers.IO` |
| Bitmap decode / encode / transform | `Dispatchers.Default` |
| UI state updates | `Dispatchers.Main` (via StateFlow) |
| Room DAOs | Room handles internally (suspend funs) |

## Commit Rules

- Run `./gradlew testDebugUnitTest --no-daemon` — must be green before committing
- If Room entity changed: run `./gradlew kspDebugKotlin`, commit the new `app/schemas/.../N.json`
- One logical change per commit; mix of feature + refactor = two commits
