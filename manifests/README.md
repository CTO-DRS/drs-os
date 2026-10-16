# DRS OS Manifest Strategy

AOSP checkouts are defined by repo manifests (XML). This directory holds
DRS OS's manifest documentation and the **template** local manifest that
will attach DRS-owned repositories into a future AOSP checkout.

## Official sources (verified Phase 0, 2026-10-16)

- Manifest repository: https://android.googlesource.com/platform/manifest
- Setup guide: https://source.android.com/docs/setup/start
- Cuttlefish: https://source.android.com/docs/devices/cuttlefish

## Rules

1. **Never modify upstream `default.xml`.** All DRS integration happens
   through local manifests (see local_manifests/).
2. **Never invent remotes, SHAs, or target strings.** Until DRS-owned Git
   repositories exist (requires authorization), the local manifest stays a
   template with `.invalid` placeholder hosts; `check_manifests.sh`
   reports it as template-mode, not as a validated production manifest.
3. **Pin everything.** When real remotes exist, every DRS project carries
   an explicit revision (ADR-0002); the upstream manifest commit SHA is
   recorded at init (ADR-0001).
4. **Validate before use.** `bash build/scripts/check_manifests.sh`
   (well-formedness, root element, path safety, remote/revision presence).
   Full semantic validation against a live manifest is only possible on a
   machine with the real manifest — reported as skipped, never faked.

## Files

- `local_manifests/README.md` — how local manifests work and how this one
  will be used.
- `local_manifests/drs-os-local.xml` — TEMPLATE (placeholder remotes).
