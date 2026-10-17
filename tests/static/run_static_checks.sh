#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# DRS OS — tests/static/run_static_checks.sh
# Static test suite entry point. Executed checks:
#   1. bash -n syntax check over every shell script in the repo
#   2. CRLF line-ending scan
#   3. oversized-file scan (> 1 MiB — this repo is text-only by design)
#   4. secret scan (token/key patterns)
#   5. SPDX license-header check (delegates to build/scripts tool)
#
# Guarantees: read-only, no network, no root, no destructive commands.
# Exit codes: 0 = all executed checks passed, 1 = failures found.
set -u -o pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
failed=0
fail() { printf 'FAIL  %s\n' "$1"; failed=$((failed+1)); }

echo "== 1. bash -n over all shell scripts =="
count=0
while IFS= read -r -d '' s; do
  count=$((count+1))
  if ! err="$(bash -n "$s" 2>&1)"; then
    fail "syntax: ${s#"$ROOT"/}: $err"
  fi
done < <(find "$ROOT" -type f -name '*.sh' -not -path '*/.git/*' -print0 2>/dev/null)
echo "checked ${count} script(s)"

echo "== 2. line endings (no CRLF) =="
crlf="$(grep -rIl $'\r' --exclude-dir=.git --exclude-dir=build --exclude-dir=.gradle "$ROOT" 2>/dev/null || true)"
if [ -n "$crlf" ]; then
  fail "CRLF line endings found in:"
  printf '%s\n' "$crlf"
else
  echo "PASS  no CRLF line endings"
fi

echo "== 3. oversized files (> 1 MiB) =="
big="$(find "$ROOT" \( -type d \( -name build -o -name .gradle \) -prune \) -o \( -type f -size +1M -not -path '*/.git/*' -print \) 2>/dev/null || true)"
if [ -n "$big" ]; then
  fail "oversized files present (repository is text-only by design):"
  printf '%s\n' "$big"
else
  echo "PASS  no oversized files"
fi

echo "== 4. secret scan =="
# Self-scan safe: pattern literals cannot match themselves.
sec="$(grep -rInE --exclude-dir=.git --exclude-dir=build --exclude-dir=.gradle \
  -e 'ghp_[A-Za-z0-9]{36}' \
  -e 'gho_[A-Za-z0-9]{36}' \
  -e 'ghs_[A-Za-z0-9]{36}' \
  -e 'github_pat_[A-Za-z0-9_]{22,}' \
  -e 'AKIA[0-9A-Z]{16}' \
  -e 'xox[baprs]-[A-Za-z0-9-]{10,}' \
  -e '-----BEGIN [A-Z ]*PRIVATE KEY-----' \
  "$ROOT" 2>/dev/null || true)"
if [ -n "$sec" ]; then
  fail "secret-like material found — rotate the credential AND remove it:"
  printf '%s\n' "$sec"
else
  echo "PASS  no secret-like material detected"
fi

echo "== 5. SPDX license headers =="
if bash "$ROOT/build/scripts/check_license_headers.sh"; then
  :
else
  fail "license header check reported violations (see output above)"
fi

echo "== 6. manifest validation (production placeholder rejection) =="
if bash "$ROOT/build/scripts/check_manifests.sh"; then
  :
else
  fail "manifest validation reported failures (see output above)"
fi

echo
if [ "$failed" -gt 0 ]; then
  printf 'RESULT: FAIL — %d failing check(s)\n' "$failed"
  exit 1
fi
echo "RESULT: PASS — all executed static checks succeeded."
echo "NOTE: this validates repository content only; it is not a platform test."
