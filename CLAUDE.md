# CLAUDE.md — Doc Scanner Android

Project-specific guidance for Claude Code when working in this repository.

## Project Overview

**Doc Scanner** — Android document scanning app (Kotlin, Jetpack Compose).
Scan physical documents with camera, edit pages, export to PDF.

- **Repo**: https://github.com/nguyenhuuca/doc-scan
- **Min SDK**: 26 (Android 8.0) · **Target/Compile SDK**: 35
- **Language**: Kotlin 2.0.21 · **JVM target**: 21 (Kotlin 2.x max)
- **CI**: GitHub Actions — JDK 24 build, unit tests on push to master

---

## Essential Commands

```bash
# Run unit tests (fast, no device needed)
./gradlew testDebugUnitTest --no-daemon

# Build debug APK
./gradlew assembleDebug --no-daemon

# Generate Room schema JSON after entity changes
./gradlew kspDebugKotlin --no-daemon
```

**Release**: triggered manually from GitHub Actions UI (`workflow_dispatch`).
Enter version (e.g. `1.2.0`) → tests → APK build → GitHub Release created.

---

## Project Structure

```
app/src/main/java/com/docscanner/
├── common/
│   ├── AppConfig.kt          ← ALL tunable constants (image quality, limits, etc.)
│   └── BitmapUtils.kt        ← calcInSampleSize()
├── data/
│   ├── local/
│   │   ├── db/               ← Room: AppDatabase, DAOs, Migrations.kt
│   │   ├── entity/           ← DocumentEntity, PageEntity
│   │   └── filesystem/       ← ImageStorage, ThumbnailGenerator
│   ├── pdf/                  ← PdfGenerator (raw PDF with DCTDecode)
│   └── repository/           ← DocumentRepository interface + Impl
├── di/
│   └── AppContainer.kt       ← Manual DI, all singletons
├── domain/
│   ├── model/                ← Document, Page (pure data classes)
│   └── usecase/              ← SaveDocument, DeleteDocument, ExportPdf, etc.
└── ui/
    ├── documentlist/         ← Home screen
    ├── viewer/               ← Per-document page viewer
    ├── edit/                 ← Page editing (rotate, brightness, contrast, undo)
    ├── scanner/              ← Camera scan + manual crop
    └── common/               ← Shared UI components

app/schemas/                  ← Room schema JSON (commit these files)
app/src/test/                 ← JVM unit tests (fast, no Android device)
app/src/androidTest/          ← Instrumented tests (require device/emulator)
.github/workflows/
├── build.yml                 ← CI: push to master → run unit tests
└── release.yml               ← Manual release: tests → APK → GitHub Release
```

---

## Tech Stack

| Component | Choice |
|-----------|--------|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material3 |
| Architecture | MVVM (ViewModel → Repository → Room/Filesystem) |
| DI | Manual (`AppContainer`) — no Hilt/Dagger |
| Database | Room 2.6.1 |
| Async | Kotlin Coroutines + Flow |
| Image loading (UI) | Coil 2 |
| Camera | CameraX + ML Kit Document Scanner |
| Build | Gradle 8.9 + KSP |
| CI | GitHub Actions (JDK 24) |

---

## Core Principles

### 1. AppConfig is the single source of truth for all tunable values

All magic numbers live in `AppConfig.kt`. Never hardcode values that might change:

```kotlin
// ✅ correct
scaled.compress(Bitmap.CompressFormat.JPEG, AppConfig.IMAGE_JPEG_QUALITY, out)

// ❌ wrong
scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
```

**Key knobs in AppConfig:**
- `IMAGE_JPEG_QUALITY` — 85 (print) / 75 (screen) / 70 (compact)
- `IMAGE_MAX_WIDTH / HEIGHT` — 2480×3508 = A4 at 300 DPI
- `PDF_PAGE_WIDTH/HEIGHT_PT` — 595×842 = A4 at 72 pt/inch
- `MAX_DOCUMENTS / MAX_PAGES` — 100 / 50
- `EDIT_UNDO_JPEG_QUALITY` — 75 (only in memory, not saved to disk)

### 2. No blocking I/O on main thread

- File reads/writes → `withContext(Dispatchers.IO)`
- CPU-heavy bitmap ops → `withContext(Dispatchers.Default)`
- Never call `BitmapFactory.decodeFile` or `File.exists()` from a Composable directly

### 3. Slider events must be debounced

Use `MutableSharedFlow + debounce(AppConfig.EDIT_SLIDER_DEBOUNCE_MS)` for any value
that triggers a heavy operation on drag. See `EditViewModel.kt`.

### 4. N+1 DB queries are forbidden

Use bulk Room operations (`@Update fun updatePages(List<PageEntity>)`).
Never loop calling individual DAO methods inside a transaction.

### 5. calcInSampleSize uses `||` not `&&`

The while-loop condition must be `||` so BOTH dimensions are fully downsampled:
```kotlin
while (halfHeight / inSampleSize >= maxHeight || halfWidth / inSampleSize >= maxWidth)
```

---

## Storage Layout

All data is **private internal storage** — not visible in file managers or Gallery.

```
context.filesDir  →  /data/data/com.docscanner/files/
    documents/{uuid}/pages/page_001_{ts}.jpg   ← page images (ImageStorage)
    thumbnails/{uuid}_thumb.jpg                 ← thumbnails

context.cacheDir  →  /data/data/com.docscanner/cache/
    export/export_{ts}.pdf                      ← PDF exports (deleted by OS if needed)
```

Data survives app updates. Data is deleted on uninstall or "Clear Data".

---

## PDF Generation

`PdfGenerator` builds raw PDF (PDF 1.4 spec) with **DCTDecode** filter —
JPEG bytes from disk are embedded directly without bitmap decode/re-encode.

```
PDF size ≈ sum of JPEG files on disk + ~500 bytes/page overhead
```

To reduce PDF size: lower `AppConfig.IMAGE_JPEG_QUALITY`.

---

## Database Migrations

Schema version is currently **1**. When changing entities:

1. Edit the entity class
2. Bump `version` in `@Database` in `AppDatabase.kt`
3. Add migration object in `Migrations.kt`
4. Add it to `AppDatabase.ALL`
5. Run `./gradlew kspDebugKotlin` → Room writes `app/schemas/.../N.json`
6. **Commit the JSON file** alongside the code change

Never use `fallbackToDestructiveMigration()` — it wipes all user data.

---

## Testing Strategy

| Layer | Location | Tool |
|-------|----------|------|
| Business logic, pure functions | `src/test/` | JUnit 4, Mockito |
| UI flows (fake repos) | `src/androidTest/` | Compose test rules |
| DB schema migrations | `src/androidTest/` | Room `MigrationTestHelper` |

**Unit test rule**: No Android platform types (Bitmap, Context) in `src/test/`.
Extract pure logic methods for testability; put integration tests in `src/androidTest/`.

**Backtick test names**: avoid spaces in `src/androidTest/` test methods —
D8 rejects class names with spaces below API 34 (DEX version 040 restriction).

---

## Quality Gates (must pass before commit)

```bash
./gradlew testDebugUnitTest --no-daemon   # all unit tests green
```

For code changes:
- [ ] No blocking I/O on main thread
- [ ] No N+1 DB query patterns
- [ ] AppConfig used for any new configurable value
- [ ] New Room schema change has a migration + committed JSON

---

## Prohibited Patterns

- ❌ Hardcoded magic numbers — use `AppConfig`
- ❌ `BitmapFactory.decodeFile()` outside `withContext(Dispatchers.IO)`
- ❌ Calling `repository.pageFileExists()` from a Composable — pre-compute in ViewModel
- ❌ Individual `pageDao.updatePageNumber()` in a loop — use `pageDao.updatePages(list)`
- ❌ `&&` in `calcInSampleSize` while-loop — must be `||`
- ❌ `fallbackToDestructiveMigration()` in database builder
- ❌ `SimpleDateFormat` in a shared singleton (not thread-safe) — use instance per call
- ❌ Backtick method names with spaces in `androidTest/` (DEX 040 limit)
