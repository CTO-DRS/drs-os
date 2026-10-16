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

## Phase 2 — Integration readiness, native design & security foundation (COMPLETE)

Delivered (all changes to the existing scaffold; nothing discarded):

- **Placeholder hazard remediation:** the executable-shaped placeholder
  manifest was moved out of the production path to
  `manifests/templates/drs-os-local.xml.template` (non-executable by
  extension); `manifests/local_manifests/` is now empty-by-policy;
  `check_manifests.sh` hardened to REJECT placeholder remotes in
  production scope (negative control demonstrated: exit 1).
- **Integration design:** integration map with 4-way classification
  (upstream / DRS-existing / proposed / hardware-dependent),
  upstream-sync & pinning procedure, dependency-provenance policy,
  integration-readiness report (docs/build/integration-readiness.md).
- **Security foundation:** control ledger re-based on the 5-status
  taxonomy (upstream capability / DRS design requirement /
  implementation pending / test pending / verified implementation),
  implementation checklist with evidence-gated milestones, security
  test plan (20 tests, verified-vs-planned stated honestly).
- **Native UI specification:** 12 system surfaces with upstream
  components, integration points, lifecycle, RTL, a11y, performance,
  and pre-integration tests (docs/ui/native-system-surfaces.md).
- **Deterministic automation:** rule-engine specification + Python
  reference model + 25 unit tests — executed green after catching and
  fixing 3 real test defects; no AI anywhere (ADR-0004).
- **Tooling hardening:** unsafe-command scan, endpoint allowlist,
  automation sub-check, honest-tripwire disclaimers.
- **Audit findings recorded:** sandbox mode-bit drift (644→755 across
  all files, content identical) detected via git status and corrected;
  validation baseline reproduced before changes.

Blocked-on (honest): compliant build host; DRS repository
authorization. No AOSP download/build, no flashing, no paid services,
no external publication.

## Phase 3 — Native system implementation & executable foundation (COMPLETE, 2026-10-16)

Authorized directive: convert the Phase 2 foundation into real, native,
testable components. Delivered (details and evidence in docs/phase3/):

- **Audit-first:** Phase 2 state reproduced (git d7c182c clean after
  the known sandbox mode-bit drift was corrected; all seven Phase 2
  validation runs re-executed with identical results).
- **Pure-Kotlin cores (compiled + unit-tested in the sandbox, 188/188):**
  `components/foundation` (25), `components/engine-core` (104 — the
  Kotlin implementation of the Phase 2 rule-engine spec, including
  **34 differential tests against the Python reference model** via a
  committed, byte-deterministic vector file), `components/security-core`
  (18), `components/settings-core` (41). ADR-0005 records the
  module-strategy decision.
- **Three Android apps, source-complete with pinned Gradle config and
  Arabic-default RTL resources:** DRSSecurityCenter, DRSAutomation,
  DRSSettings. Build status: **SOURCE IMPLEMENTED — BUILD NOT VERIFIED**
  (no Android SDK in the sandbox — stated everywhere it matters). The
  automation app's pure logic was additionally JVM-verified (9/9).
- **Toolchain with provenance:** kotlinc 2.0.21 + JUnit console 1.10.2
  downloaded free with SHA-256 verification; reproducible runner
  `build/scripts/run_core_tests.sh` with honest BLOCKED semantics.
- **AOSP integration artifacts:** inactive Android.bp templates,
  integration-point matrix, intentionally empty privapp-permissions
  policy (platform/integration/).
- **Validation extended:** check_phase3_code.sh (19 checks: AI-dep
  tripwire, secrets, XML well-formedness, ar/en parity, RTL/locale
  manifests, SPDX/package/TODO scans, URL allowlist, catalog
  consistency) + run_core_tests.sh wired into validate_project.sh.
- **Security review** with 4 findings (1 medium) found-by-testing and
  fixed (docs/phase3/SECURITY_REVIEW.md); limitations ledger
  (docs/phase3/KNOWN_LIMITATIONS.md).
- No AOSP download/build, no flashing, no paid services, no external
  publication, no secrets, no AI (ADR-0004 enforced by new tripwires).

Blocked-on (honest): Android SDK host (app builds), compliant AOSP host
(platform integration), DRS repository authorization (remotes).

## Phase 4 — Checkout & first boot (PROPOSED — requires authorization)

Goal: execute the gated checkout on a compliant host. Prerequisites:
host checklist passed, DRS remotes authorized and verified, branch
re-verified, manifest SHA pinned. Proposed work items:

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

Explicitly out of scope until Phase 4 is authorized: full AOSP sync
(requires compliant host + authorization), any signing-key generation,
any public repository publication, any cloud account creation.

## Standing rule

No phase transition happens automatically. Each phase ends with an explicit
report and waits for authorization.
