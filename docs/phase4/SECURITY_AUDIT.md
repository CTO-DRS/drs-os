# Phase 4 — Security & Privacy Audit

Scope: the actual Phase 4 source tree and the three APKs built from it.
Method: evidence-based checks (binary inspection + source sweeps +
validator tripwires). Static scans passing does NOT prove the operating
system is secure — that claim is impossible to make before platform
integration and on-device verification (see §6).

> **PHASE 5 CORRECTION (2026-10-17):** §1's claim of "0
> `uses-permission` entries in each" APK is **inaccurate as stated**.
> `aapt2 dump badging`/`dump permissions` shows one entry per APK:
> `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` — an auto-generated,
> same-package custom permission (protectionLevel `signature`)
> injected by the androidx/AGP manifest merger to protect dynamic
> receiver registrations on Android T+. It was present in ALL THREE
> Phase 4 APKs (two of which are byte-identical to the Phase 4
> builds, hash-verified), so this is a documentation error, not a
> build regression. The security posture is unchanged: zero platform
> permissions (`android.permission.*`), signature-protected and
> not-exported by construction. The regression guard must key on
> `android.permission.` (platform permissions), not the bare
> `uses-permission` tag.

## 1. Permissions (verified against the built APKs, not the source)

`aapt2 dump badging` on all three debug APKs: **0 `uses-permission`
entries in each**. The manifests request nothing; DRSSecurityCenter and
DRSSettings declare read-only `<queries>` visibility (system settings
intents; `com.drs.os.securitycenter` package from DRSSettings), which
are not permissions. Regression guard: the Phase 3 static suite flags
any permission added without a documented justification.

## 2. Component export surface

| App | Exported components | Notes |
|---|---|---|
| DRSSettings | `MainActivity` (launcher) | standard; no services/receivers/providers |
| DRSAutomation | `MainActivity` (launcher) | `BatteryState` receiver is registered **in code** against the protected system broadcast `ACTION_BATTERY_CHANGED`, never exported, never declared in the manifest |
| DRSSecurityCenter | `MainActivity` (launcher) | standard |

No exported services, receivers, providers, or custom permissions
exist in any manifest. External apps cannot invoke anything beyond the
launcher activities.

## 3. Network & data egress

- **No `INTERNET` permission** (see §1) — the platform enforces this;
  the apps cannot open sockets even if code tried.
- Source sweep (`INTERNET|HttpURLConnection|okhttp|java.net.Socket`
  across `packages/*/app/src` and `components/*/src/main`): **0 hits**.
- No analytics, telemetry, crash-reporting, or tracking dependencies
  exist; dependency set is androidx-only + JUnit (provenance:
  docs/build/dependency-provenance.md).

## 4. Storage & sensitive data

- DRSAutomation persists rules/state via `SharedPreferencesStore`
  (MODE_PRIVATE SharedPreferences, synchronous `commit()` for an honest
  durability boolean). Contents: rule definitions and engine state —
  no user identifiers, no PII by design (audit records carry an
  11-key no-PII schema enforced by tests).
- DRSSettings persists preferences via settings-core's corruption-
  tolerant codec (structural corruption → defaults; per-field enum
  recovery) — tested behavior, 16 tests.
- `allowBackup="false"` on all three apps — nothing enters cloud backup
  or device-transfer flows.
- No file access outside app-private storage; no world-readable modes;
  no `MODE_WORLD_*` usage (source sweep clean).

## 5. Code-level review

- **Arbitrary execution / shell**: none (no Runtime.exec, ProcessBuilder,
  or su usage — sweep clean).
- **Deserialization**: the only parsers are the project's own strict
  codecs (KvStore line codec, MiniJson RFC 8259 with depth bounds —
  depth-bound regression test added in Phase 3 after finding F-1). No
  java.io.Serializable/ObjectInputStream usage.
- **Logging**: exactly one log call in app code (`Log.w` on battery-
  receiver unregister race, message contains no user data). No sensitive
  values are logged anywhere in components (secret/PII patterns are
  scanned by the static suite).
- **Debug-only code**: `BuildConfig.DEBUG` is not branched on anywhere;
  no debug menus, no test-only backdoors compiled into release-shaped
  code.
- **Signing**: debug-signed only (apksigner evidence in
  BUILD_RESULTS.md §4). Debug artifacts are explicitly non-distributable;
  production signing requires the documented key ceremony. The repo
  `.gitignore` hard-rejects any `*.pk8/*.pem/*.keystore`.
- **Crypto**: none used (nothing to encrypt yet); no homegrown crypto.
- **WebView**: none (project rule; also verified by grep — zero
  WebView references).

## 6. Findings

| ID | Severity | Finding | Action |
|---|---|---|---|
| S-1 | INFO | `commit()` on the UI thread for rule persistence — correct for durability semantics, negligible for the small payload; acceptable for v1 foreground-only app | revisit if background execution lands (Phase 5 scheduler work) |
| S-2 | INFO | DND actions in the registry require the user-granted notification-policy access — currently labeled UNAVAILABLE in UI because no permission flow exists | permission flow enters only with a documented justification (test-plan T-14) |
| S-3 | INFO | `SecurityException` handling on permissionless APIs is defensive (try/catch → UNAVAILABLE state) — matches the four-state model | regression tests exist in security-core |
| S-4 | NOTE | The three APKs are debug-signed and must never be distributed | stated in BUILD_RESULTS.md §4; no release build performed |

No high or medium security findings remain open from this audit.
The strongest security property of this phase remains architectural:
**the platform attack surface is zero because the platform integration
has not started** — no system_app/privapp placement, no SELinux domains,
no platform signing exist yet, and none may be claimed.

## 7. Non-claims (explicit)

- "Static scans pass" is NOT a claim that the OS is secure.
- "APKs build" is NOT a claim that they are hardened for production.
- No verified-boot / OTA / SELinux / kernel-hardening property is
  claimed anywhere in this phase; those controls remain
  `implementation pending` in security-controls.md until a real target
  environment exists.
