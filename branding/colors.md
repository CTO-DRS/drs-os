# DRS OS Color System

Status: token specification (Phase 1). Contrast ratios below were
**computed** with the WCAG 2.1 relative-luminance formula by a project
script at authoring time (19/19 pairs pass their thresholds) — they are
measured values for these exact hex values, not estimates. Re-verify with
the same method if any hex changes.

## Foundations

| Token | Dark | Light | Role |
|---|---|---|---|
| `drs/bg` | `#0F1216` | `#F6F7F9` | window background |
| `drs/surface` | `#171C23` | `#FFFFFF` | cards, sheets |
| `drs/surface.alt` | `#1E242C` | `#EEF1F4` | recessed surfaces, input fields |
| `drs/text.primary` | `#E8EAED` | `#16191D` | primary text & icons |
| `drs/text.secondary` | `#A9B4C0` | `#46505A` | secondary text |
| `drs/text.disabled` | `#6B7683` | `#7A848E` | disabled (decorative use only) |

## Accent — DRS Teal

The single accent family conveys security/trust. Interactive elements use
theme-appropriate variants; never the same hex on both themes.

| Token | Dark | Light |
|---|---|---|
| `drs/primary` (accent on surfaces) | `#2DD4BF` | `#0B7A6E` |
| `drs/primary.container` (filled controls) | `#2DD4BF` | `#0B7A6E` |
| `drs/on.primary.container` | `#0F1216` | `#FFFFFF` |

## Semantic

| Token | Dark | Light | Meaning |
|---|---|---|---|
| `drs/danger` | `#FF6B6B` | `#C53030` | security alerts, destructive actions |
| `drs/warning` | `#FFB454` | `#8A5300` | cautionary states |
| `drs/success` | `#4ADE80` | `#1B7A3D` | verified/healthy states |
| `drs/info` | `#7DD3FC` | light-info to be fixed at implementation | informational |

`info/light` is deliberately not finalized: it must pass AA on light
surfaces and no current screen uses it; it will be verified and recorded
here when first consumed. Not inventing a passing value now would be
guesswork; the placeholder is explicit instead of silent.

## Verified contrast ratios (WCAG 2.1)

Dark theme (thresholds: normal text ≥ 4.5, large/UI ≥ 3.0):

| Foreground | Background | Ratio | Required | Verdict |
|---|---|---|---|---|
| text.primary `#E8EAED` | bg `#0F1216` | 15.58 | 4.5 | PASS |
| text.primary `#E8EAED` | surface `#171C23` | 14.20 | 4.5 | PASS |
| text.secondary `#A9B4C0` | bg | 8.92 | 4.5 | PASS |
| text.secondary `#A9B4C0` | surface | 8.13 | 4.5 | PASS |
| primary `#2DD4BF` | bg | 10.09 | 3.0 | PASS |
| danger `#FF6B6B` | bg | 6.77 | 4.5 | PASS |
| warning `#FFB454` | bg | 10.65 | 4.5 | PASS |
| success `#4ADE80` | bg | 10.78 | 4.5 | PASS |
| on-primary `#0F1216` | primary `#2DD4BF` | 10.09 | 4.5 | PASS |

Light theme:

| Foreground | Background | Ratio | Required | Verdict |
|---|---|---|---|---|
| text.primary `#16191D` | bg `#F6F7F9` | 16.45 | 4.5 | PASS |
| text.primary `#16191D` | surface `#FFFFFF` | 17.63 | 4.5 | PASS |
| text.secondary `#46505A` | bg | 7.67 | 4.5 | PASS |
| text.secondary `#46505A` | surface | 8.22 | 4.5 | PASS |
| primary `#0B7A6E` | surface | 5.22 | 4.5 | PASS |
| primary `#0B7A6E` | bg | 4.87 | 4.5 | PASS |
| danger `#C53030` | surface | 5.47 | 4.5 | PASS |
| warning `#8A5300` | surface | 6.33 | 4.5 | PASS |
| success `#1B7A3D` | surface | 5.39 | 4.5 | PASS |
| on-primary `#FFFFFF` | primary `#0B7A6E` | 5.22 | 4.5 | PASS |

## Usage rules

1. Text and icons only from the token set; no ad-hoc hex in any DRS
   resource (enforced by review; tooling check planned with overlays).
2. Danger color marks destructive *actions* and real security states —
   never marketing emphasis.
3. `text.disabled` never carries information.
4. Accent selection UI (branding/README.md screen 10) offers only
   contrast-verified token variants — the user cannot select a failing
   combination.
5. Both themes ship at parity: no feature is dark-only or light-only.
