# DRS OS Android App Compatibility

Status: strategy (Phase 1). No runtime exists to test against yet; this
defines the compatibility contract DRS OS commits to and the known,
honest limitations of an AOSP-only, no-GMS platform.

## 1. Compatibility commitments

1. **Public API preservation.** DRS OS does not modify public Android SDK
   APIs. Customization happens through overlays, extension points, and new
   packages (ADR-0003), so standard apps continue to see standard Android
   behavior.
2. **CTS as the gate.** Once a Cuttlefish build exists, a CTS subset
   becomes the compatibility gate for every release branch; full CTS runs
   are planned on the compliant host (see test-matrix.md).
3. **No hidden-API reliance** in DRS code without a logged decision — this
   keeps future platform upgrades cheap and app behavior stable.

## 2. The no-GMS reality (stated plainly)

DRS OS ships **Google Mobile Services-free** by design (zero budget,
licensing independence, privacy posture):

- Apps built only on the Android SDK work normally.
- Apps that hard-require Play Services (push notifications via FCM, some
  maps/payment/login flows) may degrade or fail. This is a **known,
  accepted product limitation**, documented here rather than hidden; a
  compatibility notes file will accompany releases.
- No GMS replacement is bundled. Users install what they choose through
  normal package installation; DRS OS does not ship any third-party
  services layer in early phases (adding one would be a future ADR with
  security review).

## 3. WebView policy (boundary statement)

- **System functionality is native.** No DRS system app, SystemUI
  surface, or settings screen is built on WebView/HTML/JS (project
  constraint).
- The upstream **WebView component remains available to third-party apps**
  as a normal platform component — that is app rendering, not DRS system
  functionality, and removing it would break app compatibility for no
  security gain.
- DRS system apps must never shell out to a WebView as their primary UI.

## 4. Package policy

- Signature scheme: v2/v3 via standard tooling; DRS apps signed with the
  platform or per-app keys per key-management.md.
- targetSdk policy: DRS apps track the platform targetSdk; no blanket
  compatibility shims for DRS's own code.
- Install-time checks: standard PackageInstaller verification; no
  weakened installer paths in production builds.

## 5. Verification plan

Layered: static repo checks (now) → unit/integration on Cuttlefish
(post-checkout) → CTS subset per release → manual app-compat checklist
using a fixed set of representative app categories (SDK-only app,
WebView-heavy app, offline game, utility with background work). Results
land in the release notes; categories that fail are listed, not omitted.
