# ADR-0002: Repository Strategy — Separate DRS Repos + Local Manifest

- Status: Accepted (Phase 1)
- Date: 2026-10-16
- Deciders: DRS OS platform engineering
- Supersedes: none

## Context

DRS OS custom code must live somewhere relative to the AOSP tree. Options:
(1) fork the entire AOSP tree and commit inside it, (2) keep DRS-owned
components in separate repositories and attach them with a repo
local manifest, or (3) one monolithic DRS repository containing
everything. Constraint: no DRS-owned Git repositories exist yet, and none
may be created without explicit authorization. No remote URLs may be
invented.

## Decision

**Option 2.** DRS-owned components live in separate repositories, mounted
into the AOSP checkout via `.repo/local_manifests/`. This repository holds
the *template*: `manifests/local_manifests/drs-os-local.xml`, using
`git.invalid` placeholder host and clearly-marked placeholder revisions.
The template is **not usable** for sync until real remotes exist and are
substituted; `build/scripts/check_manifests.sh` flags the placeholder and
reports template-mode explicitly rather than passing silently.

Intended repository split (paths are AOSP-tree mount points):

| Repo (name TBD at creation) | Mounts at |
|---|---|
| DRS device layer | `device/drs` |
| DRS vendor config | `vendor/drs` |
| Security & Privacy Center | `packages/apps/DRSSecurityCenter` |
| Automation engine + editor | `packages/apps/DRSAutomation` |
| DRS Files | `packages/apps/DRSFiles` |
| Overlays/theme | `overlays` |

## Consequences

- Upstream stays pristine → merges are cheap; upstream provenance is
  auditable per-component.
- Each DRS repo pins explicit revisions; a build is reproducible from
  manifest SHA + pinned DRS revisions.
- Slight operational overhead: one extra manifest artifact to maintain.
- Placeholder template cannot accidentally be used for a real sync
  (`.invalid` host fails DNS by construction).

## Alternatives considered

- **Full-tree fork:** simplest mental model, worst merge/security posture;
  violates minimal-fork policy. Rejected.
- **Monolithic DRS repo containing device+apps+docs:** acceptable fallback
  if repo hosting turns out limited, but couples unrelated components and
  complicates per-component licensing/provenance. Deferred as fallback
  only.

## References

- manifests/README.md (manifest mechanics)
- manifests/local_manifests/drs-os-local.xml (template)
- ADR-0001 (baseline pinning that composes with this strategy)
