#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# DRS OS — top-level verification command (Phase 4, task §10).
#
# Runs every check the current environment supports and reports the
# rest as BLOCKED — never as PASS. Never deletes anything; read-only
# with the exception of the Gradle builds it optionally triggers
# (build outputs are git-ignored).
#
# Usage: verify_all.sh [--skip-builds]
#   --skip-builds  run static+test layers only (faster; no Gradle builds)
#
# Exit code: number of failed checks (capped at 125), 0 = all green.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$HERE/../.." && pwd)"
cd "$REPO_ROOT"

SKIP_BUILDS=0
[ "${1:-}" = "--skip-builds" ] && SKIP_BUILDS=1

pass=0; fail=0; blocked=0
note() { printf '%s\n' "$*"; }
run() { # run <label> <cmd...>
    local label="$1"; shift
    # "--" guards against labels beginning with "-" (e.g. Gradle task
    # names like ":app:assembleDebug" are fine, but a label starting
    # with a dash would be parsed as printf options — Phase 5 fix).
    printf -- '--- %s\n' "$label"
    if "$@" > /tmp/drs_verify_out.$$ 2>&1; then
        printf 'PASS  %s\n' "$label"; pass=$((pass + 1))
    else
        printf 'FAIL  %s (exit %s)\n' "$label" "$?"
        fail=$((fail + 1))
        tail -15 /tmp/drs_verify_out.$$ | sed 's/^/      /'
    fi
    rm -f /tmp/drs_verify_out.$$
    return 0
}
blocked_for() {
    printf 'BLOCKED  %s\n' "$1"
    printf '      reason: %s\n' "$2"
    blocked=$((blocked + 1))
}

note "== DRS OS verify_all =="
note ""

# ------------------------------------------------ layer 1: static checks
run "environment check"            bash build/scripts/check_environment.sh
run "manifest validation"          bash build/scripts/check_manifests.sh
run "sepolicy structural check"    bash build/scripts/check_sepolicy.sh
run "license headers"              bash build/scripts/check_license_headers.sh
run "static suite"                 bash tests/static/run_static_checks.sh
run "phase3 code checks"           bash build/scripts/check_phase3_code.sh
run "phase4 build-config checks"   bash build/scripts/check_phase4_build.sh
run "full project validation"      bash build/scripts/validate_project.sh

# ------------------------------------------------ layer 2: JVM tests
run "core-module tests (kotlinc)"  bash build/scripts/run_core_tests.sh
run "automation reference tests"   bash tests/automation/run_automation_tests.sh

# --------------------------------------- layers 2-4: Android SDK work
if command -v java > /dev/null 2>&1 && [ -n "${ANDROID_HOME:-}" ] \
   && [ -d "$ANDROID_HOME/platforms/android-35" ]; then
    if [ "$SKIP_BUILDS" = "0" ]; then
        for app in DRSSettings DRSAutomation DRSSecurityCenter; do
            run "$app :app:assembleDebug" \
                gradle --console=plain -p "packages/apps/$app" :app:assembleDebug
            run "$app :app:testDebugUnitTest" \
                gradle --console=plain -p "packages/apps/$app" :app:testDebugUnitTest
        done
        run "artifact inventory" bash build/scripts/inventory_artifacts.sh
    else
        blocked_for "per-app Gradle builds" "--skip-builds requested; run without the flag to build"
    fi
else
    blocked_for "per-app Gradle builds" "ANDROID_HOME/SDK platform 35 not found; install per docs/phase4/TOOLCHAIN_REPORT.md §7"
fi

# ------------------------------------------- layer 3: runtime (blocked)
if [ -e /dev/kvm ]; then
    blocked_for "emulator instrumented tests" "KVM present but no AVD configured; create one and rerun"
else
    blocked_for "emulator instrumented tests + screenshots" \
        "/dev/kvm absent — x86_64 emulator images require KVM. Exact setup on a capable host:
         sdkmanager 'emulator' 'system-images;android-35;google_apis;x86_64' &&
         avdmanager create avd -n drs-test -k 'system-images;android-35;google_apis;x86_64' &&
         emulator -avd drs-test &&
         gradle -p packages/apps/DRSAutomation :app:connectedDebugAndroidTest
         (connected tests are not yet authored; see docs/phase4/TEST_RESULTS.md §4)"
fi
blocked_for "full AOSP build/Cuttlefish boot" \
    "requires >=64 GiB RAM, ~400 GB disk, KVM (docs/build/prerequisites.md); sandbox has none of these"

# ------------------------------------------------------------ summary
note ""
note "== Summary =="
note "passed=$pass failed=$fail blocked=$blocked"
note "PASS  = check actually ran and passed.  FAIL  = ran and failed."
note "BLOCKED = required tool/device/environment unavailable (prerequisites above)."
note "Nothing is labeled PASS without execution; nothing is silently skipped."
if [ "$fail" -gt 0 ]; then
    exit "$((fail > 125 ? 125 : fail))"
fi
exit 0
