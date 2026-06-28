# ADR-0001: Document Scanning Approach

<!--
Architecture Decision Record
Filename: docs/adr/0001-document-scanning-approach.md
Owner: Architect
Related PRD: docs/prd/PRD-android-document-scanner.md
-->

## Metadata

**Status:** Proposed · **Date:** 2026-06-28 · **Deciders:** nguyenhuuca@gmail.com · **Tags:** camera, ml-kit, opencv, edge-detection
**Related PRD:** [PRD-android-document-scanner](../prd/PRD-android-document-scanner.md) · **Supersedes:** N/A · **Superseded By:** N/A

**Tech Strategy:** ⚠️ Deviation — no existing Android Golden Path; this ADR establishes the baseline

---

## Context

The app must photograph paper documents and automatically detect edges to crop them precisely using a perspective transform. This is the core feature and directly determines output quality.

Three main approaches exist, each with different trade-offs across quality, APK size, offline independence, and integration complexity.

Constraints from the PRD:
- Fully offline (NFR-3)
- APK < 30 MB (NFR-5)
- Edge detection accuracy ≥ 85% (Goals)
- Processing time per page < 3 seconds (NFR-1)
- Android 8.0+ (API 26+) (NFR-4)

---

## Decision Drivers

- **Offline first** — no data sent externally, no internet dependency
- **APK size ≤ 30 MB** — bundling an ML model would likely violate this constraint
- **Accuracy ≥ 85%** — edge detection quality is a core product requirement
- **Processing time < 3 seconds/page** — must be fast on mid-range devices
- **UI customization** — need real-time overlay, preview, and manual adjustment fallback
- **Device coverage** — must run on devices without Google Play Services (some Huawei models and custom ROMs)

---

## Considered Options

### Option 1: ML Kit Document Scanner API (GMS-bundled)

Google provides `com.google.android.gms:play-services-mlkit-document-scanner` — a complete scanning flow (camera + edge detection + crop) launched as a separate Activity. The ML model runs through GMS; it is not bundled into the APK.

| Pros | Cons |
|------|------|
| High-quality ML detection with no APK size increase | Launches a separate Activity — no control over scanning UI/UX |
| Zero APK size increase | Requires Google Play Services — will not run on Huawei or custom ROMs |
| Minimal code; fast to integrate | No real-time edge overlay integrated with custom camera preview |
| Google-maintained, stable | Depends on GMS availability — outdated GMS may cause failures |
| Supports auto-capture when document is held still | Cannot customize the manual corner adjustment UI |

**APK size impact:** ~0 MB increase (model lives in GMS)
**Accuracy:** Very high (ML-based, Google-trained model)
**Processing time:** ~1–2 seconds

### Option 2: CameraX + OpenCV Android

CameraX handles the camera preview; OpenCV (`org.opencv:opencv-android-sdk`) performs contour detection (Canny edge + findContours) and perspective transform.

| Pros | Cons |
|------|------|
| Full UI control — custom overlay, corner drag handles, manual adjustment | OpenCV AAR ~20 MB — APK approaches the 30 MB limit |
| No GMS dependency; runs on all Android 8.0+ devices | Requires a complex pipeline: grayscale → blur → Canny → findContours → convexHull → perspective transform |
| Real-time edge overlay via CameraX ImageAnalysis | Contour detection accuracy drops on complex backgrounds (fabric, wood) |
| Fully on-device processing | Requires ongoing maintenance and threshold tuning |
| Truly offline, no GMS needed | Processing time on low-end devices may exceed 3 seconds |

**APK size impact:** ~20 MB increase (OpenCV core)
**Accuracy:** ~75–80% on complex backgrounds; ~90%+ on plain backgrounds
**Processing time:** ~0.5–1 second (native C++)

### Option 3: CameraX + Lightweight Custom Edge Detection (Android-native)

CameraX for preview; custom Canny/Sobel edge detection implemented in pure Kotlin/Java without OpenCV.

| Pros | Cons |
|------|------|
| Zero APK size increase | Accuracy significantly lower than Option 2 (~65–75%) |
| Full UI control | Slower processing (JVM vs C++ native) |
| No GMS or heavy library dependency | Substantial development time to implement and tune |
| Runs on all devices | Unlikely to meet the 85% accuracy target |

**APK size impact:** ~0 MB increase
**Accuracy:** ~65–75%
**Processing time:** ~2–4 seconds

### Option 4: ML Kit GMS (Primary) + CameraX + Manual Fallback (Hybrid)

ML Kit Document Scanner API as the primary path. When GMS is unavailable, fall back to CameraX with manual corner selection (no auto-detection). No OpenCV.

| Pros | Cons |
|------|------|
| Small APK (no bundled model, no OpenCV) | Inconsistent experience between GMS and non-GMS devices |
| High accuracy on GMS devices (~95% of Android devices) | Non-GMS devices must crop manually — degraded experience |
| Wide device coverage via fallback | Additional complexity from conditional code paths |
| Less code than Option 2 | Manual fallback does not meet the 85% auto-accuracy metric |

**APK size impact:** ~0 MB increase
**Accuracy:** ~95% on GMS devices; manual (unmeasured) on non-GMS
**Processing time:** ~1–2 seconds (GMS); manual (user-driven) on non-GMS

---

## Decision Outcome

**Chosen Option:** Option 4 — ML Kit GMS Primary + CameraX + Manual Corner Fallback

**Rationale:**

Option 4 provides the best trade-offs for the target user (individuals and students, primarily using GMS-enabled Android devices):

1. **APK size**: No increase — safely under 30 MB
2. **Accuracy**: ML Kit achieves > 90% on GMS devices — exceeds the 85% target
3. **Offline**: ML Kit on-device inference requires no internet after the initial GMS model download
4. **Coverage**: ~95%+ of Android devices have Google Play Services
5. **Graceful fallback**: CameraX + manual corner drag ensures non-GMS devices remain usable

Option 2 (OpenCV) is rejected because its ~20 MB footprint risks violating the 30 MB APK limit when combined with other dependencies.
Option 3 is rejected because it cannot reliably meet the 85% accuracy target.
Option 1 (pure ML Kit) is rejected because it provides no fallback for non-GMS devices.

**UI architecture with Option 4:**
- GMS path: Launch `GmsDocumentScannerOptions` → receive results via `ActivityResultLauncher`
- Non-GMS path: CameraX `ImageCapture` + manual crop screen with 4 draggable corner handles

### Quantified Impact

| Metric | Option 2 (OpenCV) | Option 4 (Chosen) | Notes |
|--------|-------------------|-------------------|-------|
| APK size increase | ~20 MB | ~0 MB | OpenCV eliminated |
| Edge detection accuracy | ~80% | ~95% (GMS) | ML Kit significantly better |
| Processing time | ~0.5s | ~1–2s | Acceptable; within 3s target |
| Device coverage | 100% Android | ~95% (GMS) + 5% manual | Acceptable trade-off |

---

## Consequences

**Positive:**
- Small APK — easy to download on slow 3G/4G connections
- High edge detection quality via Google's ML Kit model
- Less complex code to maintain compared to an OpenCV pipeline
- Manual fallback ensures 100% device coverage, albeit with a different experience

**Negative:**
- Inconsistent experience between GMS and non-GMS devices
- ML Kit scanner UI is not fully customizable (Google's built-in UI)
- Dependency on GMS for the primary path — API changes require app updates

**Risks:**
- ML Kit Document Scanner may require a GMS update (~10–30 MB background download) on first use → notify users when this is detected
- Manual corner UI may be more complex than estimated → allocate a 2-day spike to prototype the corner drag implementation before committing to the design

---

## Validation

- [ ] Prototype ML Kit Document Scanner on a test device; measure accuracy and processing time
- [ ] Test on a non-GMS device (GMS-free emulator) — confirm fallback works correctly
- [ ] Measure APK size after all dependencies are added — must be < 30 MB
- [ ] Tech Strategy alignment confirmed
- [ ] Related plan document created: `docs/plans/plan-android-document-scanner.md`

---

## Links

- [Related PRD](../prd/PRD-android-document-scanner.md)
- [Implementation Plan](../plans/plan-android-document-scanner.md)
- [ADR-0002 App Architecture & Storage](./0002-app-architecture-and-storage.md)

---

## Changelog

| Date | Author | Change |
|------|--------|--------|
| 2026-06-28 | nguyenhuuca@gmail.com | Initial draft |
