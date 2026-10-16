# DRS OS Test Matrix

Status: layers marked **ACTIVE** run today with recorded results; all other
layers are planned and gated on a real AOSP checkout on a compliant host.
No test may be reported as passed if it was not executed.

## Layer 0 — Repository static checks (ACTIVE, Phase 1)

| Check | Tool | Trigger | Status |
|---|---|---|---|
| Required files exist, non-empty, non-placeholder | validate_project.sh | every change | ACTIVE |
| Shell syntax (bash -n) on all scripts | validate_project.sh / run_static_checks.sh | every change | ACTIVE |
| Secret scan (token/key patterns) | validate_project.sh, run_static_checks.sh | every change | ACTIVE |
| No AI/cloud endpoints referenced | validate_project.sh | every change | ACTIVE |
| XML manifest well-formedness + structure | check_manifests.sh | manifest change | ACTIVE |
| SPDX license headers | check_license_headers.sh | script/XML change | ACTIVE |
| Sepolicy guard (no permissive, justified rules only) | check_sepolicy.sh | sepolicy change | ACTIVE |
| CRLF / oversized files | run_static_checks.sh | every change | ACTIVE |
| Markdown internal links resolve | validate_project.sh (python3) | doc change | ACTIVE |

## Layer 1 — Build & boot (PLANNED — needs compliant host + checkout)

| Check | Environment | Gate |
|---|---|---|
| Full sync of pinned manifest | compliant host | build-ready declaration |
| Clean build of Cuttlefish target | compliant host | reproducible double-build hash match |
| Boot to UI on Cuttlefish | compliant host + KVM | smoke checklist |
| SELinux enforcing at boot (`getenforce`) | Cuttlefish | hard gate |
| Verified boot status as designed | Cuttlefish | release gate |

## Layer 2 — Functional (PLANNED)

| Area | Method | Gate |
|---|---|---|
| Framework unmodifiedness | upstream CTS subset | per release |
| Security Center views | permission audit checklist + unit tests | per feature |
| Automation engine | deterministic rule unit tests, timing edges | per feature |
| OTA end-to-end | signed payload install, failure-injection, rollback | per release |
| Theme overlays | dark/light/RTL application matrix | per release |

## Layer 3 — Manual QA matrix (PLANNED, drafted now)

Dimensions: locales (ar, en) × themes (dark, light) × font scale
(1.0–2.0) × RTL mirroring × TalkBack enabled × touch targets (≥48dp).
Surfaces: lock screen, home, app drawer, notifications, QS, settings,
Security Center, recents, files, theming screens (spec list in
branding/README.md). Each cell is a checklist item with pass/fail
evidence at release time.

## Reporting rule

Test reports distinguish: executed+passed, executed+failed, skipped
(with reason). Phase 1 executed Layer 0 only — results are in the Phase 1
validation report; nothing above Layer 0 has ever been run.
