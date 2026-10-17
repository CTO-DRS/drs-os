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

## Phase 4 — Build verification, runtime-tooling ground truth & AOSP integration readiness (COMPLETE, 2026-10-17)

Authorized directive: "BUILD VERIFICATION, RUNTIME TESTING & AOSP
INTEGRATION" — verification first, no new prototypes. (The earlier
*proposal* named "Checkout & first boot" for Phase 4; the owner's
authorized scope replaced it. Checkout/first-boot work moved to the
next-phase proposal below, still blocked on a compliant host.)

Delivered (details and evidence in docs/phase4/):

- **Free toolchain installed and measured** (outside the repo, with
  provenance): Temurin JDK 17.0.20.1+1, Gradle 8.9 (official SHA-256
  verified), Android cmdline-tools 12.0, platform-35, build-tools
  35.0.0, platform-tools; licenses accepted.
- **Three real debug APKs built independently** (DRSSettings,
  DRSAutomation, DRSSecurityCenter — all exit 0), aapt2-badging and
  apksigner-verified; hashes recorded in BUILD_RESULTS.md.
- **10 real build-system defects found and fixed** (D-1..D-10,
  including a Kotlin block-comment nesting bug that silently swallowed
  build files, and a class referenced but never written).
- **147 unique tests, 0 failures** across two runners (kotlinc+JUnit
  console and Gradle), plus per-app Gradle unit tests; honest NOT
  RUN/BLOCKED labels for everything a device would be needed for
  (no KVM in the sandbox — instrumented tests and screenshots remain
  blocked with exact host commands recorded).
- **Security audit** with explicit non-claims; **AOSP integration
  status honestly NOT STARTED** (six falsification criteria listed);
  new scripts check_phase4_build.sh / verify_all.sh /
  inventory_artifacts.sh; 8 documents in docs/phase4/.

Blocked-on (honest): KVM-capable host (runtime verification), DRS
component remotes decision, compliant AOSP host (platform
integration). Commit identity was still the sandbox preset at this
point — replaced in Phase 5.

## Phase 5 — No-device CI layer, publication hygiene & scheduler decision (COMPLETE, 2026-10-17)

Authorized directive: "Complete everything" — execute every remaining
item that the environment supports, with honest labels for the rest.
Delivered (details and evidence in docs/phase5/PHASE5_REPORT.md):

- **Robolectric no-device CI layer** (final report item 2): pinned
  Robolectric 4.14.1 + androidx.test.ext:junit 1.2.1 + compose
  ui-test-junit4 (BOM) + junit-vintage-engine 5.10.2. **11 new tests,
  all green**: DRSSettings 8 (Arabic default with RTL geometry,
  English switch through the real AppCompatDelegate pipeline to
  EN/LTR, end-to-end corrupted-store recovery banner, automation
  master toggle persisted through REAL SharedPreferences, privacy
  "center not installed" gating, real-prefs round-trip/corruption
  semantics) and DRSAutomation 3 (SharedPreferencesStore commit()
  durability across instances, rule-codec round-trip through real
  storage, corrupted-document discard). Locale/Compose classes target
  SDK 29 deliberately — Robolectric's LocaleManager shadow on 33+
  stores but never applies per-app locales (documented limitation);
  the appcompat pre-T pipeline (minSdk 29) is fully observable.
- **Defects fixed:** verify_all.sh `printf` broke on dash-prefixed
  labels (all section headers were silently dropped); DRSSettings
  radio/switch rows made whole-row selectable/toggleable (a11y
  touch-target fix F-1; labels were dead zones before).
- **Audit correction:** Phase 4's "0 permissions in all APKs" claim
  was inaccurate — aapt2 shows the merger-injected
  `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (same package,
  protectionLevel signature) in all three debug APKs, including the
  two whose hashes are byte-identical to Phase 4. Corrected via
  pointer in docs/phase4/SECURITY_AUDIT.md; no app change needed.
- **ADR-0006:** TIME_DAILY stays engine-owned; host scheduling binds
  to WorkManager wake-ups (inexact, permission-free), exact alarms
  rejected as permission-gated; no code change.
- **Publication hygiene:** project Git remote live (private GitHub
  mirror), sandbox commit identity replaced with the owner's GitHub
  noreply identity and history re-verified on the remote.
- verify_all.sh: **17 PASS / 0 FAIL / 2 BLOCKED** (emulator/AOSP,
  hardware-blocked). Apps remain **BUILD VERIFIED — RUNTIME NOT
  VERIFIED** (Robolectric is a JVM simulation, not device runtime);
  project **NOT production-ready**.

Blocked-on (unchanged, honest): KVM host for emulator runtime +
screenshots + connected tests; compliant AOSP host for platform
integration (Soong shims, privapp-permissions, RRO); DRS component
repo split decision (needed only when platform integration starts;
the manifest template's 6 component repos have no consumer before a
compliant `repo sync` host exists).

## Next phase (PROPOSED — requires authorization): checkout & first boot

Carried from the original Phase 4 proposal; all items require a
compliant host and/or explicit authorization:

1. Host preparation checklist run-through (docs/build/prerequisites.md)
   on the target development host; record tool versions.
2. Manifest finalization once DRS component remotes are decided:
   replace `.invalid` placeholders, re-run check_manifests.sh, pin
   revisions.
3. Branch re-verification at integration time (`android-latest-release`
   → `android17-release` as of 2026-10-16) and pin the exact manifest
   commit SHA (ADR-0001).
4. Cuttlefish target confirmation from the checked-out tree before
   pinning any lunch target string.
5. Device-layer template checks once device files actually appear.

## Standing rule

No phase transition happens automatically. Each phase ends with an explicit
report and waits for authorization.
