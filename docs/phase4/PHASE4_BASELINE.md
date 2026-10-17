# Phase 4 — Baseline Audit

Status: this document records the state of the repository **before any
Phase 4 change**, measured by direct inspection of the working tree at
commit `1f610b5` (tag `phase-3-native-foundation`), not by trusting any
earlier report. Method: reproduce, then record; never reconcile silently.

Audit date: 2026-10-17 (sandbox UTC). Auditor: Phase 4 session.

## 1. Actual project structure (verified on disk)

Git state at audit time: branch `main`, HEAD `1f610b5`, tree clean,
155 tracked files, tags `phase-1-scaffold`, `phase-2-integration-foundation`,
`phase-3-native-foundation`. Commit identity remains the sandbox preset
`Z User <z@container>` and must be replaced before any publication.

```text
drs-os/
  components/                     pure-Kotlin core modules (no Android deps)
    foundation/    src only       4 .kt files,   709 lines
    engine-core/   src only      10 .kt files, 1,736 lines
    security-core/ src only       6 .kt files,   614 lines
    settings-core/ src only       2 .kt files,   359 lines
  packages/apps/                  three INDEPENDENT Gradle builds (no root
                                  build spanning all apps; no wrapper)
    DRSSecurityCenter/  app/src:  8 .kt files,   876 lines
    DRSAutomation/      app/src: 11 .kt files, 1,385 lines
    DRSSettings/        app/src:  3 .kt files,   401 lines
  build/scripts/                  6 validation/test scripts (read-only checks)
  tests/{static,automation}/      static suite + Python reference tests
  docs/                           phase 0-3 documentation (preserved)
  manifests/, platform/, overlays/, device/, vendor/, branding/
```

Module names in the Phase 4 task ("DRSSecurityCenter", "DRSAutomation",
"DRSSettings", "foundation", "engine-core", "security-core",
"settings-core") all match actual directory names. The Android modules
live at `packages/apps/<Name>/` with a nested `app/` Gradle module.

## 2. Source modules discovered

| Module | Path | Kind | Notes |
|---|---|---|---|
| foundation | `components/foundation` | Kotlin (JVM) | KvStore, MiniJson; stdlib only |
| engine-core | `components/engine-core` | Kotlin (JVM) | deterministic rule engine port of Phase 2 spec |
| security-core | `components/security-core` | Kotlin (JVM) | 4-state checklist model |
| settings-core | `components/settings-core` | Kotlin (JVM) | corruption-tolerant settings |
| DRSSecurityCenter | `packages/apps/DRSSecurityCenter/app` | Android app (Compose) | includes `:foundation`, `:security-core` |
| DRSAutomation | `packages/apps/DRSAutomation/app` | Android app (Compose) | includes `:foundation`, `:engine-core` |
| DRSSettings | `packages/apps/DRSSettings/app` | Android app (Compose) | includes `:foundation`, `:settings-core` |

Cross-component imports were checked by grep: **none**. Each component
depends only on the Kotlin stdlib. This matters for the Phase 4 fix that
adds Gradle build files to the components (see defect D-1).

## 3. Available tests (as inherited from Phase 3)

| Suite | Runner | Status at baseline |
|---|---|---|
| foundation tests (25) | `build/scripts/run_core_tests.sh` (kotlinc + JUnit console) | PASS in Phase 3; re-verified this session |
| engine-core tests (104, incl. 34 differential vs Python reference) | same | PASS in Phase 3; re-verified this session |
| security-core tests (18) | same | PASS in Phase 3; re-verified this session |
| settings-core tests (41) | same | PASS in Phase 3; re-verified this session |
| DRSAutomation app unit tests (`app/src/test`, 2 files) | Gradle `testDebugUnitTest` | **NOT RUN in Phase 3** (no SDK); needs Gradle build |
| DRSSecurityCenter / DRSSettings app unit tests | Gradle | test deps declared; JVM unit tests to be run once Gradle works |
| Automation Python reference (25) | `tests/automation/run_automation_tests.sh` | PASS in Phase 3; re-verified this session |
| Static suites | `build/scripts/validate_project.sh` (109 checks), `check_phase3_code.sh` (19), `run_static_checks.sh` | PASS; re-verified this session |

## 4. Existing build configuration (verified by reading the files)

Per app (`packages/apps/<App>/`): `settings.gradle.kts` (pluginManagement
google/mavenCentral/gradlePluginPortal; FAIL_ON_PROJECT_REPOS; includes
`:app` plus two shared components via `projectDir = file("../../../components/<m>")`),
root `build.gradle.kts` (AGP/Kotlin/Compose plugins, `apply false`),
`gradle/libs.versions.toml` (pinned catalog), `gradle.properties`
(`-Xmx2048m`, `android.useAndroidX=true`, parallel off), `app/build.gradle.kts`.

Pins (identical in all three catalogs): AGP 8.7.3, Kotlin 2.0.21,
Compose BOM 2024.12.01, core-ktx 1.15.0, activity-compose 1.9.3,
lifecycle 2.8.7, appcompat 1.7.0 (DRSSettings), navigation-compose
2.8.5 (DRSSecurityCenter, explicitly pinned in its catalog — an
initial suspicion that navigation was unpinned was **checked and
dismissed** on full-file read), JUnit Jupiter 5.10.2 / launcher 1.10.2.
Apps: `compileSdk = 35`, `minSdk = 29`, `targetSdk = 35`,
`versionName = "0.3.0-phase3"`, `resourceConfigurations ar,en`,
Compose + buildConfig on, JVM target 17, debug-only signing by default.

Manifests: all three apps request **zero permissions**, declare
`<queries>` intent/package visibility only, `allowBackup="false"`,
`supportsRtl="true"`, `android:localeConfig`, launcher activity exported
(standard). DRSAutomation has no `<queries>` block (correct: it needs
none for its v1 scope).

## 5. Current placeholders and incomplete implementations

All known and documented (not hidden):

1. `manifests/local_manifests/` is **empty by policy**; the placeholder
   manifest lives only at `manifests/templates/drs-os-local.xml.template`
   with `git.invalid` remotes, marked INACTIVE/NON-BUILDABLE. Production
   scope is guarded by `check_manifests.sh` rejection patterns.
2. `platform/integration/` contains three INACTIVE `Android.bp`
   templates and an intentionally EMPTY privapp-permissions directory —
   documented as not-activated.
3. DRSAutomation: `TIME_DAILY` trigger exists in the model but is not
   creatable from the UI (no scheduler backend yet) — honest UI labeling,
   documented.
4. No TODO/FIXME/CHANGEME/REPLACE_ME markers exist in source or config
   (sweep re-verified; matches only in validator scan patterns and policy
   docs).
5. Phase 3 status labels remain in effect for all three apps:
   `SOURCE IMPLEMENTED — BUILD NOT VERIFIED`.

## 6. Previously reported results vs. independently reproduced results

The conversation summary carried into this session claimed Phase 3 had
"not started". Disk + worklog evidence contradicted that: Phase 2
(`d7c182c`) and Phase 3 (`1f610b5`) were completed in a prior session.
This discrepancy is explained explicitly rather than silently ignored:
the summary was stale; the repository was the source of truth.

All Phase 3 claims were re-executed this session (worklog Task ID 5)
**before** any Phase 4 change, with identical outcomes:

| Claim (Phase 3 report) | Reproduced this session | Match |
|---|---|---|
| `validate_project.sh` exit 0, 109/0/0 | exit 0, 109/0/0 | yes |
| `check_phase3_code.sh` exit 0, 19/19 | exit 0, 19/19 | yes |
| `run_core_tests.sh` 188/188 (25+104+18+41) | exit 0, 188/188 | yes |
| automation reference 25/25 | exit 0, PASS | yes |
| phase3 ZIP sha256 `b66ffa23…6e27`, 155 files | recomputed, matches; 313 entries = 158 dirs + 155 files | yes |
| no secrets / no placeholders in production scope | sweeps clean | yes |

No discrepancies remain unexplained.

## 7. Toolchain and resource limitations (measured at baseline, before tooling install)

- Debian 13, x86-64, 2 vCPU (Xeon), 3.9 GiB RAM, 8.9 GiB free disk.
- Host JRE 21 **without javac**; no Gradle, no Android SDK, no emulator,
  no `/dev/kvm`, no `repo`, no AOSP tree, no sudo.
- Network available (HTTPS to api.github.com, dl.google.com verified).
- Consequence recorded in Phase 3: apps were source-complete but
  unverified. Phase 4 installs a free toolchain (Temurin JDK 17,
  Gradle 8.9, cmdline-tools + platform 35/build-tools 35.0.0) to close
  this gap; see `TOOLCHAIN_REPORT.md`.

## 8. Prioritized defect list (from baseline audit, before builds)

Resolution record: BUILD_RESULTS.md §2 (defect ledger D-1 … D-10, with the
mechanisms actually observed — which differ from the initially planned
actions for D-1, as expected when a build first runs).

| ID | Severity | Finding | Evidence | Action in Phase 4 |
|---|---|---|---|---|
| D-1 | HIGH | The four shared component modules have **no Gradle build files**, yet every app `settings.gradle.kts` does `include(":foundation")` etc. pointing at those directories. Any app build fails immediately ("project directory has no build file"). Phase 3 could not catch this because no Gradle build was ever attempted. | `ls components/*` shows only `build/` (kotlinc output) and `src/`; app settings files include them | Add `build.gradle.kts` to each component + `kotlin-jvm` plugin to each catalog/root build |
| D-2 | HIGH (risk) | App Kotlin sources have never been compiled; unknown number of compile defects may surface (API misuse, unresolved references). Cannot be enumerated before a real compiler runs. | Phase 3 label "BUILD NOT VERIFIED" | Build each app; fix real defects; record each with compiler output |
| D-3 | LOW (environment) | `org.gradle.jvmargs=-Xmx2048m` (repo `gradle.properties`) plus a Kotlin daemon risks OOM on a 3.9 GiB sandbox. Not a repo defect; tuned via `GRADLE_USER_HOME` override (1536m / 768m) documented in `TOOLCHAIN_REPORT.md`; repo files unchanged for upstream users. | `free -h`; `gradle.properties` | Sandbox-only override |

An initially suspected D-4 ("navigation-compose unpinned") was verified
against the full catalog file and dismissed — `navigationCompose = "2.8.5"`
is pinned in `packages/apps/DRSSecurityCenter/gradle/libs.versions.toml`.
Recorded here so the verification trail is explicit.

Item D-2 is intentionally listed as a *risk with unknown count*, not as a
fabricated defect list — real defects will be recorded with compiler
output as they are found and fixed.
