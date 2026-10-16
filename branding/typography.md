# DRS OS Typography

Status: selection specification (Phase 1). **No font binaries are bundled
in this repository.** The selections below are candidates whose licenses
were reviewed for compatibility; final pairing is confirmed with rendered
Arabic specimens before any binary is added (which also adds tools/README.md
inventory rows per policy).

## Font roles

| Role | Primary candidate | License | Rationale |
|---|---|---|---|
| Display / headline (ar+en) | Noto Kufi Arabic | SIL OFL 1.1 | geometric Kufi-influenced forms; premium, modern; strong at large sizes |
| Body / UI (ar) | Noto Naskh Arabic | SIL OFL 1.1 | high long-form readability; authentic naskh rhythm |
| Body / UI (ar, UI-dense alt) | IBM Plex Sans Arabic | SIL OFL 1.1 | humanist sans alternative where UI density favors sans |
| Latin companion | Inter | SIL OFL 1.1 | neutral, tall x-height, harmonizes with Kufi/Naskh weights |
| Mono / code (settings screens) | Noto Sans Mono | SIL OFL 1.1 | tabular clarity |

Decision rule: ship the minimal set that covers ar+en; every candidate
here is OFL — redistribution permitted with license text, which will be
included verbatim at inclusion time. No Google-branded font (e.g., Roboto)
is used as a DRS identity face; system fallback chains may still surface
platform fonts for unstyled content.

## Type ramp (sp; both locales)

| Token | Size/Line | Weight | Use |
|---|---|---|---|
| display | 32/40 | 600 | lock clock |
| headline | 24/32 | 600 | screen titles |
| title | 20/28 | 500 | section headers, card titles |
| body | 16/24 | 400 | primary content |
| body.small | 14/20 | 400 | dense lists |
| caption | 12/16 | 400 | timestamps, legal |

Rules: minimum 12sp; body never below 14sp for Arabic (diacritical marks
need line height); weight axis limited to 400/500/600 to control APK size.

## Arabic & RTL specifics

1. **Digit policy:** Arabic-Indic digits (٠-٩) in Arabic locale prose;
   Western digits (0-9) in numeric fields, codes, and versions (dialer,
   build numbers). Deterministic per context — no per-run detection.
2. Punctuation mirrors automatically (use start/end attributes, never
   left/right).
3. Text alignment: start/end only. Justified Arabic is avoided in UI
   (kashida issues); left natural.
4. Line height carries diacritics (tashkeel) without clipping at all
   scales to 200%.
5. Mixed-direction text (Arabic + Latin brand names) uses Unicode
   bidirectional algorithm defaults with explicit isolates where needed
   in generated strings.

## Accessibility

- All text scales 1.0–2.0 via system font scale; layouts use sp and
  wrap rather than truncate (two-line max then ellipsis with full text in
  TalkBack).
- Contrast pairs verified in colors.md apply to text over all token
  backgrounds.
- No text embedded in images; icons carry contentDescription per
  icon-guidelines.md.

## Inclusion checklist (when binaries are added)

OFL license text verbatim in the font directory · fonts registered in
tools/README.md with source URL and version · rendered specimen review
(ar + en, both themes, scales 1.0/1.3/2.0) recorded in decision log ·
subset only if tooling provenance is recorded (no untraceable subsets).
