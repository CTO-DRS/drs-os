#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
#
# DRS OS — build/scripts/check_manifests.sh
# Validates XML manifests under manifests/: well-formedness, root element,
# project path safety, remote/revision presence, placeholder detection.
#
# Guarantees: read-only, no network, no root, no host modification.
# Uses python3 stdlib (xml.etree) when available; reports a SKIPPED exit
# otherwise — never a silent pass.
#
# Exit codes: 0 = valid (template-mode warnings allowed)
#             1 = validation failure
#             2 = skipped (nothing to validate or no XML parser)
set -u -o pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
shopt -s nullglob
xmls=("$ROOT"/manifests/*.xml "$ROOT"/manifests/local_manifests/*.xml)
shopt -u nullglob

if [ "${#xmls[@]}" -eq 0 ]; then
  echo "SKIP  no XML manifests found under manifests/"
  exit 2
fi
if ! command -v python3 >/dev/null 2>&1; then
  echo "SKIP  python3 unavailable — XML validation cannot run (reported, not passed)"
  exit 2
fi

rc=0
for x in "${xmls[@]}"; do
  rel="${x#"$ROOT"/}"
  out="$(python3 - "$x" <<'PY'
import sys
import xml.etree.ElementTree as ET

path = sys.argv[1]
problems = []
warnings = []
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
        problems.append('project %s: no remote and no <default> — would be unresolvable' % name)
    elif r is not None and r not in remotes:
        problems.append('project %s: references undefined remote %r' % (name, r))
    if proj.get('revision') is None and default is None:
        problems.append('project %s: no revision and no <default> — unpinned' % name)

raw = open(path, encoding='utf-8').read()
if 'git.invalid' in raw:
    warnings.append('TEMPLATE MODE: placeholder host git.invalid present — '
                    'structural validation only, NOT a production manifest')
if default is not None:
    warnings.append('<default> element present — local manifests should avoid '
                    're-pointing upstream projects (ADR-0002)')

for w in warnings:
    print('WARN  %s' % w)
for p in problems:
    print('FAIL  %s' % p)
if not problems and not warnings:
    print('PASS  structure valid')
elif not problems:
    print('PASS  well-formed with warnings above')
sys.exit(1 if problems else 0)
PY
)"
  prc=$?
  printf '%s\n' "$out" | sed "s|^|${rel}: |"
  if [ "$prc" -ne 0 ]; then rc=1; fi
done

echo "NOTE  semantic validation against the live AOSP manifest requires a real"
echo "      checkout and is NOT performed here (reported as not-run, never faked)."
exit $rc
