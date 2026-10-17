# Phase 4 — AOSP Integration Status

Phase 4 changed **nothing** in `platform/` — and that is the accurate
report. What changed is the credibility of the components that would
eventually integrate: they now compile against real tooling and carry
verified artifacts.

## 1. Status per component

| Component | Standalone build | Platform integration | Evidence |
|---|---|---|---|
| foundation / engine-core / security-core / settings-core | **BUILD VERIFIED** (composite Gradle builds + kotlinc path) | NOT STARTED | docs/phase4/BUILD_RESULTS.md |
| DRSSecurityCenter | **BUILD VERIFIED — RUNTIME NOT VERIFIED** | NOT STARTED (still designed as privileged app + SafetyCenter surfacing, surface #9) | this file §2 |
| DRSAutomation | **BUILD VERIFIED — RUNTIME NOT VERIFIED** | NOT STARTED (scheduler decision pending real-tree review) | this file §2 |
| DRSSettings | **BUILD VERIFIED — RUNTIME NOT VERIFIED** | NOT STARTED (entry-point injection into platform Settings, surface #8; this APK is not the replacement) | this file §2 |
| Android.bp templates / privapp-permissions / RROs | n/a | INACTIVE templates only, intentionally empty whitelist | platform/integration/ (unchanged, no remotes invented) |

## 2. What integration will require (unchanged from Phase 2/3 decisions,
re-verified against the now-buildable code)

1. **Source placement**: packages stay in the DRS repositories
   (ADR-0002); only `Android.bp` shims + RROs enter the AOSP tree via a
   local manifest whose remotes must be real and verified before any
   activation — the production manifest scope remains empty-by-policy
   and guarded by rejection checks. No DRS-owned remote exists yet
   (authorization still pending); no placeholder ever enters an active
   manifest.
2. **Privileges**: DRSSecurityCenter as a privileged system app requires
   `privapp-permissions` whitelist entries reviewed one by one; the
   whitelist directory is intentionally EMPTY until each permission has
   a recorded justification. No permission exists in any APK today
   (aapt2-verified 0/0/0).
3. **Signing**: platform (or shared-system) signing — the debug certs on
   today's artifacts are irrelevant to integration and must never be
   reused for it.
4. **SELinux**: untrusted_app domain by default; any privilege escalation
   path (privapp) gets its own review in the sepolicy workflow
   (check_sepolicy.sh structural checks exist; semantic checks need a
   real tree).
5. **Build metadata**: the pinned AGP/Compose toolchain is the
   standalone-app choice; the platform build will consume the same
   sources via Soong (`Android.bp`, already templated) at the tree's
   API level — confirmed at checkout, not assumed (ADR-0001 note in
   each app build file).

## 3. Phase 4 decisions that affect future integration

- **Composite-build topology is the bridge**: components are consumed
  as `com.drs.os:<module>` coordinates by app builds, which is also the
  shape Soong can replace (module → `java_library`/`android_library`
  shims) without moving sources.
- **No behavior depends on unavailable platform APIs** — the apps'
  permissionless-API strategy (Phase 3) survived compilation; the
  four-state model renders UNAVAILABLE instead of faking privileged
  data. This keeps the future privapp step additive rather than a
  rewrite.
- **TIME_DAILY trigger** remains non-creatable until a scheduler
  (WorkManager vs JobScheduler vs AlarmManager) is chosen against real
  platform behavior — decision explicitly deferred, never faked.

## 4. What would falsify "ready to integrate"

Any of these must be true before the word "integrated" is used:
a real AOSP checkout at the pinned manifest SHA, a verified DRS remote,
an activated (non-template) local manifest, platform-signed artifacts,
boot-to-home on Cuttlefish, and the T-16..T-20 platform test plan
executed. As of Phase 4: **zero of six are true** — the honest label
remains PENDING.
