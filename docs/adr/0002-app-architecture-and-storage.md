# ADR-0002: App Architecture Pattern & Storage Strategy

<!--
Architecture Decision Record
Filename: docs/adr/0002-app-architecture-and-storage.md
Owner: Architect
Related PRD: docs/prd/PRD-android-document-scanner.md
-->

## Metadata

**Status:** Proposed · **Date:** 2026-06-28 · **Deciders:** nguyenhuuca@gmail.com · **Tags:** architecture, mvvm, storage, pdf, jetpack-compose
**Related PRD:** [PRD-android-document-scanner](../prd/PRD-android-document-scanner.md) · **Supersedes:** N/A · **Superseded By:** N/A

**Tech Strategy:** ⚠️ Deviation — Android project; outside the Java/Spring Golden Path. This ADR establishes Android-specific standards.

---

## Context

This ADR covers three closely related decisions:
1. **Architecture pattern** — how to organize code across the UI, business logic, and data layers
2. **PDF export library** — how to generate PDF files from scanned images
3. **Storage strategy** — where to persist documents and metadata on the device

These decisions are linked: the MVVM architecture affects how the Repository (storage) is structured, and the storage strategy influences how PDF export is invoked.

Constraints from the PRD:
- Fully offline (NFR-3) → no network layer; all persistence is local
- APK < 30 MB (NFR-5) → combined with ~0 MB from ADR-0001, ~30 MB budget remains
- Android 8.0+ (NFR-4)
- Documents stored locally only; no data transmitted externally (NFR-7)

---

## Decision Drivers

- **Maintainability**: The app will grow (OCR, cloud sync in v2) → clear separation of concerns required
- **Testability**: UI logic must be testable independently of the Android framework
- **APK size**: PDF library must not be too heavy (total APK budget ~30 MB)
- **Simplicity**: Solo developer → avoid over-engineering; no heavy DI framework needed
- **Privacy**: Scanned documents are sensitive → private storage preferred over public
- **Export capability**: Users must be able to share PDFs with other apps → SAF/FileProvider required

---

## Considered Options — Architecture Pattern

### Option A: MVVM + Repository + Jetpack Compose

Standard Android architecture: `Composable UI → ViewModel (StateFlow) → UseCase → Repository → DataSource (Room DB + FileSystem)`.

| Pros | Cons |
|------|------|
| Official Google-recommended pattern for Compose | Requires boilerplate: ViewModel factory, Repository interface |
| Clear separation of concerns; easy to test | For a small app, some layers may feel redundant |
| StateFlow integrates naturally with Compose recomposition | |
| Easy to extend (adding a new UseCase does not touch the UI) | |
| Extensive documentation and community support | |

### Option B: MVI (Model-View-Intent) + Jetpack Compose

Unidirectional data flow: `UI Event → Intent → Reducer → State → UI`. Popular with Orbit MVI or custom implementations.

| Pros | Cons |
|------|------|
| Predictable state; easy to debug | More complex than MVVM for a small app |
| Good for complex UIs with many concurrent events | Requires additional library (Orbit ~0.5 MB) or significant boilerplate |
| Immutable state prevents race conditions | Over-engineering for this use case |

### Option C: Jetpack Compose + Lightweight ViewModel (no Repository)

UI → ViewModel → direct access to Room/FileSystem; no UseCase or Repository layer.

| Pros | Cons |
|------|------|
| Very little boilerplate | Hard to test (ViewModel depends on Android context) |
| Fast to implement | Difficult to extend when adding cloud sync (v2) |
| | No separation of concerns |

---

## Considered Options — PDF Export Library

### Option P1: Android built-in `android.graphics.pdf.PdfDocument`

Available from API 19+; no external dependency. Supports drawing a Canvas onto PDF pages.

| Pros | Cons |
|------|------|
| Zero APK size increase | Low-level API; manual page layout calculations required |
| No external dependency | Limited PDF metadata support (author, title) |
| Sufficient for the v1 use case: embed images into PDF pages | PDF compression options are limited |
| Stable; Google-maintained | |

### Option P2: iText7 Community (Android)

Full-featured PDF library; AGPL license.

| Pros | Cons |
|------|------|
| High-level API; easy to use | **AGPL license** → commercial distribution requires a paid license |
| Supports metadata, compression, and fonts | ~5–8 MB APK size increase |
| Feature-rich for v2 (OCR text layer) | Too many features for v1 needs |

### Option P3: Apache PDFBox Android (port)

An Android port of Apache PDFBox; Apache 2.0 license.

| Pros | Cons |
|------|------|
| Apache 2.0 license (free for commercial use) | Unofficial port; infrequently maintained |
| Feature-rich | ~6–10 MB APK size increase |
| | Known bugs on certain Android versions |

---

## Considered Options — Storage Strategy

### Option S1: Private App Storage (Internal Storage)

`context.filesDir` — accessible only to the app; deleted on uninstall; no permissions required.

| Pros | Cons |
|------|------|
| No READ/WRITE_EXTERNAL_STORAGE permission required | Deleted on uninstall — documents are lost |
| Documents hidden from other apps (privacy) | Users cannot browse files via a file manager |
| No scoped storage handling needed (API 29+) | Requires a separate export step to share |

### Option S2: Public External Storage via MediaStore

Store in the `Downloads` or `Documents` folder via the MediaStore API.

| Pros | Cons |
|------|------|
| Files persist after uninstall | Complex permissions (scoped storage API 29+) |
| User can browse files | Poor privacy — readable by other apps |
| | More complex UX (permission dialogs) |

### Option S3: Private Storage + Export via SAF/FileProvider (Hybrid)

Store in private storage; expose files externally via `FileProvider` only when the user explicitly chooses to share or export.

| Pros | Cons |
|------|------|
| Privacy by default — documents not exposed to other apps | Requires FileProvider configuration in AndroidManifest |
| No storage permission required | Documents lost on uninstall (mitigated with a backup warning) |
| Sharing out is still possible (Intent chooser) | |
| Naturally compliant with scoped storage | |

---

## Decision Outcome

### Architecture: Option A — MVVM + Repository + Jetpack Compose

**Rationale:** MVVM is the standard pattern for Android with Jetpack Compose, providing sufficient separation of concerns for v1 and a clear extension path for v2 (adding cloud sync only requires a `RemoteDataSource` behind the existing Repository interface). MVI (Option B) is over-engineering for a solo developer on this scope.

### PDF Export: Option P1 — Android built-in `PdfDocument`

**Rationale:** v1 only needs to embed scanned images into PDF pages — `PdfDocument` handles this with zero APK size increase. iText7 (Option P2) carries AGPL license risk for any future commercial distribution. When v2 requires a text layer (OCR), migration to iText7 can be decided via a new ADR.

### Storage: Option S3 — Private Storage + FileProvider Export

**Rationale:** Privacy is a top priority (NFR-7). Private storage ensures documents are not accessible to other apps by default. FileProvider allows PDF sharing when the user explicitly chooses to — sufficient for v1. A clear warning about uninstall data loss will be shown in the settings screen.

---

### Overall Architecture Diagram

```
┌─────────────────────────────────────────────────────┐
│                   UI Layer (Compose)                 │
│  DocumentListScreen  CameraScreen  EditScreen        │
│         └──────────────┬──────────────┘             │
│                   ViewModel                          │
│  DocumentListVM    ScannerVM    EditVM               │
└────────────────────────┬────────────────────────────┘
                         │ StateFlow<UiState>
┌────────────────────────▼────────────────────────────┐
│               Domain Layer (Use Cases)               │
│  ScanDocumentUseCase  ExportPdfUseCase               │
│  SaveDocumentUseCase  GetDocumentsUseCase            │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│               Data Layer (Repository)                │
│  DocumentRepository                                  │
│    ├── Room DB  (metadata: id, name, date, pageCount)│
│    ├── FileSystem (page images: filesDir/documents/) │
│    └── PdfGenerator (android.graphics.pdf.PdfDocument)│
└─────────────────────────────────────────────────────┘
```

**Room database schema:**
```
documents: id (UUID), name, createdAt, updatedAt, pageCount, thumbnailPath
pages:     id (UUID), documentId (FK), pageNumber, imagePath, createdAt
```

**Filesystem layout:**
```
filesDir/
  documents/
    {documentId}/
      thumbnail.jpg          ← first-page thumbnail
      pages/
        page_001.jpg
        page_002.jpg
        ...
```

### Quantified Impact

| Concern | Decision | APK Impact | Trade-off |
|---------|----------|------------|-----------|
| PDF export | Built-in PdfDocument | 0 MB | No OCR text layer (v2 concern) |
| Architecture | MVVM | ~0 MB | Light boilerplate; worth the structure |
| Storage | Private + FileProvider | 0 MB | Documents lost on uninstall |
| **Total APK estimate** | | **~5–8 MB** (Room + Compose + Kotlin) | Well under 30 MB |

---

## Consequences

**Positive:**
- Estimated APK ~5–8 MB (Room, Compose, CameraX) + ~0 MB from ADR-0001 → total < 15 MB
- Architecture is testable: ViewModels tested with fake Repositories; UseCases are pure Kotlin
- Privacy-first: no storage permissions required; documents not exposed by default
- Clear migration path: adding cloud sync in v2 only requires a `CloudDataSource` implementing the same interface as `LocalDataSource`

**Negative:**
- Documents are deleted on uninstall — requires a clear warning dialog in the Settings screen
- Built-in `PdfDocument` does not support PDF/A or searchable PDF (migration needed when OCR is added)
- FileProvider requires additional configuration in `AndroidManifest.xml`

**Risks:**
- Room schema migrations when adding new fields in v2 (tags, cloud sync status) → write migration scripts from day one, never use `fallbackToDestructiveMigration` in production
- `PdfDocument` may cause OOM on very long documents (> 50 pages) → process pages in batches; close and recycle each page's Canvas after rendering

---

## Validation

- [ ] Prototype MVVM flow: Camera → ViewModel → UseCase → Repository → Room; confirm no memory leaks
- [ ] Test `PdfDocument` with a 20-page document; measure memory footprint and export time
- [ ] Verify FileProvider share intent works with Gmail, Zalo, and the system Files app
- [ ] Confirm total APK size after full dependency setup is < 30 MB
- [ ] Tech Strategy alignment confirmed
- [ ] Related plan document created: `docs/plans/plan-android-document-scanner.md`

---

## Links

- [Related PRD](../prd/PRD-android-document-scanner.md)
- [ADR-0001 Document Scanning Approach](./0001-document-scanning-approach.md)
- [Implementation Plan](../plans/plan-android-document-scanner.md)

---

## Changelog

| Date | Author | Change |
|------|--------|--------|
| 2026-06-28 | nguyenhuuca@gmail.com | Initial draft — covers architecture pattern, PDF export library, and storage strategy |
