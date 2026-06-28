# Swarm Worker Guidelines — Doc Scanner Android

## Context Efficiency

1. **Workers inherit session context** — CLAUDE.md and rules are loaded automatically
2. **Narrow scope** — each worker focuses on one file or one concern
3. **Minimal tools** — only the tools needed for the task
4. **Right-sized models** — Haiku for search, Sonnet for implementation, Opus for architecture

## Worker Types

| Worker | Model | Tools | Use |
|--------|-------|-------|-----|
| `worker-explorer` | haiku | Read, Glob, Grep | Fast codebase search |
| `worker-builder` | sonnet | Read, Write, Edit, Bash, Glob, Grep | Implementation / testing / refactoring |
| `worker-reviewer` | sonnet | Read, Glob, Grep, Bash | Code review / security / performance |
| `worker-researcher` | sonnet | Read, Glob, Grep, WebFetch, WebSearch | Android API / library research |
| `worker-architect` | opus | Read, Write, Edit, Glob, Grep | Complex design decisions |

## Worker Focus Modes

**worker-builder:**
- `implementation` (default) — write code per spec
- `testing` — write JVM unit tests in `src/test/`, no Android types
- `refactoring` — extract patterns, apply Two Hats Rule (see code-quality.md)

**worker-reviewer:**
- `quality` (default) — naming, patterns, tests, AppConfig usage
- `security` — path traversal, UUID validation, private storage (see security.md)
- `performance` — blocking I/O on main thread, N+1 DB, bitmap OOM, slider debounce

## Swarm Patterns for This Project

### Parallel Exploration
```
Spawn 4-6 worker-explorer agents simultaneously
Each searches a different package (ui/, data/, domain/, common/)
Aggregate findings before implementation
```

### Feature Implementation
```
1. worker-architect: design API surface (interface, UiState, UseCase)
2. worker-builder (implementation): data layer
3. worker-builder (implementation): ViewModel + UI
4. worker-builder (testing): unit tests in src/test/
5. worker-reviewer (quality): review all outputs
```

### Performance / Security Sweep
```
worker-reviewer (focus: performance OR security) scans all packages in parallel
Findings prioritised by severity
worker-builder fixes Critical/High issues
Run ./gradlew testDebugUnitTest after fixes
```

## Worker Completion Requirements

When a worker finishes, it MUST:

1. Run quality gate: `./gradlew testDebugUnitTest --no-daemon` — must be green
2. If Room entity changed: `./gradlew kspDebugKotlin` + commit schema JSON
3. Commit with descriptive message
4. **Push to remote**: `git pull --rebase && git push`
5. Report completion with: files changed, tests passed, any follow-up issues

Work is **NOT complete** until `git push` succeeds.

## Android-Specific Worker Rules

- **No Android platform types in `src/test/`** — workers writing unit tests must extract pure logic
- **No backtick names with spaces in `src/androidTest/`** — use camelCase
- **Check AppConfig first** — before adding any constant, check if it already exists
- **Check dispatcher** — every file I/O or bitmap op must be on correct dispatcher
- **Check bulk ops** — any DAO loop must be converted to bulk `@Update`/`@Insert`

## Anti-Patterns

- ❌ Workers spawning workers (single-level only)
- ❌ Using Opus for search or simple implementation
- ❌ Skipping `git push` — uncommitted work is invisible
- ❌ Skipping unit tests — never report done without green tests
- ❌ Loading full codebase into one worker — use narrow Glob/Grep first
