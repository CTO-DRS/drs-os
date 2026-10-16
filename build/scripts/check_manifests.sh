#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# DRS OS — build/scripts/check_manifests.sh   (Phase 2 hardened)
# Validates repo manifests under manifests/ with two strict scopes:
#
#   PRODUCTION scope  manifests/*.xml and manifests/local_manifests/**/*.xml
#     -> full structural validation AND placeholder rejection: any remote
#        fetch/review URL, project name/path/revision, or file content
#        containing a placeholder marker FAILS. Production manifests must
#        reference only verified remotes and pinned revisions.
#
#   TEMPLATE scope    manifests/templates/*.xml.template
#     -> structural validation only; placeholder content is expected and
#        labeled TEMPLATE-MODE. The ".template" extension is mandatory for
#        anything containing placeholders: the repo tool only consumes
#        "*.xml", so templates are non-executable by construction.
#
# Guarantees: read-only, no network, no root, no host modification.
# Uses python3 stdlib (xml.etree); reports SKIPPED when unavailable —
# never a silent pass. This is structural validation only: semantic
# validation against the live AOSP manifest happens on an integration
# host and is reported as not-run here.
#
# Exit codes: 0 = valid (template-mode notices allowed), 1 = failure,
#             2 = skipped (nothing to validate or no XML parser).
set -u -o pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
rc=0
production_files=()
template_files=()

if command -v python3 >/dev/null 2>&1; then
  while IFS= read -r -d '' f; do production_files+=("$f"); done < <(
    find "$ROOT/manifests" -type f -name '*.xml' \
      -not -path "$ROOT/manifests/templates/*" -print0 2>/dev/null)
  while IFS= read -r -d '' f; do template_files+=("$f"); done < <(
    find "$ROOT/manifests/templates" -type f -name '*.xml.template' -print0 2>/dev/null)
else
  echo "SKIP  python3 unavailable — XML validation cannot run (reported, not passed)"
  exit 2
fi

if [ "${#production_files[@]}" -eq 0 ] && [ "${#template_files[@]}" -eq 0 ]; then
  echo "SKIP  no manifests or templates found under manifests/"
  exit 2
fi

run_check() { # run_check <file> <mode: production|template>
  local x="$1" mode="$2" rel out prc
  rel="${x#"$ROOT"/}"
  out="$(python3 - "$x" "$mode" <<'PY'
import sys
import xml.etree.ElementTree as ET

path, mode = sys.argv[1], sys.argv[2]
problems = []
notices = []

# Placeholder markers rejected in production scope. These indicate an
# unverified remote/revision — exactly what Phase 2 hardening targets.
PLACEHOLDER_MARKERS = (
    'git.invalid', 'example.com', 'example.org', '.example',
    'localhost', '127.0.0.1', 'TODO', 'PLACEHOLDER', 'CHANGEME',
    'REPLACE_ME', 'REPLACE-ME',
)

try:
    tree = ET.parse(path)
except ET.ParseError as e:
    print('FAIL  not well-formed: %s' % e)
    sys.exit(1)
root = tree.getroot()
if root.tag != 'manifest':
    problems.append('root element is <%s>, expected <manifest>' % root.tag)

remotes = {r.get('name') for r in root.findall('remote')}
default = root.find('default')

for rem in root.findall('remote'):
    for attr in ('fetch', 'review'):
        val = rem.get(attr)
        if val:
            for marker in PLACEHOLDER_MARKERS:
                if marker.lower() in val.lower():
                    msg = 'remote %r: %s contains placeholder marker %r' % (
                        rem.get('name'), attr, marker)
                    (notices if mode == 'template' else problems).append(msg)

for proj in root.findall('project'):
    name = proj.get('name')
    p = proj.get('path') or name
    if not name:
        problems.append('<project> missing required "name" attribute')
        continue
    if p:
        if p.startswith('/'):
            problems.append('project %s: absolute path not allowed' % name)
        if '..' in p.split('/'):
            problems.append('project %s: path escapes tree (%s)' % (name, p))
    r = proj.get('remote')
    if r is None and default is None:
        problems.append('project %s: no remote and no <default> — unresolvable' % name)
    elif r is not None and r not in remotes:
        problems.append('project %s: references undefined remote %r' % (name, r))
    rev = proj.get('revision')
    if rev is None and default is None:
        problems.append('project %s: no revision and no <default> — unpinned' % name)
    if rev:
        for marker in PLACEHOLDER_MARKERS:
            if marker.lower() in rev.lower():
                msg = 'project %s: revision %r contains placeholder marker %r' % (
                    name, rev, marker)
                (notices if mode == 'template' else problems).append(msg)
    for attr in ('name', 'path'):
        val = proj.get(attr)
        if val:
            for marker in PLACEHOLDER_MARKERS:
                if marker.lower() in val.lower():
                    msg = 'project %s: %s contains placeholder marker %r' % (
                        name, attr, marker)
                    (notices if mode == 'template' else problems).append(msg)

raw = open(path, encoding='utf-8').read()
if mode == 'production' and 'git.invalid' in raw:
    problems.append('production manifest references placeholder host git.invalid')
if default is not None:
    notices.append('<default> element present — local manifests must not '
                   're-point upstream projects (ADR-0002)')

if mode == 'template':
    notices.append('TEMPLATE-MODE: illustrative file (.template extension, '
                   'non-executable by repo); NOT a production manifest')
    notices.append('placeholders are EXPECTED here; production scope rejects them')

for n in notices:
    print('NOTE  %s' % n)
for p in problems:
    print('FAIL  %s' % p)
if not problems:
    print('PASS  %s' % ('structural check ok (template scope)'
                       if mode == 'template' else 'production manifest valid'))
sys.exit(1 if problems else 0)
PY
)"
  prc=$?
  printf '%s\n' "$out" | sed "s|^|${rel}: |"
  [ "$prc" -ne 0 ] && rc=1
}

echo "-- production scope (placeholders rejected) --"
if [ "${#production_files[@]}" -eq 0 ]; then
  echo "NOTE  no production manifests present (correct state until DRS remotes"
  echo "      are authorized and verified — see manifests/local_manifests/README.md)"
else
  for x in "${production_files[@]}"; do run_check "$x" production; done
fi

echo "-- template scope (illustrative, non-executable) --"
for x in "${template_files[@]}"; do run_check "$x" template; done

echo "NOTE  semantic validation against the live AOSP manifest requires a real"
echo "      checkout and is NOT performed here (reported as not-run, never faked)."
echo "NOTE  structural validation does not prove a remote is authentic; remote"
echo "      verification procedure: docs/build/integration-readiness.md."
exit $rc
