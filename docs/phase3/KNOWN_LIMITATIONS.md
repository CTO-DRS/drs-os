# Phase 3 — Known Limitations

Status: honest limitation ledger (Phase 3, 2026-10-16). Anything here
must not be claimed elsewhere; conversely, anything NOT here that a
reader might assume is covered by the general rule: **unverified means
unverified**.

## Environment-imposed limitations

1. **Android/Compose UI is not compiled.** No Android SDK in the
   sandbox (measured in PHASE3_BASELINE.md). The three apps are source
   complete and Gradle-configured: `SOURCE IMPLEMENTED — BUILD NOT
   VERIFIED`. Exactly what remains unproven: Compose compilation,
   resource linking/merging, manifest merge, APK packaging, and every
   app-layer unit test that needs the Android Gradle toolchain (the two
   automation-app tests and the Security Center presentation test
   compile/run only on a real build).
2. **No runtime/behavioral verification of any Android component.** No
   emulator (no KVM), no device. Nothing has ever been launched.
3. **No AOSP integration verification.** Android.bp templates are
   design artifacts; Soong compatibility is unverified (stated on every
   template).

## Implementation-scoped limitations

4. **TIME_DAILY trigger is not creatable in the app.** Edge triggers
   need a platform scheduler (WorkManager/AlarmManager/JobScheduler
   decision pending — AOSP_INTEGRATION_STATUS.md §3.2). The UI says so
   instead of showing a dead control. Consequence: the app is honest
   but time-automated rules do not exist yet.
5. **Battery-saver actions always fail with a stable classification.**
   Android exposes no public API for third parties to toggle battery
   saver; the executor records `platform-api-unavailable` instead of
   pretending. Real enablement requires platform integration.
6. **DND actions require notification-policy special access.** Without
   it the executor returns `dnd-policy-access-denied`; the app never
   requests it silently.
7. **Automation persistence is SharedPreferences + KvStore snapshot.**
   Durable but not crash-atomic in the transactional sense; a torn
   write yields the corruption-recovery path (empty state, flagged),
   trading availability for integrity by design.
8. **Cross-app policy store does not exist.** DRSSettings' automation
   toggles affect only DRSSettings' own stored policy (v1 scope is
   stated in the UI). DRS Automation does not read it yet.
9. **Settings language switch applies to DRS apps via per-app locales
   only on API 33+; on API 29–32 AppCompatDelegate falls back to the
   app-level locale override, which may not affect other apps.** The
   per-app locale platform behavior must be verified on real builds.
10. **Fonts are not bundled.** Typography renders with the platform
    font chain until the OFL inclusion checklist (typography.md) is
    executed with rendered specimens.
11. **The verification status of `info/light` color remains open** —
    deliberately unfinalized in branding/colors.md; no Phase 3 screen
    consumes it, and none invented a value.

## Process limitations

12. **No user study, no accessibility audit with TalkBack, no RTL
    screenshot verification** — all require a running build. The RTL
    foundations (start/end usage, ar-default resources, parity check)
    are in place and statically validated, and are explicitly marked
    UNVERIFIED at runtime level per the DoD ("implemented or explicitly
    marked unverified" — this is the marking).
13. **Git identity is the sandbox preset** (`Z User <z@container>`).
    Must be replaced before any publication (unchanged since Phase 1).
14. **No external publication of any kind** (per project rules); the
    Phase 3 archive is a local file only.
