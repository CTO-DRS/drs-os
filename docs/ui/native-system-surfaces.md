# DRS OS Native System Interface Specification

Status: **design specification (Phase 2)**. This document pins, for each
of the 12 system surfaces: the upstream AOSP component responsible for
baseline behavior, the DRS customization, the native implementation
approach, integration points, lifecycle/config handling, RTL behavior,
accessibility requirements, performance requirements, and pre-integration
tests.

**Verification rule:** upstream *component* identities are verified
against official AOSP sources; exact *symbols* (class/method names) are
design-level and re-verified against the checked-out tree before any code
is written. This spec pins contracts, not byte-level APIs.

**Boundary rules (binding):** every surface is implemented with native
Android framework components and supported extension points — resource
overlays (RRO), official APIs (`TileService`, `SafetyCenterManager`,
`UpdateEngine`), and privileged apps with justified whitelists. No
WebView, no ordinary-app substitute for privileged functions, no fake
launcher presented as an OS replacement (ADR-0003, NOTICE, packages/apps
rules). Visual tokens: branding/colors.md, typography.md,
icon-guidelines.md.

## Surface matrix

| # | Surface | Upstream baseline | DRS approach (stage) |
|---|---|---|---|
| 1 | Lock screen | SystemUI keyguard | RRO theming (1) → deeper keyguard customization (2, gated) |
| 2 | Home / launcher | Launcher3 | RRO theming + config (1); behavior patches only via ADR (2) |
| 3 | App drawer | Launcher3 AllApps | RRO theming; pinned row (deterministic) (2) |
| 4 | Status bar | SystemUI StatusBar | RRO: colors, icon packs via system theming, layout dims (1) |
| 5 | Notification shade | SystemUI Notifications + NMS | RRO theming (1); channel hygiene policy (2) |
| 6 | Quick Settings | SystemUI QS + framework TileService | RRO theming (1); DRS tiles as TileService (2) |
| 7 | Recents / multitasking | Launcher3 Quickstep | RRO theming (1); no behavior fork (2) |
| 8 | System Settings | packages/apps/Settings | RRO strings/order (1); DRS About/legal (2) |
| 9 | Security & Privacy Center | SafetyCenter + PermissionController | new privileged DRS app surfacing upstream state (2) |
| 10 | Theme & appearance | OverlayManagerService + UiModeManager | DRS overlay set + picker UI (2) |
| 11 | Permission transparency | PermissionController privacy hub, AppOpsManager | DRS presentation layer on upstream data (2) |
| 12 | Update status & integrity | UpdateEngine API + Settings SystemUpdate | DRS status UI reading upstream state (2/3) |

Stage legend: (1) = overlay-only, zero code; (2) = DRS code/overlays with
extension points; (3) = deeper engineering, own ADR required.

---

## 1. Lock screen

- **Upstream:** SystemUI keyguard stack (keyguard module — window,
  bouncer, unlock flow); framework `KeyguardManager` for app-side state.
- **DRS customization:** visual identity via overlay: clock face
  (Arabic-capable), colors, date/notification layout, unlock hint.
  No bouncer logic changes.
- **Native approach:** RRO targeting SystemUI resources; theme-aware
  assets from design tokens.
- **Integration points:** SystemUI resource names (design-level list
  confirmed at checkout); no new IPC.
- **Lifecycle/config:** responds to configuration changes (locale,
  density, font scale, theme) through normal SystemUI config handling;
  no DRS-managed state.
- **RTL:** start/end-relative layout; camera/phone affordances mirror;
  Arabic clock digits per typography digit policy.
- **Accessibility:** TalkBack announces time, notifications, unlock
  affordances; contrast ≥ AA on both themes; large-text safe (no
  truncation of critical state).
- **Performance:** lock/unlock transition within upstream frame budget;
  no DRS-added work on the critical path (resources only).
- **Pre-integration tests:** overlay applies on Cuttlefish boot;
  RTL/a11y matrix cells; unlock with biometric + PIN unchanged;
  `getenforce` Enforcing after themed boot.

## 2. Home screen and launcher integration

- **Upstream:** Launcher3 (packages/apps/Launcher3) — workspace, hotseat,
  default home role.
- **DRS customization:** grid size, icon visuals, at-a-glance slot
  (date/next alarm/battery — deterministic data only), wallpaper
  defaults, search-box placeholder string. Default home remains a
  Launcher3 derivative — DRS does **not** ship a separate fake launcher
  and present it as the OS.
- **Native approach:** overlay Launcher3 resources; any deeper change
  (e.g., at-a-glance view) is a bounded Launcher3 patch listed in the
  minimal-fork ledger or uses a supported widget path.
- **Integration points:** Launcher grid config resources (names
  confirmed at checkout); `WallpaperManager` defaults.
- **Lifecycle/config:** grid/labels respond to locale and density via
  standard launcher config handling.
- **RTL:** workspace paging direction mirrors; hotseat order start-based;
  Arabic labels wrap (two lines max) then ellipsize with full text in
  TalkBack.
- **Accessibility:** actionable elements ≥ 48dp; contentDescription for
  dock and at-a-glance values; focus order start→end.
- **Performance:** cold launch and swipe-up within upstream budget — DRS
  adds zero runtime logic in stage 1; measured against upstream
  baselines once a build exists, never invented numbers.
- **Pre-integration tests:** themed launcher boots; grid/RTL/a11y matrix;
  first-boot flow intact; app open/close from recents unregressed.

## 3. App drawer

- **Upstream:** Launcher3 AllApps container with search.
- **DRS customization:** Arabic-first search placeholder and keyboard
  locale; collation follows Arabic locale ordering; optional user-pinned
  row (deterministic: only apps the user pinned — no ML suggestions,
  ADR-0004).
- **Native approach:** overlay resources; pinned-row behavior only if
  upstream exposes a compatible mechanism, else a proposed bounded patch
  (ADR required).
- **Integration points:** Launcher3 AllApps resources; no new services.
- **Lifecycle/config:** standard launcher lifecycle; drawer state on
  rotation per upstream behavior.
- **RTL:** scrollbar/index rail mirrors; search field start-aligned.
- **Accessibility:** alphabetical rail focusable; search field labeled;
  row actions ≥ 48dp.
- **Performance:** upstream scroll budget; DRS adds no scroll-path work
  in stage 1.
- **Pre-integration tests:** RTL matrix; TalkBack pass; Arabic search
  finds apps (manual QA).

## 4. Status bar

- **Upstream:** SystemUI StatusBar (icons, clock, privacy chips).
- **DRS customization:** colors/dimensions/paddings via overlay; clock
  format honoring digit policy; **privacy chips preserved exactly** —
  DRS never hides or restyles them into ambiguity.
- **Native approach:** RRO only (stage 1); icon shape via system theming.
- **Integration points:** SystemUI status-bar resources.
- **Lifecycle/config:** theme/light-dark/density via standard config.
- **RTL:** clock/status positions follow system RTL layout rules.
- **Accessibility:** chips announce camera/mic/location use; contrast on
  both themes verified.
- **Performance:** zero DRS code added.
- **Pre-integration tests:** chips visible during camera/mic use
  (manual); themed boot; RTL matrix.

## 5. Notification shade

- **Upstream:** SystemUI notification stack + framework
  NotificationManagerService for ranking and channels.
- **DRS customization:** visual theme (surface, type ramp, per-channel
  colors), swipe affordances, media template accent. Ranking semantics
  untouched — DRS never reorders or suppresses notifications silently.
- **Native approach:** RRO stage 1; channel hygiene policy (no spammy
  defaults for DRS apps) lives in DRS apps, not in forks.
- **Integration points:** SystemUI notification resources.
- **Lifecycle/config:** standard shade config; heads-up behavior
  unchanged.
- **RTL:** swipe actions mirror (start=expand, end=dismiss); timestamps
  per digit policy.
- **Accessibility:** expand/collapse actions labeled; contrast AA;
  sensitive-content redaction preserved on lock screen.
- **Performance:** upstream budgets only.
- **Pre-integration tests:** themed shade; redaction on lock screen;
  RTL matrix; media notification with DRS accent renders.

## 6. Quick Settings

- **Upstream:** SystemUI QS framework; third-party tiles via the
  public `TileService` API.
- **DRS customization:** DRS tiles — Security Center (launch), Automation
  master toggle (S9 state), Theme (dark/light/auto) — implemented as
  `TileService`s in DRS apps, plus themed tile visuals (DRS Teal active
  state, contrast-verified pair from colors.md).
- **Native approach:** overlay for visuals; tiles as separate apps using
  the public `TileService` contract; no hidden APIs.
- **Integration points:** `TileService` lifecycle contract; QS tile spec
  resources.
- **Lifecycle/config:** tile lifecycle per platform contract (bind,
  start/stop listening, state publish); state survives SystemUI restarts
  because it lives in the owning app, not in SystemUI.
- **RTL:** tile grid order mirrors; state subtitles localized ar/en.
- **Accessibility:** tile state announced on focus (on/off spoken);
  ≥ 48dp targets.
- **Performance:** event-driven state updates, no polling.
- **Pre-integration tests:** tile add/remove; state truthfulness (off
  looks off); state correct after reboot; RTL/a11y matrix.

## 7. Recent apps and multitasking

- **Upstream:** Quickstep recents inside Launcher3.
- **DRS customization:** visual theming only (cards, background, clear
  button); no AI suggestions — none upstream, none added (ADR-0004).
- **Native approach:** RRO.
- **Integration points:** Quickstep/launcher resources.
- **Lifecycle/config:** standard.
- **RTL:** carousel direction mirrors; swipe-to-dismiss mirrors.
- **Accessibility:** card actions (app info, split-screen, dismiss)
  labeled.
- **Performance:** inherited budgets; zero DRS logic.
- **Pre-integration tests:** themed recents; split-screen intact;
  RTL matrix.

## 8. System Settings

- **Upstream:** packages/apps/Settings.
- **DRS customization:** Arabic-first string overlay, ordering
  adjustments, DRS About/legal screens with honest attribution
  (NOTICE-derived), entry points to Security Center and updater status.
  No Settings logic fork in stages 1–2.
- **Native approach:** RRO for resources; DRS-specific screens live in
  DRS apps reached via shortcuts/injected rows where the platform
  supports it.
- **Integration points:** Settings resources; injection mechanism
  availability confirmed at checkout before use.
- **Lifecycle/config:** standard.
- **RTL:** full RTL pass on every touched screen.
- **Accessibility:** upstream a11y preserved; new strings pass contrast
  and scale tests.
- **Performance:** none added.
- **Pre-integration tests:** themed Settings; search finds DRS entries
  in Arabic and English; About/legal content matches NOTICE.

## 9. Security and Privacy Center

- **Upstream foundations:** SafetyCenter API surface and
  PermissionController (Mainline permission module) provide security
  state, permission management, and privacy indicators.
- **DRS customization:** one privileged DRS app that *surfaces upstream
  truth*: security dashboard (patch level, verified-boot state, lock
  state), permission timeline (permission-usage/AppOps data where
  exposed), automation rules review (S9), privacy-indicator explainer.
  DRS computes no security verdicts and displays no invented scores.
- **Native approach:** privileged app (privapp-permissions whitelist,
  per-permission justification); public/SystemApi only; SafetyCenter
  integration preferred so DRS data also appears in upstream surfaces
  (API availability confirmed at checkout).
- **Integration points:** `SafetyCenterManager`; `PermissionManager` /
  `AppOpsManager` usage history; upstream privacy indicator state.
- **Lifecycle/config:** reads state on open/resume; no persistent
  polling services; background reads only via platform callbacks.
- **RTL:** all screens authored RTL-first.
- **Accessibility:** every number traceable to a platform API and
  announced; status never color-only (icon + text).
- **Performance:** open-to-content bounded; measured later against real
  builds — no invented millisecond claims.
- **Pre-integration tests:** permission audit (every permission
  justified); state accuracy vs adb ground truth; RTL/a11y matrix;
  network-free run.

## 10. Theme and appearance settings

- **Upstream:** OverlayManagerService (RRO switching), `UiModeManager`
  (dark mode state/scheduling), `WallpaperManager`.
- **DRS customization:** theme picker (dark/light/auto with
  deterministic schedule: fixed times, or sunset/sunrise **only with
  explicit location consent** — no "adaptive AI"), accent selection
  restricted to contrast-verified token variants, font-scale preview,
  icon shape.
- **Native approach:** static RRO APKs per variant switched via
  supported overlay APIs; picker UI in a DRS app; `UiModeManager` for
  dark scheduling.
- **Integration points:** overlay enable/disable APIs, `UiModeManager`,
  `WallpaperManager`.
- **Lifecycle/config:** theme changes apply live; overlay selection
  persisted by the platform; schedule state in the picker app.
- **RTL:** preview screens RTL-first.
- **Accessibility:** previews labeled with text, not color-only;
  scale preview respects a11y settings.
- **Performance:** overlay switch within platform latency; no custom
  restart logic.
- **Pre-integration tests:** switch matrix (dark/light × ar/en × scale
  1.0/1.3/2.0); persistence across reboot; schedule fires at the set
  time (deterministic, mocked clock).

## 11. Permission transparency screens

- **Upstream:** PermissionController privacy hub (per-permission grants,
  usage timelines), `AppOpsManager` op-note data, SystemUI privacy
  indicators.
- **DRS customization:** presentation layer only: Arabic-first wording,
  DRS-grouped views (camera/mic/location timelines), plain-language
  explanations, deep links from Security Center. Upstream enforces; DRS
  reads and explains. DRS stores no separate usage log.
- **Native approach:** privileged app reading permission-usage APIs;
  surfaces into SafetyCenter where supported.
- **Integration points:** `PermissionManager`, `AppOpsManager` history
  (exact exposure verified at checkout), SafetyCenter.
- **Lifecycle/config:** reads on demand; display only; local only.
- **RTL:** timelines mirror (start→end); Arabic labels.
- **Accessibility:** textual summaries for screen readers; contrast AA.
- **Performance:** bounded async queries; no jank on open.
- **Pre-integration tests:** grant→use→view shows the event; revoke
  reflects immediately; network-free test proves data stays local.

## 12. System update status and integrity information

- **Upstream:** `UpdateEngine` (A/B) API and state; boot/verified-boot
  state exposed by the platform; Settings SystemUpdate surface pattern.
- **DRS customization:** stage 2 = status surface only: current version,
  security patch level, last update result, verified-boot state, pending
  update state. Stage 3 = updater client (download → verify → apply via
  UpdateEngine) per update-integrity.md. Every displayed value is read
  from the platform — nothing fabricated.
- **Native approach:** status UI via supported APIs/properties (exact
  read path confirmed against the tree); stage 3 uses the public
  `UpdateEngine` flow.
- **Integration points:** `UpdateEngine` state callbacks; verified-boot
  state properties (names confirmed at checkout); Settings injection.
- **Lifecycle/config:** status reads on open; no background network in
  stage 2; stage 3 scheduling per update-integrity.md (user-initiated).
- **RTL:** progress/status screens RTL-first; version strings wrapped in
  bidirectional isolates inside Arabic text.
- **Accessibility:** status never color-only; patch level announced in
  full.
- **Performance:** status reads are cheap and on-demand.
- **Pre-integration tests:** displayed values match adb/build-prop
  ground truth; network-free status view; stage 3 negative path:
  tampered payload shows explicit failure, nothing installs.

---

## Cross-surface requirements

- **Tests required before integration of ANY surface:** static suite
  (validate_project.sh + run_static_checks.sh), overlay application on
  Cuttlefish, `getenforce` Enforcing post-boot, RTL matrix cells for the
  surface, TalkBack pass, no-new-permission audit against the data-flow
  map.
- **Prohibited always:** WebView-based surfaces, invented security
  verdicts, silent suppression of upstream indicators, behavior forks
  outside the minimal-fork ledger.
