#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# DRS OS Phase 3 — reproducible JVM test runner for the pure-Kotlin
# core modules. Compiles each module with kotlinc and runs its JUnit
# tests with the JUnit Platform Console Launcher.
#
# Honest-result contract:
#   * every executed test is reported with real outcomes;
#   * a missing toolchain prints BLOCKED with the exact prerequisite and
#     exits 0 with zero executed tests — it never fakes a PASS;
#   * any compile error or test failure exits non-zero.
#
# Environment overrides:
#   DRS_TOOLS_DIR  directory holding kotlin-compiler-2.0.21/ and the
#                  JUnit console jar (default: /home/z/my-project/tools)
set -u

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

TOOLS_DIR="${DRS_TOOLS_DIR:-/home/z/my-project/tools}"
KOTLINC_BIN="$TOOLS_DIR/kotlinc-2.0.21/kotlinc/bin/kotlinc"
JUNIT_JAR="$TOOLS_DIR/junit-platform-console-standalone-1.10.2.jar"
KOTLIN_STDLIB="$TOOLS_DIR/kotlinc-2.0.21/kotlinc/lib/kotlin-stdlib.jar"

pass_count=0
fail_count=0
skipped=0
blocked=0

note() { printf '%s\n' "$*"; }

# ---------------------------------------------------------------- prereqs
missing=""
if ! command -v java >/dev/null 2>&1; then
    missing="$missing java(JRE-11+)"
fi
if [ ! -x "$KOTLINC_BIN" ]; then
    missing="$missing kotlinc-2.0.21(at:$TOOLS_DIR)"
fi
if [ ! -f "$JUNIT_JAR" ]; then
    missing="$missing junit-console-jar(at:$TOOLS_DIR)"
fi
if [ -n "$missing" ]; then
    note "== Kotlin core tests =="
    note "BLOCKED — required toolchain absent:$missing"
    note "To run: install the tools above (free downloads; provenance in"
    note "tools/README.md) or point DRS_TOOLS_DIR at an existing install."
    note "No tests were executed; nothing is reported as PASS."
    blocked=1
    note "Summary: passed=0 failed=0 blocked=1"
    exit 0
fi

# ------------------------------------------------------------ modules
# module_dir | extra compile classpath
MODULES=(
    "components/foundation:"
    "components/engine-core:components/foundation/build/kotlinc/classes"
    "components/security-core:"
    "components/settings-core:components/foundation/build/kotlinc/classes"
)

note "== Kotlin core tests (JVM, kotlinc 2.0.21 + JUnit Platform 1.10.2) =="

for entry in "${MODULES[@]}"; do
    module="${entry%%:*}"
    extra_cp="${entry#*:}"
    name="$(basename "$module")"
    # kotlinc output lives under build/kotlinc/ so it never mixes with
    # Gradle's own build/classes layout (Phase 4: components are also
    # Gradle subprojects now; keep the two runners' outputs separate).
    out="$module/build/kotlinc/classes"
    rm -rf "$out"
    mkdir -p "$out"
    cp="$JUNIT_JAR"
    [ -n "$extra_cp" ] && cp="$cp:$extra_cp"

    compile_output="$("$KOTLINC_BIN" "$module/src/main/kotlin" "$module/src/test/kotlin" \
        -cp "$cp" -d "$out" 2>&1)"
    compile_rc=$?
    if [ $compile_rc -ne 0 ]; then
        note "FAIL  $name — compile error (exit $compile_rc)"
        printf '%s\n' "$compile_output" | head -20
        fail_count=$((fail_count + 1))
        continue
    fi

    test_output="$(java -Ddrs.vectors="$REPO_ROOT/tools/automation-reference/test_vectors.json" \
        -jar "$JUNIT_JAR" execute \
        -cp "$out:$KOTLIN_STDLIB:$extra_cp" \
        --scan-classpath "$out" --fail-if-no-tests --details=summary 2>&1)"
    test_rc=$?
    succeeded="$(printf '%s\n' "$test_output" | sed -n 's/^\[[^0-9]*\([0-9][0-9]*\) tests successful.*/\1/p')"
    failed="$(printf '%s\n' "$test_output" | sed -n 's/^\[[^0-9]*\([0-9][0-9]*\) tests failed.*/\1/p')"
    succeeded="${succeeded:-0}"
    failed="${failed:-0}"
    if [ $test_rc -ne 0 ]; then
        note "FAIL  $name — tests failed: $failed succeeded: $succeeded (exit $test_rc)"
        printf '%s\n' "$test_output" | tail -15
        fail_count=$((fail_count + 1))
    else
        note "PASS  $name — $succeeded tests succeeded, 0 failed"
        pass_count=$((pass_count + 1))
    fi
done

# ------------------------------------------ vector determinism check
if command -v python3 >/dev/null 2>&1; then
    tmp_vectors="$(mktemp -d)/test_vectors.json"
    if python3 tools/automation-reference/generate_test_vectors.py \
        --out "$tmp_vectors" >/dev/null 2>&1 \
        && diff -q "$tmp_vectors" tools/automation-reference/test_vectors.json >/dev/null 2>&1; then
        note "PASS  differential-vectors — regeneration is byte-identical (deterministic)"
        pass_count=$((pass_count + 1))
    else
        note "FAIL  differential-vectors — regeneration differs from committed file"
        fail_count=$((fail_count + 1))
    fi
else
    note "SKIP  differential-vectors — python3 not available"
    skipped=$((skipped + 1))
fi

note "== Summary: passed=$pass_count failed=$fail_count skipped=$skipped =="
if [ $fail_count -gt 0 ]; then
    note "RESULT: FAIL — see failures above; fix before any integration claim."
    exit 1
fi
note "RESULT: PASS — all executed core-module tests succeeded."
note "NOTE: these are JVM domain-logic tests; they are NOT platform or"
note "      Android-build verification (see docs/phase3/BUILD_AND_TEST.md)."
exit 0
