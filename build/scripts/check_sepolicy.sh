#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# DRS OS — build/scripts/check_sepolicy.sh
# Guards the SELinux scaffold against policy drift:
#   * permissive domains are forbidden unconditionally (hard fail)
#   * allow-type rules require a justification entry in
#     docs/security/sepolicy-justifications.md
#
# Guarantees: read-only, no network, no root, no host modification.
# Full policy analysis (neverallow verification, sepolicy-analyze,
# boot-time enforcing check) requires a real AOSP tree and is reported as
# not-run — never as a pass.
#
# Exit codes: 0 = scaffold state acceptable, 1 = violation found.
set -u -o pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SEP="$ROOT/system/sepolicy"
JUST="$ROOT/docs/security/sepolicy-justifications.md"
rc=0

shopt -s nullglob
te_files=("$SEP"/*.te)
ctx_files=("$SEP"/*_contexts)
shopt -u nullglob

if [ "${#te_files[@]}" -eq 0 ] && [ "${#ctx_files[@]}" -eq 0 ]; then
  echo "PASS  no DRS sepolicy statements present (scaffold state; upstream"
  echo "      enforcing policy applies unchanged at build time — see"
  echo "      system/sepolicy/README.md)"
else
  for f in "${te_files[@]}"; do
    rel="${f#"$ROOT"/}"
    if grep -Eq '^[[:space:]]*permissive\b' "$f"; then
      echo "FAIL  permissive domain declared in $rel — forbidden (C-1)"
      rc=1
    fi
    if grep -Eq '^[[:space:]]*(allow|neverallow|allowxperm|auditallow|dontaudit)\b' "$f"; then
      if [ ! -f "$JUST" ]; then
        echo "FAIL  policy statements in $rel but no docs/security/sepolicy-justifications.md"
        rc=1
      elif ! grep -qF "$(basename "$f")" "$JUST"; then
        echo "FAIL  $rel is not justified in docs/security/sepolicy-justifications.md"
        rc=1
      else
        echo "PASS  $rel carries a justification entry"
      fi
    else
      echo "PASS  $rel contains no allow-type rules"
    fi
  done
  for f in "${ctx_files[@]}"; do
    rel="${f#"$ROOT"/}"
    echo "WARN  contexts file present: $rel — verify labels against the real"
    echo "      tree at integration time (not verifiable in this environment)"
  done
fi

echo "SKIP  neverallow/domain analysis requires a full AOSP checkout"
echo "      (sepolicy-analyze, build-time checks, boot-time getenforce)"
echo "NOTE  product configs must never set SELINUX_PERMISSIVE or disable"
echo "      enforcing mode (security-controls.md C-1)."
exit $rc
