#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# DRS OS Phase 3 — static code & resource validation.
#
# Scope: Phase 3 source tree (components/, packages/apps/, platform/,
# tools/automation-reference additions). Read-only. Honest semantics:
# executed checks are PASS/FAIL, prerequisites that cannot be checked
# here are SKIP with the reason. Pattern scans are tripwires, not
# guarantees (same disclaimer as validate_project.sh).
set -u

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

pass=0
fail=0
skip=0

ok()   { printf 'PASS  %s\n' "$1"; pass=$((pass + 1)); }
bad()  { printf 'FAIL  %s\n' "$1"; fail=$((fail + 1)); }
skip() { printf 'SKIP  %s (%s)\n' "$1" "$2"; skip=$((skip + 1)); }

# ------------------------------------------------ 1. AI/ML dependency tripwire
ai_hits="$(grep -rniE 'tensorflow|mlkit|ml-kit|com\.google\.ml|androidx\.ml|openai|anthropic|huggingface|onnx|pytorch|\.tflite|\.onnx|chatgpt|gemini-api|generativeai|mlmodel' \
    --include='*.kts' --include='*.kt' --include='*.toml' --include='*.bp' --include='*.xml' \
    components packages platform 2>/dev/null | grep -v 'docs/' || true)"
if [ -z "$ai_hits" ]; then
    ok "no AI/ML dependency markers in build files or source (ADR-0004)"
else
    bad "AI/ML dependency markers found:"
    printf '%s\n' "$ai_hits" | head -10
fi

# ------------------------------------------------ 2. secret tripwire
secret_hits="$(grep -rniE 'ghp_[A-Za-z0-9]{20,}|gho_[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16}|xox[baprs]-|BEGIN (RSA |EC )?PRIVATE KEY|api[_-]?key *[:=] *"[A-Za-z0-9]{16,}"' \
    --include='*.kt' --include='*.kts' --include='*.toml' --include='*.bp' --include='*.xml' --include='*.sh' --include='*.py' \
    components packages platform build docs 2>/dev/null || true)"
if [ -z "$secret_hits" ]; then
    ok "no secret-pattern matches in source tree"
else
    bad "secret-pattern matches found:"
    printf '%s\n' "$secret_hits" | head -10
fi

# ------------------------------------------------ 3. XML well-formedness
if command -v python3 >/dev/null 2>&1; then
    xml_bad=0
    while IFS= read -r f; do
        if ! python3 -c "import xml.etree.ElementTree as ET,sys; ET.parse(sys.argv[1])" "$f" >/dev/null 2>&1; then
            bad "XML not well-formed: $f"
            xml_bad=1
        fi
    done < <(find components packages platform -name '*.xml' -type f 2>/dev/null)
    [ $xml_bad -eq 0 ] && ok "all app/platform XML files are well-formed"
else
    skip "XML well-formedness" "python3 unavailable"
fi

# ------------------------------------------------ 4. ar/en strings parity
if command -v python3 >/dev/null 2>&1; then
    parity_bad=0
    for app_dir in packages/apps/*/app/src/main/res; do
        [ -f "$app_dir/values/strings.xml" ] || continue
        [ -f "$app_dir/values-en/strings.xml" ] || continue
        result="$(python3 - "$app_dir" <<'PYEOF'
import sys
import xml.etree.ElementTree as ET

res = sys.argv[1]
def keys(path):
    tree = ET.parse(path)
    out = {}
    for el in tree.getroot():
        if el.tag == "string":
            out[el.get("name")] = (el.text or "").strip()
    return out

ar = keys(res + "/values/strings.xml")     # Arabic = default locale
en = keys(res + "/values-en/strings.xml")
problems = []
only_ar = sorted(set(ar) - set(en))
only_en = sorted(set(en) - set(ar))
if only_ar:
    problems.append("keys missing in values-en: %s" % only_ar)
if only_en:
    problems.append("keys missing in values (ar default): %s" % only_en)
for name, k, v in [("ar", n, t) for n, t in ar.items()] + [("en", n, t) for n, t in en.items()]:
    if not v:
        problems.append("%s: empty value for %s" % (name, k))
    if "TODO" in v or "xxx" in v.lower():
        problems.append("%s: placeholder text in %s" % (name, k))
print("; ".join(problems) if problems else "OK")
PYEOF
)"
        if [ "$result" = "OK" ]; then
            ok "ar/en string parity: $app_dir"
        else
            bad "ar/en string parity: $app_dir — $result"
            parity_bad=1
        fi
    done
else
    skip "ar/en strings parity" "python3 unavailable"
fi

# ------------------------------------------------ 5. manifest sanity
for manifest in packages/apps/*/app/src/main/AndroidManifest.xml; do
    app="$(printf '%s' "$manifest" | cut -d/ -f3)"
    support_rtl="$(grep -c 'android:supportsRtl="true"' "$manifest" || true)"
    locale_cfg="$(grep -c 'android:localeConfig="@xml/locales_config"' "$manifest" || true)"
    if [ "$support_rtl" -ge 1 ] && [ "$locale_cfg" -ge 1 ]; then
        ok "manifest RTL+localeConfig: $app"
    else
        bad "manifest RTL+localeConfig: $app (supportsRtl=$support_rtl localeConfig=$locale_cfg)"
    fi
    lc="packages/apps/$app/app/src/main/res/xml/locales_config.xml"
    if [ -f "$lc" ] && grep -q 'android:name="ar"' "$lc" && grep -q 'android:name="en"' "$lc"; then
        ok "locales_config ar+en: $app"
    else
        bad "locales_config ar+en: $app"
    fi
done

# ------------------------------------------------ 6. Kotlin source hygiene
kt_files="$(find components packages platform -name '*.kt' -o -name '*.kts' | sort)"
kt_total="$(printf '%s\n' "$kt_files" | grep -c . || true)"
kt_no_spdx=0
kt_bad_pkg=0
kt_markers=0
for f in $kt_files; do
    grep -q 'SPDX-License-Identifier: Apache-2.0' "$f" || { printf 'FAIL  missing SPDX: %s\n' "$f"; kt_no_spdx=$((kt_no_spdx + 1)); }
    case "$f" in
        */app/src/test/*|*/build.gradle.kts|*/settings.gradle.kts) ;; # package check only for main sources
        *)
            head -40 "$f" | grep -qE '^package (com\.drs\.os|drs)' || { printf 'FAIL  unexpected package: %s\n' "$f"; kt_bad_pkg=$((kt_bad_pkg + 1)); }
            ;;
    esac
    grep -qnE 'TODO|FIXME|STOPSHIP|placeholder implementation' "$f" && { printf 'FAIL  unfinished-marker in: %s\n' "$f"; kt_markers=$((kt_markers + 1)); }
done
if [ $kt_no_spdx -eq 0 ]; then ok "SPDX headers on $kt_total Kotlin files"; fi
if [ $kt_bad_pkg -eq 0 ]; then ok "Kotlin main-source packages under com.drs.os"; fi
if [ $kt_markers -eq 0 ]; then ok "no TODO/FIXME/STOPSHIP markers in Kotlin sources"; fi

# ------------------------------------------------ 7. URL allowlist
url_hits="$(grep -rhoE 'https?://[A-Za-z0-9./_-]+' \
    --include='*.kt' --include='*.kts' --include='*.bp' --include='*.xml' --include='*.toml' \
    components packages platform 2>/dev/null | sort -u)"
url_bad=0
while IFS= read -r url; do
    [ -z "$url" ] && continue
    case "$url" in
        http://schemas.android.com/*|https://schemas.android.com/*) ;;
        *) printf 'FAIL  non-allowlisted URL in source: %s\n' "$url"; url_bad=1 ;;
    esac
done <<EOF
$url_hits
EOF
[ $url_bad -eq 0 ] && ok "all source URLs are on the allowlist (android schema namespaces)"

# ------------------------------------------------ 8. version-catalog consistency
if command -v python3 >/dev/null 2>&1; then
    dep_bad=0
    for app in packages/apps/*/; do
        name="$(basename "$app")"
        toml="$app/gradle/libs.versions.toml"
        buildfile="$app/app/build.gradle.kts"
        [ -f "$toml" ] && [ -f "$buildfile" ] || continue
        missing="$(python3 - "$toml" "$buildfile" <<'PYEOF'
import re, sys
toml = open(sys.argv[1]).read()
build = open(sys.argv[2]).read()
defined = set(re.findall(r'^([\w-]+) *= \{', toml, re.M))
used = set(re.findall(r'libs\.((?:plugins\.)?[\w.]+)', build))
alias = lambda dotted: dotted.replace(".", "-").replace("plugins-", "")
problems = []
for u in used:
    a = alias(u)
    if a not in defined and not a.endswith("bom"):
        problems.append(u)
print(",".join(sorted(set(problems))))
PYEOF
)"
        if [ -z "$missing" ]; then
            ok "version-catalog consistency: $name"
        else
            bad "version-catalog consistency: $name — unresolved aliases: $missing"
            dep_bad=1
        fi
    done
else
    skip "version-catalog consistency" "python3 unavailable"
fi

printf -- '-----------------------------\n'
printf 'Summary: passed=%d failed=%d skipped=%d\n' "$pass" "$fail" "$skip"
if [ $fail -gt 0 ]; then
    printf 'RESULT: FAIL — Phase 3 static validation found real issues.\n'
    exit 1
fi
printf 'RESULT: PASS — Phase 3 static checks succeeded.\n'
printf 'NOTE: static checks are structural tripwires; they are NOT an\n'
printf '      Android build, NOT a security audit, and NOT runtime tests.\n'
exit 0
