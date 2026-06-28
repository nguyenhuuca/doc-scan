# Plan: Android Document Scanner — v1 Implementation

<!--
Implementation Plan
Filename: docs/plans/plan-android-document-scanner.md
Owner: Builder
Related PRD: docs/prd/PRD-android-document-scanner.md
Related ADR: docs/adr/0001-document-scanning-approach.md, docs/adr/0002-app-architecture-and-storage.md
-->

## Overview

**Status:** Draft
**Author:** nguyenhuuca@gmail.com
**Date:** 2026-06-28
**Beads Issue:** N/A
**Related PRD:** [PRD-android-document-scanner](../prd/PRD-android-document-scanner.md)
**Related ADR:** [ADR-0001](../adr/0001-document-scanning-approach.md) · [ADR-0002](../adr/0002-app-architecture-and-storage.md)

## Objective

Build the v1 Android Document Scanner app: scan documents via camera with automatic edge detection (ML Kit), basic image editing, multi-page PDF export, and document management in private local storage — fully offline.

## Scope

### In Scope

- Project setup: Kotlin + Jetpack Compose + MVVM + Room
- Camera module: CameraX + ML Kit Document Scanner (GMS path) + manual crop fallback
- Image editing module: rotate, crop, brightness/contrast, grayscale
- PDF export module: `android.graphics.pdf.PdfDocument`
- Document storage module: Room DB + private `filesDir`
- Screens: Document List, Camera/Scan, Edit, Document Viewer
- FileProvider configuration for export and sharing

### Out of Scope

- OCR, cloud upload, e-signature, direct sharing (v2+)
- Backend, authentication, networking
- Tablet-specific layouts

## Technical Approach

### Project Structure

```
app/
├── ui/
│   ├── documentlist/      ← DocumentListScreen + DocumentListViewModel
│   ├── scanner/           ← ScannerScreen + ScannerViewModel
│   ├── edit/              ← EditScreen + EditViewModel
│   ├── viewer/            ← DocumentViewerScreen + DocumentViewerViewModel
│   └── common/            ← Shared composables, theme, navigation
├── domain/
│   ├── model/             ← Document, Page (pure Kotlin data classes)
│   └── usecase/           ← ScanDocumentUseCase, SavePageUseCase,
│                             ExportPdfUseCase, GetDocumentsUseCase,
│                             DeleteDocumentUseCase, RenameDocumentUseCase
├── data/
│   ├── repository/        ← DocumentRepository (interface + impl)
│   ├── local/
│   │   ├── db/            ← Room: DocumentDao, PageDao, AppDatabase
│   │   ├── entity/        ← DocumentEntity, PageEntity
│   │   └── filesystem/    ← ImageStorage (filesDir management)
│   └── pdf/               ← PdfGenerator
└── di/                    ← Manual DI (AppContainer; no Hilt in v1)
```

### Key Decisions

| Decision | Rationale |
|----------|-----------|
| ML Kit GMS as primary scanning path | Accuracy > 90%, zero APK size increase (see ADR-0001) |
| Manual 4-corner fallback | Covers non-GMS devices (see ADR-0001) |
| `android.graphics.pdf.PdfDocument` | Zero APK cost; sufficient for image-only PDFs in v1 (see ADR-0002) |
| Room DB for metadata | Type-safe queries, migration support, offline-first |
| Private `filesDir` for images | Privacy by default; no storage permission needed (see ADR-0002) |
| Manual DI (no Hilt) | Solo developer; small app scope; avoids annotation processing overhead |
| Kotlin Coroutines + StateFlow | Non-blocking async I/O for file and Room operations |

## Implementation Steps

### Phase 1: Project Setup (Days 1–2)

- [ ] **1.1** Create a new Android project
  - Files: `build.gradle.kts` (app + project level)
  - Details: `minSdk = 26`, `compileSdk = 35`, `targetSdk = 35`; enable Jetpack Compose; Kotlin 2.x

- [ ] **1.2** Add dependencies to `app/build.gradle.kts`
  - Files: `app/build.gradle.kts`
  - Details:
    ```kotlin
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // CameraX
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")

    // ML Kit Document Scanner
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Coil (thumbnail image loading)
    implementation("io.coil-kt:coil-compose:2.7.0")
    ```

- [ ] **1.3** Set up Compose theme and Navigation graph
  - Files: `ui/theme/Theme.kt`, `ui/theme/Color.kt`, `MainActivity.kt`, `ui/AppNavGraph.kt`
  - Details: Four routes — `document_list`, `scanner/{documentId}`, `edit/{documentId}/{pageIndex}`, `viewer/{documentId}`

- [ ] **1.4** Create the Room database schema
  - Files: `data/local/db/AppDatabase.kt`, `data/local/entity/DocumentEntity.kt`, `data/local/entity/PageEntity.kt`
  - Details:
    ```kotlin
    @Entity(tableName = "documents")
    data class DocumentEntity(
        @PrimaryKey val id: String,       // UUID
        val name: String,
        val createdAt: Long,              // epoch ms
        val updatedAt: Long,
        val pageCount: Int,
        val thumbnailPath: String?
    )

    @Entity(tableName = "pages", foreignKeys = [ForeignKey(...)])
    data class PageEntity(
        @PrimaryKey val id: String,
        val documentId: String,
        val pageNumber: Int,              // 1-based
        val imagePath: String,
        val createdAt: Long
    )
    ```

- [ ] **1.5** Configure FileProvider for PDF export
  - Files: `AndroidManifest.xml`, `res/xml/file_paths.xml`
  - Details:
    ```xml
    <!-- file_paths.xml -->
    <paths>
      <files-path name="documents" path="documents/" />
      <cache-path name="export_cache" path="export/" />
    </paths>
    ```

- [ ] **1.6** Create AppContainer (manual DI)
  - Files: `di/AppContainer.kt`, `MyApplication.kt`
  - Details: Singleton container that initializes Room DB, Repository, and UseCases at app start

---

### Phase 2: Data Layer (Days 3–4)

- [ ] **2.1** Implement Room DAOs
  - Files: `data/local/db/DocumentDao.kt`, `data/local/db/PageDao.kt`
  - Details:
    ```kotlin
    @Dao interface DocumentDao {
        @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
        fun getAllDocuments(): Flow<List<DocumentEntity>>
        @Insert(onConflict = REPLACE) suspend fun insert(doc: DocumentEntity)
        @Delete suspend fun delete(doc: DocumentEntity)
        @Query("UPDATE documents SET name=:name WHERE id=:id")
        suspend fun rename(id: String, name: String)
    }
    ```

- [ ] **2.2** Implement ImageStorage
  - Files: `data/local/filesystem/ImageStorage.kt`
  - Details:
    ```kotlin
    class ImageStorage(private val filesDir: File) {
        suspend fun savePage(docId: String, pageNumber: Int, bitmap: Bitmap): String
        suspend fun saveThumbnail(docId: String, bitmap: Bitmap): String
        suspend fun deleteDocument(docId: String)   // deletes entire folder
        fun getPageBitmap(path: String): Bitmap?
    }
    ```
    Images stored at `filesDir/documents/{docId}/pages/page_NNN.jpg`

- [ ] **2.3** Implement PdfGenerator
  - Files: `data/pdf/PdfGenerator.kt`
  - Details: Use `android.graphics.pdf.PdfDocument`; draw each Bitmap onto an A4 page (595 × 842 pt) scaled to preserve aspect ratio; save to `cacheDir/export/`

- [ ] **2.4** Implement DocumentRepository
  - Files: `data/repository/DocumentRepository.kt`, `data/repository/DocumentRepositoryImpl.kt`
  - Details: Interface + implementation combining Room DAO, ImageStorage, and PdfGenerator

- [ ] **2.5** Implement domain models and use cases
  - Files: `domain/model/Document.kt`, `domain/model/Page.kt`
  - Files: `domain/usecase/GetDocumentsUseCase.kt`, `domain/usecase/SaveDocumentUseCase.kt`, `domain/usecase/ExportPdfUseCase.kt`, `domain/usecase/DeleteDocumentUseCase.kt`, `domain/usecase/RenameDocumentUseCase.kt`

---

### Phase 3: Document List Screen (Days 5–6)

- [ ] **3.1** DocumentListViewModel
  - Files: `ui/documentlist/DocumentListViewModel.kt`
  - Details: `StateFlow<DocumentListUiState>`; collect `GetDocumentsUseCase()` flow; handle delete and rename actions

- [ ] **3.2** DocumentListScreen composable
  - Files: `ui/documentlist/DocumentListScreen.kt`
  - Details:
    - `LazyVerticalGrid` (2 columns) with cards showing thumbnail, name, date, and page count
    - FAB "+" → navigate to Scanner
    - Long-press context menu: Rename, Delete, Export PDF
    - Empty state when no documents exist
    - Coil `AsyncImage` for thumbnails

- [ ] **3.3** Rename and delete dialogs
  - Files: `ui/documentlist/components/RenameDialog.kt`, `ui/documentlist/components/DeleteConfirmDialog.kt`

---

### Phase 4: Camera / Scanner Screen (Days 7–10)

- [ ] **4.1** GMS availability check utility
  - Files: `ui/scanner/GmsChecker.kt`
  - Details: `GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS`

- [ ] **4.2** ML Kit Document Scanner integration (GMS path)
  - Files: `ui/scanner/ScannerScreen.kt`, `ui/scanner/ScannerViewModel.kt`
  - Details:
    ```kotlin
    val options = GmsDocumentScannerOptions.Builder()
        .setScannerMode(SCANNER_MODE_FULL)
        .setGalleryImportAllowed(true)
        .setPageLimit(20)
        .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
        .build()
    val scanner = GmsDocumentScanning.getClient(options)
    scanner.getStartScanIntent(activity).addOnSuccessListener { intentSender ->
        scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
    }
    ```
    On result: list of `Uri` (JPEG pages) → call `SaveDocumentUseCase`

- [ ] **4.3** Manual corner crop screen (non-GMS fallback)
  - Files: `ui/scanner/ManualCropScreen.kt`, `ui/scanner/components/CornerDragOverlay.kt`
  - Details: CameraX `ImageCapture` → capture image → display preview with 4 draggable `DragHandle` composables at the document corners → apply perspective transform on confirm
  - Perspective transform: use `android.graphics.Matrix` (bilinear interpolation, no OpenCV)

- [ ] **4.4** CameraX preview (used for non-GMS path)
  - Files: `ui/scanner/components/CameraPreview.kt`
  - Details: `AndroidView { PreviewView }` inside Compose; lifecycle-aware; request `CAMERA` permission via `rememberPermissionState`

- [ ] **4.5** ScannerViewModel — handle scan results
  - Files: `ui/scanner/ScannerViewModel.kt`
  - Details: Receives list of `Uri` from ML Kit or `Bitmap` from manual crop → calls `SaveDocumentUseCase` → navigates to Document List with a success Snackbar

---

### Phase 5: Edit Screen (Days 11–13)

- [ ] **5.1** EditViewModel
  - Files: `ui/edit/EditViewModel.kt`
  - Details: Loads `PageEntity` → exposes `StateFlow<EditUiState>` with `currentBitmap` and `isProcessing`; actions: `rotate90`, `adjustBrightness`, `adjustContrast`, `toGrayscale`, `saveAndReturn`

- [ ] **5.2** Image processing utilities
  - Files: `ui/edit/ImageProcessor.kt`
  - Details: Pure Kotlin suspend functions operating on `Bitmap`:
    - `rotateBitmap(bitmap, degrees)` → `Matrix` rotation
    - `adjustBrightness(bitmap, value: Float)` → `ColorMatrix`
    - `adjustContrast(bitmap, value: Float)` → `ColorMatrix`
    - `toGrayscale(bitmap)` → `ColorMatrix` grayscale
    - All return a new `Bitmap` (immutable approach)

- [ ] **5.3** EditScreen composable
  - Files: `ui/edit/EditScreen.kt`
  - Details:
    - Image preview occupying ~two-thirds of the screen
    - Bottom toolbar with icons for rotate, brightness slider, contrast slider, and grayscale toggle
    - Save button → calls `EditViewModel.saveAndReturn()` → navigates back
    - Undo/Redo stack in ViewModel (list of Bitmap states, max 5 entries)

---

### Phase 6: Document Viewer Screen (Days 14–15)

- [ ] **6.1** DocumentViewerViewModel
  - Files: `ui/viewer/DocumentViewerViewModel.kt`
  - Details: Loads `Document` with pages → exposes pages list; actions: `addPage` (navigate to scanner), `deletePage`, `reorderPages`, `exportPdf`

- [ ] **6.2** DocumentViewerScreen composable
  - Files: `ui/viewer/DocumentViewerScreen.kt`
  - Details:
    - `HorizontalPager` for browsing pages
    - Bottom sheet with a horizontal thumbnail list supporting drag-to-reorder
    - FAB: Add Page (navigates to scanner)
    - Top menu: Export PDF, Share, Delete page, Edit page

- [ ] **6.3** Drag-to-reorder page thumbnails
  - Files: `ui/viewer/components/ReorderableThumbnailList.kt`
  - Details: `ReorderableLazyRow` with `rememberReorderableLazyListState`; on drop → calls `DocumentViewerViewModel.reorderPages(from, to)` → updates `pageNumber` fields in a single Room transaction

- [ ] **6.4** PDF export flow
  - Files: `ui/viewer/ExportPdfHandler.kt`
  - Details: Call `ExportPdfUseCase` → receive `File` path in `cacheDir` → create share Intent via `FileProvider.getUriForFile()` → launch Intent chooser

---

### Phase 7: Polish & Testing (Days 16–18)

- [ ] **7.1** Permission handling
  - Files: `ui/common/PermissionHandler.kt`
  - Details: `rememberPermissionState(CAMERA)`; show rationale dialog on denial; graceful error state if permanently denied

- [ ] **7.2** Error handling and edge cases
  - Details:
    - Storage full → catch `IOException`, show error Snackbar
    - GMS unavailable + camera permission denied → clear error state screen
    - Document with 0 pages → disable Export button
    - Image file deleted outside the app → handle `FileNotFoundException` in `ImageStorage`

- [ ] **7.3** Unit tests — domain and data layers
  - Files: `test/domain/usecase/ExportPdfUseCaseTest.kt`, `test/data/repository/DocumentRepositoryTest.kt`
  - Details: Mock `ImageStorage` and `DocumentDao` with fake implementations; test SaveDocument, ExportPdf, and DeleteDocument flows

- [ ] **7.4** UI tests — critical paths
  - Files: `androidTest/ui/DocumentListScreenTest.kt`, `androidTest/ui/EditScreenTest.kt`
  - Details: Compose UI tests: empty state, add document flow, rename/delete; edit screen rotation action

- [ ] **7.5** Manual testing checklist
  - [ ] Scan a document on white, dark, and patterned backgrounds
  - [ ] Scan 10 pages, export PDF, open in an external PDF viewer
  - [ ] Test on a GMS-free emulator (API 26) — confirm manual crop flow works
  - [ ] Check APK size < 30 MB (Build > Analyze APK)
  - [ ] Test on a mid-range device (< 3 GB RAM) — measure processing time
  - [ ] Uninstall and reinstall — confirm documents are gone (expected) with no crash

- [ ] **7.6** Settings screen with uninstall warning
  - Files: `ui/settings/SettingsScreen.kt`
  - Details: Simple settings screen with a prominent warning: "Documents are stored inside the app — uninstalling will delete everything. Export your PDFs before uninstalling."

---

## Files to Create

| File | Action | Description |
|------|--------|-------------|
| `app/build.gradle.kts` | Create | Dependencies, compileSdk, minSdk |
| `AndroidManifest.xml` | Create | CAMERA permission, FileProvider declaration |
| `res/xml/file_paths.xml` | Create | FileProvider path config |
| `di/AppContainer.kt` | Create | Manual DI container |
| `data/local/db/AppDatabase.kt` | Create | Room database |
| `data/local/entity/DocumentEntity.kt` | Create | Room entity |
| `data/local/entity/PageEntity.kt` | Create | Room entity |
| `data/local/db/DocumentDao.kt` | Create | Room DAO |
| `data/local/db/PageDao.kt` | Create | Room DAO |
| `data/local/filesystem/ImageStorage.kt` | Create | File I/O for page images |
| `data/pdf/PdfGenerator.kt` | Create | PDF creation |
| `data/repository/DocumentRepository.kt` | Create | Repository interface |
| `data/repository/DocumentRepositoryImpl.kt` | Create | Repository implementation |
| `domain/model/Document.kt` | Create | Domain model |
| `domain/model/Page.kt` | Create | Domain model |
| `domain/usecase/GetDocumentsUseCase.kt` | Create | Use case |
| `domain/usecase/SaveDocumentUseCase.kt` | Create | Use case |
| `domain/usecase/ExportPdfUseCase.kt` | Create | Use case |
| `domain/usecase/DeleteDocumentUseCase.kt` | Create | Use case |
| `domain/usecase/RenameDocumentUseCase.kt` | Create | Use case |
| `ui/documentlist/DocumentListScreen.kt` | Create | Main screen |
| `ui/documentlist/DocumentListViewModel.kt` | Create | ViewModel |
| `ui/scanner/ScannerScreen.kt` | Create | ML Kit + fallback scanning flow |
| `ui/scanner/ScannerViewModel.kt` | Create | ViewModel |
| `ui/scanner/ManualCropScreen.kt` | Create | Non-GMS manual crop fallback |
| `ui/edit/EditScreen.kt` | Create | Image edit screen |
| `ui/edit/EditViewModel.kt` | Create | ViewModel |
| `ui/edit/ImageProcessor.kt` | Create | Bitmap operations |
| `ui/viewer/DocumentViewerScreen.kt` | Create | Multi-page document viewer |
| `ui/viewer/DocumentViewerViewModel.kt` | Create | ViewModel |
| `ui/settings/SettingsScreen.kt` | Create | Settings + uninstall warning |

## Dependencies

### Code Dependencies

| Package | Version | Purpose |
|---------|---------|---------|
| `androidx.compose:compose-bom` | 2024.12.01 | Compose UI framework |
| `androidx.navigation:navigation-compose` | 2.8.4 | Screen navigation |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.7 | ViewModel in Compose |
| `androidx.camera:camera-camera2` | 1.4.1 | Camera HAL |
| `androidx.camera:camera-lifecycle` | 1.4.1 | Lifecycle-aware camera |
| `androidx.camera:camera-view` | 1.4.1 | PreviewView |
| `com.google.android.gms:play-services-mlkit-document-scanner` | 16.0.0-beta1 | Edge detection (GMS path) |
| `androidx.room:room-runtime` | 2.6.1 | Local database |
| `androidx.room:room-ktx` | 2.6.1 | Coroutine extensions for Room |
| `io.coil-kt:coil-compose` | 2.7.0 | Thumbnail image loading |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.9.0 | Async operations |

### Service Dependencies

| Service | Status | Notes |
|---------|--------|-------|
| Google Play Services | Required for GMS path | Manual crop fallback for non-GMS devices |
| Local filesystem | Always available | Private app storage |

## Testing Strategy

### Unit Tests

| Component | Test Cases | Status |
|-----------|------------|--------|
| `ExportPdfUseCase` | Single page, multi-page, empty document | Pending |
| `SaveDocumentUseCase` | Successful save, IO error handling | Pending |
| `ImageProcessor` | Rotate 90°/180°/270°, grayscale output, brightness bounds | Pending |
| `DocumentRepositoryImpl` | CRUD operations with fake DAOs | Pending |
| `DocumentListViewModel` | Load documents, delete, rename state transitions | Pending |

### Integration Tests

| Scenario | Expected Outcome | Status |
|----------|------------------|--------|
| Scan → Save → List | Document appears in list with thumbnail | Pending |
| Edit page → Save → View | Edited image persisted correctly | Pending |
| Export PDF → Share intent | Valid PDF created; share chooser opens | Pending |
| Non-GMS path → Manual crop → Save | Document saved successfully from manual crop | Pending |

### Manual Testing

- [ ] Scan a document on 5 different background types — record accuracy
- [ ] Export a 20-page PDF; verify size and readability
- [ ] Full rotate / grayscale / brightness edit cycle
- [ ] Share PDF to Gmail and Zalo
- [ ] Test on API 26 emulator (minimum supported) and API 35 (target)

## Rollback Plan

1. Greenfield project — no previous state to restore
2. If ML Kit API breaks (beta API): pin to previous version or migrate to OpenCV (see ADR-0001 Option 2)
3. If `PdfDocument` proves insufficient: evaluate iText7 Community (AGPL) or write a new ADR

## Risks

| Risk | Mitigation |
|------|------------|
| ML Kit Document Scanner API is still in beta (`16.0.0-beta1`) — potential breaking changes | Pin the version; test thoroughly before each release |
| Manual corner crop is more complex than estimated | 2-day spike (Step 4.3) before committing to the design |
| `PdfDocument` OOM with documents > 20 pages | Process pages in batches; close and recycle each page's Canvas immediately after rendering |
| CameraX compatibility issues on certain vendor ROMs (Xiaomi, Oppo) | Test on real devices; use Camera2Interop to override settings if needed |

## Checklist

### Before Starting

- [ ] ADR-0001 and ADR-0002 approved
- [ ] Android Studio Ladybug (2024.2+) installed
- [ ] JDK 17+ available
- [ ] Android SDK API 35 + API 26 emulator ready

### Before Build / PR

- [ ] `./gradlew test` — all unit tests pass
- [ ] `./gradlew lint` — no errors (warnings acceptable)
- [ ] APK size analyzed < 30 MB (Build > Analyze APK)
- [ ] Manual testing checklist complete
- [ ] No hardcoded paths or magic strings

### Before v1.0 Release

- [ ] Tested on at least 3 physical devices (Samsung, Xiaomi, mid-range device)
- [ ] Non-GMS path tested on a GMS-free emulator
- [ ] Privacy review: confirm zero network calls
- [ ] ProGuard/R8 rules verified for Room and ML Kit

## Notes

- **ML Kit Document Scanner API** (`16.0.0-beta1`) launches a separate Activity and returns JPEG URIs — it does not expose a camera preview for customization. If real-time overlay or auto-capture is required, the approach must shift to CameraX + OpenCV (see ADR-0001).
- **Undo/Redo** in EditScreen: keep up to 5 Bitmap states in memory (~5 × 5 MB = 25 MB RAM). Reduce to 3 states under memory pressure.
- **Page ordering** is stored via `pageNumber` in Room. Reordering updates all `pageNumber` values in a single transaction.

---

## Progress Log

| Date | Update |
|------|--------|
| 2026-06-28 | Plan created from PRD + ADR-0001 + ADR-0002 |
