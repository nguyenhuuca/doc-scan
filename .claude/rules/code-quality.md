# Code Quality Standards — Doc Scanner Android

## SOLID Principles

1. **Single Responsibility**: Each ViewModel/Repository/UseCase has one reason to change
2. **Open/Closed**: Extend behaviour via new UseCases, not by modifying Repository
3. **Liskov Substitution**: Fake repos in tests must be full substitutes for real `DocumentRepository`
4. **Interface Segregation**: Keep `DocumentRepository` focused; don't add unrelated queries
5. **Dependency Inversion**: ViewModel depends on `DocumentRepository` interface, not `DocumentRepositoryImpl`

## DRY

- **Knowledge duplication** (must fix): same limit/quality constant in >1 place → move to `AppConfig`
- **Incidental duplication** (evaluate): similar Composable structure that may evolve independently

## Android-Specific Performance Rules

### Threading
- File I/O → `withContext(Dispatchers.IO)`
- CPU-heavy bitmap ops (compress, decode, transform) → `withContext(Dispatchers.Default)`
- Nothing blocking on `Dispatchers.Main` (main thread)
- Composables must never call suspend functions directly — read from `StateFlow`

### Bitmap memory
- Always use `inJustDecodeBounds = true` first pass to get dimensions without allocating
- Use `calcInSampleSize` with `||` (not `&&`) for correct two-dimension downsampling
- Recycle bitmaps explicitly after use in non-Compose contexts
- Undo stack stores JPEG `ByteArray`, not raw `Bitmap` (5 × ~200 KB vs 5 × ~34 MB)

### Database
- Never call individual DAO methods in a loop — use bulk `@Update` / `@Insert`
- Wrap multi-step DB changes in `database.withTransaction { }`
- N+1 pattern = instant review failure

### Slider / frequent events
- Debounce with `MutableSharedFlow + debounce(AppConfig.EDIT_SLIDER_DEBOUNCE_MS)`
- Never call `applyTransform` directly per slider drag event

## Quality Gates

All must pass before committing:

```bash
./gradlew testDebugUnitTest --no-daemon   # must be GREEN
```

- [ ] No new hardcoded constants (use `AppConfig`)
- [ ] No blocking I/O on main thread
- [ ] No N+1 DB query pattern
- [ ] Room schema change has migration + committed JSON in `app/schemas/`

## Refactoring Discipline

**Two Hats Rule**: Never mix refactoring and feature work in the same commit.

- **Hat 1 — Refactor**: change structure, NOT behaviour. Tests pass unchanged.
- **Hat 2 — Feature**: add behaviour. Tests updated or added.

Commit after each hat before switching.

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| ViewModel state | `*UiState` data class | `EditUiState` |
| Screen composable | `*Screen` | `DocumentViewerScreen` |
| Use case | verb + noun + `UseCase` | `SaveDocumentUseCase` |
| Repository impl | interface name + `Impl` | `DocumentRepositoryImpl` |
| DB entity | domain name + `Entity` | `PageEntity` |
| Config constants | `CATEGORY_NAME` | `IMAGE_JPEG_QUALITY` |
