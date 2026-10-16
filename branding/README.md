# DRS OS Design System — Overview and Screen Specifications

**Status: implementation specification (Phase 1 visual system; Phase 2
added the engineering-side integration spec).** These documents define
what DRS OS interfaces *must* look like and how they *will* be built; they
are not claims that any screen runs anywhere. No runnable UI exists; no
asset binaries ship in this repository.

**Engineering counterpart:** for each of the 12 system surfaces — upstream
component, integration points, lifecycle, RTL, accessibility, performance,
and pre-integration tests — see
[docs/ui/native-system-surfaces.md](../docs/ui/native-system-surfaces.md).
This file owns the visual language; that file owns the platform wiring.

## Identity

- **Name:** DRS OS. **Voice:** precise, calm, trustworthy.
- **Direction:** premium, modern, adaptive, accessible,
  performance-conscious.
- **Primary language:** Arabic, right-to-left native (start/end-relative
  layouts, mirrored directional affordances). **Secondary:** English.
- **Products of this system:** dark and light foundations, one accent
  family (DRS Teal), semantic status colors, restrained motion.

## Principles (normative for all DRS surfaces)

1. **Arabic-first RTL** — every layout is authored RTL-capable; LTR is the
   derived variant, never the other way around.
2. **Deterministic intelligence** — any "smart" surface is user-authored
   rules and visible logic (ADR-0004). No assistant, no suggestions engine
   posing as one.
3. **Accessibility floor** — WCAG 2.1 AA contrast (verified numbers in
   colors.md), scalable text to 200%, TalkBack semantics, ≥ 48dp targets.
4. **Performance honesty** — VectorDrawable-only icons, no blurs/heavy
   effects on core surfaces, cold-start budget respected.
5. **Native only** — platform components and resources (ADR-0003); no
   WebView surfaces.

## Screen specifications (implementation specs)

Each spec below names its AOSP surface and the mechanism (overlay /
extension point / new app) per ADR-0003. Visual tokens refer to colors.md,
typography.md, icon-guidelines.md.

### 1. Lock screen
Upstream keyguard, themed via SystemUI overlay (S4 stage 1). Layout:
clock (Arabic-capable face, numerals per typography.md digit policy) upper
third; date line beneath; notification stack lower third with redacted
by-default content; bottom corners: left = camera affordance, right =
phone (mirrored automatically in RTL); center-bottom: short swipe-up hint.
States: locked, biometric-scanning (calm inline indicator, no full-color
pulse), failed-unlock (one gentle shake + message), trusted-device unlocked.
Security honesty: never display verification-state fictions; show real
biometric/enrollment state only.

### 2. Home screen
Upstream Launcher with DRS overlay: grid 4×5 (phone), 5×5 (large);
adaptive-icon row on top-aligned dock (5 slots). Wallpaper: static and
live allowed, dark/light adaptive variants. At-a-glance slot under clock:
date, next alarm, battery — all deterministic data, no predictive content.
Long-press: wallpaper/widgets/settings sheet per upstream behavior.

### 3. App drawer
Swipe-up from home; full-height sheet, surface color token; search field
top (Arabic-first placeholder, keyboard opens in Arabic); alphabetical
A-to-Z index rail localized for Arabic collation; apps render with
adaptive icons; no "suggested apps" machine-learning row — an optional
"user-pinned" row exists instead (deterministic, user-controlled).

### 4. Notification center
Upstream shade themed: surface token background, DRS type ramp, per-channel
grouping preserved; swipe actions: start = expand, end = dismiss (RTL
aware); notification importance visuals exactly match upstream semantics —
DRS does not invent importance levels. Media style: native template with
accent-tinted progress. Privacy: sensitive notifications redact on lock
screen by default (setting exposed in Security Center).

### 5. Quick Settings
Upstream QS architecture; DRS tiles: Security Center (opens S6),
Automation on/off (S9 master toggle with state subtitle), Theme
(dark/light/auto), plus stock tiles. Tile visuals: 2dp-stroke DRS icon
set, active state = DRS Teal fill with dark-on-teal glyph (contrast-verified
pair in colors.md). Brightness slider: DRS accent track. No tile may hide
its state (off must look off).

### 6. System settings
Stock Settings with DRS overlay: Arabic-first strings, DRS About/legal
pages (honest attribution per NOTICE), search localized. DRS additions
live as settings entries pointing to Security Center rather than forking
Settings logic (ADR-0003 stage 1).

### 7. Security & Privacy Center (new privileged app, S6)
Single-entry security truth: dashboard (device lock state, patch level,
verified-boot status — real values only), permission manager (per-permission
grants timeline using platform data), automation rules review (view/pause/
delete rules), privacy indicators explainer. No scareware: no fake
scores, no ads, no upsell. Every number shown must be traceable to a
platform API.

### 8. Recent apps & multitasking
Upstream recents: horizontal card carousel (RTL mirrored), card =
app screenshot + icon + title; actions per card: app info, split-screen
where supported, swipe to dismiss (end-direction). Clear-all pinned
bottom-end. No AI "app suggestions" row — pinned/manual only.

### 9. File management (DRSFiles)
Two-pane list (storage roots → contents), breadcrumb path line (RTL
order), list/grid toggle, sort controls (name/date/size — deterministic,
persisted), operations: copy/move/delete/rename/share with SAF
semantics; destructive actions require explicit confirm with item count.
Downloads/Audio/Images/Documents categories as saved searches, not
re-indexed copies. No cloud accounts in v1 (data-local principle).

### 10. Theme & customization screens
Wallpaper picker (static/live, dark-light adaptive), accent selection
(restricted to palette tokens that pass contrast on both themes —
validated set in colors.md), icon shape (system shapes), font scale
(1.0–2.0 with live preview), display size, dark mode schedule (deterministic:
sunset/sunrise via location **only with explicit consent** or fixed times
— no "AI adaptive" behavior).

## Motion

Duration scale 100–300ms, standard easing curves; every transition is
interruptible (back-gesture safe); reduced-motion setting disables
non-essential movement. No parallax on text; no autoplaying loops.

## Asset provenance rule

Every future icon/asset committed to the repo gets a provenance row
(tools/README.md): designer, date, source file. No AI-generated assets
(ADR-0004). Font binaries are not shipped in this phase — licensing and
selection documented in typography.md.
