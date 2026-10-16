# DRS OS Phase Plan

Status ledger and forward plan. This file is updated at the end of each
phase. It deliberately separates what is **done** from what is **proposed**;
proposed phases are not started without explicit project-owner
authorization.

## Phase 0 — Environment & feasibility audit (COMPLETE, 2026-10-16)

Report: `download/DRS_OS_Phase0_Environment_Audit.docx` (workspace export
area; 22 pages, evidence-based).

- Measured: Debian 13 container, 2 vCPU x86-64, 4.06 GiB RAM (4 GiB cgroup
  hard limit, no swap), 9.3 GiB free on 9.9 GiB overlay rootfs, no
  /dev/kvm, no sudo, git 2.47.3, Python 3.12.15, OpenJDK 21 JRE only,
  Repo launcher absent.
- Verified from official sources: AOSP requires ≥ 64 GB RAM, ≥ 400 GB disk,
  64-bit Linux; the android-latest-release manifest resolves to revision
  `android17-release`; newest published tag android-17.0.0_r1.
- Verdict: full AOSP checkout/build **infeasible in the sandbox**;
  feasible after preparation on a compliant user-provided host. Zero-cost
  cloud alternatives reviewed; none close the gap; trial credits excluded
  (payment instrument required).
- Workspace recovery note (Phase 1): the sandbox was reset between
  sessions; the `drs-os/` directory did not exist at Phase 1 start and was
  recreated from the Phase 1 specification and Phase 0 baseline. Phase 0
  measurements were re-verified at Phase 1 start (same host profile: 2
  vCPU, 4.1 GiB RAM, 9.2 GiB free disk, git 2.47.3, Python 3.12.15).

## Phase 1 — Scaffolding, architecture & validation tooling (COMPLETE)

This repository. Delivered: repository skeleton with substantive
documentation (architecture, security, compatibility, build), four ADRs,
local-manifest template, branding/design-system specifications with
verified WCAG contrast ratios, and six static validation scripts, all
executed with real exit statuses recorded in the Phase 1 report. No AOSP
download, no build, no flashing, no paid resources, no external
publication.

## Phase 2 — Integration readiness (PROPOSED — not started)

Goal: reach a state where an AOSP checkout can be executed on a compliant
host the moment one is available, with everything pre-validated. Requires
authorization. Proposed work items:

1. **Host preparation checklist run-through** (docs/build/prerequisites.md)
   on the target development host; record tool versions into the decision
   log; no downloads in this sandbox.
2. **Manifest finalization once remotes exist.** DRS-owned Git repositories
   do not exist yet; creating them needs authorization. When authorized:
   create repositories, replace the `.invalid` placeholders in
   `manifests/local_manifests/drs-os-local.xml`, re-run
   `build/scripts/check_manifests.sh`, and pin explicit revisions.
3. **Branch re-verification at integration time.** Confirm that
   `android-latest-release` still resolves to the expected revision
   (Phase 0 verified `android17-release` on 2026-10-16); record the exact
   manifest commit SHA that will be pinned (ADR-0001).
4. **Cuttlefish target confirmation.** Confirm the exact lunch target
   string from the checked-out tree (Phase 0 did not invent one; the AOSP
   documentation lists Cuttlefish targets, but the authoritative string
   must be read from the tree before pinning).
5. **Static tooling hardening.** Extend the secret scan and manifest
   validation based on lessons from the first real local-manifest; add
   device-layer template checks only when device files actually appear.
6. **Design-spec deepening.** Convert the Security & Privacy Center and
   Quick Settings specifications into concrete overlay resource plans
   (string/resource inventories) so implementation can start immediately
   after the first successful build.
7. **Security test scaffolding** under tests/security/ (checklist-based
   review gates for future SELinux and permission changes).

Explicitly out of scope for Phase 2: full AOSP sync (requires compliant
host + authorization), any signing-key generation, any public repository
publication, any cloud account creation.

## Standing rule

No phase transition happens automatically. Each phase ends with an explicit
report and waits for authorization.
