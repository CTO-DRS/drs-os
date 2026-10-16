# overlays — Runtime Resource Overlays (Scaffold)

**Status: empty scaffold. No overlay APK exists.**

This directory is the source home for DRS OS runtime resource overlays
(RROs) per ADR-0003's overlays-first policy. In the AOSP tree the built
overlay projects mount at `overlays` (see the local manifest template).

## Planned overlay units (no code yet)

| Overlay | Targets | Content |
|---|---|---|
| DRSFrameworkOverlay | framework-res | color tokens, dimensions per branding/colors.md |
| DRSSystemUIOverlay | SystemUI | status bar, shade, lock screen resources |
| DRSSettingsOverlay | Settings | Arabic-first strings, ordering, About screens |

## Rules

- Overlays only touch resources made `overlayable` by their targets;
  security-critical AOSP resources protected by `overlayable` policy are
  never circumvented.
- Both dark and light variants and both locales (ar first, en second) are
  defined per token from branding/colors.md — no hardcoded colors in any
  DRS package.
- RTL is a first-class resource dimension in every overlay, not a patch.
