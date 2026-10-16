# Phase 3 — Build and Test Instructions

Status: instructions + what was ACTUALLY executed in the Phase 3
sandbox (2026-10-16). Execution records live in
docs/phase3/PHASE3_TEST_REPORT.md; this file is the reproducible path.

## 1. Toolchain setup (one-time, free)

Verified provenance (SHA-256 measured at download time):

| Tool | Version | Source | SHA-256 |
|---|---|---|---|
| Kotlin compiler | 2.0.21 | https://github.com/JetBrains/kotlin/releases/download/v2.0.21/kotlin-compiler-2.0.21.zip | `0352c0a45bd22f80f6b26e485cd04da8047baa5de54865281fb9f89a4a7bcf2a` |
| JUnit console | 1.10.2 | https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar | `a1de557821293ce903c213c694165fff532cf92081bac4238b9e05b35f04f43f` |

Default install layout expected by the scripts:
`/home/z/my-project/tools/kotlinc-2.0.21/kotlinc/bin/kotlinc` and
`/home/z/my-project/tools/junit-platform-console-standalone-1.10.2.jar`.
Override with `DRS_TOOLS_DIR`. A JRE 11+ is required (Phase 3 used the
present OpenJDK 21 JRE; `javac` is NOT needed for pure Kotlin).

## 2. Core modules (verified in this sandbox)

```bash
bash build/scripts/run_core_tests.sh
```

Compiles foundation → engine-core → security-core → settings-core and
runs all JUnit tests; also regenerates the differential vectors and
diffs them against the committed file (must be byte-identical).
Exit 0 = all executed tests passed. Exit 1 = compile error or test
failure (never ignored). Missing toolchain → `BLOCKED` output with the
exact prerequisite, exit 0, zero tests executed — never a fake PASS.

## 3. Android apps (requires a host with the Android SDK)

The sandbox has no Android SDK; on a host that does:

```bash
# prerequisites: JDK 17, Android SDK platform 35 + build-tools
# (sdkmanager "platforms;android-35" "build-tools;35.0.0")
cd packages/apps/DRSSecurityCenter && gradle :app:assembleDebug :app:testDebugUnitTest
cd ../DRSAutomation && gradle :app:assembleDebug :app:testDebugUnitTest
cd ../DRSSettings     && gradle :app:assembleDebug :app:testDebugUnitTest
```

Until those commands are executed on a capable host, the three apps are
**SOURCE IMPLEMENTED — BUILD NOT VERIFIED**. Do not label them
build-ready on the strength of the core tests: Compose compilation,
resource linking, and manifest merge are exactly what remains unproven.
Suggested build host floor: 8 GB RAM (Gradle+Kotlin daemon), ~10 GB free
disk for SDK + dependency caches.

## 4. Static validation (verified in this sandbox)

```bash
bash build/scripts/check_phase3_code.sh   # Phase 3 code/resource checks
bash build/scripts/validate_project.sh    # full repository validation
bash tests/static/run_static_checks.sh    # Phase 2 regression suite
```

check_phase3_code.sh covers: AI/ML dependency tripwire, secret
tripwire, XML well-formedness, ar/en string parity (values/ Arabic
default vs values-en), manifest RTL+localeConfig presence, locales
config ar+en, SPDX headers + package naming + no TODO/FIXME/STOPSHIP in
Kotlin sources, source URL allowlist, version-catalog consistency.
Tripwires are not guarantees — the disclaimer is printed by the script.

## 5. Automation reference model (Phase 2 regression, verified)

```bash
bash tests/automation/run_automation_tests.sh   # Python reference, 25 tests
```

## 6. What must NEVER be claimed

- A successful `run_core_tests.sh` does NOT mean the Android apps build.
- A successful `assembleDebug` would NOT mean the AOSP platform builds
  or boots (docs/phase3/AOSP_INTEGRATION_STATUS.md).
- Static checks do not prove the absence of security vulnerabilities
  (docs/phase3/SECURITY_REVIEW.md).
