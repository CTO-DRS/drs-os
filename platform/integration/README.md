# AOSP Integration Templates (Phase 3)

Status: **design-time integration artifacts — NOT part of any active
build.** These `Android.bp` files describe how each Phase 3 component
would be integrated into an AOSP tree once the gated checkout exists
(ADR-0001). They are deliberately inert here:

* this repository is not an AOSP tree; Soong cannot consume them;
* the target platform release/API level is confirmed only at checkout
  (Phase 0 verified `android17-release`; the exact lunch target and API
  level must be read from the tree before pinning);
* no DRS-owned Git remotes exist yet (ADR-0002) — no invented repos,
  no invented revision SHAs.

## Component → integration-point mapping

| Component | Source (this repo) | Integration point | Stage gate |
|---|---|---|---|
| DRS Security Center | `packages/apps/DRSSecurityCenter` | privileged app (privapp-permissions, currently EMPTY list) + SafetyCenter surfacing where APIs confirm | needs real tree |
| DRS Automation | `packages/apps/DRSAutomation` | ordinary system app first (no privileged flags); WorkManager scheduling needs background-execution review | needs real tree |
| DRS Settings | `packages/apps/DRSSettings` | demonstration of settings policy UI; platform Settings integration is an overlay + injection task (surface spec #8), NOT this APK | needs real tree |
| engine-core / security-core / settings-core / foundation | `components/*` | pure Kotlin libraries, consumed as source by the app modules above; AOSP integration builds them with Soong `android_library` or keeps the Gradle host | needs real tree |

## Build-system reality (honest)

The authoritative build for these apps today is **Gradle** (AGP 8.7.3,
Kotlin 2.0.21). The `Android.bp` files here are a faithful translation
for a future Soong build and exist to force the integration questions
early (privapp whitelists, SELinux context, static deps). They are not
verified against a real Soong and must not be represented as buildable.

## Rules carried into integration

1. `android:allowBackup="false"` for all DRS apps.
2. Zero permissions unless individually justified (privapp-permissions
   template starts empty on purpose).
3. Arabic-default resources (values/ = ar, values-en/ = en) survive
   Soong builds unchanged — the resource directories are standard.
4. No WebView-based screens anywhere (ADR-0003 boundary).
5. Any SELinux domain additions come with `system/sepolicy` review per
   C-1 and never set permissive mode.
