#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# DRS OS — build/scripts/validate_project.sh
# Orchestrates read-only static validation of the DRS OS repository.
#
# Guarantees: no writes, no installs, no network, no root, no destructive
# commands. Validates REPOSITORY CONTENT ONLY — a passing result is a
# static-validation success, NOT a platform build success, and does not
# imply any OS component exists or works.
#
# Honesty limitation (stated in output too): the secret scan and endpoint
# scans are pattern tripwires — they CANNOT guarantee the absence of all
# secret forms, vulnerabilities, or malicious content.
#
# Exit codes: 0 = all executed checks passed (skips reported, not hidden).
#             1 = one or more checks failed.
set -u -o pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
passed=0; failed=0; skipped=0
pass() { printf 'PASS  %s\n' "$1"; passed=$((passed+1)); }
fail() { printf 'FAIL  %s\n' "$1"; failed=$((failed+1)); }
skip() { printf 'SKIP  %s\n' "$1"; skipped=$((skipped+1)); }
section() { printf '\n== %s ==\n' "$1"; }

section "Project root"
printf 'ROOT  %s\n' "$ROOT"
if [ -d "$ROOT/.git" ]; then
  pass "local git repository present (no push performed by this script)"
else
  skip "not a git repository yet (informational)"
fi

section "Required files (exist, non-empty, non-placeholder)"
REQUIRED=(
  README.md LICENSE NOTICE CONTRIBUTING.md SECURITY.md .gitignore .editorconfig
  docs/phase-plan.md
  docs/architecture/system-overview.md
  docs/architecture/subsystem-boundaries.md
  docs/architecture/data-flow.md
  docs/security/threat-model.md
  docs/security/security-controls.md
  docs/security/update-integrity.md
  docs/security/key-management.md
  docs/security/vulnerability-management.md
  docs/compatibility/android-app-compatibility.md
  docs/compatibility/test-matrix.md
  docs/build/prerequisites.md
  docs/build/zero-cost-workflow.md
  docs/build/reproducible-builds.md
  docs/decisions/ADR-0001-aosp-baseline.md
  docs/decisions/ADR-0002-repository-strategy.md
  docs/decisions/ADR-0003-interface-customization.md
  docs/decisions/ADR-0004-no-ai-policy.md
  manifests/README.md
  manifests/local_manifests/README.md
  manifests/templates/README.md
  manifests/templates/drs-os-local.xml.template
  device/drs/README.md
  vendor/drs/README.md
  packages/apps/README.md
  overlays/README.md
  system/sepolicy/README.md
  build/README.md
  build/scripts/check_environment.sh
  build/scripts/validate_project.sh
  build/scripts/check_manifests.sh
  build/scripts/check_sepolicy.sh
  build/scripts/check_license_headers.sh
  tests/static/run_static_checks.sh
  tests/security/README.md
  tests/compatibility/README.md
  scripts/README.md
  branding/README.md
  branding/colors.md
  branding/typography.md
  branding/icon-guidelines.md
  tools/README.md
  tools/automation-reference/README.md
  tools/automation-reference/rule_engine.py
  docs/build/integration-readiness.md
  docs/build/upstream-sync.md
  docs/build/dependency-provenance.md
  docs/architecture/integration-map.md
  docs/ui/native-system-surfaces.md
  docs/automation/rule-engine-spec.md
  docs/security/implementation-checklist.md
  docs/security/test-plan.md
  tests/automation/README.md
  tests/automation/run_automation_tests.sh
  tests/automation/test_rule_engine.py
)
for rel in "${REQUIRED[@]}"; do
  f="$ROOT/$rel"
  if [ ! -f "$f" ]; then
    fail "missing file: $rel"
  elif [ ! -s "$f" ]; then
    fail "empty file: $rel"
  elif [ "$(wc -c < "$f")" -lt 64 ]; then
    fail "suspiciously small (possible placeholder): $rel"
  else
    pass "$rel"
  fi
done

section "Shell syntax (bash -n)"
sh_count=0
while IFS= read -r -d '' s; do
  sh_count=$((sh_count+1))
  if err="$(bash -n "$s" 2>&1)"; then
    pass "bash -n ${s#"$ROOT"/}"
  else
    fail "bash -n ${s#"$ROOT"/}: $err"
  fi
done < <(find "$ROOT/build" "$ROOT/tests" "$ROOT/scripts" -type f -name '*.sh' -print0 2>/dev/null)
if [ "$sh_count" -eq 0 ]; then skip "no shell scripts found (unexpected)"; fi

section "Secret scan (tokens, keys, credentials)"
# Self-scan safe: pattern literals below cannot match themselves.
sec="$(grep -rInE --exclude-dir=.git \
  -e 'ghp_[A-Za-z0-9]{36}' \
  -e 'gho_[A-Za-z0-9]{36}' \
  -e 'ghs_[A-Za-z0-9]{36}' \
  -e 'ghu_[A-Za-z0-9]{36}' \
  -e 'ghr_[A-Za-z0-9]{36}' \
  -e 'github_pat_[A-Za-z0-9_]{22,}' \
  -e 'AKIA[0-9A-Z]{16}' \
  -e 'ASIA[0-9A-Z]{16}' \
  -e 'xox[baprs]-[A-Za-z0-9-]{10,}' \
  -e '-----BEGIN [A-Z ]*PRIVATE KEY-----' \
  "$ROOT" 2>/dev/null || true)"
if [ -n "$sec" ]; then
  fail "secret-like material found (rotate the credential AND remove the file):"
  printf '%s\n' "$sec"
else
  pass "no secret-like material detected"
fi

section "AI / cloud-API policy scan (ADR-0004)"
ep="$(grep -rInE --exclude-dir=.git \
  -e 'https?://api\.(openai|anthropic|cohere|mistral)\.com' \
  -e 'https?://generativelanguage\.googleapis\.com' \
  -e 'https?://api\.huggingface\.co' \
  "$ROOT" 2>/dev/null || true)"
if [ -n "$ep" ]; then
  fail "generative-AI/cloud endpoint referenced:"
  printf '%s\n' "$ep"
else
  pass "no AI/cloud API endpoints referenced"
fi
dep_files="$(find "$ROOT" -type f \( -name 'Android.bp' -o -name '*.mk' -o -name 'build.gradle*' -o -name 'package.json' -o -name 'requirements.txt' -o -name 'pom.xml' -o -name 'Cargo.toml' \) -not -path '*/.git/*' 2>/dev/null || true)"
if [ -z "$dep_files" ]; then
  skip "no dependency manifests exist yet — AI-SDK dependency scan not applicable (reported, not passed silently)"
else
  ai_hits="$(printf '%s\n' "$dep_files" | while IFS= read -r d; do
    grep -inE 'openai|anthropic|gemini|generativelanguage|tensorflow|pytorch|transformers|langchain|cohere|huggingface' "$d" 2>/dev/null | sed "s|^|${d#"$ROOT"/}:|"
  done || true)"
  if [ -n "$ai_hits" ]; then
    fail "AI SDK identifier found in dependency manifests:"
    printf '%s\n' "$ai_hits"
  else
    dep_count="$(printf '%s\n' "$dep_files" | wc -l | tr -d ' ')"
    pass "no AI SDK identifiers in ${dep_count} dependency manifest(s)"
  fi
fi

section "Unsafe-command scan (shell scripts)"
# Tripwire for destructive patterns in any shell script. Scans scripts only
# (docs may legitimately discuss commands).
unsafe=""
while IFS= read -r -d '' s; do
  hit="$(grep -nE \
    -e 'rm[[:space:]]+-rf?[[:space:]]+/(|[[:space:]])' \
    -e 'dd[[:space:]].*of=/dev/' \
    -e 'mkfs\.[a-z0-9]+' \
    -e 'chmod[[:space:]]+-R[[:space:]]+777[[:space:]]+/' \
    -e '(curl|wget)[^|]*\|[[:space:]]*(ba|z)?sh' \
    -e '>+[[:space:]]*/dev/(sd|nvme|hd)' \
    -e '\:\(\)\{[[:space:]]*:\|:&' \
    "$s" 2>/dev/null | sed "s|^|${s#"$ROOT"/}:|" || true)"
  [ -n "$hit" ] && unsafe="$unsafe$hit
"
done < <(find "$ROOT" -type f -name '*.sh' -not -path '*/.git/*' -print0 2>/dev/null)
if [ -n "$unsafe" ]; then
  fail "unsafe/destructive command pattern in scripts:"
  printf '%s\n' "$unsafe"
else
  pass "no destructive command patterns in shell scripts"
fi

section "External endpoint allowlist"
# Every http(s) URL host in tracked text must be on the verified allowlist.
# git.invalid is the DOCUMENTED placeholder host (templates/docs only —
# production manifests reject it separately via check_manifests.sh).
ALLOWED_HOSTS='source.android.com
android.googlesource.com
www.apache.org
editorconfig.org
git.invalid'
unknown_hosts="$(grep -rhoE 'https?://[A-Za-z0-9.-]+' --exclude-dir=.git "$ROOT" 2>/dev/null \
  | sed -E 's#^https?://##; s#\.+$##' | sort -u \
  | while IFS= read -r h; do
      printf '%s\n' "$ALLOWED_HOSTS" | grep -qxF "$h" || echo "$h"
    done || true)"
if [ -n "$unknown_hosts" ]; then
  fail "unverified external host(s) referenced (add to allowlist with a recorded reason in docs/build/dependency-provenance.md, or remove):"
  printf '%s\n' "$unknown_hosts"
else
  pass "all referenced hosts are on the verified allowlist"
fi

section "Markdown internal links"
if command -v python3 >/dev/null 2>&1; then
  py_out="$(python3 - "$ROOT" <<'PY'
import os, re, sys
root = sys.argv[1]
checked = 0
broken = []
for dirpath, dirs, files in os.walk(root):
    dirs[:] = [d for d in dirs if d != '.git']
    for fn in files:
        if not fn.endswith('.md'):
            continue
        p = os.path.join(dirpath, fn)
        try:
            text = open(p, encoding='utf-8').read()
        except OSError:
            continue
        for m in re.finditer(r'\[[^\]]*\]\(([^)\s]+)\)', text):
            t = m.group(1)
            if t.startswith(('http://', 'https://', 'mailto:', '#')):
                continue
            path = t.split('#', 1)[0].replace('%20', ' ')
            if not path:
                continue
            checked += 1
            target = os.path.normpath(os.path.join(dirpath, path))
            if not os.path.exists(target):
                broken.append(os.path.relpath(p, root) + ' -> ' + t)
print('checked=' + str(checked))
if broken:
    print('BROKEN:')
    for b in broken:
        print('  ' + b)
    sys.exit(1)
PY
)"
  py_rc=$?
  if [ "$py_rc" -eq 0 ]; then
    pass "markdown internal links resolve ($(printf '%s' "$py_out" | tail -1))"
  else
    fail "markdown internal links broken:"
    printf '%s\n' "$py_out"
  fi
else
  skip "python3 unavailable — markdown link check not run (not a false pass)"
fi

section "Sub-checks"
for checker in check_manifests.sh check_sepolicy.sh check_license_headers.sh run_automation_tests.sh; do
  case "$checker" in
    run_automation_tests.sh) s="$ROOT/tests/automation/$checker";;
    *) s="$ROOT/build/scripts/$checker";;
  esac
  if [ ! -f "$s" ]; then
    fail "$checker missing"
    continue
  fi
  out="$(bash "$s" 2>&1)"; rc=$?
  printf '%s\n' "$out" | sed 's/^/    /'
  case $rc in
    0) pass "$checker";;
    2) skip "$checker (environmental skip reported above)";;
    *) fail "$checker exited $rc";;
  esac
done

section "Summary"
printf 'passed=%d failed=%d skipped=%d\n' "$passed" "$failed" "$skipped"
if [ "$failed" -gt 0 ]; then
  echo "RESULT: FAIL — fix failures above; skipped items remain explicitly listed."
  exit 1
fi
echo "RESULT: PASS — repository static validation successful."
echo "NOTE: static validation success ≠ platform build success; no AOSP code was compiled."
echo "NOTE: pattern scans (secrets, endpoints, unsafe commands) are tripwires —"
echo "      they do NOT guarantee absence of every secret or vulnerability."
