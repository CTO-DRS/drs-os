# Phase 4 — Build Results

All numbers below are from executed commands with recorded exit codes.
Nothing in this file is projected, assumed, or copied from an earlier
phase's claims. Artifacts are **debug** builds; no release build exists
and none is claimed (see §4).

## 1. Verdict per application

| App | Command (exact) | Exit | Result | Status label |
|---|---|---|---|---|
| DRSSettings | `gradle --console=plain :app:assembleDebug` (cwd `packages/apps/DRSSettings`) | 0 | `BUILD SUCCESSFUL in 1m 49s` | **BUILD VERIFIED — RUNTIME NOT VERIFIED** |
| DRSAutomation | same, cwd `packages/apps/DRSAutomation` | 0 | `BUILD SUCCESSFUL in 48s` | **BUILD VERIFIED — RUNTIME NOT VERIFIED** |
| DRSSecurityCenter | same, cwd `packages/apps/DRSSecurityCenter` | 0 | `BUILD SUCCESSFUL in 13s` (incremental) | **BUILD VERIFIED — RUNTIME NOT VERIFIED** |

Shared Kotlin components (composite included builds) compile as part of
every app build above and also build standalone
(`gradle -p components/<m> test` — exit 0 for all four).

Environment: Temurin JDK 17.0.20.1+1, Gradle 8.9, AGP 8.7.3, Kotlin
2.0.21, SDK platform 35 + build-tools 35.0.0 (TOOLCHAIN_REPORT.md).
Memory tuning: `GRADLE_USER_HOME` override (1536 MB Gradle heap,
768 MB Kotlin daemon) — repo files unchanged.

## 2. Defect ledger (real defects found by real builds)

Every item below failed an actual build or check before the fix; file
paths and mechanisms are exact.

| ID | Sev | Defect | Caught by | Fix |
|---|---|---|---|---|
| D-1 | HIGH | The four shared components had **no Gradle build files** while all three app builds `include`d them as subprojects → "project directory has no build file" class failure. | first `assembleDebug` attempt (baseline audit §8 predicted this) | `components/<m>/build.gradle.kts` ×4 (literal `id("org.jetbrains.kotlin.jvm") version "2.0.21"`, jvmToolchain 17, literal JUnit pins) |
| D-2 | HIGH | **Kotlin block-comment wildcard poison**: the component build files' comments contained `packages/apps/*/settings.gradle.kts`; `apps/` + `/*` opens a NESTED block comment (Kotlin comments nest), so the file's `plugins{}` block was silently swallowed → "No variants exist". This is why "plugin applied but zero variants" contradicted itself across probes. | systematic probe isolation (minimal control project compiled fine; poisoned file produced zero tasks/variants) | wildcard rewritten to `packages/apps/<app>/…` in comments; new guard check `check_phase4_build.sh` (comment-aware scan) prevents the whole class |
| D-3 | MED | `projectDir` subprojects **outside the consumer root** participate in zero variant exchange on Gradle 8.9 (consumer metadata empty even with a healthy producer — proven producer-side via `outgoingVariants` after D-2 fix) | `:app:assembleDebug` + `:foundation:outgoingVariants` | components converted to **composite included builds** with explicit `dependencySubstitution`; app deps switched to `com.drs.os:<module>:0.3.0` coordinates |
| D-4 | MED | `alias(libs.plugins.kotlin.jvm)` in build scripts is fragile here: with the catalog entry absent the generated accessor collides with `ToolchainManagement.jvm` (script compilation error); with the entry present it still no-ops in external-dir subprojects. A versioned `id(...)` request from a subproject fails with "already on the classpath with an unknown version" (kotlin-android already loads the same plugin jar). | script-compiler errors + probes during D-1/D-2 debugging | final shape: components are standalone builds declaring the versioned plugin themselves; root builds keep only the three AGP/Kotlin aliases; rationale documented in every affected file; `check_phase4_build.sh` enforces version lockstep |
| D-5 | HIGH | `settings-core`/`engine-core` **import foundation classes** (`KvStore`, `MiniJson`) but no build declared that dependency — masked in Phase 3 by `run_core_tests.sh` manually appending foundation classes to the classpath. First Gradle compile: `Unresolved reference 'foundation'` + cascade. | `:settings-core:compileKotlin` failure inside app build | `api("com.drs.os:foundation:0.3.0")` + own `includeBuild("../foundation")` substitution in both components |
| D-6 | HIGH | `SharedPreferencesStore` referenced by DRSAutomation `MainActivity` **never existed** in the Phase 3 tree (uncompilable source shipped as "SOURCE IMPLEMENTED") | `:app:compileDebugKotlin` | new `host/SharedPreferencesStore.kt` implementing `AutomationKeyValueStore` over `SharedPreferences` with synchronous `commit()` (honest durability boolean) |
| D-7 | MED | Compose defects in `AutomationScreens.kt`: (a) `import androidx.compose.foundation.layout.weight` resolves to a **file-internal** extension → compile error; (b) `stringResource()` called inside a non-composable `joinToString` lambda | `:app:compileDebugKotlin` | (a) import removed (weight is a Row/Column scope member); (b) `errorKeys.map { stringResource(it) }.joinToString("\n")` |
| D-8 | MED | DRSSecurityCenter: `ChecklistViewModel` imported from `…ui` but the class lives in `…ui.screens`; `DashboardPresentation.kt` used app `R` without import → unresolved + type-inference cascade | `:app:compileDebugKotlin` | import corrected; `import com.drs.os.securitycenter.R` added |
| D-9 | LOW | `run_core_tests.sh` JUnit `--scan-classpath` scanned the WHOLE classpath, so engine-core/settings-core steps re-ran foundation's 25 tests → Phase 3 reported "188" executions where the true unique count is **138** (25+79+18+16; +50 duplicate executions) | cross-checking Gradle XML counts (79/16) against kotlinc runner (104/41) | `--scan-classpath "$out"` (module output only); re-run green; correction recorded in TEST_RESULTS.md §3.1 |
| D-10 | LOW | Static scanners walked **Gradle build outputs** (CRLF `MANIFEST.MF`, >1 MiB zip caches, AGP-internal URLs) → false failures after real builds | `run_static_checks.sh` / `validate_project.sh` regression | scanners now prune `build/` and `.gradle/` dirs (repo's own source `build/scripts/` unaffected where checked by explicit lists); new URLs allowlisted with reasons in dependency-provenance.md |

## 3. Artifacts (debug; verified with aapt2 + apksigner)

| Artifact | Size (B) | SHA-256 |
|---|---|---|
| `packages/apps/DRSSettings/app/build/outputs/apk/debug/app-debug.apk` | 23,767,128 | `08cc0836bc045a0f3cd76522e09729e37d1b9d427cb0d4ddede6c1416a83041a` |
| `packages/apps/DRSAutomation/app/build/outputs/apk/debug/app-debug.apk` | 22,295,118 | `e326622050a8987a886e531fe26bb6b93387c034abe609a9ab2de4a587f477ae` |
| `packages/apps/DRSSecurityCenter/app/build/outputs/apk/debug/app-debug.apk` | 23,088,133 | `f8412fb80be817b01939b45438b76b06464b605e6bcb82e82e655b338aa90972` |

`aapt2 dump badging` (evidence, one per app): package names
`com.drs.os.drssettings` / `com.drs.os.automation` / `com.drs.os.securitycenter`,
all `versionCode=1 versionName=0.3.0-phase3`, `compileSdkVersion=35`,
Arabic default labels `إعدادات DRS` / `أتمتة DRS` / `مركز أمان DRS`.
Regenerate anytime: `bash build/scripts/inventory_artifacts.sh`.

## 4. Signing (verified, not assumed)

`apksigner verify --print-certs` on all three APKs:
`C=US, O=Android, CN=Android Debug` (SHA-256 cert digest
`bf79b2d369be62e307dfbe2ad8caa03e9d0b3194025db273f965629698d409fd` —
the standard public debug certificate). These are **debug-only
artifacts**: they must never be distributed, and no release build was
produced in this phase (release signing requires the key ceremony in
docs/security/key-management.md, which is intentionally not available).

## 5. Dependency resolution (first real resolution ever)

All pins from the Phase 3 catalogs resolved as-is on google() and
mavenCentral(): AGP 8.7.3, Kotlin 2.0.21 (+ compose compiler plugin),
Compose BOM 2024.12.01 (ui, ui-graphics, ui-tooling-preview, material3),
core-ktx 1.15.0, activity-compose 1.9.3, lifecycle 2.8.7, appcompat
1.7.0, navigation-compose 2.8.5, JUnit Jupiter 5.10.2. **No version
was upgraded** (task §3/§5). No additional repositories were added.
Hosts used: dl.google.com, repo1.maven.org, services.gradle.org,
api.adoptium.net — all recorded with reasons in
docs/build/dependency-provenance.md.

## 6. Reproduce

```bash
source /path/to/drs_build_env.sh   # or: export JAVA_HOME/ANDROID_HOME/PATH
cd drs-os
bash build/scripts/verify_all.sh            # everything, incl. builds
bash build/scripts/verify_all.sh --skip-builds   # static + JVM tests only
gradle --console=plain -p packages/apps/DRSSettings :app:assembleDebug   # one app
```

Toolchain bootstrap: TOOLCHAIN_REPORT.md §7. Build outputs are
git-ignored; the archive ships source only.
