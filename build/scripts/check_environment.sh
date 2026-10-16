#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# DRS OS — build/scripts/check_environment.sh
# Read-only host capability report.
#
# Guarantees: no writes, no installs, no network access, no root, no
# destructive commands. Safe to run anywhere.
#
# Exit codes: 0 = report generated, required tools present.
#             1 = one or more REQUIRED tools missing.
#             FAIL is reserved for required-for-static-validation items;
#             build-capability gaps are reported as WARN/INFO, because a
#             host can be perfectly valid for static work without being
#             able to build AOSP (see docs/build/prerequisites.md).
set -u -o pipefail

passed=0; failed=0; warned=0
proj_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

result() { # result <VERDICT> <item> <detail>
  case "$1" in
    PASS) passed=$((passed+1));;
    FAIL) failed=$((failed+1));;
    WARN) warned=$((warned+1));;
  esac
  printf '%-5s %-22s %s\n' "$1" "$2" "$3"
}
section() { printf '\n== %s ==\n' "$1"; }

section "Host identity"
result "INFO" "kernel"   "$(uname -s 2>/dev/null || echo unknown)"
result "INFO" "arch"     "$(uname -m 2>/dev/null || echo unknown)"
osname="unknown"
if [ -r /etc/os-release ]; then
  osname="$(sed -n 's/^PRETTY_NAME=//p' /etc/os-release | tr -d '"' )"
fi
result "INFO" "os" "${osname:-unknown}"

section "Resources"
result "INFO" "cpu cores" "$(nproc 2>/dev/null || echo unknown)"
mem_total_kb="$(awk '/^MemTotal:/{print $2}' /proc/meminfo 2>/dev/null || true)"
if [ -n "${mem_total_kb:-}" ]; then
  mem_gib=$(( mem_total_kb / 1024 / 1024 ))
  if [ "$mem_gib" -lt 16 ]; then
    result "WARN" "memory total" "${mem_gib} GiB — far below the 64 GB AOSP build requirement (static work unaffected)"
  else
    result "PASS" "memory total" "${mem_gib} GiB (build-relevant floor met)"
  fi
else
  result "WARN" "memory total" "unavailable (/proc/meminfo not readable)"
fi
disk_avail_mb="$(df -Pm "$proj_root" 2>/dev/null | awk 'NR==2{print $4}')"
if [ -n "${disk_avail_mb:-}" ]; then
  disk_gib=$(( disk_avail_mb / 1024 ))
  if [ "$disk_gib" -lt 10 ]; then
    result "WARN" "disk free" "${disk_gib} GiB — below 10 GiB sanity floor for repo work"
  else
    result "INFO" "disk free" "${disk_gib} GiB at ${proj_root} (AOSP builds need ~400 GiB)"
  fi
else
  result "WARN" "disk free" "could not measure for ${proj_root}"
fi
if [ -e /dev/kvm ]; then
  if [ -r /dev/kvm ]; then
    result "INFO" "kvm" "present and readable (Cuttlefish-capable host)"
  else
    result "WARN" "kvm" "present but not readable for this user (kvm group membership needed)"
  fi
else
  result "INFO" "kvm" "absent (Cuttlefish cannot run here; static work unaffected)"
fi

section "Required tools (static validation)"
for t in bash git python3; do
  if command -v "$t" >/dev/null 2>&1; then
    v="$("$t" --version 2>&1 | head -n1 || true)"
    result "PASS" "$t" "${v:-present}"
  else
    result "FAIL" "$t" "missing — required for DRS OS static validation"
  fi
done

section "Optional tools (platform-build only; absence is normal here)"
for t in java repo docker make zip unzip; do
  if command -v "$t" >/dev/null 2>&1; then
    v="$("$t" --version 2>&1 | head -n1 || true)"
    result "INFO" "$t" "${v:-present}"
  else
    result "INFO" "$t" "not installed (only relevant on a capable build host)"
  fi
done

section "Network policy"
result "INFO" "network" "not probed by design — this script never contacts external services"

section "Summary"
printf 'PASS=%d WARN=%d FAIL=%d\n' "$passed" "$warned" "$failed"
if [ "$failed" -gt 0 ]; then
  echo "RESULT: FAIL — required tools missing; static validation cannot run here."
  exit 1
fi
echo "RESULT: PASS — environment sufficient for DRS OS static validation."
echo "NOTE: this is NOT a statement that an AOSP build is possible here."
