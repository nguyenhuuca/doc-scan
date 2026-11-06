# Tech Strategy

The project's chosen technologies. Suggest an alternative only when the owner asks for one; otherwise build with what is listed here, at the latest stable version unless a version is pinned.

The installer fills the sections below from the stack packs it detected (`npx github:nguyenhuuca/agentic-sdlc init`). The owner confirms them with `/welcome`, which the first session offers; edit them freely afterwards: `update` never overwrites a file you changed.

<!-- sdlc:status confirmed 2026-11-06 -->

## Stack

<!-- sdlc:stack -->
| Area | Choice |
|------|--------|
| Language | Kotlin 2.0.21, JVM target 21 |
| UI | Jetpack Compose (BOM 2024.09.00) + Material3 |
| Architecture / DI | MVVM; manual DI via `AppContainer` |
| Database | Room 2.6.1, explicit migrations, schema JSON committed in `app/schemas/` |
| Async | Kotlin Coroutines 1.8.1 + Flow |
| Camera / scanning | CameraX 1.4.0 + ML Kit Document Scanner |
| Image loading | Coil 2.7.0 |
| Build | Gradle 8.9, AGP 8.5.2, KSP |
| Tests | JUnit 4 + Mockito 5 (unit); Compose UI tests + Room `MigrationTestHelper` (instrumented); JaCoCo coverage |
| Lint | Android Lint (EN/VI translation completeness in CI) |
| CI / release | GitHub Actions (JDK 24); APK published to GitHub Releases |
<!-- /sdlc:stack -->

## Not Allowed

<!-- sdlc:prohibited -->
_None chosen yet; see "Prohibited Patterns" in `CLAUDE.md`._
<!-- /sdlc:prohibited -->
