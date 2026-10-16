# DRS OS System Architecture

Status: **design baseline (Phase 1)**. Nothing in this document has been
compiled, flashed, or executed as platform code. This is the normative
architecture reference against which all future implementation is reviewed.

Baseline: official AOSP, branch `android-latest-release` (Phase 0 verified
that it resolves to revision `android17-release`; the exact manifest commit
SHA will be recorded at checkout time — ADR-0001). Initial target: x86-64
Cuttlefish virtual device.

## 1. Classification model

Every DRS OS subsystem is classified into exactly one category:

| Class | Meaning |
|---|---|
| **A — Upstream capability** | Provided by stock AOSP; DRS OS uses it as-is with configuration only |
| **B — Planned customization** | DRS OS change, designed and scheduled, not yet implemented |
| **C — Future engineering** | Requires substantial new engineering; design intent documented, implementation not scheduled |

Honesty rule: a subsystem may carry mixed classes, but each capability
inside it is labeled individually. "Designed" never means "works".

## 2. Platform stack

```
┌──────────────────────────────────────────────────────────────┐
│ Applications: DRS system apps (B/C) · third-party apps (A)   │
├──────────────────────────────────────────────────────────────┤
│ Java API framework: system_server, ActivityManager, PMS,     │
│ NotificationManagerService, PermissionController (A)          │
│ DRS SystemUI (B: overlay/customization · C: replacement)     │
│ DRS Security & Privacy Center (C: new privileged app)        │
├──────────────────────────────────────────────────────────────┤
│ ART (Android Runtime) (A)                                    │
├──────────────────────────────────────────────────────────────┤
│ Native daemons & services: init, hwservicemanager, keystore2,│
│ update_engine, servicemanager, vold, netd (A)                │
│ DRS automation engine — planned local daemon (C)             │
├──────────────────────────────────────────────────────────────┤
│ HALs (audio, sensors, graphics, keymint, …) (A: Cuttlefish)  │
├──────────────────────────────────────────────────────────────┤
│ Linux kernel + drivers (A: Cuttlefish kernel; C: device port)│
└──────────────────────────────────────────────────────────────┘
  Cross-cutting: SELinux (A, enforcing) · sepolicy additions (B, gated)
  Verified Boot / AVB (A on supported targets) · OTA (A/B updater) (A/B)
```

## 3. Subsystem catalog

Each entry states purpose, classification, dependencies, interfaces,
security boundary, tests, and implementation stage.

### S1 · AOSP core platform (framework + system services) — Class A
- **Purpose:** the stock Android Java API framework and core system services
  form the runtime substrate for everything else.
- **Dependencies:** kernel, HALs, ART.
- **Interfaces:** public Android SDK APIs and `@SystemApi` surface; Binder IPC.
- **Security boundary:** SELinux domains per service; app sandbox at Zygote
  fork; Binder permission checks.
- **Tests:** upstream CTS suite (planned, Phase ≥ 3, on capable host);
  boot-time smoke on Cuttlefish.
- **Stage:** not built (no checkout). DRS policy: prefer configuration over
  framework modification; any framework change requires per-file justification
  (ADR-0003).

### S2 · ART & application compatibility — Class A
- **Purpose:** execute DRS and third-party apps with standard Android
  semantics; dex2oat/AOT-JIT behavior unchanged.
- **Dependencies:** core platform.
- **Interfaces:** SDK; `pm`/PackageInstaller flows.
- **Security boundary:** sandbox UIDs, seccomp filters, SELinux untrusted_app
  domains; all preserved unchanged.
- **Tests:** app-compat checklist (docs/compatibility/test-matrix.md);
  CTS subset later.
- **Stage:** upstream; no modification planned.

### S3 · Kernel & hardware abstraction — Class A (Cuttlefish) / C (devices)
- **Purpose:** stock Cuttlefish kernel and HALs for the virtual device.
- **Dependencies:** host KVM (compliant host required; sandbox lacks KVM).
- **Interfaces:** standard AOSP kernel/HAL contracts.
- **Security boundary:** kernel enforced by AVB/dm-verity on supported
  partitions; HAL processes in constrained SELinux domains.
- **Tests:** boots under Cuttlefish on compliant host (planned).
- **Stage:** upstream configuration; **no custom kernel or device tree will be
  authored without a verified basis** (Phase 1 constraint, retained).

### S4 · DRS SystemUI — Class B (stage 1: customization) / C (stage 2: identity)
- **Purpose:** lock screen, home surface integration, notification shade,
  Quick Settings, recent apps — visually DRS, mechanically upstream.
- **Dependencies:** S1 framework APIs (StatusBarManagerService, Notification,
  TileService), S8 theme overlays.
- **Interfaces:** upstream SystemUI extension points, resource overlays,
  config resources, official `TileService` API for DRS tiles.
- **Security boundary:** SystemUI runs as `system` UID in its own SELinux
  domain; must not be granted Internet or arbitrary permissions.
- **Tests:** RTL/a11y/dark-light manual matrix; screenshot baselines on
  Cuttlefish.
- **Stage:** B/C — nothing implemented; first step is a resource-overlay-only
  pass to keep fork surface at zero.

### S5 · Settings integration & branding — Class B
- **Purpose:** DRS identity inside system Settings; Arabic-first strings;
  About/legal screens with honest attribution.
- **Dependencies:** S1; overlays.
- **Interfaces:** RRO overlays of `Settings` resources; no Settings fork in
  the first stage.
- **Security boundary:** none new; settings changes are resource-level.
- **Tests:** string/resource inventory review; RTL rendering.
- **Stage:** B, scheduled after first successful Cuttlefish boot.

### S6 · DRS Security & Privacy Center — Class C
- **Purpose:** a single native privileged app surfacing: permission grants
  and recent sensitive accesses, device security state (verified boot, patch
  level), encryption status, automation rules review.
- **Dependencies:** S1 `@SystemApi` where necessary (UsageStats, permission
  events), upstream privacy indicators.
- **Interfaces:** privapp with explicit `privapp-permissions` whitelist —
  every privileged permission individually justified and reviewed; no
  signature-bypass paths.
- **Security boundary:** own SELinux domain; least-privilege whitelist;
  no network permission.
- **Tests:** permission audit checklist; manual matrix; unit tests after
  checkout.
- **Stage:** C. No APK exists. Design only.

### S7 · Notification shade & Quick Settings — Class B
- **Purpose:** DRS tiles (Security Center, Automation on/off, Theme) using
  the official `TileService` API; upstream shade behavior preserved.
- **Dependencies:** S1, S4.
- **Interfaces:** `TileService`, notification channels with strict channel
  hygiene (no persistent ads/spam channels).
- **Security boundary:** tiles are plain system apps; no hidden APIs.
- **Tests:** tile state machine unit tests (post-checkout).
- **Stage:** B/C mix — tiles C, shade behavior B-overlay.

### S8 · Theme engine (overlays) — Class B
- **Purpose:** dark/light foundations, DRS accent palette, Arabic typography
  via Runtime Resource Overlays — no framework fork.
- **Dependencies:** RRO support in S1 (upstream `OverlayManagerService`).
- **Interfaces:** static RRO APKs + `overlayable` policies; per-branding/
  colors.md tokens.
- **Security boundary:** overlays restricted by `overlayable` policy so only
  DRS-signed packages can theme security-critical surfaces.
- **Tests:** overlay application matrix (dark/light/RTL) on Cuttlefish.
- **Stage:** B; resource inventories drafted in branding/.

### S9 · Deterministic local automation engine — Class C
- **Purpose:** user-authored rules ("when X, then Y") executed locally:
  e.g., enable quiet mode at a scheduled time, switch theme by battery
  level, launch camera on double-press. **No AI, no ML, no cloud** — a
  transparent, inspectable rule engine (condition → action, user-visible,
  user-controlled).
- **Dependencies:** S1 public APIs (AlarmManager, JobScheduler,
  AudioManager, Display), S6 for rule review UI.
- **Interfaces:** local service + user-facing rule editor app; rules stored
  in app-private storage; export/import as files.
- **Security boundary:** automation app holds only permissions granted
  through the normal consent flow; no signature permissions; each action
  class whitelisted in code review; rules run only when device is unlocked
  unless explicitly time-based.
- **Tests:** rule-engine unit tests (deterministic), timing edge cases.
- **Stage:** C. Design only; no code.

### S10 · OTA update & integrity architecture — Class B/C
- **Purpose:** A/B (seamless) updates via upstream `update_engine`; signed
  payloads; AVB verified boot with rollback protection on supported targets.
- **Dependencies:** S3 bootloader/AVB support, S1 `system_update` APIs.
- **Interfaces:** `update_engine` AIDL; payload generated by upstream
  `ota_from_target_files` tooling (to be validated at checkout).
- **Security boundary:** payload signature verification before install;
  rollback index anti-downgrade; no sideload path in production builds.
- **Tests:** end-to-end signed-OTA install on Cuttlefish (planned Phase ≥ 3).
- **Stage:** design documented (docs/security/update-integrity.md); C until
  a build exists.

### S11 · Cuttlefish development & test infrastructure — Class A (tooling) / B (scripts)
- **Purpose:** x86-64 Cuttlefish as the only early target; boot, snapshot,
  and console workflows.
- **Dependencies:** compliant host: KVM, ≥ 16 GB RAM practical minimum for
  CVD (64 GB for builds), 400 GB disk (Phase 0 verified sandbox fails all).
- **Interfaces:** `cvd`/`launch_cvd` per official docs; adb over TCP.
- **Security boundary:** userdebug images used for development only;
  production configs must be `user` builds with release keys.
- **Tests:** boot smoke, CTS subset later.
- **Stage:** blocked on compliant host; zero-cost plan documented.

### S12 · Physical-device porting — Class C
- **Purpose:** future bring-up on real hardware.
- **Dependencies:** S3 device trees, vendor HALs, unlocked/lockable
  bootloader with AVB support.
- **Interfaces:** AOSP device conventions (`device/<vendor>/<board>`).
- **Security boundary:** verified boot must be lockable; no shipping with
  unlocked bootloaders.
- **Stage:** C; explicitly out of scope for Phases 0–2; no device tree,
  HAL, or kernel code exists.

## 4. Extension-point policy (normative)

DRS OS prefers, in order: (1) resource overlays (RRO), (2) official
extension APIs (`TileService`, storage/backup contracts, settings
providers), (3) privileged DRS apps with justified permission whitelists,
(4) last resort, minimal documented framework patches. This ordering is
ADR-0003. Any framework patch must list exact files, reasons, and upstream
re-merge cost in the decision log.

## 5. Cross-references

- Integration map (4-way component classification): docs/architecture/integration-map.md
- Native UI integration points per surface: docs/ui/native-system-surfaces.md
- Subsystem boundary matrix: docs/architecture/subsystem-boundaries.md
- Data flows and privacy: docs/architecture/data-flow.md
- Threat model: docs/security/threat-model.md
- Control inventory with status taxonomy: docs/security/security-controls.md
- Integration readiness & blockers: docs/build/integration-readiness.md
