#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# DRS OS — Phase 4 build-configuration checks (read-only).
#
# Verifies the invariants introduced by the Phase 4 build-system repair:
#   1. Component Gradle build files exist and pin the SAME Kotlin and
#      JUnit versions as the app version catalogs (lockstep rule).
#   2. Kotlin source files contain no "wildcard-in-block-comment"
#      poison: the sequence `identifier/*` or `*/identifier` inside a
#      /* */ comment silently swallows the rest of the file (Kotlin
#      block comments nest; caught the hard way in Phase 4 — see
#      docs/phase4/BUILD_RESULTS.md defect D-6).
#   3. App settings.gradle.kts files wire the shared components as
#      composite included builds with explicit dependency substitution.
#
# Honest semantics: checks that cannot run are SKIP, never PASS.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$HERE/../.." && pwd)"
cd "$REPO_ROOT"

pass=0; fail=0; skip=0
note() { printf '%s\n' "$*"; }
ok()   { pass=$((pass+1)); printf 'PASS  %s\n' "$*"; }
bad()  { fail=$((fail+1)); printf 'FAIL  %s\n' "$*"; }
skip() { skip=$((skip+1)); printf 'SKIP  %s\n' "$*"; }

note "== Phase 4 build-configuration checks =="
note ""

# ---------------------------------------------------- 1. lockstep versions
COMPONENTS="foundation engine-core security-core settings-core"
APPS="DRSSettings DRSSecurityCenter DRSAutomation"

cat_versions() { # extract a version value from a toml catalog
    sed -n 's/^\s*'"$2"' = "\(.*\)"$/\1/p' "$1" | head -1
}

KOTLIN_PIN=""
for app in $APPS; do
    cat="packages/apps/$app/gradle/libs.versions.toml"
    if [ ! -f "$cat" ]; then bad "catalog exists: $app"; continue; fi
    v="$(cat_versions "$cat" 'kotlin')"
    if [ -z "$v" ]; then bad "catalog kotlin pin: $app"; else ok "catalog kotlin pin: $app = $v"; fi
    if [ -z "$KOTLIN_PIN" ]; then KOTLIN_PIN="$v"; fi
    if [ "$v" != "$KOTLIN_PIN" ]; then bad "kotlin pins agree across apps ($app=$v vs $KOTLIN_PIN)"; fi
done

for m in $COMPONENTS; do
    f="components/$m/build.gradle.kts"
    if [ ! -f "$f" ]; then bad "component build file: $m"; continue; fi
    ok "component build file: $m"
    ver="$(sed -n 's/^\s*id("org.jetbrains.kotlin.jvm") version "\(.*\)"$/\1/p' "$f" | head -1)"
    if [ -z "$ver" ]; then
        bad "component kotlin pin: $m (expected literal id+version)"
    elif [ "$ver" = "$KOTLIN_PIN" ]; then
        ok "component kotlin pin: $m = $ver (lockstep with catalogs)"
    else
        bad "component kotlin pin: $m = $ver != catalog $KOTLIN_PIN"
    fi
    j1="$(sed -n 's/.*junit-jupiter:\([0-9.]*\)".*/\1/p' "$f" | head -1)"
    j2="$(sed -n 's/.*junit-platform-launcher:\([0-9.]*\)".*/\1/p' "$f" | head -1)"
    ref1="$(cat_versions "packages/apps/DRSSettings/gradle/libs.versions.toml" 'junitJupiter')"
    ref2="$(cat_versions "packages/apps/DRSSettings/gradle/libs.versions.toml" 'junitLauncher')"
    if [ "$j1" = "$ref1" ] && [ "$j2" = "$ref2" ]; then
        ok "component junit pins: $m (jupiter=$j1 launcher=$j2)"
    else
        bad "component junit pins: $m (got $j1/$j2 want $ref1/$ref2)"
    fi
done

# --------------------------------------- 2. comment-wildcard poison guard
# The poison only exists in /* */ comments (Kotlin block comments NEST;
# a path wildcard `apps/*/` inside one opens a nested comment and the
# rest of the file is silently swallowed). Wildcards inside // line
# comments or string literals are harmless. Scan: strip line comments
# and string literals, keep block comments + code, flag identifier-/
# * and * /identifier sequences.
poison_hits="$(python3 - << 'PYEOF'
import glob, re, sys
hits = []
pat_open = re.compile(r'[A-Za-z0-9_]/\*')
pat_close = re.compile(r'\*/[A-Za-z0-9_]')
line_comment = re.compile(r'//[^\n]*')
string_lit = re.compile(r'"(?:\\.|[^"\\])*"')
files = []
for root in ("components", "packages"):
    files += glob.glob(f"{root}/**/*.kt", recursive=True)
    files += glob.glob(f"{root}/**/*.kts", recursive=True)
for f in sorted(set(files)):
    try:
        text = open(f, encoding="utf-8").read()
    except OSError:
        continue
    stripped = string_lit.sub('""', line_comment.sub('', text))
    if pat_open.search(stripped) or pat_close.search(stripped):
        hits.append(f)
print("\n".join(hits))
PYEOF
)"
if [ -z "$poison_hits" ]; then
    ok "no wildcard-in-block-comment poison in .kt/.kts sources"
else
    bad "wildcard-in-block-comment poison found in:"
    printf '%s\n' "$poison_hits" | sed 's/^/      /'
fi

# ------------------------------- 3. composite included-build wiring check
for app in $APPS; do
    f="packages/apps/$app/settings.gradle.kts"
    n_inc="$(grep -c 'includeBuild(' "$f" 2>/dev/null || echo 0)"
    n_sub="$(grep -c 'dependencySubstitution' "$f" 2>/dev/null || echo 0)"
    if [ "$n_inc" = "2" ] && [ "$n_sub" = "2" ]; then
        ok "composite wiring: $app (2 included builds, 2 substitutions)"
    else
        bad "composite wiring: $app (includeBuild=$n_inc dependencySubstitution=$n_sub, want 2/2)"
    fi
    if grep -q 'projectDir = file' "$f"; then
        bad "stale projectDir mapping: $app (must use includeBuild)"
    else
        ok "no stale projectDir mapping: $app"
    fi
done

# ------------------------------------------------------------- summary
note ""
note "== Summary =="
note "passed=$pass failed=$fail skipped=$skip"
if [ "$fail" -eq 0 ]; then
    note "RESULT: PASS — Phase 4 build-configuration invariants hold."
else
    note "RESULT: FAIL — see FAIL lines above."
fi
note "NOTE: these are structural checks; they are NOT a build and NOT a test run."
exit "$fail"
