# tests/compatibility — Compatibility Test Scaffolding

**Status: planned. No compatibility tests run here yet.** The layered
strategy lives in docs/compatibility/test-matrix.md (Layer 0 static checks
are active; Layers 1–3 require a real AOSP checkout on a compliant host).

## Planned artifacts (created when their subject matter exists)

- App-compat checklist workbook (fixed representative app categories,
  recorded pass/fail per release).
- Manual QA matrix sheets per the branding/README.md screen list:
  locales (ar, en) × themes × font scale × TalkBack × RTL mirroring.
- CTS-subset invocation notes for the compliant host (command lines to be
  confirmed from the actual tree — never invented in advance).

## Honesty rule

A compatibility claim without an executed test behind it is a
documentation defect. Skipped layers are listed as skipped in every
status report.
