# Phase 3 — Security Review

Status: design-and-implementation review of the Phase 3 code (2026-10-16),
performed by inspection and by the automated checks listed at the end.
**This is not a security audit**: no penetration testing, no
independent review, no platform-level verification exists yet. Findings
here are actionable issues with severity/evidence/remediation; the
regression gate for each is the named check or test.

## 1. Review scope

- components/{foundation,engine-core,security-core,settings-core}
- packages/apps/{DRSSecurityCenter,DRSAutomation,DRSSettings}
- platform/integration templates, build scripts additions

## 2. Security properties implemented and verified

| Property | Implementation | Verification |
|---|---|---|
| Closed action registry | `engine-core Validation.ActionRegistry`; unknown action ids are validation errors | unit tests + differential vectors |
| Consent gate for sensitive actions | validation rejects a rule whose action is security-sensitive without recorded consent | unit test `security-sensitive action requires recorded consent` |
| Duplicate-execution prevention | calendar-minute dedup + per-day counters in host runtime | unit tests incl. recovery-after-restart |
| Configurable execution limits | `CoordinatorLimits.maxActionsPerTrigger`; beyond-limit requests recorded, never run | unit test |
| Cancellation | cooperative `CancellationToken`; remaining requests recorded CANCELLED | unit test |
| No personal data in audit | audit record schema is 11 fixed keys; no context values, no free text beyond the rule's own explanation | unit test `audit records contain no personal data` |
| Stable, non-leaking error types | executors return classified errors (e.g. `dnd-policy-access-denied`); exception messages never enter records | unit test `executor exception is contained` |
| Corruption-safe persistence | strict codecs; corrupted input → defaults/empty + flag, never partial salvage | foundation/settings/automation unit tests |
| Input-depth protection | MiniJson nesting limit (64); strict grammar | unit tests incl. depth edge |
| Least privilege (apps) | zero permissions in all three manifests; privileged reads labeled Unavailable | check_phase3_code manifest checks + code inspection |
| No debug-signing claims | release build type unconfigured for signing; key ceremony doc referenced | code inspection (real signing happens at platform stage) |

## 3. Findings (all resolved within Phase 3)

| # | Severity | Finding | Evidence | Resolution / regression gate |
|---|---|---|---|---|
| F-1 | medium | `MiniJson` depth limit originally placed on scalar entry, so a 65-deep nested-array input was accepted against the documented bound (64) | test `rejects excessive nesting depth` failed before fix | bound moved to container entry; test now red/green with 65/64 nesting; regression: MiniJsonTest |
| F-2 | low | KvStore decoder rejected a lone trailing-newline empty document that its own docstring promised to accept | test `empty map round trip` failed before fix | decoder treats exactly one trailing newline as empty; regression: KvStoreTest |
| F-3 | low | three `settings.gradle.kts` files used `#` comments — invalid Kotlin DSL; would have broken the future Gradle build at configure time | check_phase3_code SPDX check flagged the files on first run | converted to `//` comments + SPDX; regression: check_phase3_code.sh |
| F-4 | info | `run_core_tests.sh` output parser mis-formatted counts (cosmetic) | observed output `25] tests` | sed regex fixed; regression: script run |

No unresolved security findings remain from this review.

## 4. Known limitations (not findings — explicit non-goals here)

- The Compose UI layer is not compiled in this sandbox: layout/resource
  errors there are NOT covered by this review (BUILD NOT VERIFIED).
- The executor behavior for DND/battery-saver reflects public-API
  reality; the true platform-integrated behavior is defined only against
  a real tree (docs/phase3/AOSP_INTEGRATION_STATUS.md §3).
- Threat-model-level controls (verified boot, OTA, SELinux enforcement)
  remain design requirements per docs/security/security-controls.md
  five-status ledger — nothing in Phase 3 changes their status except
  as recorded below.

## 5. Ledger deltas for docs/security/security-controls.md

- C-11 (automation engine semantics): `implementation pending` →
  `implementation pending` (core implemented and unit-tested; platform
  integration still pending — the status taxonomy requires platform
  verification for `verified implementation`, so the honest state is
  "implementation started, test pending at platform level").

## 6. Automated gates executed for this review

- `bash build/scripts/check_phase3_code.sh` — 19/19 PASS (AI-dep
  tripwire, secrets tripwire, XML well-formedness, ar/en parity,
  manifest RTL/locale, SPDX, package naming, TODO/FIXME/STOPSHIP scan,
  URL allowlist, version-catalog consistency).
- `bash build/scripts/validate_project.sh` — full suite (results in
  PHASE3_TEST_REPORT.md).
- Full JVM test suites: 188 core tests + 9 app-pure tests + 25 Python
  reference tests, all PASS (see PHASE3_TEST_REPORT.md for commands and
  real outputs).
