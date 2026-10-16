# DRS OS Dependency & Source Provenance Policy

Status: policy (Phase 2). How DRS OS guarantees that every byte it adds —
and every byte it inherits — is traceable, licensed, and reviewable.

## 1. Upstream provenance

- The AOSP baseline is traceable to a **recorded manifest commit SHA**
  (ADR-0001, procedure: upstream-sync.md).
- Upstream license notices are never stripped; NOTICE records the
  foundation and trademark posture. Distributions re-ship upstream
  licenses verbatim.
- The authoritative source for any upstream component is the checked-out
  tree at the pinned SHA — not documentation, not memory. Where this
  repository describes upstream components, it marks symbol-level
  claims as design-level until re-verified against the tree.

## 2. DRS-added dependencies (future: code, fonts, icons, libraries)

Every addition is registered in `tools/README.md` **before** commit with:

| Field | Requirement |
|---|---|
| Name + version | exact; "latest" is not a version |
| Source | canonical upstream URL, archived copy reference |
| Integrity | hash of the exact artifact used (SHA-256) |
| License | SPDX id + license text shipped alongside |
| Reviewer + date | human attribution (no AI-generated assets — ADR-0004) |
| Reason | the consumer feature; dead dependencies are removed with their feature |

Verification is manual and explicit (fetch, hash, license read) — the
zero-budget phase adds no automated fetching, and no validator downloads
anything.

## 3. Tool provenance (this repository)

| Tool | Origin | Integrity posture |
|---|---|---|
| bash / git / python3 (host) | system-provided | versions measured and recorded per environment (check_environment.sh); no redistribution |
| Automation reference model | written in this repo | stdlib-only, deterministic, unit-tested (tests/automation/) |

No third-party code is vendored in this repository today; the empty
inventory in tools/README.md is the enforced state.

## 4. Integrity rules

1. Every tracked file is attributable to a reviewed commit (git history
   is the provenance record; history rewriting is prohibited).
2. Generated files carry generation instructions in-repo (e.g., contrast
   table in colors.md is recomputed by scripts/drs_contrast_check.py
   from the project's tool area) — no orphan artifacts.
3. Binary assets are prohibited until the inventory mechanism above is
   exercised with a real, hashed, licensed addition.
4. External URLs in documentation are constrained to the verified
   allowlist (validated per change by validate_project.sh); a new
   domain requires adding it to the allowlist **with a recorded reason**
   in the same change.
