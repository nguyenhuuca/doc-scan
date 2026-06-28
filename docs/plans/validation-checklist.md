# Phase 9 — Validation Checklist

Manual testing required before v1 release. Run on a real mid-range Android device (API 26+).

**Date:** 2026-06-28  
**Status:** ⬜ Not started

---

## T-47: Manual Testing (scan, edit, share)

| # | Test | Pass/Fail | Notes |
|---|------|-----------|-------|
| 1 | Scan document on white background | ⬜ | ML Kit auto-detect |
| 2 | Scan document on dark background | ⬜ | |
| 3 | Scan document on wooden table | ⬜ | Complex background |
| 4 | Scan document on fabric/carpet | ⬜ | Hard background |
| 5 | Scan document on glass/transparent surface | ⬜ | |
| 6 | Create 10-page document via multi-page scan | ⬜ | |
| 7 | Export 10-page document as PDF | ⬜ | Verify page order |
| 8 | Share PDF to Gmail | ⬜ | Verify attachment opens |
| 9 | Share PDF to Zalo | ⬜ | Verify attachment opens |
| 10 | Apply brightness edit, save, verify thumbnail updates | ⬜ | |
| 11 | Apply rotation, undo, verify original restored | ⬜ | |
| 12 | Apply grayscale, save, re-open page, verify grayscale persists | ⬜ | |

---

## T-48: Non-GMS Fallback Test

Run on GMS-free emulator (API 26, no Google Play Services).

| # | Test | Pass/Fail | Notes |
|---|------|-----------|-------|
| 1 | Launch app — no crash | ⬜ | |
| 2 | Tap + FAB — manual crop screen appears (not ML Kit) | ⬜ | |
| 3 | Capture photo via CameraX | ⬜ | |
| 4 | Drag corner handles to crop | ⬜ | Min 48dp tap area |
| 5 | Confirm crop — document saved, appears in list | ⬜ | |
| 6 | Retake button resets to camera preview | ⬜ | |
| 7 | Full scan → edit → PDF export flow on non-GMS | ⬜ | |

---

## T-49: APK Size Analysis

| # | Check | Pass/Fail | Measurement |
|---|-------|-----------|-------------|
| 1 | Build release APK: `./gradlew assembleRelease` | ⬜ | |
| 2 | Open in Build > Analyze APK | ⬜ | |
| 3 | Total APK size < 30 MB | ⬜ | ___ MB |
| 4 | R8/ProGuard shrinking enabled | ⬜ | |
| 5 | No unexpected large resources (.aar, .so) | ⬜ | |

**ProGuard rules to add if size exceeds 30 MB:**
```
-keep class androidx.camera.** { *; }
-dontwarn com.google.android.gms.**
```

---

## T-50: Performance Validation

Run on a mid-range device (≈ Snapdragon 665, 4 GB RAM).

| # | Metric | Target | Measured | Pass/Fail |
|---|--------|--------|----------|-----------|
| 1 | Cold start time (tap icon → Document List visible) | < 2s | ___ ms | ⬜ |
| 2 | Page processing time (capture → thumbnail appears) | < 3s | ___ ms | ⬜ |
| 3 | PDF export — 5-page document | < 5s | ___ ms | ⬜ |
| 4 | PDF export — 20-page document | < 15s | ___ ms | ⬜ |
| 5 | Brightness slider drag — no visible lag | < 100ms per frame | ⬜ | ⬜ |
| 6 | Memory — no OOM on 10-page PDF export | No crash | ⬜ | ⬜ |

**Measurement method:** Use Android Studio Profiler > CPU/Memory. Log timestamps in Logcat.

---

## T-51: Security Validation

| # | Check | Pass/Fail | Notes |
|---|-------|-----------|-------|
| 1 | `INTERNET` permission absent from AndroidManifest.xml | ⬜ | grep for `INTERNET` |
| 2 | No `file://` URIs in share intents (use FileProvider only) | ⬜ | Check Logcat for FileUriExposedException |
| 3 | FileProvider `android:exported="false"` | ⬜ | Check manifest |
| 4 | FileProvider authority = `${applicationId}.fileprovider` | ⬜ | |
| 5 | No hardcoded paths in shared URIs | ⬜ | |
| 6 | `android:allowBackup="false"` in manifest | ⬜ | Documents not backed up to Google |

---

## T-52: Privacy Validation

| # | Check | Pass/Fail | Notes |
|---|-------|-----------|-------|
| 1 | No file paths logged at INFO/DEBUG level | ⬜ | Run app, check Logcat filter |
| 2 | No document names logged | ⬜ | |
| 3 | `android:allowBackup="false"` prevents Auto Backup of `filesDir/documents/` | ⬜ | |
| 4 | `cacheDir/export/` is cleared on app background | ⬜ | Share PDF, background app, check dir |
| 5 | `cacheDir/export/` is cleared on app cold start | ⬜ | Kill app, restart, check dir |
| 6 | Documents not visible to other apps (no MediaStore entries) | ⬜ | Check Files app |

---

## Sign-off

All checkboxes must be ✅ before tagging v1.0 release.

| Validator | Date | Signature |
|-----------|------|-----------|
| | | |
