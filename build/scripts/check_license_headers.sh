#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# DRS OS — build/scripts/check_license_headers.sh
# Policy: shell scripts and XML manifest templates must carry an
# `SPDX-License-Identifier: Apache-2.0` within their first 20 lines.
# Markdown documentation is governed by LICENSE at the repo root and is
# intentionally not checked here.
#
# Guarantees: read-only, no network, no root, no host modification.
#
# Exit codes: 0 = all checked files compliant, 1 = violations found.
set -u -o pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
rc=0
checked=0

check_file() {
  local f="$1" rel
  rel="${f#"$ROOT"/}"
  checked=$((checked+1))
  if head -n 20 "$f" | grep -q 'SPDX-License-Identifier: Apache-2.0'; then
    echo "PASS  $rel"
  else
    echo "FAIL  missing SPDX header (first 20 lines): $rel"
    rc=1
  fi
}

while IFS= read -r -d '' f; do
  check_file "$f"
done < <(
  find "$ROOT/build" "$ROOT/tests" "$ROOT/scripts" \
    -type f -name '*.sh' -print0 2>/dev/null
  find "$ROOT/manifests" -type f \
    \( -name '*.xml' -o -name '*.xml.template' \) -print0 2>/dev/null
)

if [ "$checked" -eq 0 ]; then
  echo "SKIP  no candidate files found (nothing to check)"
  exit 0
fi
printf '\n%d file(s) checked\n' "$checked"
exit $rc
