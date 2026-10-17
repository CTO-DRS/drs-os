# Phase 4 — Final Report

Phase: 4 — Build Verification, Runtime Testing & AOSP Integration
Date: 2026-10-17 (sandbox UTC) · Tree: `main` at Phase 4 commit ·
Method: every claim in this report has a command, an exit code, or a
file behind it.

## 1. Executive summary

The phase turned Phase 3's "SOURCE IMPLEMENTED — BUILD NOT VERIFIED"
into **three real, independently built, debug-signed APKs** and a
build system whose invariants are mechanically guarded — by installing
a free toolchain (Temurin JDK 17.0.20.1+1, Gradle 8.9, Android SDK 35,
build-tools 35.0.0), attempting real builds, and fixing **10 real
defects** the uncompilable Phase 3 tree necessarily contained. The most
serious (D-2) was a Kotlin block-comment nesting bug that silently
swallowed entire build files, and (D-6) a class referenced but never
written. All feasible tests pass (147 unique, 0 failures); everything
infeasible here is labeled BLOCKED/NOT RUN with exact prerequisites.
Runtime behavior remains unverified by necessity — the honest status of
each app is **BUILD VERIFIED — RUNTIME NOT VERIFIED**. AOSP integration
remains NOT STARTED and is not claimed.

## 2. Initial environment and toolchain

Before installs: JRE 21 (no javac), no Gradle/SDK/emulator/KVM, 2 vCPU,
3.9 GiB RAM, 8.9 GiB disk. Installed (free, outside the repo): Temurin
JDK 17.0.20.1+1 (javac verified), Gradle 8.9 (official SHA-256
verified), cmdline-tools 12.0, platform-35, build-tools 35.0.0,
platform-tools; SDK licenses accepted. Full detail with sizes and
reproduce commands: TOOLCHAIN_REPORT.md. No version pin from the Phase 3
catalogs was changed.

## 3. Applications and modules inspected

All seven named modules exist under the actual names: 4 pure-Kotlin
components (foundation 709 LoC, engine-core 1,736, security-core 614,
settings-core 359) and 3 Compose apps (876/1,385/401 LoC app-side).
Build files, manifests, resources, tests, validation scripts, and
AOSP templates were read and recorded in PHASE4_BASELINE.md (including
the stale-summary discrepancy, resolved explicitly: Phase 2/3 were
completed in a prior session and re-verified before any change).

## 4. Files changed and defects fixed

New: 4 component `build.gradle.kts` + 4 component `settings.gradle.kts`,
`SharedPreferencesStore.kt`, 3 Phase 4 check/verify scripts
(`check_phase4_build.sh`, `verify_all.sh`, `inventory_artifacts.sh`),
8 Phase 4 documents. Modified: 3 app `settings.gradle.kts` (composite
wiring), 3 `app/build.gradle.kts` (coordinates), 3 root build files
(rationale comments), `run_core_tests.sh` (scan scope + output dir),
`run_static_checks.sh` + `validate_project.sh` (prune build outputs,
allowlist), `dependency-provenance.md`, `tools/README.md`. Full ledger
with mechanisms: BUILD_RESULTS.md §2 (D-1 … D-10).

## 5. Build results per application

| App | Result | Artifact (debug APK) | SHA-256 (full values in BUILD_RESULTS §3) |
|---|---|---|---|
| DRSSettings | exit 0, `BUILD SUCCESSFUL in 1m 49s` | 23,767,128 B | `08cc0836…3041a` |
| DRSAutomation | exit 0, `BUILD SUCCESSFUL in 48s` | 22,295,118 B | `e3266220…477ae` |
| DRSSecurityCenter | exit 0, `BUILD SUCCESSFUL in 13s` | 23,088,133 B | `f8412fb8…90972` |

Verified with `aapt2 dump badging` (package, version, compileSdk,
Arabic labels present in the binaries) and `apksigner verify`
(CN=Android Debug — debug-only, non-distributable).

## 6. Test results and exact commands

147 unique tests, 0 failures: core JVM suites 25+79+18+16 (two runners:
kotlinc+JUnit console and Gradle, both exit 0), automation reference
25, DRSAutomation app unit tests 9/9 (`gradle -p
packages/apps/DRSAutomation :app:testDebugUnitTest`). Static layers:
validate_project 110/0/0, check_phase4_build 22/22, check_phase3_code
19/19, full matrix in TEST_RESULTS.md. Accounting correction vs Phase 3
documented (188 → 138; runner classpath-scan bug, D-9).

## 7. APK artifacts produced and hashes

Three debug APKs — see §5 table; regenerate with
`bash build/scripts/inventory_artifacts.sh`. No release artifacts
exist; none is claimed.

## 8. Runtime and emulator results

None — BLOCKED (no KVM, 2 vCPU, 3.9 GiB). Zero screenshots or emulator
logs exist because none were produced; the exact setup commands for a
capable host are recorded in TEST_RESULTS.md §4 and encoded as BLOCKED
output in `verify_all.sh`.

## 9. Security findings

Audit in SECURITY_AUDIT.md: 0 permissions across all three APKs
(binary-verified), launcher-only export surface, allowBackup=false, no
network code/permission, one benign log line, debug-only signing, no
WebView/crypto/shell/deserialization surface. Findings S-1..S-4 are all
INFO/NOTE. Explicit non-claims section included (static passes ≠ OS
security).

## 10. AOSP integration progress

Zero integration performed, accurately: platform/ templates remain
INACTIVE, the production manifest scope remains empty-by-policy with
rejection guards, no remote invented. What Phase 4 added is the
composite-build topology that maps cleanly onto future Soong shims, and
the proof that the permissionless-API strategy survives real
compilation. Six falsification criteria for "integrated" are listed;
zero are true. AOSP_INTEGRATION_STATUS.md.

## 11. Blockers and missing prerequisites

Runtime verification (KVM host), instrumented tests (author + device),
Robolectric decision (Phase 5), DRS-owned Git remotes (user
authorization), compliant AOSP host for any platform work, sandbox
commit identity before publication. Full list with consequences:
KNOWN_LIMITATIONS.md.

## 12. Exact reproduction steps

```bash
# 1. toolchain (sizes/urls/hashes in TOOLCHAIN_REPORT §7)
#    → JDK 17 + Gradle 8.9 + SDK 35/build-tools 35.0.0, licenses accepted
# 2. memory override for small hosts (GRADLE_USER_HOME/gradle.properties)
# 3. verification:
bash build/scripts/verify_all.sh              # all layers incl. builds
bash build/scripts/verify_all.sh --skip-builds
# 4. single build:
gradle --console=plain -p packages/apps/DRSSettings :app:assembleDebug
# 5. artifact hashes:
bash build/scripts/inventory_artifacts.sh
```

## 13. Recommended Phase 5 scope

1. **Runtime verification first**: on a KVM-capable host — install the
   3 APKs, launch each, exercise navigation + RTL/EN switching, capture
   logs/screenshots as evidence; author and run the connectedTest sets
   for the engine's deterministic paths (T-16..T-20).
2. **Robolectric Compose smoke tests** as the no-device CI layer.
3. **DRS remote authorization + real remotes**; replace sandbox commit
   identity; then and only then activate a local manifest against the
   verified remotes.
4. **Platform integration spike on a real AOSP checkout**: Soong shims
   for the four cores, privapp-permissions review per permission,
   Surface #8/#9 injection design — each with the six falsification
   criteria of AOSP_INTEGRATION_STATUS §4.
5. **Scheduler decision** for TIME_DAILY against real WorkManager
   behavior, with battery/permission documentation per the task rules.

Phase 4 status label for the whole project, unchanged and explicit:
**NOT production-ready.** Components: BUILD VERIFIED — RUNTIME NOT
VERIFIED; OS integration: NOT STARTED.
