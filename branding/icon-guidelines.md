# DRS OS Icon Guidelines

Status: specification (Phase 1). No icon assets exist yet; every future
icon follows this document and gets a provenance row (tools/README.md).
All DRS icons are **original work** — never copied or traced from existing
brand marks; the DRS motif is an abstract shield/lock geometry developed
for this project.

## Geometry

| Property | Value |
|---|---|
| Canvas | 24 × 24 dp |
| Live area | 20 × 20 dp (2dp padding all sides) |
| Stroke | 2dp, round caps, round joins |
| Corner radius | 2dp outer on filled shapes |
| Keyline shapes | Material system grid conventions (circle 20dp, square 18dp, landscape 20×16, portrait 16×20) |
| Optical corrections | documented per-icon in source comments, never eyeballed |

## Variants

- **Outline** (default, 2dp stroke) for navigation and settings.
- **Filled** for active/selected states and status glyphs (QS active
  tiles, verified-boot badge).
- Adaptive launcher icons: full-bleed 108dp canvas, 72dp safe zone, DRS
  Teal container per colors.md with monochrome layer for themed icons.

## Color & contrast

- Monochrome glyphs use `text.primary` token on their surface.
- Status glyphs (danger/warning/success) use the theme's semantic token —
  never a fixed hex (dark/light parity is mandatory).
- Contrast: icon-on-surface pairs inherit the verified text-token pairs
  (≥ 3.0 effective for UI glyphs; colors.md records exact values).

## RTL mirroring (mandatory review item)

| Icon type | Rule |
|---|---|
| Directional (back/forward, indent, reply, time-flow) | **mirror** in RTL |
| Media controls | mirror play/pause/next per platform convention |
| Non-directional (search, settings, shield, wifi) | never mirror |
| Numbers/letters inside badges | do not mirror glyph content |

Implementation: mirror via `autoMirrored` vector attribute (or RTL
qualifier resource), never runtime scale-flips in code.

## Accessibility & file rules

- Every icon in an interactive control has a contentDescription;
  decorative icons are explicitly `importantForAccessibility=no`.
- Format: **VectorDrawable XML only** (native; no raster, no web/SVG
  assets committed); source SVGs, if any, live out-of-repo with provenance
  noted.
- File naming: `ic_<name>_<variant>.xml` (e.g., `ic_shield_outline.xml`).
- One concept per icon; no text inside icons except single numerals in
  badges.

## DRS motif (original identity)

The DRS mark: a rounded-shield silhouette whose negative space forms a
subtle keyhole — constructed from the geometry table above so it scales
from status-bar to launcher. Variants: outline (UI), filled (trusted
states), monochrome (adaptive). Until vector sources are authored and
reviewed, no rendering of the mark is committed — the written construction
here is the design record, not an asset.
