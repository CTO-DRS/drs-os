# DRS OS Security Implementation Checklist

Status: **planning artifact (Phase 2)**. Ordered gates that must be
satisfied — with evidence — before the corresponding milestone can be
declared done. A written security design (the documents in this
directory) is **not** equivalent to a security audit; this checklist is
the bridge from design to verifiable practice.

## Gate 0 — Repository foundation (current phase)

- [x] No secrets in repository (scan executed, exit 0 — tripwire, not proof)
- [x] No placeholder remotes in production manifest scope (validator
      rejects; negative control demonstrated)
- [x] No AI dependencies (ADR-0004; endpoint + dependency scans pass)
- [x] Sepolicy scaffold rule-free (guard active)
- [x] License/attribution present (LICENSE, NOTICE, SPDX headers)
- [ ] Public security contact published (deferred until authorized
      public presence)

## Gate 1 — First compliant host & checkout

- [ ] Host meets prerequisites (docs/build/prerequisites.md §3, evidence
      recorded per item)
- [ ] `repo init` with re-verified branch; **manifest commit SHA recorded
      in decision log before sync** (ADR-0001)
- [ ] DRS repositories created under authorization; local manifest
      promotes from template with verified remotes + pinned revisions;
      production-mode validator passes
- [ ] Sync completes; no local patches to upstream applied yet

## Gate 2 — First boot (Cuttlefish, userdebug)

- [ ] Clean build of confirmed lunch target from the real tree
- [ ] Boots to UI; boot smoke checklist recorded
- [ ] `getenforce` = Enforcing at boot (evidence: console capture)
- [ ] No DRS overlay/policy applied yet — pure upstream baseline boot
- [ ] Baseline boot artifacts hashed and recorded (reproducibility input)

## Gate 3 — First DRS customization (overlays only)

- [ ] DRS overlays build and apply (dark/light, ar/en) with zero
      framework patches
- [ ] `overlayable` policy review: security-critical AOSP resources
      protected; overlay APK signed with DRS key (never test keys for
      any reviewed artifact)
- [ ] RTL verification pass on all themed surfaces
- [ ] SELinux policy diff vs upstream = empty (overlays require none) —
      evidence: build-time policy artifact comparison

## Gate 4 — First privileged DRS app (Security & Privacy Center)

- [ ] `privapp-permissions` whitelist with per-permission written
      justification; diff review signed off
- [ ] Application signed with DRS key; never platform-debug-signed in
      reviewed builds
- [ ] No INTERNET permission; data-flow map updated
- [ ] SELinux: operates within `priv_app` domain (no new domain unless
      justified per system/sepolicy/README.md process)
- [ ] Permission audit: every requested permission maps to a documented
      user-facing feature

## Gate 5 — First signed OTA

- [ ] Release keys generated offline per key-management.md (dual control,
      generation record in decision log)
- [ ] Production build is `user` variant, release-signed (test keys
      absent — verified)
- [ ] Signed OTA installs on Cuttlefish; tampered payload rejected
      (negative test evidence)
- [ ] Rollback protection behavior demonstrated (downgrade rejected)
- [ ] AVB: locked-bootloader verification status recorded
- [ ] update_engine failure path: fallback to previous slot verified

## Standing gate rules

- Every checkbox above requires **evidence** (command, output, artifact
  hash, or signed review note) — a checked box without evidence is a
  defect.
- Security-relevant deviations require an ADR before implementation,
  not after.
- Any failure at a gate stops the line; no "temporary" weakenings.
