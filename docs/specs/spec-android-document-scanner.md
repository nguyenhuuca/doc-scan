# Feature Specification: Android Document Scanner

<!--
Feature Specification
Filename: docs/specs/spec-android-document-scanner.md
Owner: Builder
Handoff to: Builder (/builder), QA Engineer (/qa-engineer)

Purpose: Defines exact system behavior, screen contracts, data layer contracts,
         and acceptance criteria for the Android Document Scanner app v1.
         Dev reads this and implements. QA reads this and tests.
         No ambiguity allowed.

Related:
- PRD:     docs/prd/PRD-android-document-scanner.md
- ADR-001: docs/adr/0001-document-scanning-approach.md
- ADR-002: docs/adr/0002-app-architecture-and-storage.md
- Plan:    docs/plans/plan-android-document-scanner.md
-->

## Metadata

**Status:** Draft
**Author:** nguyenhuuca@gmail.com
**Date:** 2026-06-28
**Related PRD:** [PRD-android-document-scanner](../prd/PRD-android-document-scanner.md)
**Related ADR:** [ADR-0001](../adr/0001-document-scanning-approach.md) · [ADR-0002](../adr/0002-app-architecture-and-storage.md)

---

## Overview

The Android Document Scanner app allows individuals and students to digitize paper documents using their phone camera. Users can scan one or more pages, apply basic image corrections (rotation, brightness, contrast, grayscale), combine pages into a single PDF, and manage their documents in private local storage. The app operates entirely offline with no account required.

---

## Business Rules

### Rule 1 — Maximum Pages per Document

A document must not exceed **50 pages**. The Add Page action is disabled once a document reaches 50 pages. Attempting to programmatically bypass this limit must be rejected by the `SavePageUseCase` with an `IllegalStateException`.

### Rule 2 — Maximum Documents on Device

The app must not allow creation of more than **100 documents**. When the document count reaches 100, the FAB (create new document) is disabled and shows a tooltip: *"Document limit reached (100). Delete existing documents to create new ones."*

### Rule 3 — Storage Warning Threshold

Before starting a scan (either GMS path or manual path), the app must check available free storage. If free storage is **< 100 MB**, display a warning dialog:
- *"Low storage: less than 100 MB available. Your scan may fail to save. Free up space and try again."*
- The user can dismiss the warning and proceed, or cancel.

If free storage is **< 50 MB**, scanning is **blocked** entirely (not just warned):
- Show a blocking dialog: *"Not enough storage. Free up at least 50 MB to scan."*
- No scan is allowed to start until space is freed.

### Rule 4 — Image Quality on Save

Every scanned page image must be saved as **JPEG at 85% quality**. If the captured image exceeds **2480 × 3508 pixels** (A4 at 300 DPI), it must be downscaled to fit within those bounds while preserving aspect ratio. This applies to both GMS-path results and manual-crop results.

### Rule 5 — Document Naming

- A new document is auto-named **"Document {YYYY-MM-DD HH:mm}"** using the device's local time at creation.
- Renamed documents must have a name of **1–50 characters**.
- Names must not contain `/`, `\`, `:`, `*`, `?`, `"`, `<`, `>`, or `|` (characters that conflict with filesystem paths).
- Leading and trailing whitespace is stripped before saving.

> **Assumption:** Duplicate document names are allowed — documents are identified by UUID, not by name.

### Rule 6 — Thumbnail Generation

The first page of a document is always used as the document's thumbnail. The thumbnail must be saved as a JPEG at **50% quality**, scaled to a maximum of **256 × 256 px** while preserving aspect ratio. If the first page is replaced or deleted, the thumbnail must be regenerated from the new first page.

### Rule 7 — PDF Export Output

Exported PDFs embed pages in order of `pageNumber`. Each page is rendered on an **A4 canvas (595 × 842 pt)**, centered, scaled to fit while preserving aspect ratio with a **10 pt margin** on all sides. The exported PDF is written to `cacheDir/export/{documentId}_{timestamp}.pdf` and shared via FileProvider. The cache export file is deleted when the app is backgrounded after sharing completes.

### Rule 8 — Data Locality

No document data (images, PDF files, metadata) may be transmitted over a network connection. The app must not request INTERNET permission. All operations are local.

---

## Functional Requirements

### FR-1: Camera Scanning — GMS Path

When Google Play Services is available, the app **must** launch the ML Kit Document Scanner via `GmsDocumentScannerOptions` with the following fixed configuration:
- `setScannerMode(SCANNER_MODE_FULL)`
- `setGalleryImportAllowed(true)`
- `setPageLimit(50)`
- `setResultFormats(RESULT_FORMAT_JPEG)`

The app **must** handle `ActivityResultLauncher` results: on success, process all returned JPEG URIs; on cancellation, return to the Document List without saving; on error, display a Snackbar with *"Scan failed. Please try again."*

### FR-2: Camera Scanning — Manual Crop Path (Non-GMS Fallback)

When Google Play Services is unavailable, the app **must** fall back to a manual crop screen:
1. Launch CameraX `ImageCapture` for single-shot capture.
2. After capture, display the full image with 4 draggable corner handles (one at each corner of the detected or estimated document boundary).
3. The user drags corners to define the document boundary, then taps **Confirm**.
4. Apply a perspective transform using `android.graphics.Matrix` to produce a rectangular crop.
5. Proceed to the edit/preview step with the cropped image.

The app **must** detect GMS availability before launching any scanning screen and route accordingly.

### FR-3: Page Image Processing

After scan capture (either path), the app **must**:
1. Downscale the image if it exceeds 2480 × 3508 px (see Rule 4).
2. Compress to JPEG at 85% quality.
3. Save to `filesDir/documents/{documentId}/pages/page_{NNN}.jpg` (zero-padded 3-digit page number).
4. Generate a thumbnail for the document (see Rule 6).
5. Insert a `PageEntity` row into Room and update the parent `DocumentEntity` (`pageCount`, `updatedAt`, `thumbnailPath`).

All file I/O and Room writes **must** run on the IO dispatcher (not the Main dispatcher).

### FR-4: Image Editing

The edit screen **must** provide the following operations on a scanned page image:
- **Rotate**: 90° clockwise per tap; 4 taps returns to original orientation.
- **Brightness**: slider from –100 to +100; default 0; applied via `ColorMatrix`.
- **Contrast**: slider from –100 to +100; default 0; applied via `ColorMatrix`.
- **Grayscale**: toggle; converts image to grayscale via `ColorMatrix`; can be toggled off to revert (within the current edit session only).
- **Undo**: revert to the previous state; maximum undo depth of **5 steps**.

Each operation **must** produce a new `Bitmap` without mutating the previous state (immutable pipeline). The original saved image is **not** modified until the user explicitly taps **Save**.

Tapping **Save** must overwrite the existing `page_{NNN}.jpg` on disk, regenerate the document thumbnail if this is page 1, and update `DocumentEntity.updatedAt` in Room.

### FR-5: Document List

The Document List screen **must**:
- Load and display all documents ordered by `updatedAt` descending.
- Show each document as a card with: thumbnail, name, creation date (`createdAt` formatted as `MMM dd, yyyy`), and page count.
- Show an **empty state** illustration and message *"No documents yet. Tap + to scan your first document."* when the list is empty.
- Support **long-press** on a card to show a context menu: **Rename**, **Delete**, **Export PDF**.
- The FAB **must** be disabled (not hidden) when the document count is ≥ 100.

### FR-6: Document Rename

Rename **must**:
1. Show a dialog with a pre-filled text field containing the current name.
2. Validate: 1–50 chars, no forbidden characters (Rule 5). Show inline error on violation.
3. Strip leading/trailing whitespace before saving.
4. On confirm, update `DocumentEntity.name` and `updatedAt` in Room.
5. Dismiss the dialog and reflect the new name immediately in the list.

### FR-7: Document Delete

Delete **must**:
1. Show a confirmation dialog: *"Delete '{name}'? This cannot be undone."* with **Cancel** and **Delete** buttons.
2. On confirm: delete all page image files (`filesDir/documents/{documentId}/`), delete all `PageEntity` rows for this document, delete the `DocumentEntity` row — all in a single coroutine scope.
3. If any file deletion fails (e.g., file already missing), log the error and continue — do not abort the Room deletion.
4. The document must disappear from the list immediately after the database operation completes (Room Flow auto-updates).

### FR-8: Page Reorder

In the Document Viewer, the page thumbnail strip **must** support drag-to-reorder. After a successful reorder:
- Update `pageNumber` for all affected `PageEntity` rows in a single Room transaction.
- Update `DocumentEntity.updatedAt`.
- If page 1 changed, regenerate the thumbnail.
- The `HorizontalPager` must reflect the new order without requiring a full screen reload.

### FR-9: PDF Export

PDF export **must**:
1. Be triggered from the Document Viewer top menu or the Document List long-press menu.
2. Run asynchronously on the IO dispatcher; show a non-dismissable progress dialog while in progress.
3. Render pages in `pageNumber` order onto A4 pages (595 × 842 pt), centered with 10 pt margins.
4. Write the output to `cacheDir/export/{documentId}_{System.currentTimeMillis()}.pdf`.
5. On success: dismiss the progress dialog and launch a system share chooser via `FileProvider.getUriForFile()` with MIME type `application/pdf`.
6. On failure: dismiss the progress dialog and show a Snackbar: *"Export failed. Please try again."*
7. Delete the temporary PDF from `cacheDir/export/` when the app moves to the background (via `ProcessLifecycleOwner`).

### FR-10: Import from Gallery (Should Have)

From the Document Viewer, the **Add Page** menu **must** include an option *"Choose from gallery"*. This launches a system image picker (`ActivityResultContracts.PickVisualMedia`). The selected image is processed through the same pipeline as a scanned image (FR-3): downscale, compress, save, update Room. Only JPEG and PNG images may be selected; other formats **must** be rejected with a Snackbar: *"Unsupported file type. Please select a JPEG or PNG image."*

### FR-11: Single-Page Image Export (Should Have)

From the Document Viewer page menu (three-dot menu per page), **Export as image** **must** copy the page's JPEG file to `cacheDir/export/page_{UUID}.jpg` and share it via a system share chooser with MIME type `image/jpeg`.

### FR-12: Settings Screen

A Settings screen (accessible from the Document List top-bar menu) **must** display:
- App version number.
- Total document count and total storage used by `filesDir/documents/`.
- A warning banner: *"⚠️ Your documents are stored inside the app. Uninstalling will permanently delete all documents. Export important documents as PDFs before uninstalling."*

---

## Data Layer Contracts

### Room Database — Schema

```sql
-- Table: documents
CREATE TABLE documents (
    id             TEXT    PRIMARY KEY NOT NULL,   -- UUID string
    name           TEXT    NOT NULL,
    created_at     INTEGER NOT NULL,               -- epoch milliseconds
    updated_at     INTEGER NOT NULL,
    page_count     INTEGER NOT NULL DEFAULT 0,
    thumbnail_path TEXT                            -- nullable; absolute path
);

-- Table: pages
CREATE TABLE pages (
    id          TEXT    PRIMARY KEY NOT NULL,      -- UUID string
    document_id TEXT    NOT NULL,
    page_number INTEGER NOT NULL,                  -- 1-based, unique per document
    image_path  TEXT    NOT NULL,                  -- absolute path to JPEG
    created_at  INTEGER NOT NULL,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_pages_document_id ON pages(document_id);
CREATE INDEX idx_documents_updated_at ON documents(updated_at DESC);
```

**Room database version:** 1
**Migration strategy:** explicit `Migration` objects only; `fallbackToDestructiveMigration` must never be used in production builds.

### Repository Interface Contract

```kotlin
interface DocumentRepository {
    // Emits on every change (Room Flow)
    fun getAllDocuments(): Flow<List<Document>>

    // Returns null if not found
    suspend fun getDocumentWithPages(documentId: String): DocumentWithPages?

    // Creates document + first page atomically; throws StorageFullException if < 50 MB free
    suspend fun createDocument(name: String, pages: List<Bitmap>): Document

    // Appends a page; throws PageLimitException if pageCount >= 50
    suspend fun addPage(documentId: String, image: Bitmap): Page

    // Updates page image on disk + Room
    suspend fun updatePage(page: Page, image: Bitmap)

    // Atomic reorder: updates all pageNumber values in one transaction
    suspend fun reorderPages(documentId: String, orderedPageIds: List<String>)

    // Deletes document folder + all Room rows
    suspend fun deleteDocument(documentId: String)

    // Updates name field; throws DocumentNameException if name invalid
    suspend fun renameDocument(documentId: String, newName: String)

    // Returns File in cacheDir/export/; throws ExportException on failure
    suspend fun exportPdf(documentId: String): File

    // Returns File in cacheDir/export/; page-specific export
    suspend fun exportPage(page: Page): File
}
```

### Custom Exceptions

| Exception | Thrown when |
|-----------|-------------|
| `PageLimitException` | Adding a page to a document that already has 50 pages |
| `DocumentLimitException` | Creating a document when 100 already exist |
| `StorageFullException` | Free storage < 50 MB when attempting to save an image |
| `DocumentNameException` | Name fails Rule 5 validation |
| `ExportException` | PDF or image export fails (wraps the underlying IOException) |

### Filesystem Layout

```
filesDir/
  documents/
    {documentId}/              ← one folder per document
      thumbnail.jpg            ← 256x256 max, JPEG 50% quality
      pages/
        page_001.jpg           ← JPEG 85%, max 2480x3508 px
        page_002.jpg
        ...

cacheDir/
  export/                      ← ephemeral; cleaned on app background
    {documentId}_{timestamp}.pdf
    page_{uuid}.jpg
```

---

## Screen Contracts

### Screen 1: Document List (`DocumentListScreen`)

**Route:** `document_list` (start destination)

| Element | Behavior |
|---------|----------|
| `LazyVerticalGrid` (2 cols) | Displays `DocumentCard` per document, ordered by `updatedAt` DESC |
| `DocumentCard` | Shows thumbnail, name, date (`MMM dd, yyyy`), page count badge |
| FAB "+" | Navigates to `scanner/new`; disabled + tooltip when documentCount ≥ 100 |
| Long-press on card | Bottom sheet: Rename, Delete, Export PDF |
| Empty state | Shown when `documents.isEmpty()`; illustration + message |
| Top-bar menu | Settings icon → navigates to `settings` |

**UiState:**
```kotlin
data class DocumentListUiState(
    val documents: List<Document> = emptyList(),
    val isLoading: Boolean = false,
    val documentLimitReached: Boolean = false   // true when count >= 100
)
```

---

### Screen 2: Scanner (`ScannerScreen`)

**Route:** `scanner/{documentId}` — `documentId = "new"` creates a new document after scan

| Element | Behavior |
|---------|----------|
| GMS check on launch | If GMS available → launch ML Kit scanner; else → show `ManualCropScreen` |
| ML Kit result: success | Call `createDocument` or `addPage` depending on route param; navigate to `viewer/{documentId}` |
| ML Kit result: cancelled | Navigate back to Document List; no save |
| ML Kit result: error | Show Snackbar "Scan failed. Please try again."; stay on current screen |
| Low storage (< 100 MB) | Show warning dialog before launching scanner |
| Blocked storage (< 50 MB) | Show blocking dialog; do not launch scanner |
| Camera permission denied | Show `PermissionRationaleScreen` (see FR below) |

---

### Screen 3: Manual Crop (`ManualCropScreen`)

**Route:** Internal, no separate route — shown within `ScannerScreen` composable when GMS is unavailable

| Element | Behavior |
|---------|----------|
| CameraX preview | Full-screen `PreviewView` |
| Capture button | Triggers `ImageCapture.takePicture()` |
| After capture | Preview image shown with 4 `DraggableHandle` composables at estimated document corners |
| Drag handles | User drags to adjust boundary; minimum bounding box 100 × 100 px |
| Confirm button | Applies perspective transform via `Matrix`; proceeds to add page to document |
| Retake button | Returns to camera preview; discards captured image |

---

### Screen 4: Edit (`EditScreen`)

**Route:** `edit/{documentId}/{pageIndex}`

| Element | Behavior |
|---------|----------|
| Image preview | Shows `currentBitmap` from ViewModel state; fills ~65% of screen height |
| Rotate button | Applies 90° CW rotation; recorded in undo stack |
| Brightness slider | Range –100 to +100; default 0; updates preview in real-time |
| Contrast slider | Range –100 to +100; default 0; updates preview in real-time |
| Grayscale toggle | On/off; recorded in undo stack |
| Undo button | Reverts to previous state; disabled when undo stack is empty |
| Save button | Writes current bitmap to disk (see FR-4); shows progress indicator; navigates back |
| Back (without save) | Pops backstack; shows discard confirmation dialog if changes exist |

**UiState:**
```kotlin
data class EditUiState(
    val currentBitmap: Bitmap? = null,
    val isProcessing: Boolean = false,
    val canUndo: Boolean = false,
    val hasUnsavedChanges: Boolean = false
)
```

---

### Screen 5: Document Viewer (`DocumentViewerScreen`)

**Route:** `viewer/{documentId}`

| Element | Behavior |
|---------|----------|
| `HorizontalPager` | Swipe between pages; shows `AsyncImage` for each page |
| Page indicator | "Page X of Y" text below pager |
| Bottom thumbnail strip | Horizontal scrollable list of page thumbnails; supports drag-to-reorder |
| FAB "Add page" | Opens bottom sheet: "Scan new page" or "Choose from gallery"; disabled when pageCount ≥ 50 |
| Top-bar menu | Export PDF, Document Info (name, page count, size) |
| Per-page menu (three-dot) | Edit page, Export as image, Delete page |
| Delete page (single) | Confirmation dialog; if deleting last page → delete entire document and navigate to Document List |

**UiState:**
```kotlin
data class DocumentViewerUiState(
    val document: Document? = null,
    val pages: List<Page> = emptyList(),
    val currentPageIndex: Int = 0,
    val isExporting: Boolean = false,
    val pageLimitReached: Boolean = false   // true when pageCount >= 50
)
```

---

### Screen 6: Permission Rationale (`PermissionRationaleScreen`)

Shown when `CAMERA` permission is permanently denied (user tapped "Don't ask again").

| Element | Behavior |
|---------|----------|
| Illustration + explanation | "Camera access is required to scan documents." |
| "Open Settings" button | Launches `ACTION_APPLICATION_DETAILS_SETTINGS` for this app |
| "Go Back" button | Navigates to Document List |

This screen is **not** a separate route — it is a full-screen composable rendered inside `ScannerScreen` based on permission state.

---

## Security Requirements

### Permissions

| Permission | Required | Justification |
|------------|----------|---------------|
| `android.permission.CAMERA` | Yes | Camera scanning (both GMS and manual paths) |
| `android.permission.READ_MEDIA_IMAGES` (API 33+) | Yes (Should Have — FR-10 only) | Gallery import |
| `android.permission.READ_EXTERNAL_STORAGE` (API ≤ 32) | Yes (Should Have — FR-10 only) | Gallery import on older devices |

**Must not request:**
- `INTERNET` — no network access in v1
- `WRITE_EXTERNAL_STORAGE` — private storage requires no write permission
- `READ_EXTERNAL_STORAGE` — not needed unless gallery import (FR-10) is implemented

### Data Validation

| Field | Rule | Exception |
|-------|------|-----------|
| Document name | 1–50 chars; no `/\:*?"<>|`; non-empty after trim | `DocumentNameException` |
| Page image | Must be non-null Bitmap; width and height > 0 | `IllegalArgumentException` |
| Page number | Must be 1-based positive integer; must not duplicate within document | `IllegalArgumentException` |
| Gallery import file | MIME type must be `image/jpeg` or `image/png` | Show Snackbar; do not throw |

### FileProvider

The `FileProvider` authority must be `{applicationId}.fileprovider`. Exported files must be served only from `cacheDir/export/`. The `android:grantUriPermissions="true"` attribute is required. Direct file paths must never be exposed in share Intents — only `content://` URIs via FileProvider.

### Sensitive Data

- Document thumbnails and page images must reside in `filesDir` (not `cacheDir` or external storage) so they are excluded from Android Auto Backup by default.
- No document metadata or content may appear in system logs. `Timber` (or equivalent) log statements must redact file paths to avoid leaking document names in production builds.

> **Assumption:** Android Auto Backup is disabled for `filesDir/documents/` via `android:allowBackup="false"` or a custom backup rules XML to prevent documents being synced to Google Drive without the user's explicit knowledge.

---

## Caching Impact

### Coil Image Cache

Coil is used for thumbnail loading in `DocumentListScreen` and `DocumentViewerScreen`.

| Cache | Impact | Required Action |
|-------|--------|----------------|
| Coil memory cache | Reads thumbnail Bitmaps | When a document is renamed: no action needed (URI unchanged) |
| Coil disk cache | Caches decoded thumbnails by URI | When a page is edited (FR-4): the `imagePath` URI changes (new filename suffix with timestamp) → old cache entry is never invalidated; **use timestamp-suffixed filenames for edited pages** to avoid stale cache |
| Coil memory cache | After document deletion | Coil entries become orphans in memory; they will be evicted naturally (no explicit invalidation needed) |

**Naming convention for edited pages:**

To prevent Coil serving stale cached images after an edit-save, page image filenames must include the `updatedAt` timestamp:
- Before edit: `page_001.jpg`
- After edit: `page_001_{updatedAt}.jpg`
- The old file is deleted; the Room `PageEntity.imagePath` is updated to the new path.

### In-Memory Edit State Cache

The `EditViewModel` holds up to 5 `Bitmap` snapshots in memory for undo/redo. Each snapshot is approximately 5–25 MB depending on resolution. If the system sends `onTrimMemory(TRIM_MEMORY_MODERATE)`, the undo stack must be cleared to `[currentBitmap]` only (depth 1).

---

## Non-Functional Requirements

### Performance

| Operation | Target | Notes |
|-----------|--------|-------|
| App cold start to Document List visible | < 2 seconds | Room query on IO dispatcher; list renders as data arrives via Flow |
| Per-page scan processing (FR-3) | < 3 seconds | Downscale + compress + save + Room insert |
| Image edit operation (rotate, brightness, contrast, grayscale) | < 500 ms | On IO dispatcher; show spinner if > 200 ms |
| Thumbnail generation after scan | < 2 seconds | Included within the 3s page processing budget |
| PDF export — 10-page document | < 5 seconds | Async; progress dialog shown |
| Document list load (100 documents) | < 1 second | Room `Flow` with index on `updated_at DESC` |
| Thumbnail loading in list (cold) | < 300 ms per image | Coil with memory + disk cache |

### Memory

| Constraint | Limit | Notes |
|------------|-------|-------|
| Max runtime RAM | < 200 MB | Enforce via Android StrictMode in debug builds |
| Edit undo stack | ≤ 5 Bitmaps | Clear to 1 on `onTrimMemory(MODERATE)` |
| PDF export peak RAM | < 100 MB | Process pages sequentially; close and recycle each Canvas after render |

### Storage

| Constraint | Limit | Notes |
|------------|-------|-------|
| Single page image (saved) | ≤ ~3 MB | JPEG 85%, 2480×3508 px |
| Single document (50 pages) | ≤ ~150 MB | 50 × 3 MB max |
| Total app data (100 docs) | ≤ ~15 GB theoretical max | Practical: warn at 100 MB free |

### Availability

All features work without internet. The GMS path requires Google Play Services to be installed; when absent, the app automatically falls back to the manual crop path. There is no degraded-mode network dependency.

---

## Edge Cases

### EC-1: User attempts to add page 51

**Condition:** `DocumentViewerUiState.pageLimitReached == true` (pageCount ≥ 50).
**Expected:** The FAB "Add page" is visually disabled (alpha 0.38, non-clickable). A long-press on the FAB shows a tooltip: *"Page limit reached (50 pages max)."* No navigation to scanner occurs. `SavePageUseCase` also throws `PageLimitException` if called programmatically — it must never silently drop pages.

### EC-2: User attempts to create document 101

**Condition:** `DocumentListUiState.documentLimitReached == true` (documentCount ≥ 100).
**Expected:** FAB is disabled with tooltip: *"Document limit reached (100 max). Delete a document to create a new one."* The scanner screen is never launched.

### EC-3: App killed mid-PDF export

**Condition:** Process is killed (OOM or user force-stop) while `PdfGenerator` is writing to `cacheDir/export/`.
**Expected:** On next app launch, `AppContainer` initialization scans `cacheDir/export/` and deletes any file whose last-modified time is > 10 minutes ago. No partial PDF is ever presented to the user.

### EC-4: GMS requires update on first scan

**Condition:** GMS is present but the ML Kit Document Scanner module requires a GMS update before it can run. The `getStartScanIntent()` future completes with an error.
**Expected:** Show a dialog: *"Google Play Services needs to update to enable automatic scanning. Manual crop mode will be used instead."* with buttons **Update GMS** (opens Play Store) and **Use Manual Mode** (proceeds to `ManualCropScreen`).

### EC-5: Page image file missing on Document Viewer open

**Condition:** `PageEntity` exists in Room but `page_{NNN}.jpg` has been deleted by a third-party file cleaner app.
**Expected:** The viewer displays a placeholder image (grey rectangle with a broken-image icon) for the missing page. The Edit and Export actions for that page are disabled. A banner is shown: *"One or more pages are missing from disk. They may have been deleted by a file cleaner app."*

### EC-6: Edit screen — all brightness/contrast at extremes

**Condition:** User sets brightness to +100 and contrast to +100 simultaneously.
**Expected:** The resulting `Bitmap` may appear fully white or fully black. This is accepted behavior — no clamp or error is shown. The user can undo to recover.

### EC-7: Rename to same name

**Condition:** User opens the rename dialog and taps Confirm without changing the name.
**Expected:** The Room update is skipped (no write). The dialog is dismissed. `updatedAt` is not changed.

### EC-8: Delete last page of a document

**Condition:** User deletes the only remaining page (page 1 of 1) from `DocumentViewerScreen`.
**Expected:** Show confirmation dialog: *"Deleting this page will also delete the entire document. Continue?"* If confirmed: delete the document (FR-7), navigate to Document List with a Snackbar: *"Document deleted."*

### EC-9: Configuration change during scan

**Condition:** Screen rotates while the ML Kit scanner Activity is open (GMS path).
**Expected:** ML Kit handles this internally. On the manual crop path: `ScannerViewModel` retains `capturedBitmap` across configuration changes via `SavedStateHandle`. The captured image is not lost.

### EC-10: Storage check finds exactly 100 MB free

**Condition:** `StatFs.availableBytes == 100 * 1024 * 1024L` (exactly at the warning threshold).
**Expected:** The warning dialog is shown (not the blocking dialog). The boundary is `< 100 MB` for warning and `< 50 MB` for blocking; 100 MB exactly is treated as sufficient.

---

## Acceptance Criteria

### Scanning

- [ ] Tapping "+" when documentCount < 100 opens the scanner (ML Kit or manual depending on GMS availability)
- [ ] ML Kit scanner returns 1 or more pages → document is created and visible in the list within 3 seconds
- [ ] Cancelling the ML Kit scanner returns to the Document List with no new document created
- [ ] On a non-GMS device (or GMS-free emulator), tapping "+" opens the manual crop screen
- [ ] Manual crop: capturing a photo, adjusting 4 corners, and tapping Confirm saves the page correctly
- [ ] Saved page image is JPEG at 85% quality and ≤ 2480×3508 px
- [ ] Scan is blocked (blocking dialog shown) when free storage < 50 MB
- [ ] Warning dialog is shown (non-blocking) when free storage is between 50 MB and 100 MB

### Document Management

- [ ] Document list shows all documents ordered by most recently updated first
- [ ] Empty state is shown when no documents exist
- [ ] FAB is disabled (not hidden) when documentCount ≥ 100
- [ ] Long-press on a document card shows Rename, Delete, Export PDF options
- [ ] Rename saves name within 1–50 chars; rejects forbidden characters with an inline error
- [ ] Rename with the same name as current dismisses the dialog without a Room write
- [ ] Delete shows a confirmation dialog; confirmed delete removes document from list immediately
- [ ] Deleting a document removes its folder from `filesDir/documents/`

### Image Editing

- [ ] Edit screen loads the correct page image
- [ ] Rotate button rotates image 90° CW; 4 presses returns to original orientation
- [ ] Brightness and contrast sliders update the preview in real-time (< 500 ms per change)
- [ ] Grayscale toggle converts image to grayscale and can be toggled back within the session
- [ ] Undo reverts to the previous state; undo button is disabled when stack is empty (max depth 5)
- [ ] Tapping Save writes the current bitmap to disk; navigating back to the viewer shows the updated image
- [ ] Tapping Back with unsaved changes shows a discard confirmation dialog

### PDF Export

- [ ] Export PDF from Document Viewer produces a valid PDF file
- [ ] PDF pages are rendered in `pageNumber` order
- [ ] Export of a 10-page document completes in < 5 seconds
- [ ] A progress dialog is shown for the duration of export and dismissed on completion
- [ ] On export success, a system share chooser opens with `application/pdf` MIME type
- [ ] On export failure, a Snackbar shows "Export failed. Please try again."
- [ ] Exported PDF file is removed from `cacheDir/export/` when the app moves to background

### Page Management

- [ ] Document Viewer shows pages in `pageNumber` order via `HorizontalPager`
- [ ] Page reorder via drag-and-drop updates the order in Room and in the pager immediately
- [ ] "Add page" FAB is disabled when pageCount ≥ 50; tooltip explains the limit
- [ ] Deleting the last page of a document triggers a confirmation dialog and then deletes the document
- [ ] Missing page image (file deleted externally) shows a placeholder with a banner notification

### Permissions & Security

- [ ] Camera permission not yet granted → system permission dialog is shown
- [ ] Camera permission permanently denied → `PermissionRationaleScreen` shown with "Open Settings" button
- [ ] App has no `INTERNET` permission in the manifest
- [ ] Exported files are shared via `content://` URIs (FileProvider), never direct `file://` URIs

### Performance & Stability

- [ ] App cold start to Document List visible in < 2 seconds on a mid-range device
- [ ] No ANR (Application Not Responding) during scan, edit, or export operations (all heavy work off Main thread)
- [ ] App does not crash when rotated during manual crop (captured bitmap is retained)
- [ ] No orphan files left in `cacheDir/export/` after process kill (cleaned on next launch)

---

## Open Questions

- [ ] Should the Document List support search/filter by name in v1? (Currently not in scope but easy to add)
- [ ] Should `updatedAt` be used as the sort key, or should users be able to choose between sort-by-date-created and sort-by-date-modified?
- [ ] Is Android Auto Backup to be explicitly disabled (via backup rules XML) to prevent inadvertent cloud sync of sensitive documents?

---

## Version History

| Version | Date | Author | Change |
|---------|------|--------|--------|
| 1.0 | 2026-06-28 | nguyenhuuca@gmail.com | Initial draft |
