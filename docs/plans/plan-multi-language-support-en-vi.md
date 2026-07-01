# Plan: Multi-Language Support (English + Vietnamese)

<!--
Implementation Plan
Filename: docs/plans/plan-multi-language-support-en-vi.md
Generated from: /swarm-plan
Input: docs/adr/0003-multi-language-support-en-vi.md
-->

## Overview

**Status:** Ready for Execution
**Date:** 2026-07-01
**Total Tasks:** 13
**Estimated Effort:** ~6 days (solo) · ~4 days (3 parallel builders on T-02–T-06)
**Decision Reversibility:** Two-Way Door — resource-only + one manifest-adjacent lint gate; no schema/API change, trivially revertable
**Required Artifact (per size rubric):** Plan only — no PRD needed, ADR-0003 already carries the requirements and decisions

**Related Docs:**
- ADR: [0003-multi-language-support-en-vi](../adr/0003-multi-language-support-en-vi.md) — source of all decisions (A/B/C/D) referenced below
- PRD: [PRD-android-document-scanner](../prd/PRD-android-document-scanner.md) — open question this closes (line 137)

**Beads tracking note:** The `bd` CLI is not installed in this environment (checked: not on `PATH`, no `.beads/` directory). Task IDs below (T-01…T-13) are tracked instead via the harness's session TaskCreate/TaskList tool with `blockedBy` dependencies mirroring the graph below. If Beads is set up later, these 13 tasks map 1:1 to `bd create` entries.

---

## Exploration Summary

Exploration for this plan reused the codebase audit already performed while drafting ADR-0003 (no re-exploration needed — the same facts apply):

| Finding | Detail |
|---------|--------|
| Hardcoded strings | ~48 literals across 8 files: `DocumentViewerScreen.kt`, `ReorderableThumbnailList.kt`, `EditScreen.kt`, `DocumentListScreen.kt`, `RenameDialog.kt`, `DeleteConfirmDialog.kt`, `ScannerScreen.kt`, `ManualCropScreen.kt`, `SettingsScreen.kt` |
| Pre-existing violation | `SettingsScreen.kt:98` has hardcoded **Vietnamese** (`"Có gì mới"`) inside an English-default screen — must be extracted like everything else |
| No locale infra | No `values-vi/`, no `android:localeConfig`, no `androidx.appcompat` dependency — confirms ADR-0003 Decision B1 (system-locale-only, zero new dependency) is starting from a clean slate |
| Non-resource content | `release_notes.yaml` (CI-generated) stays English-only per ADR Decision C1 — **out of scope** for this plan |
| Locale-sensitive formatting | `SettingsScreen.formatBytes()` and `EditScreen`'s contrast `"%.2f"` display need `Locale.US` forced per ADR Decision C2 |
| Test coupling risk | `EditScreenTest.kt` and `DocumentListScreenTest.kt` assert on hardcoded English literals via `onNodeWithText("...")` — must be updated to reference resources so they don't silently decouple from `values/strings.xml` |
| Regression gate | Android Lint's default `MissingTranslation`/`ExtraTranslation` checks activate automatically once `values-vi/` exists — just needs wiring into `.github/workflows/build.yml` (ADR Decision D) |

No spikes required — this is a mechanical resource-extraction task plus one translation pass, not a technical unknown.

---

## Task List

### Phase 1 — String Externalization Refactor (Hat 1: no behavior change)

| ID | Task | Est. | Deliverable |
|----|------|------|-------------|
| T-01 | Add ~48 new entries to `res/values/strings.xml` (plain + format-arg, e.g. `<string name="page_number">Page %1$d</string>`, `<string name="brightness_label">Brightness: %1$d</string>`) covering every screen listed above | 0.5d | Updated `values/strings.xml` — additive only, no code changes yet |
| T-02 | Replace hardcoded `Text("...")`/`contentDescription = "..."` in `DocumentViewerScreen.kt` + `ReorderableThumbnailList.kt` with `stringResource(R.string.x [, args])` | 0.5d | Both files migrated |
| T-03 | Same migration for `EditScreen.kt`; also force `Locale.US` in the contrast `"%.2f"` format call (ADR Decision C2) | 0.5d | `EditScreen.kt` migrated + locale-safe formatting |
| T-04 | Same migration for `DocumentListScreen.kt`, `RenameDialog.kt`, `DeleteConfirmDialog.kt` | 0.5d | All three files migrated |
| T-05 | Same migration for `ScannerScreen.kt`, `ManualCropScreen.kt` | 0.5d | Both files migrated |
| T-06 | Same migration for `SettingsScreen.kt` — including removing the hardcoded Vietnamese `"Có gì mới"` string; force `Locale.US` in `formatBytes()` (ADR Decision C2) | 0.5d | `SettingsScreen.kt` migrated + locale-safe formatting |
| T-07 | Update `EditScreenTest.kt` and `DocumentListScreenTest.kt` assertions to read expected text via `context.getString(R.string.x)` instead of re-hardcoded literals | 0.5d | Tests decoupled from literal English copy |
| T-08 | Quality gate + Hat-1 commit: `./gradlew testDebugUnitTest --no-daemon` and `./gradlew lintDebug` both green; commit as pure refactor (no `values-vi/` yet, so no translation-completeness warnings possible at this point) | 0.25d | Green CI, refactor commit |

**Phase 1 total: ~3.75 days**

### Phase 2 — Vietnamese Translation (Hat 2: feature)

| ID | Task | Est. | Deliverable |
|----|------|------|-------------|
| T-09 | Create `res/values-vi/strings.xml` — translate all ~70 entries (21 pre-existing + ~48 extracted in T-01) | 1d | Complete Vietnamese resource file |
| T-10 | Manual QA: switch emulator to Vietnamese, walk every screen (List, Scanner, Edit, Viewer, Settings, all dialogs); check for English leakage and text overflow/truncation (Vietnamese strings run ~15–25% longer) | 0.5d | QA checklist completed, overflow issues filed/fixed |
| T-11 | Manual QA: switch emulator to a third locale (e.g. French); confirm correct fallback to `values/` (English) | 0.25d | Fallback confirmed |

**Phase 2 total: ~1.75 days**

### Phase 3 — Regression Gate & Sign-off

| ID | Task | Est. | Deliverable |
|----|------|------|-------------|
| T-12 | Add `./gradlew lintDebug` as a required step in `.github/workflows/build.yml`, gating on zero `MissingTranslation`/`ExtraTranslation` warnings (ADR Decision D) | 0.25d | Updated CI workflow |
| T-13 | Final validation against the ADR-0003 checklist; Hat-2 commit (translations + CI gate) | 0.25d | All ADR-0003 validation boxes checked |

**Phase 3 total: ~0.5 days**

---

## File Registry

| File | Action | Task | Notes |
|------|--------|------|-------|
| `app/src/main/res/values/strings.xml` | **UPDATE** | T-01 | +~48 entries |
| `app/src/main/res/values-vi/strings.xml` | **CREATE** | T-09 | Full Vietnamese translation, ~70 entries |
| `ui/viewer/DocumentViewerScreen.kt` | **UPDATE** | T-02 | Remove hardcoded literals |
| `ui/viewer/components/ReorderableThumbnailList.kt` | **UPDATE** | T-02 | Remove hardcoded `contentDescription` |
| `ui/edit/EditScreen.kt` | **UPDATE** | T-03 | Remove hardcoded literals + `Locale.US` fix |
| `ui/documentlist/DocumentListScreen.kt` | **UPDATE** | T-04 | Remove hardcoded literals |
| `ui/documentlist/components/RenameDialog.kt` | **UPDATE** | T-04 | Remove hardcoded literals |
| `ui/documentlist/components/DeleteConfirmDialog.kt` | **UPDATE** | T-04 | Remove hardcoded literals |
| `ui/scanner/ScannerScreen.kt` | **UPDATE** | T-05 | Remove hardcoded literals |
| `ui/scanner/components/ManualCropScreen.kt` | **UPDATE** | T-05 | Remove hardcoded literals |
| `ui/settings/SettingsScreen.kt` | **UPDATE** | T-06 | Remove hardcoded literals (incl. Vietnamese one) + `Locale.US` fix |
| `src/androidTest/.../EditScreenTest.kt` | **UPDATE** | T-07 | Assert via `getString()` |
| `src/androidTest/.../DocumentListScreenTest.kt` | **UPDATE** | T-07 | Assert via `getString()` |
| `.github/workflows/build.yml` | **UPDATE** | T-12 | Add `lintDebug` required check |

**Not touched (explicitly out of scope):** `app/src/main/assets/release_notes.yaml` (ADR Decision C1 — stays English-only).

---

## Dependency Graph

```
T-01 (strings.xml additions)
  ├─► T-02 (DocumentViewerScreen + ReorderableThumbnailList)  ┐
  ├─► T-03 (EditScreen + Locale.US)                            │
  ├─► T-04 (DocumentListScreen + dialogs)                      ├─► T-07 (test updates)
  ├─► T-05 (ScannerScreen + ManualCropScreen)                  │
  └─► T-06 (SettingsScreen + Locale.US)                        ┘
                                                                   │
                                                                   ▼
                                                          T-08 (green gate + Hat-1 commit)
                                                                   │
                                                                   ▼
                                                          T-09 (values-vi/strings.xml)
                                                                   │
                                          ┌────────────────────────┼────────────────────────┐
                                          ▼                        ▼                        ▼
                                   T-10 (vi QA)            T-11 (fallback QA)        T-12 (CI lint gate)
                                          └────────────────────────┴────────────────────────┘
                                                                   │
                                                                   ▼
                                                          T-13 (final validation + Hat-2 commit)
```

**Critical path:** T-01 → (T-02..T-06, parallel) → T-07 → T-08 → T-09 → (T-10/T-11/T-12, parallel) → T-13

---

## Parallelization Window

### Window A — Phase 1 body (after T-01 lands)

| Stream | Task |
|--------|------|
| Builder 1 | T-02 (Viewer) |
| Builder 2 | T-03 (Edit) |
| Builder 3 | T-04 (DocumentList + dialogs) → T-05 (Scanner) → T-06 (Settings) |

All three streams touch disjoint files — safe to run fully in parallel. Converge at T-07.

### Window B — Phase 2/3 tail (after T-09 lands)

| Stream | Task |
|--------|------|
| Stream 1 | T-10 (vi QA) |
| Stream 2 | T-11 (fallback QA) |
| Stream 3 | T-12 (CI lint gate) |

---

## Risk Register

| ID | Risk | Likelihood | Impact | Mitigation |
|----|------|-----------|--------|------------|
| R-01 | Format-string positional args (`%1$d`, `%1$s`) reordered incorrectly in `values-vi/strings.xml` by translation | M | M | T-10 manual QA must exercise every interpolated string (page numbers, brightness/contrast labels) with real values, not just static text |
| R-02 | Vietnamese text overflow/truncation in fixed-width UI (buttons, chips, dialog titles) | M | M | T-10 explicitly checks every screen at vi length; adjust `Text` `maxLines`/`overflow` or widen containers where needed |
| R-03 | `onNodeWithText()` in instrumented tests silently breaks once English copy changes post-migration | L | M | T-07 converts assertions to `getString()`-based lookups before T-08's gate |
| R-04 | `lintDebug` surfaces pre-existing unrelated lint warnings when first enabled as a required CI check | M | L | Scope T-12 to only gate on `MissingTranslation`/`ExtraTranslation`; do not fail CI on unrelated pre-existing lint categories (baseline them if noisy) |

---

## Definition of Done

A task is complete when:
- [ ] Code compiles with no errors
- [ ] No hardcoded UI string literals remain in `app/src/main/java/com/docscanner/ui/**`
- [ ] `./gradlew testDebugUnitTest --no-daemon` passes

The feature is done when:
- [ ] All T-01 through T-13 complete
- [ ] `values-vi/strings.xml` has 100% coverage of `values/strings.xml` keys (zero `MissingTranslation`)
- [ ] `./gradlew lintDebug` green, wired into CI
- [ ] Every ADR-0003 validation checkbox checked
- [ ] Manual vi + fallback-locale QA passed

---

## Execution Handoff

**For `/swarm-execute` or manual execution:**

1. Single builder starts T-01 (must land before anything else).
2. Fan out T-02–T-06 across up to 3 builders (disjoint files, no merge conflicts expected).
3. Converge: one builder does T-07, then T-08 (commit checkpoint — Hat 1 ends here).
4. T-09 (translation) is best done by one person/pass for terminology consistency (e.g. "Document" → "Tài liệu" used consistently), not split across builders.
5. T-10/T-11/T-12 can run in parallel after T-09.
6. T-13 closes the loop — final ADR-0003 checklist pass and Hat-2 commit.

---

## Version History

| Date | Author | Change |
|------|--------|--------|
| 2026-07-01 | nguyenhuuca@gmail.com | Initial plan generated from ADR-0003 |
