#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# DRS OS — tests/automation/run_automation_tests.sh
# Runs the deterministic rule-engine reference-model unit tests.
# Read-only, no network, no root, no host modification.
#
# Exit codes: 0 = all tests passed, 1 = test failure, 2 = skipped
#             (python3 unavailable).
set -u -o pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

if ! command -v python3 >/dev/null 2>&1; then
  echo "SKIP  python3 unavailable — automation reference tests cannot run"
  echo "      (reported as skipped, never as a false pass)"
  exit 2
fi

echo "== DRS OS automation rule-engine reference-model tests =="
python3 "$ROOT/tests/automation/test_rule_engine.py" 2>&1
rc=$?
if [ "$rc" -eq 0 ]; then
  echo "RESULT: PASS — automation reference model semantics verified."
  echo "NOTE: reference-model verification ≠ product integration; platform"
  echo "      engine tests are T-19/T-20 in docs/security/test-plan.md."
else
  echo "RESULT: FAIL (exit $rc) — rule engine deviates from the spec."
fi
exit $rc
