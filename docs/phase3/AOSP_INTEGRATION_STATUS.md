# Phase 3 — AOSP Integration Status

Status: honest integration ledger (Phase 3, 2026-10-16). The purpose of
this file is to prevent the single most dangerous integration mistake:
mistaking a working standalone component for a working platform
integration. **Nothing in Phase 3 is integrated into AOSP.**

## 1. Verified integration inputs (from Phase 0/2, unchanged)

- Baseline branch: `android-latest-release` → revision
  `android17-release`, newest tag `android-17.0.0_r1` (verified
  2026-10-16 from the live manifest; re-verify at checkout per ADR-0001).
- No DRS-owned repositories exist; creating them requires
  authorization. The local-manifest template remains in
  `manifests/templates/` (non-executable, `git.invalid` placeholders
  documented); production scope stays empty and guarded
  (check_manifests.sh).
- Lunch target: never invented; to be read from the checked-out tree.

## 2. Component-by-component status

| Component | Standalone status | Integration point (design) | Integrated? |
|---|---|---|---|
| engine-core | compiled + tested (JVM) | source of DRSAutomation; future scheduler host | **NO** |
| security-core | compiled + tested (JVM) | source of DRSSecurityCenter | **NO** |
| settings-core | compiled + tested (JVM) | source of DRSSettings | **NO** |
| foundation | compiled + tested (JVM) | shared codec library | **NO** |
| DRSSecurityCenter app | SOURCE IMPLEMENTED — BUILD NOT VERIFIED | privileged app + SafetyCenter surfacing (surface #9) | **NO** |
| DRSAutomation app | SOURCE IMPLEMENTED — BUILD NOT VERIFIED (pure logic JVM-verified) | system app; WorkManager/JobScheduler decision pending real-tree review | **NO** |
| DRSSettings app | SOURCE IMPLEMENTED — BUILD NOT VERIFIED | overlay + entry-point injection into platform Settings (surface #8); this APK is not the replacement | **NO** |
| RRO overlays | not started (Phase 2 design only) | SystemUI/Launcher/Settings theming (surfaces #1–#8) | **NO** |
| SELinux policy | not started | system/sepolicy additions reviewed under C-1 | **NO** |

## 3. Open integration decisions (must be resolved against the real tree)

1. **API/SDK level**: standalone apps use compileSdk 35 as a documented
   local choice; the platform tree defines the authoritative
   `TARGET_PLATFORM`/`sdk_version` for Soong builds.
2. **Scheduler for TIME_DAILY triggers**: WorkManager (bundled,
   battery-conscious, no exact-time guarantees) vs AlarmManager exact
   alarms (needs SCHEDULE_EXACT_ALARM policy) vs platform JobScheduler.
   Battery/timing implications are documented in
   docs/automation/rule-engine-spec.md §4; the choice is gated on a real
   tree and a UX decision — nothing was assumed in code (the editor
   refuses to create TIME_DAILY rules until this exists).
3. **Cross-app policy store**: the automation master toggle and
   dry-run-default live in DRSSettings' own storage in v1; a shared
   store across DRS apps (ContentProvider or platform settings) is an
   explicit pending task — never simulated.
4. **Privileged Security Center stage**: SafetyCenter/permission-usage
   reads require the privapp stage; the whitelist policy is pinned
   (empty, justification-per-entry) in
   platform/integration/privapp-permissions/README.md.
5. **Signing**: the Soong templates mark `certificate: "platform"` as a
   REVIEW placeholder; the real decision follows
   docs/security/key-management.md (own key ceremony; debug keys never
   for production).

## 4. What Phase 3 deliberately did NOT do

- No AOSP checkout, no platform build, no Cuttlefish flash (infeasible
  here: 4 GiB RAM / 9.2 GiB disk / no KVM — measured).
- No repository creation, no remotes, no publication.
- No privileged-API implementation claims for the Security Center; the
  standalone app reads permissionless APIs only and labels the rest
  Unavailable.

## 5. Path to integration (Phase 4 candidates, in dependency order)

1. Android build host → run the three `assembleDebug` + unit-test
   commands in docs/phase3/BUILD_AND_TEST.md; promote apps to
   BUILD VERIFIED with recorded outputs.
2. DRS repository authorization → create repos, replace template
   placeholders per docs/build/integration-readiness.md gate, pin
   revisions, re-run check_manifests.sh.
3. Compliant AOSP host → gated checkout (ADR-0001), lunch-target
   confirmation, first Cuttlefish boot with `getenforce` check.
4. Overlay stage (zero-code theming) for surfaces #1–#8 per
   docs/ui/native-system-surfaces.md, then privileged Security Center
   stage with justified whitelist entries.
5. Scheduler decision (§3.2) → TIME_DAILY trigger enablement with
   WorkManager/JobScheduler and its documented timing semantics.
