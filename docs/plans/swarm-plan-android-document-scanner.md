# Swarm Plan: Android Document Scanner v1

<!--
Swarm Execution Plan
Filename: docs/plans/swarm-plan-android-document-scanner.md
Generated from: /swarm-plan
Inputs: docs/adr/ + docs/specs/spec-android-document-scanner.md
-->

## Overview

**Status:** Ready for Execution
**Date:** 2026-06-28
**Total Tasks:** 52
**Estimated Effort:** 27–30 days (solo) · 10–12 days (3 parallel builders)
**Spikes (uncertain scope):** 4 (T-10, T-21, T-25, T-19)

**Related Docs:**
- Spec: [spec-android-document-scanner](../specs/spec-android-document-scanner.md)
- Plan: [plan-android-document-scanner](plan-android-document-scanner.md)
- ADR-0001: [0001-document-scanning-approach](../adr/0001-document-scanning-approach.md)
- ADR-0002: [0002-app-architecture-and-storage](../adr/0002-app-architecture-and-storage.md)

---

## Exploration Summary

Three parallel workers analyzed the spec, ADRs, and existing plan. Key findings:

| Finding | Detail |
|---------|--------|
| **Critical path bottleneck** | Tasks 1.2 → 1.4 → 2.1 → 2.4 → 2.5 block ALL ViewModels — protect this sequence |
| **Highest fan-out blocker** | T-12 (UseCases): 5 ViewModels blocked until complete |
| **Gaps found** | 13 items from spec with no assigned implementation task (added as T-42–T-52) |
| **Technical spikes** | 4 tasks with uncertain scope — spike before committing |
| **Parallelization window** | Days 5–15: all 4 ViewModels + 4 Screens + 2 scanning paths run in parallel |
| **Test coverage needed** | 10 unit test suites + 10 integration test scenarios |

---

## Task List

### Phase 1 — Project Setup (Days 1–2)

#### T-00: Project Structure (do this first, before any other task)

Create the full package/directory skeleton. Every subsequent task drops files into these folders — agree on the layout once, never debate it again.

```
app/src/main/
├── AndroidManifest.xml
└── java/com/docscanner/
    │
    ├── MyApplication.kt                  ← registers AppContainer
    ├── MainActivity.kt                   ← single-activity host
    │
    ├── di/
    │   └── AppContainer.kt               ← manual DI: holds all singletons
    │
    ├── domain/
    │   ├── model/
    │   │   ├── Document.kt
    │   │   └── Page.kt
    │   └── usecase/
    │       ├── GetDocumentsUseCase.kt
    │       ├── SaveDocumentUseCase.kt
    │       ├── ExportPdfUseCase.kt
    │       ├── DeleteDocumentUseCase.kt
    │       └── RenameDocumentUseCase.kt
    │
    ├── data/
    │   ├── repository/
    │   │   ├── DocumentRepository.kt     ← interface
    │   │   └── DocumentRepositoryImpl.kt
    │   ├── local/
    │   │   ├── db/
    │   │   │   ├── AppDatabase.kt
    │   │   │   ├── DocumentDao.kt
    │   │   │   └── PageDao.kt
    │   │   ├── entity/
    │   │   │   ├── DocumentEntity.kt
    │   │   │   └── PageEntity.kt
    │   │   └── filesystem/
    │   │       ├── ImageStorage.kt
    │   │       └── ThumbnailGenerator.kt
    │   └── pdf/
    │       └── PdfGenerator.kt
    │
    ├── ui/
    │   ├── AppNavGraph.kt                ← Navigation routes
    │   ├── common/
    │   │   ├── PermissionHandler.kt
    │   │   ├── StorageChecker.kt
    │   │   └── GmsChecker.kt
    │   ├── theme/
    │   │   ├── Theme.kt
    │   │   ├── Color.kt
    │   │   └── Type.kt
    │   ├── documentlist/
    │   │   ├── DocumentListScreen.kt
    │   │   ├── DocumentListViewModel.kt
    │   │   └── components/
    │   │       ├── DocumentCard.kt
    │   │       ├── RenameDialog.kt
    │   │       └── DeleteConfirmDialog.kt
    │   ├── scanner/
    │   │   ├── ScannerScreen.kt
    │   │   ├── ScannerViewModel.kt
    │   │   ├── MlKitScannerLauncher.kt
    │   │   └── components/
    │   │       ├── ManualCropScreen.kt
    │   │       ├── CornerDragOverlay.kt
    │   │       ├── CameraPreview.kt
    │   │       └── PermissionRationaleScreen.kt
    │   ├── edit/
    │   │   ├── EditScreen.kt
    │   │   ├── EditViewModel.kt
    │   │   └── ImageProcessor.kt
    │   ├── viewer/
    │   │   ├── DocumentViewerScreen.kt
    │   │   ├── DocumentViewerViewModel.kt
    │   │   └── components/
    │   │       ├── ReorderableThumbnailList.kt
    │   │       ├── ExportPdfHandler.kt
    │   │       └── MissingPagePlaceholder.kt
    │   └── settings/
    │       └── SettingsScreen.kt
    │
    └── common/
        └── exceptions/
            ├── PageLimitException.kt
            ├── DocumentLimitException.kt
            ├── StorageFullException.kt
            ├── DocumentNameException.kt
            └── ExportException.kt

app/src/main/res/
├── xml/
│   └── file_paths.xml                    ← FileProvider paths
└── values/
    ├── strings.xml
    └── themes.xml

app/src/test/java/com/docscanner/
├── domain/usecase/                       ← unit tests
└── data/

app/src/androidTest/java/com/docscanner/
└── ui/                                   ← UI / integration tests
```

**Filesystem layout at runtime (`context.filesDir`):**
```
filesDir/
  documents/
    {documentId}/
      thumbnail.jpg            ← 256×256 max, JPEG 50%
      pages/
        page_001.jpg           ← JPEG 85%, max 2480×3508 px
        page_001_1719561600000.jpg  ← after edit (timestamp suffix)

cacheDir/
  export/                      ← ephemeral; cleaned on app background
    {documentId}_{timestamp}.pdf
    page_{uuid}.jpg
```

**Navigation routes:**
```
document_list          ← start destination
scanner/{documentId}   ← "new" = create doc; UUID = add page to existing
edit/{documentId}/{pageIndex}
viewer/{documentId}
settings
```

**Est: 0.5d · Deliverable: all empty package directories + placeholder `TODO` files so the project compiles from day 1**

---

| ID | Task | Est. | Spike | Deliverable |
|----|------|------|-------|-------------|
| T-01 | Create Android project (Kotlin, AGP 8.x, `minSdk=26`, `compileSdk=35`) | 0.5d | No | `build.gradle.kts` (project + app), `settings.gradle.kts` |
| T-02 | Add all Gradle dependencies (Compose BOM, CameraX, ML Kit, Room, Coil, Coroutines) | 0.5d | No | `app/build.gradle.kts` with pinned versions |
| T-03 | Compose theme, typography, color tokens, `AppNavGraph` with 4 routes | 1d | No | `Theme.kt`, `Color.kt`, `AppNavGraph.kt`, `MainActivity.kt` |
| T-04 | Room schema — `DocumentEntity`, `PageEntity`, `AppDatabase` (version 1, no destructive migration) | 0.5d | No | Entities + `AppDatabase.kt` + KSP config |
| T-05 | FileProvider config — `file_paths.xml`, `AndroidManifest.xml` authority + permissions | 0.5d | No | `res/xml/file_paths.xml`, manifest declaration |
| T-06 | `AppContainer` (manual DI) + `MyApplication` singleton initialization | 0.5d | No | `di/AppContainer.kt`, `MyApplication.kt` |

**Phase 1 total: 4 days (including T-00)**

---

### Phase 2 — Data Layer (Days 3–4)

| ID | Task | Est. | Spike | Deliverable |
|----|------|------|-------|-------------|
| T-07 | `DocumentDao` + `PageDao` (CRUD, `Flow<List>`, cascade delete, index on `updated_at DESC`) | 1d | No | `DocumentDao.kt`, `PageDao.kt` |
| T-08 | `ImageStorage` — save/load/delete page images; downscale to 2480×3508, JPEG 85%; timestamp-suffixed filenames for edited pages | 1d | No | `ImageStorage.kt` |
| T-09 | Thumbnail generation — JPEG 50%, max 256×256 px; regenerate on first-page edit or reorder | 0.5d | No | `ThumbnailGenerator.kt` (or part of `ImageStorage`) |
| T-10 | `PdfGenerator` — A4 canvas 595×842 pt, 10 pt margins, pages in order, batch processing, recycle canvas per page | 2d | **Yes** | `PdfGenerator.kt` using `android.graphics.pdf.PdfDocument` |
| T-11 | `DocumentRepository` interface + `DocumentRepositoryImpl` composing DAOs + ImageStorage + PdfGenerator | 1d | No | `DocumentRepository.kt`, `DocumentRepositoryImpl.kt` |
| T-12 | Domain models (`Document`, `Page`) + all 5 use cases + custom exceptions | 1d | No | `domain/model/`, `domain/usecase/`, `common/exceptions/` |

> T-08 must implement timestamp-suffixed filenames (`page_001_{updatedAt}.jpg`) to prevent Coil serving stale cached images after edit-save. Old file is deleted; Room `PageEntity.imagePath` updated to new path.

**Phase 2 total: 6.5 days (T-10 spike: allocate 2 days; validate PDF output quality on day 1)**

---

### Phase 3 — Document List Screen (Days 5–6)

| ID | Task | Est. | Spike | Deliverable |
|----|------|------|-------|-------------|
| T-13 | `DocumentListViewModel` — collect `GetDocumentsUseCase` Flow, handle delete/rename/export actions, track document limit state | 1d | No | `DocumentListViewModel.kt` with `DocumentListUiState` |
| T-14 | `DocumentListScreen` — `LazyVerticalGrid` (2 col), `DocumentCard`, FAB, empty state, long-press context menu, Settings nav | 1d | No | `DocumentListScreen.kt`, `DocumentCard.kt` |
| T-15 | Rename dialog (1–50 chars, forbidden char regex, whitespace strip, same-name skip) + Delete confirmation dialog | 0.5d | No | `RenameDialog.kt`, `DeleteConfirmDialog.kt` |

**Phase 3 total: 2.5 days**

---

### Phase 4 — Camera / Scanner Screen (Days 7–10)

| ID | Task | Est. | Spike | Deliverable |
|----|------|------|-------|-------------|
| T-16 | GMS availability check utility + storage check utility (`StatFs`, < 50 MB blocking, < 100 MB warning) | 0.5d | No | `GmsChecker.kt`, `StorageChecker.kt` |
| T-17 | `CameraPreview` composable — `AndroidView(PreviewView)`, lifecycle-aware, `CAMERA` permission request | 0.5d | No | `CameraPreview.kt` |
| T-18 | `PermissionRationaleScreen` — explanation + "Open Settings" (`ACTION_APPLICATION_DETAILS_SETTINGS`) | 0.5d | No | `PermissionRationaleScreen.kt` |
| T-19 | ML Kit Document Scanner integration — `GmsDocumentScannerOptions`, `ActivityResultLauncher`, JPEG URI result handling; detect GMS update required (EC-4) | 1d | **Yes** | `MlKitScannerLauncher.kt` + EC-4 dialog |
| T-20 | Manual corner crop screen — CameraX `ImageCapture`, 4 `DraggableHandle` composables, perspective transform via `android.graphics.Matrix`, Retake button | 2d | **Yes** | `ManualCropScreen.kt`, `CornerDragOverlay.kt`, `PerspectiveTransform.kt` |
| T-21 | `ScannerViewModel` — GMS/manual routing, `SavePageUseCase` call, storage check before launch, navigation on completion, `SavedStateHandle` for `capturedBitmap` (EC-9) | 1d | No | `ScannerViewModel.kt` |

**Phase 4 total: 5.5 days (T-20 is the second biggest spike)**

---

### Phase 5 — Image Edit Screen (Days 11–13)

| ID | Task | Est. | Spike | Deliverable |
|----|------|------|-------|-------------|
| T-22 | `ImageProcessor` — immutable Bitmap pipeline: rotate 90° CW, brightness (`ColorMatrix`), contrast (`ColorMatrix`), grayscale (`ColorMatrix`); all on IO dispatcher | 1d | **Yes** | `ImageProcessor.kt` (pure suspend functions) |
| T-23 | `EditViewModel` — undo stack (max 5, clear to 1 on `TRIM_MEMORY_MODERATE`), `hasUnsavedChanges`, `isProcessing`, save-to-disk action with thumbnail regen if page 1 | 1d | No | `EditViewModel.kt` with `EditUiState` |
| T-24 | `EditScreen` — image preview (65% height), rotate button, brightness/contrast sliders, grayscale toggle, undo button, Save, discard confirmation on Back | 1d | No | `EditScreen.kt` |

**Phase 5 total: 3 days**

---

### Phase 6 — Document Viewer Screen (Days 14–15)

| ID | Task | Est. | Spike | Deliverable |
|----|------|------|-------|-------------|
| T-25 | `DocumentViewerViewModel` — load document + pages, `addPage`, `deletePage` (with delete-doc-if-last logic EC-8), `reorderPages` (atomic Room transaction + thumbnail regen), `exportPdf` | 1d | No | `DocumentViewerViewModel.kt` with `DocumentViewerUiState` |
| T-26 | `DocumentViewerScreen` — `HorizontalPager`, page indicator, thumbnail strip, FAB (disabled at 50 pages), top-bar menu (Export PDF), per-page three-dot menu (Edit, Export as image, Delete) | 1d | No | `DocumentViewerScreen.kt` |
| T-27 | Drag-to-reorder thumbnail strip — `ReorderableLazyRow` with `rememberReorderableLazyListState`, single Room transaction on drop | 0.5d | No | `ReorderableThumbnailList.kt` |
| T-28 | PDF export flow — async, progress dialog, `ExportPdfUseCase`, FileProvider share intent (`application/pdf`), error Snackbar | 0.5d | No | `ExportPdfHandler.kt` |
| T-29 | Missing-page placeholder (EC-5) — file-existence check per page, placeholder composable, disabled Edit/Export, banner | 0.5d | No | `MissingPagePlaceholder.kt`, existence check in ViewModel |
| T-30 | Gallery import (FR-10 Should Have) — `PickVisualMedia`, MIME validation (JPEG/PNG only), FR-3 pipeline | 1d | No | Gallery import action in `DocumentViewerViewModel` |
| T-31 | Single-page image export (FR-11 Should Have) — copy JPEG to `cacheDir/export/`, share via FileProvider (`image/jpeg`) | 0.5d | No | Page export action in ViewModel + share intent |

**Phase 6 total: 5 days**

---

### Phase 7 — Settings, Cleanup & Polish (Days 16–17)

| ID | Task | Est. | Spike | Deliverable |
|----|------|------|-------|-------------|
| T-32 | `SettingsScreen` — app version, document count, total storage used (`filesDir/documents/`), uninstall warning banner | 0.5d | No | `SettingsScreen.kt` |
| T-33 | `ProcessLifecycleOwner` observer — delete `cacheDir/export/` when app moves to background | 0.5d | No | `ExportCacheCleanup.kt` registered in `MyApplication` |
| T-34 | Startup cache cleanup — on `AppContainer` init, delete files in `cacheDir/export/` with `lastModified > 10 min ago` (EC-3) | 0.5d | No | Startup cleanup logic in `AppContainer.kt` |
| T-35 | Global error handling polish — storage full Snackbar, GMS error Snackbar, export failure Snackbar, IO error logging (file paths redacted) | 0.5d | No | Consistent error handling in all ViewModels |

**Phase 7 total: 2 days**

---

### Phase 8 — Tests (Days 18–20)

| ID | Task | Est. | Spike | Deliverable |
|----|------|------|-------|-------------|
| T-36 | Unit tests — `SavePageUseCase`: page limit (50), storage block (< 50 MB), downscale, JPEG 85% | 0.5d | No | `SavePageUseCaseTest.kt` |
| T-37 | Unit tests — `DocumentRepository`: rename validation (forbidden chars, length, same-name skip, whitespace), doc limit (100), delete file-error resilience | 0.5d | No | `DocumentRepositoryTest.kt` |
| T-38 | Unit tests — `ImageProcessor`: rotate 4×, brightness/contrast bounds, grayscale toggle, immutability | 0.5d | No | `ImageProcessorTest.kt` |
| T-39 | Unit tests — `PdfGenerator`: page order, A4 dimensions, margins, OOM guard (> 20 pages), output file path | 0.5d | No | `PdfGeneratorTest.kt` |
| T-40 | Unit tests — storage check: exactly 100 MB (EC-10 boundary), < 50 MB blocking, > 100 MB allowed | 0.5d | No | `StorageCheckerTest.kt` |
| T-41 | Unit tests — thumbnail: 256×256 cap, JPEG 50%, aspect ratio, regeneration on page-1 change | 0.5d | No | `ThumbnailGeneratorTest.kt` |
| T-42 | Integration tests — scan flow (GMS: cancel, success, error), manual crop flow, document appears in list | 0.5d | No | `ScannerFlowTest.kt` |
| T-43 | Integration tests — page reorder atomicity, thumbnail regen on reorder, HorizontalPager reflects new order | 0.5d | No | `PageReorderTest.kt` |
| T-44 | Integration tests — delete last page (EC-8), missing page placeholder (EC-5), gallery import MIME validation | 0.5d | No | `EdgeCaseTest.kt` |
| T-45 | UI tests — Document List (empty state, FAB disabled at 100, rename/delete dialogs, long-press menu) | 0.5d | No | `DocumentListScreenTest.kt` |
| T-46 | UI tests — Edit screen (rotate, sliders, undo depth, discard dialog, save navigates back) | 0.5d | No | `EditScreenTest.kt` |

**Phase 8 total: 5.5 days**

---

### Phase 9 — Final Validation (Days 21)

| ID | Task | Est. | Spike | Deliverable |
|----|------|------|-------|-------------|
| T-47 | Manual testing — scan on 5 background types, 10-page PDF, edit cycle, share PDF to Gmail/Zalo | 0.5d | No | Completed manual test checklist |
| T-48 | Non-GMS test — GMS-free emulator (API 26), confirm manual crop flow end-to-end | 0.5d | No | Confirmed fallback behavior |
| T-49 | APK size analysis — `Build > Analyze APK`, must be < 30 MB | 0.5d | No | Screenshot of APK analyzer + ProGuard/R8 rules |
| T-50 | Performance validation — cold start < 2s, page processing < 3s, PDF export < 5s on mid-range device | 0.5d | No | Measurements logged |
| T-51 | Security validation — confirm `INTERNET` permission absent, `file://` URIs absent, FileProvider correct | 0.5d | No | Manifest audit results |
| T-52 | Privacy validation — confirm no file paths in logs, Auto Backup disabled for `filesDir/documents/` | 0.5d | No | Logcat audit + backup rules XML |

**Phase 9 total: 3 days**

---

## File Registry

Every file touched in this project. Legend: **CREATE** = file không tồn tại, tạo mới. **UPDATE** = file đã có, chỉnh sửa thêm.

### Config & Build

| File | Action | Task | Notes |
|------|--------|------|-------|
| `build.gradle.kts` (project) | **CREATE** | T-01 | AGP version, Kotlin version, KSP plugin |
| `build.gradle.kts` (app) | **CREATE** | T-01, T-02 | T-01 tạo skeleton; T-02 thêm toàn bộ dependencies |
| `settings.gradle.kts` | **CREATE** | T-01 | App module declaration, repository config |
| `gradle.properties` | **CREATE** | T-01 | `android.useAndroidX=true`, `kotlin.code.style=official` |
| `AndroidManifest.xml` | **CREATE** | T-01 | T-01 tạo; T-05 thêm FileProvider + permissions |
| `AndroidManifest.xml` | **UPDATE** | T-05 | Thêm `<provider>` FileProvider, `CAMERA` permission |
| `res/xml/file_paths.xml` | **CREATE** | T-05 | FileProvider path config (`files-path`, `cache-path`) |
| `res/values/strings.xml` | **CREATE** | T-03 | App name + UI strings |
| `res/values/themes.xml` | **CREATE** | T-03 | Material3 theme base |
| `proguard-rules.pro` | **UPDATE** | T-49 | Thêm rules cho Room, ML Kit, Coil khi validate APK size |

### Entry Points

| File | Action | Task | Notes |
|------|--------|------|-------|
| `MyApplication.kt` | **CREATE** | T-06 | Khởi tạo `AppContainer`; đăng ký `ProcessLifecycleOwner` cleanup |
| `MyApplication.kt` | **UPDATE** | T-33 | Thêm `ExportCacheCleanup` observer |
| `MainActivity.kt` | **CREATE** | T-03 | Single-activity host; set content = `AppNavGraph` |

### DI

| File | Action | Task | Notes |
|------|--------|------|-------|
| `di/AppContainer.kt` | **CREATE** | T-06 | Manual singleton container: DB, Repository, UseCases |
| `di/AppContainer.kt` | **UPDATE** | T-34 | Thêm startup cache cleanup call trong `init` block |

### Theme & Navigation

| File | Action | Task | Notes |
|------|--------|------|-------|
| `ui/theme/Theme.kt` | **CREATE** | T-03 | `MaterialTheme` wrapper, dark/light support |
| `ui/theme/Color.kt` | **CREATE** | T-03 | Color tokens |
| `ui/theme/Type.kt` | **CREATE** | T-03 | Typography scale |
| `ui/AppNavGraph.kt` | **CREATE** | T-03 | `NavHost` với 5 routes: `document_list`, `scanner/{documentId}`, `edit/{documentId}/{pageIndex}`, `viewer/{documentId}`, `settings` |

### Domain Layer

| File | Action | Task | Notes |
|------|--------|------|-------|
| `domain/model/Document.kt` | **CREATE** | T-12 | Pure Kotlin data class: `id`, `name`, `createdAt`, `updatedAt`, `pageCount`, `thumbnailPath` |
| `domain/model/Page.kt` | **CREATE** | T-12 | Pure Kotlin data class: `id`, `documentId`, `pageNumber`, `imagePath`, `createdAt` |
| `domain/usecase/GetDocumentsUseCase.kt` | **CREATE** | T-12 | Returns `Flow<List<Document>>` từ Repository |
| `domain/usecase/SaveDocumentUseCase.kt` | **CREATE** | T-12 | Tạo doc mới hoặc thêm page; enforce Rule 1 (50 pages), Rule 2 (100 docs), Rule 3 (storage), Rule 5 (auto-name) |
| `domain/usecase/ExportPdfUseCase.kt` | **CREATE** | T-12 | Gọi `PdfGenerator`; trả về `File` trong `cacheDir/export/` |
| `domain/usecase/DeleteDocumentUseCase.kt` | **CREATE** | T-12 | Xóa files + Room rows; tiếp tục nếu file đã mất |
| `domain/usecase/RenameDocumentUseCase.kt` | **CREATE** | T-12 | Validate Rule 5; skip Room write nếu tên không đổi (EC-7) |
| `common/exceptions/PageLimitException.kt` | **CREATE** | T-12 | Thrown khi pageCount ≥ 50 |
| `common/exceptions/DocumentLimitException.kt` | **CREATE** | T-12 | Thrown khi documentCount ≥ 100 |
| `common/exceptions/StorageFullException.kt` | **CREATE** | T-12 | Thrown khi free storage < 50 MB |
| `common/exceptions/DocumentNameException.kt` | **CREATE** | T-12 | Thrown khi tên vi phạm Rule 5 |
| `common/exceptions/ExportException.kt` | **CREATE** | T-12 | Wraps `IOException` trong export flow |

### Data Layer — Room

| File | Action | Task | Notes |
|------|--------|------|-------|
| `data/local/entity/DocumentEntity.kt` | **CREATE** | T-04 | `@Entity(tableName="documents")`; UUID id, name, createdAt, updatedAt, pageCount, thumbnailPath |
| `data/local/entity/PageEntity.kt` | **CREATE** | T-04 | `@Entity(tableName="pages")`; FK → documents ON DELETE CASCADE |
| `data/local/db/AppDatabase.kt` | **CREATE** | T-04 | `@Database(version=1)`; không dùng `fallbackToDestructiveMigration` |
| `data/local/db/DocumentDao.kt` | **CREATE** | T-07 | `getAllDocuments(): Flow<List>`, insert, delete, rename, count |
| `data/local/db/PageDao.kt` | **CREATE** | T-07 | insert, deleteByDocumentId, getPagesByDocumentId, updatePageNumbers (batch reorder) |

### Data Layer — Filesystem

| File | Action | Task | Notes |
|------|--------|------|-------|
| `data/local/filesystem/ImageStorage.kt` | **CREATE** | T-08 | Lưu Bitmap → JPEG 85%, downscale ≤ 2480×3508; timestamp-suffix filenames (`page_001_{updatedAt}.jpg`); xóa file cũ khi update |
| `data/local/filesystem/ThumbnailGenerator.kt` | **CREATE** | T-09 | Scale → max 256×256, JPEG 50%; gọi lại khi page 1 thay đổi |

### Data Layer — PDF

| File | Action | Task | Notes |
|------|--------|------|-------|
| `data/pdf/PdfGenerator.kt` | **CREATE** | T-10 | `android.graphics.pdf.PdfDocument`; A4 (595×842 pt), 10 pt margin; process pages sequentially; `page.close()` sau mỗi trang; output → `cacheDir/export/` |

### Data Layer — Repository

| File | Action | Task | Notes |
|------|--------|------|-------|
| `data/repository/DocumentRepository.kt` | **CREATE** | T-11 | Interface: `getAllDocuments()`, `createDocument()`, `addPage()`, `updatePage()`, `reorderPages()`, `deleteDocument()`, `renameDocument()`, `exportPdf()`, `exportPage()` |
| `data/repository/DocumentRepositoryImpl.kt` | **CREATE** | T-11 | Impl: tổng hợp DAOs + ImageStorage + ThumbnailGenerator + PdfGenerator |

### UI — Common Utilities

| File | Action | Task | Notes |
|------|--------|------|-------|
| `ui/common/GmsChecker.kt` | **CREATE** | T-16 | `GoogleApiAvailability.isGooglePlayServicesAvailable()` → Boolean |
| `ui/common/StorageChecker.kt` | **CREATE** | T-16 | `StatFs.availableBytes`; trả về `SUFFICIENT / WARNING / BLOCKED` |
| `ui/common/PermissionHandler.kt` | **CREATE** | T-18 | `rememberPermissionState(CAMERA)`; rationale dialog logic |
| `ui/common/ExportCacheCleanup.kt` | **CREATE** | T-33 | `DefaultLifecycleObserver.onStop()` → xóa `cacheDir/export/` |

### UI — Document List

| File | Action | Task | Notes |
|------|--------|------|-------|
| `ui/documentlist/DocumentListViewModel.kt` | **CREATE** | T-13 | `StateFlow<DocumentListUiState>`; actions: delete, rename, exportPdf; tracks `documentLimitReached` |
| `ui/documentlist/DocumentListScreen.kt` | **CREATE** | T-14 | `LazyVerticalGrid`, FAB disabled khi ≥ 100 docs, long-press menu, empty state, Settings nav |
| `ui/documentlist/components/DocumentCard.kt` | **CREATE** | T-14 | Thumbnail (`AsyncImage`), name, date, page count badge |
| `ui/documentlist/components/RenameDialog.kt` | **CREATE** | T-15 | Pre-filled text field, inline validation error, whitespace strip, same-name skip |
| `ui/documentlist/components/DeleteConfirmDialog.kt` | **CREATE** | T-15 | "Cannot be undone" message, Cancel + Delete buttons |

### UI — Scanner

| File | Action | Task | Notes |
|------|--------|------|-------|
| `ui/scanner/ScannerScreen.kt` | **CREATE** | T-19 | Entry point: check GMS → route to ML Kit hoặc ManualCropScreen; check storage trước khi launch |
| `ui/scanner/ScannerViewModel.kt` | **CREATE** | T-21 | Route GMS/manual; gọi `SaveDocumentUseCase`; `SavedStateHandle` cho `capturedBitmap` (EC-9) |
| `ui/scanner/MlKitScannerLauncher.kt` | **CREATE** | T-19 | `GmsDocumentScannerOptions` config; `ActivityResultLauncher`; EC-4 dialog khi GMS cần update |
| `ui/scanner/components/CameraPreview.kt` | **CREATE** | T-17 | `AndroidView { PreviewView }`, lifecycle-aware CameraX binding |
| `ui/scanner/components/ManualCropScreen.kt` | **CREATE** | T-20 | CameraX `ImageCapture`; hiện preview sau khi chụp; Confirm/Retake |
| `ui/scanner/components/CornerDragOverlay.kt` | **CREATE** | T-20 | 4 `DraggableHandle` composable; minimum tap area 48×48 dp; cập nhật real-time |
| `ui/scanner/components/PerspectiveTransform.kt` | **CREATE** | T-20 | `android.graphics.Matrix` perspective warp; thuần Kotlin, không OpenCV |
| `ui/scanner/components/PermissionRationaleScreen.kt` | **CREATE** | T-18 | Full-screen: giải thích + "Open Settings" button |

### UI — Edit

| File | Action | Task | Notes |
|------|--------|------|-------|
| `ui/edit/EditViewModel.kt` | **CREATE** | T-23 | Undo stack ≤ 5; xóa về 1 khi `TRIM_MEMORY_MODERATE`; thumbnail regen nếu page 1 |
| `ui/edit/EditScreen.kt` | **CREATE** | T-24 | Preview 65% height; rotate, brightness/contrast sliders, grayscale toggle, undo, Save, discard dialog |
| `ui/edit/ImageProcessor.kt` | **CREATE** | T-22 | Pure suspend functions; immutable Bitmap pipeline: `rotateBitmap`, `adjustBrightness`, `adjustContrast`, `toGrayscale` |

### UI — Document Viewer

| File | Action | Task | Notes |
|------|--------|------|-------|
| `ui/viewer/DocumentViewerViewModel.kt` | **CREATE** | T-25 | Load doc + pages; `addPage`, `deletePage` (EC-8: delete last → delete doc), `reorderPages` (atomic tx + thumbnail regen), `exportPdf` |
| `ui/viewer/DocumentViewerScreen.kt` | **CREATE** | T-26 | `HorizontalPager`, page indicator, thumbnail strip, FAB disabled ≥ 50 pages, top-bar Export menu, per-page three-dot menu |
| `ui/viewer/components/ReorderableThumbnailList.kt` | **CREATE** | T-27 | `ReorderableLazyRow`; single Room transaction khi drop |
| `ui/viewer/components/ExportPdfHandler.kt` | **CREATE** | T-28 | Progress dialog; `ExportPdfUseCase`; FileProvider share intent; error Snackbar |
| `ui/viewer/components/MissingPagePlaceholder.kt` | **CREATE** | T-29 | Placeholder composable (grey + broken-image icon); banner cảnh báo; Edit/Export bị disable |

### UI — Settings

| File | Action | Task | Notes |
|------|--------|------|-------|
| `ui/settings/SettingsScreen.kt` | **CREATE** | T-32 | App version, document count, storage used, uninstall warning banner |

### Test Files

| File | Action | Task | Notes |
|------|--------|------|-------|
| `test/.../SavePageUseCaseTest.kt` | **CREATE** | T-36 | Page limit, storage block, downscale, JPEG quality |
| `test/.../DocumentRepositoryTest.kt` | **CREATE** | T-37 | Rename validation, doc limit, delete resilience |
| `test/.../ImageProcessorTest.kt` | **CREATE** | T-38 | Rotate 4×, brightness/contrast bounds, grayscale, immutability |
| `test/.../PdfGeneratorTest.kt` | **CREATE** | T-39 | Page order, A4 dims, margins, OOM guard |
| `test/.../StorageCheckerTest.kt` | **CREATE** | T-40 | Exactly 100 MB boundary (EC-10), < 50 MB blocking |
| `test/.../ThumbnailGeneratorTest.kt` | **CREATE** | T-41 | 256×256 cap, JPEG 50%, aspect ratio, regen on page-1 change |
| `androidTest/.../ScannerFlowTest.kt` | **CREATE** | T-42 | GMS cancel/success/error, manual crop end-to-end |
| `androidTest/.../PageReorderTest.kt` | **CREATE** | T-43 | Atomic reorder, thumbnail regen, pager reflects new order |
| `androidTest/.../EdgeCaseTest.kt` | **CREATE** | T-44 | EC-5 missing page, EC-8 delete last page, gallery MIME reject |
| `androidTest/.../DocumentListScreenTest.kt` | **CREATE** | T-45 | Empty state, FAB disabled at 100, dialogs, long-press |
| `androidTest/.../EditScreenTest.kt` | **CREATE** | T-46 | Rotate, sliders, undo depth, discard dialog, save |

---

### Summary: Files by Action

| Action | Count |
|--------|-------|
| **CREATE** | 55 files |
| **UPDATE** | 3 files (`AndroidManifest.xml`, `MyApplication.kt`, `di/AppContainer.kt`, `proguard-rules.pro`) |
| **Total** | 58 files |

> Không có file nào bị **DELETE** — greenfield project.

---

## Dependency Graph

```
PHASE 1 — Setup (Days 1–2)
══════════════════════════════════════════════════════
T-01 (Project)
  └─► T-02 (Dependencies)
        ├─► T-03 (Theme + Nav)
        ├─► T-04 (Room Schema) ◄── CRITICAL
        ├─► T-05 (FileProvider)
        └─► T-06 (AppContainer)

PHASE 2 — Data Layer (Days 3–4) ◄── ALL VIEWMODELS BLOCKED UNTIL T-12
══════════════════════════════════════════════════════
T-04 ─► T-07 (DAOs)    ◄── CRITICAL
T-02 ─► T-08 (ImageStorage)  ┐
T-02 ─► T-09 (Thumbnails)    ├─► T-11 (Repository)
T-02 ─► T-10 (PdfGenerator)  ┘     └─► T-12 (UseCases + Domain) ◄── UNLOCK POINT
T-05 ─► T-08
T-07 ─► T-11

PHASES 3–6 — Screens (Days 5–15) ← all unblock after T-12
══════════════════════════════════════════════════════
                    T-12
                    ├─► T-13 (DocListVM)  ─► T-14 (DocListScreen) ─► T-15 (Dialogs)
                    │
                    ├─► T-21 (ScannerVM)  ◄── also needs T-16, T-19, T-20
                    │     T-16 (GMS+Storage check) ┐
                    │     T-17 (CameraPreview)      ├─► T-19 (ML Kit) ─┐
                    │     T-18 (Permission screen)   │                   ├─► T-21
                    │     T-20 (Manual crop) ◄───────┘                  │
                    │                                                     │
                    ├─► T-23 (EditVM) ◄── also needs T-22 (ImageProc)
                    │     T-22 (ImageProcessor) ─► T-23 ─► T-24 (EditScreen)
                    │
                    └─► T-25 (ViewerVM)
                          └─► T-26 (ViewerScreen)
                                ├─► T-27 (Reorder)
                                ├─► T-28 (PDF export flow)
                                ├─► T-29 (Missing page placeholder)
                                ├─► T-30 (Gallery import)
                                └─► T-31 (Image export)

PHASE 7 — Polish (Days 16–17)  ← can start after Phases 3-6 done
══════════════════════════════════════════════════════
T-32 (Settings)       — depends on T-12
T-33 (Background cleanup) — depends on T-28
T-34 (Startup cleanup) — depends on T-06
T-35 (Error polish)   — depends on all Phases 3-6

PHASE 8 — Tests (Days 18–20)
══════════════════════════════════════════════════════
T-36..T-41 (Unit tests)      — depend on T-07..T-12
T-42..T-44 (Integration)     — depend on Phases 3-6
T-45..T-46 (UI tests)        — depend on Phases 3-6

PHASE 9 — Validation (Day 21)
══════════════════════════════════════════════════════
T-47..T-52 — depend on all Phases 3-8
```

---

## Critical Path

```
T-01 → T-02 → T-04 → T-07 → T-11 → T-12
  |             └──────────────────────────┘
  └─► T-02 → T-08 → T-11 (parallel with T-07)
```

**Minimum calendar time through critical path: ~4 days before any UI work can start.**

The critical path exits at **T-12 (Use Cases)**. After T-12:
- 4 ViewModels (T-13, T-21, T-23, T-25) can start in parallel
- 4 Scanner sub-tasks (T-16–T-20) can start in parallel after T-02

**Do not start any ViewModel before T-12 is complete and code-reviewed.**

---

## Parallelization Windows

### Window A — Days 3–4 (Phase 2 internal parallelism)

| Stream | Tasks |
|--------|-------|
| Stream 1 | T-07 (DAOs) → T-11 (Repository) |
| Stream 2 | T-08 (ImageStorage) + T-09 (Thumbnails) |
| Stream 3 | T-10 (PdfGenerator) spike |

T-10 is the spike — start it on Day 3 alongside T-07. If it takes longer than 2 days, T-11 can still be scaffolded with a stub `PdfGenerator`.

### Window B — Days 5–10 (Phases 3–4 parallel)

| Stream | Tasks |
|--------|-------|
| Stream 1 | T-13 → T-14 → T-15 (Document List) |
| Stream 2 | T-16 + T-17 + T-18, then T-19 → T-21 (Scanner GMS path) |
| Stream 3 | T-20 (Manual crop spike) |
| Stream 4 | T-22 → T-23 → T-24 (Edit, can start early with stub ViewModel) |

### Window C — Days 11–15 (Phases 5–6 parallel)

| Stream | Tasks |
|--------|-------|
| Stream 1 | T-25 → T-26 → T-27 + T-28 + T-29 (Viewer) |
| Stream 2 | T-30 + T-31 (Gallery import + image export, Should Have) |
| Stream 3 | T-32 + T-33 + T-34 (Settings + cleanup) |

### Window D — Days 18–20 (Tests run in parallel)

All T-36..T-46 can run in parallel across unit, integration, and UI test suites.

---

## Risk Register

| ID | Risk | Likelihood | Impact | Mitigation |
|----|------|-----------|--------|------------|
| R-01 | ML Kit GMS `getStartScanIntent()` error handling (EC-4) not prototyped | M | H | 1-day spike on Day 7 before committing to T-19 full build |
| R-02 | `android.graphics.Matrix` perspective transform produces unacceptable quality | M | H | Prototype in T-20 spike; if quality insufficient, evaluate OpenCV (ADR-0001 Option 2) |
| R-03 | `PdfDocument` OOM on > 20 pages on low-RAM devices | M | M | Process pages sequentially; call `page.close()` immediately; test on 2 GB device |
| R-04 | Real-time `ColorMatrix` brightness/contrast > 500 ms on high-res Bitmap | M | M | Downscale preview Bitmap to max 800 px for edit; save original resolution on Save |
| R-05 | Coil serving stale image after edit despite timestamp-suffix strategy | L | M | Integration test T-43 must verify stale cache does not appear after edit-save |
| R-06 | ML Kit Document Scanner API v16.0.0-beta1 breaking changes | L | H | Pin version; monitor release notes; fallback plan: manual crop only |
| R-07 | Manual crop UX unusable on low-end device touch screens | M | M | Test corner drag on budget device; add minimum handle tap area 48×48 dp |
| R-08 | Undo stack memory pressure (5 × 25 MB = 125 MB) on < 2 GB RAM device | M | H | Reduce stack to 3 on devices with `ActivityManager.isLowRamDevice()` |
| R-09 | Startup cache cleanup (`cacheDir/export/` scan) slows cold start | L | L | Run cleanup on background coroutine, not on main thread |
| R-10 | Room schema migration errors in future versions | L | H | Write explicit `Migration` objects from day 1; never use `fallbackToDestructiveMigration` in release builds |

---

## Gaps Addressed (from spec analysis)

These 11 items from the spec had no explicit implementation task — they are now assigned:

| Gap | Assigned To |
|-----|-------------|
| EC-3: Orphan cache cleanup on launch | T-34 |
| EC-4: GMS update detection flow | T-19 |
| EC-5: Missing page file placeholder | T-29 |
| EC-7: Skip Room write on same-name rename | T-15 (rename dialog logic) |
| EC-9: `SavedStateHandle` for `capturedBitmap` | T-21 (ScannerViewModel) |
| Rule 2: FAB disabled tooltip at 100 docs | T-14 (DocumentListScreen) |
| Rule 3: Storage check before scan launch | T-16 + T-21 |
| Rule 5: Auto-naming in Repository/UseCase | T-12 (SaveDocumentUseCase) |
| Rule 6: Thumbnail regen on page-1 edit | T-23 (EditViewModel) |
| Rule 6: Thumbnail regen on page reorder | T-25 (DocumentViewerViewModel) |
| Caching: Timestamp-suffix filenames | T-08 (ImageStorage) |
| FR-9: `ProcessLifecycleOwner` PDF cleanup | T-33 |

---

## Spike Outcomes Required Before Commitment

| Spike | Task | Must Prove | If Fails |
|-------|------|------------|----------|
| PDF generation | T-10 | 10-page PDF renders < 5s; no OOM on 10-page doc on 2 GB device | Use streaming/batch approach; consider iText7 (AGPL risk: new ADR) |
| Manual corner crop | T-20 | Acceptable perspective transform quality; drag UX usable on 5-inch screen | Evaluate limited OpenCV (< 10 MB subset) or accept accuracy loss |
| ML Kit GMS error handling | T-19 | `getStartScanIntent()` error returns `UserRequiresAction`; fallback dialog works | Route all non-GMS paths to manual crop unconditionally |
| Image processing performance | T-22 | `ColorMatrix` brightness on 2480×3508 Bitmap < 500 ms on mid-range device | Downscale edit preview to 800 px max; apply to full-res only on Save |

---

## Execution Handoff

**For `/swarm-execute`:**

Start with Phase 1 (T-01 through T-06) as a single builder.
After T-12 (Use Cases) is complete and code-reviewed, fan out:

```
Builder A: Phase 3 (Document List) → Phase 7 cleanup tasks
Builder B: Phase 4 (Camera/Scanner) — owns both GMS + manual spikes
Builder C: Phase 5 (Edit) + Phase 6 (Viewer)
```

All builders converge on Phase 8 (Tests) and Phase 9 (Validation).

**Recommended spike order (first 4 days):**
1. Day 1–2: T-01 → T-02 → T-04 → T-05 → T-06 (Phase 1 complete)
2. Day 3: Start T-07 (DAOs) + T-10 (PdfGenerator spike) in parallel
3. Day 4: T-08 + T-09 + T-11 (if T-10 spike passes); finish T-12 (UseCases)
4. Day 5 morning: Review T-12 contracts before any ViewModel begins

---

## Definition of Done

A task is complete when:
- [ ] Code compiles with no errors
- [ ] `./gradlew lint` has no new errors
- [ ] Relevant unit tests pass
- [ ] The specific acceptance criteria from the spec for this task are met
- [ ] No hardcoded file paths or magic strings

The feature is done when:
- [ ] All T-01 through T-52 complete
- [ ] `./gradlew test` passes
- [ ] APK size < 30 MB (T-49)
- [ ] Manual test checklist complete (T-47)
- [ ] All 32 acceptance criteria in spec checked off

---

## Version History

| Date | Author | Change |
|------|--------|--------|
| 2026-06-28 | nguyenhuuca@gmail.com | Initial swarm plan from parallel worker analysis |
