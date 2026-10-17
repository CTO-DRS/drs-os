#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# DRS OS — artifact inventory (read-only): APKs produced by Phase 4
# builds, with size and SHA-256. Exits 0 when at least one artifact is
# inventoried; exits 1 if the build outputs referenced do not exist
# (honest failure, never a fake listing).
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$HERE/../.." && pwd)"

note() { printf '%s\n' "$*"; }
count=0
note "== DRS OS APK artifacts =="
for app in DRSSettings DRSAutomation DRSSecurityCenter; do
    apk="$REPO_ROOT/packages/apps/$app/app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$apk" ] && [ -s "$apk" ]; then
        size="$(wc -c < "$apk" | tr -d ' ')"
        sha="$(sha256sum "$apk" | cut -d' ' -f1)"
        note "apk      : $app (debug)"
        note "path     : packages/apps/$app/app/build/outputs/apk/debug/app-debug.apk"
        note "size     : $size bytes"
        note "sha256   : $sha"
        note ""
        count=$((count + 1))
    fi
done
note "total artifacts: $count"
[ "$count" -gt 0 ] || { note "RESULT: FAIL — no APK artifacts found (nothing is invented)."; exit 1; }
note "RESULT: PASS — inventory complete."
note "NOTE: an APK being present does NOT prove it installs, launches, or"
note "      performs correctly (runtime verification requires a device/emulator)."
exit 0
