# PRD: Android Document Scanner App

<!--
Product Requirements Document
Filename: docs/prd/PRD-android-document-scanner.md
Owner: Product
Handoff to: Architect (/architect), UI/UX Designer (/ui-ux-designer)
-->

## Overview

**Status:** Draft
**Author:** nguyenhuuca@gmail.com
**Date:** 2026-06-28
**Version:** 1.0
**Beads Issue:** N/A
**PR-FAQ:** N/A
**Stakeholders:** Product Owner

## Problem Statement

Individuals and students frequently need to digitize paper documents (invoices, contracts, handwritten notes, ID cards, certificates) for storage and later use. Existing solutions require a physical scanner, or involve manually photographing documents and editing them across multiple apps — inconvenient, time-consuming, and often producing poor image quality.

The Android Document Scanner app addresses this by providing a tool that scans documents directly using the phone camera, with automatic edge detection, image editing, and PDF export — all working offline on the device.

### Evidence

**Quantitative Evidence:**
- Similar apps (CamScanner, Adobe Scan) have tens of millions of downloads on the Google Play Store, demonstrating massive demand
- Students routinely need to scan documents for study groups, assignments, and course material archiving

> **Assumption:** Target users are individual users and students, primarily in Vietnam, using Android devices, who need a fast solution that does not depend on an internet connection.

**Qualitative Evidence:**
- Users report that existing apps (CamScanner) require account registration and an internet connection — frustrating when only a quick scan is needed
- Many users want control over their personal data and do not want documents uploaded to third-party servers

## Goals & Success Metrics

| Goal | Metric | Target |
|------|--------|--------|
| Fast and accurate scanning | Time from app open to PDF ready | < 30 seconds |
| Accurate edge detection | Auto-detection rate requiring no manual adjustment | ≥ 85% |
| High output image quality | Text legibility in exported PDF | Readable at standard viewing size |
| Fully offline operation | Features working without internet | 100% of v1 features offline |
| Smooth performance | Processing time per scanned page | < 3 seconds |

## User Stories

### Student / Individual User

- As a student, I want to photograph a document and have its edges automatically detected so that I do not need to crop manually every time
  - Acceptance: Camera shows a real-time edge overlay; after capture, the image is cropped automatically according to the detected boundary
- As an individual, I want to combine multiple scanned pages into a single PDF so that I can send an entire document in one file
  - Acceptance: User can add multiple pages to one document and export as PDF; page order can be rearranged via drag-and-drop
- As a user, I want to edit an image after scanning so that I can improve its quality (contrast, rotation, crop)
  - Acceptance: Edit screen offers tools for 90° rotation, free crop, brightness/contrast adjustment, and grayscale conversion
- As a user, I want to view a list of my scanned documents so that I can find and reopen any of them at any time
  - Acceptance: Home screen shows a document list with name, creation date, and thumbnail; rename and delete are available

## Requirements

### Functional Requirements

| ID | Requirement | Priority | Notes |
|----|-------------|----------|-------|
| FR-1 | Camera with real-time document edge detection overlay | Must Have | CameraX + ML Kit or OpenCV |
| FR-2 | Auto-crop image according to detected edges after capture | Must Have | Perspective transform |
| FR-3 | Image editing: rotate, crop, brightness/contrast adjustment | Must Have | |
| FR-4 | Convert image to grayscale / black-and-white (document mode) | Must Have | Improves text legibility |
| FR-5 | Combine multiple pages into one document | Must Have | |
| FR-6 | Export document as a PDF file | Must Have | |
| FR-7 | Store documents in device local storage | Must Have | |
| FR-8 | Document list with thumbnails | Must Have | |
| FR-9 | Rename and delete documents | Must Have | |
| FR-10 | Reorder pages within a document | Should Have | Drag & drop |
| FR-11 | Export a single page as an image (JPG/PNG) | Should Have | |
| FR-12 | Import images from device gallery into a document | Should Have | Alternative when re-scanning is not needed |

### Non-Functional Requirements

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-1 | Processing time per scanned page | < 3 seconds |
| NFR-2 | App startup time | < 2 seconds |
| NFR-3 | Fully offline operation | 100% of v1 features require no internet |
| NFR-4 | Minimum Android version | Android 8.0 (API 26) |
| NFR-5 | APK size | < 30 MB |
| NFR-6 | Runtime memory usage | < 200 MB RAM |
| NFR-7 | Data privacy | Documents stored locally only; no data transmitted externally |

## Scope

### In Scope (v1)

- Document scanning via camera with automatic edge detection and cropping
- Multi-page document export as PDF
- Basic image editing: rotation, crop, brightness/contrast, grayscale
- Local document storage and management on the device
- Rename and delete documents
- Fully offline operation

### Out of Scope (v2+)

- Cloud upload (Google Drive, Dropbox, OneDrive)
- E-signature (digital signing on documents)
- Direct sharing via email/Zalo/WhatsApp from within the app
- OCR / text recognition
- QR code / barcode scanning
- Multi-device data synchronization

## Dependencies

| Dependency | Owner | Status | Risk |
|------------|-------|--------|------|
| CameraX (Jetpack) | Google | Stable (androidx.camera) | Low |
| ML Kit Document Scanner API or OpenCV | Google / Open Source | Stable | Medium — edge detection quality needs evaluation |
| iTextPDF or Apache PDFBox Android | Open Source | Stable | Low — PDF creation library |
| Android Storage (MediaStore / SAF) | Google | Stable | Low |

> **Assumption:** ML Kit Document Scanner API (`com.google.android.gms:play-services-mlkit-document-scanner`) is the preferred edge detection solution because it runs via Google Play Services without bundling a model into the APK. For devices without Google Play Services, fallback to OpenCV or manual corner selection.

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Poor edge detection on dark images or complex backgrounds | M | H | Allow manual corner adjustment after auto-detection |
| Large APK size due to bundled ML model | M | M | Use ML Kit via Play Services (no bundle); OpenCV as fallback |
| Slow PDF export for documents with many pages (> 10) | L | M | Async processing with progress indicator |
| Device storage full due to high-quality images | M | M | Compress images before saving; warn when storage < 100 MB |
| Camera compatibility issues across Android device vendors | M | H | Use CameraX as abstraction layer; test on multiple devices |

## Open Questions

- [ ] Should auto-capture (trigger when document is detected and held still) be supported?
- [ ] Where should PDFs be saved: private app storage or the public Documents folder?
- [ ] Should the UI support multiple languages (EN + VI) in v1?
- [ ] Should a PIN or biometric lock be available to protect sensitive documents?

## Appendix

### Mockups / Wireframes

TBD — requires UI/UX Designer

### Competitive Analysis

| App | Strengths | Weaknesses |
|-----|-----------|------------|
| CamScanner | Feature-rich | Requires login, uploads to cloud |
| Adobe Scan | Good OCR, Adobe integration | Requires Adobe account |
| Microsoft Lens | Office integration | Tied to Microsoft ecosystem |
| **This app** | Fully offline, no account required | No OCR in v1 |

### Tech Stack Recommendation

> **Assumption:** Kotlin with Jetpack Compose for UI, CameraX for camera, ML Kit Document Scanner for edge detection, Android built-in `PdfDocument` for PDF export.

---

## Approval

| Role | Name | Date | Status |
|------|------|------|--------|
| Product | nguyenhuuca@gmail.com | 2026-06-28 | Pending |
| Engineering | | | Pending |
| Design | | | Pending |

---

## Next Steps & Handoffs

After PRD approval:

1. [ ] **Architect Review**: Technical feasibility, stack decisions, ADR
   - Trigger: `/architect`
   - Output: ADR (`docs/adr/0001-document-scanning-approach.md`)

2. [ ] **UI/UX Designer**: Wireframes for Camera Screen, Edit Screen, Document List
   - Trigger: `/ui-ux-designer`
   - Output: Design Spec

3. [ ] **Implementation Plan**: Decompose into tasks per screen/module
   - Trigger: `/builder` or `/architect`
   - Output: `docs/plans/plan-android-document-scanner.md`

**Related Artifacts:**
- ADR: [ADR-0001](../adr/0001-document-scanning-approach.md) · [ADR-0002](../adr/0002-app-architecture-and-storage.md)
- Design Spec: TBD
- Implementation Plan: [plan-android-document-scanner](../plans/plan-android-document-scanner.md)

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-06-28 | nguyenhuuca@gmail.com | Initial draft |
