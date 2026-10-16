# Local Manifests — Usage and Status

The `repo` tool merges every XML in `.repo/local_manifests/` (on a host
where a checkout was initialized) with the upstream `default.xml`. DRS OS
uses this mechanism exclusively — upstream `default.xml` is never edited
(ADR-0002).

## Intended future usage (on a capable, authorized host only)

```bash
# after repo init per ADR-0001, and only once real DRS remotes exist:
mkdir -p .repo/local_manifests
cp <drs-os-checkout>/manifests/local_manifests/drs-os-local.xml \
   .repo/local_manifests/
# then substitute placeholder remote/revision values with audited, pinned
# values; re-run build/scripts/check_manifests.sh before any sync
```

## Current status

- `drs-os-local.xml` is a **TEMPLATE**: remote host is `git.invalid`
  (DNS-invalid by construction — a real sync with it cannot succeed),
  project names and revisions are placeholders.
- No DRS-owned Git repositories exist; creating them or publishing
  anything externally requires separate, explicit authorization.
- Validation today is structural only (XML well-formedness, element
  checks, path safety, placeholder detection) via
  `build/scripts/check_manifests.sh`. Semantic validation against a real
  upstream manifest is not possible in this environment and is reported
  as such — never as a pass.
