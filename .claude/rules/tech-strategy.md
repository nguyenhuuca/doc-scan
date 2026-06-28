# Tech Strategy — Doc Scanner Android

This is the **SINGLE SOURCE OF TRUTH** for technology choices.

## Compliance

1. **Follow This File**: Use the technologies listed below
2. **No Deviations**: Do not suggest alternatives unless explicitly instructed
3. **No New Dependencies**: Audit carefully before adding any library

---

## Language & Platform

| Component | Choice |
|-----------|--------|
| Language | Kotlin 2.0.21 |
| Min SDK | 26 (Android 8.0) |
| Target / Compile SDK | 35 |
| JVM target | 21 (Kotlin 2.x max — do NOT use 24) |
| Build tool | Gradle 8.9 + KSP |
| CI JDK | 24 (compile only, not JVM target) |

---

## Android Stack

| Component | Choice |
|-----------|--------|
| UI | Jetpack Compose + Material3 |
| Navigation | Compose Navigation |
| Architecture | MVVM (ViewModel → Repository → Room / Filesystem) |
| DI | Manual (`AppContainer`) — no Hilt/Dagger |
| Async | Kotlin Coroutines + Flow |
| Image loading (UI) | Coil 2 |
| Camera | CameraX 1.4.0 |
| Document scanning | ML Kit Document Scanner 16.0.0-beta1 |
| Drag-to-reorder | sh.calvin.reorderable 2.4.0 |

---

## Data Layer

| Component | Choice |
|-----------|--------|
| Database | Room 2.6.1 |
| Schema migrations | Manual `Migration` objects in `Migrations.kt` |
| Image storage | Private internal storage (`context.filesDir`) |
| Image format | JPEG, quality configurable via `AppConfig.IMAGE_JPEG_QUALITY` |
| PDF generation | Raw PDF 1.4 with DCTDecode (no third-party PDF library) |
| Thumbnail | Downscaled JPEG in `filesDir/thumbnails/` |

---

## Configuration

All tunable constants live in **`AppConfig.kt`** — the single source of truth.
Never hardcode values that appear in `AppConfig`.

| Constant | Default | Notes |
|----------|---------|-------|
| `IMAGE_MAX_WIDTH` | 2480 | A4 portrait 300 DPI |
| `IMAGE_MAX_HEIGHT` | 3508 | A4 portrait 300 DPI |
| `IMAGE_JPEG_QUALITY` | 85 | 75 = screen quality, 70 = compact |
| `PDF_PAGE_WIDTH_PT` | 595 | A4 at 72 pt/inch |
| `PDF_PAGE_HEIGHT_PT` | 842 | A4 at 72 pt/inch |
| `MAX_DOCUMENTS` | 100 | |
| `MAX_PAGES` | 50 | |
| `MIN_STORAGE_BYTES` | 50 MB | |
| `EDIT_UNDO_JPEG_QUALITY` | 75 | In-memory only |
| `EDIT_SLIDER_DEBOUNCE_MS` | 300 | Brightness/contrast sliders |

---

## CI / CD

| Component | Choice |
|-----------|--------|
| Platform | GitHub Actions |
| Build trigger | Push / PR to `master` → unit tests |
| Release trigger | `workflow_dispatch` with `version` input |
| JDK on CI | 24 (Temurin) |
| Release artifact | `doc-scanner-{version}.apk` (debug build) |
| Release notes | Auto-generated from git log between tags |

**Commands:**
```bash
./gradlew testDebugUnitTest --no-daemon   # unit tests
./gradlew assembleDebug --no-daemon       # build APK
./gradlew kspDebugKotlin --no-daemon      # regenerate Room schema JSON
```

---

## Testing

| Layer | Location | Runner |
|-------|----------|--------|
| Business logic / pure functions | `src/test/` | JUnit 4 + Mockito |
| UI flows (fake repos) | `src/androidTest/` | Compose test + Espresso |
| DB migration | `src/androidTest/` | Room `MigrationTestHelper` |

**Rules:**
- No Android platform types (`Bitmap`, `Context`) in `src/test/` — extract pure logic
- No backtick method names with spaces in `src/androidTest/` (DEX 040 / API < 34 limit)
- Fake repositories implement full `DocumentRepository` interface

---

## Prohibited Patterns

- ❌ Hardcoded constants — all go in `AppConfig`
- ❌ Blocking I/O (`BitmapFactory.decodeFile`, `File.exists`) on main thread or in Composables
- ❌ N+1 DB updates in loops — use bulk `@Update` DAO methods
- ❌ `&&` in `calcInSampleSize` while-loop — must be `||`
- ❌ `SimpleDateFormat` as a shared singleton — not thread-safe
- ❌ `fallbackToDestructiveMigration()` — wipes user data
- ❌ `ddl-auto: create/update` equivalent — use `Migrations.kt`
- ❌ Third-party PDF library — raw PDF 1.4 is sufficient
- ❌ Hilt / Dagger — manual DI via `AppContainer`
- ❌ External storage without explicit user action (privacy)
