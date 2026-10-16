# DRS OS Manifest Strategy

AOSP checkouts are defined by repo manifests (XML). This directory holds
DRS OS's manifest documentation, the **production** local-manifest area
(currently empty by policy), and **illustrative templates** (non-executable
by extension).

## Official sources (verified Phase 0, 2026-10-16)

- Manifest repository: https://android.googlesource.com/platform/manifest
- Setup guide: https://source.android.com/docs/setup/start
- Cuttlefish: https://source.android.com/docs/devices/cuttlefish

## Rules

1. **Never modify upstream `default.xml`.** All DRS integration happens
   through local manifests (see local_manifests/).
2. **Template/production separation (Phase 2).** Placeholder content lives
   only in `templates/*.xml.template` — non-executable by construction
   (repo only consumes `*.xml`). The production area
   `local_manifests/` accepts only verified manifests: any placeholder
   remote (`git.invalid`, `example.com`, `localhost`, `PLACEHOLDER`,
   …) is **rejected** there by `check_manifests.sh`.
3. **Never invent remotes, SHAs, or target strings.** No DRS-owned Git
   repositories exist yet (creation requires authorization). Until then
   no production manifest exists — that is the honest state, not a gap
   to paper over.
4. **Pin everything.** When real remotes exist, every DRS project carries
   an explicit pinned revision (ADR-0002); the upstream manifest commit
   SHA is recorded at init (ADR-0001). Procedure:
   docs/build/upstream-sync.md.
5. **Validate before use.** `bash build/scripts/check_manifests.sh`
   (well-formedness, root element, path safety, remote/revision
   presence, placeholder rejection in production scope). Structural
   validation does not prove remote authenticity — remote verification
   procedure is documented in docs/build/integration-readiness.md.
   Semantic validation against a live manifest happens only on an
   integration host and is reported as not-run elsewhere.

## Files

- `local_manifests/README.md` — production area (empty by policy) and the
  gated promotion procedure.
- `templates/README.md` + `templates/drs-os-local.xml.template` —
  illustrative layout, non-executable by extension.

